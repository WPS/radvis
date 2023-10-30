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

package de.wps.radvis.backend.manuellerimport.common.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.event.PreDlmReimportJobEvent;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ImportSessionRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;

class ManuellerImportServiceTest {

	@Mock
	private ImportSessionRepository importSessionRepository;

	@Mock
	private ShapeZipService shapeZipService;

	@Mock
	private ShapeFileRepository shapeFileRepository;

	@Mock
	private ManuellerImportFehlerRepository manuellerImportFehlerRepository;

	private ManuellerImportService manuellerImportService;

	@BeforeEach
	void beforeEach() {
		openMocks(this);

		manuellerImportService = new ManuellerImportService(importSessionRepository, shapeZipService,
			shapeFileRepository, manuellerImportFehlerRepository);
	}

	@Test
	public void onPreDlmReimport() {
		// Arrange
		final var preDlmReimportJobEvent = new PreDlmReimportJobEvent();

		// Act
		manuellerImportService.onPreDlmReimport(preDlmReimportJobEvent);

		// Assert
		verify(importSessionRepository).clear();
	}

	@Test
	public void saveImportSession() {
		// Arrange
		final var importSession = mock(AbstractImportSession.class);
		when(importSession.getBenutzer()).thenReturn(BenutzerTestDataProvider.defaultBenutzer().build());
		// Act
		manuellerImportService.saveImportSession(importSession);

		// Assert
		verify(importSessionRepository).save(importSession);
	}

	@Test
	public void findImportSessionFromBenutzer() {
		// Arrange
		final var benutzer = mock(Benutzer.class);
		final var importSession = mock(AbstractImportSession.class);

		final var expectedResult = Optional.of(importSession);
		when(importSessionRepository.find(benutzer)).thenReturn(expectedResult);

		// Act
		final var result = manuellerImportService.findImportSessionFromBenutzer(benutzer);

		// Assert
		assertThat(result).isEqualTo(expectedResult);
	}

}