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

package de.wps.radvis.backend.fahrradroute.domain;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteVariante;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradroutenVariantenTfisImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * Importiert Varianten aus TFIS für Landesradfernwege, die bereits im System vorhanden sind.
 * 
 * Fügt neue Varianten hinzu oder aktualisiert, sofern die existierende Variante keine durchgehende Geometrie besitzt
 * (also nicht gematched werden konnte)
 */
@WithFehlercode(Fehlercode.FAHRRADROUTE_TFIS_IMPORT)
public class LandesradfernwegeVariantenTfisImportJob extends AbstractFahrradroutenVariantenTfisImportJob {
	// Dieser Job Name sollte sich nicht mehr aendern, weil Controller und DB Eintraege den Namen verwenden
	public static final String JOB_NAME = "LandesradfernwegVariantenTfisImportJob";
	private final FahrradrouteRepository fahrradrouteRepository;

	public LandesradfernwegeVariantenTfisImportJob(
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		FahrradrouteRepository fahrradrouteRepository,
		ShapeFileRepository shapeFileRepository,
		KantenRepository kantenRepository,
		TfisImportService tfisImportService,
		Path tfisRadwegePath) {
		super(jobExecutionDescriptionRepository, tfisImportService, kantenRepository, shapeFileRepository,
			tfisRadwegePath);
		this.fahrradrouteRepository = fahrradrouteRepository;
	}

	@Override
	public String getName() {
		return LandesradfernwegeVariantenTfisImportJob.JOB_NAME;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.LANDESRADFERNWEGE_VARIANTEN_TFIS_IMPORT_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		FahrradroutenVariantenTfisImportStatistik importStatistik = new FahrradroutenVariantenTfisImportStatistik();

		importFromTfis(importStatistik);

		log.info("JobStatistik: " + importStatistik.toString());
		return Optional.of(importStatistik);
	}

	@Override
	protected void saveFahrradroutenVarianten(TfisId tfisId, List<FahrradrouteVariante> importedVarianten,
		FahrradroutenVariantenTfisImportStatistik importStatistik) {
		Fahrradroute fahrradroute = fahrradrouteRepository.findByTfisId(tfisId)
			.orElseThrow(EntityNotFoundException::new);

		List<FahrradrouteVariante> existingVarianten = new ArrayList<>(fahrradroute.getVarianten());

		for (int i = 0; i < importedVarianten.size(); i++) {
			Optional<FahrradrouteVariante> existingFahrradrouteVariante = fahrradroute.findFahrradrouteVariante(
				importedVarianten.get(i).getTfisId());
			if (existingFahrradrouteVariante.isEmpty()) {
				importStatistik.anzahlVariantenHinzugefuegt++;
				existingVarianten.add(importedVarianten.get(i));
			} else if (existingFahrradrouteVariante.get().getGeometrie().isEmpty()) {
				importStatistik.anzahlVariantenAktualisiert++;
				FahrradrouteVariante updatedFV = importedVarianten.get(i).toBuilder()
					// Die Kategorie wollen wir nicht verändern, falls sie manuell gesetzt wurde
					.kategorie(existingFahrradrouteVariante.get().getKategorie())
					.id(existingFahrradrouteVariante.get().getId())
					.build();
				existingVarianten.remove(existingFahrradrouteVariante.get());
				existingVarianten.add(updatedFV);
			}
		}
		fahrradroute.replaceFahrradrouteVarianten(existingVarianten);
		fahrradrouteRepository.save(fahrradroute);
	}

	@Override
	protected Set<TfisId> getZuBeruecksichtigendeFahrradroutenTfisIds() {
		return fahrradrouteRepository.findAllTfisIdsOfLandesradfernwege();
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Importiert LRFW-Varianten aus der TFIS-Datei von Platte und versucht diese auf das Netz zu matchen. Bestehende Varianten werden aktualisiert, nicht mehr vorhandene entfernt.",
			"Listen von Varianten an TFIS-LRFW-Fahrradrouten verändern sich.",
			"Profilinformationen sind hiernach ggf. veraltet oder leer. Sollte nach allen netzverändernden Jobs laufen.",
			JobExecutionDurationEstimate.UNKNOWN
		);
	}
}
