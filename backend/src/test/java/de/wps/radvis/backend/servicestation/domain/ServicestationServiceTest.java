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

package de.wps.radvis.backend.servicestation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.dokument.domain.entity.provider.DokumentTestDataProvider;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.entity.provider.ServicestationTestDataProvider;
import jakarta.persistence.EntityNotFoundException;

class ServicestationServiceTest {
	private ServicestationService servicestationService;
	@Mock
	private ServicestationRepository servicestationRepository;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		servicestationService = new ServicestationService(servicestationRepository);
	}

	@Test
	void dokumenteHinzufuegenUndLoeschen() {
		DokumentListe dokumentListe = new DokumentListe();
		dokumentListe.addDokument(DokumentTestDataProvider.withDefaultValues().id(1L).build());
		Servicestation servicestation = ServicestationTestDataProvider.withDefaultValues().id(1L)
			.dokumentListe(dokumentListe).build();

		when(servicestationRepository.findById(any())).thenReturn(Optional.ofNullable(servicestation));

		servicestationService.addDokument(servicestation.getId(),
			DokumentTestDataProvider.withDefaultValues().id(2L).build());
		servicestationService.addDokument(servicestation.getId(),
			DokumentTestDataProvider.withDefaultValues().id(3L).build());
		servicestationService.deleteDokument(servicestation.getId(), 2L);

		assertThat(servicestationService.getDokument(servicestation.getId(), 1L).getId()).isEqualTo(1L);
		assertThat(servicestationService.getDokument(servicestation.getId(), 3L).getId()).isEqualTo(3L);

		assertThrows(EntityNotFoundException.class,
			() -> servicestationService.getDokument(servicestation.getId(), 2L));
	}

}
