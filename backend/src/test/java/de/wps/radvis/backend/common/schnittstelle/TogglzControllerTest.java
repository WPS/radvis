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

package de.wps.radvis.backend.common.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.togglz.junit5.AllDisabled;
import org.togglz.junit5.AllEnabled;

import de.wps.radvis.backend.common.domain.FeatureTogglz;

class TogglzControllerTest {

	private TogglzController togglzController;

	@BeforeEach
	void beforeEach() {
		MockitoAnnotations.openMocks(this);
		togglzController = new TogglzController();
	}

	@Test
	@AllEnabled(FeatureTogglz.class)
	void getAllTogglz_Enabled() {
		// Act
		final var result = togglzController.getTogglz();

		// Assert
		assertThat(result).hasSize(FeatureTogglz.values().length);
		assertThat(result).allMatch(FeatureTogglzView::getEnabled);
	}

	@Test
	@AllDisabled(FeatureTogglz.class)
	void getAllTogglz_Disabled() {
		// Act
		final var result = togglzController.getTogglz();

		// Assert
		assertThat(result).hasSize(FeatureTogglz.values().length);
		assertThat(result).allMatch(togglzView -> !togglzView.getEnabled());
	}

}