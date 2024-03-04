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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.hamcrest.MockitoHamcrest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.massnahme.domain.bezug.NetzBezugTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitEntferntEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class MassnahmeRueckstufungStornierungServiceTest {
	@Mock
	private MassnahmeService massnahmeService;

	@Mock
	private MassnahmenZustaendigkeitsService massnahmenZustaendigkeitsService;

	@Mock
	private BenutzerService benutzerService;

	@Mock
	private KantenRepository kantenRepository;

	@Mock
	private MailService mailService;

	@Mock
	private MailConfigurationProperties mailConfigurationProperties;

	@Mock
	private CommonConfigurationProperties commonConfigurationProperties;

	@Mock
	private TemplateEngine templateEngine;

	private MockedStatic<LocalDateTime> localDateTimeMock;
	private LocalDateTime localDateTimeTestValue;

	private Benutzer technischerBenutzer;

	@Captor
	private ArgumentCaptor<Context> contextCaptor;

	private MassnahmeRueckstufungStornierungService massnahmeRueckstufungStornierungService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		massnahmeRueckstufungStornierungService = new MassnahmeRueckstufungStornierungService(
			massnahmeService,
			massnahmenZustaendigkeitsService,
			benutzerService,
			kantenRepository,
			mailService,
			mailConfigurationProperties,
			commonConfigurationProperties,
			templateEngine);
		when(commonConfigurationProperties.getBasisUrl()).thenReturn("basisUrl");

		localDateTimeTestValue = LocalDateTime.of(2022, 2, 22, 0, 0);

		technischerBenutzer = BenutzerTestDataProvider.technischerBenutzer().id(12345L).build();

		when(benutzerService.getTechnischerBenutzer()).thenReturn(technischerBenutzer);

		localDateTimeMock = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);
		localDateTimeMock.when(LocalDateTime::now).thenReturn(localDateTimeTestValue);
	}

	@AfterEach
	void cleanUp() {
		localDateTimeMock.close();
	}

	@Test
	void storniereMassnahmenBeiRueckstufung_happyPath_setztStatusKorrekt() {
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100000L)
			.build();

		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().id(10000L).build();

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(1000L)
			.build();

		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
			.id(100L).build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200), QuellSystem.DLM)
			.id(200L).build();
		Kante kante = KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.id(10L)
			.kantenAttributGruppe(kantenAttributGruppe)
			.build();

		KantenAttributGruppe kantenAttributGruppeRadNETZ = KantenAttributGruppeTestDataProvider.defaultValue()
			.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
			.id(2000L)
			.build();

		Knoten vonKnotenRadNETZ = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100),
				QuellSystem.DLM)
			.id(300L).build();
		Knoten nachKnotenRadNETZ = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200),
				QuellSystem.DLM)
			.id(400L).build();
		Kante kanteRadNETZ = KanteTestDataProvider.fromKnoten(vonKnotenRadNETZ, nachKnotenRadNETZ)
			.id(20L)
			.kantenAttributGruppe(kantenAttributGruppeRadNETZ)
			.build();

		Massnahme betroffeneMassnahmeKanteAbschnittsweise = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.umsetzungsstand(new Umsetzungsstand())
			.baulastZustaendiger(organisation)
			.build();

		Massnahme betroffeneMassnahmeVonKnoten = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.netzbezug(NetzBezugTestDataProvider.forKnoten(vonKnoten))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.umsetzungsstand(new Umsetzungsstand())
			.baulastZustaendiger(organisation)
			.build();

		Massnahme betroffeneMassnahmeKantePunktuell = MassnahmeTestDataProvider.withDefaultValues()
			.id(3L)
			.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.umsetzungsstand(new Umsetzungsstand())
			.baulastZustaendiger(organisation)
			.build();

		Massnahme nurTeilweiseBetroffeneMassnahme = MassnahmeTestDataProvider.withDefaultValues()
			.id(4L)
			.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante, kanteRadNETZ))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.umsetzungsstand(new Umsetzungsstand())
			.baulastZustaendiger(organisation)
			.build();

		when(kantenRepository.findByKantenAttributGruppeId(eq(kantenAttributGruppe.getId()))).thenReturn(kante);
		when(massnahmeService.findByKanteIdInNetzBezug(eq(kante.getId())))
			.thenReturn(List.of(betroffeneMassnahmeKanteAbschnittsweise, betroffeneMassnahmeKantePunktuell,
				nurTeilweiseBetroffeneMassnahme));
		when(massnahmeService.findByKnotenIdInNetzBezug(eq(vonKnoten.getId())))
			.thenReturn(List.of(betroffeneMassnahmeVonKnoten));

		when(massnahmeService.hatRadNETZNetzBezug(eq(betroffeneMassnahmeKanteAbschnittsweise)))
			.thenReturn(false);
		when(massnahmeService.hatRadNETZNetzBezug(eq(betroffeneMassnahmeKantePunktuell)))
			.thenReturn(false);
		when(massnahmeService.hatRadNETZNetzBezug(eq(betroffeneMassnahmeVonKnoten)))
			.thenReturn(false);
		when(massnahmeService.hatRadNETZNetzBezug(eq(nurTeilweiseBetroffeneMassnahme)))
			.thenReturn(true);

		when(massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
			MockitoHamcrest.argThat(is(oneOf(
				betroffeneMassnahmeKanteAbschnittsweise, betroffeneMassnahmeKantePunktuell,
				betroffeneMassnahmeVonKnoten, nurTeilweiseBetroffeneMassnahme
			))))).thenReturn(List.of(benutzer));

		// Act
		massnahmeRueckstufungStornierungService
			.storniereMassnahmenBeiRueckstufung(new RadNetzZugehoerigkeitEntferntEvent(kantenAttributGruppe.getId()));

		// Assert
		assertThat(betroffeneMassnahmeKanteAbschnittsweise.getUmsetzungsstatus()).isEqualTo(Umsetzungsstatus.STORNIERT);
		assertThat(betroffeneMassnahmeKanteAbschnittsweise.getBenutzerLetzteAenderung()).isEqualTo(technischerBenutzer);
		assertThat(betroffeneMassnahmeKanteAbschnittsweise.getLetzteAenderung()).isEqualTo(localDateTimeTestValue);

		assertThat(betroffeneMassnahmeVonKnoten.getUmsetzungsstatus()).isEqualTo(Umsetzungsstatus.STORNIERT);
		assertThat(betroffeneMassnahmeVonKnoten.getBenutzerLetzteAenderung()).isEqualTo(technischerBenutzer);
		assertThat(betroffeneMassnahmeVonKnoten.getLetzteAenderung()).isEqualTo(localDateTimeTestValue);

		assertThat(betroffeneMassnahmeKantePunktuell.getUmsetzungsstatus()).isEqualTo(Umsetzungsstatus.STORNIERT);
		assertThat(betroffeneMassnahmeKantePunktuell.getBenutzerLetzteAenderung()).isEqualTo(technischerBenutzer);
		assertThat(betroffeneMassnahmeKantePunktuell.getLetzteAenderung()).isEqualTo(localDateTimeTestValue);

		assertThat(nurTeilweiseBetroffeneMassnahme.getUmsetzungsstatus()).isNotEqualTo(Umsetzungsstatus.STORNIERT);
		assertThat(nurTeilweiseBetroffeneMassnahme.getBenutzerLetzteAenderung()).isNotEqualTo(technischerBenutzer);
		assertThat(nurTeilweiseBetroffeneMassnahme.getLetzteAenderung()).isNotEqualTo(localDateTimeTestValue);

		verify(mailService, times(1)).sendHtmlMail(eq(List.of(benutzer.getMailadresse().toString())),
			eq("[RadVIS] Massnahmenstornierung aufgrund von RadNETZ-Rueckstufung"), any());

		verify(templateEngine, times(1)).process(any(String.class), contextCaptor.capture());

		verify(massnahmeService, times(1)).findByKanteIdInNetzBezug(eq(kante.getId()));
		verify(massnahmeService, times(2)).findByKnotenIdInNetzBezug(anyLong());

		String linkTemplate = "basisUrl/viewer/massnahmen/%s?infrastrukturen=massnahmen&tabellenVisible=true&netzklassen=RADNETZ";
		assertThat(contextCaptor.getValue().getVariable("radvisLinks"))
			.asList()
			.containsExactlyInAnyOrder(
				String.format(linkTemplate, betroffeneMassnahmeKanteAbschnittsweise.getId()),
				String.format(linkTemplate, betroffeneMassnahmeVonKnoten.getId()),
				String.format(linkTemplate, betroffeneMassnahmeKantePunktuell.getId())
			);
	}

	@Test
	void storniereMassnahmenBeiRueckstufung_keineZustaendigenBenutzer_KeineEmail() {
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100000L)
			.build();

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(1000L)
			.build();

		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
			.id(100L).build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200), QuellSystem.DLM)
			.id(200L).build();
		Kante kante = KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.id(10L)
			.kantenAttributGruppe(kantenAttributGruppe)
			.build();

		Massnahme betroffeneMassnahmeKante = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.umsetzungsstand(new Umsetzungsstand())
			.baulastZustaendiger(organisation)
			.build();

		Massnahme betroffeneMassnahmeVonKnoten = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.netzbezug(NetzBezugTestDataProvider.forKnoten(vonKnoten))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.umsetzungsstand(new Umsetzungsstand())
			.baulastZustaendiger(organisation)
			.build();

		when(kantenRepository.findByKantenAttributGruppeId(eq(kantenAttributGruppe.getId()))).thenReturn(kante);
		when(massnahmeService.findByKanteIdInNetzBezug(eq(kante.getId())))
			.thenReturn(List.of(betroffeneMassnahmeKante));
		when(massnahmeService.findByKnotenIdInNetzBezug(eq(vonKnoten.getId())))
			.thenReturn(List.of(betroffeneMassnahmeVonKnoten));

		when(massnahmeService.hatRadNETZNetzBezug(eq(betroffeneMassnahmeKante)))
			.thenReturn(false);
		when(massnahmeService.hatRadNETZNetzBezug(eq(betroffeneMassnahmeVonKnoten)))
			.thenReturn(false);

		when(massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
			MockitoHamcrest.argThat(is(oneOf(
				betroffeneMassnahmeKante, betroffeneMassnahmeVonKnoten
			))))).thenReturn(Collections.emptyList());

		// Act
		massnahmeRueckstufungStornierungService
			.storniereMassnahmenBeiRueckstufung(new RadNetzZugehoerigkeitEntferntEvent(kantenAttributGruppe.getId()));

		// Assert
		assertThat(betroffeneMassnahmeKante.getUmsetzungsstatus()).isEqualTo(Umsetzungsstatus.STORNIERT);
		assertThat(betroffeneMassnahmeKante.getBenutzerLetzteAenderung()).isEqualTo(technischerBenutzer);
		assertThat(betroffeneMassnahmeKante.getLetzteAenderung()).isEqualTo(localDateTimeTestValue);

		assertThat(betroffeneMassnahmeVonKnoten.getUmsetzungsstatus()).isEqualTo(Umsetzungsstatus.STORNIERT);
		assertThat(betroffeneMassnahmeVonKnoten.getBenutzerLetzteAenderung()).isEqualTo(technischerBenutzer);
		assertThat(betroffeneMassnahmeVonKnoten.getLetzteAenderung()).isEqualTo(localDateTimeTestValue);

		verify(mailService, never()).sendHtmlMail(any(), any(), any());
		verify(templateEngine, never()).process(any(String.class), any());
	}

	@Test
	void storniereMassnahmenBeiRueckstufung_mehrereAttributGruppen_nurEineEmailProBenutzer() {
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100000L)
			.build();

		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().id(10000L).build();

		KantenAttributGruppe kantenAttributGruppe1 = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(1000L)
			.build();

		Knoten vonKnoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
			.id(100L).build();
		Knoten nachKnoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200), QuellSystem.DLM)
			.id(200L).build();
		Kante kante1 = KanteTestDataProvider.fromKnoten(vonKnoten1, nachKnoten1)
			.id(10L)
			.kantenAttributGruppe(kantenAttributGruppe1)
			.build();

		KantenAttributGruppe kantenAttributGruppe2 = KantenAttributGruppeTestDataProvider.defaultValue()
			.id(2000L)
			.build();
		Knoten vonKnoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
			.id(300L).build();
		Knoten nachKnoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 200), QuellSystem.DLM)
			.id(400L).build();
		Kante kante2 = KanteTestDataProvider.fromKnoten(vonKnoten2, nachKnoten2)
			.id(20L)
			.kantenAttributGruppe(kantenAttributGruppe2)
			.build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.netzbezug(NetzBezugTestDataProvider.forKanteAbschnittsweise(kante1))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.umsetzungsstand(new Umsetzungsstand())
			.baulastZustaendiger(organisation)
			.build();

		Massnahme betroffeneMassnahme2 = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.netzbezug(NetzBezugTestDataProvider.forKantePunktuell(kante2))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.umsetzungsstand(new Umsetzungsstand())
			.baulastZustaendiger(organisation)
			.build();

		when(kantenRepository.findByKantenAttributGruppeId(eq(kantenAttributGruppe1.getId()))).thenReturn(kante1);
		when(kantenRepository.findByKantenAttributGruppeId(eq(kantenAttributGruppe2.getId()))).thenReturn(kante2);
		when(massnahmeService.findByKanteIdInNetzBezug(eq(kante1.getId())))
			.thenReturn(List.of(betroffeneMassnahme1));
		when(massnahmeService.findByKnotenIdInNetzBezug(eq(vonKnoten1.getId())))
			.thenReturn(List.of(betroffeneMassnahme1));
		when(massnahmeService.findByKnotenIdInNetzBezug(eq(nachKnoten1.getId())))
			.thenReturn(List.of(betroffeneMassnahme1));
		when(massnahmeService.findByKanteIdInNetzBezug(eq(kante2.getId())))
			.thenReturn(List.of(betroffeneMassnahme2));
		when(massnahmeService.findByKnotenIdInNetzBezug(eq(vonKnoten2.getId())))
			.thenReturn(List.of(betroffeneMassnahme2));
		when(massnahmeService.findByKnotenIdInNetzBezug(eq(nachKnoten2.getId())))
			.thenReturn(List.of(betroffeneMassnahme2));

		when(massnahmeService.hatRadNETZNetzBezug(eq(betroffeneMassnahme1)))
			.thenReturn(false);
		when(massnahmeService.hatRadNETZNetzBezug(eq(betroffeneMassnahme2)))
			.thenReturn(false);

		when(massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
			MockitoHamcrest.argThat(is(oneOf(
				betroffeneMassnahme1, betroffeneMassnahme2
			))))).thenReturn(List.of(benutzer));

		// Act
		massnahmeRueckstufungStornierungService
			.storniereMassnahmenBeiRueckstufung(
				new RadNetzZugehoerigkeitEntferntEvent(kantenAttributGruppe1.getId(), kantenAttributGruppe2.getId()));

		// Assert
		assertThat(betroffeneMassnahme1.getUmsetzungsstatus()).isEqualTo(Umsetzungsstatus.STORNIERT);
		assertThat(betroffeneMassnahme1.getBenutzerLetzteAenderung()).isEqualTo(technischerBenutzer);
		assertThat(betroffeneMassnahme1.getLetzteAenderung()).isEqualTo(localDateTimeTestValue);

		assertThat(betroffeneMassnahme2.getUmsetzungsstatus()).isEqualTo(Umsetzungsstatus.STORNIERT);
		assertThat(betroffeneMassnahme2.getBenutzerLetzteAenderung()).isEqualTo(technischerBenutzer);
		assertThat(betroffeneMassnahme2.getLetzteAenderung()).isEqualTo(localDateTimeTestValue);

		verify(massnahmeService, times(2)).findByKanteIdInNetzBezug(anyLong());
		verify(massnahmeService, times(4)).findByKnotenIdInNetzBezug(anyLong());

		verify(mailService, times(1)).sendHtmlMail(eq(List.of(benutzer.getMailadresse().toString())),
			eq("[RadVIS] Massnahmenstornierung aufgrund von RadNETZ-Rueckstufung"), any());

		verify(templateEngine, times(1)).process(any(String.class), contextCaptor.capture());

		String linkTemplate = "basisUrl/viewer/massnahmen/%s?infrastrukturen=massnahmen&tabellenVisible=true&netzklassen=RADNETZ";
		assertThat(contextCaptor.getValue().getVariable("radvisLinks"))
			.asList()
			.containsExactlyInAnyOrder(
				String.format(linkTemplate, betroffeneMassnahme1.getId()),
				String.format(linkTemplate, betroffeneMassnahme2.getId())
			);
	}
}
