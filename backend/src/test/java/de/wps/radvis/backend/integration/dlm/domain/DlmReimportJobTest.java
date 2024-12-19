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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisherSensitiveTest;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.matching.domain.repository.CustomDlmMatchingRepositoryFactory;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.service.CustomGrundnetzMappingServiceFactory;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GraphhopperUpdateService;
import de.wps.radvis.backend.matching.domain.service.GrundnetzMappingService;
import de.wps.radvis.backend.matching.domain.service.KanteUpdateElevationService;
import de.wps.radvis.backend.netz.domain.event.GrundnetzAktualisiertEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DlmRepository;
import lombok.Getter;
import lombok.Setter;

public class DlmReimportJobTest implements RadVisDomainEventPublisherSensitiveTest {
	DlmReimportJob dlmReimportJob;
	@Mock
	private JobExecutionDescriptionRepository jobExecutionRepository;
	@Mock
	private DlmPbfErstellungService dlmPbfErstellungService;
	@Mock
	private GraphhopperUpdateService graphhopperUpdateService;
	@Mock
	private KantenRepository kantenRepository;
	@Mock
	private KnotenRepository knotenRepository;
	@Mock
	private NetzService netzService;
	@Mock
	private DlmRepository dlmRepository;
	@Mock
	private GrundnetzMappingService grundnetzMappingService;
	@Mock
	private KanteUpdateElevationService elevationUpdateService;
	@Mock
	private CustomDlmMatchingRepositoryFactory customDlmMatchingRepositoryFactory;
	@Mock
	private CustomGrundnetzMappingServiceFactory customGrundnetzMappingServiceFactory;
	@Mock
	private DlmMatchingRepository dlmMatchingRepository;

	@Getter
	@Setter
	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		when(netzService.saveKante(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

		when(customGrundnetzMappingServiceFactory.createGrundnetzMappingService(any()))
			.thenReturn(grundnetzMappingService);
		when(customDlmMatchingRepositoryFactory.createCustomMatchingRepository(any()))
			.thenReturn(dlmMatchingRepository);

		dlmReimportJob = new DlmReimportJob(
			jobExecutionRepository,
			dlmPbfErstellungService,
			new KantenAttributeUebertragungService(Laenge.of(1.0)),
			new VernetzungService(kantenRepository, knotenRepository, netzService),
			netzService,
			new DlmImportService(dlmRepository, netzService),
			customDlmMatchingRepositoryFactory, customGrundnetzMappingServiceFactory);
	}

	@Test
	void updateNetzabhaengigeDaten() {
		// arrange
		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 0), new Coordinate(100, 100))
						.fachId("123").build()));

		// act
		dlmReimportJob.doRun();

		// assert
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(any(GrundnetzAktualisiertEvent.class)));
	}

	@Test
	void deletesRessourcesAfterFinishing() {
		// arrange
		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(new Coordinate(0, 0), new Coordinate(100, 100))
						.fachId("123").build()));

		// act
		dlmReimportJob.doRun();

		// assert
		verify(dlmMatchingRepository, times(1)).close();
	}
}
