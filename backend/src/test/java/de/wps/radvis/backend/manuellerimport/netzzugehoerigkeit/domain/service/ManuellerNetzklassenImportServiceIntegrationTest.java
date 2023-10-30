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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.exception.ZipFileRequiredFilesMissingException;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.ShapeFileRepositoryImpl;
import de.wps.radvis.backend.manuellerimport.common.domain.exception.GeometryTypeMismatchException;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ImportSessionRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AutomatischerImportSchritt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionStatus;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklasseImportSession;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklassenImportSessionTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.quellimport.common.schnittstelle.repositoryImpl.FeatureImportRepositoryImpl;
import jakarta.persistence.EntityManager;

@ExtendWith(OutputCaptureExtension.class)
class ManuellerNetzklassenImportServiceIntegrationTest {

	FeatureImportRepositoryImpl featureImportRepositoryImpl;

	@Mock
	ManuellerNetzklassenImportAbbildungsService manuellerNetzklassenImportAbbildungsService;

	@Mock
	ImportSessionRepository netzklassenImportSessionRepository;

	@Mock
	ManuellerImportFehlerRepository manuellerImportFehlerRepository;

	@Mock
	Benutzer benutzer;

	@Mock
	NetzService netzService;

	@Mock
	EntityManager entityManager;

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95))
	);

	ManuellerNetzklassenImportService manuellerNetzklassenImportService;

	ManuellerNetzklassenImportUebernahmeService manuellerNetzklassenImportUebernahmeService;

	ManuellerImportService manuellerImportService;

	private ShapeZipService zipService;

	@BeforeEach
	void setup() throws GeometryTypeMismatchException {
		MockitoAnnotations.openMocks(this);

		featureImportRepositoryImpl = new FeatureImportRepositoryImpl(coordinateReferenceSystemConverter, null);
		zipService = new ShapeZipService();
		ShapeFileRepository shapeFileRepository = new ShapeFileRepositoryImpl(coordinateReferenceSystemConverter);
		manuellerImportService = new ManuellerImportService(netzklassenImportSessionRepository, zipService,
			shapeFileRepository, manuellerImportFehlerRepository);
		manuellerNetzklassenImportUebernahmeService = new ManuellerNetzklassenImportUebernahmeService(netzService,
			entityManager);

		manuellerNetzklassenImportService = new ManuellerNetzklassenImportService(manuellerImportService,
			manuellerNetzklassenImportAbbildungsService,
			manuellerNetzklassenImportUebernahmeService,
			zipService, shapeFileRepository, manuellerImportFehlerRepository);
		when(benutzer.getServiceBwId()).thenReturn(ServiceBwId.of("testId"));
		when(benutzer.getOrganisation()).thenReturn(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		when(manuellerNetzklassenImportAbbildungsService.extractLinestring(any())).thenCallRealMethod();
	}

	@Test
	void testImportNetzklassen_zipMitKorrekterShapeFile_zipWirdEntpacktUndkorrekteLineStringsWerdenAusgelesen(
		CapturedOutput output)
		throws IOException, ZipFileRequiredFilesMissingException {
		// arrange
		File testLineStringsFile = new File("src/test/resources/shp/Bodenseekreis_Kreisnetz.zip");

		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(), any()))
			.thenReturn(
				new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(11L, 22L, 33L), Set.of()));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100000000, 10000000))
			.build();
		File shpDirectory = zipService.unzip(
			Files.readAllBytes(testLineStringsFile.toPath()));
		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(benutzer).organisation(organisation).netzklasse(Netzklasse.RADVORRANGROUTEN).build();

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(netzklasseImportSession, shpDirectory);

		// assert
		assertThat(netzklasseImportSession.hatFehler()).isFalse();
		assertThat(netzklasseImportSession.getAktuellerImportSchritt()).isEqualTo(
			AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN);
		assertThat(netzklasseImportSession.getStatus()).isEqualTo(
			ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		assertThat(output).containsPattern("ZipFile nach (.*) entpackt");
		assertThat(output).contains(
			"8621 LineStrings aus der Shapefile befinden sich im Bereich der gewählten Organisation (DefaultOrganisation)");
		assertThat(netzklasseImportSession.getKanteIds()).containsExactlyInAnyOrder(11L, 22L, 33L);
	}

	@Test
	void testImportNetzklassen_zipEnthaeltStoredEntries_zipWirdEntpacktUndkorrekteLineStringsWerdenAusgelesen(
		CapturedOutput output)
		throws IOException, ZipFileRequiredFilesMissingException {
		// arrange
		File testLineStringsFile = new File("src/test/resources/shp/test_attribute.zip");

		when(manuellerNetzklassenImportAbbildungsService.findKantenFromLineStrings(any(), any()))
			.thenReturn(
				new ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis(Set.of(11L, 22L, 33L), Set.of()));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100000000, 10000000))
			.build();
		File shpDirectory = zipService.unzip(
			Files.readAllBytes(testLineStringsFile.toPath()));
		NetzklasseImportSession netzklasseImportSession = NetzklassenImportSessionTestDataProvider
			.forBenutzer(benutzer).organisation(organisation).netzklasse(Netzklasse.RADVORRANGROUTEN).build();

		// act
		manuellerNetzklassenImportService.runAutomatischeAbbildung(netzklasseImportSession, shpDirectory);

		// assert
		assertThat(netzklasseImportSession.hatFehler()).isFalse();
		assertThat(netzklasseImportSession.getAktuellerImportSchritt()).isEqualTo(
			AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN);
		assertThat(netzklasseImportSession.getStatus()).isEqualTo(
			ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		assertThat(output).containsPattern("ZipFile nach (.*) entpackt");
		assertThat(output).contains(
			"3 LineStrings aus der Shapefile befinden sich im Bereich der gewählten Organisation (DefaultOrganisation)");
		assertThat(netzklasseImportSession.getKanteIds()).containsExactlyInAnyOrder(11L, 22L, 33L);
	}
}
