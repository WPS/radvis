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
import static org.valid4j.Assertive.require;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import lombok.Getter;

@Getter
public class KanteGeometryView {
	private final long id;
	private final LineString geometry;

	public KanteGeometryView(long id, Geometry geometry) {
		require(geometry, notNullValue());
		require(geometry.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
				+ "' entspricht nicht geometry.getSRID() '" + geometry.getSRID() + "'");
		require(geometry.getGeometryType().equals(Geometry.TYPENAME_LINESTRING), "Geometrietyp nicht zulässig");

		this.id = id;
		this.geometry = (LineString) geometry;
	}
}
