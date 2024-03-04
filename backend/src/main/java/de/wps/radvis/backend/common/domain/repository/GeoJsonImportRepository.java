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

package de.wps.radvis.backend.common.domain.repository;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;

public interface GeoJsonImportRepository {
	boolean validateJSON(String geoJsonString);

	Stream<SimpleFeature> readFeaturesFromGeojsonFile(MultipartFile file) throws ReadGeoJSONException;

	Stream<SimpleFeature> readFeaturesFromByteArray(byte[] bytes) throws ReadGeoJSONException;

	String getFileContentAsString(URL url) throws IOException;

	List<SimpleFeature> getSimpleFeatures(String geoJsonString) throws ReadGeoJSONException;

	List<SimpleFeature> getSimpleFeatures(String geoJsonString, SimpleFeatureType type) throws ReadGeoJSONException;
}
