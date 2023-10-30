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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.WKTWriter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opengis.feature.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.manuellerimport.common.ManuellerImportCommonConfiguration;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.PbfErstellungsRepository;
import de.wps.radvis.backend.matching.domain.service.MatchingKorrekturService;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsTestDataProvider;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.quellimport.common.ImportsCommonConfiguration;
import de.wps.radvis.backend.quellimport.common.domain.FeatureImportRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Tag("group3")
@Slf4j
@ContextConfiguration(classes = {
	CommonConfiguration.class,
	GeoConverterConfiguration.class,
	ImportsCommonConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	MatchingConfiguration.class,
	NetzfehlerConfiguration.class,
	KommentarConfiguration.class,
	ManuellerImportCommonConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class,
	BarriereConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	DLMConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
@ActiveProfiles("test")
@SuppressWarnings({ "rawtypes", "unchecked" })
class ManuellerNetzklassenImportAbbildungsServiceTestIT extends DBIntegrationTestIT {

	static ExtentProperty commonExtentSchwaebischHall = new ExtentProperty(548298, 559792, 5436959, 5443026);
	static ExtentProperty commonExtentBodensee = new ExtentProperty(502229.0, 594092.2, 5253338, 5319781);

	@TempDir
	public File temp;

	// Dependencies für Service
	@Mock
	private GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties;

	@Mock
	private OsmPbfConfigurationProperties osmPbfConfigurationProperties;

	@Autowired
	GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties;

	@Autowired
	CommonConfigurationProperties commonConfigurationProperties;

	@Autowired
	MatchingKorrekturService matchingKorrekturService;

	@Autowired
	InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory;

	ManuellerNetzklassenImportAbbildungsService manuellerNetzklassenImportAbbildungsService;

	// Dependencies für Test
	@Autowired
	FeatureImportRepository featureImportRepository;

	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	BarriereRepository barriereRepository;

	@PersistenceContext
	EntityManager entityManager;

	private PbfErstellungsRepository pbfErstellungsRepository;

	@BeforeEach
	void setup() {
		CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
			commonConfigurationProperties.getBadenWuerttembergEnvelope());

		pbfErstellungsRepository = new PbfErstellungsRepositoryImpl(coordinateReferenceSystemConverter, entityManager, barriereRepository, kantenRepository);
	}

	@Test
	public void testFindKantenFromLineStrings_minimaleBspDaten_zweiKantenWerdenGefunden() throws IOException {
		// arrange

		File testLineStringsFile = new File("src/test/resources/shp/test_Netzzugehoerigkeit_ausschnitt.shp");
		Stream<ImportedFeature> radStreckenFeatures;
		radStreckenFeatures = featureImportRepository.getImportedFeaturesFromShapeFiles(
			Geometry.TYPENAME_LINESTRING, QuellSystem.DLM, Art.Strecke, testLineStringsFile);

		Set<LineString> importedLineStrings = radStreckenFeatures
			.map(ImportedFeature::getGeometrie)
			.map(geometry -> (LineString) geometry).collect(Collectors.toSet());

		// fuelle Kantenrepository und setzte dabei die ursprungstechnische id auf die ID aus der json
		ladeDLMDatenAusJSONDatei("src/test/resources/alleDLMKantenSchwaebischHall_Ausschnitt.json");

		List<List<Kante>> kanten = List.of(
			StreamSupport.stream(kantenRepository.findAll().spliterator(), false).collect(Collectors.toList()));

		File testPbf = new File(temp, "test.osm.pbf");

		pbfErstellungsRepository.writePbf(PbfErstellungsTestDataProvider.getEnvelopeToKantenStreamMap(kanten), testPbf);

		initNetzklassenAbbildungsService(testPbf.getAbsolutePath(), commonExtentSchwaebischHall);

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(2L)
			.bereich(GeometryTestdataProvider.getQuadratischenBereichFuerLinestrings(importedLineStrings))
			.build();

		// act
		Set<Long> zugehoerigeKanteIds = manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(
			importedLineStrings,
			organisation).matchedKanten;

		// assert
		List<Kante> expectedKanten = (List<Kante>) entityManager.createQuery(
				"SELECT kante FROM Kante kante WHERE kante.ursprungsfeatureTechnischeID IN :ids ")
			.setParameter("ids", List.of("34997", "26314"))
			.getResultList();
		assertThat(expectedKanten).hasSize(2);

		assertThat(zugehoerigeKanteIds)
			.containsExactlyInAnyOrderElementsOf(
				expectedKanten.stream().map(Kante::getId).collect(Collectors.toList()));
	}

	// aus performance gründen wird dieser test per default disabled
	@Disabled
	@Test
	public void testFindKantenFromLineStrings_umfangreicheBspDaten_alleKantenWerdenGefunden() throws IOException {
		// arrange
		initNetzklassenAbbildungsService("src/test/resources/dlm_schwaebischHall.osm.pbf", commonExtentSchwaebischHall);

		File testLineStringsFile = new File("src/test/resources/shp/test_Netzzugehoerigkeit.shp");
		Stream<ImportedFeature> radStreckenFeatures;
		radStreckenFeatures = featureImportRepository.getImportedFeaturesFromShapeFiles(
			Geometry.TYPENAME_LINESTRING, QuellSystem.DLM, Art.Strecke, testLineStringsFile);

		Set<LineString> importedLineStrings = radStreckenFeatures
			.map(ImportedFeature::getGeometrie)
			.map(geometry -> (LineString) geometry).collect(Collectors.toSet());

		// fuelle Kantenrepository und setzte dabei die ursprungstechnische id auf die ID aus der json
		ladeDLMDatenAusJSONDatei("src/test/resources/alleDLMKantenSchwaebischHall.json");

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(2L)
			.bereich(GeometryTestdataProvider.getQuadratischenBereichFuerLinestrings(importedLineStrings))
			.build();

		// act
		Set<Long> zugehoerigeKanteIds = manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(
			importedLineStrings,
			organisation).matchedKanten;

		// assert
		List<String> expectedKanteIds = List.of(
			"27057", "25614", "33007", "26356", "34362", "26338", "34343",

			"31809", "24182", "33705", "23461",

			"26314", "31112", "31868", "31848", "26351", "34338", "27763", "21920", "29132", "23487", "27038", "31175",
			"22641",
			"27025", "27013", "33078",

			"36955", "19153", "31146", "21984", "23489", "27761", "31832", "22691", "35657", "31195", "33011", "22620",
			"31887",
			"36211");
		List<Kante> expectedKanten = (List<Kante>) entityManager.createQuery(
				"SELECT kante FROM Kante kante WHERE kante.ursprungsfeatureTechnischeID IN :ids ")
			.setParameter("ids", expectedKanteIds)
			.getResultList();
		assertThat(expectedKanten).hasSize(expectedKanteIds.size());

		assertThat(zugehoerigeKanteIds)
			.containsExactlyInAnyOrderElementsOf(
				expectedKanten.stream().map(Kante::getId).collect(Collectors.toList()));
	}

	// aus performance gründen wird dieser test per default disabled
	@Disabled
	@Test
	void generiereWKTFuerNetzklassenAbbildungDerBodenSeeDaten() throws IOException {
		// arrange
		initNetzklassenAbbildungsService("src/test/resources/dlm_Bodensee_Ausschnitt.osm.pbf", commonExtentBodensee);
		File testLineStringsFile = new File("src/test/resources/shp/Bodenseekreis_Kreisnetz.shp");
		Stream<ImportedFeature> radStreckenFeatures = featureImportRepository.getImportedFeaturesFromShapeFiles(
			Geometry.TYPENAME_LINESTRING, QuellSystem.DLM, Art.Strecke, testLineStringsFile);

		Set<LineString> importedLineStrings = radStreckenFeatures
			.map(ImportedFeature::getGeometrie)
			.map(geometry -> (LineString) geometry)
			.collect(Collectors.toSet());

		GeometryCollection collection = new GeometryCollection(importedLineStrings.toArray(new Geometry[] {}),
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory());

		Envelope envelope = collection.getEnvelopeInternal();
		System.out.println(envelope);
		// fuelle Kantenrepository und setzte dabei die ursprungstechnische id auf die ID aus der json
		ladeDLMDatenAusJSONDatei("src/test/resources/alleDLMKantenBodensee_Ausschnitt.json");

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(2L)
			.bereich(GeometryTestdataProvider.getQuadratischenBereichFuerLinestrings(importedLineStrings))
			.build();

		// act
		Set<Long> zugehoerigeKanteIds = manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(
			importedLineStrings,
			organisation).matchedKanten;

		// Konvertiere zum wkt-format
		WKTWriter wktWriter = new WKTWriter();

		kantenRepository.findAllById(zugehoerigeKanteIds).forEach(kante -> {
			System.out.println(wktWriter.write(kante.getGeometry()));
		});

	}

	private void ladeDLMDatenAusJSONDatei(String filename) throws IOException {
		File alleDLMKanten = new File(filename);
		log.info("Lade Dlm Kanten aus {}", alleDLMKanten.getAbsolutePath());
		try (InputStream inputStream = new FileInputStream(alleDLMKanten)) {
			GeometryJSON geometryJSON = new GeometryJSON();
			FeatureJSON featureJSON = new FeatureJSON(geometryJSON);

			FeatureCollection featureCollection = featureJSON.readFeatureCollection(inputStream);
			try (FeatureIterator iterator = featureCollection.features()) {
				while (iterator.hasNext()) {
					Feature feature = iterator.next();
					LineString lineString = (LineString) feature.getDefaultGeometryProperty().getValue();
					lineString.setSRID(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
					long id = Long.parseLong(feature.getIdentifier().getID());
					kantenRepository.save(KanteTestDataProvider.withDefaultValues()
						.geometry(lineString)
						.quelle(QuellSystem.DLM)
						.ursprungsfeatureTechnischeID("" + id)
						.build());

				}
			}

		}
	}

	private void initNetzklassenAbbildungsService(String pfadZuDlmPbf, ExtentProperty commonExtent) {
		MockitoAnnotations.openMocks(this);

		File tiffTile = new File(graphhopperDlmConfigurationProperties.getTiffTilesVerzeichnis(), "/test-tiff-tiles");
		tiffTile.mkdir();

		GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties = new GraphhopperDlmConfigurationProperties(
			pfadZuDlmPbf,
			new File(temp, "/test-routing-graph-cache").getAbsolutePath(),
			new File(temp, "/test-mapping-graph-cache").getAbsolutePath(), 0.6,
			new File(temp, "/test-elevation-cache").getAbsolutePath(),
			tiffTile.getPath());
		ReflectionTestUtils.setField(commonConfigurationProperties, "extentProperty", commonExtent);

		MatchingConfiguration matchingConfiguration = new MatchingConfiguration(
			new GeoConverterConfiguration(commonConfigurationProperties),
			commonConfigurationProperties,
			graphhopperOsmConfigurationProperties,
			osmPbfConfigurationProperties,
			graphhopperDlmConfigurationProperties);

		manuellerNetzklassenImportAbbildungsService = new ManuellerNetzklassenImportAbbildungsService(
			matchingConfiguration.matchingFuerManuellerImportService(), inMemoryKantenRepositoryFactory);
	}
}
