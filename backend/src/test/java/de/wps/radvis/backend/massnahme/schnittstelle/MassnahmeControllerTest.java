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

package de.wps.radvis.backend.massnahme.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.dokument.schnittstelle.AddDokumentCommand;
import de.wps.radvis.backend.dokument.schnittstelle.view.DokumenteView;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandabfrageService;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.schnittstelle.view.MassnahmeEditView;
import de.wps.radvis.backend.massnahme.schnittstelle.view.UmsetzungsstandEditView;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.NonNull;

class MassnahmeControllerTest {
	@Mock
	private @NonNull MassnahmeService massnahmeService;
	@Mock
	private @NonNull UmsetzungsstandabfrageService umsetzungsstandabfrageService;
	@Mock
	private @NonNull CreateMassnahmeCommandConverter createMassnahmeCommandConverter;
	@Mock
	private @NonNull SaveMassnahmeCommandConverter saveMassnahmeCommandConverter;
	@Mock
	private @NonNull SaveUmsetzungsstandCommandConverter saveUmsetungsstandCommandConverter;
	@Mock
	private @NonNull MassnahmeGuard massnahmeGuard;
	@Mock
	private @NonNull BenutzerResolver benutzerResolver;
	@Mock
	private @NonNull VerwaltungseinheitService verwaltungseinheitService;
	@Mock
	private @NonNull CsvRepository csvRepository;

	private MassnahmeController massnahmeController;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		massnahmeController = new MassnahmeController(massnahmeService, umsetzungsstandabfrageService,
			createMassnahmeCommandConverter, saveMassnahmeCommandConverter, saveUmsetungsstandCommandConverter,
			massnahmeGuard, benutzerResolver, verwaltungseinheitService, csvRepository);
	}

	@Test
	void getMassnahmeForEdit_canEdit_true() {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId).build();
		when(massnahmeService.get(massnahmeId)).thenReturn(massnahme);
		when(massnahmeGuard.darfMassnahmeBearbeiten(any(), eq(massnahme))).thenReturn(true);

		// act
		MassnahmeEditView massnahmeForEdit = massnahmeController.getMassnahmeForEdit(massnahmeId,
			mock(Authentication.class));

		// assert
		assertThat(massnahmeForEdit.isCanEdit()).isTrue();
	}

	@Test
	void getMassnahmeForEdit_archiviert_canEdit_false() {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId).build();
		massnahme.archivieren();
		when(massnahmeService.get(massnahmeId)).thenReturn(massnahme);
		when(massnahmeGuard.darfMassnahmeBearbeiten(any(), eq(massnahme))).thenReturn(true);

		// act
		MassnahmeEditView massnahmeForEdit = massnahmeController.getMassnahmeForEdit(massnahmeId,
			mock(Authentication.class));

		// assert
		assertThat(massnahmeForEdit.isCanEdit()).isFalse();
	}

	@Test
	void getDokumentListe_canEdit_true() {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId).build();
		when(massnahmeService.get(massnahmeId)).thenReturn(massnahme);
		when(massnahmeGuard.darfMassnahmeBearbeiten(any(), eq(massnahme))).thenReturn(true);

		// act
		DokumenteView dokumenteView = massnahmeController.getDokumentListe(massnahmeId,
			mock(Authentication.class));

		// assert
		assertThat(dokumenteView.isCanEdit()).isTrue();
	}

	@Test
	void getDokumentListe_archiviert_canEdit_false() {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId).build();
		massnahme.archivieren();
		when(massnahmeService.get(massnahmeId)).thenReturn(massnahme);
		when(massnahmeGuard.darfMassnahmeBearbeiten(any(), eq(massnahme))).thenReturn(true);

		// act
		DokumenteView dokumenteView = massnahmeController.getDokumentListe(massnahmeId,
			mock(Authentication.class));

		// assert
		assertThat(dokumenteView.isCanEdit()).isFalse();

	}

	@Test
	void getUmsetzungsstandForEdit_canEdit_true() {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build();
		when(massnahmeService.get(massnahmeId)).thenReturn(massnahme);
		when(massnahmeGuard.darfMassnahmeBearbeiten(any(), eq(massnahme))).thenReturn(true);

		// act
		UmsetzungsstandEditView umsetzungsstandEditView = massnahmeController.getUmsetzungsstandForEdit(massnahmeId,
			mock(Authentication.class));

		// assert
		assertThat(umsetzungsstandEditView.isCanEdit()).isTrue();
	}

	@Test
	void getUmsetzungsstandForEdit_archiviert_canEdit_false() {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build();
		massnahme.archivieren();
		when(massnahmeService.get(massnahmeId)).thenReturn(massnahme);
		when(massnahmeGuard.darfMassnahmeBearbeiten(any(), eq(massnahme))).thenReturn(true);

		// act
		UmsetzungsstandEditView umsetzungsstandEditView = massnahmeController.getUmsetzungsstandForEdit(massnahmeId,
			mock(Authentication.class));

		// assert
		assertThat(umsetzungsstandEditView.isCanEdit()).isFalse();
	}

	@Test
	void saveMassnahme_archiviert_throws() {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId).build();
		massnahme.archivieren();
		Long version = 4l;
		when(massnahmeService.loadForModification(massnahmeId, version)).thenReturn(massnahme);
		SaveMassnahmeCommand command = mock(SaveMassnahmeCommand.class);
		when(command.getId()).thenReturn(massnahmeId);
		when(command.getVersion()).thenReturn(version);

		// act+assert
		assertThrows(ResponseStatusException.class, () -> {
			massnahmeController.saveMassnahme(mock(Authentication.class), command);
		});
	}

	@Test
	void saveUmsetzungsstand_archiviert_throws() {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId).build();
		massnahme.archivieren();
		Long version = 4l;
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		when(massnahmeService.loadUmsetzungsstandForModification(massnahmeId, version)).thenReturn(umsetzungsstand);
		when(massnahmeService.getMassnahmeByUmsetzungsstand(umsetzungsstand)).thenReturn(massnahme);
		SaveUmsetzungsstandCommand command = mock(SaveUmsetzungsstandCommand.class);
		when(command.getId()).thenReturn(massnahmeId);
		when(command.getVersion()).thenReturn(version);

		// act+assert
		assertThrows(ResponseStatusException.class, () -> {
			massnahmeController.saveUmsetzungsstand(mock(Authentication.class), command);
		});
	}

	@Test
	void uploadDatei_archiviert_throws() throws IOException {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId).build();
		massnahme.archivieren();
		when(massnahmeService.get(massnahmeId)).thenReturn(massnahme);
		MultipartFile multipartFile = mock(MultipartFile.class);
		when(multipartFile.getBytes()).thenReturn(new byte[] {});

		// act+assert
		assertThrows(ResponseStatusException.class, () -> {
			massnahmeController.uploadDatei(massnahmeId, mock(AddDokumentCommand.class), multipartFile,
				mock(Authentication.class));
		});
	}

	@Test
	void deleteDatei_archiviert_throws() throws IOException {
		// arrange
		long massnahmeId = 234l;
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(massnahmeId).build();
		massnahme.archivieren();
		when(massnahmeService.get(massnahmeId)).thenReturn(massnahme);

		// act+assert
		assertThrows(ResponseStatusException.class, () -> {
			massnahmeController.deleteDatei(mock(Authentication.class), massnahmeId, 1l);
		});
	}
}
