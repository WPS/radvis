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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.matching.domain.repository.PbfErstellungsRepository;
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
		File outputFile = new File(dlmBasisDaten);
		log.info("Starte Erstellung von PBF-Datei {}", outputFile.getAbsolutePath());
		try {
			Files.createDirectories(outputFile.getParentFile().toPath());
		} catch (IOException e) {
			throw new IOException("Ordner-Struktur für Datei " + outputFile.getAbsolutePath()
				+ " konnte nicht erstellt werden", e);
		}

		File tempOutputFile = File.createTempFile(outputFile.getName(), "temp");

		List<Envelope> partitionen = AbstractJob.getPartitionen(config.getExtentProperty(), config.getPbfpartitionen());

		log.debug("Hole Kanten-Streams aus der DB");
		Map<Envelope, Stream<Kante>> kantenStreams = partitionen.stream().collect(Collectors.toMap(Function.identity(),
			partition -> kantenRepository
				.getKantenInBereichNachQuellenEagerFetchFahrtrichtungEagerFetchFuehrungsformAttributeLinks(
					partition,
					Set.of(QuellSystem.DLM, QuellSystem.RadVis))));

		pbfErstellungsRepository.writePbf(kantenStreams, tempOutputFile);

		log.info("Verschiebe temporäre PBF-Datei {} nach {}", tempOutputFile.getAbsolutePath(), outputFile
			.getAbsolutePath());
		Files.move(
			Paths.get(tempOutputFile.getAbsolutePath()),
			Paths.get(outputFile.getAbsolutePath()),
			StandardCopyOption.REPLACE_EXISTING);
	}

	public void erstellePbfForKanten(File outputFile, Collection<Kante> kanten) throws IOException {
		log.info("Starte Erstellung von PBF-Datei {} für {} Kanten", outputFile.getAbsolutePath(), kanten.size());

		// Wir überspringen die Korrekte Erstellung von Partitionen der Einfachheit halber: Die Kanten sind bereits im
		// Speicher und können direkt verarbeitet werden. Eine Partitionierung hätte hier also erst einmal kaum bis
		// keine Vorteile.
		Envelope partition = AbstractJob.getPartitionen(config.getExtentProperty(), 1).get(0);

		Map<Envelope, Stream<Kante>> kantenStreamMap = new HashMap<>();
		kantenStreamMap.put(partition, kanten.stream());

		pbfErstellungsRepository.writePbf(kantenStreamMap, outputFile);

		log.info("DLM-PBF Erstellung beendet");
	}
}
