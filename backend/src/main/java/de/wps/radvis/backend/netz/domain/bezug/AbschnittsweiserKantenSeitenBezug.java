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

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
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

	public static List<AbschnittsweiserKantenSeitenBezug> fasseIntersectionsZusammen(
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
				LinearReferenzierterAbschnitt.vonZuerst)).collect(Collectors.toList());

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
}
