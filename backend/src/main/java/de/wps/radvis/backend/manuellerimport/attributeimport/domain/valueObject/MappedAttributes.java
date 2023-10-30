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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class MappedAttributes {

	private final MappedAttributesProperties attributeProperties;
	private final LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;
	private final boolean orientierungUmgedrehtZurKante;

	// Ob und auf welche Seite sich die Attribute beziehen.
	// Resultiert aus einem attributierten Seitenbezug (wenn vorhanden), ansonsten aus der geometrischen
	// Haendigkeit des Features bzgl. der dazugehoerigen Kante
	private final Seitenbezug seitenbezug;

	private MappedAttributes(Map<String, Object> attributeProperties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Seitenbezug seitenbezug,
		boolean orientierungUmgedrehtZurKante) {
		require(attributeProperties, notNullValue());
		require(linearReferenzierterAbschnitt, notNullValue());
		require(seitenbezug, notNullValue());

		this.attributeProperties = MappedAttributesProperties.of(attributeProperties);
		this.linearReferenzierterAbschnitt = linearReferenzierterAbschnitt;
		this.seitenbezug = seitenbezug;
		this.orientierungUmgedrehtZurKante = orientierungUmgedrehtZurKante;
	}

	static public MappedAttributes of(Map<String, Object> properties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		Seitenbezug seitenbezug,
		boolean orientierungUmgedrehtZurKante) {
		return new MappedAttributes(properties, linearReferenzierterAbschnitt, seitenbezug,
			orientierungUmgedrehtZurKante);
	}

	// Konstruktoraufrufe in static of methoden umschreiben
	static public MappedAttributes of(MappedFeature mappedFeature, boolean istOrientierungVerkehrtherum) {
		return new MappedAttributes(mappedFeature.getProperties(), mappedFeature.getLinearReferenzierterAbschnitt(),
			Seitenbezug.BEIDSEITIG, istOrientierungVerkehrtherum);
	}

	public static String getAttributwertMitGroesstemAnteil(List<MappedAttributes> mappedAttributes, String attributName,
		KantenKonfliktProtokoll kantenKonfliktProtokoll) {
		require(mappedAttributes, notNullValue());
		require(mappedAttributes.size() > 0,
			"Für das Finden des längsten Segments darf die Liste der MappedFeatures nicht leer sein");

		List<MappedAttributes> sortedFeatures = mappedAttributes.stream()
			.sorted(Comparator.comparing(MappedAttributes::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toList());
		Map<String, Double> valueToAnteil = berechneAnteilProAttributwert(attributName, sortedFeatures);

		String attributwertMitGroesstemAnteil = valueToAnteil.entrySet().stream()
			.max(Comparator.comparingDouble(Map.Entry::getValue))
			.get()
			.getKey();

		if (valueToAnteil.entrySet().size() > 1) {
			Set<String> nichtUbernommeneWerte = valueToAnteil.entrySet().stream()
				.filter(e -> e.getValue() > 0.1)
				.map(Map.Entry::getKey)
				.filter(value -> !value.equals(attributwertMitGroesstemAnteil))
				.collect(Collectors.toSet());
			if (!nichtUbernommeneWerte.isEmpty()) {
				kantenKonfliktProtokoll.add(
					new Konflikt(attributName, attributwertMitGroesstemAnteil, nichtUbernommeneWerte));
			}
		}
		return attributwertMitGroesstemAnteil;
	}

	/**
	 * @param mappedAttributesList
	 * 	mappedAttributesList muss normalisiert sein, damit diese Methode funktioniert
	 * @return
	 */
	public static List<MappedAttributes> loeseUeberschneidungenAuf(List<MappedAttributes> mappedAttributesList,
		String attributName, KantenKonfliktProtokoll kantenKonfliktProtokoll) {
		Map<LinearReferenzierterAbschnitt, Deque<MappedAttributes>> map = new HashMap<>();

		mappedAttributesList.forEach(mappedAttributes ->
			map.merge(
				mappedAttributes.getLinearReferenzierterAbschnitt(),
				new ArrayDeque<>(List.of(mappedAttributes)),
				(oldList, listWithNewMappedAttributes) -> {
					oldList.addAll(listWithNewMappedAttributes);
					return oldList;
				}));

		List<MappedAttributes> resultUnsorted = new ArrayList<>();
		for (Map.Entry<LinearReferenzierterAbschnitt, Deque<MappedAttributes>> entry : map.entrySet()) {
			Deque<MappedAttributes> mappedAttributesForLinRef = entry.getValue();
			MappedAttributes lastMappedAttributes = mappedAttributesForLinRef.removeLast();
			if (!mappedAttributesForLinRef.isEmpty()) {
				String uebernommenerWert = lastMappedAttributes.getProperty(attributName);
				Set<String> nichtUebernommeneWerte = mappedAttributesForLinRef.stream()
					.map(mA -> mA.getProperty(attributName))
					.collect(Collectors.toSet());
				nichtUebernommeneWerte.remove(uebernommenerWert);

				if (!nichtUebernommeneWerte.isEmpty()) {
					kantenKonfliktProtokoll.add(
						new Konflikt(
							entry.getKey(),
							attributName,
							uebernommenerWert,
							nichtUebernommeneWerte));
				}
			}
			resultUnsorted.add(lastMappedAttributes);
		}

		return resultUnsorted.stream()
			.sorted(Comparator.comparing(MappedAttributes::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst))
			.collect(Collectors.toList());
	}

	private static Map<String, Double> berechneAnteilProAttributwert(String attribut,
		List<MappedAttributes> sortedFeatures) {
		Map<String, Double> valueToAnteil = new HashMap<>();
		Map<String, Double> valueToCurrentMetermarke = new HashMap<>();
		for (MappedAttributes feature : sortedFeatures) {
			String value = feature.getProperty(attribut);
			LinearReferenzierterAbschnitt linearReferenzierterAbschnitt = feature.getLinearReferenzierterAbschnitt();

			double currentMetermarke = valueToCurrentMetermarke.getOrDefault(value, .0);
			if (currentMetermarke < linearReferenzierterAbschnitt.getBisValue()) {
				valueToAnteil.merge(value,
					LinearReferenzierterAbschnitt.of(
						Math.max(currentMetermarke, linearReferenzierterAbschnitt.getVonValue()),
						linearReferenzierterAbschnitt.getBisValue()).relativeLaenge(),
					Double::sum);
				valueToCurrentMetermarke.put(value, linearReferenzierterAbschnitt.getBisValue());
			}
		}

		return valueToAnteil;
	}

	public String getProperty(String attributName) {
		return this.attributeProperties.getProperty(attributName);
	}

	public Map<String, Object> getProperties() {
		return this.attributeProperties.getPropertyMap();
	}

}
