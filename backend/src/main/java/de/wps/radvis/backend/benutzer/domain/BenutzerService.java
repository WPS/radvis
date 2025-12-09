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

import static org.valid4j.Assertive.require;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerDBListView;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerExistiertBereitsException;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerIstNichtRegistriertException;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.domain.FrontendLinks;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.Mailadresse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
public class BenutzerService {

	public static List<Recht> benutzerverwaltungsRechte = List.of(
		Recht.ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN,
		Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN);

	private final BenutzerRepository benutzerRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final ServiceBwId technischerBenutzerServiceBwId;
	private final String basisUrl;
	private final MailService mailService;

	public BenutzerService(
		BenutzerRepository benutzerRepository,
		VerwaltungseinheitService verwaltungseinheitService,
		String technischerBenutzerServiceBwId,
		String basisUrl,
		MailService mailService) {
		this.benutzerRepository = benutzerRepository;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.technischerBenutzerServiceBwId = ServiceBwId.of(technischerBenutzerServiceBwId);
		this.basisUrl = basisUrl;
		this.mailService = mailService;
	}

	public Benutzer registriereBenutzer(Name vorname, Name nachname, Long organisationsID, Set<Rolle> rollen,
		ServiceBwId serviceBwId, Mailadresse mailadresse)
		throws BenutzerExistiertBereitsException {
		if (benutzerRepository.existsByServiceBwId(serviceBwId)) {
			throw new BenutzerExistiertBereitsException(
				String.format("Benutzer mit Service BW Id '%s' existiert bereits.", serviceBwId.getValue()));
		}

		Optional<Verwaltungseinheit> organisation = verwaltungseinheitService.findById(organisationsID);

		if (organisation.isEmpty()) {
			throw new EntityNotFoundException(
				String.format("Eine Organisation mit der ID '%d' existiert nicht.", organisationsID));
		}

		return benutzerRepository.save(new Benutzer(
			vorname,
			nachname,
			BenutzerStatus.WARTE_AUF_FREISCHALTUNG,
			organisation.get(),
			mailadresse,
			serviceBwId,
			rollen
		));
	}

	public void registriereAdministrator(Name vorname, Name nachname, ServiceBwId serviceBwId,
		Mailadresse mailadresse) throws BenutzerExistiertBereitsException {
		if (benutzerRepository.existsByServiceBwId(serviceBwId)) {
			throw new BenutzerExistiertBereitsException(
				String.format("Benutzer mit Service BW Id '%s' existiert bereits.", serviceBwId.getValue()));
		}

		benutzerRepository.save(new Benutzer(
			vorname,
			nachname,
			BenutzerStatus.AKTIV,
			verwaltungseinheitService.getBundesland(),
			mailadresse,
			serviceBwId,
			Set.of(Rolle.RADVIS_ADMINISTRATOR)));
	}

	public void registriereTechnischenBenutzer(Name vorname, Name nachname, ServiceBwId serviceBwId,
		Mailadresse mailadresse) throws BenutzerExistiertBereitsException {
		if (benutzerRepository.existsByServiceBwId(serviceBwId)) {
			throw new BenutzerExistiertBereitsException(
				String.format("Benutzer mit Service BW Id '%s' existiert bereits.", serviceBwId.getValue()));
		}

		benutzerRepository.save(new Benutzer(
			vorname,
			nachname,
			BenutzerStatus.WARTE_AUF_FREISCHALTUNG,
			verwaltungseinheitService.getUnbekannteOrganisation(),
			mailadresse,
			serviceBwId,
			Set.of(Rolle.RADVIS_BETRACHTER)));
	}

	public boolean istBenutzerRegistriert(ServiceBwId serviceBwId) {
		return benutzerRepository.existsByServiceBwId(serviceBwId);
	}

