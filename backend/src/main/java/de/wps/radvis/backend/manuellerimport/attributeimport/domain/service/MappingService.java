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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import java.util.List;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.KantenMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.exception.AttributUebernahmeException;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.KantenKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributes;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;

public class MappingService {

	public void map(AttributeMapper mapper, String attribut, KantenMapping kantenMapping,
		KantenKonfliktProtokoll kantenKonfliktProtokoll) {
		boolean istAttributSeitenbezogen = mapper.isAttributSeitenbezogen(attribut);

		if (!mapper.isLinearReferenziert(attribut)) {
			if (!istAttributSeitenbezogen || !kantenMapping.habenFeaturesSeitenbezug()) {
				mapSimple(mapper, attribut, kantenMapping, kantenKonfliktProtokoll);
			} else {
				mapSeitenbezogen(mapper, attribut, kantenMapping, kantenKonfliktProtokoll);
			}
		} else {
			if (!istAttributSeitenbezogen || !kantenMapping.habenFeaturesSeitenbezug()) {
				mapLinearReferenziert(mapper, attribut, kantenMapping, kantenKonfliktProtokoll);
			} else {
				mapLinearReferenziertUndSeitenbezogen(mapper, attribut, kantenMapping, kantenKonfliktProtokoll);
			}
		}
	}

	private void mapSimple(AttributeMapper mapper, String attribut, KantenMapping kantenMapping,
		KantenKonfliktProtokoll kantenKonfliktProtokoll) {
		List<MappedAttributes> mappedAttributes = filterNullValues(kantenMapping.getMappedAttributes(), attribut);
		if (mappedAttributes.isEmpty()) {
			return;
		}

		if (mapper.isRichtung(attribut)) {
			mappedAttributes = mappedAttributes.stream()
				.map(mA -> mapper.dreheRichtungUm(mA, attribut))
				.collect(Collectors.toList());
		}
		// Brauchen wir hier einen Threshold für nichtLR-Attribute?
		String attributwert = MappedAttributes.getAttributwertMitGroesstemAnteil(
			mappedAttributes,
			attribut,
			kantenKonfliktProtokoll);
		try {
			mapper.applyEinfach(attribut, attributwert, kantenMapping.getKante());
		} catch (AttributUebernahmeException e) {
			writeFehlerToKantenKonfliktProtokoll(attribut, kantenKonfliktProtokoll, e);
		}
	}

	private static void writeFehlerToKantenKonfliktProtokoll(String attribut,
		KantenKonfliktProtokoll kantenKonfliktProtokoll,
		AttributUebernahmeException e, String additionalMessage) {
		e.getFehler().forEach(f -> kantenKonfliktProtokoll.add(
			new Konflikt(f.getLinearReferenzierterAbschnitt(), attribut, f.getMessage() + additionalMessage,
				f.getNichtUerbenommeneWerte())));
	}

	private static void writeFehlerToKantenKonfliktProtokoll(String attribut,
		KantenKonfliktProtokoll kantenKonfliktProtokoll,
		AttributUebernahmeException e) {
		writeFehlerToKantenKonfliktProtokoll(attribut, kantenKonfliktProtokoll, e, "");
	}

	private void mapSeitenbezogen(AttributeMapper mapper, String attribut, KantenMapping kantenMapping,
		KantenKonfliktProtokoll kantenKonfliktProtokoll) {
		List<MappedAttributes> mappedAttributesLinks = filterNullValues(
			kantenMapping.getNormalizedMappedAttributesLinks(), attribut);
		List<MappedAttributes> mappedAttributesRechts = filterNullValues(
			kantenMapping.getNormalizedMappedAttributesRechts(), attribut);

		if (mapper.isRichtung(attribut)) {
			mappedAttributesLinks = mappedAttributesLinks.stream()
				.map(mA -> mapper.dreheRichtungUm(mA, attribut))
				.collect(Collectors.toList());

			mappedAttributesRechts = mappedAttributesRechts.stream()
				.map(mA -> mapper.dreheRichtungUm(mA, attribut))
				.collect(Collectors.toList());
		}

		if (mappedAttributesLinks.isEmpty() && mappedAttributesRechts.isEmpty()) {
			return;
		} else if (mappedAttributesLinks.isEmpty()) {
			mappedAttributesLinks = mappedAttributesRechts;
		} else if (mappedAttributesRechts.isEmpty()) {
			mappedAttributesRechts = mappedAttributesLinks;
		}

		String attributwertLinks = MappedAttributes.getAttributwertMitGroesstemAnteil(mappedAttributesLinks, attribut,
			kantenKonfliktProtokoll);
		String attributwertRechts = MappedAttributes.getAttributwertMitGroesstemAnteil(mappedAttributesRechts,
			attribut, kantenKonfliktProtokoll);

		mapper.applyBeideSeiten(attribut, attributwertLinks, attributwertRechts, kantenMapping.getKante());
	}

