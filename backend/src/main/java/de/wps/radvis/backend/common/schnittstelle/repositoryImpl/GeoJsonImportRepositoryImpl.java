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

package de.wps.radvis.backend.common.schnittstelle.repositoryImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.geojson.Crs;
import org.geojson.GeoJsonObject;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.SchemaException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeoJsonImportRepositoryImpl implements GeoJsonImportRepository {

	private final CoordinateReferenceSystemConverter converter;

	public GeoJsonImportRepositoryImpl(CoordinateReferenceSystemConverter converter) {
		this.converter = converter;
	}

	@Override
	public Stream<SimpleFeature> readFeaturesFromGeojsonFile(MultipartFile file) throws ReadGeoJSONException {
		try {
			return readFeaturesFromByteArray(file.getBytes());
		} catch (IOException e) {
			throw new ReadGeoJSONException("Die hochgeladene Datei kann nicht ausgelesen werden.", e);
		}
	}

	@Override
	public Stream<SimpleFeature> readFeaturesFromByteArray(byte[] bytes) throws ReadGeoJSONException {
		String jsonString = new String(bytes);
		// An dieser Stelle erstmal "nur" nach json validieren.
		// Wenn es nicht dem GeoJSON Standart entspricht fliegen wir spaeter beim konvertieren raus
		if (!validateJSON(jsonString)) {
			throw new ReadGeoJSONException("Die hochgeladene Datei enthält kein gültiges JSON.");
		}

		return getSimpleFeatures(jsonString).stream();
	}

	@Override
	public String getFileContentAsString(URL url) throws IOException {
		StringBuilder content = new StringBuilder();
		String geoJsonString;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line);
			}
			geoJsonString = content.toString();
		}
		return geoJsonString;
	}

	@Override
	public boolean validateJSON(String jsonString) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.readTree(jsonString);
		} catch (JacksonException e) {
			return false;
		}
		return true;
	}

	@Override
	public List<SimpleFeature> getSimpleFeatures(String geoJsonString) throws ReadGeoJSONException {
		return getSimpleFeatures(geoJsonString, null);
	}

	@Override
	public List<SimpleFeature> getSimpleFeatures(String geoJsonString, SimpleFeatureType type)
		throws ReadGeoJSONException {
		SimpleFeatureCollection simpleFeatureCollection;

		// Die Methode parseFeatureCollection wirft leider keine guten Fachlichen Exceptions, sondern irgendwelchen
		// runtime Kram, wenn etwas nicht einlesbar ist. Deshalb sollte hier alles was auftreten kann abgefangen werden.
		try (GeoJSONReader geoJSONReader = new GeoJSONReader(geoJsonString)) {
			if (!Objects.isNull(type)) {
				geoJSONReader.setSchema(type);
			}
			simpleFeatureCollection = geoJSONReader.getFeatures();
			// Wir müssen hier einen AssertionError in GeoTools catchen können, daher kann hier Throwable vorerst nicht
			// nicht durch Exception ersetzt werden.
		} catch (Throwable e) {
			throw new ReadGeoJSONException(e);
		}

		// GeoJSONReader.parseFeatureCollection kann nicht wirklich das KoordinatenReferenzSystem aus einer
		// GeoJSON Datei auslesen, deshalb:
		// Versuchen aus dem geoJSON String mit einer anderen Bibliothek das Koordinatensystem zu bestimmten.
		// Falls keins angegeben ist, oder das nicht erfolgreich ist, wird von dem CRS ausgegangen, was an dem
		// SimpleFeature durch die Methode GeoJSONReader.parseFeatureCollection angegeben wurde.
		// Dies ist immer(?) WGS84 und damit wird es im Folgenden einfach mal probiert, da der GeoJSON Standart
		// Die extra angabe eines CRS sowieso nicht vorsieht und immer von WGS84 ausgeht.
		Optional<KoordinatenReferenzSystem> crsOfGeoJSON = getCRSOfGeoJSON(geoJsonString);

		SimpleFeatureIterator featureIterator = simpleFeatureCollection.features();
		List<SimpleFeature> simpleFeatures = new ArrayList<>();
		while (featureIterator.hasNext()) {
			SimpleFeature feature = featureIterator.next();

			// Features ohne Geometrien werden ignoriert
			if (feature.getDefaultGeometry() == null) {
				continue;
			}

			SimpleFeature featureInUtm32;
			try {
				if (crsOfGeoJSON.isPresent() && crsOfGeoJSON.get().equals(KoordinatenReferenzSystem.ETRS89_UTM32_N)) {
					// UTM32 wird nicht richtig durch GeoJSONReader.parseFeatureCollection erkannt. Es ist aber schon
					// das richtige zielkoordinatensystem -> es muss nur noch am FeatureType das CRS richtig gesetzt werden
					// da die entsprechende Methode das nicht machen konnte.
					featureInUtm32 = converter.changeFeatureTypeCrsFromFeature(feature,
						KoordinatenReferenzSystem.ETRS89_UTM32_N);
				} else if (feature.getFeatureType().getCoordinateReferenceSystem() != null) {
					// Bei anderen Koordinatensystemen insb. WGS84 muss auch noch transformiert werden
					featureInUtm32 = converter.transformFeature(feature, KoordinatenReferenzSystem.ETRS89_UTM32_N);
				} else {
					throw new ReadGeoJSONException(
						"Es konnte kein Koordinaten Referenz System an den Features ermittelt werden.");
				}

			} catch (FactoryException | SchemaException | TransformException e) {
				throw new ReadGeoJSONException(
					"Die Geometrien konnten nicht in UTM32 transformiert werden.", e);
			}
			simpleFeatures.add(featureInUtm32);
		}
		featureIterator.close();
		return simpleFeatures;
	}

	private Optional<KoordinatenReferenzSystem> getCRSOfGeoJSON(String geoJSON) {
		try {
			GeoJsonObject geoJsonObject = (new ObjectMapper()).readValue(geoJSON, GeoJsonObject.class);
			Crs geoJsonObjectCrs = geoJsonObject.getCrs();
			if (geoJsonObjectCrs == null) {
				return Optional.empty();
			}
			return Optional.of(KoordinatenReferenzSystem.of(geoJsonObjectCrs));
		} catch (IOException | FactoryException e) {
			return Optional.empty();
		}
	}
}
