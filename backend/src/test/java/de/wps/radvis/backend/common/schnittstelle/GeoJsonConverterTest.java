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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.geojson.Feature;
import org.geojson.LineString;
import org.geojson.Point;
import org.geojson.Polygon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;

public class GeoJsonConverterTest {

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void parsePoint() {
		// Arrange
		ImportedFeature radvisFeature = ImportedFeatureTestDataProvider.defaultRadNetzObject()
			.point(new Coordinate(25.25, 876.11))
			.build();

		// Act
		Feature result = GeoJsonConverter.createFeature(radvisFeature.getGeometrie());

		// Assert
		assertThat(result.getGeometry()).isExactlyInstanceOf(org.geojson.Point.class);
		Point point = (Point) result.getGeometry();
		assertThat(point.getCoordinates().getLongitude()).isEqualTo(25.25);
		assertThat(point.getCoordinates().getLatitude()).isEqualTo(876.11);
		assertThat(point.getCoordinates().hasAltitude()).isFalse();
	}

	@Test
	public void parseLineString() {
		// Arrange
		ImportedFeature radvisFeature = ImportedFeatureTestDataProvider.defaultRadNetzObject()
			.lineString(new Coordinate(25.25, 876.11), new Coordinate(1, 2), new Coordinate(3, 4)).build();

		// Act
		Feature result = GeoJsonConverter.createFeature(radvisFeature.getGeometrie());

		// Assert
		assertThat(result.getGeometry()).isExactlyInstanceOf(org.geojson.LineString.class);
		LineString lineString = (LineString) result.getGeometry();
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

	@Test
	public void parsePolygon() {
		// Arrange
		org.locationtech.jts.geom.Polygon polygon = getGeometryFactory()
			.createPolygon(new Coordinate[] { new Coordinate(25.25, 876.11), new Coordinate(1, 2), new Coordinate(3, 4),
				new Coordinate(25.25, 876.11) });

		// Act
		Feature result = GeoJsonConverter.createFeature(polygon);

		// Assert
		assertThat(result.getGeometry()).isExactlyInstanceOf(org.geojson.Polygon.class);
		Polygon resultPolygon = (Polygon) result.getGeometry();
		assertThat(resultPolygon.getExteriorRing().size()).isEqualTo(4);
		assertThat(resultPolygon.getExteriorRing().get(0).getLongitude()).isEqualTo(25.25);
		assertThat(resultPolygon.getExteriorRing().get(0).getLatitude()).isEqualTo(876.11);
		assertThat(resultPolygon.getExteriorRing().get(0).hasAltitude()).isFalse();
		assertThat(resultPolygon.getExteriorRing().get(1).getLongitude()).isEqualTo(1);
		assertThat(resultPolygon.getExteriorRing().get(1).getLatitude()).isEqualTo(2);
		assertThat(resultPolygon.getExteriorRing().get(1).hasAltitude()).isFalse();
		assertThat(resultPolygon.getExteriorRing().get(2).getLongitude()).isEqualTo(3);
		assertThat(resultPolygon.getExteriorRing().get(2).getLatitude()).isEqualTo(4);
		assertThat(resultPolygon.getExteriorRing().get(2).hasAltitude()).isFalse();
		assertThat(resultPolygon.getExteriorRing().get(3).getLongitude()).isEqualTo(25.25);
		assertThat(resultPolygon.getExteriorRing().get(3).getLatitude()).isEqualTo(876.11);
		assertThat(resultPolygon.getExteriorRing().get(3).hasAltitude()).isFalse();
	}

	@Test
	public void polygonMitLochNotSupported() {
		// Arrange
		GeometryFactory geometryFactory = getGeometryFactory();
		org.locationtech.jts.geom.Polygon polygon = geometryFactory.createPolygon(
			// Äußeres Polygon (Shell)
			geometryFactory.createLinearRing(new Coordinate[] { new Coordinate(1, 1),
				new Coordinate(4, 1), new Coordinate(4, 4), new Coordinate(1, 4), new Coordinate(1, 1) }),
			// Inneres Polygon (Loch, Hole)
			new LinearRing[] {
				geometryFactory.createLinearRing(new Coordinate[] { new Coordinate(2, 2),
					new Coordinate(3, 2), new Coordinate(3, 3), new Coordinate(2, 3),
					new Coordinate(2, 2) }) });

		// Act
		assertThrows(RuntimeException.class, () -> {
			GeoJsonConverter.createFeature(polygon);
		});
	}

	private GeometryFactory getGeometryFactory() {
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
	}
}
