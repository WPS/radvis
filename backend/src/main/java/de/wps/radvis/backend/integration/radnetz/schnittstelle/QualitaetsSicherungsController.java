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

package de.wps.radvis.backend.integration.radnetz.schnittstelle;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.integration.radnetz.domain.repository.QualitaetsSicherungsRepository;
import de.wps.radvis.backend.integration.radnetz.schnittstelle.command.MarkOrganisationAsQualitaetsgesichertCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.NonNull;

@RestController
@RequestMapping("/api/organisationen")
public class QualitaetsSicherungsController {

	private final VerwaltungseinheitService verwaltungseinheitService;
	private final QualitaetsSicherungsRepository qualitaetsSicherungsRepository;
	private final QualitaetsSicherungsGuard qualitaetsSicherungsGuard;

	public QualitaetsSicherungsController(@NonNull VerwaltungseinheitService verwaltungseinheitService,
		QualitaetsSicherungsRepository qualitaetsSicherungsRepository,
		QualitaetsSicherungsGuard qualitaetsSicherungsGuard) {
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.qualitaetsSicherungsRepository = qualitaetsSicherungsRepository;
		this.qualitaetsSicherungsGuard = qualitaetsSicherungsGuard;
	}

	@PostMapping("/markAsQualitaetsgesichert")
	@WithAuditing(context = AuditingContext.MARK_ORGANISATION_AS_QUALITAETSGESICHERT)
	@Transactional
	public void markAsQualitaetsgesichert(
		Authentication authentication,
		@RequestBody @Valid MarkOrganisationAsQualitaetsgesichertCommand command) {
		this.qualitaetsSicherungsGuard.markAsQualitaetsgesichert(authentication, command);

		verwaltungseinheitService.markGebietskoerperschaftAsQualitaetsgesichert(command.getId());
		Verwaltungseinheit landkreis = verwaltungseinheitService.findById(command.getId()).orElseThrow();
		qualitaetsSicherungsRepository.setzeDLMAlsGrundnetz(landkreis);
	}

	@GetMapping("/liegenAlleInQualitaetsgesichertenLandkreisen/{kanteIds}")
	public boolean liegenAlleInQualitaetsgesichertenLandkreisen(
		@PathVariable List<Long> kanteIds) {
		return qualitaetsSicherungsRepository.liegenAlleInQualitaetsgesichertenLandkreisen(kanteIds);
	}
}
