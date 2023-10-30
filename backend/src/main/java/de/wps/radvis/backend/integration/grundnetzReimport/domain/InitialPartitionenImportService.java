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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.DLMInitialImportJobStatistik;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMWFSImportRepository;
import lombok.extern.slf4j.Slf4j;

// Wir brauchen hier eine separate Klasse wegen @Transactional (Proxying der Bean)
@Slf4j
public class InitialPartitionenImportService {

	private final CreateKantenService createKantenService;
	private final DLMWFSImportRepository dlmWfsImportRepository;
	private final NetzService netzService;

	public InitialPartitionenImportService(CreateKantenService createKantenService,
		DLMWFSImportRepository dlmWfsImportRepository, NetzService netzService) {
		this.createKantenService = createKantenService;
		this.dlmWfsImportRepository = dlmWfsImportRepository;
		this.netzService = netzService;
	}

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public void importPartition(Envelope partition, Set<String> importierteDlmIds,
		DLMInitialImportJobStatistik dlmInitialImportJobStatistik) {

		KnotenIndex knotenIndex = new KnotenIndex();
		netzService.getKnotenInBereichNachQuelle(partition, QuellSystem.DLM)
			.forEach(knotenIndex::fuegeEin);
		// strassenFeatures werden erst gesammelt, da sonst der Endpunkt die Verbindung kappt
		List<ImportedFeature> strassenFeatures = dlmWfsImportRepository.readStrassenFeatures(partition).collect(
			Collectors.toList());
		strassenFeatures.forEach(importedFeature -> {
			importFeature(importedFeature, importierteDlmIds, knotenIndex, dlmInitialImportJobStatistik);
			++dlmInitialImportJobStatistik.abgearbeiteteStrassen;
		});

		// wegeFeatures werden erst gesammelt, da sonst der Endpunkt die Verbindung kappt
		List<ImportedFeature> wegeFeatures = dlmWfsImportRepository.readWegeFeatures(partition)
			.collect(Collectors.toList());
		wegeFeatures.forEach(importedFeature -> {
			importFeature(importedFeature, importierteDlmIds, knotenIndex, dlmInitialImportJobStatistik);
			++dlmInitialImportJobStatistik.abgearbeiteteWege;
		});

	}

	private void importFeature(ImportedFeature importedFeature, Set<String> importierteDlmIds, KnotenIndex knotenIndex,
		DLMInitialImportJobStatistik dlmInitialImportJobStatistik) {
		if (isAutobahn(importedFeature)) {
			dlmInitialImportJobStatistik.autobahnen++;
			return;
		}
		if (!importierteDlmIds.contains(importedFeature.getTechnischeId())) {
			createKantenService.createNewDLMKante(importedFeature, dlmInitialImportJobStatistik,
				knotenIndex);
			importierteDlmIds.add(importedFeature.getTechnischeId());
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
