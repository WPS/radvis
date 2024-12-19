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
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode
@Slf4j
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

	public List<LineString> getLineStrings() {
		return getImmutableKantenAbschnittBezug().stream().map(kantenSeitenAbschnitt -> {
			return kantenSeitenAbschnitt.getGeometrie();
		}).toList();
	}

	public List<Point> getPoints() {
		List<Point> points = new ArrayList<>();

		getImmutableKantenPunktBezug().stream().forEach(punkt -> {
			points.add(punkt.getPointGeometry());
		});

		getImmutableKnotenBezug().stream().map(Knoten::getPoint).forEach(points::add);

		return points;
	}

	public GeometryCollection getGeometrie() {
		List<Geometry> geometries = new ArrayList<>();

		geometries.addAll(getPoints());

		geometries.addAll(getLineStrings());

		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createGeometryCollection(geometries.toArray(new Geometry[0]));
	}

	public boolean containsKante(Long kanteId) {
		require(kanteId, notNullValue());
		boolean containsAbschnittWithKantenId = getImmutableKantenAbschnittBezug().stream()
			.anyMatch(kantenBezug -> kantenBezug.getKante().getId().equals(kanteId));
		boolean containsPunkbezugWithKantenId = getImmutableKantenPunktBezug().stream()
			.anyMatch(punkt -> punkt.getKante().getId().equals(kanteId));
		return containsAbschnittWithKantenId || containsPunkbezugWithKantenId;
	}

	public boolean containsKnoten(long knotenId) {
		return getImmutableKnotenBezug().stream().anyMatch(knoten -> knoten.getId().equals(knotenId));
	}

	public abstract Set<AbschnittsweiserKantenSeitenBezug> getImmutableKantenAbschnittBezug();

	public abstract Set<PunktuellerKantenSeitenBezug> getImmutableKantenPunktBezug();

	public abstract Set<Knoten> getImmutableKnotenBezug();

	protected Set<Knoten> ersetzeKnoten(Set<Knoten> alterKnotenBezug, Map<Long, Knoten> ersatzKnoten) {
		Set<Knoten> neuerKnotenBezug = new HashSet<>(alterKnotenBezug);
		int sizeBefore = neuerKnotenBezug.size();

		for (Long zuErsetzenderKnoten : ersatzKnoten.keySet()) {
			Knoten neuerKnoten = ersatzKnoten.get(zuErsetzenderKnoten);
			if (!neuerKnotenBezug.contains(neuerKnoten)) {
				if (neuerKnotenBezug.removeIf(kn -> kn.getId().equals(zuErsetzenderKnoten))) {
					neuerKnotenBezug.add(neuerKnoten);
				}
			} else if (neuerKnotenBezug.stream().anyMatch(kb -> kb.getId().equals(zuErsetzenderKnoten))) {
				log.debug(
					"Knoten {} konnte nicht ersetzt werden, da Ersatzknoten {} bereits im Netzbezug enthalten ist. "
						+ "Bitte stattdessen den Knoten aus dem Netzbezug löschen.",
					zuErsetzenderKnoten, neuerKnoten.getId());
			}
		}

		ensure(sizeBefore == neuerKnotenBezug.size());

		return neuerKnotenBezug;
	}
}
