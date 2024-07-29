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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public abstract class AbstractNetzBezug {

	public Optional<Point> getDisplayGeometry() {
		if (getImmutableKnotenBezug()
			.stream().findFirst().isPresent()) {
			return Optional.of(getImmutableKnotenBezug().stream().findFirst().get().getPoint());
		} else if (getImmutableKantenPunktBezug()
			.stream().findFirst().isPresent()) {
			return Optional.of(getImmutableKantenPunktBezug().stream().findFirst().get().getPointGeometry());
		} else if (getImmutableKantenAbschnittBezug()
			.stream().findFirst().isPresent()) {
			return Optional.of(getImmutableKantenAbschnittBezug().stream().findFirst().get()
				.getKante().getGeometry().getStartPoint());
		} else {
			return Optional.empty();
		}
	}

	protected boolean isValid(Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezug,
		Set<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug,
		Set<Knoten> knotenBezug) {
		return mindestensEinenBezug(abschnittsweiserKantenSeitenBezug, punktuellerKantenSeitenBezug, knotenBezug) &&
			keineUeberlappung(abschnittsweiserKantenSeitenBezug);
	}

	protected Set<AbschnittsweiserKantenSeitenBezug> defragment(
		Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezug) {
		if (abschnittsweiserKantenSeitenBezug.size() == 0) {
			return new HashSet<>();
		}

		// nach Kante gruppieren
		Map<Kante, Set<AbschnittsweiserKantenSeitenBezug>> groupByKante = AbschnittsweiserKantenSeitenBezug
			.groupByKante(abschnittsweiserKantenSeitenBezug);
		Set<AbschnittsweiserKantenSeitenBezug> result = new HashSet<>();
		groupByKante.forEach((kante, bezuege) -> {
			// sortieren
			List<AbschnittsweiserKantenSeitenBezug> defragmentedLinks = defragmentAbschnitteEinerSeite(bezuege.stream()
				.filter(bezug -> bezug.getSeitenbezug() == Seitenbezug.LINKS).collect(Collectors.toList()));

			List<AbschnittsweiserKantenSeitenBezug> defragmentedRechts = defragmentAbschnitteEinerSeite(bezuege.stream()
				.filter(bezug -> bezug.getSeitenbezug() == Seitenbezug.RECHTS).collect(Collectors.toList()));

			List<AbschnittsweiserKantenSeitenBezug> beidseitig = bezuege.stream()
				.filter(bezug -> bezug.getSeitenbezug() == Seitenbezug.BEIDSEITIG).collect(Collectors.toList());

			Set<AbschnittsweiserKantenSeitenBezug> mergedBeidseitig = fasseSeitenbezugZusammen(defragmentedLinks,
				defragmentedRechts);
			beidseitig.addAll(mergedBeidseitig);

			List<AbschnittsweiserKantenSeitenBezug> defragmentedBeidseitig = defragmentAbschnitteEinerSeite(
				beidseitig);

			defragmentedLinks.removeIf(abschnitt -> mergedBeidseitig.stream()
				.anyMatch(mergedAbschnitt -> mergedAbschnitt.getLinearReferenzierterAbschnitt().getVon()
					.equals(abschnitt.getLinearReferenzierterAbschnitt().getVon())));
			defragmentedRechts.removeIf(abschnitt -> mergedBeidseitig.stream()
				.anyMatch(mergedAbschnitt -> mergedAbschnitt.getLinearReferenzierterAbschnitt().getVon()
					.equals(abschnitt.getLinearReferenzierterAbschnitt().getVon())));

			result.addAll(defragmentedBeidseitig);
			result.addAll(defragmentedLinks);
			result.addAll(defragmentedRechts);
		});

		return result;
	}

	private Set<AbschnittsweiserKantenSeitenBezug> fasseSeitenbezugZusammen(
		List<AbschnittsweiserKantenSeitenBezug> links, List<AbschnittsweiserKantenSeitenBezug> rechts) {
		if (links.isEmpty() || rechts.isEmpty()) {
			return new HashSet<>();
		}

		require(links.stream().map(AbschnittsweiserKantenBezug::getKante).allMatch(
			referenzierteKante -> referenzierteKante.equals(links.stream().findFirst().get().getKante())));
		require(rechts.stream().map(AbschnittsweiserKantenBezug::getKante).allMatch(
			referenzierteKante -> referenzierteKante.equals(rechts.stream().findFirst().get().getKante())));
		require(links.stream().allMatch(linkerAbschnitt -> linkerAbschnitt.getSeitenbezug() == Seitenbezug.LINKS));
		require(rechts.stream().allMatch(rechterAbschnitt -> rechterAbschnitt.getSeitenbezug() == Seitenbezug.RECHTS));

		return links.stream().filter(
			linkerAbschnitt -> rechts.stream().map(AbschnittsweiserKantenBezug::getLinearReferenzierterAbschnitt)
				.anyMatch(
					rechhteReferenz -> rechhteReferenz.equals(linkerAbschnitt.getLinearReferenzierterAbschnitt())))
			.map(commonAbschnitt -> commonAbschnitt.withSeitenbezug(Seitenbezug.BEIDSEITIG))
			.collect(Collectors.toSet());
	}

	private List<AbschnittsweiserKantenSeitenBezug> defragmentAbschnitteEinerSeite(
		List<AbschnittsweiserKantenSeitenBezug> bezuege) {
		List<AbschnittsweiserKantenSeitenBezug> asList = new ArrayList<>(bezuege);

		if (asList.isEmpty()) {
			return asList;
		}

		require(bezuege.stream().allMatch(b -> b.getSeitenbezug() == bezuege.get(0).getSeitenbezug()),
			"Alle Abschnitte müssen auf derselben Seite sein");

		asList.sort(Comparator.comparingDouble(a -> a.getLinearReferenzierterAbschnitt().getVonValue()));

		List<AbschnittsweiserKantenSeitenBezug> defragmented = new ArrayList<>();
		defragmented.add(asList.get(0));
		for (int i = 1; i < asList.size(); i++) {
			AbschnittsweiserKantenSeitenBezug nextElement = asList.get(i);
			AbschnittsweiserKantenSeitenBezug previousElement = defragmented.get(defragmented.size() - 1);
			if (LineareReferenz.fractionEqual(nextElement.getLinearReferenzierterAbschnitt().getVon(),
				previousElement.getLinearReferenzierterAbschnitt().getBis())) {
				// adjazente Elemente mergen
				AbschnittsweiserKantenSeitenBezug merged = new AbschnittsweiserKantenSeitenBezug(
					previousElement.getKante(),
					LinearReferenzierterAbschnitt.of(
						previousElement.getLinearReferenzierterAbschnitt().getVonValue(),
						nextElement.getLinearReferenzierterAbschnitt().getBisValue()),
					previousElement.getSeitenbezug());
				defragmented.remove(defragmented.size() - 1);
				defragmented.add(merged);
			} else {
				defragmented.add(nextElement);
			}
		}
		return defragmented;
	}

	public static boolean mindestensEinenBezug(
		Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezug,
		Set<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezug,
		Set<Knoten> knotenBezug) {
		boolean listenVorhanden = abschnittsweiserKantenSeitenBezug != null && punktuellerKantenSeitenBezug != null
			&& knotenBezug != null;
		return listenVorhanden && (!abschnittsweiserKantenSeitenBezug.isEmpty()
			|| !punktuellerKantenSeitenBezug.isEmpty()
			|| !knotenBezug.isEmpty());
	}

	protected static boolean keineUeberlappung(
		Set<AbschnittsweiserKantenSeitenBezug> abschnittsweiserKantenSeitenBezug) {
		Map<Kante, Set<AbschnittsweiserKantenSeitenBezug>> groupbyKante = AbschnittsweiserKantenSeitenBezug
			.groupByKante(
				abschnittsweiserKantenSeitenBezug);
		return groupbyKante.values().stream().noneMatch(AbschnittsweiserKantenSeitenBezug::ueberlappenSichBezuege);
	}

	public GeometryCollection getGeometrie() {
		List<Geometry> geometries = new ArrayList<>();
		getMutableAbschnittsweiserKantenSeitenBezug().stream().map(kantenSeitenAbschnitt -> {
			LineString geometry = kantenSeitenAbschnitt.getKante().getGeometry();
			LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(geometry);
			LinearReferenzierterAbschnitt linearReferenzierterAbschnitt = kantenSeitenAbschnitt
				.getLinearReferenzierterAbschnitt();
			return lengthIndexedLine.extractLine(
				linearReferenzierterAbschnitt.getVonValue() * geometry.getLength(),
				linearReferenzierterAbschnitt.getBisValue() * geometry.getLength());
		}).forEach(geometries::add);

		getMutablePunktuellerKantenSeitenBezug().stream().map(kantenpunkt -> {
			LineString geometry = kantenpunkt.getKante().getGeometry();
			LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(geometry);
			LineareReferenz lineareReferenz = kantenpunkt.getLineareReferenz();
			Coordinate punktkoodinate = lengthIndexedLine.extractPoint(
				lineareReferenz.getAbschnittsmarke() * geometry.getLength());
			return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(punktkoodinate);
		}).forEach(geometries::add);

		getMutableKnotenBezug().stream().map(Knoten::getPoint).forEach(geometries::add);
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createGeometryCollection(geometries.toArray(new Geometry[0]));
	}

	public void removeKante(Long kanteId) {
		require(kanteId, notNullValue());
		getMutableAbschnittsweiserKantenSeitenBezug().removeIf(
			kantenBezug -> kantenBezug.getKante().getId().equals(kanteId));
		getMutablePunktuellerKantenSeitenBezug().removeIf(punkt -> punkt.getKante().getId().equals(kanteId));
	}

	public void removeKnoten(Long knotenId) {
		require(knotenId, notNullValue());
		getMutableKnotenBezug().removeIf(knoten -> knoten.getId().equals(knotenId));
	}

	public Set<AbschnittsweiserKantenSeitenBezug> getImmutableKantenAbschnittBezug() {
		return Collections.unmodifiableSet(new HashSet<>(getMutableAbschnittsweiserKantenSeitenBezug()));
	}

	public Set<PunktuellerKantenSeitenBezug> getImmutableKantenPunktBezug() {
		return Collections.unmodifiableSet(new HashSet<>(getMutablePunktuellerKantenSeitenBezug()));
	}

	public Set<Knoten> getImmutableKnotenBezug() {
		return Collections.unmodifiableSet(new HashSet<>(getMutableKnotenBezug()));
	}

	protected abstract Set<AbschnittsweiserKantenSeitenBezug> getMutableAbschnittsweiserKantenSeitenBezug();

	protected abstract Set<PunktuellerKantenSeitenBezug> getMutablePunktuellerKantenSeitenBezug();

	protected abstract Set<Knoten> getMutableKnotenBezug();

}
