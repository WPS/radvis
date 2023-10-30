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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.exception.ZipFileRequiredFilesMissingException;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.exception.GeometryTypeMismatchException;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AutomatischerImportSchritt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag.Severity;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionStatus;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ManuellerImportFehlerursache;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklasseImportSession;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklassenImportSessionTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;

@ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings({ "rawtypes", "unchecked" })
class ManuellerNetzklassenImportServiceTest {

	@Mock
	ManuellerNetzklassenImportAbbildungsService manuellerNetzklassenImportAbbildungsService;

	@Mock
	NetzService netzService;

	@Mock
	Benutzer benutzer;

	@Captor
	ArgumentCaptor<Set<LineString>> lineStringSetCaptor;

	ManuellerNetzklassenImportService manuellerNetzklassenImportService;

	ManuellerNetzklassenImportUebernahmeService manuellerNetzklassenImportUebernahmeService;

	@Mock
	EntityManager entityManager;

	@Mock
	ManuellerImportService manuellerImportService;

	@Mock
	ManuellerImportFehlerRepository manuellerImportFehlerRepository;

	@Mock
	ShapeZipService shapeZipServiceMock;

	@Mock
	ShapeFileRepository shapeFileRepository;

	@BeforeEach
	void setup() throws GeometryTypeMismatchException, IOException, ZipFileRequiredFilesMissingException {
		MockitoAnnotations.openMocks(this);
		manuellerNetzklassenImportUebernahmeService = new ManuellerNetzklassenImportUebernahmeService(netzService,
			entityManager);
		manuellerNetzklassenImportService = new ManuellerNetzklassenImportService(manuellerImportService,
			manuellerNetzklassenImportAbbildungsService,
			manuellerNetzklassenImportUebernahmeService, shapeZipServiceMock, shapeFileRepository,
			manuellerImportFehlerRepository);
		when(benutzer.getServiceBwId()).thenReturn(ServiceBwId.of("testId"));
		when(benutzer.getOrganisation()).thenReturn(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		when(manuellerNetzklassenImportAbbildungsService.extractLinestring(any())).thenCallRealMethod();
		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));
	}

	@Test
	void testImportNetzklassen_korrekteZip_lineStringsWerdenAusgelesenUndAnhandDesOrganisationsbereichesGefiltert()
		throws IOException, ZipFileRequiredFilesMissingException,
		ShapeProjectionException {
		// arrange
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(LineString.class).buildFeatureType());

		LineString linestring = GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(10, 10));
		featureBuilder.add(linestring);
		SimpleFeature l1Feature = featureBuilder.buildFeature("id1");

		LineString linestring2 = GeometryTestdataProvider.createLineString(new Coordinate(99, 99),
			new Coordinate(110, 110));
		featureBuilder.add(linestring2);
		SimpleFeature l2Feature = featureBuilder.buildFeature("id2");

		LineString linestring3 = GeometryTestdataProvider.createLineString(new Coordinate(101, 101),
			new Coordinate(120, 120));
		featureBuilder.add(linestring3);
		SimpleFeature l3Feature = featureBuilder.buildFeature("id3");

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(l1Feature, l2Feature, l3Feature));

		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
			.build();

		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(benutzer).organisation(organisation).netzklasse(Netzklasse.RADVORRANGROUTEN).build();

		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(),
			eq(netzklasseImportSession.getOrganisation()))).thenReturn(
			new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(11L, 22L, 33L), Set.of()));

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(netzklasseImportSession, new File(""));

		// assert
		verify(manuellerNetzklassenImportAbbildungsService, times(1)).findKantenFromLineStrings(
			lineStringSetCaptor.capture(),
			eq(netzklasseImportSession.getOrganisation()));

		assertThat(lineStringSetCaptor.getValue()).containsExactlyInAnyOrder(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(10, 10)),
			GeometryTestdataProvider.createLineString(new Coordinate(99, 99), new Coordinate(110, 110)));
		assertThat(netzklasseImportSession.hatFehler()).isFalse();
		assertThat(netzklasseImportSession.getAktuellerImportSchritt()).isEqualTo(
			AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN);
		assertThat(netzklasseImportSession.getKanteIds()).containsExactlyInAnyOrder(11L, 22L, 33L);
		assertThat(netzklasseImportSession.getNetzklasse()).isEqualTo(Netzklasse.RADVORRANGROUTEN);
	}

	@Test
	void testImportNetzklassen_1emptyGeometry_1valid()
		throws IOException, ZipFileRequiredFilesMissingException,
		ShapeProjectionException {
		// arrange
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(LineString.class).buildFeatureType());

		featureBuilder.add(null);
		SimpleFeature l1Feature = featureBuilder.buildFeature("id1");

		LineString linestring2 = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(10, 10));
		featureBuilder.add(linestring2);
		SimpleFeature l2Feature = featureBuilder.buildFeature("id2");

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(l1Feature, l2Feature));

		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
			.build();

		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(benutzer).organisation(organisation).netzklasse(Netzklasse.RADVORRANGROUTEN).build();

		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(),
			eq(netzklasseImportSession.getOrganisation()))).thenReturn(
			new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(22L), Set.of()));

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(netzklasseImportSession, new File(""));

		// assert
		verify(manuellerNetzklassenImportAbbildungsService, times(1)).findKantenFromLineStrings(
			lineStringSetCaptor.capture(),
			eq(netzklasseImportSession.getOrganisation()));

		assertThat(lineStringSetCaptor.getValue()).containsExactlyInAnyOrder(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(10, 10)));

		assertThat(netzklasseImportSession.getLog()).hasSize(1);
		assertThat(netzklasseImportSession.getLog().get(0).getFehlerBeschreibung())
			.isEqualTo(
				"Shapefile enthält 1 Features ohne Geometrien, welche aus diesem Grund nicht importiert werden konnten.");
	}

	@Test
	void testImportNetzklassen_korrekteZip_setStatusWirdAufgerufen() throws Exception {
		// arrange
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(LineString.class).buildFeatureType());

		LineString linestring = GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(10, 10));
		featureBuilder.add(linestring);
		SimpleFeature l1Feature = featureBuilder.buildFeature("id1");

		LineString linestring2 = GeometryTestdataProvider.createLineString(new Coordinate(99, 99),
			new Coordinate(110, 110));
		featureBuilder.add(linestring2);
		SimpleFeature l2Feature = featureBuilder.buildFeature("id2");

		LineString linestring3 = GeometryTestdataProvider.createLineString(new Coordinate(101, 101),
			new Coordinate(120, 120));
		featureBuilder.add(linestring3);
		SimpleFeature l3Feature = featureBuilder.buildFeature("id3");

		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(l1Feature, l2Feature, l3Feature));

		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
			.build();

		NetzklasseImportSession sessionMock = Mockito.mock(NetzklasseImportSession.class);
		when(sessionMock.getOrganisation()).thenReturn(organisation);

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(sessionMock, new File(""));

		// assert
		verify(sessionMock, times(1)).setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_RUNNING);
		verify(sessionMock, times(1)).setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
	}

	@Test
	void testImportNetzklassen_unerwarteteException_aufSessionGeschrieben()
		throws IOException, ShapeProjectionException {
		// arrange
		when(shapeFileRepository.readShape(any()))
			.thenThrow(new IOException("Es ist ein unerwarteter Fehler aufgetreten."));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
			.build();

		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(benutzer).organisation(organisation).netzklasse(Netzklasse.RADVORRANGROUTEN).build();
		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(),
			eq(netzklasseImportSession.getOrganisation()))).thenReturn(
			new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(11L, 22L, 33L), Set.of()));

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(netzklasseImportSession, new File(""));

		// assert
		assertThat(netzklasseImportSession.getStatus())
			.isEqualTo(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		assertThat(netzklasseImportSession.getLog()).hasSize(1);
		assertThat(netzklasseImportSession.getLog().get(0).getFehlerBeschreibung())
			.isEqualTo("Es ist ein unerwarteter Fehler aufgetreten.");
	}

	@Test
	void testRunAutomatischeAbbildung_RequireViolation_aufSessionGeschrieben()
		throws IOException, ZipFileRequiredFilesMissingException, ShapeProjectionException {
		// arrange
		File testLineStringsFile = new File("src/test/resources/shp/Bodenseekreis_Kreisnetz.zip");

		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(LineString.class).buildFeatureType());

		LineString linestring = GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(10, 10));
		featureBuilder.add(linestring);

		SimpleFeature l1Feature = featureBuilder.buildFeature("id1");
		when(shapeFileRepository.readShape(any())).thenReturn(Stream.of(l1Feature));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
			.build();

		File shpDirectory = shapeZipServiceMock.unzip(Files.readAllBytes(testLineStringsFile.toPath()));
		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(benutzer).organisation(organisation).netzklasse(Netzklasse.RADVORRANGROUTEN).build();

		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(),
			eq(netzklasseImportSession.getOrganisation()))).thenThrow(new RequireViolation("wambo!"));

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(netzklasseImportSession, shpDirectory);

		// assert
		assertThat(netzklasseImportSession.getLog()).contains(
			ImportLogEintrag.ofError("Es ist ein unerwarteter Fehler aufgetreten."));
		assertThat(netzklasseImportSession.getStatus()).isEqualTo(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
	}

	@Test
	public void testeExecuteSession_sessionWurdeSchonExecuted_WirftException() {
		// arrange
		NetzklasseImportSession session = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer()
				.build())
			.netzklasse(Netzklasse.RADVORRANGROUTEN).build();

		session.setStatus(ImportSessionStatus.UPDATE_DONE);

		// act + assert
		assertThatThrownBy(() -> manuellerNetzklassenImportService.runUpdate(session)).isInstanceOf(
				RequireViolation.class)
			.hasMessage("Eine Session darf nur nach der automatischen Abbildung und nur einmal ausgeführt werden");
	}

	@Test
	public void testeExecuteSession_sessionWirdGeradeExecuted_WirftException() {
		// arrange
		NetzklasseImportSession session = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer()
				.build())
			.netzklasse(Netzklasse.RADVORRANGROUTEN).build();

		session.setStatus(ImportSessionStatus.UPDATE_EXECUTING);

		// act + assert
		assertThatThrownBy(() -> manuellerNetzklassenImportService.runUpdate(session)).isInstanceOf(
				RequireViolation.class)
			.hasMessage("Eine Session darf nur nach der automatischen Abbildung und nur einmal ausgeführt werden");
	}

	@Test
	public void testeExecuteSession_automatischeAbbildungLaeuftNoch_WirftException() {
		// arrange
		NetzklasseImportSession session = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer()
				.build())
			.netzklasse(Netzklasse.RADVORRANGROUTEN).build();

		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_RUNNING);

		// act + assert
		assertThatThrownBy(() -> manuellerNetzklassenImportService.runUpdate(session)).isInstanceOf(
				RequireViolation.class)
			.hasMessage("Eine Session darf nur nach der automatischen Abbildung und nur einmal ausgeführt werden");
	}

	@Test
	public void testeExecuteSession_sessionPasstNetzklassenInBereichAn() {
		// arrange
		Verwaltungseinheit zustaendigeOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(5L)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 400, 400))
			.build();

		Kante radNETZKante = KanteTestDataProvider.withDefaultValues()
			.id(5L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT)).build())
			.build();

		Kante hatNetzklasseSchon = KanteTestDataProvider.withDefaultValues()
			.id(10L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG)).build())
			.build();

		Kante hatNetzklasseNochNicht = KanteTestDataProvider.withDefaultValues()
			.id(15L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of()).build())
			.build();

		Kante hatNetzklasseUndSollEntferntWerden = KanteTestDataProvider.withDefaultValues()
			.id(20L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG)).build())
			.build();

		when(netzService.getKantenInOrganisationsbereichEagerFetchNetzklassen(zustaendigeOrganisation))
			.thenReturn(
				Set.of(radNETZKante, hatNetzklasseSchon, hatNetzklasseNochNicht, hatNetzklasseUndSollEntferntWerden));

		NetzklasseImportSession session = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer()
				.build())
			.organisation(zustaendigeOrganisation)
			.netzklasse(Netzklasse.KOMMUNALNETZ_ALLTAG)
			.build();

		session.getKanteIds().add(5L);
		session.getKanteIds().add(10L);
		session.getKanteIds().add(15L);

		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);

		// act
		manuellerNetzklassenImportService.runUpdate(session);

		// assert
		assertThat(radNETZKante.getKantenAttributGruppe().getNetzklassen())
			.containsExactlyInAnyOrder(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT,
				Netzklasse.KOMMUNALNETZ_ALLTAG);
		assertThat(hatNetzklasseSchon.getKantenAttributGruppe().getNetzklassen())
			.containsExactlyInAnyOrder(Netzklasse.KOMMUNALNETZ_ALLTAG);
		assertThat(hatNetzklasseNochNicht.getKantenAttributGruppe().getNetzklassen())
			.containsExactlyInAnyOrder(Netzklasse.KOMMUNALNETZ_ALLTAG);
		assertThat(hatNetzklasseUndSollEntferntWerden.getKantenAttributGruppe().getNetzklassen())
			.isEmpty();
	}

	@Test
	public void testeExecuteSession_sessionPasstNetzklassenInBereichAn_istProjektion() {
		// arrange
		Verwaltungseinheit zustaendigeOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(5L)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 400, 400))
			.build();

		Kante radNETZKante = KanteTestDataProvider.withDefaultValues()
			.id(5L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT)).build())
			.build();

		Kante hatNetzklasseUndSollEntferntWerden = KanteTestDataProvider.withDefaultValues()
			.id(20L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG)).build())
			.build();

		when(netzService.getKantenInOrganisationsbereichEagerFetchNetzklassen(zustaendigeOrganisation))
			.thenReturn(Set.of(radNETZKante, hatNetzklasseUndSollEntferntWerden));

		NetzklasseImportSession session = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer()
				.build())
			.organisation(zustaendigeOrganisation)
			.netzklasse(Netzklasse.KOMMUNALNETZ_ALLTAG)
			.build();

		session.getKanteIds().add(5L);
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);

		// act + assert
		manuellerNetzklassenImportService.runUpdate(session);

		assertThat(radNETZKante.getKantenAttributGruppe().getNetzklassen())
			.containsExactlyInAnyOrder(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT,
				Netzklasse.KOMMUNALNETZ_ALLTAG);
		assertThat(hatNetzklasseUndSollEntferntWerden.getKantenAttributGruppe().getNetzklassen())
			.isEmpty();

		// projectionstest:
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		when(netzService.getKantenInOrganisationsbereichEagerFetchNetzklassen(zustaendigeOrganisation))
			.thenReturn(Set.of(radNETZKante, hatNetzklasseUndSollEntferntWerden));
		manuellerNetzklassenImportService.runUpdate(session);

		assertThat(radNETZKante.getKantenAttributGruppe().getNetzklassen())
			.containsExactlyInAnyOrder(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT,
				Netzklasse.KOMMUNALNETZ_ALLTAG);
		assertThat(hatNetzklasseUndSollEntferntWerden.getKantenAttributGruppe().getNetzklassen())
			.isEmpty();
	}

	@Test
	public void testeExecuteSession_ignoriertNetzklassenVollstaendigAusserhalbVonBereich() {
		// arrange
		Verwaltungseinheit zustaendigeOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(5L)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 400, 400))
			.build();

		Kante teilweiseInnerhalb = KanteTestDataProvider.withDefaultValues()
			.id(10L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(399.5, 399.5), new Coordinate(420, 401)))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of()).build())
			.build();

		Kante innerhalb = KanteTestDataProvider.withDefaultValues()
			.id(5L)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT)).build())
			.build();

		when(netzService.getKantenInOrganisationsbereichEagerFetchNetzklassen(zustaendigeOrganisation))
			.thenReturn(Set.of(innerhalb, teilweiseInnerhalb));

		NetzklasseImportSession session = NetzklassenImportSessionTestDataProvider
			.forBenutzer(BenutzerTestDataProvider.defaultBenutzer()
				.build())
			.organisation(zustaendigeOrganisation)
			.netzklasse(Netzklasse.KOMMUNALNETZ_ALLTAG)
			.build();

		session.getKanteIds().add(5L);
		session.getKanteIds().add(10L);
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);

		// act
		manuellerNetzklassenImportService.runUpdate(session);

		// assert
		assertThat(innerhalb.getKantenAttributGruppe().getNetzklassen())
			.containsExactlyInAnyOrder(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT,
				Netzklasse.KOMMUNALNETZ_ALLTAG);
		assertThat(teilweiseInnerhalb.getKantenAttributGruppe().getNetzklassen())
			.containsExactlyInAnyOrder(Netzklasse.KOMMUNALNETZ_ALLTAG);
	}

	@Nested
	class StatusTests {

		@Mock
		ManuellerNetzklassenImportUebernahmeService manuellerNetzklassenImportUebernahmeService;

		@BeforeEach
		void setUp() {
			MockitoAnnotations.openMocks(this);
			manuellerNetzklassenImportService = new ManuellerNetzklassenImportService(manuellerImportService,
				manuellerNetzklassenImportAbbildungsService,
				manuellerNetzklassenImportUebernahmeService, shapeZipServiceMock, shapeFileRepository,
				manuellerImportFehlerRepository);
		}

		@Test
		public void testeExecuteSession_optimisticLockException_KannErneutAusgefuehrtWerden() {
			// arrange
			doThrow(OptimisticLockException.class).when(manuellerNetzklassenImportUebernahmeService)
				.uebernehmeNetzzugehoerigkeit(any());

			NetzklasseImportSession session = NetzklassenImportSessionTestDataProvider
				.forBenutzer(BenutzerTestDataProvider.defaultBenutzer()
					.build())
				.netzklasse(Netzklasse.KOMMUNALNETZ_ALLTAG)
				.build();
			session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);

			// act
			assertThatThrownBy(() -> manuellerNetzklassenImportService.runUpdate(session)).isInstanceOf(
					OptimisticLockException.class)
				.hasMessage(
					"Kanten in der Organisation wurden während des Speicherns aus einer anderen Quelle verändert."
						+ " Bitte versuchen Sie es erneut.");

			// assert
			assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
			assertThat(session.getLog()).isEmpty();
		}

		@Test
		public void testeExecuteSession_andereUnerwarteteException_KannNichtErneutAusgefuehrtWerden() {
			// arrange
			doThrow(RequireViolation.class).when(manuellerNetzklassenImportUebernahmeService)
				.uebernehmeNetzzugehoerigkeit(any());

			NetzklasseImportSession session = NetzklassenImportSessionTestDataProvider
				.forBenutzer(BenutzerTestDataProvider.defaultBenutzer()
					.build())
				.netzklasse(Netzklasse.KOMMUNALNETZ_ALLTAG)
				.build();
			session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);

			// act
			assertThatThrownBy(() -> manuellerNetzklassenImportService.runUpdate(session)).isInstanceOf(
				RequireViolation.class);

			// assert
			assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.UPDATE_DONE);
			assertThat(session.getLog()).containsExactly(
				ImportLogEintrag.ofError("Es ist ein Unbekannter Fehler aufgetreten"));
		}
	}

	@Test
	void noFeatures_warn() throws IOException, ZipFileRequiredFilesMissingException, ShapeProjectionException {
		// arrange
		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));
		when(shapeFileRepository.readShape(any())).thenReturn(Stream.empty());
		NetzklasseImportSession sessionMock = mock(NetzklasseImportSession.class);
		ArgumentCaptor<ImportLogEintrag> logEintragArgument = ArgumentCaptor.forClass(ImportLogEintrag.class);
		when(sessionMock.getOrganisation()).thenReturn(mock(Verwaltungseinheit.class));

		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(), any())).thenReturn(
			new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(), Set.of()));

		// act + assert
		assertThatNoException().isThrownBy(
			() -> manuellerNetzklassenImportService.runAutomatischeAbbildung(sessionMock, new File("")));
		verify(sessionMock).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
	}

	@Test
	void noFeaturesInBereich_warn() throws IOException, ZipFileRequiredFilesMissingException, ShapeProjectionException {
		// arrange
		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));
		when(shapeFileRepository.readShape(any()))
			.thenReturn(Stream.of(SimpleFeatureTestDataProvider.defaultFeature()));
		NetzklasseImportSession sessionMock = mock(NetzklasseImportSession.class);
		ArgumentCaptor<ImportLogEintrag> logEintragArgument = ArgumentCaptor.forClass(ImportLogEintrag.class);
		Verwaltungseinheit organisationMock = mock(Verwaltungseinheit.class);
		MultiPolygon zustaendigkeitsBereichMock = mock(MultiPolygon.class);
		when(zustaendigkeitsBereichMock.intersects(any())).thenReturn(false);
		when(organisationMock.getBereich()).thenReturn(Optional.of(zustaendigkeitsBereichMock));
		when(sessionMock.getOrganisation()).thenReturn(organisationMock);

		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(), any())).thenReturn(
			new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(), Set.of()));

		// act + assert
		assertThatNoException().isThrownBy(
			() -> manuellerNetzklassenImportService.runAutomatischeAbbildung(sessionMock, new File("")));
		verify(sessionMock).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
	}

	@Test
	void filterZustaendigkeitsbereich()
		throws IOException, ZipFileRequiredFilesMissingException, ShapeProjectionException {
		// arrange
		when(shapeZipServiceMock.unzip(any())).thenReturn(new File(""));
		when(shapeZipServiceMock.getShapeFileFromDirectory(any())).thenReturn(Optional.of(new File("")));
		when(shapeFileRepository.readShape(any()))
			.thenReturn(
				Stream.of(SimpleFeatureTestDataProvider.withLineString(new Coordinate(0, 0), new Coordinate(0, 10))));
		NetzklasseImportSession sessionMock = mock(NetzklasseImportSession.class);
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
		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(), any())).thenReturn(
			new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(), Set.of()));

		// act + assert
		assertThatNoException().isThrownBy(
			() -> manuellerNetzklassenImportService.runAutomatischeAbbildung(sessionMock, new File("")));
		verify(sessionMock).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
	}

	@Test
	void noLinestring_warn() throws IOException, ShapeProjectionException {
		// arrange
		Polygon polygon = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(1, 1),
				new Coordinate(1, 0), new Coordinate(0, 0) });
		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(Polygon.class).buildFeatureType());
		f1.add(polygon);
		when(shapeFileRepository.readShape(any()))
			.thenReturn(Stream.of(f1.buildFeature("test")));
		NetzklasseImportSession sessionMock = mock(NetzklasseImportSession.class);
		Verwaltungseinheit organisationMock = mock(Verwaltungseinheit.class);
		MultiPolygon zustaendigkeitsBereichMock = mock(MultiPolygon.class);
		when(zustaendigkeitsBereichMock.intersects(any())).thenReturn(true);
		when(organisationMock.getBereich()).thenReturn(Optional.of(zustaendigkeitsBereichMock));
		when(sessionMock.getOrganisation()).thenReturn(organisationMock);
		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(), any())).thenReturn(
			new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(), Set.of()));

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(sessionMock,
			new File(""));

		// assert
		ArgumentCaptor<ImportLogEintrag> logEintragArgument = ArgumentCaptor.forClass(ImportLogEintrag.class);
		verify(sessionMock).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
		verify(manuellerNetzklassenImportAbbildungsService, times(1)).findKantenFromLineStrings(
			lineStringSetCaptor.capture(),
			any());
		assertThat(lineStringSetCaptor.getValue()).isEmpty();
	}

	@Test
	void multilinestring_multiple_warn() throws IOException, ShapeProjectionException {
		// arrange
		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(
			SimpleFeatureTestDataProvider.typeWithGeometry(MultiLineString.class).buildFeatureType());
		LineString linestring = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { new Coordinate(0, 1), new Coordinate(1, 1) });

		MultiLineString multiLineStringSingle = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createMultiLineString(new LineString[] { linestring });
		f1.add(multiLineStringSingle);
		String keepId = "keep";
		SimpleFeature featureKeep = f1.buildFeature(keepId);

		String filteredId = "filter";
		MultiLineString multiLineStringMultiple = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createMultiLineString(new LineString[] { linestring, linestring });
		f1.add(multiLineStringMultiple);
		SimpleFeature featureFiltered = f1.buildFeature(filteredId);

		when(shapeFileRepository.readShape(any()))
			.thenReturn(Stream.of(featureKeep, featureFiltered));

		NetzklasseImportSession sessionMock = mock(NetzklasseImportSession.class);
		Verwaltungseinheit organisationMock = mock(Verwaltungseinheit.class);
		MultiPolygon zustaendigkeitsBereichMock = mock(MultiPolygon.class);
		when(zustaendigkeitsBereichMock.intersects(any())).thenReturn(true);
		when(organisationMock.getBereich()).thenReturn(Optional.of(zustaendigkeitsBereichMock));
		when(sessionMock.getOrganisation()).thenReturn(organisationMock);

		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(), any())).thenReturn(
			new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(), Set.of()));

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(sessionMock,
			new File(""));

		// assert
		ArgumentCaptor<ImportLogEintrag> logEintragArgument = ArgumentCaptor.forClass(ImportLogEintrag.class);
		verify(sessionMock, times(1)).addLogEintrag(logEintragArgument.capture());
		assertThat(logEintragArgument.getValue().getSeverity()).isEqualTo(Severity.WARN);
		verify(manuellerNetzklassenImportAbbildungsService, times(1)).findKantenFromLineStrings(
			lineStringSetCaptor.capture(),
			any());
		assertThat(lineStringSetCaptor.getValue()).containsExactly(linestring);
	}

	@Test
	void nichtGematchteLineStrings_werdenInDieSessionGeschrieben() {
		// arrange
		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(benutzer)
			.organisation(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
				.build())
			.netzklasse(Netzklasse.RADVORRANGROUTEN).build();

		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(),
			eq(netzklasseImportSession.getOrganisation()))).thenReturn(
			new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(
				Set.of(),
				Set.of(
					GeometryTestdataProvider.createLineString(new Coordinate(99, 99), new Coordinate(110, 110)))));

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(netzklasseImportSession, new File(""));

		// assert
		assertThat(netzklasseImportSession.getNichtGematchteFeatureLineStrings()).containsExactly(
			GeometryTestdataProvider.createLineString(new Coordinate(99, 99), new Coordinate(110, 110)));
	}

	@Test
	void fehlerProtokolleWerdenErstelltFuerNichtGematchteLineStrings() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
			.build();
		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(benutzer)
			.organisation(organisation)
			.netzklasse(Netzklasse.RADVORRANGROUTEN)
			.build();
		netzklasseImportSession.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		netzklasseImportSession.addNichtGematchteFeatureLineStrings(
			Set.of(GeometryTestdataProvider.createLineString(new Coordinate(99, 99), new Coordinate(110, 110))));

		// act
		manuellerNetzklassenImportService.runUpdate(netzklasseImportSession);

		// assert
		ArgumentCaptor<List<ManuellerImportFehler>> captor = ArgumentCaptor.forClass(List.class);
		verify(manuellerImportFehlerRepository).saveAll(captor.capture());

		assertThat(captor.getValue().size()).isEqualTo(1);
		ManuellerImportFehler manuellerImportFehler = captor.getValue().get(0);
		assertThat(manuellerImportFehler.getKante()).isEqualTo(Optional.empty());
		assertThat((LineString) manuellerImportFehler.getOriginalGeometrie().get()).isEqualTo(
			GeometryTestdataProvider.createLineString(new Coordinate(99, 99), new Coordinate(110, 110)));
		assertThat(manuellerImportFehler.getImportTyp()).isEqualTo(ImportTyp.NETZKLASSE_ZUWEISEN);
		assertThat(manuellerImportFehler.getBenutzer()).isEqualTo(benutzer);
		assertThat(manuellerImportFehler.getOrganisation()).isEqualTo(organisation);
		assertThat(manuellerImportFehler.getFehlerursache()).isEqualTo(ManuellerImportFehlerursache.KEIN_MATCHING);
		assertThat(manuellerImportFehler.getKonflikte()).isEqualTo(Optional.empty());
	}

	@Test
	void testeCreateKeinMatchingFehler() {
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().id(10L).build();
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(15L)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100)).build();
		LocalDateTime startTime = LocalDateTime.of(2022, 9, 28, 10, 6);

		List<LineString> lineStrings = List.of(
			GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(20, 10)),
			GeometryTestdataProvider.createLineString(new Coordinate(50, 10), new Coordinate(20, 10)),
			GeometryTestdataProvider.createLineString(new Coordinate(30, 10), new Coordinate(20, 10)));

		when(manuellerImportFehlerRepository.saveAll(anyList()))
			.thenAnswer(invocationOnMock -> invocationOnMock.<Iterable<ManuellerImportFehler>>getArgument(0));

		List<ManuellerImportFehler> manuelleImportKeinMatchingFehler = (List<ManuellerImportFehler>) this.manuellerImportFehlerRepository
			.saveAll(
				lineStrings.stream()
					.map(
						ls -> new ManuellerImportFehler(ls,
							ImportTyp.NETZKLASSE_ZUWEISEN, startTime, benutzer, organisation))
					.collect(Collectors.toList()));

		assertThat(manuelleImportKeinMatchingFehler)
			.extracting(ManuellerImportFehler::getImportTyp)
			.allMatch(ImportTyp.NETZKLASSE_ZUWEISEN::equals, "Importtyp: Attribute Übernehmen");
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
			.containsExactlyInAnyOrderElementsOf(lineStrings);
	}
}
