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

import static de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider.benutzerDBListViewComparator;
import static de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider.getDbListView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerDBListView;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerIstNichtRegistriertException;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.OptimisticLockException;

class BenutzerServiceTest {

	@Mock
	VerwaltungseinheitService verwaltungseinheitService;

	@Mock
	BenutzerRepository benutzerRepository;

	BenutzerService benutzerService;

	@Mock
	MailService mailService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		benutzerService = new BenutzerService(
			benutzerRepository,
			verwaltungseinheitService,
			"technischerBenutzerServiceBwId",
			"basisUrl",
			mailService);
	}

	@Test
	void testGetBenutzerFormodification_versionVeraltet() {
		// arrange
		Long id = 1L;
		Long version = 2L;

		Benutzer benutzer = BenutzerTestDataProvider
			.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.id(id)
			.version(version)
			.build();

		when(benutzerRepository.findById(id)).thenReturn(Optional.of(benutzer));

		// act + assert
		assertThrows(OptimisticLockException.class, () -> benutzerService.getBenutzerForModifikation(id, 1L));
	}

	@Test
	void testbenutzerHatEinesDerRechte() {
		// arrange

		Benutzer benutzer = BenutzerTestDataProvider.externerDienstleister(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(103L).name("Gemeinde")
				.organisationsArt(OrganisationsArt.GEMEINDE)
				.build())
			.rollen(Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN, Rolle.EXTERNER_DIENSTLEISTER))
			.build();

		// act + assert
		assertThat(benutzerService.benutzerHatEinesDerRechte(
			benutzer,
			List.of(
				Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN,
				Recht.RADNETZ_ROUTENVERLEGUNGEN,
				Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT))).isTrue();

		assertThat(benutzerService.benutzerHatEinesDerRechte(
			benutzer, List.of(Recht.ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN))).isFalse();

		assertThat(benutzerService.benutzerHatEinesDerRechte(
			benutzer,
			List.of(
				Recht.RADVERKEHRSBEAUFTRAGTER,
				Recht.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN,
				Recht.KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER,
				Recht.BETRACHTER_EXTERNER_DIENSTLEISTER))).isFalse();

		assertThat(benutzerService.benutzerHatEinesDerRechte(benutzer, List.of(Recht.ALLE_ROLLEN))).isFalse();
	}

	@Test
	void testErmittleVergaberechteFuerRolle_RADVIS_ADMINISTRATOR() {
		assertThat(
			benutzerService.ermittleVergaberechteFuerRolle(Rolle.RADVIS_ADMINISTRATOR))
				.containsExactlyInAnyOrder(Recht.ALLE_ROLLEN);
	}

	@Test
	void testErmittleVergaberechteFuerRolle_KREISKOORDINATOREN() {
		assertThat(
			benutzerService.ermittleVergaberechteFuerRolle(Rolle.KREISKOORDINATOREN))
				.containsExactlyInAnyOrder(
					Recht.ALLE_ROLLEN,
					Recht.KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER);
	}

	@Test
	void testErmittleVergaberechteFuerRolle_RADWEGE_ERFASSERIN_KOMMUNE_KREIS() {
		assertThat(
			benutzerService.ermittleVergaberechteFuerRolle(Rolle.RADWEGE_ERFASSERIN))
				.containsExactlyInAnyOrder(
					Recht.ALLE_ROLLEN,
					Recht.KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER);
	}

	@Test
	void testErmittleVergaberechteFuerRolle_RADNETZ_ERFASSERIN_REGIERUNGSBEZIRK() {
		assertThat(
			benutzerService.ermittleVergaberechteFuerRolle(Rolle.RADVERKEHRSBEAUFTRAGTER))
				.containsExactlyInAnyOrder(
					Recht.ALLE_ROLLEN,
					Recht.RADVERKEHRSBEAUFTRAGTER);
	}

	@Test
	void testErmittleVergaberechteFuerRolle_BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN() {
		assertThat(
			benutzerService.ermittleVergaberechteFuerRolle(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN))
				.containsExactlyInAnyOrder(
					Recht.ALLE_ROLLEN,
					Recht.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN);
	}

	@Test
	void testErmittleVergaberechteFuerRolle_RADNETZ_QUALITAETSSICHERIN() {
		assertThat(
			benutzerService.ermittleVergaberechteFuerRolle(Rolle.RADNETZ_QUALITAETSSICHERIN))
				.containsExactlyInAnyOrder(Recht.ALLE_ROLLEN);
	}

	@Test
	void testErmittleVergaberechteFuerRolle_BETRACHTERIN() {
		assertThat(
			benutzerService.ermittleVergaberechteFuerRolle(Rolle.RADVIS_BETRACHTER))
				.containsExactlyInAnyOrder(
					Recht.ALLE_ROLLEN,
					Recht.BETRACHTER_EXTERNER_DIENSTLEISTER);
	}

	@Test
	void testErmittleVergaberechteFuerRolle_EXTERNER_DIENSTLEISTER() {
		assertThat(
			benutzerService.ermittleVergaberechteFuerRolle(Rolle.EXTERNER_DIENSTLEISTER))
				.containsExactlyInAnyOrder(
					Recht.ALLE_ROLLEN,
					Recht.BETRACHTER_EXTERNER_DIENSTLEISTER);
	}

	@Test
	void testFindeZustaendigeAdmins_inEigenerOrganisation_findetUndBenachrichtigtNurEigene_eskaliertNichtNachOben() {
		// arrange
		Verwaltungseinheit zustaendigeLandesOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(1L).build();
		Verwaltungseinheit andereLandesOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(2L).build();
		Verwaltungseinheit zustaendigeKreisOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(3L).uebergeordneteOrganisation(zustaendigeLandesOrganisation)
			.build();
		Verwaltungseinheit andereKreisOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(4L).uebergeordneteOrganisation(andereLandesOrganisation)
			.build();
		Verwaltungseinheit zustaendigeGemeindeOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Gemeindeorganisation")
			.organisationsArt(OrganisationsArt.GEMEINDE).id(5L).uebergeordneteOrganisation(zustaendigeKreisOrganisation)
			.build();

		// benutzer
		Benutzer lowbobBenutzer = new Benutzer(Name.of("Testus"), Name.of("Testperson"), BenutzerStatus.INAKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("meinemail@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130000"), Set.of(Rolle.RADWEGE_ERFASSERIN));
		// zuständige Admins
		Benutzer radvisAdmin = new Benutzer(Name.of("Radvis"), Name.of("Admin"), BenutzerStatus.AKTIV,
			zustaendigeLandesOrganisation, Mailadresse.of("admin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130001"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		Benutzer kreiskoodinator = new Benutzer(Name.of("Kreis"), Name.of("Koordinator"), BenutzerStatus.AKTIV,
			zustaendigeKreisOrganisation, Mailadresse.of("kreiskoordinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130002"), Set.of(Rolle.KREISKOORDINATOREN));
		Benutzer radnetzErfasserInRegierungsbezirk = new Benutzer(Name.of("Radnetz"), Name.of("Erfasser"),
			BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("erfasser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"), Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER));
		Benutzer bearbeiterVMadministration = new Benutzer(Name.of("bearbeiterVM"), Name.of("Radnetzadministrator"),
			BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("radnetzadmin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130004"),
			Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN));
		// nicht zuständige Admins
		Benutzer nichtZustaendigKreiskoodinator = new Benutzer(Name.of("anderer"), Name.of("kreiskoodrinator"),
			BenutzerStatus.AKTIV,
			andereKreisOrganisation, Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130005"), Set.of(Rolle.KREISKOORDINATOREN));
		Benutzer nichtZustaendigradneterfasserRegierungsbezirk = new Benutzer(Name.of("anderer"),
			Name.of("kreiskoodrinator"), BenutzerStatus.AKTIV,
			andereLandesOrganisation, Mailadresse.of("anderer.erfasser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130006"), Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER));
		// nicht zuständig sosntiges
		Benutzer poweruser = new Benutzer(Name.of("Max"), Name.of("Power"), BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("poweruser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130010"), Set.of(Rolle.EXTERNER_DIENSTLEISTER));

		when(benutzerRepository.findByRollenAndStatus(Rolle.RADVIS_ADMINISTRATOR, BenutzerStatus.AKTIV))
			.thenReturn(List.of(radvisAdmin));

		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeLandesOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(radvisAdmin));
		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeKreisOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(kreiskoodinator));
		when(
			benutzerRepository.findByOrganisationAndStatus(zustaendigeGemeindeOrganisation, BenutzerStatus.AKTIV))
				.thenReturn(List.of(radnetzErfasserInRegierungsbezirk, bearbeiterVMadministration, poweruser));
		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeGemeindeOrganisation,
			BenutzerStatus.INAKTIV))
				.thenReturn(List.of(lowbobBenutzer));
		when(benutzerRepository.findByOrganisationAndStatus(andereKreisOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(nichtZustaendigKreiskoodinator));
		when(benutzerRepository.findByOrganisationAndStatus(andereLandesOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(nichtZustaendigradneterfasserRegierungsbezirk));

		// act
		Iterable<Benutzer> zustaendigeBenutzer = benutzerService.getAlleZustaendigenBenutzer(lowbobBenutzer);

		// assert
		assertThat(zustaendigeBenutzer)
			.extracting("mailadresse")
			.containsExactlyInAnyOrder(
				Mailadresse.of("erfasser@testRadvis.de"))
			.doesNotContain(
				// Diese Admins sind nicht für die Organisation Zuständig
				Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
				Mailadresse.of("anderer.erfasser@testRadvis.de"))
			.doesNotContain(
				// Diese Admins sollten nicht benachrichtigt werden, weil es Admins auf einer unteren Ebene gibt, die
				// zuständig wären
				Mailadresse.of("admin@testRadvis.de"),
				Mailadresse.of("kreiskoordinator@testRadvis.de"))
			.doesNotContain(
				// Diese User sind in der richtigen Organisation, haben aber keine Adminrechte (einer davoin ist der
				// User selbst)
				Mailadresse.of("meinemail@testRadvis.de"),
				Mailadresse.of("poweruser@testRadvis.de"),
				Mailadresse.of("radnetzadmin@testRadvis.de"));
	}

	@Test
	void testFindeZustaendigeAdmins_inEigenerOrganisation_findetNichtUndBenachrichtigtEineEbeneHoeher() {
		// arrange
		Verwaltungseinheit zustaendigeLandesOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(1L).build();
		Verwaltungseinheit andereLandesOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(2L).build();
		Verwaltungseinheit zustaendigeKreisOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(3L).uebergeordneteOrganisation(zustaendigeLandesOrganisation)
			.build();
		Verwaltungseinheit andereKreisOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(4L).uebergeordneteOrganisation(andereLandesOrganisation)
			.build();
		Verwaltungseinheit zustaendigeGemeindeOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Gemeindeorganisation")
			.organisationsArt(OrganisationsArt.GEMEINDE).id(5L).uebergeordneteOrganisation(zustaendigeKreisOrganisation)
			.build();

		// benutzer
		Benutzer lowbobBenutzer = new Benutzer(Name.of("Testus"), Name.of("Testperson"), BenutzerStatus.INAKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("meinemail@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130000"), Set.of(Rolle.RADWEGE_ERFASSERIN));
		// zuständige Admins
		Benutzer radvisAdmin = new Benutzer(Name.of("Radvis"), Name.of("Admin"), BenutzerStatus.AKTIV,
			zustaendigeLandesOrganisation, Mailadresse.of("admin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130001"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		Benutzer kreiskoodinator = new Benutzer(Name.of("Kreis"), Name.of("Koordinator"), BenutzerStatus.AKTIV,
			zustaendigeKreisOrganisation, Mailadresse.of("kreiskoordinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130002"), Set.of(Rolle.KREISKOORDINATOREN));
		// nicht zuständige Admins
		Benutzer nichtZustaendigKreiskoodinator = new Benutzer(Name.of("anderer"), Name.of("kreiskoodrinator"),
			BenutzerStatus.AKTIV,
			andereKreisOrganisation, Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130005"), Set.of(Rolle.KREISKOORDINATOREN));
		Benutzer nichtZustaendigradneterfasserRegierungsbezirk = new Benutzer(Name.of("anderer"),
			Name.of("kreiskoodrinator"), BenutzerStatus.AKTIV,
			andereLandesOrganisation, Mailadresse.of("anderer.erfasser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130006"), Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER));
		// nicht zuständig sosntiges
		Benutzer poweruser = new Benutzer(Name.of("Max"), Name.of("Power"), BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("poweruser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130010"), Set.of(Rolle.EXTERNER_DIENSTLEISTER));

		when(benutzerRepository.findByRollenAndStatus(Rolle.RADVIS_ADMINISTRATOR, BenutzerStatus.AKTIV))
			.thenReturn(List.of(radvisAdmin));

		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeLandesOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(radvisAdmin));
		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeKreisOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(kreiskoodinator));
		when(
			benutzerRepository.findByOrganisationAndStatus(zustaendigeGemeindeOrganisation, BenutzerStatus.AKTIV))
				.thenReturn(List.of(poweruser));
		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeGemeindeOrganisation,
			BenutzerStatus.INAKTIV))
				.thenReturn(List.of(lowbobBenutzer));
		when(benutzerRepository.findByOrganisationAndStatus(andereKreisOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(nichtZustaendigKreiskoodinator));
		when(benutzerRepository.findByOrganisationAndStatus(andereLandesOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(nichtZustaendigradneterfasserRegierungsbezirk));

		// act
		Iterable<Benutzer> zustaendigeBenutzer = benutzerService.getAlleZustaendigenBenutzer(lowbobBenutzer);

		// assert
		assertThat(zustaendigeBenutzer)
			.extracting("mailadresse")
			.containsExactlyInAnyOrder(
				Mailadresse.of("kreiskoordinator@testRadvis.de"))
			.doesNotContain(
				// Diese Admins sind nicht für die Organisation Zuständig
				Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
				Mailadresse.of("anderer.erfasser@testRadvis.de"))
			.doesNotContain(
				// Diese Admins sollten nicht benachrichtigt werden, weil es Admins auf einer unteren Ebene gibt, die
				// zuständig wären
				Mailadresse.of("admin@testRadvis.de"))
			.doesNotContain(
				// Diese User sind in der richtigen Organisation, haben aber keine Adminrechte (einer davoin ist der
				// User selbst)
				Mailadresse.of("meinemail@testRadvis.de"),
				Mailadresse.of("poweruser@testRadvis.de"));
	}

	@Test
	void testFindeZustaendigeAdmins_inEigenerOrganisation_findetNichtUndBenachrichtigtZeiEbeneHoeher() {
		// arrange
		Verwaltungseinheit zustaendigeLandesOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(1L).build();
		Verwaltungseinheit andereLandesOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(2L).build();
		Verwaltungseinheit zustaendigeKreisOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(3L).uebergeordneteOrganisation(zustaendigeLandesOrganisation)
			.build();
		Verwaltungseinheit andereKreisOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(4L).uebergeordneteOrganisation(andereLandesOrganisation)
			.build();
		Verwaltungseinheit zustaendigeGemeindeOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Gemeindeorganisation")
			.organisationsArt(OrganisationsArt.GEMEINDE).id(5L).uebergeordneteOrganisation(zustaendigeKreisOrganisation)
			.build();

		// benutzer
		Benutzer lowbobBenutzer = new Benutzer(Name.of("Testus"), Name.of("Testperson"), BenutzerStatus.INAKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("meinemail@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130000"), Set.of(Rolle.RADWEGE_ERFASSERIN));
		// zuständige Admins
		Benutzer radvisAdmin = new Benutzer(Name.of("Radvis"), Name.of("Admin"), BenutzerStatus.AKTIV,
			zustaendigeLandesOrganisation, Mailadresse.of("admin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130001"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		Benutzer andererRadvisAdmin = new Benutzer(Name.of("Raaaaaadvis"), Name.of("WaaaaaAdmin"), BenutzerStatus.AKTIV,
			andereLandesOrganisation, Mailadresse.of("zweiterAdmin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130021"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		Benutzer landesVMrandetzBearbeiter = new Benutzer(Name.of("VM ARbeiter"), Name.of("Regierungsbezirk"),
			BenutzerStatus.AKTIV,
			zustaendigeLandesOrganisation, Mailadresse.of("landesVMrandetzBearbeiter@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130002"),
			Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN));
		// nicht zuständige Admins
		Benutzer nichtZustaendigKreiskoodinator = new Benutzer(Name.of("anderer"), Name.of("kreiskoodrinator"),
			BenutzerStatus.AKTIV,
			andereKreisOrganisation, Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130005"), Set.of(Rolle.KREISKOORDINATOREN));
		Benutzer nichtZustaendigradneterfasserRegierungsbezirk = new Benutzer(Name.of("anderer"),
			Name.of("kreiskoodrinator"), BenutzerStatus.AKTIV,
			andereLandesOrganisation, Mailadresse.of("anderer.erfasser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130006"), Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER));
		// nicht zuständig sosntiges
		Benutzer poweruser = new Benutzer(Name.of("Max"), Name.of("Power"), BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("poweruser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130010"), Set.of(Rolle.EXTERNER_DIENSTLEISTER));

		when(benutzerRepository.findByRollenAndStatus(Rolle.RADVIS_ADMINISTRATOR, BenutzerStatus.AKTIV))
			.thenReturn(List.of(radvisAdmin, andererRadvisAdmin));

		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeLandesOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(radvisAdmin, landesVMrandetzBearbeiter));
		when(
			benutzerRepository.findByOrganisationAndStatus(zustaendigeGemeindeOrganisation, BenutzerStatus.AKTIV))
				.thenReturn(List.of(poweruser));
		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeGemeindeOrganisation,
			BenutzerStatus.INAKTIV))
				.thenReturn(List.of(lowbobBenutzer));
		when(benutzerRepository.findByOrganisationAndStatus(andereKreisOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(nichtZustaendigKreiskoodinator));
		when(benutzerRepository.findByOrganisationAndStatus(andereLandesOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(andererRadvisAdmin, nichtZustaendigradneterfasserRegierungsbezirk));

		// act
		Iterable<Benutzer> zustaendigeBenutzer = benutzerService.getAlleZustaendigenBenutzer(lowbobBenutzer);

		// assert
		assertThat(zustaendigeBenutzer)
			.extracting("mailadresse")
			.containsExactlyInAnyOrder(
				Mailadresse.of("admin@testRadvis.de"))
			.doesNotContain(
				// Diese Admins sind nicht für die Organisation Zuständig
				Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
				Mailadresse.of("anderer.erfasser@testRadvis.de"))
			.doesNotContain(
				// Diese Admins sollten nicht benachrichtigt werden, weil es Admins auf einer unteren Ebene gibt, die
				// zuständig wären
				Mailadresse.of("zweiterAdmin@testRadvis.de"))
			.doesNotContain(
				// Diese User sind in der richtigen Organisation, haben aber keine Adminrechte (einer davoin ist der
				// User selbst)
				Mailadresse.of("meinemail@testRadvis.de"),
				Mailadresse.of("landesVMrandetzBearbeiter@testRadvis.de"),
				Mailadresse.of("poweruser@testRadvis.de"));
	}

	@Test
	void testFindeZustaendigeAdmins_niemandZustaendig_zentraladminWirdBenachrichtigt() {
		// arrange
		Verwaltungseinheit zustaendigeLandesOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(1L).build();
		Verwaltungseinheit andereLandesOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(2L).build();
		Verwaltungseinheit zustaendigeKreisOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(3L).uebergeordneteOrganisation(zustaendigeLandesOrganisation)
			.build();
		Verwaltungseinheit andereKreisOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(4L).uebergeordneteOrganisation(andereLandesOrganisation)
			.build();
		Verwaltungseinheit zustaendigeGemeindeOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Gemeindeorganisation")
			.organisationsArt(OrganisationsArt.GEMEINDE).id(5L).uebergeordneteOrganisation(zustaendigeKreisOrganisation)
			.build();

		// benutzer
		Benutzer lowbobBenutzer = new Benutzer(Name.of("Testus"), Name.of("Testperson"), BenutzerStatus.INAKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("meinemail@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130000"), Set.of(Rolle.RADWEGE_ERFASSERIN));
		// zuständige Admins
		Benutzer andererRadvisAdmin = new Benutzer(Name.of("Raaaaaadvis"), Name.of("WaaaaaAdmin"), BenutzerStatus.AKTIV,
			andereLandesOrganisation, Mailadresse.of("zweiterAdmin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130021"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		// nicht zuständige Admins
		Benutzer nichtZustaendigKreiskoodinator = new Benutzer(Name.of("anderer"), Name.of("kreiskoodrinator"),
			BenutzerStatus.AKTIV,
			andereKreisOrganisation, Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130005"), Set.of(Rolle.KREISKOORDINATOREN));
		Benutzer nichtZustaendigradneterfasserRegierungsbezirk = new Benutzer(Name.of("anderer"),
			Name.of("kreiskoodrinator"), BenutzerStatus.AKTIV,
			andereLandesOrganisation, Mailadresse.of("anderer.erfasser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130006"), Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER));
		// nicht zuständig sosntiges
		Benutzer poweruser = new Benutzer(Name.of("Max"), Name.of("Power"), BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("poweruser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130010"), Set.of(Rolle.EXTERNER_DIENSTLEISTER));

		when(benutzerRepository.findByRollenAndStatus(Rolle.RADVIS_ADMINISTRATOR, BenutzerStatus.AKTIV))
			.thenReturn(List.of(andererRadvisAdmin));

		when(
			benutzerRepository.findByOrganisationAndStatus(zustaendigeGemeindeOrganisation, BenutzerStatus.AKTIV))
				.thenReturn(List.of(poweruser));
		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeGemeindeOrganisation,
			BenutzerStatus.INAKTIV))
				.thenReturn(List.of(lowbobBenutzer));
		when(benutzerRepository.findByOrganisationAndStatus(andereKreisOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(nichtZustaendigKreiskoodinator));
		when(benutzerRepository.findByOrganisationAndStatus(andereLandesOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(andererRadvisAdmin, nichtZustaendigradneterfasserRegierungsbezirk));

		// act
		Iterable<Benutzer> zustaendigeBenutzer = benutzerService.getAlleZustaendigenBenutzer(lowbobBenutzer);

		// assert
		assertThat(zustaendigeBenutzer)
			.extracting("mailadresse")
			.containsExactlyInAnyOrder(
				Mailadresse.of("zweiterAdmin@testRadvis.de"))
			.doesNotContain(
				// Diese Admins sind nicht für die Organisation Zuständig
				Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
				Mailadresse.of("anderer.erfasser@testRadvis.de"))
			.doesNotContain(
				// Diese User sind in der richtigen Organisation, haben aber keine Adminrechte (einer davoin ist der
				// User selbst)
				Mailadresse.of("meinemail@testRadvis.de"),
				Mailadresse.of("poweruser@testRadvis.de"));
	}

	@Test
	void testFindeZustaendigeAdmins_neuerZentraladmin() {
		// arrange
		Verwaltungseinheit zustaendigeLandesOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(1L).build();
		Verwaltungseinheit andereLandesOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(2L).build();
		Verwaltungseinheit zustaendigeKreisOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(3L).uebergeordneteOrganisation(zustaendigeLandesOrganisation)
			.build();
		Verwaltungseinheit andereKreisOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(4L).uebergeordneteOrganisation(andereLandesOrganisation)
			.build();
		Verwaltungseinheit zustaendigeGemeindeOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Gemeindeorganisation")
			.organisationsArt(OrganisationsArt.GEMEINDE).id(5L).uebergeordneteOrganisation(zustaendigeKreisOrganisation)
			.build();

		// benutzer
		Benutzer neuerAdmin = new Benutzer(Name.of("Testus"), Name.of("Testperson"), BenutzerStatus.INAKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("meinemail@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130000"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		// zuständige Admins
		Benutzer radvisAdmin = new Benutzer(Name.of("Radvis"), Name.of("Admin"), BenutzerStatus.AKTIV,
			zustaendigeLandesOrganisation, Mailadresse.of("admin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130001"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		Benutzer andererRadvisAdmin = new Benutzer(Name.of("Raaaaaadvis"), Name.of("WaaaaaAdmin"), BenutzerStatus.AKTIV,
			andereLandesOrganisation, Mailadresse.of("zweiterAdmin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130021"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		// nicht zuständige Admins
		Benutzer kreiskoodinator = new Benutzer(Name.of("Kreis"), Name.of("Koordinator"), BenutzerStatus.AKTIV,
			zustaendigeKreisOrganisation, Mailadresse.of("kreiskoordinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130002"), Set.of(Rolle.KREISKOORDINATOREN));
		Benutzer radneterfasserRegierungsbezirk = new Benutzer(Name.of("Radnetz"), Name.of("Erfasser"),
			BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("erfasser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"), Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER));
		Benutzer bearbeiterVMadministration = new Benutzer(Name.of("bearbeiterVM"), Name.of("Radnetzadministrator"),
			BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("radnetzadmin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130004"),
			Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN));
		Benutzer nichtZustaendigKreiskoodinator = new Benutzer(Name.of("anderer"), Name.of("kreiskoodrinator"),
			BenutzerStatus.AKTIV,
			andereKreisOrganisation, Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130005"), Set.of(Rolle.KREISKOORDINATOREN));
		Benutzer nichtZustaendigradneterfasserRegierungsbezirk = new Benutzer(Name.of("anderer"),
			Name.of("kreiskoodrinator"), BenutzerStatus.AKTIV,
			andereLandesOrganisation, Mailadresse.of("anderer.erfasser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130005"), Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER));
		// nicht zuständig sosntiges
		Benutzer poweruser = new Benutzer(Name.of("Max"), Name.of("Power"), BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("poweruser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130010"), Set.of(Rolle.EXTERNER_DIENSTLEISTER));

		when(benutzerRepository.findByRollenAndStatus(Rolle.RADVIS_ADMINISTRATOR, BenutzerStatus.AKTIV))
			.thenReturn(List.of(radvisAdmin, andererRadvisAdmin));

		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeLandesOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(radvisAdmin));
		when(
			benutzerRepository.findByOrganisationAndStatus(zustaendigeGemeindeOrganisation, BenutzerStatus.AKTIV))
				.thenReturn(List.of(neuerAdmin, poweruser, radneterfasserRegierungsbezirk, bearbeiterVMadministration));
		when(benutzerRepository.findByOrganisationAndStatus(zustaendigeKreisOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(kreiskoodinator));
		when(benutzerRepository.findByOrganisationAndStatus(andereKreisOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(nichtZustaendigKreiskoodinator));
		when(benutzerRepository.findByOrganisationAndStatus(andereLandesOrganisation, BenutzerStatus.AKTIV))
			.thenReturn(List.of(andererRadvisAdmin, nichtZustaendigradneterfasserRegierungsbezirk));

		// act
		Iterable<Benutzer> potentiellZustaendigeBenutzer = benutzerService.getAlleZustaendigenBenutzer(neuerAdmin);

		// assert
		assertThat(potentiellZustaendigeBenutzer)
			.extracting("mailadresse")
			.containsExactlyInAnyOrder(
				Mailadresse.of("admin@testRadvis.de"),
				Mailadresse.of("zweiterAdmin@testRadvis.de"));
	}

	@Test
	public void testeGetAlleBenutzerByZustaendigerBenutzer_mitVerschiedenenRollen() {
		// arrange
		Verwaltungseinheit obersteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Zuständige Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).id(1L).build();
		Verwaltungseinheit mittlereOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Zuständige Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).id(3L).uebergeordneteOrganisation(obersteOrganisation)
			.build();
		Verwaltungseinheit untereOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Zuständige Gemeindeorganisation")
			.organisationsArt(OrganisationsArt.GEMEINDE).id(5L).uebergeordneteOrganisation(mittlereOrganisation)
			.build();

		// zuständige Admins
		Benutzer radvisAdmin = BenutzerTestDataProvider.defaultBenutzer().vorname(Name.of("Radvis"))
			.nachname(Name.of("Admin")).status(BenutzerStatus.AKTIV)
			.organisation(obersteOrganisation).mailadresse(Mailadresse.of("admin@testRadvis.de"))
			.serviceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130001"))
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR)).id(1L).build();
		Benutzer andererRadvisAdmin = BenutzerTestDataProvider.defaultBenutzer().vorname(Name.of("Radvis"))
			.nachname(Name.of("AdminZwei")).status(BenutzerStatus.AKTIV)
			.organisation(mittlereOrganisation).mailadresse(Mailadresse.of("zweiterAdmin@testRadvis.de"))
			.serviceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130021"))
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR)).id(2L).build();
		Benutzer kreiskoodinator = BenutzerTestDataProvider.defaultBenutzer().vorname(Name.of("Kreis"))
			.nachname(Name.of("Koordinator")).status(BenutzerStatus.AKTIV)
			.organisation(mittlereOrganisation).mailadresse(Mailadresse.of("kreiskoordinator@testRadvis.de"))
			.serviceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130002"))
			.rollen(Set.of(Rolle.KREISKOORDINATOREN)).id(3L).build();
		Benutzer benutzerEins = BenutzerTestDataProvider.defaultBenutzer().vorname(Name.of("Benutzer"))
			.nachname(Name.of("Eins")).status(BenutzerStatus.AKTIV)
			.organisation(untereOrganisation).mailadresse(Mailadresse.of("erfasser@testRadvis.de"))
			.serviceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"))
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER)).id(4L).build();
		Benutzer benutzerZwei = BenutzerTestDataProvider.defaultBenutzer().vorname(Name.of("Benutzer"))
			.nachname(Name.of("Zwei")).status(BenutzerStatus.AKTIV)
			.organisation(obersteOrganisation).mailadresse(Mailadresse.of("anderer.erfasser@testRadvis.de"))
			.serviceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130005"))
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER)).id(5L).build();

		when(benutzerRepository.findAllDBListViews()).thenReturn(
			Stream.of(radvisAdmin, andererRadvisAdmin, kreiskoodinator, benutzerEins,
				benutzerZwei).map(BenutzerTestDataProvider::getDbListView).toList());

		when(benutzerRepository.findAllDBListViewsInVerwaltungseinheitWithId(any())).thenAnswer(
			invocationOnMock -> Stream.of(radvisAdmin, andererRadvisAdmin, kreiskoodinator, benutzerEins,
				benutzerZwei).filter(b -> b.getOrganisation().getId().equals(invocationOnMock.getArgument(0)))
				.map(BenutzerTestDataProvider::getDbListView).toList());

		when(verwaltungseinheitService.findAllUntergeordnetIds(obersteOrganisation.getId()))
			.thenReturn(List.of(obersteOrganisation.getId(), untereOrganisation.getId(), mittlereOrganisation.getId()));
		when(verwaltungseinheitService.findAllUntergeordnetIds(mittlereOrganisation.getId()))
			.thenReturn(List.of(mittlereOrganisation.getId(), untereOrganisation.getId()));
		when(verwaltungseinheitService.findAllUntergeordnetIds(untereOrganisation.getId()))
			.thenReturn(List.of(untereOrganisation.getId()));

		// act
		List<BenutzerDBListView> alleBenutzerByZustaendigerBenutzerFuerRadvisAdmin = benutzerService
			.getAlleBenutzerByZustaendigerBenutzer(
				radvisAdmin);
		List<BenutzerDBListView> alleBenutzerByZustaendigerBenutzerFuerKreiskoordinator = benutzerService
			.getAlleBenutzerByZustaendigerBenutzer(
				kreiskoodinator);
		List<BenutzerDBListView> alleBenutzerByZustaendigerBenutzerFuerBenutzer = benutzerService
			.getAlleBenutzerByZustaendigerBenutzer(
				benutzerEins);

		// assert
		assertThat(alleBenutzerByZustaendigerBenutzerFuerRadvisAdmin)
			.usingElementComparator(benutzerDBListViewComparator)
			.containsExactlyInAnyOrder(
				getDbListView(radvisAdmin),
				getDbListView(andererRadvisAdmin),
				getDbListView(kreiskoodinator),
				getDbListView(benutzerEins),
				getDbListView(benutzerZwei));
		assertThat(alleBenutzerByZustaendigerBenutzerFuerKreiskoordinator)
			.usingElementComparator(benutzerDBListViewComparator)
			.containsExactlyInAnyOrder(
				getDbListView(kreiskoodinator),
				getDbListView(benutzerEins));
		assertThat(alleBenutzerByZustaendigerBenutzerFuerBenutzer).isEmpty();
	}

	@Test
	void testReaktiviereBenutzer_aendertBenutzerStatus() throws BenutzerIstNichtRegistriertException {
		// arrange
		Benutzer inaktiverBenutzer = BenutzerTestDataProvider
			.defaultBenutzer()
			.id(2L)
			.version(1L)
			.status(BenutzerStatus.INAKTIV)
			.build();

		when(benutzerRepository.findById(2L)).thenReturn(Optional.of(inaktiverBenutzer));
		when(benutzerRepository.save(any(Benutzer.class))).thenAnswer(
			invocationOnMock -> invocationOnMock.getArguments()[0]);

		// act
		Benutzer reaktivierterBenutzer = benutzerService.beantrageReaktivierungFuerBenutzer(inaktiverBenutzer);

		// assert
		assertThat(reaktivierterBenutzer.getStatus()).isEqualTo(BenutzerStatus.WARTE_AUF_FREISCHALTUNG);
		verify(mailService).sendMail(anyList(), eq("Antrag auf Reaktivierung"), anyString());
	}

	@Test
	void testAendereBenutzerstatus_setztAnderenStatusAlsAktiv_ablaufdatumWirdNull()
		throws BenutzerIstNichtRegistriertException {
		// Arrange
		List<BenutzerStatus> benutzerStatusListeOhneAktiv = Arrays.stream(BenutzerStatus.values())
			.filter(status -> status != BenutzerStatus.AKTIV)
			.toList();

		Long id = 1L;
		Long version = 2L;

		for (BenutzerStatus status : benutzerStatusListeOhneAktiv) {
			Benutzer benutzer = mock(Benutzer.class);
			when(benutzer.getVersion()).thenReturn(version);
			when(benutzerRepository.findById(id)).thenReturn(Optional.of(benutzer));

			// Act
			benutzerService.aendereBenutzerstatus(id, version, status);

			// Assert
			verify(benutzer).setAblaufdatum(null);
		}
	}

	@Test
	void testAendereBenutzerstatus_setztStatusAktiv_ablaufdatumWirdNichtGeaendert()
		throws BenutzerIstNichtRegistriertException {
		// Arrange
		Long id = 1L;
		Long version = 2L;

		Benutzer benutzer = mock(Benutzer.class);
		when(benutzer.getVersion()).thenReturn(version);
		when(benutzerRepository.findById(id)).thenReturn(Optional.of(benutzer));

		// Act
		benutzerService.aendereBenutzerstatus(id, version, BenutzerStatus.AKTIV);

		// Assert
		verify(benutzer, never()).setAblaufdatum(any());
	}
}
