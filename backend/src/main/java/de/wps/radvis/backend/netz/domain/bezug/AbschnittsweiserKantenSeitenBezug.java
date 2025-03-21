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

package de.wps.radvis.backend.netz.domain.bezug;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
@Slf4j
public class AbschnittsweiserKantenSeitenBezug extends AbschnittsweiserKantenBezug {

	public AbschnittsweiserKantenSeitenBezug(Kante kante, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		Seitenbezug seitenbezug) {
		super(kante, linearReferenzierterAbschnitt);
		require(seitenbezug, notNullValue());
		this.seitenbezug = seitenbezug;
	}

	@Enumerated(EnumType.STRING)
	private Seitenbezug seitenbezug;

	public AbschnittsweiserKantenSeitenBezug withSeitenbezug(Seitenbezug seitenbezug) {
		return new AbschnittsweiserKantenSeitenBezug(this.getKante(), this.getLinearReferenzierterAbschnitt(),
			seitenbezug);
	}

	public AbschnittsweiserKantenSeitenBezug copyWithLR(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return new AbschnittsweiserKantenSeitenBezug(this.getKante(), linearReferenzierterAbschnitt, seitenbezug);
	}

	public Optional<AbschnittsweiserKantenSeitenBezug> intersection(AbschnittsweiserKantenSeitenBezug other) {
		if (!(this.seitenbezug == Seitenbezug.BEIDSEITIG || other.seitenbezug == Seitenbezug.BEIDSEITIG
			|| this.seitenbezug == other.seitenbezug)) {
			return Optional.empty();
		}

		return super.intersection(other)
			.map(kantenBezug -> new AbschnittsweiserKantenSeitenBezug(kantenBezug.getKante(),
				kantenBezug.getLinearReferenzierterAbschnitt(), seitenbezug));
	}

	public static Map<Kante, Set<AbschnittsweiserKantenSeitenBezug>> groupByKante(
		Set<AbschnittsweiserKantenSeitenBezug> bezuege) {
		HashMap<Kante, Set<AbschnittsweiserKantenSeitenBezug>> result = new HashMap<>();
		for (AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug : bezuege) {
			if (!result.containsKey(abschnittsweiserKantenSeitenBezug.getKante())) {
				result.put(abschnittsweiserKantenSeitenBezug.getKante(), new HashSet<>());
			}
			result.get(abschnittsweiserKantenSeitenBezug.getKante()).add(abschnittsweiserKantenSeitenBezug);
		}
		return result;
	}

	public static boolean ueberlappenSichBezuege(Collection<AbschnittsweiserKantenSeitenBezug> bezuege) {
		if (bezuege.isEmpty()) {
			return false;
		}

		return bezuege.stream().anyMatch(
			bezug1 -> bezuege.stream()
				.filter(bezug2 -> bezug1.intersection(bezug2).isPresent())
				.count() > 1 // jeder Bezug überlappt sich selbst, hat er Überlappung mit anderen Bezügen?
		);
	}

