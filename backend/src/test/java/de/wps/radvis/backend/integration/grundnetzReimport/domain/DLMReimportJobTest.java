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

package de.wps.radvis.backend.integration.grundnetzReimport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RadVisDomainEvent;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMAttributMapper;
import de.wps.radvis.backend.matching.domain.KanteUpdateElevationService;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KanteDeletedEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMWFSImportRepository;
import jakarta.persistence.EntityManager;

class DLMReimportJobTest {

	private DLMReimportJob dlmReimportJob;

	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	private DLMWFSImportRepository dlmImportRepository;
	@Mock
	private NetzService netzService;
	@Mock
	private KantenRepository kantenRepository;
	@Mock
	private EntityManager entityManager;
	@Mock
	private KantenMappingRepository kantenMappingRepository;
	@Mock
	private KnotenRepository knotenRepository;
	@Mock
	private KanteUpdateElevationService kanteUpdateElevationService;

	private UpdateKantenService updateKantenService;
	private CreateKantenService createKantenService;
	private ExecuteTopologischeUpdatesService executeTopologischeUpdatesService;

	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		FindKnotenFromIndexService findKnotenFromIndexService = new FindKnotenFromIndexService();

		this.updateKantenService = new UpdateKantenService(new DLMAttributMapper(), kantenRepository);

		this.createKantenService = new CreateKantenService(new DLMAttributMapper(), netzService,
			findKnotenFromIndexService);

		this.executeTopologischeUpdatesService = new ExecuteTopologischeUpdatesService(findKnotenFromIndexService,
			kantenMappingRepository);

		when(netzService.countAndLogVernetzungFehlerhaft()).thenReturn(0);
		when(netzService.getAnzahlKanten()).thenReturn(1L);

		dlmReimportJob = new DLMReimportJob(
			jobExecutionDescriptionRepository,
			dlmImportRepository, netzService, updateKantenService,
			createKantenService, executeTopologischeUpdatesService, kantenMappingRepository, entityManager,
			new VernetzungService(kantenRepository, knotenRepository, netzService), kanteUpdateElevationService);

		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	void sollNichtStartenWennKeineKantenVorhanden() {
		// arrange
		when(netzService.getAnzahlKanten()).thenReturn(0L);

		// act & assrert
		assertThrows(RuntimeException.class, () -> dlmReimportJob.doRun());
	}

