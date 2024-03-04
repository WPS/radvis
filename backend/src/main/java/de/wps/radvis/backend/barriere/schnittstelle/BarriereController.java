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

package de.wps.radvis.backend.barriere.schnittstelle;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.barriere.domain.BarriereService;
import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.barriere.schnittstelle.view.BarriereEditView;
import de.wps.radvis.backend.barriere.schnittstelle.view.BarriereListenView;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/barriere")
public class BarriereController {

	private final BarriereRepository barriereRepository;

	private final SaveBarriereCommandConverter saveBarriereCommandConverter;

	private final BarriereService barriereService;

	private final BarriereGuard barriereGuard;

	private final BenutzerResolver benutzerResolver;

	public BarriereController(BarriereRepository barriereRepository,
		SaveBarriereCommandConverter saveBarriereCommandConverter,
		BarriereService barriereService, BarriereGuard barriereGuard,
		BenutzerResolver benutzerResolver) {
		this.barriereRepository = barriereRepository;
		this.saveBarriereCommandConverter = saveBarriereCommandConverter;
		this.barriereService = barriereService;
		this.barriereGuard = barriereGuard;
		this.benutzerResolver = benutzerResolver;
	}

	@PostMapping("/new")
	@WithAuditing(context = AuditingContext.SAVE_BARRIERE_COMMAND)
	public Long createBarriere(@RequestBody @Valid SaveBarriereCommand command, Authentication authentication) {
		Barriere barriere = saveBarriereCommandConverter.convert(command);
		barriereGuard.createBarriere(authentication, barriere.getVerantwortlich());

		Barriere saved = this.barriereRepository.save(barriere);
		return saved.getId();
	}

	@PostMapping("{id}")
	@WithAuditing(context = AuditingContext.SAVE_BARRIERE_COMMAND)
	public BarriereEditView updateBarriere(@PathVariable("id") Long id,
		@RequestBody @Valid SaveBarriereCommand command, Authentication authentication) {
		Barriere barriere = barriereService.loadForModification(id, command.getVersion());
		barriereGuard.updateBarriere(authentication, barriere.getVerantwortlich());

		saveBarriereCommandConverter.apply(barriere, command);
		return new BarriereEditView(barriereRepository.save(barriere), true);
	}

	@GetMapping("{id}")
	public BarriereEditView getBarriere(@PathVariable("id") Long id, Authentication authentication) {
		Optional<Barriere> barriere = barriereRepository.findById(id);

		if (barriere.isPresent()) {
			boolean darfBearbeiten = barriereService.darfNutzerBearbeiten(
				benutzerResolver.fromAuthentication(authentication), barriere.get());
			return new BarriereEditView(barriere.get(), darfBearbeiten);

		} else {
			throw new EntityNotFoundException();
		}
	}

	@GetMapping("/list")
	public List<BarriereListenView> getAlleBarriere() {
		return StreamSupport.stream(barriereRepository.findAll().spliterator(), false).map(BarriereListenView::new)
			.collect(Collectors.toList());
	}
}
