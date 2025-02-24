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

import static org.valid4j.Assertive.require;

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
import jakarta.validation.constraints.NotNull;
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

	/**
	 * Ermittelt, ob die beiden LineStrings mit einer gewissen Toleranz parallel oder anti-parallel sind.
	 * 
	 * @param anzahlSegmente Anzahl Segmente für eine Unterteilung während der Prüfung auf Parallelität. Höhere Werte führen zu einer höheren Genauigkeit.
	 * @param toleranceInDeg Toleranz in Grad, innerhalb derer LineStrings als parallel gelten.
	 */
	public static boolean sindParallel(LineString lineStringA, LineString lineStringB, int anzahlSegmente,
		double toleranceInDeg) {
		return sindExaktParallel(lineStringA, lineStringB, anzahlSegmente, toleranceInDeg) || sindExaktParallel(
			lineStringA, lineStringB.reverse(), anzahlSegmente, toleranceInDeg);
	}

	/**
	 * Ermittelt, ob die beiden LineStrings parallel verlaufen. Entgegengesetzt verlaufende LineStrings, also anti-
	 * parallele LineStrings, zählen nicht als parallel (es wird also false zurückgegeben).
	 *
	 * Die angegebene Toleranz sollte nicht zu klein sein, da LineStrings während der Prüfung neu zusammengebaut werden
	 * und sich daher deren Geometrie verändert. Selbst perfekt parallel verlaufende LineStrings können also mit einer
	 * zu kleinen Toleranz als "nicht parallel" erkannt werden. Alternativ kann die Anzahl der Segmente erhöht werden,
	 * wodurch sich diese Ungenauigkeiten pro Segment rausmitteln.
	 *
	 * Für die Prüfung werden nur die Winkel der einzelnen Segmente der LineStrings betrachtet. Selbst stark verschobene
	 * LineStrings können daher als parallel gelten.
	 */
	public static boolean sindExaktParallel(LineString lineStringA, LineString lineStringB, int anzahlSegmente,
		double toleranceInDeg) {
		require(!lineStringA.isEmpty());
		require(!lineStringB.isEmpty());
		require(anzahlSegmente > 0);
		require(toleranceInDeg >= 0);

		// Generelles Vorgehen:
		// LineStrings neu "samplen", sodass beide gleich viele Punkte und damit Segmente enthalten. Dann wird pro
		// Segment aus beiden LineStrings geschaut, wie stark sich deren Winkel unterscheiden. Es wird also pro
		// Segment-Paar eine Differenz deren Richtung berechnet und darüber gemittelt. Ist diese gemittelte Differenz
		// der Segment-Richtungen innerhalb der angegebenen Toleranz, sind die beiden LineStrings parallel.

		// Segmente in n Teilstücke unterteilen. Wir brauchen keine echten Geometrien, Koordinaten reichen aus. Zwei
		// aufeinander folgende Koordinaten bilden also ein Segment.
		Coordinate[] lineStringAResamplesCoordinates = getCoordinatesOnLineString(lineStringA, anzahlSegmente + 1);
		Coordinate[] lineStringBResamplesCoordinates = getCoordinatesOnLineString(lineStringB, anzahlSegmente + 1);

		return getAverageAzimuthDifference(lineStringAResamplesCoordinates, lineStringBResamplesCoordinates)
			<= toleranceInDeg;
	}

	/**
	 * Ermittelt den durchschnittlichen Winkel der gegebenen Liniensegmente, die durch die Liste an Koordinaten
	 * ausgedrückt werden. Koordinate i und i+1 bilden ein Segment. Es werden hier also immer zwei aufeinander folgende
	 * Koordinaten betrachtet. Entsprechend müssen beide Arrays gleich viele Koordinaten enthalten.
	 *
	 * Ein Ergebnis von z.B. 10 heißt, dass die Winkel (Azimuthe) der korrespondierenden Segmente aus Array A und B
	 * Durchschnitt um 10° voneinander abweichen. Ein Ergebnis von 0 bedeutet, dass für alle korrespondierenden
	 * Liniensegmente in die EXAKT gleiche Richtung zeigen.
	 *
	 * @param coordinatesA Liste von Koordinaten, es werden die Teilstücke dazwischen betrachtet.
	 * @param coordinatesB Liste von Koordinaten. Anzahl der Koordinaten muss gleich sein wie coordinatesA.
	 * @return Durchschnittlichen Winkel zwischen übergebenen Segmente in Grad.
	 */
	private static @NotNull double getAverageAzimuthDifference(Coordinate[] coordinatesA, Coordinate[] coordinatesB) {
		require(coordinatesA.length == coordinatesB.length);

		int anzahlSegmente = coordinatesA.length - 1;
		double azimuthDifferenceSum = 0d;

		for (int i = 0; i < anzahlSegmente; i++) {
			// Das Ergebnis von Angle.angle() zeigt nach Osten, also ein LineString [(0,0), (10,0)] hat 0°. Das ist für
			// die Bestimmung der Differenten und späteren Nutzung bei der Prüfung auf Parallelität aber irrelevant.
			double azimuthA = Angle.toDegrees(Angle.angle(coordinatesA[i], coordinatesA[i + 1]));
			double azimuthB = Angle.toDegrees(Angle.angle(coordinatesB[i], coordinatesB[i + 1]));

			// Wir betrachten nur die minimal Differenz der Winkel, da Differenzen von Winkeln nicht eindeutig sind.
			// Eine Differenz von 90° ist z.B. äquivalent mit einer Differenz von 270°. Der erste Wert ist der innere
			// und der zweite der äußere Winkel zwischen zwei Geraden. Wir nutzen nur den inneren, um Prüfungen der
			// Art "ist der Durchschnittswert kleiner als ..." zu ermöglichen.
			azimuthDifferenceSum += Math.min(
				toAbsoluteAngle(azimuthA - azimuthB),
				toAbsoluteAngle(azimuthB - azimuthA)
			);
		}

		return azimuthDifferenceSum / anzahlSegmente;
	}

	/**
	 * Erstellt ein Array mit anzahlKoordinaten vielen Koordinaten, die alle auf dem gegebenen LineString liegen
	 * und alle gleich weit voneinander entfernt sind.
	 */
	private static Coordinate[] getCoordinatesOnLineString(LineString basisLineString, int anzahlKoordinaten) {
		LengthIndexedLine locationIndexedLine = new LengthIndexedLine(basisLineString);
		Coordinate[] coordinates = new Coordinate[anzahlKoordinaten];
		coordinates[0] = locationIndexedLine.extractPoint(0);
		for (int i = 1; i < coordinates.length; i++) {
			// anzahlKoordinaten - 1, da wir die erste Koordinate schon haben und sonst die letzte hier ermittelte
			// Koordinate nicht mit der letzten Koordinate des basisLineStrings übereinstimmt.
			coordinates[i] = locationIndexedLine.extractPoint(basisLineString.getLength() / (anzahlKoordinaten - 1)
				* i);
		}
		return coordinates;
	}

	/**
	 * Wandelt den angegebenen Winkel in einen positiven Winkel zwischen 0° (inkl.) und 360° (exkl.) um.
	 * Beispiele: 10° -> 10°; -10° -> 350°; 370° -> 10°; -1000° -> 80°
	 */
	private static double toAbsoluteAngle(double angle) {
		return ((angle % 360) + 360) % 360;
	}
}
