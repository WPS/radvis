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

package de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.exception.ZipFileExtractException;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.service.ZipService;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.valueObject.MassnahmenDateianhaengeImportZuordnungStatus;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;

class ManuellerMassnahmenDateianhaengeImportServiceTest {
	@Mock
	private ManuellerImportService manuellerImportService;
	@Mock
	private ZipService zipService;
	@Mock
	private CsvRepository csvRepository;
	@Mock
	private MassnahmeRepository massnahmeRepository;
	@Mock
	private VerwaltungseinheitRepository verwaltungseinheitRepository;

	private ManuellerMassnahmenDateianhaengeImportService manuellerMassnahmenDateianhaengeImportService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		manuellerMassnahmenDateianhaengeImportService = new ManuellerMassnahmenDateianhaengeImportService(
			manuellerImportService, zipService, csvRepository, massnahmeRepository, verwaltungseinheitRepository);
	}

	@Test
	void ladeDateien() throws IOException, ZipFileExtractException {
		// arrange
		Long massnahmeId = 856l;
		String konzeptId = "ABC";
		File mockDateien = mockDateien(konzeptId);
		when(zipService.unzip(any(), any())).thenReturn(mockDateien);
		MassnahmenDateianhaengeImportSession session = new MassnahmenDateianhaengeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200), "Mein Tanzbereich",
			List.of(2l), Konzeptionsquelle.RADNETZ_MASSNAHME, null);
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of(konzeptId), session.getKonzeptionsquelle()))
				.thenReturn(List.of(MassnahmeTestDataProvider
					.withKanten(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 30, 20, QuellSystem.DLM).build())
					.id(massnahmeId)
					.build()));

		// act
		manuellerMassnahmenDateianhaengeImportService.ladeDateien(session, mockDateien);

		// assert
		assertThat(session.getSchritt()).isEqualTo(MassnahmenDateianhaengeImportSession.FEHLER_UEBERPRUEFEN);
		assertThat(session.getZuordnungen()).hasSize(1);
		assertThat(session.getZuordnungen().get(konzeptId).getHinweis()).isEmpty();
		assertThat(session.getZuordnungen().get(konzeptId).getOrdnerName()).isEqualTo(konzeptId);
		assertThat(session.getZuordnungen().get(konzeptId).getMassnahmeId()).isEqualTo(massnahmeId);
		assertThat(session.getZuordnungen().get(konzeptId).getStatus())
			.isEqualTo(MassnahmenDateianhaengeImportZuordnungStatus.ZUGEORDNET);
	}

	@Test
	void ladeDateien_massnahmeArchiviert_zuordnungShouldBeFehlerhaft() throws IOException, ZipFileExtractException {
		// arrange
		Long massnahmeId = 856l;
		String konzeptId = "ABC";
		File mockDateien = mockDateien(konzeptId);
		when(zipService.unzip(any(), any())).thenReturn(mockDateien);
		MassnahmenDateianhaengeImportSession session = new MassnahmenDateianhaengeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GeometryTestdataProvider.createQuadratischerBereich(0, 0, 200, 200), "Mein Tanzbereich",
			List.of(2l), Konzeptionsquelle.RADNETZ_MASSNAHME, null);
		Massnahme massnahme = MassnahmeTestDataProvider
			.withKanten(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 30, 20, QuellSystem.DLM).build())
			.id(massnahmeId)
			.build();
		massnahme.archivieren();
		when(massnahmeRepository.findByMassnahmeKonzeptIdAndKonzeptionsquelleAndGeloeschtFalse(
			MassnahmeKonzeptID.of(konzeptId), session.getKonzeptionsquelle()))
				.thenReturn(List.of(massnahme));

		// act
		manuellerMassnahmenDateianhaengeImportService.ladeDateien(session, mockDateien);

		// assert
		assertThat(session.getZuordnungen()).hasSize(1);
		assertThat(session.getZuordnungen().get(konzeptId).getHinweis()).isPresent();
		assertThat(session.getZuordnungen().get(konzeptId).getOrdnerName()).isEqualTo(konzeptId);
		assertThat(session.getZuordnungen().get(konzeptId).getMassnahmeId()).isNull();
		assertThat(session.getZuordnungen().get(konzeptId).getStatus())
			.isEqualTo(MassnahmenDateianhaengeImportZuordnungStatus.FEHLERHAFT);
	}

	private File mockDateien(String konzeptId) {
		File massnahmeFile = mock(File.class);
		when(massnahmeFile.isFile()).thenReturn(true);
		File massnahmeDir = mock(File.class);
		when(massnahmeDir.isDirectory()).thenReturn(true);
		when(massnahmeDir.getName()).thenReturn(konzeptId);
		when(massnahmeDir.listFiles()).thenReturn(new File[] { massnahmeFile });
		File hauptDir = mock(File.class);
		when(hauptDir.listFiles()).thenReturn(new File[] { massnahmeDir });
		File unzippedFile = mock(File.class);
		when(unzippedFile.listFiles()).thenReturn(new File[] { hauptDir });
		return unzippedFile;
	}

}
