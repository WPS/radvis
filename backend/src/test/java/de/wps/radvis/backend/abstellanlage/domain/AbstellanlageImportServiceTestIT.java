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

package de.wps.radvis.backend.abstellanlage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.schnittstelle.CSVExportConverter;
import de.wps.radvis.backend.abstellanlage.AbstellanlageConfiguration;
import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.entity.provider.AbstellanlageTestDataProvider;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenStatus;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.GebuehrenProMonat;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.exception.CsvImportException;
import de.wps.radvis.backend.common.domain.exception.CsvReadException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.CsvRepositoryImpl;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group6")
@ContextConfiguration(classes = { AbstellanlageConfiguration.class, DokumentConfiguration.class })
@EnableConfigurationProperties(value = CommonConfigurationProperties.class)
@MockBeans({
	@MockBean(BenutzerResolver.class),
})
class AbstellanlageImportServiceTestIT extends DBIntegrationTestIT {
	private static final String BASE_URL = "https://radvis-dev.landbw.de/";

	AbstellanlageImportService abstellanlageImportService;
	@Autowired
	AbstellanlageRepository abstellanlageRepository;
	@MockBean
	VerwaltungseinheitService verwaltungseinheitService;
	@MockBean
	ZustaendigkeitsService zustaendigkeitsService;

	Benutzer adminBenutzer;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		abstellanlageImportService = new AbstellanlageImportService(abstellanlageRepository, verwaltungseinheitService,
			zustaendigkeitsService, BASE_URL);
		adminBenutzer = BenutzerTestDataProvider
			.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.build();

