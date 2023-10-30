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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.event.MassnahmeChangedEvent;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;

class MassnahmenBenachrichtigungsServiceTest {

	@Mock
	private MassnahmeService massnahmeService;

	@Mock
	private MailService mailService;

	@Mock
	private MailConfigurationProperties mailConfigurationProperties;

	@Mock
	private CommonConfigurationProperties commonConfigurationProperties;

	@Mock
	private TemplateEngine templateEngine;

	@Captor
	private ArgumentCaptor<List<String>> captor;

	private MassnahmenBenachrichtigungsService massnahmenBenachrichtigungsService;

	@BeforeEach
	void setup() {
		openMocks(this);
		massnahmenBenachrichtigungsService = new MassnahmenBenachrichtigungsService(massnahmeService, mailService,
			mailConfigurationProperties, commonConfigurationProperties, templateEngine);
		when(commonConfigurationProperties.getBasisUrl()).thenReturn("basisUrl/");
	}

	@Test
	void test_onMassnahmeChanged() {
		//arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().bezeichnung(Bezeichnung.of(
			"Good Ol' Massnahme")).id(42L).build();

		when(massnahmeService.get(42L)).thenReturn(massnahme);

		Benutzer benutzer1 = BenutzerTestDataProvider.defaultBenutzer().mailadresse(Mailadresse.of("benutzer1@wps.de"))
			.id(10L).build();
		Benutzer benutzer2 = BenutzerTestDataProvider.defaultBenutzer().mailadresse(Mailadresse.of("benutzer2@wps.de"))
			.id(20L).build();

		massnahme.fuegeZuBenachrichtigendenBenutzerHinzu(benutzer1);
		massnahme.fuegeZuBenachrichtigendenBenutzerHinzu(benutzer2);

		MassnahmeChangedEvent massnahmeChangedEvent = new MassnahmeChangedEvent(42L);

		//act
		massnahmenBenachrichtigungsService.onMassnahmeChanged(massnahmeChangedEvent);

		//assert
		ArgumentCaptor<String> betreffCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

		verify(mailService, times(2)).sendHtmlMail(captor.capture(), betreffCaptor.capture(), any());
		verify(templateEngine, times(2)).process(any(String.class), contextCaptor.capture());
		assertThat(captor.getAllValues()).containsExactlyInAnyOrder(List.of(benutzer1.getMailadresse().toString()),
			List.of(benutzer2.getMailadresse().toString()));
		assertThat(betreffCaptor.getAllValues()).hasSize(2).allMatch(betreff -> betreff.contains("Good Ol' Massnahme"));
		assertThat(contextCaptor.getAllValues()).hasSize(2).allMatch(ctx -> ctx.getVariable("radvisLink").toString()
			.equals("basisUrl/app/viewer/massnahmen/42?infrastrukturen=massnahmen&tabellenVisible=true"));

	}

}
