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

package de.wps.radvis.backend.fahrradroute.schnittstelle;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.fahrradroute.domain.FahrradrouteService;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.schnittstelle.view.FahrradrouteDetailView;
import de.wps.radvis.backend.fahrradroute.schnittstelle.view.FahrradrouteImportprotokollView;
import de.wps.radvis.backend.fahrradroute.schnittstelle.view.FahrradrouteListenView;
import de.wps.radvis.backend.fahrradroute.schnittstelle.view.ProfilRoutingResultView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.NonNull;

@RestController
@RequestMapping("/api/fahrradroute")
public class FahrradrouteController {
	private final FahrradrouteService fahrradrouteService;
	private final SaveFahrradrouteCommandConverter saveFahrradrouteCommandConverter;
	private final CreateFahrradrouteCommandConverter createFahrradrouteCommandConverter;
	private final FahrradrouteGuard fahrradrouteGuard;
	private final BenutzerResolver benutzerResolver;
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	private FahrradrouteRepository fahrradrouteRepository;

	public FahrradrouteController(@NonNull FahrradrouteService fahrradrouteService,
		@NonNull FahrradrouteGuard fahrradrouteGuard,
		@NonNull SaveFahrradrouteCommandConverter saveFahrradrouteCommandConverter,
		CreateFahrradrouteCommandConverter createFahrradrouteCommandConverter,
		@NonNull BenutzerResolver benutzerResolver,
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		FahrradrouteRepository fahrradrouteRepository) {
		this.fahrradrouteService = fahrradrouteService;
		this.fahrradrouteGuard = fahrradrouteGuard;
		this.saveFahrradrouteCommandConverter = saveFahrradrouteCommandConverter;
		this.createFahrradrouteCommandConverter = createFahrradrouteCommandConverter;
		this.benutzerResolver = benutzerResolver;
		this.jobExecutionDescriptionRepository = jobExecutionDescriptionRepository;
		this.fahrradrouteRepository = fahrradrouteRepository;
	}

	@GetMapping("{id}")
	public FahrradrouteDetailView getFahrradroute(Authentication authentication, @PathVariable("id") Long id) {
		Fahrradroute fahrradroute = fahrradrouteService.get(id);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		return getFahrradrouteDetailView(fahrradroute, benutzer);
	}

	@GetMapping("/list")
	public List<FahrradrouteListenView> getAlleFahrradrouten() {
		return fahrradrouteService.getAlleFahrradrouteListenOhneGeomViews().stream().map(FahrradrouteListenView::new)
			.collect(Collectors.toList());
	}

	@GetMapping("/importprotokoll/{id}")
	public FahrradrouteImportprotokollView getFahrradroutenImportprotokoll(@PathVariable("id") Long id) {
		JobExecutionDescription jobExecutionDescription = jobExecutionDescriptionRepository.findById(id)
			.orElseThrow(EntityNotFoundException::new);
		List<String> geloescht = fahrradrouteRepository.findAllNamesOfDeletedByJobId(jobExecutionDescription.getId());
		List<String> erstellt = fahrradrouteRepository.findAllNamesOfInsertedByJobId(jobExecutionDescription.getId());
		return new FahrradrouteImportprotokollView(jobExecutionDescription, geloescht, erstellt);
	}

	@PostMapping("/create")
	@WithAuditing(context = AuditingContext.CREATE_FAHRRADROUTE_COMMAND)
	public Long createFahrradroute(Authentication authentication,
		@RequestBody @Valid CreateFahrradrouteCommand command) {
		fahrradrouteGuard.createFahrradroute(authentication, command);
		Fahrradroute fahrradroute = createFahrradrouteCommandConverter.convert(authentication, command);
		return fahrradrouteService.saveFahrradroute(fahrradroute).getId();
	}

	@PostMapping("/save")
	@WithAuditing(context = AuditingContext.SAVE_FAHRRADROUTE_COMMAND)
	public FahrradrouteDetailView saveFahrradroute(Authentication authentication,
		@RequestBody @Valid SaveFahrradrouteCommand command) {
		Fahrradroute fahrradroute = fahrradrouteService.loadForModification(command.getId(),
			command.getVersion());

		fahrradrouteGuard.saveFahrradroute(authentication, command, fahrradroute);

		saveFahrradrouteCommandConverter.apply(command, fahrradroute);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return getFahrradrouteDetailView(fahrradrouteService.saveFahrradroute(fahrradroute), benutzer);
	}

	@WithAuditing(context = AuditingContext.DELETE_FAHRRADROUTE_COMMAND)
	@DeleteMapping("{id}")
	public void deleteFahrradroute(@PathVariable("id") Long id, Authentication authentication,
		@RequestBody @Valid DeleteFahrradrouteCommand command) {
		Fahrradroute fahrradroute = fahrradrouteService.loadForModification(command.getId(),
			command.getVersion());
		fahrradrouteGuard.deleteFahrradroute(authentication, fahrradroute);
		fahrradroute.alsGeloeschtMarkieren();
		fahrradrouteService.saveFahrradroute(fahrradroute);
	}

	@WithAuditing(context = AuditingContext.CHANGE_FAHRRADROUTE_VEROEFFENTLICHT_COMMAND)
	@PostMapping()
	public FahrradrouteDetailView changeVeroeffentlicht(Authentication authentication,
		@RequestBody @Valid ChangeFahrradrouteVeroeffentlichtCommand command) {
		fahrradrouteGuard.changeVeroeffentlicht(authentication, command);

		Fahrradroute fahrradroute = fahrradrouteService.loadForModification(command.getId(),
			command.getVersion());
		if (command.isVeroeffentlicht()) {
			fahrradroute.veroeffentlichen();
		} else {
			fahrradroute.veroeffentlichungZuruecknehmen();
		}

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return getFahrradrouteDetailView(fahrradrouteService.saveFahrradroute(fahrradroute), benutzer);
	}

	@PostMapping("/routing")
	public Optional<ProfilRoutingResultView> createStrecke(@RequestBody Geometry stuetzpunkte,
		@RequestParam(name = "customProfileId", required = false, defaultValue = "0") long customProfileId,
		@RequestParam(name = "mitFahrtrichtung", required = false, defaultValue = "true") boolean fahrtrichtungBeruecksichtigen
	) {
		return fahrradrouteService.createStrecke((LineString) stuetzpunkte, customProfileId,
				fahrtrichtungBeruecksichtigen)
			.map(ProfilRoutingResultView::new);
	}

	private FahrradrouteDetailView getFahrradrouteDetailView(Fahrradroute fahrradroute, Benutzer benutzer) {
		boolean anyKanteInZustaendigkeitsbereich = fahrradrouteGuard.darfFahrradrouteBearbeiten(benutzer, fahrradroute);
		boolean darfAttributeBearbeiten =
			anyKanteInZustaendigkeitsbereich && fahrradroute.getFahrradrouteTyp().equals(FahrradrouteTyp.RADVIS_ROUTE);
		return new FahrradrouteDetailView(fahrradroute, darfAttributeBearbeiten, anyKanteInZustaendigkeitsbereich);
	}
}
