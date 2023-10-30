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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import de.wps.radvis.backend.quellimport.common.schnittstelle.repositoryImpl.FeatureImportRepositoryImpl;

class FeatureImportRepositoryImplTest {

	/*
	 * Attribute test_shape_punkte.shp id Zweck Prüfnumme 1 Kreuzung 1234,56 2 Wegweiser 987,60
	 */
	private static final String TEST_SHAPE_PUNKTE_SHP = "./shp/test_shape_punkte.shp";

	/*
	 * PunkteShapeFile mit m-Koordinate
	 */
	private static final String TEST_SHAPE_PUNKTE_M_KOORDINATE_SHP = "shp/test_shape_punkte_m_koordinate.shp";

	/*
	 * Attribute test_shape_linien.shp id str_name breite 1 Andelsbachstraße 2,35 2 Brunnenmatt 1,54
	 */
	private static final String TEST_SHAPE_LINIEN_SHP = "shp/test_shape_linien/test_shape_linien.shp";

	/*
	 * Attribute test_shape_linien_achsen_vertauscht.shp enthält vertauschte Koordinaten
	 */
	private static final String TEST_SHAPE_LINIEN_ACHSEN_VERTAUSCHT_SHP = "shp/test_shape_linien_achsen_vertauscht.shp";

	/**
	 *
	 */
	private static final String TEST_NULLPOINTER = "shp/no_geometry/no_geometry.shp";

