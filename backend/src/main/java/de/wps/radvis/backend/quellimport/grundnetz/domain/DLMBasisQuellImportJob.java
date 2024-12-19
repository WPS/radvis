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

package de.wps.radvis.backend.quellimport.grundnetz.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.entity.DLMBasisQuellImportJobStatistik;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DLMBasisQuellImportJob extends AbstractJob {

	private ImportedFeaturePersistentRepository importedFeaturePersistentRepository;
	private DlmRepository dlmImportRepository;
	private AtomicInteger counter;

	public DLMBasisQuellImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		ImportedFeaturePersistentRepository importedFeaturePersistentRepository,
		DlmRepository dlmImportRepository) {
		super(jobExecutionDescriptionRepository);
		this.counter = new AtomicInteger();

		this.importedFeaturePersistentRepository = importedFeaturePersistentRepository;
		this.dlmImportRepository = dlmImportRepository;

	}

	@Override
	@Transactional
	protected Optional<JobStatistik> doRun() {
		DLMBasisQuellImportJobStatistik statistik = new DLMBasisQuellImportJobStatistik();
		log.info("DLM-Basis-Daten werden importiert.");
		statistik.reset();

		List<Envelope> partitions = this.dlmImportRepository.getPartitionen();
		HashSet<String> dlmIds = new HashSet<>();

		log.info("Importiere Strassen Features über {} Partitionen...", partitions.size());
		counter.set(0);
		AtomicInteger partitionCounter = new AtomicInteger();
		partitionCounter.set(0);
		partitions.forEach(partition -> {
			dlmImportRepository.getKanten(partition)
				.forEach(importedFeature -> {
					importFeature(importedFeature, dlmIds);
				});
			log.info("finished partition {}", partitionCounter.incrementAndGet());
		});
		log.info("Es wurden {} Strassen Features importiert.", counter.get());

		statistik.importierteStrassen = counter.get();
		return Optional.of(statistik);
	}

	private void importFeature(ImportedFeature importedFeature, HashSet<String> dlmIds) {
		if (!isAutobahn(importedFeature)) {
			if (counter.get() % 10000 == 0) {
				log.info(counter.get() + " Features importiert");
			}
			if (dlmIds.add(importedFeature.getTechnischeId())) {
				importedFeaturePersistentRepository.save(importedFeature);
				counter.incrementAndGet();
			}
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

}
