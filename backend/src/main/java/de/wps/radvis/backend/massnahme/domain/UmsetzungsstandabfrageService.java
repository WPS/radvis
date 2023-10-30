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

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.transaction.Transactional;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.google.common.collect.Lists;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
public class UmsetzungsstandabfrageService {
	private final MassnahmeRepository massnahmeRepository;
	private final MassnahmenZustaendigkeitsService massnahmenZustaendigkeitsService;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final MailService mailService;
	private final MailConfigurationProperties mailConfigurationProperties;
	private final CommonConfigurationProperties commonConfigurationProperties;
	private final UmsetzungsstandsabfrageConfigurationProperties umsetzungsstandsabfrageConfigurationProperties;
	private final TemplateEngine templateEngine;
	private final PostgisConfigurationProperties postgisConfigurationProperties;

	public UmsetzungsstandabfrageService(
		MassnahmeRepository massnahmeRepository,
		MassnahmenZustaendigkeitsService massnahmenZustaendigkeitsService,
		VerwaltungseinheitService verwaltungseinheitService, MailService mailService,
		MailConfigurationProperties mailConfigurationProperties,
		CommonConfigurationProperties commonConfigurationProperties,
		UmsetzungsstandsabfrageConfigurationProperties umsetzungsstandsabfrageConfigurationProperties,
		PostgisConfigurationProperties postgisConfigurationProperties,
		TemplateEngine templateEngine) {
		require(postgisConfigurationProperties, notNullValue());
		this.massnahmeRepository = massnahmeRepository;
		this.massnahmenZustaendigkeitsService = massnahmenZustaendigkeitsService;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.mailService = mailService;
		this.mailConfigurationProperties = mailConfigurationProperties;
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.umsetzungsstandsabfrageConfigurationProperties = umsetzungsstandsabfrageConfigurationProperties;
		this.postgisConfigurationProperties = postgisConfigurationProperties;
		this.templateEngine = templateEngine;
	}

	public List<Massnahme> starteUmsetzungsstandsabfrage(List<Long> massnahmeIds) {
		String allIds = massnahmeIds.stream().sorted().map(Object::toString).collect(Collectors.joining(", "));
		log.info("Umsetzungsstandsabfrage für folgende {} Maßnahmen angestoßen:\n{}", massnahmeIds.size(), allIds);

		return this.getMassnahmenStream(massnahmeIds)
			.filter(massnahme ->
				massnahme.getUmsetzungsstatus() != Umsetzungsstatus.STORNIERT
					&& massnahme.getUmsetzungsstatus() != Umsetzungsstatus.UMGESETZT
					&& massnahme.getUmsetzungsstand().isPresent())
			.peek(massnahme -> massnahme.getUmsetzungsstand().get().fordereAktualisierungAn())
			.collect(Collectors.toList());
	}

	public void benachrichtigeNutzer(List<Massnahme> massnahmen) {
		String beantwortungsFrist = bestimmeUndFormattiereBeantwortungsfrist(LocalDateTime.now(),
			umsetzungsstandsabfrageConfigurationProperties.getFrist());
		Map<Benutzer, Set<Benutzer>> kreiskoordinatorToMailempfaenger = new HashMap<>();
		Set<Verwaltungseinheit> verwaltungseinheitOhneZustaendigeBearbeiter = new HashSet<>();

		log.info("Versende Benachrichtigungen zur Beantwortung der Umsetzungsstandsabfragen basierend auf {} Maßnahmen", massnahmen.size());

		massnahmen
			.stream()
			.flatMap(massnahme -> {
				List<Benutzer> zustaendigeBearbeiter = massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
					massnahme);
				if (massnahme.getBaulastZustaendiger().isPresent() && zustaendigeBearbeiter.isEmpty()) {
					verwaltungseinheitOhneZustaendigeBearbeiter.add(massnahme.getBaulastZustaendiger().get());
				}
				return zustaendigeBearbeiter.stream();
			})
			.distinct()
			.forEach(benutzer -> {
				mailService.sendHtmlMail(
					List.of(benutzer.getMailadresse().toString()),
					"[RadVIS] Umsetzungsstandabfrage der RadNETZ Maßnahmen",
					generateUmsetzungsstandAbfrageEmail(benutzer.getOrganisation(), beantwortungsFrist));

				if (FeatureTogglz.UMSETZUNGSSTANDABFRAGE_KREISKOORDINATOREN_BENACHRICHTIGEN.isActive()) {
					getAlleKreiskoordinatorenInUndUeber(benutzer.getOrganisation())
						.forEach(kreiskoordinator -> {
							kreiskoordinatorToMailempfaenger.computeIfAbsent(kreiskoordinator, k -> new HashSet<>())
								.add(benutzer);
						});
				}
			});

