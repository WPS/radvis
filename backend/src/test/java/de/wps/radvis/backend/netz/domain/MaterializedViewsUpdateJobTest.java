/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.netz.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.netz.domain.service.NetzService;

class MaterializedViewsUpdateJobTest {
	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	private NetzService netzService;

	private MaterializedViewsUpdateJob materializedViewsUpdateJob;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		materializedViewsUpdateJob = new MaterializedViewsUpdateJob(
			jobExecutionDescriptionRepository, netzService);
	}

	@Test
	public void test_doRun_callsNetzService() {
		// Act
		Optional<JobStatistik> jobStatistik = materializedViewsUpdateJob.doRun();

		// Assert
		assertThat(jobStatistik).isEmpty();
		verify(netzService).refreshNetzMaterializedViews();
	}
}