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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opengis.feature.simple.SimpleFeature;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.exception.ZipFileRequiredFilesMissingException;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportSession;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMappingTestDataProvider;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.repository.ShapeFileAttributeRepository;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.ImportierbaresAttribut;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.KantenKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AttributeImportFormat;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AutomatischerImportSchritt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag.Severity;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionStatus;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ManuellerImportFehlerursache;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import jakarta.persistence.OptimisticLockException;

class ManuellerAttributeImportServiceTest {

	private ManuellerAttributeImportService manuellerAttributeImportService;

	@Mock
	private ManuellerAttributeImportAbbildungsService manuellerAttributeImportAbbildungsService;

	@Mock
	private ShapeZipService shapeZipServiceMock;

	@Mock
	private ManuellerImportService manuellerImportService;

	@Mock
	private ShapeFileAttributeRepository attributeRepository;

	@Mock
	private KantenRepository kantenRepository;

	@Mock
	private ManuellerImportFehlerRepository manuellerImportFehlerRepository;

	@Mock
	private AttributMapperFactory mapperFactory;

	@Mock
	private ShapeFileRepository shapeFileRepository;

	@Mock
	private ManuellerAttributeImportUebernahmeService manuellerAttributeImportUebernahmeService;

	private ShapeZipService shapeZipService;

	Benutzer benutzer;
	Verwaltungseinheit organisation;
	LocalDateTime startTime;

