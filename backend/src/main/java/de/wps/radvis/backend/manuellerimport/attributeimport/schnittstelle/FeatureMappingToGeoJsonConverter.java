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

package de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle;

import java.util.List;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;

public class FeatureMappingToGeoJsonConverter {
	public FeatureCollection convert(List<FeatureMapping> featureMappings) {
		FeatureCollection featureCollection = GeoJsonConverter.createFeatureCollection();
		featureMappings.forEach(
			featureMapping -> featureCollection.add(this.convertFeatureMappingToFeature(featureMapping)));
		return featureCollection;
	}

	public Feature convertFeatureMappingToFeature(FeatureMapping featureMapping) {
		LineString importedLineString = featureMapping.getImportedLineString();
		Feature feature = GeoJsonConverter.createFeature(
			CoordinateReferenceSystemConverterUtility.transformGeometry(importedLineString,
				KoordinatenReferenzSystem.ETRS89_UTM32_N));
		feature.setProperty("mappedGrundnetzkanten", featureMapping.getKantenAufDieGemappedWurde());
		feature.setId(Long.toString(featureMapping.getId()));
		return feature;
	}
}
