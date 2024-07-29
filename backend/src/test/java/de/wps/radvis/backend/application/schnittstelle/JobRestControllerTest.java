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

package de.wps.radvis.backend.application.schnittstelle;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.web.server.ResponseStatusException;

import de.wps.radvis.backend.application.domain.InitialImportService;

class JobRestControllerTest {
	private JobRestController jobController;

	@Mock
	InitialImportService initialImportService;
	@Mock
	RadVisJobScheduler radVisJobScheduler;
	@Mock
	Environment environment;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		jobController = new JobRestController(initialImportService, radVisJobScheduler, environment, new ArrayList<>());
		when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });
	}

	@Test
	void testDescribeRollen_jobModusAktiv_autorisiert()
		throws Exception {
		// arrange
		jobController.switchJobModus();

		// act + assert
		assertDoesNotThrow(() -> jobController.describe());

		jobController.switchJobModus();
	}

	@Test
	void testDescribeRollen_jobModusNichtAktiv_nichtAutorisiert()
		throws Exception {
		// arrange

		// act + assert
		assertThatThrownBy(
			() -> jobController.describe())
				.isInstanceOf(ResponseStatusException.class)
				.hasMessage("403 FORBIDDEN \"Jobs können nur im Job-Modus ausgeführt werden.\"");
	}
}
