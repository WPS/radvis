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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RamUsageUtility;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.DLMReimportJobStatistik;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.SplitUpdate;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.TopologischesUpdate;
import de.wps.radvis.backend.matching.domain.KanteUpdateElevationService;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.event.KanteDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KanteTopologieChangedEvent;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMWFSImportRepository;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DLMReimportJob extends AbstractJob {

	// Gleicher Wert wie im Vernetzungskorrekturjob. Hat da beim Ausprobieren gut hingehauen.
	private static final double TOLERANZ_RADVIS_KNOTEN = 18.0;

	// Sobald das Feature Toggle entfernt wird, kann der Wert wieder als Konstante kodiert werden
	public static int getMaximalErlaubteVerschiebungEinerKante() {
		return FeatureTogglz.DLM_REIMPORT_FIX.isActive() ? 300 : 30;
	}

	private final NetzService netzService;
	private final DLMWFSImportRepository dlmWfsImportRepository;
	private final UpdateKantenService updateKantenService;
	private final CreateKantenService createKantenService;
	private final ExecuteTopologischeUpdatesService executeTopologischeUpdatesService;
	private final KantenMappingRepository kantenMappingRepository;
	private final EntityManager entityManager;
	private final VernetzungService vernetzungService;
	private final KanteUpdateElevationService kanteUpdateElevationService;

	public DLMReimportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		DLMWFSImportRepository dlmWfsImportRepository, NetzService netzService,
		UpdateKantenService updateKantenService,
		CreateKantenService createKantenService, ExecuteTopologischeUpdatesService executeTopologischeUpdatesService,
		KantenMappingRepository kantenMappingRepository,
		EntityManager entityManager, VernetzungService vernetzungService,
		KanteUpdateElevationService kanteUpdateElevationService) {
		super(jobExecutionDescriptionRepository);

		this.dlmWfsImportRepository = dlmWfsImportRepository;
		this.netzService = netzService;
		this.updateKantenService = updateKantenService;
		this.createKantenService = createKantenService;
		this.executeTopologischeUpdatesService = executeTopologischeUpdatesService;
		this.kantenMappingRepository = kantenMappingRepository;
		this.entityManager = entityManager;
		this.vernetzungService = vernetzungService;
		this.kanteUpdateElevationService = kanteUpdateElevationService;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.DLM_REIMPORT_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run() {
		return run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.DLM_REIMPORT_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	@Transactional
	protected Optional<JobStatistik> doRun() {
		if (netzService.getAnzahlKanten() == 0) {
			throw new RuntimeException(
				"Es sind noch keine Kanten importiert - bitte den DLMInitialImportJob für einen initialen Import verwenden");
		}
		List<Envelope> partitions = this.dlmWfsImportRepository.getPartitionen();

		LocalDateTime datumReImport = LocalDateTime.now();

		DLMReimportJobStatistik dlmReimportJobStatistik = new DLMReimportJobStatistik();
		dlmReimportJobStatistik.reset();

		reimportiereDlm(partitions, datumReImport, dlmReimportJobStatistik);
		entityManager.flush();
		entityManager.clear();

		log.info("Starting elevation Update: 3D-Geometrie an Kanten ermitteln+speichern...");
		kanteUpdateElevationService.updateElevations();
		log.info("... Elevation Update done");

		log.info("Refreshing RadVisNetz-Materialized-Views");
		this.netzService.refreshRadVisNetzMaterializedViews();
		log.info("Finished refreshing RadVisNetz-Materialized-Views");

		if (FeatureTogglz.VERNETZUNG_KORREKTUR.isActive()) {
			vernetzeRadvisKantenNeu(dlmReimportJobStatistik);
			entityManager.flush();
			entityManager.clear();
			dlmReimportJobStatistik.anzahlVernetzungsfehlerNachJobausfuehrung = netzService.countAndLogVernetzungFehlerhaft();
			if (dlmReimportJobStatistik.anzahlVernetzungsfehlerNachJobausfuehrung > 0) {
				throw new RuntimeException("Vernetzung wurde kompromittiert! Rollback ...\n"
					+ dlmReimportJobStatistik.toString());
			}
		}

		if (dlmReimportJobStatistik.fatalErrorOccurred) {
			try {
				log.info("Der Job ist durchgelaufen. Statistik: {}", dlmReimportJobStatistik.toJSON());
			} catch (JsonProcessingException e) {
				log.error("Statistik konnte nicht nach JSON serialisiert werden :(", e);
			}
			throw new RuntimeException();
		} else {
			try {
				log.info("JobStatistik:\n{}", dlmReimportJobStatistik.toJSON());
			} catch (JsonProcessingException e) {
				log.error("Statistik konnte nicht nach JSON serialisiert werden :(", e);
			}
			return Optional.of(dlmReimportJobStatistik);
		}
	}

	private void vernetzeRadvisKantenNeu(DLMReimportJobStatistik dlmReimportJobStatistik) {
		log.info("Vernetzte RadVIS-Kanten neu...");

		vernetzungService.vernetzeAlleRadvisKantenNeu(
			dlmReimportJobStatistik.radvisKantenVernetzungStatistik, TOLERANZ_RADVIS_KNOTEN,
			NetzAenderungAusloeser.DLM_REIMPORT_JOB);

		dlmReimportJobStatistik.anzahlGeloescheterVerwaisterKnoten += dlmReimportJobStatistik.radvisKantenVernetzungStatistik.anzahlKnotenGeloescht;
	}

	private void reimportiereDlm(List<Envelope> partitions, LocalDateTime datumReImport,
		DLMReimportJobStatistik dlmReimportJobStatistik) {
		AtomicInteger partitionCounter = new AtomicInteger();
		partitionCounter.set(0);
		AtomicInteger counter = new AtomicInteger(0);
		Set<Kante> nichtReimportierteKanten = new HashSet<>();
		Map<Envelope, List<TopologischesUpdate>> topologischesUpdates = new HashMap<>();

		// das grosse HashSet reImportierteDlmIds lebt nur im Scope, um anschliessend Memory zu sparen
		{
			log.info("Erkenne Updates des DLM-Netzes über {} Partitionen...", partitions.size());
			Set<String> reImportierteDlmIds = new HashSet<>();
			partitions.forEach(partition -> {
				log.info("Starting Partition with extent {}", partition);
				List<TopologischesUpdate> updatesDerPartition = new ArrayList<>();
				topologischesUpdates.put(partition, updatesDerPartition);

				// relevant, da durch die Verschiebung die DLM-Kanten in eine andere Partition als das neue
				// entsprechende
				// Feature landen könnten
				Envelope biggerEnvelopeForDLM = partition.copy();
				biggerEnvelopeForDLM.expandBy(
					DLMReimportJob.getMaximalErlaubteVerschiebungEinerKante());

				Map<String, Kante> bestehendeDLMKanten = new HashMap<>();
				netzService.getKantenInBereichNachQuelleEagerFetchKantenAttribute(biggerEnvelopeForDLM, QuellSystem.DLM)
					.forEach(kante -> bestehendeDLMKanten.put(kante.getDlmId().getValue(), kante));

				KnotenIndex knotenIndex = new KnotenIndex();
				netzService.getKnotenInBereichNachQuelle(biggerEnvelopeForDLM, QuellSystem.DLM)
					.forEach(knotenIndex::fuegeEin);

				// strassenFeatures werden erst gesammelt, da sonst der Endpunkt die Verbindung kappt
				List<ImportedFeature> strassenFeatures = dlmWfsImportRepository.readStrassenFeatures(partition).collect(
					Collectors.toList());
				strassenFeatures.forEach(importedFeature -> {
					reImportFeature(importedFeature, dlmReimportJobStatistik, reImportierteDlmIds, bestehendeDLMKanten,
						knotenIndex).ifPresent(updatesDerPartition::add);
					++dlmReimportJobStatistik.abgearbeiteteStrassen;
					logProgress(counter, 20000, "Features");
				});
				RamUsageUtility.logCurrentRamUsage("Zwischen Features");
				// wegeFeatures werden erst gesammelt, da sonst der Endpunkt die Verbindung kappt
				List<ImportedFeature> wegeFeatures = dlmWfsImportRepository.readWegeFeatures(partition)
					.collect(Collectors.toList());
				wegeFeatures.forEach(importedFeature -> {
					reImportFeature(importedFeature, dlmReimportJobStatistik, reImportierteDlmIds, bestehendeDLMKanten,
						knotenIndex).ifPresent(updatesDerPartition::add);
					++dlmReimportJobStatistik.abgearbeiteteWege;
					logProgress(counter, 20000, "Features");
				});

				nichtReimportierteKanten.removeIf(kante -> reImportierteDlmIds.contains(kante.getDlmId().getValue()));
				nichtReimportierteKanten
					.addAll(bestimmeNichtReimportierteKanten(bestehendeDLMKanten, reImportierteDlmIds));

				entityManager.flush();
				entityManager.clear();
				RamUsageUtility.logCurrentRamUsage("Am Ende der Partition");
				log.info("finished partition {}", partitionCounter.incrementAndGet());
			});
		}

		if (dlmReimportJobStatistik.abgearbeiteteWege + dlmReimportJobStatistik.abgearbeiteteStrassen == 0) {
			throw new RuntimeException("Es sind keine Features von dem Endpunkt zurückgekommen");
		}

		log.info("Es wurden {} Strassen Features fertig abgearbeitet oder zu topologischen Updates weiterverarbeitet.",
			dlmReimportJobStatistik.abgearbeiteteStrassen);
		log.info("Es wurden {} Wege Features fertig abgearbeitet oder zu topologischen Updates weiterverarbeitet.",
			dlmReimportJobStatistik.abgearbeiteteWege);
		log.info("Dabei wurden {} neue Kanten erstellt", dlmReimportJobStatistik.neueKanteHinzugefuegt);

		// Kategorisiere Topologische Updates und bereite diese vor
		List<SplitUpdate> splitUpdates = new ArrayList<>();
		log.info("Kategorisiere {} topologische Updates in splitUpdates und simpleTopologicalUpdates",
			topologischesUpdates.values().stream().mapToInt(List::size).sum());
		partitionCounter.set(0);
		HashMap<Envelope, List<TopologischesUpdate>> simpleTopologicalUpdates = new HashMap<>();
		topologischesUpdates.forEach((partition, topologischeUpdatesDerPartition) -> {
			List<TopologischesUpdate> simpleTopologicalUpdatesDerPartition = new ArrayList<>();

			log.info("Starting Partition with extent {}", partition);
			topologischeUpdatesDerPartition.forEach(topologischesUpdate -> {
				// return Kante to Hibernate context
				topologischesUpdate.setKante(entityManager.merge(topologischesUpdate.getKante()));

				Optional<SplitUpdate> split = updateKantenService
					.findSplitIfExists(topologischesUpdate, nichtReimportierteKanten, dlmReimportJobStatistik);

				if (split.isPresent()) {
					splitUpdates.add(split.get());
				} else {
					simpleTopologicalUpdatesDerPartition.add(topologischesUpdate);
				}
			});
			simpleTopologicalUpdates.put(partition, simpleTopologicalUpdatesDerPartition);

			log.info("finished partition {}", partitionCounter.incrementAndGet());
		});

		log.info("Führe {} simpleTopologicalUpdates partitioniert aus",
			simpleTopologicalUpdates.values().stream().mapToInt(List::size).sum());
		partitionCounter.set(0);
		Map<Kante, LineString> topologischStarkVeraenderteKanten = new HashMap<>();

		simpleTopologicalUpdates.forEach((partition, simpleUpdates) -> {
			log.info("Starting Partition with extent {}", partition);
			Envelope biggerExtentFuerKnoten = partition.copy();
			biggerExtentFuerKnoten.expandBy(2000);

			KnotenIndex knotenIndex = new KnotenIndex();
			netzService.getKnotenInBereichNachQuelle(biggerExtentFuerKnoten, QuellSystem.DLM)
				.forEach(knotenIndex::fuegeEin);

			simpleUpdates.forEach(update -> {
				log.debug("Topologisches Update: neue Geom: {}; KanteId: {}", update.getNeueGeometry(),
					update.getKante().getId());
				if (!biggerExtentFuerKnoten.contains(update.getNeueGeometry().getEnvelopeInternal())) {
					log.warn("Knotenindex enthält keine Knoten für die neue Geometrie {} der Kante {} ",
						update.getNeueGeometry(), update.getKante().getId());
				}

				Kante kante = this.executeTopologischeUpdatesService
					.executeSimplesTopologischesUpdate(update, dlmReimportJobStatistik, knotenIndex,
						topologischStarkVeraenderteKanten);
				netzService.saveKante(kante);
			});
			log.info("finished partition {}", partitionCounter.incrementAndGet());
		});

		log.info("Führe {} Splits durch", splitUpdates.size());
		counter.set(0);
		splitUpdates.forEach(update -> {
			log.debug("Split Update: {}", update);
			topologischStarkVeraenderteKanten.put(update.getKante(), update.getKante().getGeometry());
			List<Kante> kanten = executeTopologischeUpdatesService.executeSplitUpdate(update);
			kanten.forEach(netzService::saveKante);
			logProgressInPercent(splitUpdates.size(), counter, 4);
		});

		entityManager.flush();
		entityManager.clear();

		topologischStarkVeraenderteKanten.forEach((kante, lineString) -> RadVisDomainEventPublisher.publish(
			new KanteTopologieChangedEvent(kante.getId(), lineString, NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				datumReImport)));

		List<Kante> toDeleteInPersistenceContext = nichtReimportierteKanten.stream().map(entityManager::merge)
			.collect(Collectors.toList());

		toDeleteInPersistenceContext.forEach(kante -> RadVisDomainEventPublisher.publish(
			new KanteDeletedEvent(kante.getId(), kante.getGeometry(), NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				datumReImport)));

		toDeleteInPersistenceContext.stream()
			.map(Kante::getId)
			.forEach(kantenMappingRepository::deleteByGrundnetzKantenId);

		netzService.deleteAll(toDeleteInPersistenceContext);

		dlmReimportJobStatistik.geloeschteKanten += toDeleteInPersistenceContext.size();
		log.info("Es wurde(n) {} Kante(n) gelöscht", dlmReimportJobStatistik.geloeschteKanten);

		dlmReimportJobStatistik.anzahlGeloescheterVerwaisterKnoten = this.netzService
			.deleteVerwaisteDLMKnoten(NetzAenderungAusloeser.DLM_REIMPORT_JOB);

		log.info("Es wurden {} verwaiste DLM-Knoten gelöscht",
			dlmReimportJobStatistik.anzahlGeloescheterVerwaisterKnoten);
	}

	private Set<Kante> bestimmeNichtReimportierteKanten(Map<String, Kante> bestehendeDLMKanten, Set<String> dlmIds) {
		return bestehendeDLMKanten.values().stream()
			.filter(kante -> !dlmIds.contains(kante.getDlmId().getValue())).collect(Collectors.toSet());
	}

	private Optional<TopologischesUpdate> reImportFeature(ImportedFeature importedFeature,
		DLMReimportJobStatistik dlmReimportJobStatistik,
		Set<String> dlmIds, Map<String, Kante> bestehendeDLMKanten,
		KnotenIndex knotenIndex) {
		// Wir checken vor dem Hinzufügen der DLM-IDs, ob es sich um eine Kreisgeometrie handelt,
		// damit topologische Änderungen hin zu Kreisen zum Löschen der Kante führt.
		if (isKreisGeometrie(importedFeature)) {
			dlmReimportJobStatistik.startUndEndpunktGleich++;
			return Optional.empty();
		}

		if (isAutobahn(importedFeature)) {
			dlmReimportJobStatistik.autobahnen++;
			return Optional.empty();
		}

		if (!dlmIds.add(importedFeature.getTechnischeId())) {
			return Optional.empty();
		}

		if (bestehendeDLMKanten.containsKey(importedFeature.getTechnischeId())) {
			Kante bestehendeDLMKante = bestehendeDLMKanten.get(importedFeature.getTechnischeId());
			Optional<TopologischesUpdate> topologischesUpdate = updateKantenService
				.updateBestehendeDLMKante(importedFeature, bestehendeDLMKante, dlmReimportJobStatistik);

			if (topologischesUpdate.isEmpty()) {
				netzService.saveKante(bestehendeDLMKante);
			}
			return topologischesUpdate;
		} else {
			createKantenService.createNewDLMKante(importedFeature, dlmReimportJobStatistik, knotenIndex);
			return Optional.empty();
		}
	}

	private boolean isAutobahn(ImportedFeature importedFeature) {
		if (!importedFeature.hasAttribut("bezeichnung")) {
			return false;
		}
		// Die Bezeichnung ist entweder ein einzelner Strassenname z.B. A8 oder mit ; konkateniert z.B. E52;A8
		String[] strassenbezeichnungen = importedFeature.getAttribut("bezeichnung").toString().split(";");
		return Arrays.stream(strassenbezeichnungen)
			.anyMatch(strassenbezeichnung -> strassenbezeichnung.startsWith("A"));
	}

	private boolean isKreisGeometrie(ImportedFeature importedFeature) {
		return ((LineString) importedFeature.getGeometrie()).isClosed();
	}
}
