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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import lombok.Getter;
import lombok.Setter;

public class KanteMapView extends AbstractEntity {

	@Getter
	@Setter
	private LineString geometrie;

	@Setter
	private LineString verlaufLinks;

	@Setter
	private LineString verlaufRechts;

	@Getter
	private final boolean istStrecke;

	private static final GeometryPrecisionReducer GEOMETRY_PRECISION_REDUCER = new GeometryPrecisionReducer(
		new PrecisionModel(100));

	static {
		GEOMETRY_PRECISION_REDUCER.setChangePrecisionModel(true);
	}

	@Getter
	private boolean isZweiseitig;

	public KanteMapView(Long id, Geometry geometrie, boolean isZweiseitig, boolean istStrecke) {
		require(id, notNullValue());
		require(geometrie, notNullValue());
		require(geometrie.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
				+ "' entspricht nicht geometry.getSRID() '" + geometrie.getSRID() + "'");
		require(geometrie.getGeometryType().equals(Geometry.TYPENAME_LINESTRING), "Geometrietyp nicht zulässig");

		this.id = id;
		this.geometrie = (LineString) GEOMETRY_PRECISION_REDUCER.reduce(geometrie);
		this.isZweiseitig = isZweiseitig;
		this.istStrecke = istStrecke;
	}

	public KanteMapView(Long id, Geometry geometrie, Geometry verlaufLinks, Geometry verlaufRechts,
		boolean isZweiseitig) {
		require(id, notNullValue());
		require(geometrie, notNullValue());
		require(geometrie.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
				+ "' entspricht nicht geometry.getSRID() '" + geometrie.getSRID() + "'");
		require(geometrie.getGeometryType().equals(Geometry.TYPENAME_LINESTRING), "Geometrietyp nicht zulässig");
		this.istStrecke = false;

		if (verlaufLinks != null) {
			require(verlaufLinks.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
				"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
					+ "' entspricht nicht geometry.getSRID() '" + verlaufLinks.getSRID() + "'");
			require(verlaufLinks.getGeometryType().equals(Geometry.TYPENAME_LINESTRING), "Geometrietyp nicht zulässig");
			this.verlaufLinks = (LineString) GEOMETRY_PRECISION_REDUCER.reduce(verlaufLinks);
		}

		if (verlaufRechts != null) {
			require(verlaufRechts.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
				"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
					+ "' entspricht nicht geometry.getSRID() '" + verlaufRechts.getSRID() + "'");
			require(verlaufRechts.getGeometryType().equals(Geometry.TYPENAME_LINESTRING),
				"Geometrietyp nicht zulässig");
			this.verlaufRechts = (LineString) GEOMETRY_PRECISION_REDUCER.reduce(verlaufRechts);
		}

		this.id = id;
		this.geometrie = (LineString) GEOMETRY_PRECISION_REDUCER.reduce(geometrie);
		this.isZweiseitig = isZweiseitig;
	}

	public Optional<LineString> getVerlaufLinks() {
		return Optional.ofNullable(verlaufLinks);
	}

	public Optional<LineString> getVerlaufRechts() {
		return Optional.ofNullable(verlaufRechts);
	}
}
