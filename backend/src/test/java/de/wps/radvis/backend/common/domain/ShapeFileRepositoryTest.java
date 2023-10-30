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

package de.wps.radvis.backend.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.core.io.ClassPathResource;

import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.ShapeFileRepositoryImpl;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeEncodingException;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeUnreadableException;

public class ShapeFileRepositoryTest {
	private SimpleFeatureType TYPE;
	private ShapeFileRepository shapeFileRepository;

	private static final String TEST_SHP_UTF7 = "./shp/test_shpfile_UTF-7";
	private static final String TEST_SHP_UTF8 = "./shp/test_attribute";
	private static final String TEST_SHP_KAPUTT = "./shp/test_shape_kaputt";
	private static final String TEST_SHP_EPSG4326 = "./shp/test_shape_linien";

	@Mock
	CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	@BeforeEach
	public void setup() throws SchemaException {
		MockitoAnnotations.openMocks(this);
		TYPE = DataUtilities.createType(
			"Location",
			"the_geom:LineString:srid=25832,"
				+ "name:String,"
				+ "number:Integer");
		shapeFileRepository = new ShapeFileRepositoryImpl(coordinateReferenceSystemConverter);
	}

	@Test
	public void writeAndRead() throws IOException, ShapeProjectionException {
		// arrange
		List<SimpleFeature> features = new ArrayList<>();

		Coordinate[] coordinates = new Coordinate[3];
		coordinates[0] = new Coordinate(0, 0);
		coordinates[1] = new Coordinate(0, 10);
		coordinates[2] = new Coordinate(10, 15);

		LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(coordinates);

		for (int i = 1; i < 4; i++) {
			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
			featureBuilder.add(lineString);
			featureBuilder.add("Testname" + i);
			featureBuilder.add(i);
			SimpleFeature feature = featureBuilder.buildFeature(null);
			features.add(feature);
		}
		File shpDirectory = Files.createTempDirectory("shape_repo_test").toFile();
		File shpFile = new File(shpDirectory, "shape_repo_test.shp");

		// act
		shapeFileRepository.writeShape(shpDirectory, shpFile, features);
		Stream<SimpleFeature> stream = shapeFileRepository.readShape(shpFile);
		List<SimpleFeature> result = stream.collect(Collectors.toList());
		stream.close();

		assertThat(result).hasSize(3);
		assertThat(result).extracting(SimpleFeature::getDefaultGeometry)
			.allMatch(geom -> (((MultiLineString) geom).getGeometryN(0)).equalsExact(lineString));

		assertThat(result).extracting(f -> (Geometry) f.getDefaultGeometry()).extracting(Geometry::getSRID)
			.allMatch(srid -> srid == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(result).extracting(f -> ((MultiLineString) f.getDefaultGeometry()).getGeometryN(0).getSRID())
			.allMatch(srid -> srid == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(result).extracting(f -> ((Geometry) f.getDefaultGeometry()).getCentroid().getSRID())
			.allMatch(srid -> srid == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		assertThat(result).extracting(f -> f.getProperty("number").getValue()).containsExactlyInAnyOrder(1, 2, 3);
		assertThat(result).extracting(f -> f.getProperty("name").getValue()).containsExactlyInAnyOrder("Testname" + 1,
			"Testname" + 2, "Testname" + 3);
		assertThat(new File(shpDirectory, "shape_repo_test" + ".cpg").exists()).isTrue();
	}

	@Test
	void testReadShape_defaultgeometryOfFeatureIsNull_noException_SRIDnotSet()
		throws IOException {
		// arrange
		List<SimpleFeature> features = new ArrayList<>();
		for (int i = 1; i < 4; i++) {
			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
			featureBuilder.add("Testname" + i);
			featureBuilder.add(i);
			SimpleFeature feature = featureBuilder.buildFeature(null);
			features.add(feature);
		}

		File shpDirectory = Files.createTempDirectory("shape_repo_test").toFile();
		File shpFile = new File(shpDirectory, "shape_repo_test.shp");

		shapeFileRepository.writeShape(shpDirectory, shpFile, features);

		// act + assert
		List<SimpleFeature> result = new ArrayList<>();
		assertDoesNotThrow(() -> {
			Stream<SimpleFeature> stream = shapeFileRepository.readShape(shpFile);
			result.addAll(stream.collect(Collectors.toList()));
			stream.close();
		});
		assertThat(result).hasSize(3);

	}

	@Test
	void testValidate_shpKorrekt_wirftKeineException() throws IOException {
		File shpFile = new ClassPathResource(TEST_SHP_UTF8).getFile();

		// act
		assertThatNoException().isThrownBy(() -> shapeFileRepository.validate(shpFile));
	}

	@Test
	void testValidate_shpInUTF7_wirftKorrekteException() throws IOException {
		File shpFile = new ClassPathResource(TEST_SHP_UTF7).getFile();

		// act
		assertThatThrownBy(() -> shapeFileRepository.validate(shpFile))
			.isInstanceOf(ShapeEncodingException.class)
			.hasMessage("Die ShapeFile muss in UTF-8 vorliegen. Das angebene Encoding 'UTF-7' ist ungültig.");
	}

	@Test
	void testValidate_shpNichtEinlesbar_wirftKorrekteException() throws IOException {
		File shpFile = new ClassPathResource(TEST_SHP_KAPUTT).getFile();

		// act
		assertThatThrownBy(() -> shapeFileRepository.validate(shpFile))
			.isInstanceOf(ShapeUnreadableException.class);
	}

	@Test
	void testValidate_shpInFalscherProjektion_wirftKorrekteException() throws IOException {
		File shpFile = new ClassPathResource(TEST_SHP_EPSG4326).getFile();

		// act
		assertThatThrownBy(() -> shapeFileRepository.validate(shpFile))
			.isInstanceOf(ShapeProjectionException.class)
			.hasMessage(
				"Das KoordinatenReferenzSystem der Shape muss UTM-32 (EPSG:25832) sein. Angegebenes KoordinatenReferenzSystem: EPSG:4326");
	}

	@Test
	void testeTransformGeometryToUTM32() {
		// arrange
		SimpleFeatureBuilder TDPsimpleFeatureBuilder = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(MultiLineString.class).buildFeatureType());

		MultiLineString multiLineStringSingle = KoordinatenReferenzSystem.WGS84.getGeometryFactory()
			.createMultiLineString(new LineString[] { KoordinatenReferenzSystem.WGS84.getGeometryFactory()
				.createLineString(new Coordinate[] { new Coordinate(0, 1), new Coordinate(1, 1) }) });
		TDPsimpleFeatureBuilder.add(multiLineStringSingle);
		String keepId = "keep";
		SimpleFeature feature = TDPsimpleFeatureBuilder.buildFeature(keepId);

		MultiLineString transformed = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createMultiLineString(new LineString[] { KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createLineString(new Coordinate[] { new Coordinate(0, 1), new Coordinate(1, 1) }) });
		when(coordinateReferenceSystemConverter.transformGeometry(any(), any())).thenReturn(
			transformed);

		// act
		SimpleFeature result = shapeFileRepository.transformGeometryToUTM32(feature);

		// assert
		assertThat(((Geometry) result.getDefaultGeometry()).getSRID())
			.isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(((Geometry) result.getDefaultGeometry()).getCentroid().getSRID())
			.isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(((Geometry) result.getDefaultGeometry()).getGeometryType()).isEqualTo(
			MultiLineString.TYPENAME_MULTILINESTRING);
		assertThat(((MultiLineString) feature.getDefaultGeometry()).getGeometryN(0).getSRID()).isEqualTo(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(((MultiLineString) feature.getDefaultGeometry()).getGeometryN(0).getCoordinates()).isEqualTo(
			transformed.getGeometryN(0).getCoordinates());
	}

	@Test
	void testeTransformGeometryToUTM32_noSRIDOnGeometry_derivedGeometryHasCorrectSRID() {
		// arrange
		SimpleFeatureBuilder TDPsimpleFeatureBuilder = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(MultiLineString.class).buildFeatureType());

		MultiLineString multiLineStringSingle = new GeometryFactory().createMultiLineString(
			new LineString[] {
				new GeometryFactory().createLineString(
					new Coordinate[] { new Coordinate(0, 1), new Coordinate(1, 1) })
			}
		);
		TDPsimpleFeatureBuilder.add(multiLineStringSingle);
		String keepId = "keep";
		SimpleFeature feature = TDPsimpleFeatureBuilder.buildFeature(keepId);

		MultiLineString transformed = new GeometryFactory().createMultiLineString(
			new LineString[] {
				new GeometryFactory().createLineString(
					new Coordinate[] { new Coordinate(0, 1), new Coordinate(1, 1) })
			}
		);
		when(coordinateReferenceSystemConverter.transformGeometry(any(), any())).thenReturn(
			transformed);

		// act
		SimpleFeature result = shapeFileRepository.transformGeometryToUTM32(feature);

		// assert
		assertThat(((Geometry) result.getDefaultGeometry()).getSRID())
			.isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(((Geometry) result.getDefaultGeometry()).getCentroid().getSRID())
			.isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(((Geometry) result.getDefaultGeometry()).getGeometryType()).isEqualTo(
			MultiLineString.TYPENAME_MULTILINESTRING);
		assertThat(((MultiLineString) feature.getDefaultGeometry()).getGeometryN(0).getSRID()).isEqualTo(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(((MultiLineString) feature.getDefaultGeometry()).getGeometryN(0).getCentroid().getSRID()).isEqualTo(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(((MultiLineString) feature.getDefaultGeometry()).getGeometryN(0).getCoordinates()).isEqualTo(
			transformed.getGeometryN(0).getCoordinates());
	}
}
