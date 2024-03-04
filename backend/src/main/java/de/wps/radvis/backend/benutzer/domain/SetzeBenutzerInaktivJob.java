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

package de.wps.radvis.backend.benutzer.domain;

import java.util.List;
import java.util.Optional;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.SetzeBenutzerInaktivJobStatistik;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerIstNichtRegistriertException;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SetzeBenutzerInaktivJob extends AbstractJob {
	private final BenutzerService benutzerService;

	private final Integer inaktivitaetsTimeoutInTagen;

	public SetzeBenutzerInaktivJob(
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		BenutzerService benutzerService,
		Integer inaktivitaetsTimeoutInTagen
	) {
		super(jobExecutionDescriptionRepository);

		this.benutzerService = benutzerService;
		this.inaktivitaetsTimeoutInTagen = inaktivitaetsTimeoutInTagen;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		SetzeBenutzerInaktivJobStatistik setzeBenutzerInaktivJobStatistik = new SetzeBenutzerInaktivJobStatistik();

		List<Benutzer> benutzerListe = benutzerService.ermittleAktiveBenutzerInaktivLaengerAls(
			inaktivitaetsTimeoutInTagen
		);
		setzeBenutzerInaktivJobStatistik.anzahlInaktivZuSetzenderBenutzer = benutzerListe.size();

		benutzerListe.forEach(benutzer -> {
			try {
				benutzerService.aendereBenutzerstatus(benutzer.getId(), benutzer.getVersion(), BenutzerStatus.INAKTIV);
				setzeBenutzerInaktivJobStatistik.anzahlErfolgreicherInaktivsetzungen++;
			} catch (BenutzerIstNichtRegistriertException | OptimisticLockException e) {
				setzeBenutzerInaktivJobStatistik.anzahlFehlgeschlagenerInaktivsetzungen++;
			}
		});

		log.info(setzeBenutzerInaktivJobStatistik.toString());
		return Optional.of(setzeBenutzerInaktivJobStatistik);
	}
}
