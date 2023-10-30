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
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;

public class ImportedFeatureBuilder {

	private String fachId;
	private Geometry geometrie;
	private Map<String, Object> attribute;
	private LocalDateTime importDatum;
	private QuellSystem quelle;
	private Art art;

	private final GeometryFactory geometryFactory;

	private ImportedFeatureBuilder() {
		this.attribute = new HashMap<>();
		this.geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
	}

	public static ImportedFeatureBuilder empty() {
		return new ImportedFeatureBuilder();
	}

	public ImportedFeatureBuilder fachId(String fachId) {
		this.fachId = fachId;
		return this;
	}

	public ImportedFeatureBuilder lineString(Coordinate... coordinates) {
		require(coordinates.length > 1, "Mindestens 2 Koordinatenpaare benötigt. 1 für Start und 1 für Endpunkt");
		this.geometrie = geometryFactory.createLineString(coordinates);
		return this;
	}

	public ImportedFeatureBuilder point(Coordinate coordinate) {
		this.geometrie = geometryFactory.createPoint(coordinate);
		return this;
	}

	public ImportedFeatureBuilder geometry(Geometry geometry) {
		require(geometry, notNullValue());
		require(geometry.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
				+ "' entspricht nicht geometry.getSRID() '" + geometry.getSRID() + "'");

		this.geometrie = geometry;
		return this;
	}

	public ImportedFeatureBuilder attribute(Map<String, Object> attribute) {
		this.attribute = attribute;
		return this;
	}

	public ImportedFeatureBuilder addAttribut(String key, Object value) {
		attribute.put(key, value);
		return this;
	}

	public ImportedFeatureBuilder importDatum(LocalDateTime importDatum) {
		this.importDatum = importDatum;
		return this;
	}

	public ImportedFeatureBuilder quelle(QuellSystem quelle) {
		this.quelle = quelle;
		return this;
	}

	public ImportedFeatureBuilder art(Art art) {
		this.art = art;
		return this;
	}

	public ImportedFeature build() {
		return new ImportedFeature(fachId, geometrie, attribute, importDatum, quelle, art);
	}

}