	public Optional<Benutzer> findBenutzerByServiceBwIdAndInitialize(ServiceBwId serviceBWID) {
		Optional<Benutzer> potentialBenutzer = benutzerRepository.findByServiceBwId(serviceBWID);
		potentialBenutzer.ifPresent((benutzer) -> {
			if (!benutzer.getOrganisation().getOrganisationsArt().istGebietskoerperschaft()) {
				Hibernate.initialize(benutzer.getOrganisation());
				Hibernate.initialize(((Organisation) benutzer.getOrganisation()).getZustaendigFuerBereichOf());
			}
		});
		return potentialBenutzer;
	}

	public Benutzer getBenutzer(long id) throws BenutzerIstNichtRegistriertException {
		return benutzerRepository.findById(id).orElseThrow(BenutzerIstNichtRegistriertException::new);
	}

	public Benutzer save(Benutzer benutzer) {
		return benutzerRepository.save(benutzer);
	}

	public Benutzer aendereBenutzerstatus(long id, long version, BenutzerStatus status)
		throws BenutzerIstNichtRegistriertException {
		Benutzer benutzer = benutzerRepository.findById(id).orElseThrow(BenutzerIstNichtRegistriertException::new);
		if (version < benutzer.getVersion()) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}
		benutzer.setStatus(status);

		if (status != BenutzerStatus.AKTIV) {
			benutzer.setAblaufdatum(null);
		}