	@Test
	void testDoRun_bestehendeKanteNichtImReImport_kanteWirdGeloescht() {
		// arrange
		Envelope bereich = new Envelope(0, 200, 0, 200);
		Envelope groessererBereichFuerDLM = bereich.copy();
		groessererBereichFuerDLM.expandBy(DLMReimportJob.getMaximalErlaubteVerschiebungEinerKante());

		List<Envelope> partitions = List.of(bereich);
		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		List<Knoten> bestehendeKnoten = List.of(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(1L).build(),
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).id(2L).build(),
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 20), QuellSystem.DLM).id(3L).build(),
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 30), QuellSystem.DLM).id(4L).build());

		List<Kante> bestehendeKanten = List.of(
			KanteTestDataProvider.fromKnoten(bestehendeKnoten.get(0), bestehendeKnoten.get(1))
				.quelle(QuellSystem.DLM)
				.id(10L)
				.dlmId(DlmId.of("1"))
				.build(),
			KanteTestDataProvider.fromKnoten(bestehendeKnoten.get(1), bestehendeKnoten.get(2))
				.quelle(QuellSystem.DLM)
				.id(20L)
				.dlmId(DlmId.of("2"))
				.build(),
			KanteTestDataProvider.fromKnoten(bestehendeKnoten.get(2), bestehendeKnoten.get(3))
				.quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("3"))
				.id(30L)
				.build());

		when(netzService
			.getKantenInBereichNachQuelleEagerFetchKantenAttribute(groessererBereichFuerDLM, QuellSystem.DLM))
			.thenReturn(
				bestehendeKanten.stream());

		Stream<ImportedFeature> importedFeatureStream = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(0, 0), new Coordinate(10, 10))
				.fachId("1")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(20, 20), new Coordinate(30, 30))
				.fachId("3")
				.build());
		when(dlmImportRepository.readStrassenFeatures(bereich)).thenReturn(importedFeatureStream);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<Kante>> captor = ArgumentCaptor.forClass(Collection.class);
		doNothing().when(netzService).deleteAll(captor.capture());
		Kante bestehendeKante1 = bestehendeKanten.get(1);
		when(entityManager.merge(bestehendeKante1)).thenReturn(bestehendeKante1);

		// act
		dlmReimportJob.doRun();

		// assert
		assertThat(captor.getValue()).contains(bestehendeKante1);

		ArgumentCaptor<RadVisDomainEvent> eventCaptor = ArgumentCaptor.forClass(RadVisDomainEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()), times(1));
		assertThat(eventCaptor.getAllValues())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withIgnoredFields("datum").build())
			.containsExactly(
				new KanteDeletedEvent(bestehendeKante1.getId(), bestehendeKante1.getGeometry(),
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()));
	}

	@Test
	void testDoRun_bestehendeKanteKuerzerUndNichtImReImportDerEntsprechendenPartition_kanteWirdNichtGeloescht() {
		// arrange
		Envelope bereich1 = new Envelope(0, 20, 0, 20);
		Envelope bereich2 = new Envelope(20, 40, 0, 20);
		List<Envelope> partitions = List.of(bereich1, bereich2);
		Envelope groessererBereichFuerDLM1 = bereich1.copy();
		groessererBereichFuerDLM1.expandBy(DLMReimportJob.getMaximalErlaubteVerschiebungEinerKante());
		Envelope groessererBereichFuerDLM2 = bereich2.copy();
		groessererBereichFuerDLM2.expandBy(DLMReimportJob.getMaximalErlaubteVerschiebungEinerKante());

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		List<Knoten> bestehendeKnoten = List.of(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(1L).build(),
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).id(2L).build(),
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(19, 19), QuellSystem.DLM).id(3L).build(),
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 30), QuellSystem.DLM).id(4L).build());

		List<Kante> bestehendeKantenBereich1 = List.of(
			KanteTestDataProvider.fromKnoten(bestehendeKnoten.get(0), bestehendeKnoten.get(1))
				.quelle(QuellSystem.DLM)
				.id(10L)
				.dlmId(DlmId.of("1"))
				.build(),
			KanteTestDataProvider.fromKnoten(bestehendeKnoten.get(1), bestehendeKnoten.get(2))
				.quelle(QuellSystem.DLM)
				.id(19L)
				.dlmId(DlmId.of("2"))
				.build(),
			KanteTestDataProvider.fromKnoten(bestehendeKnoten.get(2), bestehendeKnoten.get(3))
				.quelle(QuellSystem.DLM)
				.id(30L)
				.dlmId(DlmId.of("3"))
				.build());

		when(netzService
			.getKantenInBereichNachQuelleEagerFetchKantenAttribute(groessererBereichFuerDLM1, QuellSystem.DLM))
			.thenReturn(
				bestehendeKantenBereich1.stream());

		when(netzService
			.getKantenInBereichNachQuelleEagerFetchKantenAttribute(groessererBereichFuerDLM2, QuellSystem.DLM))
			.thenReturn(
				Stream.empty());
		doNothing().when(netzService).deleteAll(any());
		when(entityManager.merge(any())).then(invocationOnMock -> invocationOnMock.getArgument(0));

		Stream<ImportedFeature> importedFeatureStreamBereich1 = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(0, 0), new Coordinate(10, 10))
				.fachId("1")
				.build(),
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(10, 10), new Coordinate(19, 19))
				.fachId("2")
				.build());

		Stream<ImportedFeature> importedFeatureStreamBereich2 = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(21, 21), new Coordinate(30, 30))
				.fachId("3")
				.build());
		when(dlmImportRepository.readStrassenFeatures(bereich1)).thenReturn(importedFeatureStreamBereich1);
		when(dlmImportRepository.readStrassenFeatures(bereich2)).thenReturn(importedFeatureStreamBereich2);

		// act
		dlmReimportJob.doRun();

		// assert
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<Kante>> kantenCaptor = ArgumentCaptor.forClass(Collection.class);
		verify(netzService, times(1)).deleteAll(kantenCaptor.capture());
		assertThat(kantenCaptor.getValue()).isEmpty();

		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(any()), times(0));
	}

	@Test
	void testDoRun_neueKanteKreisgeometrie_AlteKanteWirdGeloescht() {
		// arrange
		Envelope bereich1 = new Envelope(366612.87392123876, 629524.2093880848, 5248138.677671564, 5549206.116131693);
		Envelope bereich2 = new Envelope(366000.87392123876, 630000.2093880848, 5200000.677671564, 5600000.116131693);
		List<Envelope> partitions = List.of(bereich1, bereich2);
		Envelope groessererBereichFuerDLM1 = bereich1.copy();
		groessererBereichFuerDLM1.expandBy(DLMReimportJob.getMaximalErlaubteVerschiebungEinerKante());
		Envelope groessererBereichFuerDLM2 = bereich2.copy();
		groessererBereichFuerDLM2.expandBy(DLMReimportJob.getMaximalErlaubteVerschiebungEinerKante());

		when(dlmImportRepository.getPartitionen()).thenReturn(partitions);

		Coordinate[] koordinaten = new Coordinate[] {
			new Coordinate(565137.72, 5340760.35),
			new Coordinate(565136.71, 5340717.07),
			new Coordinate(565140.07, 5340712.06),
			new Coordinate(565143.4, 5340710.08),
			new Coordinate(565159.18, 5340709.27)
		};

		Kante alteKante = KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(koordinaten))
			.quelle(QuellSystem.DLM)
			.id(10L)
			.dlmId(DlmId.of("1"))
			.build();

		List<Kante> bestehendeKantenBereich1 = List.of(
			alteKante);

		when(netzService
			.getKantenInBereichNachQuelleEagerFetchKantenAttribute(groessererBereichFuerDLM1, QuellSystem.DLM))
			.thenReturn(
				bestehendeKantenBereich1.stream());

		when(netzService
			.getKantenInBereichNachQuelleEagerFetchKantenAttribute(groessererBereichFuerDLM2, QuellSystem.DLM))
			.thenReturn(
				Stream.empty());

		Stream<ImportedFeature> importedFeatureStreamBereich1 = Stream.of(
			ImportedFeatureTestDataProvider.defaultWFSObject()
				.lineString(new Coordinate(565140.53, 5340759.94),
					new Coordinate(565145.3, 5340746.76),
					new Coordinate(565156.88, 5340727.08),
					new Coordinate(565159.42, 5340720.41),
					new Coordinate(565156.41, 5340714.69),
					new Coordinate(565150.39, 5340712.57),
					new Coordinate(565144.82, 5340713.42),
					new Coordinate(565140.53, 5340716.44),
					new Coordinate(565138.63, 5340723.58),
					new Coordinate(565140.53, 5340759.94))
				.fachId("1")
				.build());

		when(dlmImportRepository.readStrassenFeatures(bereich1)).thenReturn(importedFeatureStreamBereich1);
		when(dlmImportRepository.readStrassenFeatures(bereich2)).thenReturn(Stream.empty());
		when(entityManager.merge(alteKante)).thenReturn(alteKante);

		// act
		dlmReimportJob.doRun();

		// assert
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<Kante>> toDeleteInPersistenceContext = ArgumentCaptor.forClass(Collection.class);
		verify(netzService, times(1)).deleteAll(
			toDeleteInPersistenceContext.capture());

		Collection<Kante> kanten = toDeleteInPersistenceContext.getValue();
		assertThat(kanten).containsExactly(alteKante);

		ArgumentCaptor<RadVisDomainEvent> eventCaptor = ArgumentCaptor.forClass(RadVisDomainEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()), times(1));
		assertThat(eventCaptor.getAllValues())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withIgnoredFields("datum").build())
			.containsExactly(
				new KanteDeletedEvent(alteKante.getId(), alteKante.getGeometry(),
					NetzAenderungAusloeser.DLM_REIMPORT_JOB, LocalDateTime.now()));

	}
}
