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

package de.wps.radvis.backend.administration.schnittstelle;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.administration.domain.AdministrationService;
import de.wps.radvis.backend.administration.schnittstelle.command.CreateOrganisationCommand;
import de.wps.radvis.backend.administration.schnittstelle.command.SaveOrganisationCommand;
import de.wps.radvis.backend.administration.schnittstelle.view.OrganisationView;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/administration/organisationen")
@Slf4j
public class OrganisationVerwaltungController {
	private final OrganisationRepository organisationRepository;

	private final VerwaltungseinheitService verwaltungseinheitService;
	private final OrganisationGuard organisationGuard;
	private final BenutzerResolver benutzerResolver;
	private final BenutzerService benutzerService;
	private final AdministrationService administrationService;
	private final GebietskoerperschaftRepository gebietskoerperschaftRepository;

	public OrganisationVerwaltungController(
		@NonNull VerwaltungseinheitService verwaltungseinheitService,
		@NonNull OrganisationGuard organisationGuard,
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull BenutzerService benutzerService, AdministrationService administrationService,
		GebietskoerperschaftRepository gebietskoerperschaftRepository,
		OrganisationRepository organisationRepository) {
		this.gebietskoerperschaftRepository = gebietskoerperschaftRepository;
		this.administrationService = administrationService;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.organisationGuard = organisationGuard;
		this.benutzerResolver = benutzerResolver;
		this.benutzerService = benutzerService;
		this.organisationRepository = organisationRepository;
	}

	@GetMapping()
	public List<VerwaltungseinheitView> listNichtGebietskoerperschaften(Authentication authentication) {
		organisationGuard.listNichtGebietskoerperschaften(authentication);

		return verwaltungseinheitService.getAllAsView().stream()
			.filter(orga -> !orga.getOrganisationsArt().istGebietskoerperschaft())
			.map(VerwaltungseinheitView::new)
			.collect(Collectors.toList());
	}

	@GetMapping("zuweisbar")
	public List<VerwaltungseinheitView> listAlleZuweisbaren(Authentication authentication) {
		organisationGuard.listAlleZuweisbaren(authentication);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return administrationService.getAllZuweisbareOrganisationenForBenutzer(benutzer).stream()
			.map(VerwaltungseinheitView::new).collect(Collectors.toList());
	}

	@GetMapping("{id}")
	public OrganisationView organisationView(@PathVariable("id") Long id, Authentication authentication) {
		organisationGuard.organisationView(authentication);

		Organisation organisation = organisationRepository.findById(id).orElseThrow();

		return new OrganisationView(organisation,
			benutzerService.darfBenutzerOrganisationErstellenOderBearbeiten(
				benutzerResolver.fromAuthentication(authentication), organisation));
	}

	@PostMapping(path = "/save")
	public OrganisationView save(Authentication authentication,
		@RequestBody @Valid SaveOrganisationCommand command) {
		organisationGuard.save(authentication, command);

		Organisation zuBearbeitendeOrganisation = administrationService
			.loadForModification(command.getId(), command.getVersion());

		Set<Gebietskoerperschaft> zustaendigkeit = command.getZustaendigFuerBereichOf().stream()
			.map(id -> gebietskoerperschaftRepository.findById(id).orElseThrow(EntityNotFoundException::new))
			.collect(Collectors.toSet());

		zuBearbeitendeOrganisation.update(command.getName(), command.getOrganisationsArt(),
			zustaendigkeit);

		return new OrganisationView(organisationRepository.save(zuBearbeitendeOrganisation),
			true);
	}

	@PostMapping(path = "/create")
	public OrganisationView create(Authentication authentication,
		@RequestBody @Valid CreateOrganisationCommand command) {
		organisationGuard.create(authentication, command);

		Set<Gebietskoerperschaft> zustaendigkeit = command.getZustaendigFuerBereichOf().stream()
			.map(id -> gebietskoerperschaftRepository.findById(id).orElseThrow(EntityNotFoundException::new))
			.collect(Collectors.toSet());

		Organisation organisation = new Organisation(command.getName(),
			benutzerResolver.fromAuthentication(authentication).getOrganisation(),
			command.getOrganisationsArt(), zustaendigkeit, true);

		return new OrganisationView(organisationRepository.save(organisation), true);
	}

	@PostMapping(path = "/deaktiviere-organisation")
	public OrganisationView deaktiviereOrganisation(Authentication authentication, @RequestParam long id) {
		organisationGuard.deaktiviereOrganisation(authentication, id);

		Organisation zuDeaktivierenOrganisation = organisationRepository.findById(id).orElseThrow();

		zuDeaktivierenOrganisation.deaktiviere();

		Organisation saved = organisationRepository.save(zuDeaktivierenOrganisation);
		log.info("{} wurde von Benutzer {} deaktiviert.", saved,
			benutzerResolver.fromAuthentication(authentication).getId());
		return new OrganisationView(saved, true);
	}

	@PostMapping(path = "/aktiviere-organisation")
	public OrganisationView aktiviereOrganisation(Authentication authentication, @RequestParam long id) {
		organisationGuard.aktiviereOrganisation(authentication, id);

		Organisation zuAktivierenOrganisation = organisationRepository.findById(id).orElseThrow();

		zuAktivierenOrganisation.aktiviere();

		Organisation saved = organisationRepository.save(zuAktivierenOrganisation);
		log.info("{} wurde von Benutzer {} aktiviert.", saved,
			benutzerResolver.fromAuthentication(authentication).getId());
		return new OrganisationView(saved, true);
	}
}
