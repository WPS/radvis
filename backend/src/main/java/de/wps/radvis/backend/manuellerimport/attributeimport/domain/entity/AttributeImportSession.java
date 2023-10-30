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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AttributeImportFormat;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class AttributeImportSession extends AbstractImportSession {

	@Getter
	@Setter
	private List<String> attribute;

	@Getter
	@Setter
	private List<FeatureMapping> featureMappings;

	@Getter
	@Setter
	private AttributeImportKonfliktProtokoll attributeImportKonfliktProtokoll;

	@Getter
	private AttributeImportFormat attributeImportFormat;

	@Builder
	public AttributeImportSession(@NonNull Benutzer benutzer, @NonNull Verwaltungseinheit organisation,
		@NonNull List<String> attribute, @NonNull AttributeImportFormat attributeImportFormat) {
		super(benutzer, organisation);
		require(attribute, notNullValue());
		require(attributeImportFormat, notNullValue());
		this.attribute = attribute;
		this.attributeImportFormat = attributeImportFormat;
	}

	public FeatureMapping deleteMappedGrundnetzkanteFromFeatureMapping(Long featureMappingId, Long kanteId) {
		var foundFeatureMapping = this.featureMappings.stream()
			.filter(featureMapping -> featureMapping.getId() == featureMappingId).findFirst()
			.orElseThrow(() -> new RuntimeException("Kein FeatureMapping mit ID " + featureMappingId + " gefunden"));
		foundFeatureMapping.remove(kanteId);
		return foundFeatureMapping;
	}

	public long getAnzahlFeaturesOhneMatch() {
		if (featureMappings == null) {
			return 0;
		}
		return featureMappings.stream()
			.filter(featureMapping -> featureMapping.getKantenAufDieGemappedWurde().isEmpty()).count();
	}

	public long getAnzahlKantenMitUneindeutigerAttributzuordnung() {
		if (attributeImportKonfliktProtokoll == null
			|| attributeImportKonfliktProtokoll.getKantenKonfliktProtokolle() == null) {
			return 0;
		}
		return attributeImportKonfliktProtokoll.getKantenKonfliktProtokolle().size();
	}
}