		if (FeatureTogglz.UMSETZUNGSSTANDABFRAGE_KREISKOORDINATOREN_BENACHRICHTIGEN.isActive()) {
			benachrichtigeKreiskoordinatorinnen(beantwortungsFrist, kreiskoordinatorToMailempfaenger,
				verwaltungseinheitOhneZustaendigeBearbeiter);
		}
	}

	private List<Benutzer> getAlleKreiskoordinatorenInUndUeber(Verwaltungseinheit verwaltungseinheit) {
		List<Benutzer> alleKreiskoordinatorinnen = new ArrayList<>(
			massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(verwaltungseinheit)
		);

		// Uebergeordnete Orgas bis hin zur einschliesslich Kreisebene durchsuchen
		Optional<Verwaltungseinheit> zuDurchsuchendeVerwaltungseinheit = verwaltungseinheit.getUebergeordneteVerwaltungseinheit();
		while (zuDurchsuchendeVerwaltungseinheit.isPresent() &&
			!zuDurchsuchendeVerwaltungseinheit.get().getOrganisationsArt().equals(OrganisationsArt.REGIERUNGSBEZIRK)
			&&
			!zuDurchsuchendeVerwaltungseinheit.get().getOrganisationsArt().equals(OrganisationsArt.BUNDESLAND)) {
			alleKreiskoordinatorinnen.addAll(
				massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(
					zuDurchsuchendeVerwaltungseinheit.get())
			);
			zuDurchsuchendeVerwaltungseinheit = zuDurchsuchendeVerwaltungseinheit.get()
				.getUebergeordneteVerwaltungseinheit();
		}
		return alleKreiskoordinatorinnen;
	}

	private void benachrichtigeKreiskoordinatorinnen(String beantwortungsFrist,
		Map<Benutzer, Set<Benutzer>> kreiskoordinatorToMailempfaenger,
		Set<Verwaltungseinheit> verwaltungseinheitOhneRadwegeerfasserin) {

		kreiskoordinatorToMailempfaenger.forEach((kreiskoordinator, mailEmpfaenger) -> {
			Map<Verwaltungseinheit, Set<Benutzer>> gruppierteMailempfaenger = mailEmpfaenger.stream()
				.collect(Collectors.groupingBy(Benutzer::getOrganisation, Collectors.toSet()));

			Set<Verwaltungseinheit> kreiskoordinatorZustaendigAberOhneZustaendigeBearbeiter = verwaltungseinheitOhneRadwegeerfasserin.stream()
				.filter(
					verwaltungseinheit -> verwaltungseinheitService.istUebergeordnet(
						kreiskoordinator.getOrganisation(),
						verwaltungseinheit)
				).collect(Collectors.toSet());

			String email = generateUmsetzungsstandAbfrageEmailAnKreiskoordinatorinnen(kreiskoordinator,
				gruppierteMailempfaenger, kreiskoordinatorZustaendigAberOhneZustaendigeBearbeiter, beantwortungsFrist);
			mailService.sendHtmlMail(
				List.of(kreiskoordinator.getMailadresse().toString()),
				"[RadVIS] Übersicht der Benachrichtigungen durch die RadNETZ Maßnahmen Umsetzungsstandabfrage",
				email);
		});

		if (kreiskoordinatorToMailempfaenger.isEmpty()) {
			log.warn(
				"Es wurden keine Kreiskoordinator:innen gefunden, die über den Mailversand benachrichtigt werden konnten.");
		}
	}

	private String generateUmsetzungsstandAbfrageEmailAnKreiskoordinatorinnen(Benutzer kreiskoordinator,
		Map<Verwaltungseinheit, Set<Benutzer>> verwaltungseinheitToEmailEmpfaenger,
		Set<Verwaltungseinheit> kreiskoordinatorZustaendigAberOhneRadwegeerfasserin, String beantwortungsFrist) {

		Context ctx = new Context();
		ctx.setVariable("empfaenger", kreiskoordinator);
		ctx.setVariable("beantwortungsfrist", beantwortungsFrist);
		ctx.setVariable("radvisSupportMail", mailConfigurationProperties.getRadvisSupportMail());
		ctx.setVariable("verwaltungseinheitToEmailEmpfaenger", verwaltungseinheitToEmailEmpfaenger);
		ctx.setVariable("kreiskoordinatorZustaendigAberOhneRadwegeerfasserin",
			kreiskoordinatorZustaendigAberOhneRadwegeerfasserin);

		return this.templateEngine.process("umsetzungsstand-kreiskoordinator-benachrichtigung-template.html", ctx);
	}

	private String generateUmsetzungsstandAbfrageEmail(Verwaltungseinheit organisation, String beantwortungsFrist) {

		String radvisSupportMail = mailConfigurationProperties.getRadvisSupportMail();
		String radvisLink = getRadvisLink(organisation);

		Context ctx = new Context();
		ctx.setVariable("beantwortungsfrist", beantwortungsFrist);
		ctx.setVariable("radvisSupportMail", radvisSupportMail);
		ctx.setVariable("radvisLink", radvisLink);

		return this.templateEngine.process("umsetzungsstand-abfrage-mail-template.html", ctx);
	}

	protected String bestimmeUndFormattiereBeantwortungsfrist(LocalDateTime now, int frist) {
		return now.plusWeeks(frist).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
	}

	protected String getRadvisLink(Verwaltungseinheit organisation) {
		return commonConfigurationProperties.getBasisUrl()
			+ "app/viewer?infrastrukturen=massnahmen&tabellenVisible=true"
			+ "&filter_massnahmen=umsetzungsstandStatus:Aktualisierung%2520angefordert,baulastZustaendiger:"
			+ organisation.getName().replaceAll(" ", "%2520");
	}

	public Stream<Massnahme> getMassnahmenStream(List<Long> massnahmeIds) {
		List<List<Long>> partitionierteIDs = Lists.partition(massnahmeIds,
			postgisConfigurationProperties.getArgumentLimit());

		return partitionierteIDs.stream().flatMap(massnahmeRepository::findAllByIdInAndGeloeschtFalse);
	}
}
