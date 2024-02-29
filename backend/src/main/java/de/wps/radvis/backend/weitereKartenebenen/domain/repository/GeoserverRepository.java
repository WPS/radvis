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

package de.wps.radvis.backend.weitereKartenebenen.domain.repository;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.DateiLayerFormat;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverDatastoreName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverLayerName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverStyleName;

public interface GeoserverRepository {

	GeoserverLayerName createDataStoreAndLayer(GeoserverDatastoreName datastoreName,
		DateiLayerFormat dateiLayerFormat, MultipartFile file) throws IOException, InterruptedException;

	GeoserverLayerName getLayerNameFromDatastore(GeoserverDatastoreName datastoreName)
		throws IOException, InterruptedException;

	void removeDatastoreAndLayer(GeoserverDatastoreName datastoreName) throws IOException, InterruptedException;

	void addStyleToLayer(GeoserverLayerName geoserverLayerName, GeoserverStyleName geoserverStyleName,
		boolean makeDefault)
		throws IOException, InterruptedException;

	GeoserverStyleName createStyle(GeoserverStyleName geoserverStyleName, MultipartFile sldFile)
		throws IOException, InterruptedException;

	void deleteStyle(GeoserverStyleName geoserverStyleName) throws IOException, InterruptedException;

	void validateStyleForLayer(GeoserverLayerName geoserverLayerName, GeoserverStyleName geoserverStyleName)
		throws IOException, InterruptedException;
}
