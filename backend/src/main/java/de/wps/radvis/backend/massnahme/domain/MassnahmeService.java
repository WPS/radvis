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

package de.wps.radvis.backend.massnahme.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;

import org.springframework.context.event.EventListener;

import de.wps.radvis.backend.common.domain.service.AbstractVersionierteEntityService;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.massnahme.domain.dbView.MassnahmeListenDbView;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeUmsetzungsstandViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.UmsetzungsstandRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.event.KanteDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KanteTopologieChangedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
public class MassnahmeService extends AbstractVersionierteEntityService<Massnahme> {

	private final MassnahmeRepository massnahmeRepository;
	private final MassnahmeViewRepository massnahmeViewRepository;
	private final UmsetzungsstandRepository umsetzungsstandRepository;
	private final MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository;
	MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;
	private final KantenRepository kantenRepository;

	public MassnahmeService(MassnahmeRepository massnahmeRepository,
		MassnahmeViewRepository massnahmeViewRepository,
		MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository,
		UmsetzungsstandRepository umsetzungsstandRepository,
		KantenRepository kantenRepository,
		MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService) {
		super(massnahmeRepository);
		this.massnahmeRepository = massnahmeRepository;
		this.massnahmeViewRepository = massnahmeViewRepository;
		this.massnahmeUmsetzungsstandViewRepository = massnahmeUmsetzungsstandViewRepository;
		this.massnahmeNetzbezugAenderungProtokollierungsService = massnahmeNetzbezugAenderungProtokollierungsService;
		this.umsetzungsstandRepository = umsetzungsstandRepository;
		this.kantenRepository = kantenRepository;
	}

	@Override
	public Massnahme get(Long massnahmeId) {
		return massnahmeRepository.findByIdAndGeloeschtFalse(massnahmeId).orElseThrow(EntityNotFoundException::new);
	}

	// Es kann pro PaketId bis zu zwei Maßnahmen geben: Eine für Start- und eine für Zielstandard
	public List<Massnahme> getMassnahmeByPaketId(String paketId) {
		return massnahmeRepository.findByMassnahmenPaketId(MassnahmenPaketId.of(paketId));
	}

	public Umsetzungsstand getUmsetzungsstand(Long umsetzungsstandId) {
		return umsetzungsstandRepository.findById(umsetzungsstandId).orElseThrow(EntityNotFoundException::new);
	}

