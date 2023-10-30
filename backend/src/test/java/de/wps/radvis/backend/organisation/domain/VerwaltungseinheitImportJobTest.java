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

package de.wps.radvis.backend.organisation.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

class VerwaltungseinheitImportJobTest {

	/**
	 * Wenn dieser ImportJob umbenannt wird, wird der redundant ausgefuehrt und das fuehrt zu doppelten Organisationen.
	 */
	@Test
	void jobNameSollNichtUmbenanntWerden() {
		assertThat(VerwaltungseinheitImportJob.JOB_NAME).isEqualTo("OrganisationenImportJob");
	}
}
