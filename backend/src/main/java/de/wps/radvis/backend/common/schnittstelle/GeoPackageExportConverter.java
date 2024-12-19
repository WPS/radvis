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

package de.wps.radvis.backend.common.schnittstelle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Geometry;
import org.valid4j.Assertive;

import de.wps.radvis.backend.common.domain.SimpleFeatureTypeFactory;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeoPackageExportConverter implements ExportConverter {

	@Override
	public byte[] convert(List<ExportData> data) {
		if (data.isEmpty()) {
			return new byte[0];
		}

		File exportGeoPkgFile = null;
		byte[] result;
		try {
			exportGeoPkgFile = convertToFile(data);
			result = FileUtils.readFileToByteArray(exportGeoPkgFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (exportGeoPkgFile != null) {
				exportGeoPkgFile.delete();
			}
		}
		return result;
	}

	public File convertToFile(List<ExportData> data) throws IOException {
		Assertive.require(!data.isEmpty(), "GeoPackage Export-Daten dürfen nicht leer sein");

		SimpleFeatureType simpleFeatureType = SimpleFeatureTypeFactory.createSimpleFeatureType(
			data.get(0).getProperties(),
			Geometry.class,
			SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_THE_GEOM);
		ListFeatureCollection collection = getListFeatureCollection(data, simpleFeatureType);

		File exportGeoPkgFile = Files.createTempFile("export", "gpkg").toFile();
		exportGeoPkgFile.deleteOnExit();
		GeoPackage geopkg = new GeoPackage(exportGeoPkgFile);
		geopkg.init();
		FeatureEntry entry = new FeatureEntry();
		geopkg.add(entry, collection);
		geopkg.addCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		geopkg.close();

		return exportGeoPkgFile;
	}

	@Override
	public String getDateinamenSuffix() {
		return "_geopackage_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".gpkg";
	}

	@NotNull
	private static ListFeatureCollection getListFeatureCollection(List<ExportData> data,
		SimpleFeatureType simpleFeatureType) {
		ListFeatureCollection collection = new ListFeatureCollection(simpleFeatureType);

		for (int i = 0; i < data.size(); i++) {
			ExportData singleData = data.get(i);
			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureType);
			simpleFeatureBuilder.set(SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_THE_GEOM,
				singleData.getGeometry());
			singleData.getProperties().forEach(simpleFeatureBuilder::set);
			collection.add(simpleFeatureBuilder.buildFeature(String.valueOf(i)));
		}
		return collection;
	}
}
