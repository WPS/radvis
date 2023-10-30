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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RamUsageUtility;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.DLMInitialImportJobStatistik;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMWFSImportRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DLMInitialImportJob extends AbstractJob {

	private final NetzService netzService;
	private final DLMWFSImportRepository dlmWfsImportRepository;
	private final EntityManager entityManager;
	private final InitialPartitionenImportService initialPartitionenImportService;

	public DLMInitialImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		DLMWFSImportRepository dlmWfsImportRepository, NetzService netzService, EntityManager entityManager,
		InitialPartitionenImportService initialPartitionenImportService) {
		super(jobExecutionDescriptionRepository);

		this.dlmWfsImportRepository = dlmWfsImportRepository;
		this.netzService = netzService;
		this.entityManager = entityManager;
		this.initialPartitionenImportService = initialPartitionenImportService;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.DLM_INITIAL_IMPORT_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run() {
		return run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.DLM_INITIAL_IMPORT_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	@Transactional
	protected Optional<JobStatistik> doRun() {
		List<Envelope> partitions = this.dlmWfsImportRepository.getPartitionen();
		DLMInitialImportJobStatistik dlmInitialImportJobStatistik = new DLMInitialImportJobStatistik();
		if (netzService.getAnzahlKanten() != 0) {
			throw new RuntimeException(
				"Es sind bereits Kanten importiert - dieser Job geht aber von einem leeren Netz aus");
		}
		try {
			importiereDlm(partitions, dlmInitialImportJobStatistik);
		} catch (Throwable e) {
			// Wenn irgendwas schief geht, müssen wir manuell aufräumen, weil die Partitionen separat committed werden
			log.error("Beim initialen Import des DLM-Netzes ist ein Fehler aufgetreten", e);
			log.warn("Lösche die bisher bearbeiteten Partitionen");
			netzService.loescheGesamtesNetz();
		}
		entityManager.flush();
		entityManager.clear();
		RamUsageUtility.logCurrentRamUsage("Freier Speicher nach dem initialen Import");
		return Optional.of(dlmInitialImportJobStatistik);
	}

	private void importiereDlm(List<Envelope> partitions, DLMInitialImportJobStatistik dlmInitialImportJobStatistik) {
		AtomicInteger partitionCounter = new AtomicInteger();
		partitionCounter.set(0);
		Set<String> importierteDlmIds = new HashSet<>();

		partitions.forEach(partition -> {
			long anzahlImportierteKantenVorher = importierteDlmIds.size();
			log.info("Importiere Partition {}/{} with extent {}", partitionCounter.get() + 1, partitions.size(),
				partition);
			this.initialPartitionenImportService.importPartition(partition, importierteDlmIds,
				dlmInitialImportJobStatistik);
			RamUsageUtility.logCurrentRamUsage("Freier Speicher am Ende der Partition");
			log.info("Partition {}/{} fertig", partitionCounter.incrementAndGet(), partitions.size());
			long anzahlImportierterKantenDieserPartition = importierteDlmIds.size() - anzahlImportierteKantenVorher;
			log.info("Es wurden {} Kanten in dieser Partition importiert. Insgesamt wurden bisher {} Kanten importiert",
				anzahlImportierterKantenDieserPartition, importierteDlmIds.size());
		});

		if (dlmInitialImportJobStatistik.abgearbeiteteWege + dlmInitialImportJobStatistik.abgearbeiteteStrassen == 0) {
			throw new RuntimeException("Es sind keine Features vom Endpunkt zurückgekommen");
		}

		log.info("Es wurden {} Strassen Features importiert.", dlmInitialImportJobStatistik.abgearbeiteteStrassen);
		log.info("Es wurden {} Wege Features importiert.", dlmInitialImportJobStatistik.abgearbeiteteWege);
		log.info("Dabei wurden {} neue Kanten erstellt", dlmInitialImportJobStatistik.neueKanteHinzugefuegt);
	}

}