		return benutzerRepository.save(benutzer);
	}

	public Benutzer getBenutzerForModifikation(Long id, Long version) {
		Benutzer benutzer = benutzerRepository.findById(id).orElseThrow();
		if (version < benutzer.getVersion()) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}
		return benutzer;
	}

	public List<Benutzer> getRadvisAdmins() {
		return benutzerRepository.findByRollenAndStatus(Rolle.RADVIS_ADMINISTRATOR, BenutzerStatus.AKTIV);
	}

	public Benutzer getTechnischerBenutzer() {
		return this.benutzerRepository.findByServiceBwId(this.technischerBenutzerServiceBwId).orElseThrow();
	}

	public List<Benutzer> findByOrganisationAndStatus(Verwaltungseinheit organisation, BenutzerStatus aktiv) {
		return benutzerRepository.findByOrganisationAndStatus(organisation, aktiv);
	}

	public List<BenutzerDBListView> getAlleBenutzerByZustaendigerBenutzer(Benutzer aktiverBenutzer) {

		if (aktiverBenutzer.getRollen().contains(Rolle.RADVIS_ADMINISTRATOR)) {
			return benutzerRepository.findAllDBListViews();
		} else if (benutzerHatEinesDerRechte(aktiverBenutzer, benutzerverwaltungsRechte)) {
			Verwaltungseinheit verwaltungseinheit = aktiverBenutzer.getOrganisation();
			return verwaltungseinheitService.findAllUntergeordnetIds(verwaltungseinheit.getId())
				.stream()
				.flatMap(verwaltungseinheitId -> benutzerRepository.findAllDBListViewsInVerwaltungseinheitWithId(
					verwaltungseinheitId).stream())
				.filter(b -> !b.getRollen().contains(Rolle.RADVIS_ADMINISTRATOR))
				.collect(Collectors.toSet())
				.stream().toList();
		} else {
			return Collections.emptyList();
		}
	}

	public boolean pruefeBearbeiterIstAutorisiertFuerBenutzer(Benutzer bearbeiter, Benutzer zuBearbeitenderBenutzer)
		throws BenutzerIstNichtRegistriertException {
		List<Benutzer> zustaendigeBenutzer = getAlleAutorisiertenBenutzer(zuBearbeitenderBenutzer);
		return zustaendigeBenutzer.contains(bearbeiter);
	}

	public boolean darfBenutzerOrganisationErstellenOderBearbeiten(Benutzer aktiverBenutzer,
		Verwaltungseinheit organisationToEditOrCreate) {

		if (aktiverBenutzer.hatRecht(Recht.ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN)) {
			return true;
		} else if (aktiverBenutzer.hatRecht(Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN)) {
			return verwaltungseinheitService.istUebergeordnet(aktiverBenutzer.getOrganisation(),
				organisationToEditOrCreate);
		}
		return false;
	}

	private List<Benutzer> getAlleAutorisiertenBenutzer(Benutzer zuBearbeitenderBenutzer) {
		if (zuBearbeitenderBenutzer.getRollen().contains(Rolle.RADVIS_ADMINISTRATOR)) {
			return getRadvisAdmins();
		}
		return findAutorisierteFuerNormalenBenutzer(zuBearbeitenderBenutzer);
	}

	private List<Benutzer> findAutorisierteFuerNormalenBenutzer(Benutzer zuBearbeitenderBenutzer) {
		List<Benutzer> autorisierteBenutzer = new ArrayList<>();
		Verwaltungseinheit aktuelleOrganisationsebene = zuBearbeitenderBenutzer.getOrganisation();
		while (aktuelleOrganisationsebene != null) {
			autorisierteBenutzer.addAll(findAdminaufSelberEbene(aktuelleOrganisationsebene));
			aktuelleOrganisationsebene = aktuelleOrganisationsebene.getUebergeordneteVerwaltungseinheit()
				.orElse(null);
		}
		autorisierteBenutzer.addAll(getRadvisAdmins());
		return autorisierteBenutzer;
	}

	public List<Benutzer> findAdminaufSelberEbene(Verwaltungseinheit organisation) {
		List<Benutzer> potentiellZustaendigeBenutzer = findByOrganisationAndStatus(organisation, BenutzerStatus.AKTIV);
		return potentiellZustaendigeBenutzer.stream()
			.filter(b -> benutzerHatEinesDerRechte(b, benutzerverwaltungsRechte))
			.collect(Collectors.toList());
	}

	boolean benutzerHatEinesDerRechte(Benutzer benutzer, List<Recht> rechte) {
		return rechte.stream().anyMatch(benutzer::hatRecht);
	}

	/**
	 * Gibt alle Rechte zurück mit denen die entsprechende Rolle verbeben werden
	 * darf
	 *
	 * @param rolle
	 *     für die die Vergaberechte ermittelt werden sollen
	 * @return Alle Rechte, mit der man die Rolle vergeben darf
	 */
	public List<Recht> ermittleVergaberechteFuerRolle(Rolle rolle) {
		if (rolle == Rolle.KREISKOORDINATOREN
			|| rolle == Rolle.RADWEGE_ERFASSERIN || rolle == Rolle.RADROUTEN_BEARBEITERIN) {
			return List.of(
				Recht.ALLE_ROLLEN,
				Recht.KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER);
		}

		if (rolle == Rolle.RADVERKEHRSBEAUFTRAGTER) {
			return List.of(Recht.ALLE_ROLLEN, Recht.RADVERKEHRSBEAUFTRAGTER);
		}

		if (rolle == Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN) {
			return List.of(Recht.ALLE_ROLLEN, Recht.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN);
		}

		if (rolle == Rolle.RADVIS_BETRACHTER
			|| rolle == Rolle.EXTERNER_DIENSTLEISTER) {
			return List.of(Recht.ALLE_ROLLEN, Recht.BETRACHTER_EXTERNER_DIENSTLEISTER);
		}

		// POWER_USER und RADVIS_ADMINISTRATOR UND RADNETZ_QUALITAETSSICHERIN
		return List.of(Recht.ALLE_ROLLEN);
	}

	/**
	 * Gibt alle Benutzer zurück, welche aktuell für den gegebenen Benutzer
	 * zuständig sind (und z.b. eine Email bei
	 * einer neuen Registriereung erhalten sollen). Dies ist eine echte Teilmenge
	 * der Autorisierten Benutzer. Dabei
	 * werden nur die Admins der nächst niedrigensten Ebene gefunden, die
	 * autorisiert sind.
	 */
	public List<Benutzer> getAlleZustaendigenBenutzer(Benutzer benutzer) {
		if (benutzer.getRollen().contains(Rolle.RADVIS_ADMINISTRATOR)) {
			return getRadvisAdmins();
		}

		List<Benutzer> zustaendigenBenutzer = new ArrayList<>();
		Verwaltungseinheit aktuelleOrganisationsebene = benutzer.getOrganisation();

		while (zustaendigenBenutzer.isEmpty() && aktuelleOrganisationsebene != null) {
			zustaendigenBenutzer = findAdminaufSelberEbene(aktuelleOrganisationsebene);
			aktuelleOrganisationsebene = aktuelleOrganisationsebene.getUebergeordneteVerwaltungseinheit()
				.orElse(null);
		}

		// Admins herausfiltern, die nicht alle Rollen vergeben dürfen
		zustaendigenBenutzer = zustaendigenBenutzer.stream()
			.filter(admin -> benutzer.getRollen().stream().noneMatch(
				rolle -> ermittleVergaberechteFuerRolle(rolle).stream().noneMatch(admin::hatRecht)))
			.collect(Collectors.toList());

		// sollten sich keine zuständigen Admins finden lassen, wird an die
		// Zentraladmins eskaliert
		return zustaendigenBenutzer.isEmpty()
			? getRadvisAdmins()
			: zustaendigenBenutzer;
	}

	/**
	 * Ermittelt die freigeschalteten Benutzer (ohne Admins), die sich länger als spezifiziert nicht eingeloggt haben
	 *
	 * @param dauerInTagen
	 *     Anzahl an Tagen (exklusive), bis Benutzer als inaktiv angesehen werden
	 * @return Alle Rechte, mit der man die Rolle vergeben darf
	 */
	public List<Benutzer> ermittleAktiveBenutzerInaktivLaengerAls(Integer dauerInTagen) {
		LocalDate grenzDatum = LocalDate.now().minusDays(dauerInTagen);

		return benutzerRepository.findByStatusAndRollenIsNotContainingAndLetzteAktivitaetBefore(
			BenutzerStatus.AKTIV,
			Rolle.RADVIS_ADMINISTRATOR,
			grenzDatum
		);
	}

	public List<Benutzer> ermittleBenutzerAblaufdatumUeberschritten() {
		return benutzerRepository.findByStatusAndAblaufdatumBefore(BenutzerStatus.AKTIV, LocalDate.now());
	}

	public Benutzer beantrageReaktivierungFuerBenutzer(Benutzer benutzer) throws BenutzerIstNichtRegistriertException {
		require(benutzer.getStatus().equals(BenutzerStatus.INAKTIV),
			"Nur inaktive Benutzer können reaktiviert werden!");
		Benutzer beantragenderBenutzer = aendereBenutzerstatus(benutzer.getId(), benutzer.getVersion(),
			BenutzerStatus.WARTE_AUF_FREISCHALTUNG);

		String mailText = String.format(
			"Der Benutzer '%s %s' hat eine Reaktivierung beantragt und wartet auf Freischaltung.\n" +
				"Die Bearbeitung des Benutzers kann erfolgen unter %s .",
			beantragenderBenutzer.getVorname(),
			beantragenderBenutzer.getNachname(),
			this.basisUrl + FrontendLinks.benutzerAdministration(benutzer.getId()));

		this.versendeMailFuerBenutzerAenderungAnZustaendige(
			beantragenderBenutzer,
			"Antrag auf Reaktivierung",
			mailText);

		log.info("Benutzer mit ServiceBW-ID [{}] hat eine Reaktivierung beantragt.",
			beantragenderBenutzer.getServiceBwId());
		return beantragenderBenutzer;
	}

	private void versendeMailFuerBenutzerAenderungAnZustaendige(Benutzer benutzer, String betreff, String mailText) {
		List<Benutzer> zustaendigeBenutzer = this.getAlleZustaendigenBenutzer(benutzer);
		List<String> zustaendigeMailadressen = zustaendigeBenutzer.stream()
			.map(b -> b.getMailadresse().toString())
			.toList();
		mailService.sendMail(zustaendigeMailadressen, betreff, mailText);
	}

	public long getAnzahlBenutzerGesamt() {
		return benutzerRepository.count();
	}

	public long getAnzahlBenutzerAktiv() {
		return benutzerRepository.countByStatus(BenutzerStatus.AKTIV);
	}

}
