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
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class UmsetzungsstandabfrageServiceTest {
	@Mock
	private MassnahmeRepository massnahmeRepository;

	@Mock
	private MassnahmenZustaendigkeitsService massnahmenZustaendigkeitsService;

	@Mock
	private VerwaltungseinheitService verwaltungseinheitService;

	@Mock
	private MailService mailService;

	@Mock
	private MailConfigurationProperties mailConfigurationProperties;

	@Mock
	private CommonConfigurationProperties commonConfigurationProperties;

	@Mock
	private UmsetzungsstandsabfrageConfigurationProperties umsetzungsstandsabfrageConfigurationProperties;

	@Mock
	private PostgisConfigurationProperties postgisConfigurationProperties;

	@Mock
	private TemplateEngine templateEngine;

	@Captor
	private ArgumentCaptor<List<String>> emailEmpfaengerCaptor;

	@Captor
	private ArgumentCaptor<Context> templateContextCaptor;

	private UmsetzungsstandabfrageService umsetzungsstandabfrageService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(postgisConfigurationProperties.getArgumentLimit()).thenReturn(10);
		when(umsetzungsstandsabfrageConfigurationProperties.getFrist()).thenReturn(8);
		umsetzungsstandabfrageService = new UmsetzungsstandabfrageService(massnahmeRepository,
			massnahmenZustaendigkeitsService,
			verwaltungseinheitService, mailService,
			mailConfigurationProperties, commonConfigurationProperties, umsetzungsstandsabfrageConfigurationProperties,
			postgisConfigurationProperties, templateEngine);
	}

	@Test
	void starteUmsetzungsstandsabfrage_happyPath_setztStatusKorrekt() {
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.bearbeiterinVmRadnetzAdminInaktiv(organisation).build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();
		this.setzeUmsetzungsstandStatusAufAktualisiert(betroffeneMassnahme1, benutzer);

		Massnahme betroffeneMassnahme2 = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.baulastZustaendiger(organisation)
			.zustaendiger(organisation)
			.build();
		this.setzeUmsetzungsstandStatusAufAktualisiert(betroffeneMassnahme2, benutzer);

		Massnahme nichtBetroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(3L)
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();
		this.setzeUmsetzungsstandStatusAufAktualisiert(nichtBetroffeneMassnahme1, benutzer);

		Massnahme nichtBetroffeneMassnahme2 = MassnahmeTestDataProvider.withDefaultValues()
			.id(4L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.UMGESETZT)
			.baulastZustaendiger(organisation)
			.zustaendiger(organisation)
			.build();
		this.setzeUmsetzungsstandStatusAufAktualisiert(nichtBetroffeneMassnahme2, benutzer);

		Massnahme nichtBetroffeneMassnahme3 = MassnahmeTestDataProvider.withDefaultValues()
			.id(5L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();
		this.setzeUmsetzungsstandStatusAufAktualisiert(nichtBetroffeneMassnahme3, benutzer);
		nichtBetroffeneMassnahme3.stornieren(benutzer, LocalDateTime.now());

		List<Long> massnahmeIds = List.of(betroffeneMassnahme1.getId(), betroffeneMassnahme2.getId(),
			nichtBetroffeneMassnahme1.getId(),
			nichtBetroffeneMassnahme2.getId(), nichtBetroffeneMassnahme3.getId());
		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(eq(massnahmeIds))).thenReturn(
			Stream.of(betroffeneMassnahme1, betroffeneMassnahme2,
				nichtBetroffeneMassnahme1,
				nichtBetroffeneMassnahme2, nichtBetroffeneMassnahme3));

		// Act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(massnahmeIds);
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// Assert
		assertThat(betroffeneMassnahme1.getUmsetzungsstand().orElseThrow().getUmsetzungsstandStatus()).isEqualTo(
			UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT);
		assertThat(betroffeneMassnahme2.getUmsetzungsstand().orElseThrow().getUmsetzungsstandStatus()).isEqualTo(
			UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT);

		assertThat(nichtBetroffeneMassnahme1.getUmsetzungsstand()).isNotPresent();
		assertThat(nichtBetroffeneMassnahme2.getUmsetzungsstand().orElseThrow().getUmsetzungsstandStatus())
			.isEqualTo(UmsetzungsstandStatus.AKTUALISIERT);
		assertThat(nichtBetroffeneMassnahme3.getUmsetzungsstand().orElseThrow().getUmsetzungsstandStatus())
			.isEqualTo(UmsetzungsstandStatus.AKTUALISIERT);
	}

	@Test
	void starteUmsetzungsstandsabfrage_EmpfaengerkreisBestimmen_radwegeErfasserinDirektInOrga() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation).build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme1)))
				.thenReturn(List.of(benutzer));

		this.setzeUmsetzungsstandStatusAufAktualisiert(betroffeneMassnahme1, benutzer);

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(
			eq(List.of(betroffeneMassnahme1.getId())))).thenReturn(
				Stream.of(betroffeneMassnahme1));

		// act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(
			List.of(betroffeneMassnahme1.getId()));
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// assert
		verify(mailService, times(1)).sendHtmlMail(emailEmpfaengerCaptor.capture(), any(), any());
		List<String> empfaenger = emailEmpfaengerCaptor.getValue();
		assertThat(empfaenger).containsExactly("radwegeErfasserinKommuneKreis@testRadvis.com");
	}

	@Test
	void starteUmsetzungsstandsabfrage_EmailAnKreiskoordinatorinnen_direktInOrga() {
		// arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(100L)
			.build();
		Benutzer radwegeErfasserin = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(verwaltungseinheit).id(1L)
			.build();
		Benutzer kreiskoordinatorin = BenutzerTestDataProvider.kreiskoordinator(verwaltungseinheit).id(2L).build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(verwaltungseinheit)
			.build();

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme1)))
				.thenReturn(List.of(radwegeErfasserin));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(verwaltungseinheit)))
			.thenReturn(List.of(kreiskoordinatorin));

		when(verwaltungseinheitService.istUebergeordnet(any(), any())).thenReturn(true);

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(
			eq(List.of(betroffeneMassnahme1.getId())))).thenReturn(
				Stream.of(betroffeneMassnahme1));

		// act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(
			List.of(betroffeneMassnahme1.getId()));
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// assert
		verify(mailService, times(2)).sendHtmlMail(emailEmpfaengerCaptor.capture(), any(), any());
		List<String> benachrichtigteKreiskoordinatinnen = emailEmpfaengerCaptor.getAllValues().get(1);
		assertThat(benachrichtigteKreiskoordinatinnen).containsExactly("kreisKoordinator@testRadvis.com");

		verify(templateEngine, times(2)).process(any(String.class), templateContextCaptor.capture());
		Context context = templateContextCaptor.getAllValues().get(1);
		assertThat((Benutzer) context.getVariable("empfaenger")).isEqualTo(kreiskoordinatorin);

		Map<Verwaltungseinheit, Set<Benutzer>> expectedMap = new HashMap<>();
		expectedMap.put(verwaltungseinheit, Set.of(radwegeErfasserin));
		assertThat(context.getVariable("verwaltungseinheitToEmailEmpfaenger")).isEqualTo(expectedMap);

		assertThat(context.getVariable("kreiskoordinatorZustaendigAberOhneRadwegeerfasserin")).isEqualTo(Set.of());
	}

	@Test
	void starteUmsetzungsstandsabfrage_EmailAnKreiskoordinatorinnen_InUebergeordneterOrgaVonRadwegeerfasserin() {
		// arrange
		Verwaltungseinheit kreis = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(102L)
			.build();
		Verwaltungseinheit gemeinde = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.uebergeordneteOrganisation(kreis)
			.id(101L)
			.build();

		Benutzer kreiskoordinatorin = BenutzerTestDataProvider.kreiskoordinator(kreis).id(2L).build();
		Benutzer radwegeErfasserin = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(gemeinde).id(1L).build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(gemeinde)
			.build();

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme1)))
				.thenReturn(List.of(radwegeErfasserin));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(gemeinde)))
			.thenReturn(List.of());
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(kreis)))
			.thenReturn(List.of(kreiskoordinatorin));

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(
			eq(List.of(betroffeneMassnahme1.getId())))).thenReturn(
				Stream.of(betroffeneMassnahme1));

		when(verwaltungseinheitService.istUebergeordnet(any(), any())).thenReturn(true);

		// act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(
			List.of(betroffeneMassnahme1.getId()));
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// assert
		verify(mailService, times(2)).sendHtmlMail(emailEmpfaengerCaptor.capture(), any(), any());
		List<String> benachrichtigteKreiskoordinatinnen = emailEmpfaengerCaptor.getAllValues().get(1);
		assertThat(benachrichtigteKreiskoordinatinnen).containsExactly("kreisKoordinator@testRadvis.com");

		verify(templateEngine, times(2)).process(any(String.class), templateContextCaptor.capture());
		Context context = templateContextCaptor.getAllValues().get(1);
		assertThat((Benutzer) context.getVariable("empfaenger")).isEqualTo(kreiskoordinatorin);

		Map<Verwaltungseinheit, Set<Benutzer>> expectedMap = new HashMap<>();
		expectedMap.put(gemeinde, Set.of(radwegeErfasserin));
		assertThat(context.getVariable("verwaltungseinheitToEmailEmpfaenger")).isEqualTo(expectedMap);

		assertThat(context.getVariable("kreiskoordinatorZustaendigAberOhneRadwegeerfasserin")).isEqualTo(Set.of());
	}

	@Test
	void starteUmsetzungsstandsabfrage_EmailAnKreiskoordinatorinnen_BaulastZustHatKeineRadwegeErfasserin() {
		// arrange
		Verwaltungseinheit kreis = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.KREIS)
			.id(102L)
			.build();
		Verwaltungseinheit mitRadwegeErfasserin = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.id(101L)
			.uebergeordneteOrganisation(kreis)
			.build();
		Verwaltungseinheit ohneRadwegeerfasserin = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(100L)
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.uebergeordneteOrganisation(kreis)
			.build();
		Benutzer radwegeErfasserin = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(mitRadwegeErfasserin).id(1L)
			.build();
		Benutzer kreiskoordinatorin = BenutzerTestDataProvider.kreiskoordinator(kreis).id(2L).build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(mitRadwegeErfasserin)
			.build();
		Massnahme betroffeneMassnahme2 = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(ohneRadwegeerfasserin)
			.build();

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme1)))
				.thenReturn(List.of(radwegeErfasserin));
		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme2)))
				.thenReturn(List.of());
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(mitRadwegeErfasserin)))
			.thenReturn(List.of(kreiskoordinatorin));

		when(verwaltungseinheitService.istUebergeordnet(any(), any())).thenReturn(true);

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(
			eq(List.of(betroffeneMassnahme1.getId(), betroffeneMassnahme2.getId())))).thenReturn(
				Stream.of(betroffeneMassnahme1, betroffeneMassnahme2));

		// act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(
			List.of(betroffeneMassnahme1.getId(), betroffeneMassnahme2.getId()));
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// assert
		verify(mailService, times(2)).sendHtmlMail(emailEmpfaengerCaptor.capture(), any(), any());
		List<String> benachrichtigteKreiskoordinatinnen = emailEmpfaengerCaptor.getAllValues().get(1);
		assertThat(benachrichtigteKreiskoordinatinnen).containsExactly("kreisKoordinator@testRadvis.com");

		verify(templateEngine, times(2)).process(any(String.class), templateContextCaptor.capture());
		Context context = templateContextCaptor.getAllValues().get(1);
		assertThat((Benutzer) context.getVariable("empfaenger")).isEqualTo(kreiskoordinatorin);

		Map<Verwaltungseinheit, Set<Benutzer>> expectedMap = new HashMap<>();
		expectedMap.put(mitRadwegeErfasserin, Set.of(radwegeErfasserin));
		assertThat(context.getVariable("verwaltungseinheitToEmailEmpfaenger")).isEqualTo(expectedMap);

		assertThat(context.getVariable("kreiskoordinatorZustaendigAberOhneRadwegeerfasserin"))
			.isEqualTo(Set.of(ohneRadwegeerfasserin));
	}

	@Test
	void starteUmsetzungsstandsabfrage_EmailAnKreiskoordinatorinnen_3KreiskoordUnterschEbenen_2massnahmen() {
		// arrange
		Verwaltungseinheit kreis = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(101L)
			.name("kreis1")
			.build();
		Verwaltungseinheit gemeinde1 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.name("gemeinde1")
			.uebergeordneteOrganisation(kreis)
			.id(111L)
			.build();
		Verwaltungseinheit gemeinde2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.name("gemeinde2")
			.uebergeordneteOrganisation(kreis)
			.id(112L)
			.build();

		Benutzer kreiskoordinatorinGemeinde1 = BenutzerTestDataProvider.kreiskoordinator(gemeinde1).id(1L)
			.vorname(Name.of("kreiskoordinatorin")).nachname(Name.of("gemeinde1"))
			.mailadresse(Mailadresse.of("kreiskoordinatorinGemeinde1@testRadvis.de"))
			.build();
		Benutzer kreiskoordinatorinGemeinde2 = BenutzerTestDataProvider.kreiskoordinator(gemeinde2).id(2L)
			.vorname(Name.of("kreiskoordinatorin")).nachname(Name.of("gemeinde2"))
			.mailadresse(Mailadresse.of("kreiskoordinatorinGemeinde2@testRadvis.de"))
			.build();
		Benutzer kreiskoordinatorinKreis = BenutzerTestDataProvider.kreiskoordinator(kreis).id(3L)
			.vorname(Name.of("kreiskoordinatorin")).nachname(Name.of("kreis"))
			.mailadresse(Mailadresse.of("kreiskoordinatorinKreis@testRadvis.de"))
			.build();
		Benutzer radwegeErfasserin1 = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(gemeinde1).id(11L)
			.vorname(Name.of("radwegeerfasserin")).nachname(Name.of("1"))
			.mailadresse(Mailadresse.of("radwegeErfasserin1@testRadvis.de"))
			.build();
		Benutzer radwegeErfasserin2 = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(gemeinde2).id(12L)
			.vorname(Name.of("radwegeerfasserin")).nachname(Name.of("2"))
			.mailadresse(Mailadresse.of("radwegeErfasserin2@testRadvis.de"))
			.build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1001L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(gemeinde1)
			.build();
		Massnahme betroffeneMassnahme2 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1002L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(gemeinde2)
			.build();

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme1)))
				.thenReturn(List.of(radwegeErfasserin1));
		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme2)))
				.thenReturn(List.of(radwegeErfasserin2));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(gemeinde1)))
			.thenReturn(List.of(kreiskoordinatorinGemeinde1));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(gemeinde2)))
			.thenReturn(List.of(kreiskoordinatorinGemeinde2));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(kreis)))
			.thenReturn(List.of(kreiskoordinatorinKreis));

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(
			eq(List.of(betroffeneMassnahme1.getId(), betroffeneMassnahme2.getId())))).thenReturn(
				Stream.of(betroffeneMassnahme1, betroffeneMassnahme2));

		// act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(
			List.of(betroffeneMassnahme1.getId(), betroffeneMassnahme2.getId())
		);
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// assert
		verify(mailService, times(5)).sendHtmlMail(emailEmpfaengerCaptor.capture(), any(), any());
		List<String> benachrichtigteKreiskoordinatinnen = emailEmpfaengerCaptor.getAllValues().stream()
			.flatMap(List::stream).collect(Collectors.toList());
		assertThat(benachrichtigteKreiskoordinatinnen).containsExactlyInAnyOrder(
			"radwegeErfasserin1@testRadvis.de",
			"radwegeErfasserin2@testRadvis.de",
			"kreiskoordinatorinGemeinde1@testRadvis.de",
			"kreiskoordinatorinGemeinde2@testRadvis.de",
			"kreiskoordinatorinKreis@testRadvis.de");

		verify(templateEngine, times(5)).process(any(String.class), templateContextCaptor.capture());

		Context context1 = templateContextCaptor.getAllValues().get(2);
		assertThat((Benutzer) context1.getVariable("empfaenger")).isEqualTo(kreiskoordinatorinGemeinde1);
		Map<Verwaltungseinheit, Set<Benutzer>> expectedMap1 = new HashMap<>();
		expectedMap1.put(gemeinde1, Set.of(radwegeErfasserin1));
		assertThat(context1.getVariable("verwaltungseinheitToEmailEmpfaenger")).isEqualTo(expectedMap1);
		assertThat(context1.getVariable("kreiskoordinatorZustaendigAberOhneRadwegeerfasserin")).isEqualTo(Set.of());

		Context context2 = templateContextCaptor.getAllValues().get(3);
		assertThat((Benutzer) context2.getVariable("empfaenger")).isEqualTo(kreiskoordinatorinGemeinde2);
		Map<Verwaltungseinheit, Set<Benutzer>> expectedMap2 = new HashMap<>();
		expectedMap2.put(gemeinde2, Set.of(radwegeErfasserin2));
		assertThat(context2.getVariable("verwaltungseinheitToEmailEmpfaenger")).isEqualTo(expectedMap2);
		assertThat(context2.getVariable("kreiskoordinatorZustaendigAberOhneRadwegeerfasserin")).isEqualTo(Set.of());

		Context context3 = templateContextCaptor.getAllValues().get(4);
		assertThat((Benutzer) context3.getVariable("empfaenger")).isEqualTo(kreiskoordinatorinKreis);
		Map<Verwaltungseinheit, Set<Benutzer>> expectedMap3 = new HashMap<>();
		expectedMap3.put(gemeinde1, Set.of(radwegeErfasserin1));
		expectedMap3.put(gemeinde2, Set.of(radwegeErfasserin2));
		assertThat(context3.getVariable("verwaltungseinheitToEmailEmpfaenger")).isEqualTo(expectedMap3);
		assertThat(context3.getVariable("kreiskoordinatorZustaendigAberOhneRadwegeerfasserin")).isEqualTo(Set.of());
	}

	@Test
	void starteUmsetzungsstandsabfrage_keinMailversandt_anNichtVerknuepfte() {
		// arrange
		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("verwaltungseinheit")
			.id(100L)
			.build();
		Verwaltungseinheit verwaltungseinheitOhneBenutzerMitMassnahme = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("verwaltungseinheitOhneBenutzerMitMassnahme")

			.id(101L)
			.build();
		Verwaltungseinheit verwaltungseinheitMitBenutzerOhneMassnahme = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("verwaltungseinheitMitBenutzerOhneMassnahme")

			.id(103L)
			.build();
		Benutzer radwegeErfasserin = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(verwaltungseinheit).id(1L)
			.vorname(Name.of("radwegeErfasserin"))
			.build();
		Benutzer radwegeErfasserinOhneMassnahme = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(
			verwaltungseinheitMitBenutzerOhneMassnahme).id(2L)
			.vorname(Name.of("radwegeErfasserinOhneMassnahme"))
			.build();
		Benutzer kreiskoordinatorin = BenutzerTestDataProvider.kreiskoordinator(verwaltungseinheit).id(3L)
			.vorname(Name.of("kreiskoordinatorin"))
			.build();
		Benutzer kreiskoordinatorinOhneMassnahme = BenutzerTestDataProvider.kreiskoordinator(
			verwaltungseinheitMitBenutzerOhneMassnahme).id(4L)
			.vorname(Name.of("kreiskoordinatorinOhneMassnahme"))
			.build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(verwaltungseinheit)
			.build();

		Massnahme betroffeneMassnahme2 = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(verwaltungseinheitOhneBenutzerMitMassnahme)
			.build();

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme1)))
				.thenReturn(List.of(radwegeErfasserin));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(verwaltungseinheit)))
			.thenReturn(List.of(kreiskoordinatorin));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(
			eq(verwaltungseinheitMitBenutzerOhneMassnahme)))
				.thenReturn(List.of(kreiskoordinatorinOhneMassnahme));

		when(verwaltungseinheitService.istUebergeordnet(any(), any())).thenReturn(true);

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(
			eq(List.of(betroffeneMassnahme1.getId(), betroffeneMassnahme2.getId())))).thenReturn(
				Stream.of(betroffeneMassnahme1, betroffeneMassnahme2));

		// act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(
			List.of(betroffeneMassnahme1.getId(), betroffeneMassnahme2.getId()));
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// assert
		verify(mailService, times(2)).sendHtmlMail(emailEmpfaengerCaptor.capture(), any(), any());
		List<String> benachrichtigteKreiskoordinatinnen = emailEmpfaengerCaptor.getAllValues().get(1);
		assertThat(benachrichtigteKreiskoordinatinnen).containsExactly("kreisKoordinator@testRadvis.com");

		verify(templateEngine, times(2)).process(any(String.class), templateContextCaptor.capture());
		Context context = templateContextCaptor.getAllValues().get(1);
		assertThat((Benutzer) context.getVariable("empfaenger")).isEqualTo(kreiskoordinatorin);

		Map<Verwaltungseinheit, Set<Benutzer>> expectedMap = new HashMap<>();
		expectedMap.put(verwaltungseinheit, Set.of(radwegeErfasserin));
		assertThat(context.getVariable("verwaltungseinheitToEmailEmpfaenger")).isEqualTo(expectedMap);

		assertThat(context.getVariable("kreiskoordinatorZustaendigAberOhneRadwegeerfasserin"))
			.isEqualTo(Set.of(verwaltungseinheitOhneBenutzerMitMassnahme));
	}

	@Test
	void starteUmsetzungsstandsabfrage_EmailAnKreiskoordinatorinnen_UeberKreisIgnoriert() {
		// arrange
		Verwaltungseinheit bundesland = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.BUNDESLAND)
			.id(101L)
			.name("bundesland")
			.build();
		Verwaltungseinheit kreis = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK)
			.id(112L)
			.name("kreis")
			.uebergeordneteOrganisation(bundesland)
			.build();
		Verwaltungseinheit gemeinde = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.name("gemeinde")
			.uebergeordneteOrganisation(kreis)
			.id(113L)
			.build();

		Benutzer kreiskoordinatorinBundesland = BenutzerTestDataProvider.kreiskoordinator(bundesland).id(1L)
			.vorname(Name.of("kreiskoordinatorin")).nachname(Name.of("bundesland"))
			.mailadresse(Mailadresse.of("kreiskoordinatorinBundesland@testRadvis.de"))
			.build();
		Benutzer kreiskoordinatorinKreis = BenutzerTestDataProvider.kreiskoordinator(kreis).id(2L)
			.vorname(Name.of("kreiskoordinatorin")).nachname(Name.of("kreis"))
			.mailadresse(Mailadresse.of("kreiskoordinatorinKreis@testRadvis.de"))
			.build();
		Benutzer kreiskoordinatorinGemeinde = BenutzerTestDataProvider.kreiskoordinator(gemeinde).id(3L)
			.vorname(Name.of("kreiskoordinatorin")).nachname(Name.of("gemeinde"))
			.mailadresse(Mailadresse.of("kreiskoordinatorinGemeinde@testRadvis.de"))
			.build();
		Benutzer radwegeErfasserin1 = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(gemeinde).id(11L)
			.vorname(Name.of("radwegeerfasserin")).nachname(Name.of("1"))
			.mailadresse(Mailadresse.of("radwegeErfasserin1@testRadvis.de"))
			.build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1001L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(gemeinde)
			.build();

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme1)))
				.thenReturn(List.of(radwegeErfasserin1));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(gemeinde)))
			.thenReturn(List.of(kreiskoordinatorinGemeinde));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(kreis)))
			.thenReturn(List.of(kreiskoordinatorinKreis));
		when(massnahmenZustaendigkeitsService.getZustaendigeKreiskoordinatoren(eq(bundesland)))
			.thenReturn(List.of(kreiskoordinatorinBundesland));

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(
			eq(List.of(betroffeneMassnahme1.getId())))).thenReturn(Stream.of(betroffeneMassnahme1));

		// act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(
			List.of(betroffeneMassnahme1.getId())
		);
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// assert
		verify(mailService, times(2)).sendHtmlMail(emailEmpfaengerCaptor.capture(), any(), any());
		List<String> benachrichtigteKreiskoordinatinnen = emailEmpfaengerCaptor.getAllValues().stream()
			.flatMap(List::stream).collect(Collectors.toList());
		assertThat(benachrichtigteKreiskoordinatinnen).containsExactlyInAnyOrder(
			"radwegeErfasserin1@testRadvis.de",
			"kreiskoordinatorinGemeinde@testRadvis.de");
	}

	@Test
	void starteUmsetzungsstandsabfrage_versendeNurEineMailProBenutzer_1Zustaendiger() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();
		Benutzer benutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation).build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();
		Massnahme betroffeneMassnahme2 = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
				or(eq(betroffeneMassnahme1), eq(betroffeneMassnahme2))))
					.thenReturn(List.of(benutzer));

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(
			eq(List.of(betroffeneMassnahme1.getId(), betroffeneMassnahme2.getId()))))
				.thenReturn(Stream.of(betroffeneMassnahme1, betroffeneMassnahme2));

		// act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(
			List.of(betroffeneMassnahme1.getId(), betroffeneMassnahme2.getId()));
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// assert
		verify(mailService, times(1)).sendHtmlMail(emailEmpfaengerCaptor.capture(), any(), any());
		List<String> empfaenger = emailEmpfaengerCaptor.getValue();
		assertThat(empfaenger).containsExactly("radwegeErfasserinKommuneKreis@testRadvis.com");
	}

	@Test
	void starteUmsetzungsstandsabfrage_versendeNurEineMailProBenutzer_2Zustaendiger() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();

		Benutzer benutzer1 = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation)
			.id(1L)
			.vorname(Name.of("BenutzerA")).mailadresse(
				Mailadresse.of("benutzerA@testRadvis.com")).build();
		Benutzer benutzer2 = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation)
			.id(2L)
			.vorname(Name.of("BenutzerB")).mailadresse(
				Mailadresse.of("benutzerB@testRadvis.com")).build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();
		Massnahme betroffeneMassnahme2 = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
				or(eq(betroffeneMassnahme1), eq(betroffeneMassnahme2))))
					.thenReturn(List.of(benutzer1, benutzer2));

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(eq(List.of(1L, 2L))))
			.thenReturn(Stream.of(betroffeneMassnahme1, betroffeneMassnahme2));

		// act
		List<Massnahme> massnahmen = umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(List.of(1L, 2L));
		umsetzungsstandabfrageService.benachrichtigeNutzer(massnahmen);

		// assert
		verify(mailService, times(2)).sendHtmlMail(emailEmpfaengerCaptor.capture(), any(), any());
		List<List<String>> empfaenger = emailEmpfaengerCaptor.getAllValues();
		assertThat(empfaenger.get(0)).containsExactly("benutzerA@testRadvis.com");
		assertThat(empfaenger.get(1)).containsExactly("benutzerB@testRadvis.com");
	}

	@Test
	void starteUmsetzungsstandsabfrage_keineBenutzerKeineMail() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();
		Massnahme betroffeneMassnahme2 = MassnahmeTestDataProvider.withDefaultValues()
			.id(2L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(eq(List.of(1L, 2L))))
			.thenReturn(Stream.of(betroffeneMassnahme1, betroffeneMassnahme2));

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(
				or(eq(betroffeneMassnahme1), eq(betroffeneMassnahme2))))
					.thenReturn(Collections.emptyList());

		// act
		umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(List.of(1L, 2L));

		// assert
		verify(mailService, never()).sendMail(any(), any(), any());
	}

	@Test
	void starteUmsetzungsstandsabfrage_EmpfaengerkreisBestimmen_keineRadwegeErfasserinInOrga() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();
		Benutzer nichtRadwegeErfasserin = BenutzerTestDataProvider.kreiskoordinator(organisation).build();

		Massnahme betroffeneMassnahme1 = MassnahmeTestDataProvider.withDefaultValues()
			.id(1L)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.zustaendiger(organisation)
			.build();
		this.setzeUmsetzungsstandStatusAufAktualisiert(betroffeneMassnahme1, nichtRadwegeErfasserin);

		when(
			massnahmenZustaendigkeitsService.getZustaendigeBarbeiterVonUmsetzungsstandabfrage(eq(betroffeneMassnahme1)))
				.thenReturn(List.of(nichtRadwegeErfasserin));

		when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(
			eq(List.of(betroffeneMassnahme1.getId())))).thenReturn(
				Stream.of(betroffeneMassnahme1));

		// act
		umsetzungsstandabfrageService.starteUmsetzungsstandsabfrage(List.of(betroffeneMassnahme1.getId()));

		// assert
		verify(mailService, never()).sendMail(any(), any(), any());
		verify(massnahmeRepository).findAllByIdInAndGeloeschtFalse((eq(List.of(betroffeneMassnahme1.getId()))));
	}

	@Test
	void radvisLinkFuerOrganisation() {
		// arrange
		when(commonConfigurationProperties.getBasisUrl()).thenReturn("https://abc.de");
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Name mit Leerzeichen")
			.build();

		// act
		String radvisLink = umsetzungsstandabfrageService.getRadvisLink(organisation);

		// assert
		assertThat(radvisLink).isEqualTo("https://abc.de/viewer?infrastrukturen=massnahmen&tabellenVisible=true"
			+ "&filter_massnahmen=umsetzungsstandStatus:Aktualisierung%2520angefordert,zustaendiger:Name%2520mit%2520Leerzeichen");
	}

	@Test
	void beantwortungsFristIstJetztPlusXWochen() {
		// arrange
		LocalDateTime now = LocalDateTime.of(2022, 7, 4, 0, 0);
		int frist = 8;

		// act
		String achtWochenSpaeterUndFormattiert = umsetzungsstandabfrageService.bestimmeUndFormattiereBeantwortungsfrist(
			now, frist);

		// assert
		assertThat(achtWochenSpaeterUndFormattiert).isEqualTo("29.08.2022");
	}

	@Test
	void testeGetMassnahmenStream() {
		List<Long> allIndices = new ArrayList<>();
		List<Massnahme> alle = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			List<Massnahme> mlist = new ArrayList<>();
			for (int j = i * postgisConfigurationProperties.getArgumentLimit(); j < (i + 1)
				* postgisConfigurationProperties.getArgumentLimit(); j++) {
				Massnahme m = MassnahmeTestDataProvider.withDefaultValues().id((long) j).build();
				mlist.add(m);
				allIndices.add((long) j);
			}
			when(massnahmeRepository.findAllByIdInAndGeloeschtFalse(eq(
				mlist.stream().map(AbstractEntity::getId).collect(Collectors.toList())))).thenReturn(mlist.stream());
			alle.addAll(mlist);
		}

		// act
		List<Massnahme> result = umsetzungsstandabfrageService.getMassnahmenStream(allIndices)
			.collect(Collectors.toList());

		assertThat(alle.size()).isEqualTo(3 * postgisConfigurationProperties.getArgumentLimit());
		assertThat(result.size()).isEqualTo(3 * postgisConfigurationProperties.getArgumentLimit());
		assertThat(result).containsExactlyElementsOf(alle);
	}

	private void setzeUmsetzungsstandStatusAufAktualisiert(Massnahme massnahme, Benutzer benutzer) {
		massnahme.getUmsetzungsstand().ifPresent(
			us -> us.update(
				true,
				LocalDateTime.now(),
				benutzer,
				null,
				PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH,
				null,
				null,
				null,
				null, massnahme.getUmsetzungsstatus()));
	}
}
