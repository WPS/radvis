/*
 * Copyright (c) 2023 WPS - Workplace Solutions GmbH
 *
 * Licensed under the EUPL, Version 1.2 or as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.wps.radvis.backend.netz.domain.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

import de.wps.radvis.backend.common.domain.exception.KeineUeberschneidungException;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LineStrings {

	// Ausgleich von Projektionsungenauigkeit zwischen Referenzsystemen
	private static final double UNGENAUIGKEIT = 0.3;

	public static boolean istUeberschneidungValide(LineString ueberschneidungLineString) {
		return ueberschneidungLineString.getNumPoints() >= 2
			&& ueberschneidungLineString.getLength() >= 0.5 * KnotenIndex.SNAPPING_DISTANCE;
	}

	/**
	 * @param vonLS
	 *     simple LS
	 */
	public static Optional<LineString> calculateUeberschneidungslinestring(LineString aufLS, LineString vonLS) {
		if (!vonLS.isSimple()) {
			return Optional.empty();
		}

		try {
			Geometry intersectionInBufferGeometry = aufLS.buffer(UNGENAUIGKEIT).intersection(vonLS);
			LineString intersectionInBufferLS = getLongestLinestringFromGeometry(intersectionInBufferGeometry);
			LineString intersectionAufLS = projiziereAufLineString(aufLS, intersectionInBufferLS);

			if (!istUeberschneidungValide(intersectionAufLS)) {
				return Optional.empty();
			}
			return Optional.of(intersectionAufLS);
		} catch (KeineUeberschneidungException e) {
			return Optional.empty();
		}
	}

	public static boolean haveSameStationierungsrichtung(LineString geometry1, LineString geometry2) {
		LocationIndexedLine locationIndexedKantenGeometrie = new LocationIndexedLine(
			geometry1);
		LinearLocation anfangUeberschneidung = locationIndexedKantenGeometrie
			.project(geometry2.getStartPoint().getCoordinate());
		LinearLocation endeUeberschneidung = locationIndexedKantenGeometrie
			.project(geometry2.getEndPoint().getCoordinate());

		double abstandZumAnfangUeberschneidung = anfangUeberschneidung.getCoordinate(geometry1)
			.distance(geometry1.getStartPoint().getCoordinate());
		double abstandZumEndeUeberschneidung = endeUeberschneidung.getCoordinate(geometry1)
			.distance(geometry1.getStartPoint().getCoordinate());

		return abstandZumAnfangUeberschneidung < abstandZumEndeUeberschneidung;
	}

	private static LineString projiziereAufLineString(LineString aufLS, LineString vonLS) {
		LocationIndexedLine locationIndexedGrundnetzGeometrie = new LocationIndexedLine(aufLS);

		final LinearLocation projektionDesAnfangs = locationIndexedGrundnetzGeometrie
			.project(vonLS.getStartPoint().getCoordinate());
		final LinearLocation projektionDesEndes = locationIndexedGrundnetzGeometrie
			.project(vonLS.getEndPoint().getCoordinate());

		List<LinearLocation> projektionen = Arrays.asList(projektionDesAnfangs, projektionDesEndes);
		projektionen.sort(Comparator.comparing(LinearLocation::getSegmentIndex)
			.thenComparing(LinearLocation::getSegmentFraction));

		final LinearLocation startlocationDerUeberschneidung = projektionen.get(0);
		final LinearLocation endlocationDerUeberschneidung = projektionen.get(1);

		return (LineString) locationIndexedGrundnetzGeometrie.extractLine(startlocationDerUeberschneidung,
			endlocationDerUeberschneidung);
	}

	private static LineString getLongestLinestringFromGeometry(Geometry grobeIntersections)
		throws KeineUeberschneidungException {
		if (grobeIntersections.isEmpty()) {
			throw new KeineUeberschneidungException("Keine Überschneidung vorhanden");
		}

		if (grobeIntersections.getGeometryType().equals(Geometry.TYPENAME_MULTILINESTRING)) {
			LineString longestIntersection = null;
			for (int i = 0; i < grobeIntersections.getNumGeometries(); i++) {
				Geometry currentIntersection = grobeIntersections.getGeometryN(i);
				if (currentIntersection.getGeometryType().equals(Geometry.TYPENAME_LINESTRING)
					&& currentIntersection.getLength() > UNGENAUIGKEIT) {
					if (longestIntersection == null
						|| longestIntersection.getLength() < currentIntersection.getLength()) {
						longestIntersection = (LineString) currentIntersection;
					}
				}
			}

			if (longestIntersection == null) {
				throw new KeineUeberschneidungException("Keine Überschneidung vorhanden");
			}

			return longestIntersection;
		} else if (grobeIntersections.getGeometryType().equals(Geometry.TYPENAME_LINESTRING)) {
			return (LineString) grobeIntersections;
		} else {
			throw new KeineUeberschneidungException("Keine Überschneidung vorhanden");
		}
	}

	/*
	 * Erkennt Stützpunkte des lineStrings an denen der Winkel zwischen dem ein- und ausgehenden Teilstück nahe 0 bzw.
	 * nahe 2pi ist. Dies entspricht einer Kehrtwende, wenn der lineString entlang der Koordinaten abgelaufen wird.
	 * ACHTUNG! Bei identischen, aufeinanderfolgenden Koordinaten im lineString wird die Prüfung für diese Koordinaten
	 * mit einer Warnung abgebrochen. Es könnte dann aber trotzdem eine Kehrtwende an dieser Stelle vorliegen.
	 */
	public static MultiPoint findeKehrtwenden(LineString lineString) {
		List<Coordinate> kehrtwenden = new ArrayList<>();
		Coordinate[] routedGeometryCoordinates = lineString.getCoordinates();
		// (Muss auf insg. 3 Punkte zugreifen, desshalb wird die erkennung fuer die letzten zwei Punkte
		// vernachlaessigt)
		for (int i = 0; i + 2 < routedGeometryCoordinates.length; i++) {
			Coordinate tip1 = routedGeometryCoordinates[i];
			Coordinate base = routedGeometryCoordinates[i + 1];
			Coordinate tip2 = routedGeometryCoordinates[i + 2];

			if (tip1.equals(base) || tip2.equals(base)) {
				log.warn(
					"LineString enthält identische, aufeinanderfolgende Koordinaten! (Coordinaten ab index {}: {}, {}, {}) Prüfung an dieser Stelle nicht möglich!",
					i, tip1, base, tip2);
				continue;
			}

			if (Angle.angleBetween(tip1, base, tip2) < 0.02) {
				kehrtwenden.add(base);
			}
		}
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createMultiPointFromCoords(kehrtwenden.toArray(new Coordinate[0]));
	}

	/*
	 * Findet Segmente des andersVerlaufenderLineString, an denen er einen groesseren Abstand zu dem referenzLineString
	 * als maxAbstandZwischenPunkten hat. Sobald ein Punkt j von einem LineString einen Abstand >
	 * maxAbstandZwischenPunkten hat, wird der Abschnitt j-1 bis j+1 hinzugefügt (unter Beachtung der Anzahl an
	 * Stuetzpunkten des LineStrings).
	 */
	public static MultiLineString findeSegmenteZweierLinestringsMitAbstandGroesserAls(
		LineString andersVerlaufenderLineString,
		LineString referenzLineString, int maxAbstandZwischenPunkten) {
		List<Coordinate> coordinateTooFarAway = new ArrayList<>();
		List<LineString> abweichendeSegmente = new ArrayList<>();
		Coordinate[] coordinates = andersVerlaufenderLineString.getCoordinates();
		for (int j = 0; j < coordinates.length; j++) {
			Coordinate coordinate = coordinates[j];
			if (referenzLineString.distance(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPoint(coordinate)) > maxAbstandZwischenPunkten) {
				// Wenn die Koordinate j zu weit entfernt ist, ist auch ein Teil der Strecke zwischen j-1 und j
				// zu weit weg. Da die exakte Stelle nicht so wichtig ist, fuege die komplette Strecke zwischen j-1 und
				// j
				// zu dem abweichenden Abschnitt hinzu
				if (coordinateTooFarAway.isEmpty() && j > 0) {
					coordinateTooFarAway.add(coordinates[j - 1]);
				}
				coordinateTooFarAway.add(coordinate);
			} else if (!coordinateTooFarAway.isEmpty()) {
				// obwohl diese Koordinate nicht mehr zu weit weg ist, ist die Strecke zwischen dem letzten "zu weit
				// weg" Punkt noch zum Teil zu weit weg -> wie oben auch diese Strecke komplett mit hinzufuegen
				coordinateTooFarAway.add(coordinate);

				// Abweichendes Segment abschließen
				abweichendeSegmente.add(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createLineString(coordinateTooFarAway.toArray(new Coordinate[0])));
				coordinateTooFarAway.clear();
			}
		}
		// Wenn der letzte Punkt des andersVerlaufenderLineString immernoch zu weit weg ist, dann wurde der obige
		// Codeabschnitt
		// zum Erstellen des LineStrings nicht ausgefuehrt. Aus diesem Grund muss dann hier aus den letzten
		// Koordinaten der LineString erstellt werden (ist erst ab 2 Koordinaten moeglich).
		if (coordinateTooFarAway.size() > 1) {
			abweichendeSegmente.add(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createLineString(coordinateTooFarAway.toArray(new Coordinate[0])));
			coordinateTooFarAway.clear();
		}

		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createMultiLineString(abweichendeSegmente.toArray(new LineString[0]));
	}

	/**
	 * Entfernt jene Kehrtwenden aus einem durch Routing oder Matching erstellten LineString, für die es kein Gegenstück
	 * (Entfernung < 2m) in der originalGeometrie gibt
	 *
	 * @param lineStringAusMatchingOderRouting
	 *     Der durch Routing oder Matching ermittelte LineString, aus dem die Kehrtwenden entfernt werden sollen
	 * @param originalGeometrie
	 *     Die ReferenzGeometrie, die Grundlage für das Routing/Matching war. Dient zur Überprüfung, ob eine
	 *     Kehrtwende artifiziell ist.
	 * @return Der durch Routing oder Matching ermittelte LineString ohne Kehrtwenden
	 */
	public static LineString entferneArtifizielleKehrtwenden(LineString lineStringAusMatchingOderRouting,
		LineString originalGeometrie) {
		MultiPoint kehrtwendenOriginalGeometrie = LineStrings.findeKehrtwenden(originalGeometrie);
		MultiPoint kehrtwendenZielGeometrie = LineStrings.findeKehrtwenden(lineStringAusMatchingOderRouting);

		Coordinate[] result = Arrays.copyOf(lineStringAusMatchingOderRouting.getCoordinates(),
			lineStringAusMatchingOderRouting.getCoordinates().length);
		List<Integer> indicesToRemove = new ArrayList<>();
		for (int i = 0; i < kehrtwendenZielGeometrie.getNumGeometries(); i++) {
			Point point = (Point) kehrtwendenZielGeometrie.getGeometryN(i);

			boolean hatPendantInOriginalGeometrie = Arrays.stream(kehrtwendenOriginalGeometrie.getCoordinates())
				.anyMatch(coor -> coor.distance(point.getCoordinate()) < 2);
			if (!hatPendantInOriginalGeometrie) {
				int pos = ArrayUtils.indexOf(result, point.getCoordinate());
				if (pos < 1 || pos > result.length - 2) {
					log.info("Kehrtwende nicht gefunden oder am Ende/Anfang!");
					continue;
				}
				int j = 1;
				while (pos + j < result.length && pos - j >= 0 &&
					result[pos + j].equals(result[pos - j])) {
					indicesToRemove.add(pos + j);
					indicesToRemove.add(pos - j + 1);
					j++;
				}
			}

		}

		for (int i = 0; i < result.length; i++) {
			if (indicesToRemove.contains(i)) {
				result[i] = null;
			}
		}

		result = Arrays.stream(result).filter(Objects::nonNull).toArray(Coordinate[]::new);

		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(result);
	}

	public static Coordinate getMidPoint(LineString lineString) {
		return new LengthIndexedLine(lineString)
			.extractPoint(lineString.getLength() / 2.0);
	}
}
