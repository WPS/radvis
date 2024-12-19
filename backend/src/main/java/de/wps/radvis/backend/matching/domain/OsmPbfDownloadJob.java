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

package de.wps.radvis.backend.matching.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithFehlercode(Fehlercode.OSM_DOWNLOAD)
public class OsmPbfDownloadJob extends AbstractJob {

	private final String osmBasisDatenDownloadLink;
	private final File osmBasisDaten;

	public OsmPbfDownloadJob(JobExecutionDescriptionRepository repository, File osmBasisDaten,
		String osmBasisDatenDownloadLink) {
		super(repository);
		require(osmBasisDaten, notNullValue());
		require(osmBasisDatenDownloadLink, notNullValue());

		this.osmBasisDatenDownloadLink = osmBasisDatenDownloadLink;
		this.osmBasisDaten = osmBasisDaten;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		log.info("Aktualisiere die Osm Basis Pbf Datei von: {}", osmBasisDatenDownloadLink);

		try {
			BufferedInputStream in = new BufferedInputStream(new URL(osmBasisDatenDownloadLink).openStream());
			Files.copy(in, osmBasisDaten.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (MalformedURLException e) {
			log.error(
				"Der angegebene Downloadlink ist keine gültige Url. Im weiteren Verlauf wird die letzte bekannte OsmPbfDatei verwendet: {}",
				osmBasisDaten.getAbsolutePath());
		} catch (IOException e) {
			log.error(
				"Fehler beim Download der aktuellen Osm Basis Pbf. Im weiteren Verlauf wird die letzte bekannte OsmPbfDatei verwendet: {}",
				osmBasisDaten.getAbsolutePath());
		}

		return Optional.empty();
	}
}