		when(verwaltungseinheitService.getBundeslandBereichPrepared())
			.thenReturn(
				PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 1000, 1000)));
		when(zustaendigkeitsService.istImZustaendigkeitsbereich(any(Geometry.class), eq(adminBenutzer))).thenReturn(true);
	}

	@Test
	void testImport_idNochNichtVorhanden_wirdNeuAngelegt()
		throws CsvReadException, CsvImportException {
		// arrange
		Abstellanlage neuZuErstellendeAbstellanlage = AbstellanlageTestDataProvider.withDefaultValues()
			// Diese id gibt es nicht in der Datenbank, -> Abstellanlage soll neu angelegt werden
			// wird aber für das Erstellen der CSV-Datei benoetigt
			.id(123456789L)
			.status(AbstellanlagenStatus.AKTIV)
			.gebuehrenProMonat(GebuehrenProMonat.of("399"))
			.quellSystem(AbstellanlagenQuellSystem.RADVIS)
			.build();
		CsvData csvData = createCsv(neuZuErstellendeAbstellanlage);

		// act
		CsvData protokoll = abstellanlageImportService.importCsv(csvData, adminBenutzer);

		// assert
		Iterable<Abstellanlage> allAbstellanlagen = abstellanlageRepository.findAll();
		assertThat(allAbstellanlagen).hasSize(1);

		Abstellanlage savedAbstellanlage = allAbstellanlagen.iterator().next();
		assertThat(savedAbstellanlage).usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Abstellanlage.class)
			.ignoringFields("id", "version", "dokumentListe")
			.isEqualTo(neuZuErstellendeAbstellanlage);
		assertThat(savedAbstellanlage.getId()).isNotEqualTo(neuZuErstellendeAbstellanlage.getId());
		assertThat(savedAbstellanlage.getDokumentListe().getId()).isNotNull();

		assertThat(protokoll.getHeader()).containsAll(Abstellanlage.CsvHeader.ALL);
		assertThat(protokoll.getHeader()).containsAll(AbstellanlageImportService.PROTOKOLL_HEADER);

		assertThat(protokoll.getRows()).hasSize(1);
		assertThat(protokoll.getRows().get(0).get("Aktion")).isEqualTo("Hinzugefügt");
		assertThat(protokoll.getRows().get(0).get("URL"))
			.isEqualTo(BASE_URL + "app" + FrontendLinks.abstellanlageDetails(savedAbstellanlage.getId()));
		assertThat(protokoll.getRows().get(0).get("Fehler")).isEmpty();
	}

	@Test
	void testImport_wirdAnhandIDupdated() throws CsvReadException, CsvImportException {
		// arrange
		Abstellanlage abstellanlage = abstellanlageRepository
			.save(AbstellanlageTestDataProvider.withDefaultValues().build());

		Abstellanlage updatedAbstellanlage = abstellanlage.toBuilder().status(AbstellanlagenStatus.AKTIV)
			.gebuehrenProMonat(GebuehrenProMonat.of("399")).build();

		CsvData csvData = createCsv(updatedAbstellanlage);

		// act
		CsvData protokoll = abstellanlageImportService.importCsv(csvData, adminBenutzer);

		// assert
		Iterable<Abstellanlage> allAbstellanlagen = abstellanlageRepository.findAll();
		assertThat(allAbstellanlagen).hasSize(1);

		Abstellanlage savedAbstellanlage = allAbstellanlagen.iterator().next();
		assertThat(savedAbstellanlage).usingRecursiveComparison().usingOverriddenEquals()
			.ignoringFields("id", "version")
			.isEqualTo(updatedAbstellanlage);

		assertThat(protokoll.getHeader()).containsAll(Abstellanlage.CsvHeader.ALL);
		assertThat(protokoll.getHeader()).containsAll(AbstellanlageImportService.PROTOKOLL_HEADER);

		assertThat(protokoll.getRows()).hasSize(1);
		assertThat(protokoll.getRows().get(0).get("Aktion")).isEqualTo("Aktualisiert");
		assertThat(protokoll.getRows().get(0).get("URL"))
			.isEqualTo(BASE_URL + "app" + FrontendLinks.abstellanlageDetails(savedAbstellanlage.getId()));
		assertThat(protokoll.getRows().get(0).get("Fehler")).isEmpty();
	}

	@Test
	void testImport_neuErstellen_inCsvAlsQuellsystemMobidata_wirdIgnoriert()
		throws CsvReadException, CsvImportException {
		// arrange
		CsvData csvData = createCsv(AbstellanlageTestDataProvider.withDefaultValues()
			.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW)
			// Diese id gibt es nicht in der Datenbank, -> Abstellanlage soll neu angelegt werden
			// wird aber für das Erstellen der CSV-Datei benoetigt
			.id(123456789L)
			.build());

		// act
		CsvData protokoll = abstellanlageImportService.importCsv(csvData, adminBenutzer);

		// assert
		Iterable<Abstellanlage> allAbstellanlagen = abstellanlageRepository.findAll();
		assertThat(allAbstellanlagen).hasSize(0);

		assertThat(protokoll.getHeader()).containsAll(Abstellanlage.CsvHeader.ALL);
		assertThat(protokoll.getHeader()).containsAll(AbstellanlageImportService.PROTOKOLL_HEADER);

		assertThat(protokoll.getRows()).hasSize(1);
		assertThat(protokoll.getRows().get(0).get("Aktion")).isEqualTo("Ignoriert");
		assertThat(protokoll.getRows().get(0).get("Fehler")).isEqualTo(
			"Fahrradabstellanlagen mit dem Quellsystem MobiDataBW können nicht über den manuellen CSV-Import importiert werden.");
	}

	@Test
	void testImport_wirdAnhandIDupdated_csvUpdatedQuellsystemVonMobidataAufRadVIS_wirdNeuAngelegt()
		throws CsvReadException, CsvImportException {
		// arrange
		Abstellanlage abstellanlage = abstellanlageRepository
			.save(AbstellanlageTestDataProvider.withDefaultValues()
				.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW)
				.build());

		Abstellanlage updatedAbstellanlage = abstellanlage.toBuilder().status(AbstellanlagenStatus.AKTIV)
			.gebuehrenProMonat(GebuehrenProMonat.of("399"))
			.quellSystem(AbstellanlagenQuellSystem.RADVIS)
			.build();

		CsvData csvData = createCsv(updatedAbstellanlage);

		// act
		CsvData protokoll = abstellanlageImportService.importCsv(csvData, adminBenutzer);

		// assert
		Iterable<Abstellanlage> allAbstellanlagen = abstellanlageRepository.findAll();
		assertThat(allAbstellanlagen).hasSize(2);

		Abstellanlage savedAbstellanlage = StreamSupport.stream(allAbstellanlagen.spliterator(), false)
			.filter(abstellanlage1 -> abstellanlage1.getQuellSystem().equals(AbstellanlagenQuellSystem.RADVIS))
			.findFirst().get();
		assertThat(savedAbstellanlage).usingRecursiveComparison().usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Abstellanlage.class)
			.ignoringFields("id", "version", "dokumentListe")
			.isEqualTo(updatedAbstellanlage);
		assertThat(savedAbstellanlage.getId()).isNotEqualTo(updatedAbstellanlage.getId());
		assertThat(savedAbstellanlage.getDokumentListe().getId()).isNotNull();

		assertThat(protokoll.getHeader()).containsAll(Abstellanlage.CsvHeader.ALL);
		assertThat(protokoll.getHeader()).containsAll(AbstellanlageImportService.PROTOKOLL_HEADER);

		assertThat(protokoll.getRows()).hasSize(1);
		assertThat(protokoll.getRows().get(0).get("Aktion")).isEqualTo("Hinzugefügt");
		assertThat(protokoll.getRows().get(0).get("URL"))
			.isEqualTo(BASE_URL + "app" + FrontendLinks.abstellanlageDetails(savedAbstellanlage.getId()));
		assertThat(protokoll.getRows().get(0).get("Fehler")).isEmpty();
	}

	@Test
	void testImport_keineBerechtigung() throws CsvReadException {
		// arrange
		Abstellanlage abstellanlage = abstellanlageRepository
			.save(AbstellanlageTestDataProvider.withDefaultValues().build());

		Abstellanlage updatedAbstellanlage = abstellanlage.toBuilder().status(AbstellanlagenStatus.AKTIV)
			.gebuehrenProMonat(GebuehrenProMonat.of("399")).build();

		CsvData csvData = createCsv(updatedAbstellanlage);
		Benutzer benutzerOhneRecht = BenutzerTestDataProvider
			.defaultBenutzer()
			.rollen(Set.of(Rolle.RADROUTEN_BEARBEITERIN)) // Kein Recht für Serviceangebote
			.build();

		// act & assert
		assertThatThrownBy(() -> abstellanlageImportService.importCsv(csvData, benutzerOhneRecht)).isInstanceOf(
			AccessDeniedException.class);
	}

	private CsvData createCsv(Abstellanlage abstellanlage) throws CsvReadException {
		AbstellanlageRepository abstellanlageRepositoryMock = Mockito.mock(AbstellanlageRepository.class);
		when(abstellanlageRepositoryMock.findAllById(any())).thenReturn(List.of(abstellanlage));
		List<ExportData> export = new AbstellanlageExporterService(abstellanlageRepositoryMock)
			.export(List.of(1l));

		CsvRepository csvRepository = new CsvRepositoryImpl();
		byte[] csv = new CSVExportConverter(csvRepository).convert(export);
		CsvData csvData = csvRepository.read(csv, Abstellanlage.CsvHeader.ALL);
		return csvData;
	}
}
