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

package de.wps.radvis.backend.massnahme.domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmenblatterImportProtokoll;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MassnahmenblaetterImportJob extends AbstractJob {

	@PersistenceContext
	private EntityManager entityManager;

	private final Path dokukatasterFileFolder;
	private final Path massnahmenkatasterFileFolder;
	private final MassnahmeService massnahmeService;
	private final BenutzerService benutzerService;

	public MassnahmenblaetterImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		Path dokukatasterFileFolder, Path massnahmenkatasterFileFolder, MassnahmeService massnahmeService,
		BenutzerService benutzerService) {
		super(jobExecutionDescriptionRepository);
		this.dokukatasterFileFolder = dokukatasterFileFolder;
		this.massnahmenkatasterFileFolder = massnahmenkatasterFileFolder;
		this.massnahmeService = massnahmeService;
		this.benutzerService = benutzerService;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MASSNAHMEBLAETTER_IMPORT_JOB)
	public JobExecutionDescription run() {
		return super.run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MASSNAHMEBLAETTER_IMPORT_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {

		Benutzer technischerBenutzer = benutzerService.getTechnischerBenutzer();

		MassnahmenblatterImportProtokoll protokoll = new MassnahmenblatterImportProtokoll();

		protokoll.massnahmenblaetterGesammt = getMassnahmenkatasterFiles().count();
		log.info("Es wurden {} Massnahmenblaetter gefunden", protokoll.massnahmenblaetterGesammt);
		protokoll.dokublaetterGesammt = getDokukatasterFiles().count();
		log.info("Es wurden {} Dokublaetter gefunden", protokoll.dokublaetterGesammt);

		massnahmenblaetterAnhaengen(getMassnahmenkatasterFiles(), technischerBenutzer, protokoll);
		dokulaetterAnhaengen(getDokukatasterFiles(), technischerBenutzer, protokoll);

		protokoll.log();
		return Optional.empty();
	}

	private void massnahmenblaetterAnhaengen(Stream<File> massnahmeBlaetter, Benutzer technischerBenutzer,
		MassnahmenblatterImportProtokoll protokoll) {

		massnahmeBlaetter.forEach(massnahmenblattFile -> {
			// Massnahme-Paket-ID ermitteln:
			String massnahmePacketId = MassnahmenPaketIdExtractor.normalize(
				massnahmenblattFile.getName().replace(".pdf", ""));
			log.info(massnahmePacketId);

			// Passende Massnahmen finden:
			List<Massnahme> massnahmen = findMassnahmen(massnahmePacketId, protokoll);

			fuegeDatenblattAn(massnahmen, massnahmePacketId, massnahmenblattFile,
				technischerBenutzer, protokoll);

			if (!massnahmen.isEmpty()) {
				int counter = protokoll.countVerarbeiteteMassnahmenBlaetter.incrementAndGet();
				if (counter % 500 == 0) {
					entityManager.flush();
					entityManager.clear();
				}
			}
		});
	}

	private void dokulaetterAnhaengen(Stream<File> dokuBlaetter, Benutzer technischerBenutzer,
		MassnahmenblatterImportProtokoll protokoll) {

		dokuBlaetter.forEach(dokublattFile -> {
			// Massnahme-Paker-ID ermitteln:
			String massnahmePacketId = MassnahmenPaketIdExtractor.normalize(
				dokublattFile.getName().replace("_Dok_1.pdf", ""));
			log.info(massnahmePacketId);

			// Passende Massnahmen finden:
			List<Massnahme> massnahmen = findMassnahmen(massnahmePacketId, protokoll);

			fuegeDatenblattAn(massnahmen, massnahmePacketId, dokublattFile,
				technischerBenutzer, protokoll);

			if (!massnahmen.isEmpty()) {
				int counter = protokoll.countVerarbeiteteDokuBlaetter.incrementAndGet();
				if (counter % 500 == 0) {
					entityManager.flush();
					entityManager.clear();
				}
			}
		});
	}

	/***
	 * Fügt das Datenblatt an jede der übergebene Massnahme an. Protokolliert, wenn es Fehler beim Anhängen gab.
	 */
	private void fuegeDatenblattAn(List<Massnahme> massnahmen, String massnahmePacketId, File massnahmenblattFile,
		Benutzer technischerBenutzer, MassnahmenblatterImportProtokoll protokoll) {

		massnahmen.forEach(massnahme -> {
			log.info("Für MPIP {} wurde die Massnahme {}-{} gefunden", massnahmePacketId, massnahme.getId(),
				massnahme.getBezeichnung());
			try {
				byte[] fileContent = Files.readAllBytes(massnahmenblattFile.toPath());
				Dokument massnahmenBlattDokument = new Dokument(
					massnahmenblattFile.getName(),
					technischerBenutzer,
					fileContent,
					LocalDateTime.now());
				log.info("Dokument {} wurde angelegt", massnahmenBlattDokument.getDateiname());
				massnahmeService.haengeDateiAn(massnahme, massnahmenBlattDokument);
				protokoll.erfolgreicheZuordnung.incrementAndGet();
			} catch (IOException e) {
				log.error("Datei {} konnte nicht verarbeitet werden", massnahmenblattFile.getName());
				protokoll.reportDokumentNichtVerarbeitbar(massnahmePacketId);
			}
		});
	}

	private List<Massnahme> findMassnahmen(String massnahmePacketId, MassnahmenblatterImportProtokoll protokoll) {
		if (MassnahmenPaketId.isValid(massnahmePacketId)) {
			List<Massnahme> massnahmen = massnahmeService.findByMassnahmePaketId(
				MassnahmenPaketId.of(massnahmePacketId));

			//Wenn zur PaketId keine Massnahme Gefunden wurde
			if (massnahmen.isEmpty()) {
				log.info("Es wurde keine Massnahme mit der MPID {} gefunden", massnahmePacketId);
				protokoll.reportKeinePassendeMassnahme(massnahmePacketId);
				protokoll.blaetterOhneMassnahme.incrementAndGet();
			}

			/*
			 * Wenn passende Massnahmen mit Massnahmen_paket_id gefunden wurden, so werden die Datenblaetter dort angehängt.
			 * Eigentlich sollten die Massnahmen eindeutig sein, aber es kann vorkommen dass es mehrere Massnehmen für die selbe MPID gibt, welche zusammen gehören.
			 * In diesem Fall wird das Datenblatt jeweils an alle passenden Massnahmen angehängt. Dieser Fall wird protokolliert.
			 */
			if (massnahmen.size() > 1) {
				protokoll.reportMehrdeutigeMassnahmenPaketId(massnahmePacketId);
				protokoll.blaetterMitMehrdeutigerMPID.incrementAndGet();
			}

			return massnahmen;
		}
		protokoll.reportUngueltigeMassnahmePaketId(massnahmePacketId);
		protokoll.undgueltigeMPID.incrementAndGet();
		return Collections.emptyList();
	}

	private Stream<File> getDokukatasterFiles() {
		File rootLevelDirectory = dokukatasterFileFolder.toFile();
		if (!rootLevelDirectory.exists()) {
			throw new RuntimeException("Dokukatasterblätter rootLevelDirectory does not not exist");
		}
		File[] dokukatasterFiles = Objects.requireNonNull(
			rootLevelDirectory.listFiles((dir, name) -> name.endsWith("Dok_1.pdf")));
		return Arrays.stream(dokukatasterFiles);
	}

	private Stream<File> getMassnahmenkatasterFiles() {
		File rootLevelDirectory = massnahmenkatasterFileFolder.toFile();
		if (!rootLevelDirectory.exists()) {
			throw new RuntimeException("Massnahmenkatasterblätter rootLevelDirectory does not not exist");
		}
		File[] dokukatasterFiles = Objects.requireNonNull(
			rootLevelDirectory.listFiles((dir, name) -> name.endsWith(".pdf")));
		return Arrays.stream(dokukatasterFiles);
	}

}
