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

package de.wps.radvis.backend.matching.domain.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.matching.domain.PbfErstellungsRepository;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlmPbfErstellungService {
	private final KantenRepository kantenRepository;
	private final PbfErstellungsRepository pbfErstellungsRepository;
	private final DLMConfigurationProperties config;
	private final String dlmBasisDaten;

	public DlmPbfErstellungService(KantenRepository kantenRepository,
		PbfErstellungsRepository pbfErstellungsRepository,
		DLMConfigurationProperties config,
		String dlmBasisDaten) {
		this.kantenRepository = kantenRepository;
		this.pbfErstellungsRepository = pbfErstellungsRepository;
		this.config = config;
		this.dlmBasisDaten = dlmBasisDaten;
	}

	public void erstelleDlmPbf() throws IOException {
		log.info("Starte Erstellung der dlmPbf-Datei...");
		File outputFile = new File(dlmBasisDaten);
		File tempOutputFile = File.createTempFile(outputFile.getName(), "temp");

		try {
			Files.createDirectories(outputFile.getParentFile().toPath());
		} catch (IOException e) {
			log.error("Ordner-Struktur konnte nicht erstellt werden: {}", outputFile.getParentFile().getAbsolutePath(),
				e);
		}

		List<Envelope> partitionen = AbstractJob.getPartitionen(config.getExtentProperty(), config.getPbfpartitionen());

		log.info("Hole Kanten-Stream (DLM) aus der DB...");
		Map<Envelope, Stream<Kante>> kantenStreams = partitionen.stream().collect(Collectors.toMap(Function.identity(),
			partition -> kantenRepository.getKantenInBereichNachQuellenEagerFetchFahrtrichtungEagerFetchFuehrungsformAttributeLinks(
				partition,
				Set.of(QuellSystem.DLM, QuellSystem.RadVis))));

		log.info("Schreibe DLM-Kanten als osm.pbf-Daten nach {}.", outputFile.getAbsolutePath());
		pbfErstellungsRepository.writePbf(kantenStreams, tempOutputFile);
		log.info("Schreiben der DlmPbf-Datei beendet.");
		Files.move(
			Paths.get(tempOutputFile.getAbsolutePath()),
			Paths.get(outputFile.getAbsolutePath()),
			StandardCopyOption.REPLACE_EXISTING);
	}
}
