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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.transaction.Transactional;

import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitEntferntEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
public class MassnahmeRueckstufungStornierungService {
	private final MassnahmeService massnahmeService;
	private final MassnahmenZustaendigkeitsService massnahmenZustaendigkeitsService;
	private final BenutzerService benutzerService;
	private final KantenRepository kantenRepository;
	private final MailService mailService;
	private final MailConfigurationProperties mailConfigurationProperties;
	private final CommonConfigurationProperties commonConfigurationProperties;
	private final TemplateEngine templateEngine;

	public MassnahmeRueckstufungStornierungService(
		MassnahmeService massnahmeService,
		MassnahmenZustaendigkeitsService massnahmenZustaendigkeitsService,
		BenutzerService benutzerService, KantenRepository kantenRepository,
		MailService mailService,
		MailConfigurationProperties mailConfigurationProperties,
		CommonConfigurationProperties commonConfigurationProperties,
		TemplateEngine templateEngine) {
		this.massnahmeService = massnahmeService;
		this.massnahmenZustaendigkeitsService = massnahmenZustaendigkeitsService;
		this.benutzerService = benutzerService;
		this.kantenRepository = kantenRepository;
		this.mailService = mailService;
		this.mailConfigurationProperties = mailConfigurationProperties;
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.templateEngine = templateEngine;
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void storniereMassnahmenBeiRueckstufung(RadNetzZugehoerigkeitEntferntEvent event) {
		Stream<Kante> kanten = event.getKantenAttributGruppeIds().stream()
			.map(kantenRepository::findByKantenAttributGruppeId);
		LocalDateTime stornierungsZeitpunkt = LocalDateTime.now();
		Set<Massnahme> massnahmen = new HashSet<>();

		kanten.forEach(kante -> {
			massnahmen.addAll(massnahmeService.findByKanteIdInNetzBezug(kante.getId()));
			massnahmen.addAll(massnahmeService.findByKnotenIdInNetzBezug(kante.getVonKnoten().getId()));
			massnahmen.addAll(massnahmeService.findByKnotenIdInNetzBezug(kante.getNachKnoten().getId()));
		});

		Map<Benutzer, Set<Long>> benutzerMassnahmenIdMap = new HashMap<>();

		massnahmen.stream()
			.filter(massnahme -> !massnahmeService.hatRadNETZNetzBezug(massnahme) && massnahme.isRadNETZMassnahme())
			.forEach(massnahme -> {
				massnahme.stornieren(benutzerService.getTechnischerBenutzer(), stornierungsZeitpunkt);
				massnahmeService.saveMassnahme(massnahme);
				massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(massnahme).forEach(
					benutzer -> benutzerMassnahmenIdMap.merge(
						benutzer,
						new HashSet<>() {{
							add(massnahme.getId());
						}},
						(oldMassnahmenIds, newMassnahmenIds) -> {
							oldMassnahmenIds.addAll(newMassnahmenIds);
							return oldMassnahmenIds;
						}));
			});

		// Map von empfaenger auf massnahmen
		benutzerMassnahmenIdMap
			.forEach((benutzer, massnahmenIds) ->
				mailService.sendHtmlMail(
					List.of(benutzer.getMailadresse().toString()),
					"[RadVIS] Massnahmenstornierung aufgrund von RadNETZ-Rueckstufung",
					generateRueckstufungStornierungEmail(massnahmenIds)));

	}

	private String generateRueckstufungStornierungEmail(Set<Long> massnahmenIds) {
		String radvisSupportMail = mailConfigurationProperties.getRadvisSupportMail();
		List<String> radvisLinks = getRadvisLinks(massnahmenIds);

		Context ctx = new Context();
		ctx.setVariable("radvisSupportMail", radvisSupportMail);
		ctx.setVariable("radvisLinks", radvisLinks);

		return this.templateEngine.process("rueckstufung-stornierung-mail-template.html", ctx);
	}

	private List<String> getRadvisLinks(Set<Long> massnahmeIds) {
		return massnahmeIds.stream().map(this::getRadvisLink).collect(Collectors.toList());
	}

	private String getRadvisLink(Long massnahmeId) {
		return commonConfigurationProperties.getBasisUrl()
			+ "app/viewer/massnahmen/" + massnahmeId
			+ "?infrastrukturen=massnahmen&tabellenVisible=true&netzklassen=RADNETZ";
	}
}
