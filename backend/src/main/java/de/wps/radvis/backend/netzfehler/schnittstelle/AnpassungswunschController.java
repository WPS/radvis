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

package de.wps.radvis.backend.netzfehler.schnittstelle;

import static de.wps.radvis.backend.benutzer.domain.valueObject.Recht.ANPASSUNGSWUENSCHE_BEARBEITEN;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.schnittstelle.RadvisViewController;
import de.wps.radvis.backend.kommentar.domain.entity.Kommentar;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.kommentar.schnittstelle.AddKommentarCommand;
import de.wps.radvis.backend.kommentar.schnittstelle.view.KommentarView;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschRepository;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschService;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.schnittstelle.view.AnpassungswunschListenView;
import de.wps.radvis.backend.netzfehler.schnittstelle.view.AnpassungswunschView;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/anpassungswunsch")
@Validated
@RadvisViewController
public class AnpassungswunschController {

	private final AnpassungswunschService anpassungswunschService;
	private final AnpassungswunschGuard anpassungswunschGuard;
	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final AnpassungswunschRepository anpassungswunschRepository;
	private final SaveAnpassungswunschCommandConverter saveAnpassungswunschCommandConverter;

	public AnpassungswunschController(AnpassungswunschService anpassungswunschService,
		AnpassungswunschGuard anpassungswunschGuard, BenutzerResolver benutzerResolver,
		VerwaltungseinheitResolver verwaltungseinheitResolver, AnpassungswunschRepository anpassungswunschRepository,
		SaveAnpassungswunschCommandConverter saveAnpassungswunschCommandConverter) {
		this.anpassungswunschService = anpassungswunschService;
		this.anpassungswunschGuard = anpassungswunschGuard;
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.anpassungswunschRepository = anpassungswunschRepository;
		this.saveAnpassungswunschCommandConverter = saveAnpassungswunschCommandConverter;
	}

	@GetMapping("/list")
	public List<AnpassungswunschListenView> getAlleAnpassungswuensche(
		@RequestParam Optional<Boolean> abgeschlosseneAusblenden,
		@RequestParam Optional<List<Long>> nebenFahrradrouten) {
		return anpassungswunschService
			.getAlleAnpassungswuensche(abgeschlosseneAusblenden.orElse(true),
				nebenFahrradrouten.orElse(Collections.emptyList()))
			.map(AnpassungswunschListenView::new)
			.collect(Collectors.toList());
	}

	@GetMapping("/{id}")
	public AnpassungswunschView getAnpassungswunsch(Authentication authentication, @PathVariable("id") Long id) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		boolean canEdit = benutzer.hatRecht(ANPASSUNGSWUENSCHE_BEARBEITEN);
		Anpassungswunsch anpassungswunsch = anpassungswunschService.getAnpassungswunsch(id);
		return new AnpassungswunschView(anpassungswunsch,
			anpassungswunschService.getUrsaechlicheKonsistenzregelVerletzung(
				anpassungswunsch.getKonsistenzregelVerletzungReferenz().orElse(null)),
			canEdit);
	}

	@PostMapping("/create")
	public AnpassungswunschView createAnpassungswunsch(Authentication authentication,
		@RequestBody @Valid SaveAnpassungswunschCommand command) throws AccessDeniedException {
		anpassungswunschGuard.createAnpassungswunsch(authentication, command);
		Anpassungswunsch created = anpassungswunschService.create(
			(Point) command.getGeometrie(),
			command.getBeschreibung(),
			command.getStatus(), command.getKategorie(), benutzerResolver.fromAuthentication(authentication),
			Optional.ofNullable(command.getVerantwortlicheOrganisation())
				.map(verwaltungseinheitResolver::resolve),
			Optional.ofNullable(command.getFehlerprotokollId()));

		anpassungswunschService.versendeInfoMailZuNeuemAnpassungswunsch(created);
		return new AnpassungswunschView(created, anpassungswunschService.getUrsaechlicheKonsistenzregelVerletzung(
			created.getKonsistenzregelVerletzungReferenz().orElse(null)), true);
	}

	@DeleteMapping("/{id}")
	public boolean deleteAnpassungswunsch(Authentication authentication, @PathVariable("id") Long id)
		throws AccessDeniedException {
		anpassungswunschGuard.deleteAnpassungswunsch(authentication, id);
		return anpassungswunschService.delete(id);
	}

	@PostMapping("/{id}/update")
	public AnpassungswunschView updateAnpassungswunsch(Authentication authentication, @PathVariable("id") Long id,
		@RequestBody @Valid SaveAnpassungswunschCommand command) throws AccessDeniedException {
		anpassungswunschGuard.updateAnpassungswunsch(authentication, id, command);
		Anpassungswunsch anpassungswunsch = anpassungswunschService.getAnpassungswunsch(id);
		// Wir versenden nur dann eine Mail, wenn sich die Kategorie geändert hat
		boolean infoMailVersenden = !anpassungswunsch.getKategorie().equals(command.getKategorie());

		saveAnpassungswunschCommandConverter.apply(anpassungswunsch, command,
			benutzerResolver.fromAuthentication(authentication));

		Anpassungswunsch saved = anpassungswunschRepository.save(anpassungswunsch);
		KonsistenzregelVerletzung ursaechlicheKonsistenzregelVerletzung = anpassungswunschService
			.getUrsaechlicheKonsistenzregelVerletzung(
				anpassungswunsch.getKonsistenzregelVerletzungReferenz().orElse(null));

		if (infoMailVersenden) {
			anpassungswunschService.versendeInfoMailZuNeuemAnpassungswunsch(saved);
		}

		return new AnpassungswunschView(
			saved,
			ursaechlicheKonsistenzregelVerletzung,
			true);
	}

	@GetMapping("{id}/kommentarliste")
	public List<KommentarView> getKommentarListe(Authentication authentication, @PathVariable("id") Long id) {
		KommentarListe kommentarListe = anpassungswunschService.getAnpassungswunsch(id).getKommentarListe();
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return KommentarView.convertAll(kommentarListe, benutzer);
	}

	@PostMapping("{id}/kommentar")
	@Transactional
	public List<KommentarView> addKommentar(
		@PathVariable("id") Long id,
		Authentication authentication,
		@RequestBody @Valid AddKommentarCommand command) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		Anpassungswunsch anpassungswunsch = anpassungswunschService.getAnpassungswunsch(id);
		anpassungswunsch
			.addKommentar(new Kommentar(command.getKommentarText(), benutzer));

		return KommentarView.convertAll(anpassungswunsch.getKommentarListe(), benutzer);
	}
}
