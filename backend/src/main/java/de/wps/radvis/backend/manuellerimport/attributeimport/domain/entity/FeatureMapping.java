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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FeatureMapping {
	// identifier nur unique innerhalb einer session
	private final long id;

	private final Map<String, Object> properties;

	private LineString importedLineString;

	private final Set<MappedGrundnetzkante> kantenAufDieGemappedWurde;

	@Builder
	public FeatureMapping(Long id, Map<String, Object> properties, LineString importedLineString) {
		require(id, notNullValue());
		require(properties, notNullValue());
		require(importedLineString, notNullValue());

		this.id = id;
		this.properties = properties;
		this.importedLineString = importedLineString;
		this.kantenAufDieGemappedWurde = new HashSet<>();
	}

	public void add(MappedGrundnetzkante mappedGrundnetzkante) {
		require(mappedGrundnetzkante, notNullValue());
		kantenAufDieGemappedWurde.add(mappedGrundnetzkante);
	}

	public void remove(Long kanteId) {
		require(kanteId, notNullValue());
		var success = kantenAufDieGemappedWurde.removeIf(
			mappedGrundnetzkante -> mappedGrundnetzkante.getKanteId().equals(kanteId));
		if (!success) {
			throw new RuntimeException("Keine MappedGrundnetzkante mit kanteId " + kanteId + " gefunden");
		}
	}

	public void updateLinestringAndResetMapping(LineString lineString) {
		this.importedLineString = lineString;
		this.kantenAufDieGemappedWurde.clear();
	}
}
