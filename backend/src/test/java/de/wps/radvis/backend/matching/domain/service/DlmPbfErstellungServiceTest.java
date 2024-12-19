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

package de.wps.radvis.backend.matching.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.matching.domain.repository.PbfErstellungsRepository;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;

class DlmPbfErstellungServiceTest {

	@Mock
	KantenRepository kantenRepository;
	@Mock
	PbfErstellungsRepository pbfErstellungsRepository;
	@Mock
	DLMConfigurationProperties dlmConfigurationProperties;

	String dlmBasisDaten = "foobar";

	DlmPbfErstellungService service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new DlmPbfErstellungService(
			kantenRepository,
			pbfErstellungsRepository,
			dlmConfigurationProperties,
			dlmBasisDaten
		);
	}

	@Test
	public void testErstelleFilteredPbf() throws IOException {
		// Arrange
		when(dlmConfigurationProperties.getExtentProperty()).thenReturn(new ExtentProperty(1.0, 2.0, 10.0, 20.0));
		when(dlmConfigurationProperties.getPbfpartitionen()).thenReturn(1);

		List<Kante> kanten = List.of(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			KanteTestDataProvider.withDefaultValues().id(2L).build(),
			KanteTestDataProvider.withDefaultValues().id(3L).build()
		);

		// Act
		service.erstellePbfForKanten(new File("foobar"), kanten);

		// Assert
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Envelope, Stream<Kante>>> captor = ArgumentCaptor.forClass(Map.class);
		verify(pbfErstellungsRepository).writePbf(captor.capture(), any());

		Collection<Stream<Kante>> kantenStreams = captor.getValue().values();
		assertThat(kantenStreams).hasSize(1);

		List<Kante> actualKanten = kantenStreams.stream().toList().get(0).toList();
		assertThat(actualKanten).containsAll(kanten);
	}
}