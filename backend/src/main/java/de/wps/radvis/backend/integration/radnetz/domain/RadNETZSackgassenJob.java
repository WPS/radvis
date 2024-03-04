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

package de.wps.radvis.backend.integration.radnetz.domain;

import static org.valid4j.Assertive.require;

import java.util.Optional;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerBeschreibung;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;
import jakarta.transaction.Transactional;

public class RadNETZSackgassenJob extends AbstractJob {

	private final NetzfehlerRepository netzfehlerRepository;
	private final RadNETZNachbearbeitungsRepository radNETZNachbearbeitungsRepository;

	public RadNETZSackgassenJob(NetzfehlerRepository netzfehlerRepository,
		RadNETZNachbearbeitungsRepository radNETZNachbearbeitungsRepository,
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository) {
		super(jobExecutionDescriptionRepository);

		require(netzfehlerRepository, Matchers.notNullValue());
		require(radNETZNachbearbeitungsRepository, Matchers.notNullValue());

		this.netzfehlerRepository = netzfehlerRepository;
		this.radNETZNachbearbeitungsRepository = radNETZNachbearbeitungsRepository;
	}

	@Override
	@Transactional
	protected Optional<JobStatistik> doRun() {
		Stream<Geometry> points = radNETZNachbearbeitungsRepository.getKnotenMitHoechstensEinerAdjazentenRadNETZKante();

		points.forEach(point -> {
			netzfehlerRepository.save(
				new Netzfehler(NetzfehlerTyp.RADNETZ_SACKGASSE,
					NetzfehlerBeschreibung.of(
						"In dem Zielnetz gibt es eine Sackgasse im Bezug auf die RadNETZ Abbildung."
							+ " Dies deutet wahrscheinlich auf eine fehlerhafte Abbildung hin."),
					this.getName(), point));
		});

		return Optional.empty();
	}
}
