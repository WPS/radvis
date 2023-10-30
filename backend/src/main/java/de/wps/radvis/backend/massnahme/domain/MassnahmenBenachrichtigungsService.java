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

import java.util.List;

import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.event.MassnahmeChangedEvent;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;

public class MassnahmenBenachrichtigungsService {

	private final MassnahmeService massnahmeService;
	private final MailService mailService;
	private final MailConfigurationProperties mailConfigurationProperties;
	private final CommonConfigurationProperties commonConfigurationProperties;
	private final TemplateEngine templateEngine;

	public MassnahmenBenachrichtigungsService(
		MassnahmeService massnahmeService, MailService mailService,
		MailConfigurationProperties mailConfigurationProperties,
		CommonConfigurationProperties commonConfigurationProperties, TemplateEngine templateEngine) {
		this.massnahmeService = massnahmeService;
		this.mailService = mailService;
		this.mailConfigurationProperties = mailConfigurationProperties;
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.templateEngine = templateEngine;
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onMassnahmeChanged(MassnahmeChangedEvent massnahmeChangedEvent) {
		Massnahme massnahme = this.massnahmeService.get(massnahmeChangedEvent.getMassnahmeId());
		this.versendeAenderungsBenachrichtigung(massnahme);
	}

	private void versendeAenderungsBenachrichtigung(Massnahme massnahme) {
		massnahme.getZuBenachrichtigendeBenutzer().forEach(benutzer -> {
			mailService.sendHtmlMail(
				List.of(benutzer.getMailadresse().toString()),
				String.format("[RadVIS] Änderung der Maßnahme %s", massnahme.getBezeichnung()),
				generateAenderungsBenachrichtigungsEmail(
					benutzer.getVorname(),
					benutzer.getNachname(),
					massnahme.getBezeichnung(),
					massnahme.getId()));
		});
	}

	private String generateAenderungsBenachrichtigungsEmail(Name vorname, Name nachname, Bezeichnung bezeichnung,
		Long massnahmeId) {

		String radvisSupportMail = mailConfigurationProperties.getRadvisSupportMail();
		String radvisLink = getRadvisLink(massnahmeId);

		Context ctx = new Context();
		ctx.setVariable("vorname", vorname.toString());
		ctx.setVariable("nachname", nachname.toString());
		ctx.setVariable("radvisSupportMail", radvisSupportMail);
		ctx.setVariable("radvisLink", radvisLink);
		ctx.setVariable("bezeichnung", bezeichnung.toString());

		return this.templateEngine.process("massnahme-aenderungs-benachrichtigung-mail-template.html", ctx);
	}

	protected String getRadvisLink(Long massnahmeId) {
		return commonConfigurationProperties.getBasisUrl()
			+ "app/viewer/massnahmen/" + massnahmeId + "?infrastrukturen=massnahmen&tabellenVisible=true";
	}
}
