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

package de.wps.radvis.backend.furtKreuzung.schnittstelle;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.schnittstelle.view.EnumOptionListView;
import de.wps.radvis.backend.furtKreuzung.domain.FurtKreuzungService;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.repository.FurtKreuzungRepository;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtKreuzungMusterloesung;
import de.wps.radvis.backend.furtKreuzung.schnittstelle.view.FurtKreuzungEditView;
import de.wps.radvis.backend.furtKreuzung.schnittstelle.view.FurtKreuzungListenView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/furtkreuzung")
@Validated
public class FurtKreuzungController {
	private final FurtKreuzungRepository furtKreuzungRepository;

	private final SaveFurtKreuzungCommandConverter saveFurtKreuzungCommandConverter;

	private final FurtKreuzungService furtKreuzungService;

	private final FurtKreuzungGuard furtKreuzungGuard;

	private final BenutzerResolver benutzerResolver;

	public FurtKreuzungController(FurtKreuzungRepository furtKreuzungRepository,
		SaveFurtKreuzungCommandConverter createFurtKreuzungCommandConverter,
		FurtKreuzungService furtKreuzungService,
		FurtKreuzungGuard furtKreuzungGuard, BenutzerResolver benutzerResolver) {
		this.furtKreuzungRepository = furtKreuzungRepository;
		this.saveFurtKreuzungCommandConverter = createFurtKreuzungCommandConverter;
		this.furtKreuzungService = furtKreuzungService;
		this.furtKreuzungGuard = furtKreuzungGuard;
		this.benutzerResolver = benutzerResolver;
	}

	@PostMapping("new")
	@WithAuditing(context = AuditingContext.SAVE_FURT_KREUZUNG_COMMAND)
	public Long createFurtKreuzung(@RequestBody @Valid SaveFurtKreuzungCommand command, Authentication authentication) {
		FurtKreuzung furtKreuzung = saveFurtKreuzungCommandConverter.convert(command);
		furtKreuzungGuard.createFurtKreuzung(authentication, furtKreuzung.getVerantwortlicheOrganisation());

		FurtKreuzung saved = this.furtKreuzungRepository.save(furtKreuzung);
		return saved.getId();
	}

	@PostMapping("{id}")
	@WithAuditing(context = AuditingContext.SAVE_FURT_KREUZUNG_COMMAND)
	public FurtKreuzungEditView updateFurtKreuzung(@PathVariable("id") Long id,
		@RequestBody @Valid SaveFurtKreuzungCommand command, Authentication authentication) {
		FurtKreuzung furtKreuzung = furtKreuzungService.loadForModification(id, command.getVersion());
		furtKreuzungGuard.updateFurtKreuzung(authentication, furtKreuzung.getVerantwortlicheOrganisation());

		saveFurtKreuzungCommandConverter.apply(furtKreuzung, command);
		return new FurtKreuzungEditView(furtKreuzungRepository.save(furtKreuzung), true);
	}

	@GetMapping("{id}")
	public FurtKreuzungEditView getFurtKreuzung(@PathVariable Long id, Authentication authentication) {
		Optional<FurtKreuzung> furtKreuzung = furtKreuzungRepository.findById(id);

		if (furtKreuzung.isPresent()) {
			boolean darfBearbeiten = furtKreuzungService.darfNutzerBearbeiten(
				benutzerResolver.fromAuthentication(authentication), furtKreuzung.get());
			return new FurtKreuzungEditView(furtKreuzung.get(), darfBearbeiten);

		} else {
			throw new EntityNotFoundException();
		}

	}

	@GetMapping("/musterloesung/list")
	public Stream<EnumOptionListView> getFurtKreuzungMusterloesungen() {
		return FurtKreuzungMusterloesung.getWertebereich().stream()
			.map(enm -> new EnumOptionListView(enm.name(), enm.getDisplayText()));
	}

	@GetMapping("/list")
	public List<FurtKreuzungListenView> getAlleFurtenKreuzungen() {
		return StreamSupport.stream(furtKreuzungRepository.findAll().spliterator(), false)
			.map(FurtKreuzungListenView::new)
			.collect(Collectors.toList());
	}
}
