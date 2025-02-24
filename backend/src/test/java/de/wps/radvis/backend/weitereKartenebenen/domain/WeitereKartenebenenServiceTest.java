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

package de.wps.radvis.backend.weitereKartenebenen.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.WeitereKartenebene;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.WeitereKartenebenenTestDataProvider;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.WeitereKartenebenenRepository;

class WeitereKartenebenenServiceTest {
	private WeitereKartenebenenService weitereKartenebenenService;
	@Mock
	private WeitereKartenebenenRepository weitereKartenebenenRepository;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		weitereKartenebenenService = new WeitereKartenebenenService(weitereKartenebenenRepository);
	}

	@Test
	void getAllForNutzer() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		WeitereKartenebene benutzerLayer = WeitereKartenebenenTestDataProvider.defaultValue().id(1l).defaultLayer(false)
			.build();
		WeitereKartenebene benutzerLayerDefault = WeitereKartenebenenTestDataProvider.defaultValue().id(2l)
			.defaultLayer(true)
			.build();
		WeitereKartenebene layerDefault = WeitereKartenebenenTestDataProvider.defaultValue().id(3l).defaultLayer(true)
			.build();

		when(weitereKartenebenenRepository.findAllByBenutzerOrderById(any()))
			.thenReturn(List.of(benutzerLayer, benutzerLayerDefault));
		when(weitereKartenebenenRepository.findAllDefault())
			.thenReturn(List.of(benutzerLayerDefault, layerDefault));

		// act
		List<WeitereKartenebene> allForNutzer = weitereKartenebenenService.getAllForNutzer(benutzer);

		// assert
		ArgumentCaptor<Benutzer> argumentCaptor = ArgumentCaptor.forClass(Benutzer.class);
		verify(weitereKartenebenenRepository, times(1)).findAllByBenutzerOrderById(argumentCaptor.capture());
		assertThat(argumentCaptor.getValue()).isEqualTo(benutzer);
		assertThat(allForNutzer).containsExactlyInAnyOrder(benutzerLayer, benutzerLayerDefault, layerDefault);
	}

}
