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

package de.wps.radvis.backend.quellimport.common.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import lombok.Getter;

public class ImportedFeatureMapView extends AbstractEntity {
	@Getter
	private Geometry geometrie;

	@Getter
	private Double anteil;

	public ImportedFeatureMapView(Long id, Geometry geometrie, Double anteil) {
		require(id, notNullValue());
		require(geometrie, notNullValue());
		require(geometrie.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
				+ "' entspricht nicht geometry.getSRID() '" + geometrie.getSRID() + "'");
		require(geometrie.getGeometryType().equals(Geometry.TYPENAME_LINESTRING)
			|| geometrie.getGeometryType().equals(Geometry.TYPENAME_POINT)
			|| geometrie.getGeometryType().equals(Geometry.TYPENAME_MULTILINESTRING), "Geometrietyp nicht zulässig");

		this.id = id;
		this.geometrie = geometrie;
		this.anteil = anteil;

		ensure(getGeometrie(), notNullValue());
		ensure(getGeometrie().getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
	}
}
