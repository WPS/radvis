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

package de.wps.radvis.backend.benutzer.domain;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerIstNichtRegistriertException;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;

public class SetzeBenutzerInaktivJobTest {
	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	BenutzerService benutzerService;

	private SetzeBenutzerInaktivJob setzeBenutzerInaktivJob;

	private final Integer inaktivitaetsTimeoutInTagen = 365;

	@BeforeEach
	void setup() {
		openMocks(this);
		setzeBenutzerInaktivJob = new SetzeBenutzerInaktivJob(jobExecutionDescriptionRepository, benutzerService,
			inaktivitaetsTimeoutInTagen);
	}

	@Test
	void testSetzeBenutzerInaktiv() throws BenutzerIstNichtRegistriertException {
		//arrange
		Long id = 1L;
		Long version = 2L;

		Benutzer benutzer = BenutzerTestDataProvider
			.defaultBenutzer()
			.id(id)
			.version(version)
			.build();

		when(benutzerService.ermittleAktiveBenutzerInaktivLaengerAls(inaktivitaetsTimeoutInTagen)).thenReturn(
			List.of(benutzer));

		//act
		setzeBenutzerInaktivJob.doRun();

		//assert
		verify(benutzerService).aendereBenutzerstatus(benutzer.getId(), benutzer.getVersion(), BenutzerStatus.INAKTIV);
		//
	}
}
