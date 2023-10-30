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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@MappedSuperclass
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString()
@EqualsAndHashCode
public abstract class LinearReferenzierteAttribute {

	@Getter
	@Embedded
	protected LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;

	protected LinearReferenzierteAttribute(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(linearReferenzierterAbschnitt, notNullValue());
		this.linearReferenzierterAbschnitt = linearReferenzierterAbschnitt;
	}

	/**
	 * Diese Methode nimmt eine LineareReferenz und gibt eine Kopie der bestehenden Attribute zurück, dessen
	 * LineareReferenzierte Attribute so zerschnitten wurden, dass Anfang und Endpunkt der LineareReferenz auch Anfangs-
	 * und Endpunkte (bzw. "Metermarken") von Attributen innerhalb der Liste ist. Dabei bleibt der Inhalt der Attribute auf
	 * in der Liste UNVERÄNDERT!
	 * Beispiel:
	 * Input: attribute = [LRA(0, 0.5), LRA(0.5, 1)], lineareReferenz = LR(0.3, 0.8)
	 * Ergebnis: [LRA(0, 0.3), LRA(0.3, 0.5), LRA(0.5, 0.8), LRA(0.8, 1)]
	 */
	@SuppressWarnings("unchecked")
	public static <T extends LinearReferenzierteAttribute> List<T> getAufLineareReferenzZugeschnitten(List<T> attribute,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {

		List<T> result = new ArrayList<>();
		for (T attribut : attribute) {
			double letzterCutpoint = attribut.linearReferenzierterAbschnitt.getVonValue();
			if (attribut.linearReferenzierterAbschnitt.containsStrictly(linearReferenzierterAbschnitt.getVonValue())) {
				result.add(
					(T) attribut.withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(letzterCutpoint,
							linearReferenzierterAbschnitt.getVonValue())));
				letzterCutpoint = linearReferenzierterAbschnitt.getVonValue();
			}
			if (attribut.linearReferenzierterAbschnitt.containsStrictly(linearReferenzierterAbschnitt.getBisValue())) {
				result.add(
					(T) attribut.withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(letzterCutpoint,
							linearReferenzierterAbschnitt.getBisValue())));
				letzterCutpoint = linearReferenzierterAbschnitt.getBisValue();
			}
			result.add(
				(T) attribut.withLinearReferenzierterAbschnitt(
					LinearReferenzierterAbschnitt.of(letzterCutpoint,
						attribut.linearReferenzierterAbschnitt.getBisValue())));
		}
		return result;
	}

	abstract public LinearReferenzierteAttribute withLinearReferenzierterAbschnitt(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt);

	abstract public LinearReferenzierteAttribute withDefaultValuesAndLineareReferenz(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt);

	abstract public LinearReferenzierteAttribute copyWithSameValues();

	abstract public boolean sindAttributeGleich(LinearReferenzierteAttribute other);

	abstract public boolean widersprechenSichAttribute(LinearReferenzierteAttribute other);

	abstract protected Optional<? extends LinearReferenzierteAttribute> union(LinearReferenzierteAttribute other);

	static public Optional<? extends LinearReferenzierteAttribute> union(LinearReferenzierteAttribute first,
		LinearReferenzierteAttribute second) {
		if (first != null) {
			return first.union(second);
		}
		return Optional.empty();
	}

	public static <T extends LinearReferenzierteAttribute> List<T> defragmentiereLinearReferenzierteAttribute(
		List<T> attribute, double gesamtLaenge, double mindestLaengeProSegment) {
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(attribute.stream()
				.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt).collect(Collectors.toList())),
			"LR-Attribute müssen von 0 bis 1 gehen (=coverFullLine), um defragmentiert werden zu können.");

		attribute.sort(
			Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst));

		List<T> zusammengefasst = fasseGleicheBenachbarteSegmenteZusammen(attribute);

		List<T> bereinigt = entferneSehrKleineLinearReferenzierteAttribute(zusammengefasst, gesamtLaenge,
			mindestLaengeProSegment);

		List<T> interpoliertUndBereinigt = interpoliereLueckenZwischenSegmenten(bereinigt);

		return fasseGleicheBenachbarteSegmenteZusammen(interpoliertUndBereinigt);
	}

	static <T extends LinearReferenzierteAttribute> List<T> entferneSehrKleineLinearReferenzierteAttribute(
		List<T> attribute, double gesamtLaenge, double mindestLaengeProSegment) {

		List<T> collect = attribute.stream()
			.filter(attribut -> attribut.linearReferenzierterAbschnitt.relativeLaenge() * gesamtLaenge
				> mindestLaengeProSegment)
			.collect(Collectors.toList());

		if (collect.isEmpty()) {
			collect = List.of(attribute.stream()
				.max(Comparator.comparing(attribut -> attribut.linearReferenzierterAbschnitt.relativeLaenge())).get());
		}
		return collect;
	}

	@SuppressWarnings("unchecked")
	static <T extends LinearReferenzierteAttribute> List<T> interpoliereLueckenZwischenSegmenten(
		List<T> attribute) {

		List<T> interpoliert = new ArrayList<>();

		T firstSegment = attribute.get(0);
		if (firstSegment.getLinearReferenzierterAbschnitt().getVonValue() != 0) {
			interpoliert.add((T) firstSegment
				.withLinearReferenzierterAbschnitt(
					LinearReferenzierterAbschnitt.of(0,
						firstSegment.getLinearReferenzierterAbschnitt().getBisValue())));
		} else {
			interpoliert.add(firstSegment);
		}

		for (int i = 0; i < attribute.size() - 1; i++) {
			T seg = interpoliert.get(i);
			T nextSeg = attribute.get(i + 1);
			if (seg.getLinearReferenzierterAbschnitt().getBisValue() > nextSeg.getLinearReferenzierterAbschnitt()
				.getVonValue()) {
				throw new RuntimeException("Zu interpolierende LR-Attribute dürfen sich nicht überschneiden!");
			} else if (seg.getLinearReferenzierterAbschnitt().getBisValue() == seg.getLinearReferenzierterAbschnitt()
				.getVonValue()) {
				// punktintersection, keine Interpolation notwendig
				interpoliert.add(nextSeg);
			} else {
				// interpolate
				double lueckenLaenge =
					nextSeg.getLinearReferenzierterAbschnitt().getVonValue() - seg.getLinearReferenzierterAbschnitt()
						.getBisValue();
				double interpolationspunkt = seg.getLinearReferenzierterAbschnitt().getBisValue() + lueckenLaenge / 2;

				LinearReferenzierterAbschnitt interpoliertSeg = LinearReferenzierterAbschnitt
					.of(seg.getLinearReferenzierterAbschnitt().getVonValue(), interpolationspunkt);
				LinearReferenzierterAbschnitt interpoliertNext = LinearReferenzierterAbschnitt
					.of(interpolationspunkt, nextSeg.getLinearReferenzierterAbschnitt().getBisValue());

				interpoliert.set(i, (T) seg.withLinearReferenzierterAbschnitt(interpoliertSeg));
				interpoliert.add((T) nextSeg.withLinearReferenzierterAbschnitt(interpoliertNext));
			}
		}

		T lastSegment = interpoliert.get(interpoliert.size() - 1);
		if (lastSegment.getLinearReferenzierterAbschnitt().getBisValue() != 1.) {
			interpoliert.set(interpoliert.size() - 1,
				(T) lastSegment.withLinearReferenzierterAbschnitt(
					LinearReferenzierterAbschnitt.of(lastSegment.getLinearReferenzierterAbschnitt().getVonValue(), 1)));
		}

		ensure(LinearReferenzierterAbschnitt.segmentsCoverFullLine(interpoliert.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt).collect(Collectors.toList())));
		ensure(attribute.size() == interpoliert.size());
		return interpoliert;
	}

	@SuppressWarnings("unchecked")
	static <T extends LinearReferenzierteAttribute> List<T> fasseGleicheBenachbarteSegmenteZusammen(List<T> attribute) {
		List<T> zusammengefasst = new ArrayList<>();
		T current = attribute.get(0);
		for (int i = 0; i < attribute.size() - 1; i++) {
			T next = attribute.get(i + 1);

			if (current.sindAttributeGleich(next)) {
				Optional<LinearReferenzierterAbschnitt> union = current.linearReferenzierterAbschnitt.union(
					next.linearReferenzierterAbschnitt);
				if (union.isEmpty()) {
					// da SegmentCoversFullLine und wir in order durch gehen, kann das nicht auftreten
					throw new RuntimeException();
				}
				current = (T) current.withLinearReferenzierterAbschnitt(union.get());
			} else {
				zusammengefasst.add(current);
				current = next;
			}
		}

		zusammengefasst.add(current);

		return zusammengefasst;
	}

	@SuppressWarnings("unchecked")
	public static <T extends LinearReferenzierteAttribute> List<T> getReversedAttribute(List<T> attribute) {
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(
				attribute.stream().map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
					.collect(Collectors.toList())),
			"Linear referenzierte Attribute müssen eine gesamte Kante abdecken");

		return attribute.stream()
			.map(attribut -> (T) attribut.withLinearReferenzierterAbschnitt(
				attribut.getLinearReferenzierterAbschnitt().fuerUmgedrehteStrecke()))
			.sorted(Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	public static <T extends LinearReferenzierteAttribute> List<T> schneideAttributeAufLineareReferenzZu(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, List<T> attribute) {
		// Schneide Anfang von nur teils abgebildeter LR Attribute auf Anfang von
		// lineareReferenz zu
		T attributeAnfang = attribute.get(0);
		if (attributeAnfang.getLinearReferenzierterAbschnitt().getVonValue()
			< linearReferenzierterAbschnitt.getVonValue()) {
			LinearReferenzierterAbschnitt anfangsReferenz = LinearReferenzierterAbschnitt
				.of(linearReferenzierterAbschnitt.getVonValue(),
					attributeAnfang.getLinearReferenzierterAbschnitt().getBisValue());
			attribute.set(0, (T) attributeAnfang.withLinearReferenzierterAbschnitt(anfangsReferenz));
		}

		// Schneide Ende von nur teils abgebildeter LR Attribute auf Anfang von lineareReferenz
		// zu
		int letzterIndex = attribute.size() - 1;
		T attributeEnde = attribute
			.get(letzterIndex);
		if (attributeEnde.getLinearReferenzierterAbschnitt().getBisValue()
			> linearReferenzierterAbschnitt.getBisValue()) {
			LinearReferenzierterAbschnitt endReferenz = LinearReferenzierterAbschnitt
				.of(attributeEnde.getLinearReferenzierterAbschnitt().getVonValue(),
					linearReferenzierterAbschnitt.getBisValue());

			attribute.set(letzterIndex, (T) attributeEnde.withLinearReferenzierterAbschnitt(endReferenz));
		}

		return attribute;
	}

	@SuppressWarnings("unchecked")
	public static <T extends LinearReferenzierteAttribute> List<T> getIntersecting(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, List<T> attribute) {
		return getAufLineareReferenzZugeschnitten(attribute, linearReferenzierterAbschnitt).stream()
			.filter(
				attribut -> attribut.getLinearReferenzierterAbschnitt().intersection(linearReferenzierterAbschnitt)
					.isPresent())
			.collect(
				Collectors.toList());
	}

	public abstract <T extends LinearReferenzierteAttribute> T mergeAttributeNimmErstenNichtDefaultWert(T other,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt);

	// Das subset der LinearReferenzierten Attribute für den Ausschnitt der
	// lineareReferenz ermitteln und diese normalisieren.
	@SuppressWarnings("unchecked")
	public static <T extends
		LinearReferenzierteAttribute> List<T> projiziereAuschnittLinearReferenzierterAttributeAufLineareReferenz(
		List<T> linearReferenzierteAttribute, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		boolean muessenAttributeUmgedrehtWerden) {

		if (muessenAttributeUmgedrehtWerden) {
			linearReferenzierteAttribute = LinearReferenzierteAttribute
				.getReversedAttribute(linearReferenzierteAttribute);
			linearReferenzierterAbschnitt = linearReferenzierterAbschnitt.fuerUmgedrehteStrecke();
		}

		// Ermittle Subset der LR Attribute, die lineareReferenzAufQuellnetzKante schneidet
		List<T> ursprungsAttributeSortiertUndGefiltert = sortiereUndFiltereLineareReferenzen(
			linearReferenzierteAttribute, linearReferenzierterAbschnitt)
			// Wir wollen die bestehenden Attribute in diesem Schritt nicht verändern
			.map(T::copyWithSameValues)
			.map(t -> (T) t)
			.collect(Collectors.toList());

		ursprungsAttributeSortiertUndGefiltert = schneideAttributeAufLineareReferenzZu(linearReferenzierterAbschnitt,
			ursprungsAttributeSortiertUndGefiltert);

		return normalisiereLineareReferenzierteAttributeAufUeberschneidung(ursprungsAttributeSortiertUndGefiltert,
			linearReferenzierterAbschnitt);
	}

	public static <T extends LinearReferenzierteAttribute, R> R getWertMitGroesstemAnteil(
		Collection<T> linearReferenzierteAttribute, Function<T, R> valueExtractor) {
		require(!linearReferenzierteAttribute.isEmpty(), "linearReferenzierteAttribute dürfen nicht leer sein");

		if (linearReferenzierteAttribute.size() == 1) {
			return linearReferenzierteAttribute.stream().findFirst().map(valueExtractor).get();
		}

		Map<R, Double> wertToAnteilMap = new HashMap<>();
		linearReferenzierteAttribute.forEach(fuehrungsformAttribute -> {
			double relativeLaenge = fuehrungsformAttribute.getLinearReferenzierterAbschnitt().relativeLaenge();
			R wert = valueExtractor.apply(fuehrungsformAttribute);
			wertToAnteilMap.merge(wert, relativeLaenge, Double::sum);
		});

		return wertToAnteilMap.entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue)).get()
			.getKey();
	}

	@SuppressWarnings("unchecked")
	private static <T extends
		LinearReferenzierteAttribute> List<T> normalisiereLineareReferenzierteAttributeAufUeberschneidung(
		List<T> ursprungsAttributeSortiertUndGefiltert,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnittAufQuellnetzKante) {
		double segementLaenge = linearReferenzierterAbschnittAufQuellnetzKante.getBisValue()
			- linearReferenzierterAbschnittAufQuellnetzKante.getVonValue();

		return ursprungsAttributeSortiertUndGefiltert.stream().map(t -> {
			LinearReferenzierterAbschnitt korrigierteLinearReferenzierterAbschnitt = LinearReferenzierterAbschnitt
				.of((t.getLinearReferenzierterAbschnitt().getVonValue()
						- linearReferenzierterAbschnittAufQuellnetzKante.getVonValue())
						/ segementLaenge,
					(t.getLinearReferenzierterAbschnitt().getBisValue()
						- linearReferenzierterAbschnittAufQuellnetzKante.getVonValue())
						/ segementLaenge);

			return (T) t.withLinearReferenzierterAbschnitt(korrigierteLinearReferenzierterAbschnitt);
		}).collect(Collectors.toList());
	}

	private static <T extends LinearReferenzierteAttribute> Stream<T> sortiereUndFiltereLineareReferenzen(
		List<T> ursprungsAttribute, LinearReferenzierterAbschnitt linearReferenzierterAbschnittDerQuellnetzKante) {

		return ursprungsAttribute.stream()
			.filter(lineareReferenzierteAttribut -> lineareReferenzierteAttribut.getLinearReferenzierterAbschnitt()
				.intersection(linearReferenzierterAbschnittDerQuellnetzKante).isPresent())
			.sorted(
				Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
					LinearReferenzierterAbschnitt.vonZuerst));
	}
}