	private FeatureImportRepositoryImpl shpImportService;
	@Mock
	private CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		when(coordinateReferenceSystemConverter.transformGeometry(any(), any())).thenAnswer(i -> {
			// Es ist wichtig eine Kopie zu machen, damit das Envelope korrekt erzeugt wird.
			Geometry geometrie = ((Geometry) i.getArgument(0)).copy();
			geometrie.setSRID(((KoordinatenReferenzSystem) i.getArgument(1)).getSrid());
			return geometrie;
		});
		when(coordinateReferenceSystemConverter.sindKoordinatenPlausibel(any())).thenReturn(true);
		shpImportService = new FeatureImportRepositoryImpl(coordinateReferenceSystemConverter, null);
	}

	@AfterEach
	public void after() {
		shpImportService.closeIterators();
	}

	@Test
	public void readShapeFile_simpleImport() throws IOException {
		// Act
		ClassPathResource punktFile = new ClassPathResource(TEST_SHAPE_PUNKTE_SHP);
		ClassPathResource linienFile = new ClassPathResource(TEST_SHAPE_LINIEN_SHP);
		List<ImportedFeature> resultPunkte = shpImportService
			.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_POINT, QuellSystem.RadNETZ, Art.Strecke,
				punktFile.getFile())
			.collect(
				Collectors.toList());
		List<ImportedFeature> resultStrecken = shpImportService
			.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_LINESTRING, QuellSystem.RadNETZ, Art.Strecke,
				linienFile.getFile())
			.collect(
				Collectors.toList());

		// Assert
		assertNotNull(resultPunkte);
		assertNotNull(resultStrecken);
		int anzahlPunktGeometrien = 2;
		int anzahlLinienGeometrien = 2;
		assertThat(resultPunkte).hasSize(anzahlPunktGeometrien);
		assertThat(resultPunkte).allMatch(resultPunkt -> resultPunkt.getQuelle().equals(QuellSystem.RadNETZ));
		assertThat(resultPunkte).allMatch(resultPunkt -> resultPunkt.getArt().equals(Art.Strecke));
		assertThat(resultStrecken).hasSize(anzahlLinienGeometrien);
		assertThat(resultStrecken).allMatch(resultStrecke -> resultStrecke.getQuelle().equals(QuellSystem.RadNETZ));
		assertThat(resultStrecken).allMatch(resultStrecke -> resultStrecke.getArt().equals(Art.Strecke));
	}

	@Test
	public void testReadShapefiles_checkFeatures_punkte() throws IOException {
		// Arrange

		ClassPathResource punktFile = new ClassPathResource(TEST_SHAPE_PUNKTE_SHP);

		// Act
		List<ImportedFeature> massnahmenPunkte = shpImportService
			.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_POINT, QuellSystem.RadNETZ, Art.Strecke,
				punktFile.getFile())
			.collect(
				Collectors.toList());

		// Assert
		assertNotNull(massnahmenPunkte);
		assertThat(massnahmenPunkte).hasSize(2);

		assertThat(massnahmenPunkte.get(0).getTechnischeId()).isEqualTo("test_shape_punkte.1");
		Map<String, Object> attribute1 = massnahmenPunkte.get(0).getAttribute();
		assertThat(attribute1.keySet()).containsExactlyInAnyOrder("id", "Zweck", "Prüfnumme", "the_geom");
		assertThat(attribute1.get("id")).isEqualTo(1L);
		assertThat(attribute1.get("Zweck")).isEqualTo("Kreuzung");
		assertThat(attribute1.get("Prüfnumme")).isEqualTo(1234.56);

		assertThat(massnahmenPunkte.get(1).getTechnischeId()).isEqualTo("test_shape_punkte.2");
		Map<String, Object> attribute2 = massnahmenPunkte.get(1).getAttribute();
		assertThat(attribute2.get("id")).isEqualTo(2L);
		assertThat(attribute2.get("Zweck")).isEqualTo("Wegweiser");
		assertThat(attribute2.get("Prüfnumme")).isEqualTo(987.60);
	}

	@Test
	public void testReadShapefiles_checkFeatures_linien() throws IOException {

		ClassPathResource linienFile = new ClassPathResource(TEST_SHAPE_LINIEN_SHP);

		// Act
		List<ImportedFeature> streckenLinien = shpImportService
			.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_LINESTRING, QuellSystem.RadNETZ, Art.Strecke,
				linienFile.getFile())
			.collect(
				Collectors.toList());

		// Assert
		assertNotNull(streckenLinien);
		assertThat(streckenLinien).hasSize(2);

		ImportedFeature strecke0 = streckenLinien.get(0);
		assertThat(strecke0.getTechnischeId()).isEqualTo("test_shape_linien.1");
		assertThat(strecke0.getAttribute().keySet()).containsExactlyInAnyOrder("id",
			"Str_name", "Breite", "the_geom");
		assertThat(strecke0.getGeometrie().getCoordinates()).hasSize(5);
		Map<String, Object> attribute3 = strecke0.getAttribute();
		assertThat(attribute3.get("id")).isEqualTo(1L);
		assertThat(attribute3.get("Str_name")).isEqualTo("Andelsbachstraße");
		assertThat(attribute3.get("Breite")).isEqualTo(2.35);

		ImportedFeature strecke1 = streckenLinien.get(1);
		assertThat(strecke1.getTechnischeId()).isEqualTo("test_shape_linien.2");
		assertThat(strecke1.getAttribute().keySet()).containsExactlyInAnyOrder("id",
			"Str_name", "Breite", "the_geom");
		assertThat(strecke1.getGeometrie().getCoordinates())
			.hasSize(3);
		Map<String, Object> attribute4 = strecke1.getAttribute();
		assertThat(attribute4.get("id")).isEqualTo(2L);
		assertThat(attribute4.get("Str_name")).isEqualTo("Brunnenmatt");
		assertThat(attribute4.get("Breite")).isEqualTo(1.54);
	}

	@Test
	void testReadShapeFiles_getauschteKoordinaten_erstelltKorrektStreckenLinien() throws IOException {

		ClassPathResource linienFile = new ClassPathResource(TEST_SHAPE_LINIEN_ACHSEN_VERTAUSCHT_SHP);

		// Act
		List<ImportedFeature> streckenLinien = shpImportService
			.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_LINESTRING, QuellSystem.RadNETZ, Art.Strecke,
				linienFile.getFile())
			.collect(
				Collectors.toList());

		// Assert
		assertNotNull(streckenLinien);
		assertThat(streckenLinien).hasSize(1);
	}

	@Test
	void testReadShapeFiles_shapeFileFeaturesenthaltenMOrdinate_importierteGeometrienEnthaeltKeineMOrdinate()
		throws IOException {

		ClassPathResource punkteFile = new ClassPathResource(TEST_SHAPE_PUNKTE_M_KOORDINATE_SHP);

		// Act
		List<ImportedFeature> punkte = shpImportService
			.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_POINT, QuellSystem.RadNETZ, Art.Strecke,
				punkteFile.getFile())
			.collect(
				Collectors.toList());

		// Assert
		assertNotNull(punkte);
		assertThat(punkte).hasSize(3);
		assertThat(punkte).extracting(ImportedFeature::getGeometrie).extracting(Geometry::getCoordinates).allMatch(
			coordinates -> Arrays.stream(coordinates).allMatch(coordinate -> Double.isNaN(coordinate.getM())));
	}

	@Test
	public void testReadRadwegeDb_noNullPointer() throws IOException {
		ClassPathResource shpWithNullGeometry = new ClassPathResource(TEST_NULLPOINTER);

		assertDoesNotThrow(() -> shpImportService
			.getImportedFeaturesFromShapeFiles(Geometry.TYPENAME_POINT, QuellSystem.RadNETZ, Art.Strecke,
				shpWithNullGeometry.getFile())
			.collect(
				Collectors.toList()));
	}
}