	private void mapLinearReferenziert(AttributeMapper mapper, String attribut, KantenMapping kantenMapping,
		KantenKonfliktProtokoll kantenKonfliktProtokoll) {
		List<MappedAttributes> normalizedMappedAttributes = kantenMapping.getNormalizedMappedAttributes();
		List<MappedAttributes> mappedAttributesList = mapper.shouldFilterNullValues(attribut) ?
			filterNullValues(normalizedMappedAttributes, attribut) :
			normalizedMappedAttributes;
		MappedAttributes.loeseUeberschneidungenAuf(mappedAttributesList, attribut, kantenKonfliktProtokoll)
			.forEach(mappedAttributes ->
			{
				try {
					mapper.applyLinearReferenzierterAbschnitt(attribut, mappedAttributes.getAttributeProperties(),
						mappedAttributes.getLinearReferenzierterAbschnitt(),
						kantenMapping.getKante());
				} catch (AttributUebernahmeException e) {
					writeFehlerToKantenKonfliktProtokoll(attribut, kantenKonfliktProtokoll, e);
				}
			});
	}

	private void mapLinearReferenziertUndSeitenbezogen(AttributeMapper mapper, String attribut,
		KantenMapping kantenMapping, KantenKonfliktProtokoll kantenKonfliktProtokoll) {
		List<MappedAttributes> normalizedMappedAttributesLinks = kantenMapping.getNormalizedMappedAttributesLinks();
		List<MappedAttributes> mappedAttributesListLinks = mapper.shouldFilterNullValues(attribut) ?
			filterNullValues(normalizedMappedAttributesLinks, attribut) :
			normalizedMappedAttributesLinks;
		MappedAttributes.loeseUeberschneidungenAuf(mappedAttributesListLinks, attribut, kantenKonfliktProtokoll)
			.forEach(mappedAttributes ->
			{
				try {
					mapper.applyLinearReferenzierterAbschnittSeitenbezogen(
						attribut,
						mappedAttributes.getAttributeProperties(),
						mappedAttributes.getLinearReferenzierterAbschnitt(),
						Seitenbezug.LINKS,
						kantenMapping.getKante());
				} catch (AttributUebernahmeException e) {
					writeFehlerToKantenKonfliktProtokoll(attribut, kantenKonfliktProtokoll, e, " (Seitenbezug: Links)");
				}
			});
		List<MappedAttributes> normalizedMappedAttributesRechts = kantenMapping.getNormalizedMappedAttributesRechts();
		List<MappedAttributes> mappedAttributesListRechts = mapper.shouldFilterNullValues(attribut) ?
			filterNullValues(normalizedMappedAttributesRechts, attribut) :
			normalizedMappedAttributesRechts;
		MappedAttributes.loeseUeberschneidungenAuf(mappedAttributesListRechts, attribut, kantenKonfliktProtokoll)
			.forEach(mappedAttributes ->
			{
				try {
					mapper.applyLinearReferenzierterAbschnittSeitenbezogen(
						attribut,
						mappedAttributes.getAttributeProperties(),
						mappedAttributes.getLinearReferenzierterAbschnitt(),
						Seitenbezug.RECHTS,
						kantenMapping.getKante());
				} catch (AttributUebernahmeException e) {
					writeFehlerToKantenKonfliktProtokoll(attribut, kantenKonfliktProtokoll, e,
						" (Seitenbezug: Rechts)");
				}
			});
	}

	private static List<MappedAttributes> filterNullValues(List<MappedAttributes> mappedAttributes,
		String attributname) {
		return mappedAttributes.stream()
			.filter(mappedAttribute -> {
				String attributWert = mappedAttribute.getProperty(attributname);
				return attributWert != null && !attributWert.isEmpty();
			}).collect(Collectors.toList());
	}
}
