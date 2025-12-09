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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.event.MassnahmeChangedEvent;
import de.wps.radvis.backend.massnahme.domain.event.MassnahmeStornierungAngefragtEvent;
import de.wps.radvis.backend.massnahme.domain.valueObject.BegruendungStornierungsanfrage;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.Mailadresse;

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

	@Mock
	private MassnahmenZustaendigkeitsService massnahmenZustaendigkeitsService;

	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;

	@Captor
	private ArgumentCaptor<List<String>> captor;

	private MassnahmenBenachrichtigungsService massnahmenBenachrichtigungsService;

	@BeforeEach
	void setup() {
		openMocks(this);
		massnahmenBenachrichtigungsService = new MassnahmenBenachrichtigungsService(massnahmeService, mailService,
			mailConfigurationProperties, commonConfigurationProperties, templateEngine,
			massnahmenZustaendigkeitsService, verwaltungseinheitService);
		when(commonConfigurationProperties.getBasisUrl()).thenReturn("basisUrl");
	}

	@Test
	void test_onMassnahmeChanged() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().bezeichnung(Bezeichnung.of(
			"Good Ol' Massnahme")).id(42L).build();

		when(massnahmeService.get(42L)).thenReturn(massnahme);

		Benutzer benutzer1 = BenutzerTestDataProvider.defaultBenutzer()
			.mailadresse(Mailadresse.of("benutzer1@testRadvis.de"))
			.id(10L).build();
		Benutzer benutzer2 = BenutzerTestDataProvider.defaultBenutzer()
			.mailadresse(Mailadresse.of("benutzer2@testRadvis.de"))
			.id(20L).build();

		massnahme.fuegeZuBenachrichtigendenBenutzerHinzu(benutzer1);
		massnahme.fuegeZuBenachrichtigendenBenutzerHinzu(benutzer2);

		MassnahmeChangedEvent massnahmeChangedEvent = new MassnahmeChangedEvent(42L);

		// act
		massnahmenBenachrichtigungsService.onMassnahmeChanged(massnahmeChangedEvent);

		// assert
		ArgumentCaptor<String> betreffCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

		verify(mailService, times(2)).sendHtmlMail(captor.capture(), betreffCaptor.capture(), any());
		verify(templateEngine, times(2)).process(any(String.class), contextCaptor.capture());
		assertThat(captor.getAllValues()).containsExactlyInAnyOrder(List.of(benutzer1.getMailadresse().toString()),
			List.of(benutzer2.getMailadresse().toString()));
		assertThat(betreffCaptor.getAllValues()).hasSize(2).allMatch(betreff -> betreff.contains("Good Ol' Massnahme"));
		assertThat(contextCaptor.getAllValues()).hasSize(2).allMatch(ctx -> ctx.getVariable("radvisLink").toString()
			.equals("basisUrl/viewer/massnahmen/42?infrastrukturen=massnahmen&tabellenVisible=true"));

	}

	@Test
	void onMassnahmeStornierungAngefragt() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().bezeichnung(Bezeichnung.of("Blubb"))
			.benutzerLetzteAenderung(BenutzerTestDataProvider.defaultBenutzer()
				.mailadresse(Mailadresse.of("blubb@abc.de")).vorname(Name.of("Rad")).nachname(Name.of("Vis"))
				.build())
			.umsetzungsstatus(Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024)
			.begruendungStornierungsanfrage(BegruendungStornierungsanfrage.of("Zwingende Begründung"))
			.zustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Gebiet")
				.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK).build())
			.id(756L).build();
		when(massnahmenZustaendigkeitsService.getZustaendigeRegierungsbezirke(any()))
			.thenReturn(List.of(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build()));
		String mailadresse = "test@abc.de";
		when(verwaltungseinheitService.findFunktionspostfach(any()))
			.thenReturn(Optional.of(Mailadresse.of(mailadresse)));

		// act
		massnahmenBenachrichtigungsService
			.onMassnahmeStornierungAngefragt(new MassnahmeStornierungAngefragtEvent(massnahme));

		// assert
		ArgumentCaptor<String> betreffCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

		verify(mailService, times(1)).sendHtmlMail(captor.capture(), betreffCaptor.capture(), any());
		verify(templateEngine, times(1)).process(any(String.class), contextCaptor.capture());
		assertThat(captor.getValue()).containsExactly(mailadresse);
		assertThat(betreffCaptor.getValue()).isEqualTo("[RadVIS] Stornierungsanfrage zu Maßnahme Blubb");
		assertThat(contextCaptor.getValue().getVariable("radvisLink").toString())
			.isEqualTo("basisUrl/viewer/massnahmen/756?infrastrukturen=massnahmen&tabellenVisible=true");
		assertThat(contextCaptor.getValue().getVariable("zustaendigerName").toString())
			.isEqualTo("Gebiet (Regierungsbezirk)");
		assertThat(contextCaptor.getValue().getVariable("anfragerEmail").toString()).isEqualTo("blubb@abc.de");
		assertThat(contextCaptor.getValue().getVariable("anfragerName").toString()).isEqualTo("Rad Vis");
		assertThat(contextCaptor.getValue().getVariable("begruendung").toString()).isEqualTo("Zwingende Begründung");
	}

	@Test
	void onMassnahmeStornierungAngefragt_noZustaendigesRP_doesNotSend() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().bezeichnung(Bezeichnung.of("Blubb"))
			.benutzerLetzteAenderung(BenutzerTestDataProvider.defaultBenutzer()
				.mailadresse(Mailadresse.of("blubb@abc.de")).vorname(Name.of("Rad")).nachname(Name.of("Vis"))
				.build())
			.zustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Gebiet")
				.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK).build())
			.id(756L).build();
		when(massnahmenZustaendigkeitsService.getZustaendigeRegierungsbezirke(any()))
			.thenReturn(Collections.emptyList());
		String mailadresse = "test@abc.de";
		when(verwaltungseinheitService.findFunktionspostfach(any()))
			.thenReturn(Optional.of(Mailadresse.of(mailadresse)));

		// act
		massnahmenBenachrichtigungsService
			.onMassnahmeStornierungAngefragt(new MassnahmeStornierungAngefragtEvent(massnahme));

		// assert
		verify(mailService, never()).sendHtmlMail(any(), any(), any());
	}

	@Test
	void onMassnahmeStornierungAngefragt_noFunktionspostfach_doesNotSend() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().bezeichnung(Bezeichnung.of("Blubb"))
			.benutzerLetzteAenderung(BenutzerTestDataProvider.defaultBenutzer()
				.mailadresse(Mailadresse.of("blubb@abc.de")).vorname(Name.of("Rad")).nachname(Name.of("Vis"))
				.build())
			.zustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Gebiet")
				.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK).build())
			.id(756L).build();
		when(massnahmenZustaendigkeitsService.getZustaendigeRegierungsbezirke(any()))
			.thenReturn(List.of(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build()));
		when(verwaltungseinheitService.findFunktionspostfach(any()))
			.thenReturn(Optional.empty());

		// act
		massnahmenBenachrichtigungsService
			.onMassnahmeStornierungAngefragt(new MassnahmeStornierungAngefragtEvent(massnahme));

		// assert
		verify(mailService, never()).sendHtmlMail(any(), any(), any());
	}
}