	@BeforeEach
	void setup() {
		openMocks(this);
		shapeZipService = new ShapeZipService();

		benutzer = BenutzerTestDataProvider.defaultBenutzer().id(10L).build();
		organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(15L)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100)).build();
		startTime = LocalDateTime.of(2022, 9, 23, 15, 17);

		this.manuellerAttributeImportService = new ManuellerAttributeImportService(manuellerImportService,
			manuellerAttributeImportAbbildungsService,
			manuellerAttributeImportUebernahmeService,
			shapeZipServiceMock, shapeFileRepository, attributeRepository, mapperFactory, kantenRepository,
			manuellerImportFehlerRepository);
	}

	@Test
	void testRunAutomatischeAbbildung_noFeatures_warn()
		throws IOException, ZipFileRequiredFilesMissingException, ShapeProjectionException {
		// arrange
		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));
		when(this.manuellerAttributeImportAbbildungsService
			.bildeFeaturesAb(any(), any())).thenReturn(List.of());
		when(shapeFileRepository.readShape(any())).thenReturn(Stream.empty());
		AttributeImportSession sessionMock = mock(AttributeImportSession.class);
		ArgumentCaptor<ImportLogEintrag> logEintragArgument = ArgumentCaptor.forClass(ImportLogEintrag.class);
		when(sessionMock.getOrganisation()).thenReturn(mock(Verwaltungseinheit.class));

		// act + assert
		assertThatNoException().isThrownBy(
			() -> manuellerAttributeImportService.runAutomatischeAbbildung(sessionMock, new File("")));
		verify(sessionMock).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
	}

	@Test
	void testRunAutomatischeAbbildung_noFeaturesInBereich_warn()
		throws IOException, ZipFileRequiredFilesMissingException, ShapeProjectionException {
		// arrange
		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));
		when(this.manuellerAttributeImportAbbildungsService
			.bildeFeaturesAb(any(), any())).thenReturn(List.of());
		when(shapeFileRepository.readShape(any()))
			.thenReturn(Stream.of(SimpleFeatureTestDataProvider.defaultFeature()));
		AttributeImportSession sessionMock = mock(AttributeImportSession.class);
		ArgumentCaptor<ImportLogEintrag> logEintragArgument = ArgumentCaptor.forClass(ImportLogEintrag.class);
		Verwaltungseinheit organisationMock = mock(Verwaltungseinheit.class);
		MultiPolygon zustaendigkeitsBereichMock = mock(MultiPolygon.class);
		when(zustaendigkeitsBereichMock.intersects(any())).thenReturn(false);
		when(organisationMock.getBereich()).thenReturn(Optional.of(zustaendigkeitsBereichMock));
		when(sessionMock.getOrganisation()).thenReturn(organisationMock);

		// act + assert
		assertThatNoException().isThrownBy(
			() -> manuellerAttributeImportService.runAutomatischeAbbildung(sessionMock, new File("")));
		verify(sessionMock).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
	}

	@Test
	void testRunAutomatischeAbbildung_filterZustaendigkeitsbereich()
		throws IOException, ZipFileRequiredFilesMissingException, ShapeProjectionException {
		// arrange
		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));
		when(this.manuellerAttributeImportAbbildungsService
			.bildeFeaturesAb(any(), any())).thenReturn(List.of());
		when(shapeFileRepository.readShape(any()))
			.thenReturn(
				Stream.of(SimpleFeatureTestDataProvider.withLineString(new Coordinate(0, 0), new Coordinate(0, 10))));
		AttributeImportSession sessionMock = mock(AttributeImportSession.class);
		ArgumentCaptor<ImportLogEintrag> logEintragArgument = ArgumentCaptor.forClass(ImportLogEintrag.class);
		Verwaltungseinheit organisationMock = mock(Verwaltungseinheit.class);
		Polygon polygon = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPolygon(
			new Coordinate[] { new Coordinate(100, 100), new Coordinate(100, 200), new Coordinate(200, 200),
				new Coordinate(200, 100),
				new Coordinate(100, 100) });
		MultiPolygon zustaendigkeitsBereich = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createMultiPolygon(new Polygon[] { polygon });
		when(organisationMock.getBereich()).thenReturn(Optional.of(zustaendigkeitsBereich));
		when(sessionMock.getOrganisation()).thenReturn(organisationMock);

		// act + assert
		assertThatNoException().isThrownBy(
			() -> manuellerAttributeImportService.runAutomatischeAbbildung(sessionMock, new File("")));
		verify(sessionMock).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
	}

	@Test
	void testRunAutomatischeAbbildung_ProjektionHappyPath_sessionKorrekt() throws Exception {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = benutzer.getOrganisation();
		File testLineStringsFile = new File("src/test/resources/shp/Bodenseekreis_Kreisnetz.zip");
		File shpDirectory = shapeZipService.unzip(
			Files.readAllBytes(testLineStringsFile.toPath()));
		when(shapeZipServiceMock.unzip(any())).thenReturn(shpDirectory);
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(shpDirectory));
		AttributeImportSession session = new AttributeImportSession(benutzer, organisation,
			List.of("einAttribut"), AttributeImportFormat.LUBW);
		Stream<SimpleFeature> importedFeatures = Stream.empty();
		List<FeatureMapping> featureMappings = List.of();
		when(shapeFileRepository.readShape(any())).thenReturn(
			importedFeatures);

		when(this.manuellerAttributeImportAbbildungsService.bildeFeaturesAb(any(),
			eq(session))).thenReturn(featureMappings);

		// act
		this.manuellerAttributeImportService.runAutomatischeAbbildung(session, testLineStringsFile);

		// assert
		assertThat(session.getAktuellerImportSchritt()).isEqualTo(
			AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN);
		assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
	}

	@Test
	public void runAutomatischeAbbildung_emptyGeometry_1Warning()
		throws ShapeProjectionException, IOException {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
			.build();
		AttributeImportSession session = new AttributeImportSession(benutzer, organisation,
			List.of("einAttribut"), AttributeImportFormat.LUBW);

		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(LineString.class).buildFeatureType());

		featureBuilder.add(null);
		SimpleFeature l1Feature = featureBuilder.buildFeature("id1");

		LineString linestring2 = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(10, 10));
		featureBuilder.add(linestring2);
		SimpleFeature l2Feature = featureBuilder.buildFeature("id2");

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(l1Feature, l2Feature));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));

		// act
		this.manuellerAttributeImportService.runAutomatischeAbbildung(session, new File(""));

		// assert
		assertThat(session.getLog()).hasSize(1);
		assertThat(session.getLog().get(0).getFehlerBeschreibung())
			.isEqualTo(
				"Shapefile enthält 1 Features ohne Geometrien, welche aus diesem Grund nicht importiert werden konnten.");
	}

	@Test
	void testRunUpdate_happyPath_sessionStatusKorrekt() {
		// arrange
		AttributeImportSession session = new AttributeImportSession(benutzer, organisation, List.of("a", "b", "c"),
			AttributeImportFormat.LUBW);
		when(mapperFactory.createMapper(session.getAttributeImportFormat())).thenReturn(new LUBWMapper());
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = benutzer.getOrganisation();
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		session.setFeatureMappings(List.of());

		// act
		this.manuellerAttributeImportService.runUpdate(session);

		// assert
		assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.UPDATE_DONE);
	}

	@Test
	void testRunUpdate_optimisticLockException_schlaegtFehlAberkannErneutAusgefuehrtWerden() {
		// arrange
		AttributeImportSession session = new AttributeImportSession(benutzer, organisation, List.of("a", "b", "c"),
			AttributeImportFormat.LUBW);
		when(mapperFactory.createMapper(session.getAttributeImportFormat())).thenReturn(new LUBWMapper());
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = benutzer.getOrganisation();
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		session.setFeatureMappings(List.of());
		doThrow(new OptimisticLockException("Oh no!")).when(
			manuellerAttributeImportUebernahmeService).attributeUebernehmen(any(), any(), any(), any(), any());

		// act
		assertThatThrownBy(() -> this.manuellerAttributeImportService.runUpdate(session)).isInstanceOf(
				OptimisticLockException.class)
			.hasMessage("Kanten in der Organisation wurden während des Speicherns aus einer anderen Quelle verändert."
				+ " Bitte versuchen Sie es erneut.");
		// assert
		assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		assertThat(session.getLog()).isEmpty();
	}

	@Test
	void testRunUpdate_unexpectedException_schlaegtFehlUndKannNichtErneutAusgefuehrtWerden() {
		// arrange
		AttributeImportSession session = new AttributeImportSession(benutzer, organisation, List.of("a", "b", "c"),
			AttributeImportFormat.LUBW);
		when(mapperFactory.createMapper(session.getAttributeImportFormat())).thenReturn(new LUBWMapper());
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = benutzer.getOrganisation();
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		session.setFeatureMappings(List.of());
		doThrow(new RequireViolation("Oh no!")).when(
			manuellerAttributeImportUebernahmeService).attributeUebernehmen(any(), any(), any(), any(), any());

		// act
		assertThatThrownBy(() -> this.manuellerAttributeImportService.runUpdate(session)).isInstanceOf(
				RequireViolation.class)
			.hasMessage("Oh no!");

		// assert
		assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.UPDATE_DONE);
		assertThat(session.getLog()).containsExactly(
			ImportLogEintrag.ofError("Es ist ein unerwarteter Fehler aufgetreten."));
	}

	@Test
	void testExecuteSession_fehlerfall_sessionStatusKorrektUndFehlerGesetzt() {
		// arrange
		AttributeImportSession session = new AttributeImportSession(benutzer, organisation, List.of("a", "b", "c"),
			AttributeImportFormat.LUBW);
		when(mapperFactory.createMapper(session.getAttributeImportFormat())).thenReturn(new LUBWMapper());
		doThrow(new RuntimeException()).when(
			manuellerAttributeImportUebernahmeService).attributeUebernehmen(any(), any(), any(), any(),
			any());
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = benutzer.getOrganisation();
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		session.setFeatureMappings(List.of());

		// act & assert
		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> this.manuellerAttributeImportService.runUpdate(session));

		// assert
		assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.UPDATE_DONE);
		assertThat(session.getLog().size()).isEqualTo(1);
	}

	@Test
	void testrunUpdate_fehlerfall_KonfliktprotokolleNichtNull() {
		// arrange
		AttributeImportSession session = new AttributeImportSession(benutzer, organisation, List.of("a", "b", "c"),
			AttributeImportFormat.LUBW);
		when(mapperFactory.createMapper(session.getAttributeImportFormat())).thenReturn(new LUBWMapper());
		doThrow(new RuntimeException()).when(
			manuellerAttributeImportUebernahmeService).attributeUebernehmen(any(), any(), any(), any(),
			any());
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = benutzer.getOrganisation();
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		session.setFeatureMappings(List.of());

		// act & assert
		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> this.manuellerAttributeImportService.runUpdate(session));

		// assert
		assertThat(session.getAttributeImportKonfliktProtokoll()).isNotNull();
	}

	@Test
	void testProjektion_SonstigerFehler_addFehlerUndThrow() throws Exception {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = benutzer.getOrganisation();
		File testLineStringsFile = new File("src/test/resources/shp/Bodenseekreis_Kreisnetz.zip");
		AttributeImportSession session = new AttributeImportSession(benutzer, organisation, List.of("einAttribut"),
			AttributeImportFormat.LUBW);
		when(shapeFileRepository.readShape(any())).thenThrow(
			new RuntimeException("Fehler"));

		// act & assert
		this.manuellerAttributeImportService.runAutomatischeAbbildung(session, testLineStringsFile);

		// assert
		assertThat(session.getLog().size()).isEqualTo(1);
		assertThat(session.getAktuellerImportSchritt()).isEqualTo(
			AutomatischerImportSchritt.IMPORT_DER_DATEN);
		assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
	}

	@Test
	void testProjektion_RequireViolation_addFehlerUndThrow() throws Exception {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		Verwaltungseinheit organisation = benutzer.getOrganisation();
		File testLineStringsFile = new File("src/test/resources/shp/Bodenseekreis_Kreisnetz.zip");
		File shpDirectory = shapeZipService.unzip(
			Files.readAllBytes(testLineStringsFile.toPath()));
		when(shapeZipServiceMock.unzip(any())).thenReturn(shpDirectory);
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(shpDirectory));
		AttributeImportSession session = new AttributeImportSession(benutzer, organisation,
			List.of("einAttribut"), AttributeImportFormat.LUBW);
		Stream<SimpleFeature> importedFeatures = Stream.empty();
		when(shapeFileRepository.readShape(any())).thenReturn(
			importedFeatures);

		when(manuellerAttributeImportAbbildungsService.bildeFeaturesAb(any(), any())).thenThrow(
			new RequireViolation("WAMBO!"));

		// act & assert
		this.manuellerAttributeImportService.runAutomatischeAbbildung(session, testLineStringsFile);

		// assert
		assertThat(session.getLog()).contains(ImportLogEintrag.ofError("Es ist ein unerwarteter Fehler aufgetreten."));
		assertThat(session.getAktuellerImportSchritt()).isEqualTo(
			AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ);
		assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
	}

	@Test
	public void validateAttribute_filterAttribute() throws IOException, ZipFileRequiredFilesMissingException {
		LUBWMapper mapper = mock(LUBWMapper.class);
		when(mapperFactory.createMapper(any())).thenReturn(mapper);
		String validAttributName = "validAttributName";
		String invalidAttributName = "invalidAttributName";
		String attributWert = "attributWert1";
		String attributWert2 = "attributWert2";
		String radvisAttribut = "validRadvisAttribut";

		when(mapper.getImportGruppe(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(mapper.shouldFilterNullValues(any())).thenReturn(true);

		when(mapper.isAttributNameValid(validAttributName)).thenReturn(true);
		when(mapper.isAttributNameValid(invalidAttributName)).thenReturn(false);
		when(mapper.getRadVisAttributName(validAttributName)).thenReturn(radvisAttribut);
		when(mapper.getRadVisAttributName(invalidAttributName)).thenReturn("invalidRadvisAttribut");
		when(attributeRepository.getAttributnamen(any())).thenReturn(Set.of(validAttributName, invalidAttributName));
		when(attributeRepository.getAttributWerte(any(), eq(validAttributName)))
			.thenReturn(Stream.of(attributWert, attributWert2));
		when(mapper.isAttributWertValid(any(), any())).thenReturn(true);
		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));

		InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("shp/empty.zip");
		List<ImportierbaresAttribut> validateAttribute = manuellerAttributeImportService
			.validateAttribute(resourceAsStream.readAllBytes(), AttributeImportFormat.LUBW);

		assertThat(validateAttribute)
			.containsExactly(ImportierbaresAttribut.of(validAttributName, radvisAttribut, validAttributName, true,
				Collections.emptySet()));
	}

	@Test
	public void validateAttribute_invalidAttributWerte() throws IOException, ZipFileRequiredFilesMissingException {
		LUBWMapper mapper = mock(LUBWMapper.class);
		when(mapperFactory.createMapper(any())).thenReturn(mapper);
		String validAttributName = "validAttributName";
		String validAttributWert = "attributWert1";
		String invalidAttributWert = "attributWert2";
		String radvisAttribut = "validRadvisAttribut";

		when(mapper.getImportGruppe(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(mapper.shouldFilterNullValues(any())).thenReturn(true);

		when(mapper.isAttributNameValid(validAttributName)).thenReturn(true);
		when(mapper.getRadVisAttributName(validAttributName)).thenReturn(radvisAttribut);
		when(attributeRepository.getAttributnamen(any())).thenReturn(Set.of(validAttributName));
		when(attributeRepository.getAttributWerte(any(), eq(validAttributName)))
			.thenReturn(Stream.of(validAttributWert, invalidAttributWert));
		when(mapper.isAttributWertValid(validAttributName, validAttributWert)).thenReturn(true);
		when(mapper.isAttributWertValid(validAttributName, invalidAttributWert)).thenReturn(false);
		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));

		InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("shp/empty.zip");
		List<ImportierbaresAttribut> validateAttribute = manuellerAttributeImportService
			.validateAttribute(resourceAsStream.readAllBytes(), AttributeImportFormat.LUBW);

		assertThat(validateAttribute)
			.containsExactly(ImportierbaresAttribut.of(validAttributName, radvisAttribut, validAttributName, false,
				Set.of(invalidAttributWert)));
	}

	@Test
	public void validateAttribute_multiple_invalidAndValid() throws IOException, ZipFileRequiredFilesMissingException {
		LUBWMapper mapper = mock(LUBWMapper.class);
		when(mapperFactory.createMapper(any())).thenReturn(mapper);
		String attributName1 = "attributName1";
		String attributName2 = "attributName2";
		String validAttributWertName1 = "attributWert1";
		String invalidAttributWertName1 = "attributWert2";
		String validAttributWert1Name2 = "attributWert21";
		String validAttributWert2Name2 = "attributWert22";
		String radvisAttribut1 = "RadvisAttribut1";
		String radvisAttribut2 = "RadvisAttribut2";

		when(attributeRepository.getAttributnamen(any())).thenReturn(Set.of(attributName1, attributName2));

		when(mapper.getImportGruppe(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(mapper.shouldFilterNullValues(any())).thenReturn(true);

		when(mapper.isAttributNameValid(attributName1)).thenReturn(true);
		when(mapper.getRadVisAttributName(attributName1)).thenReturn(radvisAttribut1);
		when(attributeRepository.getAttributWerte(any(), eq(attributName1)))
			.thenReturn(Stream.of(validAttributWertName1, invalidAttributWertName1));
		when(mapper.isAttributWertValid(attributName1, validAttributWertName1)).thenReturn(true);
		when(mapper.isAttributWertValid(attributName1, invalidAttributWertName1)).thenReturn(false);

		when(mapper.isAttributNameValid(attributName2)).thenReturn(true);
		when(mapper.getRadVisAttributName(attributName2)).thenReturn(radvisAttribut2);
		when(attributeRepository.getAttributWerte(any(), eq(attributName2)))
			.thenReturn(Stream.of(validAttributWert1Name2, validAttributWert2Name2));
		when(mapper.isAttributWertValid(attributName2, validAttributWert1Name2)).thenReturn(true);
		when(mapper.isAttributWertValid(attributName2, validAttributWert2Name2)).thenReturn(true);

		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));

		InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("shp/empty.zip");
		List<ImportierbaresAttribut> validateAttribute = manuellerAttributeImportService
			.validateAttribute(resourceAsStream.readAllBytes(), AttributeImportFormat.LUBW);

		assertThat(validateAttribute)
			.containsExactlyInAnyOrder(ImportierbaresAttribut.of(attributName1, radvisAttribut1, attributName1, false,
					Set.of(invalidAttributWertName1)),
				ImportierbaresAttribut.of(attributName2, radvisAttribut2, attributName2, true, Collections.emptySet()));
	}

	@Test
	public void validateAttribute_invalidAndValid_groupsInvalidValues()
		throws IOException, ZipFileRequiredFilesMissingException {
		LUBWMapper mapper = mock(LUBWMapper.class);
		when(mapperFactory.createMapper(any())).thenReturn(mapper);
		String attributName1 = "attributName1";
		String validAttributWertName1 = "attributWert1";
		String invalidAttributWertName1 = "invalid";
		String invalidAttributWertName2 = "invalid";
		String invalidAttributWertName3 = "auch invalid";
		String radvisAttribut1 = "RadvisAttribut1";

		when(attributeRepository.getAttributnamen(any())).thenReturn(Set.of(attributName1));

		when(mapper.getImportGruppe(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(mapper.shouldFilterNullValues(any())).thenReturn(true);

		when(mapper.isAttributNameValid(attributName1)).thenReturn(true);
		when(mapper.getRadVisAttributName(attributName1)).thenReturn(radvisAttribut1);
		when(attributeRepository.getAttributWerte(any(), eq(attributName1)))
			.thenReturn(Stream.of(validAttributWertName1, invalidAttributWertName1, invalidAttributWertName2,
				invalidAttributWertName3));
		when(mapper.isAttributWertValid(attributName1, validAttributWertName1)).thenReturn(true);
		when(mapper.isAttributWertValid(attributName1, invalidAttributWertName1)).thenReturn(false);
		when(mapper.isAttributWertValid(attributName1, invalidAttributWertName2)).thenReturn(false);
		when(mapper.isAttributWertValid(attributName1, invalidAttributWertName3)).thenReturn(false);

		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));

		InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("shp/empty.zip");
		List<ImportierbaresAttribut> validateAttribute = manuellerAttributeImportService
			.validateAttribute(resourceAsStream.readAllBytes(), AttributeImportFormat.LUBW);

		ImportierbaresAttribut expected = validateAttribute.get(0);
		assertThat(expected.getAttributName()).isEqualTo(attributName1);
		assertThat(expected.getRadvisName()).isEqualTo(radvisAttribut1);
		assertThat(expected.isValid()).isFalse();
		assertThat(expected.getUngueltigeWerte()).containsExactlyInAnyOrder(invalidAttributWertName1,
			invalidAttributWertName3);
	}

	@Test
	public void updateFeatureMapping() {
		long featureMappingId = 895465L;
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200)).id(2L).build();

		AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			organisation, List.of("Muffins", "Are Good Actually"),
			AttributeImportFormat.LUBW);

		FeatureMapping featureMapping = FeatureMappingTestDataProvider.withCoordinates(new Coordinate(110, 111),
			new Coordinate(170, 171)).id(featureMappingId).build();

		MappedGrundnetzkante mappedGrundnetzkanteOriginal = new MappedGrundnetzkante(
			GeometryTestdataProvider.createLineString(new Coordinate(110, 107), new Coordinate(140, 120)), 2L,
			GeometryTestdataProvider.createLineString(new Coordinate(110, 107), new Coordinate(140, 120)));
		featureMapping.add(mappedGrundnetzkanteOriginal);

		session.setFeatureMappings(List.of(featureMapping));

		MappedGrundnetzkante newMappedGrundnetzkante = new MappedGrundnetzkante(
			GeometryTestdataProvider.createLineString(new Coordinate(140, 120), new Coordinate(170, 150)), 3L,
			GeometryTestdataProvider.createLineString(new Coordinate(140, 120), new Coordinate(170, 150)));

		when(manuellerAttributeImportAbbildungsService.rematchFeaturemapping(featureMapping, organisation)).then(
			sth -> {
				FeatureMapping argument = sth.getArgument(0);
				argument.add(newMappedGrundnetzkante);
				return argument;
			});

		LineString updatedLS = GeometryTestdataProvider.createLineString(new Coordinate(110, 110),
			new Coordinate(140, 140), new Coordinate(170, 170));

		// act
		manuellerAttributeImportService.updateFeatureMapping(session, featureMappingId,
			updatedLS);

		// assert
		assertThat(featureMapping.getImportedLineString().getCoordinates()).containsExactly(updatedLS.getCoordinates());
		assertThat(featureMapping.getKantenAufDieGemappedWurde()).doesNotContain(mappedGrundnetzkanteOriginal);
		assertThat(featureMapping.getKantenAufDieGemappedWurde()).containsExactly(newMappedGrundnetzkante);
	}

	@Test
	public void updateFeatureMapping_requireFeatureExistsOnSession() {
		// arrange
		AttributeImportSession session = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build(),
			List.of("Muffins", "Are Good Actually"), AttributeImportFormat.LUBW);

		FeatureMapping featureMapping = FeatureMappingTestDataProvider
			.withCoordinates(new Coordinate(110, 111), new Coordinate(170, 171)).build();

		MappedGrundnetzkante mappedGrundnetzkanteOriginal = new MappedGrundnetzkante(
			GeometryTestdataProvider.createLineString(new Coordinate(110, 107), new Coordinate(140, 120)), 1L,
			GeometryTestdataProvider.createLineString(new Coordinate(110, 107), new Coordinate(140, 120)));
		featureMapping.add(mappedGrundnetzkanteOriginal);

		session.setFeatureMappings(List.of(featureMapping));

		LineString updatedLS = GeometryTestdataProvider.createLineString(new Coordinate(110, 110),
			new Coordinate(140, 140), new Coordinate(170, 170));

		// act + assert
		assertThatThrownBy(() -> manuellerAttributeImportService.updateFeatureMapping(session, 3L,
			updatedLS)).isInstanceOf(RequireViolation.class);
	}

	@Test
	void testeCreateKeinMatchingFehler() {

		FeatureMapping mitMapping = FeatureMappingTestDataProvider.withCoordinates(new Coordinate(40, 40),
			new Coordinate(80, 80)).build();
		mitMapping.add(new MappedGrundnetzkante(GeometryTestdataProvider.createLineString(), 55L,
			GeometryTestdataProvider.createLineString()));
		List<FeatureMapping> featureMappings = List.of(
			FeatureMappingTestDataProvider.withCoordinates(new Coordinate(10, 10), new Coordinate(20, 10)).build(),
			FeatureMappingTestDataProvider.withCoordinates(new Coordinate(50, 10), new Coordinate(20, 10)).build(),
			FeatureMappingTestDataProvider.withCoordinates(new Coordinate(30, 10), new Coordinate(20, 10)).build(),
			mitMapping);

		when(manuellerImportFehlerRepository.saveAll(anyList()))
			.thenAnswer(invocationOnMock -> invocationOnMock.<Iterable<ManuellerImportFehler>>getArgument(0));

		List<ManuellerImportFehler> manuelleImportKeinMatchingFehler = (List<ManuellerImportFehler>) manuellerImportFehlerRepository
			.saveAll(
				featureMappings.stream()
					.filter(fm -> fm.getKantenAufDieGemappedWurde().isEmpty())
					.map(fm -> new ManuellerImportFehler(fm.getImportedLineString(), ImportTyp.ATTRIBUTE_UEBERNEHMEN,
						startTime, benutzer, organisation))
					.collect(Collectors.toList()));

		verify(manuellerImportFehlerRepository).saveAll(manuelleImportKeinMatchingFehler);

		assertThat(manuelleImportKeinMatchingFehler)
			.extracting(ManuellerImportFehler::getImportTyp)
			.allMatch(ImportTyp.ATTRIBUTE_UEBERNEHMEN::equals, "Importtyp: Attribute Übernehmen");
		assertThat(manuelleImportKeinMatchingFehler)
			.extracting(ManuellerImportFehler::getFehlerursache)
			.allMatch(ManuellerImportFehlerursache.KEIN_MATCHING::equals,
				"ManuellerImportFehlerursache: Kein Matching");
		assertThat(manuelleImportKeinMatchingFehler)
			.extracting(ManuellerImportFehler::getKante)
			.allMatch(Optional::isEmpty, "Keine Kantenreferenz da kein Matching");
		assertThat(manuelleImportKeinMatchingFehler)
			.extracting(ManuellerImportFehler::getImportZeitpunkt)
			.allMatch(startTime::equals, "Alle Fehler haben den selben zeitstempel");
		assertThat(manuelleImportKeinMatchingFehler)
			.extracting(ManuellerImportFehler::getBenutzer)
			.allMatch(benutzer::equals, "Alle Fehler haben den Benutzer des Imports");
		assertThat(manuelleImportKeinMatchingFehler)
			.extracting(ManuellerImportFehler::getOrganisation)
			.allMatch(organisation::equals, "Alle Fehler haben die Organisation des Imports");
		assertThat(manuelleImportKeinMatchingFehler)
			.extracting(ManuellerImportFehler::getKonflikte)
			.allMatch(Optional::isEmpty);

		assertThat(manuelleImportKeinMatchingFehler)
			.extracting(ManuellerImportFehler::getOriginalGeometrie)
			.extracting(Optional::get)
			.containsExactlyInAnyOrder(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(20, 10)),
				GeometryTestdataProvider.createLineString(new Coordinate(50, 10), new Coordinate(20, 10)),
				GeometryTestdataProvider.createLineString(new Coordinate(30, 10), new Coordinate(20, 10)));

	}

	@Test
	void createManuellerImportAttributeNichtEindeutigFehler() {
		List<KantenKonfliktProtokoll> konfliktProtokolle = new ArrayList<>(List.of(
			new KantenKonfliktProtokoll(1L,
				GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(20, 10))),
			new KantenKonfliktProtokoll(2L,
				GeometryTestdataProvider.createLineString(new Coordinate(50, 10), new Coordinate(20, 10))),
			new KantenKonfliktProtokoll(3L,
				GeometryTestdataProvider.createLineString(new Coordinate(30, 10), new Coordinate(20, 10)))));

		Konflikt konflikt = new Konflikt("name", "42", Set.of("1", "2"));
		konfliktProtokolle.forEach(kKP -> kKP.add(konflikt));

		konfliktProtokolle.forEach(
			kkp -> when(kantenRepository.findById(kkp.getKanteId())).thenReturn(Optional.of(
				KanteTestDataProvider.withDefaultValues().id(kkp.getKanteId()).geometry(kkp.getKantenGeometrie())
					.build())));

		KantenKonfliktProtokoll kanteGeloescht = new KantenKonfliktProtokoll(4L,
			GeometryTestdataProvider.createLineString());
		konfliktProtokolle.add(kanteGeloescht);

		when(manuellerImportFehlerRepository.saveAll(anyList()))
			.thenAnswer(invocationOnMock -> invocationOnMock.<Iterable<ManuellerImportFehler>>getArgument(0));

		List<ManuellerImportFehler> importMitUneindeutigerAttributzuordnungFehler = (List<ManuellerImportFehler>) manuellerImportFehlerRepository
			.saveAll(
				konfliktProtokolle.stream()
					.filter(kKP -> kantenRepository.findById(kKP.getKanteId()).isPresent())
					.map(kantenKonfliktProtokoll -> kantenRepository.findById(
						kantenKonfliktProtokoll.getKanteId()).map(
						k -> new ManuellerImportFehler(k, startTime, benutzer, organisation,
							kantenKonfliktProtokoll.getKonflikte())))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.collect(Collectors.toList()));

		verify(manuellerImportFehlerRepository).saveAll(importMitUneindeutigerAttributzuordnungFehler);

		assertThat(importMitUneindeutigerAttributzuordnungFehler)
			.extracting(ManuellerImportFehler::getImportTyp)
			.allMatch(ImportTyp.ATTRIBUTE_UEBERNEHMEN::equals, "Importtyp: Attribute Übernehmen");
		assertThat(importMitUneindeutigerAttributzuordnungFehler)
			.extracting(ManuellerImportFehler::getFehlerursache)
			.allMatch(ManuellerImportFehlerursache.ATTRIBUTE_NICHT_EINDEUTIG::equals,
				"ManuellerImportFehlerursache: Attribute nicht eindeutig");
		assertThat(importMitUneindeutigerAttributzuordnungFehler)
			.extracting(ManuellerImportFehler::getKante)
			.extracting(Optional::get)
			.extracting(Kante::getId).containsExactly(1L, 2L, 3L);
		assertThat(importMitUneindeutigerAttributzuordnungFehler)
			.extracting(ManuellerImportFehler::getImportZeitpunkt)
			.allMatch(startTime::equals, "Alle Fehler haben den selben zeitstempel");
		assertThat(importMitUneindeutigerAttributzuordnungFehler)
			.extracting(ManuellerImportFehler::getBenutzer)
			.allMatch(benutzer::equals, "Alle Fehler haben den Benutzer des Imports");
		assertThat(importMitUneindeutigerAttributzuordnungFehler)
			.extracting(ManuellerImportFehler::getOrganisation)
			.allMatch(organisation::equals, "Alle Fehler haben die Organisation des Imports");
		assertThat(importMitUneindeutigerAttributzuordnungFehler
			.stream()
			.map(ManuellerImportFehler::getKonflikte)
			.map(Optional::get))
			.containsExactly(Set.of(konflikt), Set.of(konflikt), Set.of(konflikt));

		assertThat(importMitUneindeutigerAttributzuordnungFehler)
			.extracting(ManuellerImportFehler::getOriginalGeometrie)
			.allMatch(Optional::isEmpty);
	}

}
