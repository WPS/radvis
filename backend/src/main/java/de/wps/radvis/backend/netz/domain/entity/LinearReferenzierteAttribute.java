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
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@MappedSuperclass
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString()
@EqualsAndHashCode
@Slf4j
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
	 * und Endpunkte (bzw. "Metermarken") von Attributen innerhalb der Liste ist. Dabei bleibt der Inhalt der Attribute
	 * auf in der Liste UNVERÄNDERT! Beispiel: Input: attribute = [LRA(0, 0.5), LRA(0.5, 1)], lineareReferenz = LR(0.3,
	 * 0.8) Ergebnis: [LRA(0, 0.3), LRA(0.3, 0.5), LRA(0.5, 0.8), LRA(0.8, 1)]
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

	/**
	 * Fügt das übergebene Attribut in die Liste ein und schneidet die Linearen Referenzen der Segmente rechts und links
	 * davon entsprechend zu. Gleichartige Attribute werden anschließend zusammengefasst.
	 * 
	 * @param <T>
	 * @param intoAttribute
	 * @param newAttribut
	 * @return neue Liste mit resultierenden Attributen
	 */
	public static <T extends LinearReferenzierteAttribute> List<T> insertInto(List<T> intoAttribute,
		T newAttribut) {
		List<T> workingSet = LinearReferenzierteAttribute
			.getAufLineareReferenzZugeschnitten(intoAttribute, newAttribut.getLinearReferenzierterAbschnitt());
		List<T> zuLoeschende = LinearReferenzierteAttribute
			.getIntersecting(newAttribut.getLinearReferenzierterAbschnitt(), workingSet);
		workingSet.removeAll(zuLoeschende);
		workingSet.add(newAttribut);
		LinearReferenzierteAttribute.sortSegmente(workingSet);

		return LinearReferenzierteAttribute
			.fasseGleicheBenachbarteSegmenteZusammen(workingSet);
	}

	abstract public LinearReferenzierteAttribute withLinearReferenzierterAbschnitt(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt);

	abstract public LinearReferenzierteAttribute withDefaultValuesAndLineareReferenz(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt);

	abstract public LinearReferenzierteAttribute copyWithSameValues();

	abstract public boolean sindAttributeGleich(LinearReferenzierteAttribute other);

	abstract public boolean widersprechenSichAttribute(LinearReferenzierteAttribute other);

	abstract protected Optional<? extends LinearReferenzierteAttribute> union(LinearReferenzierteAttribute other);

	protected abstract boolean hasOnlyDefaultAttribute();

	static public Optional<? extends LinearReferenzierteAttribute> union(LinearReferenzierteAttribute first,
		LinearReferenzierteAttribute second) {
		if (first != null) {
			return first.union(second);
		}
		return Optional.empty();
	}

	public static <T extends LinearReferenzierteAttribute> List<T> defragmentiereLinearReferenzierteAttribute(
		List<T> attribute, Laenge gesamtLaenge, Laenge mindestLaengeProSegment) {
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(attribute.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt).collect(Collectors.toList())),
			"LR-Attribute müssen von 0 bis 1 gehen (=coverFullLine), um defragmentiert werden zu können.");

		sortSegmente(attribute);

		List<T> zusammengefasst = fasseGleicheBenachbarteSegmenteZusammen(attribute);

		double relativeSegmentLaenge = Math.min(mindestLaengeProSegment.getValue() / gesamtLaenge.getValue(), 1.0);
		return mergeSegmentsKleinerAls(zusammengefasst, LineareReferenz.of(relativeSegmentLaenge));
	}

	/**
	 * Sortiert übergebene Segmente entlang des LineStrings (in place)
	 * 
	 * @param <T>
	 * @param attribute
	 */
	static <T extends LinearReferenzierteAttribute> void sortSegmente(List<T> attribute) {
		attribute.sort(
			Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst));
	}

	static <T extends LinearReferenzierteAttribute> List<T> entferneSehrKleineLinearReferenzierteAttribute(
		List<T> attribute, double gesamtLaenge, double mindestLaengeProSegment) {

		List<T> collect = attribute.stream()
			.filter(attribut -> attribut.linearReferenzierterAbschnitt.relativeLaenge()
				* gesamtLaenge > mindestLaengeProSegment)
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
				double lueckenLaenge = nextSeg.getLinearReferenzierterAbschnitt().getVonValue() - seg
					.getLinearReferenzierterAbschnitt()
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
	static <T extends LinearReferenzierteAttribute> List<T> fasseGleicheBenachbarteSegmenteZusammen(
		List<T> attribute) {

		List<T> zusammengefasst = new ArrayList<>();
		T current = attribute.get(0);
		for (int i = 0; i < attribute.size() - 1; i++) {
			T next = attribute.get(i + 1);
			require(LineareReferenz.fractionEqual(next.getLinearReferenzierterAbschnitt().getVon(),
				current.getLinearReferenzierterAbschnitt().getBis()), "Segmente müssen sortiert übergeben werden");

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
		if (attributeAnfang.getLinearReferenzierterAbschnitt().getVonValue() < linearReferenzierterAbschnitt
			.getVonValue()) {
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
		if (attributeEnde.getLinearReferenzierterAbschnitt().getBisValue() > linearReferenzierterAbschnitt
			.getBisValue()) {
			LinearReferenzierterAbschnitt endReferenz = LinearReferenzierterAbschnitt
				.of(attributeEnde.getLinearReferenzierterAbschnitt().getVonValue(),
					linearReferenzierterAbschnitt.getBisValue());

			attribute.set(letzterIndex, (T) attributeEnde.withLinearReferenzierterAbschnitt(endReferenz));
		}

		return attribute;
	}

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
	public static <T extends LinearReferenzierteAttribute> List<T> projiziereAuschnittLinearReferenzierterAttributeAufLineareReferenz(
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
	private static <T extends LinearReferenzierteAttribute> List<T> normalisiereLineareReferenzierteAttributeAufUeberschneidung(
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

	/**
	 * Fasst Segmente (echt) kleiner als übergebene Länge mit Nachbarn zusammen
	 */
	public static <T extends LinearReferenzierteAttribute> List<T> mergeSegmentsKleinerAls(
		List<T> attribute, LineareReferenz minimalSegmentLength) {
		require(LinearReferenzierterAbschnitt.segmentsCoverFullLine(attribute.stream()
			.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt).collect(Collectors.toList())),
			"LR-Attribute müssen von 0 bis 1 gehen (=coverFullLine), um defragmentiert werden zu können.");
		require(attribute, notNullValue());
		require(minimalSegmentLength.getAbschnittsmarke() > 0);
		List<T> result = new ArrayList<T>(attribute);

		sortSegmente(result);

		while (result.size() > 1) {
			List<T> afterMerge = mergeFirstMatchingSegment(result, minimalSegmentLength);
			if (afterMerge.size() == result.size()) {
				break;
			}
			result = afterMerge;
		}
		return fasseGleicheBenachbarteSegmenteZusammen(result);
	}

	@SuppressWarnings("unchecked")
	private static <T extends LinearReferenzierteAttribute> List<T> mergeFirstMatchingSegment(List<T> attribute,
		LineareReferenz minimalSegmentLength) {
		require(attribute.size() > 1);

		List<T> result = new ArrayList<>(attribute);

		if (result.stream()
			.allMatch(attr -> attr.getLinearReferenzierterAbschnitt().relativeLaenge() < minimalSegmentLength
				.getAbschnittsmarke())) {
			T letztesAttribut = result.get(result.size() - 1);
			T fullLineSegment = (T) letztesAttribut
				.withLinearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1.0));
			log.debug("Alle Segmente zu klein, werden zu einem Segment zusammengefasst:\n{}", fullLineSegment);
			return List.of(fullLineSegment);
		}

		for (int i = 0; i < result.size(); i++) {
			T attr = result.get(i);
			if (attr.getLinearReferenzierterAbschnitt().relativeLaenge() < minimalSegmentLength.getAbschnittsmarke()) {
				if (i < result.size() - 1) {
					T nextAttribut = result.get(i + 1);
					if (nextAttribut.getLinearReferenzierterAbschnitt().relativeLaenge() >= minimalSegmentLength
						.getAbschnittsmarke()) {
						mergeRight(result, i, attr, nextAttribut);
						break;
					}
				}
				if (i > 0) {
					T previousAttribut = result.get(i - 1);
					if (previousAttribut.getLinearReferenzierterAbschnitt().relativeLaenge() >= minimalSegmentLength
						.getAbschnittsmarke()) {
						mergeLeft(result, i, attr, previousAttribut);
						break;
					}
				}
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private static <T extends LinearReferenzierteAttribute> void mergeLeft(List<T> result, int i, T attr,
		T previousAttribut) {
		log.debug("Merge zu kleines Segment in vorhergehendes Segment:\n{}\n{}", attr,
			previousAttribut);
		result.set(i - 1,
			(T) previousAttribut.withLinearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(
				previousAttribut.getLinearReferenzierterAbschnitt().getVonValue(),
				attr.getLinearReferenzierterAbschnitt().getBisValue())));
		result.remove(i);
	}

	@SuppressWarnings("unchecked")
	private static <T extends LinearReferenzierteAttribute> void mergeRight(List<T> result, int i, T attr,
		T nextAttribut) {
		log.debug("Merge zu kleines Segment in nachfolgendes Segment:\n{}\n{}", attr, nextAttribut);
		result.set(i + 1, (T) nextAttribut.withLinearReferenzierterAbschnitt(
			LinearReferenzierterAbschnitt.of(attr.getLinearReferenzierterAbschnitt().getVonValue(),
				nextAttribut.getLinearReferenzierterAbschnitt().getBisValue())));
		result.remove(i);
	}

	public static boolean allSegmentsHaveMinimaleLaenge(Laenge kantenLaenge,
		Laenge minimaleSegmentLaenge, List<LinearReferenzierterAbschnitt> segmente) {
		// Es gibt auf PROD Kanten < 1m, die gespeichert werden können sollen
		if (kantenLaenge.getValue() < minimaleSegmentLaenge.getValue()) {
			return segmente.size() == 1;
		}

		return segmente.stream()
			.allMatch(
				segment -> segment.relativeLaenge() * kantenLaenge.getValue() >= minimaleSegmentLaenge.getValue());
	}
}
