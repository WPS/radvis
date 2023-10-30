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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.provider;

import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.Haendigkeit;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedFeature;

public class MappedFeatureTestDataProvider {

	public static MappedFeature.MappedFeatureBuilder withLineStringAndProperties(LineString lineString,
		Map<String, Object> properties) {
		return MappedFeature.builder()
			.lineString(lineString)
			.properties(properties)
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.haendigkeit(Haendigkeit.of(.0));
	}

	public static MappedFeature.MappedFeatureBuilder withLSAndLR(LineString lineString,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return MappedFeature.builder()
			.lineString(lineString)
			.properties(Map.of())
			.linearReferenzierterAbschnitt(linearReferenzierterAbschnitt)
			.haendigkeit(Haendigkeit.of(.0));
	}

	public static MappedFeature.MappedFeatureBuilder withLineString(LineString lineString) {
		return withLineStringAndProperties(lineString, new HashMap<>());
	}

	public static MappedFeature.MappedFeatureBuilder withProperties(Map<String, Object> properties) {
		return withLineStringAndProperties(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)),
			properties);
	}

	public static MappedFeature.MappedFeatureBuilder withDefaultValues() {
		return withLineString(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)));
	}
}