	/**
	 * Merged sich überlappende Bezüge (z.B. [0.2, 0.5] und [0.4, 0.6] -> [0.2, 0.6]). Der Seitenbezug wird hierbei mit
	 * beachtet, unterschiedliche Seiten können sich entsprechend nicht überlappen. Die Bezüge müssen alle die gleiche
	 * Kante betreffen.
	 */
	public static List<AbschnittsweiserKantenSeitenBezug> fasseUeberlappendeBezuegeZusammen(
		Collection<AbschnittsweiserKantenSeitenBezug> unsorted) {
		if (unsorted.isEmpty()) {
			return new ArrayList<>();
		}
		AbschnittsweiserKantenSeitenBezug first = unsorted.stream().findFirst().get();
		require(unsorted.stream().allMatch(bezug -> bezug.getKante().equals(first.getKante())));
		require(unsorted.stream().allMatch(bezug -> bezug.getSeitenbezug() == first.getSeitenbezug()));

		List<AbschnittsweiserKantenSeitenBezug> zusammengefasst = new ArrayList<>();

		List<AbschnittsweiserKantenSeitenBezug> sorted = unsorted.stream().sorted(
			Comparator.comparing(AbschnittsweiserKantenBezug::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toList());

		AbschnittsweiserKantenSeitenBezug current = sorted.get(0);
		for (int i = 0; i < sorted.size() - 1; i++) {
			AbschnittsweiserKantenSeitenBezug next = sorted.get(i + 1);

			Optional<AbschnittsweiserKantenSeitenBezug> intersection = current.intersection(next);
			if (intersection.isPresent()) {
				LinearReferenzierterAbschnitt unionLR = current.getLinearReferenzierterAbschnitt()
					.union(next.getLinearReferenzierterAbschnitt()).orElseThrow();
				current = current.copyWithLR(unionLR);
			} else {
				zusammengefasst.add(current);
				current = next;
			}
		}

		zusammengefasst.add(current);

		return zusammengefasst;
	}

	/**
	 * Wendet {@link fasseUeberlappendeBezuegeZusammen} pro kante an. Hier können die Bezüge also von unterschiedlichen
	 * Kanten sein, diese werden hier entsprechend gruppiert betrachtet.
	 */
	// TODO RAD-6596: diese Methode mit verschiedenen Seitenbezügen kompatibel machen
	public static Set<AbschnittsweiserKantenSeitenBezug> fasseUeberlappendeBezuegeProKanteZusammen(
		Set<AbschnittsweiserKantenSeitenBezug> seitenabschnittsKantenSeitenAbschnitte) {
		require(seitenabschnittsKantenSeitenAbschnitte.stream()
			.allMatch(abschnitt -> abschnitt.getSeitenbezug().equals(Seitenbezug.BEIDSEITIG)));

		return AbschnittsweiserKantenSeitenBezug.groupByKante(seitenabschnittsKantenSeitenAbschnitte).values().stream()
			.map(AbschnittsweiserKantenSeitenBezug::fasseUeberlappendeBezuegeZusammen)
			.flatMap(List::stream)
			.collect(Collectors.toSet());
	}

	public static Set<AbschnittsweiserKantenSeitenBezug> ersetzeKanteInAbschnitten(
		Set<AbschnittsweiserKantenSeitenBezug> kantenAbschnitte,
		Kante zuErsetzendeKante, Set<Kante> zuErsetzenDurch, double erlaubteAbweichung) {
		Set<AbschnittsweiserKantenSeitenBezug> result = new HashSet<>(kantenAbschnitte);
		kantenAbschnitte.stream().filter(a -> a.getKante().equals(zuErsetzendeKante)).forEach(a -> {
			Set<AbschnittsweiserKantenSeitenBezug> neueAbschnitte = new HashSet<>();
			zuErsetzenDurch.stream().forEach(
				k -> {
					Optional<LineString> ueberschneidungsLinestring = LineStrings
						.calculateUeberschneidungslinestring(k.getGeometry(), a.getGeometrie());
					if (ueberschneidungsLinestring.isPresent()) {
						Seitenbezug neuerSeitenbezug = a.getSeitenbezug();
						if (!LineStrings.haveSameStationierungsrichtung(zuErsetzendeKante.getGeometry(),
							k.getGeometry())) {
							neuerSeitenbezug = neuerSeitenbezug.withUmgekehrterStationierung();
						}
						neueAbschnitte.add(new AbschnittsweiserKantenSeitenBezug(k,
							LinearReferenzierterAbschnitt.of(k.getGeometry(), a.getGeometrie()), neuerSeitenbezug));
					}
				});

			if (!neueAbschnitte.isEmpty()) {
				Double lengthOfReplacement = neueAbschnitte.stream().map(ab -> ab.getGeometrie().getLength())
					.reduce(Double::sum).orElse(0.0);
				double laengenDifferenz = Math.abs(lengthOfReplacement - a.getGeometrie().getLength());
				if (laengenDifferenz <= erlaubteAbweichung) {
					result.remove(a);
					result.addAll(neueAbschnitte);
				} else {
					log.debug(
						"Kante {} konnte nicht ersetzt werden durch Kanten {}, da Längendifferenz {} m größer ist als die erlaubte Abweichung {} m",
						zuErsetzendeKante.getId(),
						neueAbschnitte.stream().map(abschnitt -> abschnitt.getKante().getId()).toList(),
						laengenDifferenz, erlaubteAbweichung);
				}
			}
		});

		return result;
	}

	public static Set<PunktuellerKantenSeitenBezug> ersetzeKanteInPunkten(Set<PunktuellerKantenSeitenBezug> punkte,
		Kante zuErsetzendeKante, Set<Kante> zuErsetzenDurch, double erlaubteAbweichung) {
		Set<PunktuellerKantenSeitenBezug> result = new HashSet<>(punkte);
		punkte.stream().filter(p -> p.getKante().equals(zuErsetzendeKante)).forEach(p -> {
			Point point = p.getPointGeometry();
			Optional<Kante> ersatzKante = zuErsetzenDurch.stream()
				.filter(k -> k.getGeometry().distance(point) <= erlaubteAbweichung)
				.min((k1, k2) -> Double.compare(k1.getGeometry().distance(point), k2.getGeometry().distance(point)));
			if (ersatzKante.isPresent()) {
				result.remove(p);
				Seitenbezug neuerSeitenbezug = p.getSeitenbezug();
				if (!LineStrings.haveSameStationierungsrichtung(zuErsetzendeKante.getGeometry(),
					ersatzKante.get().getGeometry())) {
					neuerSeitenbezug = neuerSeitenbezug.withUmgekehrterStationierung();
				}
				result
					.add(new PunktuellerKantenSeitenBezug(ersatzKante.get(),
						LineareReferenz.of(ersatzKante.get().getGeometry(), point.getCoordinate()),
						neuerSeitenbezug));
			} else {
				log.info(
					"Punkt auf Kante {} konnte nicht übertragen werden auf Kanten {}, da der Abstand der Kantengeometrien zum Punkt {} größer ist als der erlaubte Abstand {} m",
					zuErsetzendeKante.getId(),
					zuErsetzenDurch.stream().map(k -> k.getId()).toList(),
					point.getCoordinates(),
					erlaubteAbweichung);
			}
		});

		return result;
	}
}
