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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.exception.CsvAttributMappingException;
import de.wps.radvis.backend.common.domain.exception.CsvReadException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CSVExportConverter;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.CsvRepositoryImpl;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.valueObject.Anzahl;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationStatus;
import de.wps.radvis.backend.leihstation.domain.valueObject.UrlAdresse;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class LeihstationImportServiceTest {
	@Mock
	LeihstationRepository leihstationRepository;
	@Mock
	VerwaltungseinheitService verwaltungseinheitService;
	@Mock
	ZustaendigkeitsService zustaendigkeitsService;

	private LeihstationImportService service;
	private Benutzer adminBenutzer;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		service = new LeihstationImportService(leihstationRepository, verwaltungseinheitService, zustaendigkeitsService,
			"http://base.url");
		adminBenutzer = BenutzerTestDataProvider
			.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.build();

		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 1000, 1000)));
		when(zustaendigkeitsService.istImZustaendigkeitsbereich(any(Geometry.class), eq(adminBenutzer))).thenReturn(
			true);
	}

	@Test
	void mapAttributes_fromRadVISExport() throws CsvReadException, CsvAttributMappingException {
		// arrange
		Leihstation leihstation1 = Leihstation.builder()
			.id(null)
			.anzahlAbstellmoeglichkeiten(Anzahl.of(1)).id(10l)
			.betreiber("Mein Betreiber").geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
			.status(LeihstationStatus.AUSSER_BETRIEB).freiesAbstellen(true).quellSystem(LeihstationQuellSystem.RADVIS)
			.build();
		Leihstation leihstation2 = Leihstation.builder()
			.id(null)
			.anzahlFahrraeder(Anzahl.of(1))
			.anzahlPedelecs(Anzahl.of(234))
			.id(20l).buchungsUrl(UrlAdresse.of("www.meine-buchungs-url.de"))
			.betreiber("Mein Betreiber 2").geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(500, 600)))
			.status(LeihstationStatus.GEPLANT).freiesAbstellen(false).quellSystem(LeihstationQuellSystem.RADVIS)
			.build();

		when(leihstationRepository.findAllById(any())).thenReturn(List.of(leihstation1, leihstation2));
		List<ExportData> export = new LeihstationExporterService(leihstationRepository)
			.export(List.of(leihstation1.getId(), leihstation2.getId()));

		CsvRepository csvRepository = new CsvRepositoryImpl();
		byte[] csv = new CSVExportConverter(csvRepository).convert(export);
		CsvData csvData = csvRepository.read(csv, Leihstation.CsvHeader.ALL);

		// act
		Leihstation result1 = service
			.mapAttributes(Leihstation.builder().id(leihstation1.getId()), csvData.getRows().get(0), adminBenutzer);
		Leihstation result2 = service
			.mapAttributes(Leihstation.builder().id(leihstation2.getId()), csvData.getRows().get(1), adminBenutzer);

		// assert
		assertThat(result1).usingRecursiveComparison().usingOverriddenEquals().isEqualTo(leihstation1);
		assertThat(result2).usingRecursiveComparison().usingOverriddenEquals().isEqualTo(leihstation2);
	}

	@Nested
	class LeihstationAttributMappingExceptionTest {
		private CsvData csvData;
		List<Map<String, String>> rows;
		HashMap<String, String> incorrectAttributes;

		@BeforeEach
		void setup() throws CsvReadException {
			Leihstation leihstation1 = Leihstation.builder().anzahlAbstellmoeglichkeiten(Anzahl.of(1)).id(10l)
				.betreiber("Mein Betreiber").geometrie(
					KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 200)))
				.status(LeihstationStatus.AUSSER_BETRIEB).freiesAbstellen(true)
				.quellSystem(LeihstationQuellSystem.RADVIS)
				.build();

			LeihstationRepository leihstationRepository = Mockito.mock(LeihstationRepository.class);
			when(leihstationRepository.findAllById(any())).thenReturn(List.of(leihstation1));
			List<ExportData> export = new LeihstationExporterService(leihstationRepository)
				.export(List.of(leihstation1.getId()));

			CsvRepository csvRepository = new CsvRepositoryImpl();
			byte[] csv = new CSVExportConverter(csvRepository).convert(export);
			csvData = csvRepository.read(csv, Leihstation.CsvHeader.ALL);

			rows = csvData.getRows();
			incorrectAttributes = new HashMap<>(rows.get(0));
			incorrectAttributes.put("Quellsystem", "RadVis");

			assertDoesNotThrow(
				() -> service.mapAttributes(Leihstation.builder(), csvData.getRows().get(0), adminBenutzer));
		}

		@Test
		void anzahl_noNumber() throws CsvReadException {
			incorrectAttributes.put("Anzahl Abstellmöglichkeiten", "hallo");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(LeihstationAttributMappingException.class,
				() -> service.mapAttributes(Leihstation.builder(), csvData.getRows().get(0), adminBenutzer));
		}

		@Test
		void betreiber_leer() throws CsvReadException {
			incorrectAttributes.put("Betreiber", "");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(LeihstationAttributMappingException.class,
				() -> service.mapAttributes(Leihstation.builder(), csvData.getRows().get(0), adminBenutzer));
		}

		@Test
		void coordinates_wrongLocale() throws CsvReadException {
			incorrectAttributes.put("Position X (UTM32_N)",
				incorrectAttributes.get("Position X (UTM32_N)").replace(",", "."));
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(LeihstationAttributMappingException.class,
				() -> service.mapAttributes(Leihstation.builder(), csvData.getRows().get(0), adminBenutzer));
		}

		@Test
		void abstellen_keinJaNein() throws CsvReadException {

			incorrectAttributes.put("Freies Abstellen möglich", "hallo");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(LeihstationAttributMappingException.class,
				() -> service.mapAttributes(Leihstation.builder(), csvData.getRows().get(0), adminBenutzer));
		}

		@Test
		void status_unsinn() throws CsvReadException {
			incorrectAttributes.put("Status", "hallo");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(LeihstationAttributMappingException.class,
				() -> service.mapAttributes(Leihstation.builder(), csvData.getRows().get(0), adminBenutzer));
		}

		@Test
		void quellsystem_Quatsch() throws CsvReadException {
			incorrectAttributes.put("Quellsystem", "EntenhausenVerkehrsverzeichnis");
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(LeihstationAttributMappingException.class,
				() -> service.mapAttributes(Leihstation.builder(), csvData.getRows().get(0),
					adminBenutzer));
		}

		@Test
		void quellsystem_Mobidata() throws CsvReadException {

			// Mobidata ist zwar ein gültiges Quellsystem für Leistationen, aber nicht beim Import über csv-Datei

			incorrectAttributes.put("Quellsystem", LeihstationQuellSystem.MOBIDATABW.toString());
			csvData = CsvData.of(List.of(incorrectAttributes), csvData.getHeader());

			assertThrows(LeihstationAttributMappingException.class,
				() -> service.mapAttributes(Leihstation.builder(), csvData.getRows().get(0),
					adminBenutzer));
		}
	}
}
