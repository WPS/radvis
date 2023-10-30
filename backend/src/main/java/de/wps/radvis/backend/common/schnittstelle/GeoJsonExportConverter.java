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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.geojson.Crs;
import org.geojson.Feature;
import org.geojson.FeatureCollection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;

public class GeoJsonExportConverter implements ExportConverter {

	@Override
	public byte[] convert(List<ExportData> data) {
		FeatureCollection collection = new FeatureCollection();
		Crs crs = new Crs();
		crs.getProperties().put("name", "EPSG:" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		collection.setCrs(crs);

		data.forEach(singleData -> {
			Feature feature = GeoJsonConverter.createFeature(singleData.getGeometry());
			singleData.getProperties().forEach(feature::setProperty);
			collection.add(feature);
		});
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsBytes(collection);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public String getDateinamenSuffix() {
		return "_geojson_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".geojson";
	}

}
