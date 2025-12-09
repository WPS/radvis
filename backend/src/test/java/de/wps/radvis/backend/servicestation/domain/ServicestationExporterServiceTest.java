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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.servicestation.domain.entity.provider.ServicestationTestDataProvider;

public class ServicestationExporterServiceTest {
	private ServicestationExporterService exporterService;
	@Mock
	private ServicestationRepository servicestationRepository;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		exporterService = new ServicestationExporterService(servicestationRepository);
	}

	@Test
	public void export() {
		// arrange
		when(servicestationRepository.findAllById(any())).thenReturn(List.of(
			ServicestationTestDataProvider.withDefaultValues().build()));

		// act
		List<ExportData> result = exporterService.export(List.of(1L));

		// assert

		// Es werden nicht alle Werte getestet, dieser Test ist dafür da um die unterschiedlichen Mappings zu testen
		assertThat(result.get(0).getGeometry().getNumGeometries()).isEqualTo(1);
		assertThat(result.get(0).getGeometry().getGeometryN(0).getGeometryType()).isEqualTo("Point");
		assertThat(result.get(0).getProperties()).hasSize(18);
		assertThat(result.get(0).getProperties().get("Quellsystem")).isEqualTo("RadVIS");
		assertThat(result.get(0).getProperties().get("Luftpumpe")).isEqualTo("ja");
		assertThat(result.get(0).getProperties().get("Kettenwerkzeug")).isEqualTo("nein");
		assertThat(result.get(0).getProperties().get("Zuständig in RadVIS"))
			.isEqualTo("DefaultOrganisation (Sonstiges)");
		assertThat(result.get(0).getProperties().get("Öffnungszeiten")).isEqualTo("8-16");
	}
}
