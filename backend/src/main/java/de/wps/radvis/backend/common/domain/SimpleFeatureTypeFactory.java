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

package de.wps.radvis.backend.common.domain;

import java.util.Map;
import java.util.Set;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;

public class SimpleFeatureTypeFactory {
	public static final String GEOMETRY_ATTRIBUTE_KEY_GEOMETRY = "geometry";
	public static final String GEOMETRY_ATTRIBUTE_KEY_THE_GEOM = "the_geom";

	public static SimpleFeatureType createSimpleFeatureType(Map<String, String> properties,
		Class<? extends Geometry> clazz, String geometryAttributeKey) {
		return createSimpleFeatureType(properties.keySet(), clazz, geometryAttributeKey);
	}

	public static SimpleFeatureType createSimpleFeatureType(Map<String, String> properties,
		Class<? extends Geometry> clazz, String geometryAttributeKey, String name) {
		return createSimpleFeatureType(properties.keySet(), clazz, geometryAttributeKey, name);
	}

	public static SimpleFeatureType createSimpleFeatureType(Set<String> keys, Class<? extends Geometry> clazz,
		String geometryAttributeKey) {
		return createSimpleFeatureType(keys, clazz, geometryAttributeKey,
			"RadVis" + clazz.getSimpleName() + "FeatureType");
	}

	public static SimpleFeatureType createSimpleFeatureType(Set<String> keys, Class<? extends Geometry> clazz,
		String geometryAttributeKey, String name) {
		SimpleFeatureTypeBuilder simpleFeatureTypeBuilderLineStrings = new SimpleFeatureTypeBuilder();
		simpleFeatureTypeBuilderLineStrings.setName(name);
		simpleFeatureTypeBuilderLineStrings.setCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		simpleFeatureTypeBuilderLineStrings.add(geometryAttributeKey, clazz);
		keys.forEach(key -> simpleFeatureTypeBuilderLineStrings.add(key, String.class));
		return simpleFeatureTypeBuilderLineStrings.buildFeatureType();
	}
}
