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

package de.wps.radvis.backend.organisation.domain;

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerwaltungseinheitImportJob extends AbstractJob {

	// Achtung! Unterscheidet sich von dem Klassennamen, weil wir damit verhindern,
	// dass der Job ungewollt erneut ausgefuehrt wird.
	protected static final String JOB_NAME = "OrganisationenImportJob";

	private final VerwaltungseinheitImportRepository verwaltungseinheitImportRepository;
	private final OrganisationRepository organisationRepository;
	private final GebietskoerperschaftRepository gebietskoerperschaftRepository;

	private final File bundeslandFile;
	private final File regierungsbezirkFile;
	private final File landkreisFile;
	private final File gemeindeFile;

	public VerwaltungseinheitImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		VerwaltungseinheitImportRepository verwaltungseinheitImportRepository,
		OrganisationRepository organisationRepository, GebietskoerperschaftRepository gebietskoerperschaftRepository,
		File verwaltungsgrenzenVerzeichnis) {
		super(jobExecutionDescriptionRepository);

		require(verwaltungseinheitImportRepository, notNullValue());

		require(verwaltungsgrenzenVerzeichnis.exists(),
			"Verwaltungsgrenzenverzeichnis existiert nicht: " + verwaltungsgrenzenVerzeichnis.getAbsolutePath());
		require(verwaltungsgrenzenVerzeichnis.isDirectory(),
			"Verwaltungsgrenzenverzeichnis ist kein Verzeichnis: " + verwaltungsgrenzenVerzeichnis.getAbsolutePath());

		this.verwaltungseinheitImportRepository = verwaltungseinheitImportRepository;

		this.organisationRepository = organisationRepository;
		this.gebietskoerperschaftRepository = gebietskoerperschaftRepository;

		this.bundeslandFile = new File(verwaltungsgrenzenVerzeichnis, "v_at_land.shp");
		this.regierungsbezirkFile = new File(verwaltungsgrenzenVerzeichnis, "v_at_regierungsbezirk.shp");
		this.landkreisFile = new File(verwaltungsgrenzenVerzeichnis, "v_at_kreis.shp");
		this.gemeindeFile = new File(verwaltungsgrenzenVerzeichnis, "v_at_gemeinde.shp");

		require(bundeslandFile.exists(), "Bundesland File existiert nicht: " + bundeslandFile.getAbsolutePath());
		require(regierungsbezirkFile.exists(),
			"Regierungsbezirk File existiert nicht: " + regierungsbezirkFile.getAbsolutePath());
		require(landkreisFile.exists(), "Landkreis File existiert nicht: " + landkreisFile.getAbsolutePath());
		require(gemeindeFile.exists(), "Gemeinde File existiert nicht: " + gemeindeFile.getAbsolutePath());
	}

	@Override
	public String getName() {
		return JOB_NAME;
	}

	@Override
	@Transactional
	public JobExecutionDescription run() {
		JobExecutionDescription description = super.run();
		this.registriereZusaetzlicheOrganisationen();
		return description;
	}

	@Override
	@Transactional
	public JobExecutionDescription run(boolean force) {
		JobExecutionDescription description = super.run(force);
		this.registriereZusaetzlicheOrganisationen();
		return description;
	}

	public boolean isRepeatable() {
		return false;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		try {
			gebietskoerperschaftRepository.saveAll(verwaltungseinheitImportRepository
				.getGebietskoerperschaftenFromShapeFiles(bundeslandFile, regierungsbezirkFile, landkreisFile,
					gemeindeFile));

			organisationRepository.saveAll(
				verwaltungseinheitImportRepository.getCustomAdditionalOrganisationen());

			log.info("Organisationen wurden erfolgreich in die Datenbank geschrieben.");
		} catch (IOException e) {
			log.error("Fehler beim Einlesen der Organisationen");
			throw new RuntimeException(e);
		}

		return Optional.empty();
	}

	private void registriereZusaetzlicheOrganisationen() {
		if (organisationRepository.findByName("Toubiz").isPresent()) {
			log.info("Eine Organisation mit dem Namen 'Toubiz' existiert bereits. Das Anlegen wird übersprungen.");
			return;
		}

		Optional<Gebietskoerperschaft> bw = gebietskoerperschaftRepository.findByName("Baden-Württemberg");

		if (bw.isEmpty()) {
			log.warn(
				"Baden-Württemberg existiert nicht als Organisation in der DB. Das Anlegen wird übersprungen, da der Bereich nicht ermittelt werden kann");
			return;
		}

		organisationRepository.save(
			new Organisation("Toubiz", null, OrganisationsArt.SONSTIGES, Set.of(bw.get()), true));
	}
}
