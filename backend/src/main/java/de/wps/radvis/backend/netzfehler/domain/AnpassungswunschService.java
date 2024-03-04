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

package de.wps.radvis.backend.netzfehler.domain;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.locationtech.jts.geom.Point;
import org.springframework.context.event.EventListener;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.konsistenz.pruefung.domain.KonsistenzregelVerletzungsRepository;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.konsistenz.pruefung.domain.event.KonsistenzregelVerletzungenDeletedEvent;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import de.wps.radvis.backend.netzfehler.domain.valueObject.KonsistenzregelVerletzungReferenz;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnpassungswunschService {

	private AnpassungswunschRepository anpassungswunschRepository;
	private KonsistenzregelVerletzungsRepository konsistenzregelVerletzungsRepository;

	public AnpassungswunschService(AnpassungswunschRepository anpassungswunschRepository,
		KonsistenzregelVerletzungsRepository konsistenzregelVerletzungsRepository) {
		this.anpassungswunschRepository = anpassungswunschRepository;
		this.konsistenzregelVerletzungsRepository = konsistenzregelVerletzungsRepository;
	}

	public Anpassungswunsch create(Point geometrie, String beschreibung, AnpassungswunschStatus status,
		AnpassungswunschKategorie kategorie, Benutzer benutzer,
		Optional<Verwaltungseinheit> verantwortlicheOrganisation,
		Optional<String> fehlerprotokollId
	) {
		return anpassungswunschRepository.save(
			new Anpassungswunsch(geometrie, beschreibung, status, kategorie, benutzer, verantwortlicheOrganisation,
				buildKonsistenzregelVerletzungReferenz(fehlerprotokollId)));
	}

	private Optional<KonsistenzregelVerletzungReferenz> buildKonsistenzregelVerletzungReferenz(
		Optional<String> fehlerprotokollId) {
		if (fehlerprotokollId.isEmpty()) {
			return Optional.empty();
		}

		String[] klasseUndId = fehlerprotokollId.get().split("/");
		String fehlerprotokollKlasse = klasseUndId[0];
		Long konsistenzregelVerletzungsId = Long.valueOf(klasseUndId[1]);
		if (!fehlerprotokollKlasse.equals(KonsistenzregelVerletzung.class.getSimpleName())) {
			return Optional.empty();
		}

		return konsistenzregelVerletzungsRepository.findById(konsistenzregelVerletzungsId)
			.map(verletzung -> KonsistenzregelVerletzungReferenz.of(verletzung.getIdentity(), verletzung.getTyp()));
	}

	public boolean delete(Long id) {
		if (!anpassungswunschRepository.existsById(id)) {
			return false;
		}

		anpassungswunschRepository.deleteById(id);
		return true;
	}

	public Anpassungswunsch getAnpassungswunsch(Long id) {
		return anpassungswunschRepository.findById(id).orElseThrow(EntityNotFoundException::new);
	}

	public KonsistenzregelVerletzung getUrsaechlicheKonsistenzregelVerletzung(
		KonsistenzregelVerletzungReferenz konsistenzregelVerletzungReferenz) {
		if (konsistenzregelVerletzungReferenz == null) {
			return null;
		}
		return konsistenzregelVerletzungsRepository.findByTypAndIdentity(
			konsistenzregelVerletzungReferenz.getTyp(), konsistenzregelVerletzungReferenz.getIdentity()).orElse(null);
	}

	public Stream<Anpassungswunsch> getAlleAnpassungswuensche(boolean abgeschlosseneAusblenden) {
		Iterable<Anpassungswunsch> result = abgeschlosseneAusblenden ?
			anpassungswunschRepository.findAllByStatusIsNotIn(AnpassungswunschStatus.ALLE_ABGESCHLOSSENEN) :
			anpassungswunschRepository.findAll();

		return StreamSupport.stream(result.spliterator(), false);
	}

	public Iterable<Anpassungswunsch> findAllOpenDlm() {
		return anpassungswunschRepository.findAllByStatusAndKategorie(AnpassungswunschStatus.OFFEN,
			AnpassungswunschKategorie.DLM);
	}

	@EventListener
	public void onKonsistenzregelVerletzungenGeloescht(KonsistenzregelVerletzungenDeletedEvent event) {
		List<KonsistenzregelVerletzungReferenz> geloeschteVerletzungenReferenzen = event.getGeloeschteVerletzungenIdentities()
			.stream()
			.map(identity -> KonsistenzregelVerletzungReferenz.of(identity, event.getTyp()))
			.collect(Collectors.toList());
		List<Anpassungswunsch> zuAenderndeAnpassungswuensche = anpassungswunschRepository.findByKonsistenzregelVerletzungReferenzIn(
				geloeschteVerletzungenReferenzen)
			.filter(aw -> !aw.getStatus().istAbgeschlossen())
			.collect(Collectors.toList());

		zuAenderndeAnpassungswuensche.forEach(Anpassungswunsch::setzeStatusAufUmgesetzt);

		log.info(
			"Es wurden {} Anpassungswünsche auf 'UMGESETZT' gesetzt, weil die zugrunde liegenden Konsistenzregelverletzungen vom Typ {} nicht mehr existieren",
			zuAenderndeAnpassungswuensche.size(), event.getTyp());
	}
}
