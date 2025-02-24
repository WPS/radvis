/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.weitereKartenebenen.schnittstelle.repositoryImpl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.CoordinateXY;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.WeitereKartenebenenConfigurationProperties;

class GeoserverRepositoryImplTest {

	@Mock
	private WeitereKartenebenenConfigurationProperties weitereKartenebenenConfigurationProperties;
	@Mock
	private GeoJsonImportRepository geoJsonImportRepository;

	private GeoserverRepositoryImpl geoserverRepository;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		geoserverRepository = new GeoserverRepositoryImpl(weitereKartenebenenConfigurationProperties,
			geoJsonImportRepository);
	}

	@Test
	void geoJsonToGeoPackage_keepsSimpleFeatureTypeForSimpleGeometries() throws IOException, ReadGeoJSONException {
		MultipartFile multipartFile = mock(MultipartFile.class);

		List<SimpleFeature> inputFeatures = new ArrayList<>();
		inputFeatures.add(SimpleFeatureTestDataProvider.withLineString(new CoordinateXY(0, 1), new CoordinateXY(2, 3)));
		inputFeatures.add(SimpleFeatureTestDataProvider.withLineString(new CoordinateXY(4, 5), new CoordinateXY(6, 7)));

		when(geoJsonImportRepository.readFeaturesFromGeojsonFile(multipartFile)).thenReturn(inputFeatures.stream());

		// Act
		byte[] geoPackageData = geoserverRepository.geoJsonToGeoPackage(multipartFile);

		// Assert
		List<SimpleFeature> features = readAllFeaturesFromGeoPackageData(geoPackageData);

		Assertions.assertThat(features).hasSize(2);
		Assertions.assertThat(features.get(0).getDefaultGeometry().getClass().getSimpleName()).isEqualTo("LineString");
		Assertions.assertThat(features.get(1).getDefaultGeometry().getClass().getSimpleName()).isEqualTo("LineString");
	}

	@Test
	void geoJsonToGeoPackage_detectsMultiGeometries() throws IOException, ReadGeoJSONException {
		MultipartFile multipartFile = mock(MultipartFile.class);

		List<SimpleFeature> inputFeatures = new ArrayList<>();
		inputFeatures.add(SimpleFeatureTestDataProvider.withLineString(new CoordinateXY(0, 1), new CoordinateXY(2, 3)));
		inputFeatures.add(SimpleFeatureTestDataProvider.withMultiLineStringAndAttributes(Map.of(), new CoordinateXY(4,
			5), new CoordinateXY(6, 7)));

		when(geoJsonImportRepository.readFeaturesFromGeojsonFile(multipartFile)).thenReturn(inputFeatures.stream());

		// Act
		byte[] geoPackageData = geoserverRepository.geoJsonToGeoPackage(multipartFile);

		// Assert
		List<SimpleFeature> features = readAllFeaturesFromGeoPackageData(geoPackageData);

		Assertions.assertThat(features).hasSize(2);
		Assertions.assertThat(features.get(0).getDefaultGeometry().getClass().getSimpleName()).isEqualTo(
			"MultiLineString");
		Assertions.assertThat(features.get(1).getDefaultGeometry().getClass().getSimpleName()).isEqualTo(
			"MultiLineString");
	}

	public static List<SimpleFeature> readAllFeaturesFromGeoPackageData(byte[] geopackageData) throws IOException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(geopackageData);

		File tempFile = File.createTempFile("geopackage", ".gpkg");
		tempFile.deleteOnExit();  // Ensure file is deleted after execution
		Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

		List<SimpleFeature> features = new ArrayList<>();
		try (GeoPackage geoPackage = new GeoPackage(tempFile)) {
			geoPackage.init();

			for (FeatureEntry entry : geoPackage.features()) {
				try (SimpleFeatureReader featureIterator = geoPackage.reader(entry, null, null)) {
					while (featureIterator.hasNext()) {
						features.add(featureIterator.next());
					}
				}
			}
		}

		return features;
	}
}