	public Umsetzungsstand loadUmsetzungsstandForModification(Long id, Long version) {
		require(id, notNullValue());
		require(version, notNullValue());

		Umsetzungsstand umsetzungsstand = getUmsetzungsstand(id);

		if (!version.equals(umsetzungsstand.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return umsetzungsstand;
	}

	public Massnahme saveMassnahme(Massnahme massnahme) {
		return massnahmeRepository.save(massnahme);
	}

	public void saveUmsetzungsstand(Umsetzungsstand umsetzungsstand) {
		umsetzungsstandRepository.save(umsetzungsstand);
	}

	public Massnahme getMassnahmeByUmsetzungsstand(Umsetzungsstand umsetzungsstand) {
		return massnahmeRepository.findByUmsetzungsstandAndGeloeschtFalse(umsetzungsstand)
			.orElseThrow(EntityNotFoundException::new);
	}

	public Optional<MassnahmeNetzBezug> getNetzbezugByUmsetzungsstandId(Long umsetzungsstandId) {
		Optional<Umsetzungsstand> umsetzungsstandById = umsetzungsstandRepository.findById(umsetzungsstandId);
		if (umsetzungsstandById.isEmpty()) {
			return Optional.empty();
		}
		return massnahmeRepository.findByUmsetzungsstandAndGeloeschtFalse(umsetzungsstandById.get())
			.map(Massnahme::getNetzbezug);
	}

	public Set<MassnahmenPaketId> findAllMassnahmenPaketIds() {
		return massnahmeRepository.findAllMassnahmenPaketIds();
	}

	public List<MassnahmeListenDbView> getAlleMassnahmenListenViews() {
		return massnahmeViewRepository.findAll();
	}

	public List<MassnahmeListenDbView> getAlleMassnahmenListenViewsInBereich(Verwaltungseinheit organisation) {
		return organisation.getBereich()
			.map(massnahmeViewRepository::findAllInBereich)
			.orElse(List.of());
	}

	public void haengeDateiAn(Long id, Dokument dokument) {
		Massnahme massnahme = get(id);
		haengeDateiAn(massnahme, dokument);
	}

	public void haengeDateiAn(Massnahme massnahme, Dokument dokument) {
		massnahme.addDokument(dokument);
		massnahmeRepository.save(massnahme);
	}

	public Dokument getDokument(Long massnahmeId, Long dokumentId) {
		return get(massnahmeId)
			.getDokumentListe()
			.getDokumente()
			.stream().filter(d -> dokumentId.equals(d.getId()))
			.findFirst()
			.orElseThrow(EntityNotFoundException::new);
	}

	public void deleteDokument(Long massnahmeId, Long dokumentId) {
		Massnahme massnahme = get(massnahmeId);
		massnahme.deleteDokument(dokumentId);
		massnahmeRepository.save(massnahme);
	}

	public String[] getUmsetzungsstandAuswertungKopfzeile() {
		return new String[] {
			"Maßnahmennummer",
			"Bezeichnung",
			"Stadt-/Landkreis",
			"Gemeinde",
			"Baulastträger laut Maßnahmenblatt",
			"Umsetzungsstatus",
			"Länge der Kantensegmente in Meter",
			"Zugehörigkeit RadNETZ Alltag",
			"Zugehörigkeit RadNETZ Freizeit",
			"Zugehörigkeit RadNETZ Zielnetz",
			"1. Ist die Umsetzung erfolgt",
			"2. Umsetzung gemäß RadNETZ-Maßnahmenblatt",
			"3. Grund für Abweichung zum RadNETZ-Maßnahmenblatt",
			"4. Prüfung auf Einhaltung der RadNETZ-Qualitätsstandards",
			"5. Beschreibung der abweichenden RadNETZ-Maßnahme",
			"6. Kosten der RadNETZ-Maßnahme",
			"7. Anmerkung zu RadNETZ-Maßnahmen",
			"Vor-, Nachname und E-Mail-Adresse des Nutzers, der die Umfrage zuletzt bestätigt hat",
			"Zeitpunkt der letzten Bestätigung der Umfrage"
		};
	}

	public List<String[]> getUmsetzungsstandAuswertungCSV(List<Long> ids) {
		return massnahmeUmsetzungsstandViewRepository.findAllById(ids).map(massnahme -> {
			return new String[] {
				massnahme.getMassnahmeKonzeptId(),
				massnahme.getBezeichnung(),
				massnahme.getKreis(),
				massnahme.getGemeinde(),
				massnahme.getBaulastOrganisationsArt(),
				massnahme.getUmsetzungsstatus().toString(),
				massnahme.getLaenge().toString(),
				massnahme.getNetzklassen().contains(Netzklasse.RADNETZ_ALLTAG) ? "Ja" : "Nein",
				massnahme.getNetzklassen().contains(Netzklasse.RADNETZ_FREIZEIT) ? "Ja" : "Nein",
				massnahme.getNetzklassen().contains(Netzklasse.RADNETZ_ZIELNETZ) ? "Ja" : "Nein",
				massnahme.getIstUmgesetzt(),
				massnahme.getUmsetzungGemaessMassnahmenblatt(),
				massnahme.getGrundFuerAbweichung(),
				massnahme.getPruefungQualitaetsstandardsErfolgt(),
				massnahme.getBeschreibungAbweichenderMassnahme(),
				massnahme.getKostenDerMassnahme(),
				massnahme.getAnmerkung(),
				massnahme.getBenutzerKontaktdaten(),
				massnahme.getLetzteAenderung()
			};
		}).collect(Collectors.toList());
	}

	public List<Massnahme> findByMassnahmePaketId(MassnahmenPaketId massnahmePaketId) {
		List<Massnahme> massnahmen = massnahmeRepository.findByMassnahmenPaketId(massnahmePaketId);
		if (massnahmen.size() > 1) {
			log.warn("Es wurden mehr als eine ({}) Massnahmen für die Paket-ID {} gefunden!",
				massnahmen.size(),
				massnahmePaketId);
		}
		return massnahmen;

	}

	public List<Massnahme> findByKanteIdInNetzBezug(Long kanteId) {
		return massnahmeRepository.findByKanteInNetzBezug(kanteId);
	}

	public List<Massnahme> findByKnotenIdInNetzBezug(long knotenId) {
		return massnahmeRepository.findByKnotenInNetzBezug(knotenId);
	}

	public boolean hatRadNETZNetzBezug(Massnahme massnahme) {
		return massnahme.getNetzbezug()
			.getImmutableKantenAbschnittBezug().stream()
			.anyMatch(
				abschnittsweiserKantenSeitenBezug -> abschnittsweiserKantenSeitenBezug.getKante().isRadNETZ())
			||
			massnahme.getNetzbezug()
				.getImmutableKantenPunktBezug().stream()
				.anyMatch(punktuellerKantenSeitenBezug -> punktuellerKantenSeitenBezug.getKante().isRadNETZ())
			||
			massnahme.getNetzbezug()
				.getImmutableKnotenBezug().stream()
				.anyMatch(
					knoten -> kantenRepository.getAdjazenteKanten(knoten).stream().anyMatch(Kante::isRadNETZ));
	}

	@EventListener
	public void onKanteGeloescht(KanteDeletedEvent event) {
		List<Massnahme> massnahmen = findByKanteIdInNetzBezug(event.getKanteId());
		massnahmeNetzbezugAenderungProtokollierungsService.protokolliereNetzBezugAenderungFuerGeloeschteKante(
			massnahmen.stream().filter(m -> !m.isGeloescht()).collect(Collectors.toList()), event);
		for (Massnahme massnahme : massnahmen) {
			massnahme.removeKanteFromNetzbezug(event.getKanteId());
			massnahmeRepository.save(massnahme);
		}
	}

	@EventListener
	public void onKanteTopologieChanged(KanteTopologieChangedEvent event) {
		List<Massnahme> massnahmen = findByKanteIdInNetzBezug(event.getKanteId()).stream()
			.filter(m -> !m.isGeloescht())
			.collect(Collectors.toList());
		massnahmeNetzbezugAenderungProtokollierungsService.protokolliereNetzBezugAenderungFuerVeraenderteKante(
			massnahmen, event);
	}

	@EventListener
	public void onKnotenGeloescht(KnotenDeletedEvent event) {
		List<Massnahme> massnahmen = findByKnotenIdInNetzBezug(event.getKnotenId());
		massnahmeNetzbezugAenderungProtokollierungsService.protokolliereNetzBezugAenderungFuerGeloeschteKnoten(
			massnahmen.stream().filter(m -> !m.isGeloescht()).collect(Collectors.toList()), event);
		for (Massnahme massnahme : massnahmen) {
			massnahme.removeKnotenFromNetzbezug(event.getKnotenId());
			massnahmeRepository.save(massnahme);
		}
	}
}
