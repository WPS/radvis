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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.repository;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

public interface ShapeFileAttributeRepository {

	/**
	 * Liest die Attributnamen (keys) aus einer shp-File aus.
	 *
	 * @param shpFile
	 * 	Hier kann eine bliebige Datei, die zur SHP gehört, übergeben werden. (Insbesondere die .dbf-File)
	 * @throws IOException
	 */
	Set<String> getAttributnamen(File shpFile) throws IOException;

	/**
	 * Liest die Werte (values) für einen Attributnamen (key) aus einer shp-File aus.
	 * <p>
	 * Unbedingt Stream.close() aufrufen!
	 *
	 * @param shpFile
	 * 	Hier kann eine bliebige Datei, die zur SHP gehört, übergeben werden. (Insbesondere die .dbf-File)
	 * @throws IOException
	 */
	Stream<String> getAttributWerte(File shpFile, String attributName) throws IOException;

}
