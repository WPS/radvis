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

package de.wps.radvis.backend.quellimport.common.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeatureMapView;

class ImportedFeatureToGeoJsonConverterTest {

	private ImportedFeatureToGeoJsonConverter featureConverterService;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		featureConverterService = new ImportedFeatureToGeoJsonConverter();
	}

	@Test
	public void parsePoint() {
		// Arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzObject()
			.point(new Coordinate(25.25, 876.11))
			.build();
		ImportedFeatureMapView featureMapView = new ImportedFeatureMapView(1L, feature.getGeometrie(), null);

		// Act
		FeatureCollection result = featureConverterService
			.convertImportedFeaturesToFeatureCollection(Arrays.asList(featureMapView));

		// Assert
		assertThat(result.getFeatures().size()).isEqualTo(1);
		assertThat(result.getFeatures().get(0).getGeometry()).isExactlyInstanceOf(org.geojson.Point.class);
		Point point = (Point) result.getFeatures().get(0).getGeometry();
		assertThat(point.getCoordinates().getLongitude()).isEqualTo(25.25);
		assertThat(point.getCoordinates().getLatitude()).isEqualTo(876.11);
		assertThat(point.getCoordinates().hasAltitude()).isFalse();
	}

	@Test
	public void parseLineString() {
		// Arrange
		ImportedFeature feature = ImportedFeatureTestDataProvider.defaultRadNetzObject()
			.lineString(new Coordinate(25.25, 876.11), new Coordinate(1, 2), new Coordinate(3, 4))
			.build();
		ImportedFeatureMapView featureMapView = new ImportedFeatureMapView(1L, feature.getGeometrie(), null);

		// Act
		FeatureCollection result = featureConverterService
			.convertImportedFeaturesToFeatureCollection(Arrays.asList(featureMapView));

		// Assert
		assertThat(result.getFeatures().size()).isEqualTo(1);
		assertThat(result.getFeatures().get(0).getGeometry()).isExactlyInstanceOf(org.geojson.LineString.class);
		LineString lineString = (LineString) result.getFeatures().get(0).getGeometry();
		assertThat(lineString.getCoordinates().size()).isEqualTo(3);
		assertThat(lineString.getCoordinates().get(0).getLongitude()).isEqualTo(25.25);
		assertThat(lineString.getCoordinates().get(0).getLatitude()).isEqualTo(876.11);
		assertThat(lineString.getCoordinates().get(0).hasAltitude()).isFalse();
		assertThat(lineString.getCoordinates().get(1).getLongitude()).isEqualTo(1);
		assertThat(lineString.getCoordinates().get(1).getLatitude()).isEqualTo(2);
		assertThat(lineString.getCoordinates().get(1).hasAltitude()).isFalse();
		assertThat(lineString.getCoordinates().get(2).getLongitude()).isEqualTo(3);
		assertThat(lineString.getCoordinates().get(2).getLatitude()).isEqualTo(4);
		assertThat(lineString.getCoordinates().get(2).hasAltitude()).isFalse();
	}
}
