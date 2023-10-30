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

package de.wps.radvis.backend.leihstation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.locationtech.jts.geom.Coordinate;
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
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.CsvRepositoryImpl;
import de.wps.radvis.backend.leihstation.LeihstationConfiguration;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.valueObject.Anzahl;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationStatus;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group5")
@ContextConfiguration(classes = LeihstationConfiguration.class)
@EnableConfigurationProperties(value = CommonConfigurationProperties.class)
@MockBeans({
	@MockBean(BenutzerResolver.class),
})
public class LeihstationImportServiceTestIT extends DBIntegrationTestIT {
	private static final String BASE_URL = "https://radvis-dev.landbw.de/";

	private LeihstationImportService leihstationImportService;
	private Benutzer adminBenutzer;

	@Autowired
	LeihstationRepository leihstationRepository;
	@MockBean
	VerwaltungseinheitService verwaltungseinheitService;
	@MockBean
	ZustaendigkeitsService zustaendigkeitsService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		leihstationImportService = new LeihstationImportService(leihstationRepository, verwaltungseinheitService,
			zustaendigkeitsService, BASE_URL);
		adminBenutzer = BenutzerTestDataProvider
			.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.build();

		when(verwaltungseinheitService.getBundeslandBereichPrepared())
			.thenReturn(
				PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 1000, 1000)));
		when(zustaendigkeitsService.istImZustaendigkeitsbereich(any(Geometry.class), eq(adminBenutzer))).thenReturn(
			true);
	}

	@Test
	void importLeihstation_noZusaetzlicheSpalten()
		throws CsvImportException, CsvReadException {
		// arrange
		List<String> header = new ArrayList<>(Leihstation.CsvHeader.ALL);
		header.add("Test");
		CsvData csvData = CsvData.of(List.of(), header);

		assertThrows(CsvImportException.class, () -> leihstationImportService.importCsv(csvData, adminBenutzer));
	}

	@Test
	void importLeihstation_insert_noId()
		throws CsvReadException, CsvImportException {
		// arrange
		Leihstation leihstation1 = Leihstation.builder().anzahlAbstellmoeglichkeiten(Anzahl.of(1))
			.id(null)
			.betreiber("Mein Betreiber").geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
			.status(LeihstationStatus.AUSSER_BETRIEB).freiesAbstellen(true).quellSystem(LeihstationQuellSystem.RADVIS)
			.build();

		CsvData csvData = createCsv(leihstation1);
		List<Map<String, String>> rows = csvData.getRows();
		HashMap<String, String> correctedAttributes = new HashMap<>(rows.get(0));
		correctedAttributes.put("RadVIS-ID", "");
		csvData = CsvData.of(List.of(correctedAttributes), csvData.getHeader());

		// act
		CsvData protokoll = leihstationImportService.importCsv(csvData, adminBenutzer);

		// assert
		Iterable<Leihstation> allLeihstationen = leihstationRepository.findAll();
		assertThat(allLeihstationen).hasSize(1);

		Leihstation savedLeihstation = allLeihstationen.iterator().next();
		assertThat(savedLeihstation).usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Leihstation.class)
			.ignoringFields("id", "version")
			.isEqualTo(leihstation1);

		assertThat(protokoll.getHeader()).containsAll(Leihstation.CsvHeader.ALL);
		assertThat(protokoll.getHeader()).containsAll(LeihstationImportService.PROTOKOLL_HEADER);

		assertThat(protokoll.getRows()).hasSize(1);
		assertThat(protokoll.getRows().get(0).get("Aktion")).isEqualTo("Hinzugefügt");
		assertThat(protokoll.getRows().get(0).get("URL"))
			.isEqualTo(BASE_URL + "app" + FrontendLinks.leihstationDetails(savedLeihstation.getId()));
		assertThat(protokoll.getRows().get(0).get("Fehler")).isEmpty();
	}

	@Test
	void importLeihstation_insert_idNotFound()
		throws CsvReadException, CsvImportException {
		// arrange
		Leihstation existingLeihstation = leihstationRepository
			.save(Leihstation.builder().anzahlAbstellmoeglichkeiten(Anzahl.of(1))
				.betreiber("Mein Betreiber").geometrie(
					KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
				.status(LeihstationStatus.AUSSER_BETRIEB).freiesAbstellen(true).quellSystem(LeihstationQuellSystem.RADVIS)
				.build());

		Leihstation updatedLeihstation = existingLeihstation.toBuilder().status(LeihstationStatus.AKTIV)
			.anzahlAbstellmoeglichkeiten(Anzahl.of(324)).build();

		CsvData csvData = createCsv(updatedLeihstation);
		List<Map<String, String>> rows = csvData.getRows();
		HashMap<String, String> correctedAttributes = new HashMap<>(rows.get(0));
		correctedAttributes.put("RadVIS-ID", String.valueOf(existingLeihstation.getId() + 10L));
		csvData = CsvData.of(List.of(correctedAttributes), csvData.getHeader());

		// act
		CsvData protokoll = leihstationImportService.importCsv(csvData, adminBenutzer);

		// assert
		Iterable<Leihstation> allLeihstationen = leihstationRepository.findAll();
		assertThat(allLeihstationen).hasSize(2);

		Iterator<Leihstation> iterator = allLeihstationen.iterator();
		iterator.next();
		Leihstation newLeihstation = iterator.next();
		assertThat(newLeihstation)
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Leihstation.class)
			.ignoringFields("id", "version")
			.isEqualTo(updatedLeihstation);
		assertThat(newLeihstation.getId()).isNotEqualTo(existingLeihstation.getId());

		assertThat(protokoll.getHeader()).containsAll(Leihstation.CsvHeader.ALL);
		assertThat(protokoll.getHeader()).containsAll(LeihstationImportService.PROTOKOLL_HEADER);

		assertThat(protokoll.getRows()).hasSize(1);
		assertThat(protokoll.getRows().get(0).get("Aktion")).isEqualTo("Hinzugefügt");
		assertThat(protokoll.getRows().get(0).get("URL"))
			.isEqualTo(BASE_URL + "app" + FrontendLinks.leihstationDetails(newLeihstation.getId()));
		assertThat(protokoll.getRows().get(0).get("Fehler")).isEmpty();
	}

	@Test
	void importLeihstation_fehler()
		throws CsvReadException, CsvImportException {
		// arrange
		Leihstation leihstation1 = Leihstation.builder().anzahlAbstellmoeglichkeiten(Anzahl.of(1)).id(10l)
			.betreiber("Mein Betreiber").geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
			.status(LeihstationStatus.AUSSER_BETRIEB).freiesAbstellen(true).quellSystem(LeihstationQuellSystem.RADVIS)
			.build();

		CsvData csvData = createCsv(leihstation1);

		List<Map<String, String>> rows = csvData.getRows();
		HashMap<String, String> incorrectAttributes = new HashMap<>(rows.get(0));
		incorrectAttributes.put("Status", "hallo");
		incorrectAttributes.put("RadVIS-ID", "");
		csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

		// act
		CsvData protokoll = leihstationImportService.importCsv(csvData, adminBenutzer);

		// assert
		Iterable<Leihstation> allLeihstationen = leihstationRepository.findAll();
		assertThat(allLeihstationen).hasSize(0);

		assertThat(protokoll.getHeader()).containsAll(Leihstation.CsvHeader.ALL);
		assertThat(protokoll.getHeader()).containsAll(LeihstationImportService.PROTOKOLL_HEADER);

		assertThat(protokoll.getRows()).hasSize(1);
		assertThat(protokoll.getRows().get(0).get("Aktion")).isEqualTo("Ignoriert");
		assertThat(protokoll.getRows().get(0).get("URL")).isEmpty();
		assertThat(protokoll.getRows().get(0).get("Fehler")).contains("Status");
		assertThat(protokoll.getRows().get(0).get("Fehler")).contains("hallo");
	}

	@Test
	void importLeihstation_update_byId()
		throws CsvReadException, CsvImportException {
		// arrange
		Leihstation existingLeihstation = leihstationRepository
			.save(Leihstation.builder().anzahlAbstellmoeglichkeiten(Anzahl.of(1))
				.betreiber("Mein Betreiber").geometrie(
					KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
				.status(LeihstationStatus.AUSSER_BETRIEB).freiesAbstellen(true).quellSystem(LeihstationQuellSystem.RADVIS)
				.build());

		Leihstation updatedLeihstation = existingLeihstation.toBuilder().status(LeihstationStatus.AKTIV)
			.anzahlAbstellmoeglichkeiten(Anzahl.of(324)).build();

		CsvData csvData = createCsv(updatedLeihstation);

		// act
		CsvData protokoll = leihstationImportService.importCsv(csvData, adminBenutzer);

		// assert
		Iterable<Leihstation> allLeihstationen = leihstationRepository.findAll();
		assertThat(allLeihstationen).hasSize(1);

		Leihstation savedLeihstation = allLeihstationen.iterator().next();
		assertThat(savedLeihstation).usingRecursiveComparison().usingOverriddenEquals().ignoringFields("id", "version")
			.isEqualTo(updatedLeihstation);

		assertThat(protokoll.getHeader()).containsAll(Leihstation.CsvHeader.ALL);
		assertThat(protokoll.getHeader()).containsAll(LeihstationImportService.PROTOKOLL_HEADER);

		assertThat(protokoll.getRows()).hasSize(1);
		assertThat(protokoll.getRows().get(0).get("Aktion")).isEqualTo("Aktualisiert");
		assertThat(protokoll.getRows().get(0).get("URL"))
			.isEqualTo(BASE_URL + "app" + FrontendLinks.leihstationDetails(savedLeihstation.getId()));
		assertThat(protokoll.getRows().get(0).get("Fehler")).isEmpty();
		assertThat(savedLeihstation).isEqualTo(existingLeihstation);
	}

	@Test
	void importLeihstation_update_byPosition()
		throws CsvReadException, CsvImportException {
		// arrange
		Leihstation existingLeihstation = leihstationRepository
			.save(Leihstation.builder().anzahlAbstellmoeglichkeiten(Anzahl.of(1))
				.betreiber("Mein Betreiber").geometrie(
					KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
				.status(LeihstationStatus.AUSSER_BETRIEB).freiesAbstellen(true).quellSystem(LeihstationQuellSystem.RADVIS)
				.build());

		Leihstation updatedLeihstation = existingLeihstation.toBuilder().status(LeihstationStatus.AKTIV)
			.anzahlAbstellmoeglichkeiten(Anzahl.of(324)).build();

		CsvData csvData = createCsv(updatedLeihstation);
		List<Map<String, String>> rows = csvData.getRows();
		HashMap<String, String> correctedAttributes = new HashMap<>(rows.get(0));
		correctedAttributes.put("RadVIS-ID", "");
		csvData = CsvData.of(List.of(correctedAttributes), csvData.getHeader());

		// act
		CsvData protokoll = leihstationImportService.importCsv(csvData, adminBenutzer);

		// assert
		Iterable<Leihstation> allLeihstationen = leihstationRepository.findAll();
		assertThat(allLeihstationen).hasSize(1);

		Leihstation savedLeihstation = allLeihstationen.iterator().next();
		assertThat(savedLeihstation).usingRecursiveComparison().usingOverriddenEquals().ignoringFields("id", "version")
			.isEqualTo(updatedLeihstation);

		assertThat(protokoll.getHeader()).containsAll(Leihstation.CsvHeader.ALL);
		assertThat(protokoll.getHeader()).containsAll(LeihstationImportService.PROTOKOLL_HEADER);

		assertThat(protokoll.getRows()).hasSize(1);
		assertThat(protokoll.getRows().get(0).get("Aktion")).isEqualTo("Aktualisiert");
		assertThat(protokoll.getRows().get(0).get("URL"))
			.isEqualTo(BASE_URL + "app" + FrontendLinks.leihstationDetails(savedLeihstation.getId()));
		assertThat(protokoll.getRows().get(0).get("Fehler")).isEmpty();
		assertThat(savedLeihstation).isEqualTo(existingLeihstation);
	}

	@Test
	void testImport_keineBerechtigung() throws CsvReadException {
		// arrange
		Leihstation existingLeihstation = leihstationRepository.save(Leihstation.builder()
			.anzahlAbstellmoeglichkeiten(Anzahl.of(1))
			.betreiber("Mein Betreiber")
			.geometrie(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
			.status(LeihstationStatus.AUSSER_BETRIEB)
			.freiesAbstellen(true)
			.quellSystem(LeihstationQuellSystem.RADVIS)
			.build());

		Leihstation updatedLeihstation = existingLeihstation.toBuilder().status(LeihstationStatus.AKTIV)
			.anzahlAbstellmoeglichkeiten(Anzahl.of(324)).build();

		CsvData csvData = createCsv(updatedLeihstation);
		Benutzer benutzerOhneRecht = BenutzerTestDataProvider
			.defaultBenutzer()
			.rollen(Set.of(Rolle.RADROUTEN_BEARBEITERIN)) // Kein Recht für Serviceangebote
			.build();

		// act & assert
		assertThatThrownBy(() -> leihstationImportService.importCsv(csvData, benutzerOhneRecht)).isInstanceOf(
			AccessDeniedException.class);
	}

	private CsvData createCsv(Leihstation leihstation1) throws CsvReadException {
		LeihstationRepository leihstationRepositoryMock = Mockito.mock(LeihstationRepository.class);
		when(leihstationRepositoryMock.findAllById(any())).thenReturn(List.of(leihstation1));
		List<ExportData> export = new LeihstationExporterService(leihstationRepositoryMock)
			.export(List.of(1l));

		CsvRepository csvRepository = new CsvRepositoryImpl();
		byte[] csv = new CSVExportConverter(csvRepository).convert(export);
		CsvData csvData = csvRepository.read(csv, Leihstation.CsvHeader.ALL);
		return csvData;
	}
}
