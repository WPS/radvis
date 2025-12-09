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

package de.wps.radvis.backend.administration.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import de.wps.radvis.backend.administration.AdministrationConfiguration;
import de.wps.radvis.backend.administration.schnittstelle.command.SaveBenutzerCommand;
import de.wps.radvis.backend.administration.schnittstelle.command.SaveBenutzerCommandConverter;
import de.wps.radvis.backend.administration.schnittstelle.view.BenutzerEditView;
import de.wps.radvis.backend.administration.schnittstelle.view.BenutzerListView;
import de.wps.radvis.backend.application.JacksonConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerIstNichtRegistriertException;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

@Tag("group3")
@ContextConfiguration(classes = {
	AdministrationConfiguration.class,
	BenutzerConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerVerwaltungControllerIntegrationTestIT.TestConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class BenutzerVerwaltungControllerIntegrationTestIT extends DBIntegrationTestIT {
	@MockitoBean
	private SessionRegistry sessionRegistry;
	@MockitoBean
	private BenutzerResolver benutzerResolver;

	public static class TestConfiguration {
		@Autowired
		private BenutzerService benutzerService;
		@Autowired
		private VerwaltungseinheitService verwaltungseinheitService;
		@Autowired
		private SaveBenutzerCommandConverter saveBenutzerCommandConverter;
		@Autowired
		private BenutzerGuard benutzerGuard;
		@Autowired
		private MailService mailservice;
		@Autowired
		private SessionRegistry sessionRegistry;
		@Autowired
		private BenutzerResolver benutzerResolver;

		private final ExtentProperty extent = new ExtentProperty(492846.960, 500021.252, 5400410.543, 5418644.476);
		private final CommonConfigurationProperties commonConfigurationProperties = new CommonConfigurationProperties(
			"src/test/resources",
			60,
			extent,
			null,
			"test",
			"https://radvis-dev.landbw.de/",
			"DLM", "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources", 1.0, 1.0);

		@Bean
		public BenutzerVerwaltungController benutzerController() {
			MockitoAnnotations.openMocks(this);
			Mockito.when(benutzerResolver.fromAuthentication(Mockito.any()))
				.thenReturn(
					BenutzerTestDataProvider.admin(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
						.build());
			return new BenutzerVerwaltungController(benutzerService, verwaltungseinheitService,
				saveBenutzerCommandConverter, mailservice, benutzerResolver,
				sessionRegistry, commonConfigurationProperties, benutzerGuard);
		}
	}

	@Autowired
	private BenutzerVerwaltungController benutzerController;
	@Autowired
	private BenutzerRepository benutzerRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private OrganisationRepository organisationRepository;
	@Autowired
	private MailService mailService;

	private final JsonMapper mapper = new JsonMapper();
	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	public void setUp() {
		mapper.registerModule(new JacksonConfiguration().customJacksonGeometryModule());
	}

	@Test
	void testSaveBenutzer_BenutzerWirdGeaendert()
		throws Exception {
		// arrange
		Gebietskoerperschaft testOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Coole Organisation")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();

		testOrganisation = gebietskoerperschaftRepository.save(testOrganisation);

		Benutzer benutzer = BenutzerTestDataProvider.admin(testOrganisation)
			.vorname(Name.of("Testus"))
			.nachname(Name.of("Testperson"))
			.mailadresse(Mailadresse.of("meinemail@testRadvis.de"))
			.serviceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"))
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR, Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		Benutzer gespeicherterBenutzer = benutzerRepository.save(benutzer);
		entityManager.flush();
		entityManager.clear();

		String json = "{"
			+ "	\"id\":" + gespeicherterBenutzer.getId() + ","
			+ "	\"version\":" + gespeicherterBenutzer.getVersion() + ","
			+ "	\"vorname\":\"Testus\","
			+ "	\"nachname\":\"Realeperson\"," // <-- geändert
			+ "	\"email\":\"meinemail@testRadvis.de\","
			+ "	\"organisation\":" + testOrganisation.getId() + ","
			+ " \"rollen\" : [ \"RADVIS_ADMINISTRATOR\" ]" // <-- geändert
			+ "}";

		// act
		SaveBenutzerCommand saveBenutzerCommand = mapper.readValue(json,
			SaveBenutzerCommand.class);
		benutzerController.saveBenutzer(authentication, saveBenutzerCommand);

		// assert
		Benutzer result = benutzerRepository.findByServiceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"))
			.get();
		assertThat(result)
			.extracting("vorname", "nachname", "status", "organisation", "mailadresse", "serviceBwId")
			.containsExactly(
				Name.of("Testus"), Name.of("Realeperson"), BenutzerStatus.AKTIV, testOrganisation,
				Mailadresse.of("meinemail@testRadvis.de"),
				ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"));

		assertThat(result.getRollen()).containsExactly(Rolle.RADVIS_ADMINISTRATOR);
	}

	@Test
	void testSaveBenutzer_NichtAutorisiert_throwsException()
		throws JsonProcessingException {
		// arrange
		Gebietskoerperschaft testOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Coole Organisation")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.build();
		Gebietskoerperschaft andereOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Nicht so coole andere Organisation")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.build();

		testOrganisation = gebietskoerperschaftRepository.save(testOrganisation);
		andereOrganisation = gebietskoerperschaftRepository.save(andereOrganisation);

		Benutzer aktiverBenutzer = benutzerRepository.save(
			BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(andereOrganisation)
				.vorname(Name.of("aktiver"))
				.nachname(Name.of("Benutzer"))
				.mailadresse(Mailadresse.of("asdf@testRadvis.de"))
				.serviceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130004"))
				.build());

		Benutzer gespeicherterBenutzer = benutzerRepository.save(
			BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(andereOrganisation)
				.vorname(Name.of("Testus"))
				.nachname(Name.of("Testperson"))
				.mailadresse(Mailadresse.of("meinemail@testRadvis.de"))
				.serviceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"))
				.build());

		entityManager.flush();
		entityManager.clear();

		String json = "{"
			+ "	\"id\":" + gespeicherterBenutzer.getId() + ","
			+ "	\"version\":" + gespeicherterBenutzer.getVersion() + ","
			+ "	\"vorname\":\"Testus\","
			+ "	\"nachname\":\"Realeperson\"," // <-- geändert
			+ "	\"organisation\":" + testOrganisation.getId() + ","
			+ " \"rollen\" : [ \"EXTERNER_DIENSTLEISTER\", \"KREISKOORDINATOREN\", \"RADVIS_ADMINISTRATOR\" ]"
			// <-- geändert. nur KREISKOORDINATOREN dürfte vergeben werden
			+ "}";

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(aktiverBenutzer);

		// act + assert
		SaveBenutzerCommand saveBenutzerCommand = mapper.readValue(json,
			SaveBenutzerCommand.class);

		assertThatThrownBy(() -> benutzerController.saveBenutzer(authentication, saveBenutzerCommand))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageStartingWith("Sie sind nicht dazu autorisiert diese Rollen zu vergeben oder zu entnehmen:")
			.hasMessageContaining("'RadVIS AdministratorIn'");
	}

	@Test
	void testSaveBenutzer_aendereRollen_NichtAutorisiertFuerRollenvergabe_throwsException()
		throws JsonProcessingException {
		// arrange
		Gebietskoerperschaft laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();

		Benutzer koordinator = BenutzerTestDataProvider.kreiskoordinator(laEntenhausen).build();

		Benutzer benutzer1 = BenutzerTestDataProvider.externerDienstleister(laEntenhausen).build();

		laEntenhausen = gebietskoerperschaftRepository.save(laEntenhausen);
		assertThat(benutzer1.getStatus()).isEqualTo(BenutzerStatus.AKTIV);
		benutzer1 = benutzerRepository.save(benutzer1);
		final Benutzer derKoordinator = benutzerRepository.save(koordinator);

		entityManager.flush();
		entityManager.clear();

		String json = "{"
			+ "	\"id\":" + benutzer1.getId() + ","
			+ "	\"version\":" + benutzer1.getVersion() + ","
			+ "	\"vorname\":\"" + benutzer1.getVorname() + "\","
			+ "	\"nachname\":\"Realeperson\"," // <-- geändert
			+ "	\"organisation\":" + laEntenhausen.getId() + ","
			+ " \"rollen\" : [ \"EXTERNER_DIENSTLEISTER\", \"RADVERKEHRSBEAUFTRAGTER\" ]" // <-- geändert
			+ "}";

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(derKoordinator);

		// act + assert
		SaveBenutzerCommand saveBenutzerCommand = mapper.readValue(json,
			SaveBenutzerCommand.class);

		assertThatThrownBy(() -> benutzerController.saveBenutzer(authentication, saveBenutzerCommand))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining(
				"Sie sind nicht dazu autorisiert diese Rollen zu vergeben oder zu entnehmen: 'RadverkehrsbeauftragteR Regierungsbezirk'");
	}

	@Test
	void testSaveBenutzer_aendereAndereDaten_NichtAutorisiertFuerRollenvergabe_AberWeilRollenNichtGeaender_gehtTrotzdem()
		throws Exception {
		// arrange
		Gebietskoerperschaft laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(OrganisationsArt.BUNDESLAND)
			.build();

		Benutzer koordinator = BenutzerTestDataProvider.kreiskoordinator(laEntenhausen).build();

		Benutzer benutzer1 = BenutzerTestDataProvider.externerDienstleister(laEntenhausen)
			.vorname(Name.of("Mickey"))
			.mailadresse(Mailadresse.of("mm@testRadvis.duck"))
			.serviceBwId(ServiceBwId.of("irgendsoeineid-1"))
			.build();

		laEntenhausen = gebietskoerperschaftRepository.save(laEntenhausen);
		assertThat(benutzer1.getStatus()).isEqualTo(BenutzerStatus.AKTIV);
		benutzer1 = benutzerRepository.save(benutzer1);
		benutzerRepository.save(koordinator);

		entityManager.flush();
		entityManager.clear();

		String json = "{"
			+ "	\"id\":" + benutzer1.getId() + ","
			+ "	\"version\":" + benutzer1.getVersion() + ","
			+ "	\"vorname\":\"" + benutzer1.getVorname() + "\","
			+ "	\"nachname\":\"Realeperson\"," // <-- geändert
			+ "	\"organisation\":" + laEntenhausen.getId() + ","
			+ "	\"email\":\"" + benutzer1.getMailadresse() + "\","
			+ " \"rollen\" : [ \"EXTERNER_DIENSTLEISTER\" ]" // <-- nicht geändert, daher alles chiko!
			+ "}";

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(koordinator);

		/*
		 * Eigentlich darf ein Kreiskoordiantor keine Poweruser-Rolle Vergeben, aber da diese nicht geändert wird und
		 * schon vorher da war, ist es erlaubt
		 */

		// act
		SaveBenutzerCommand saveBenutzerCommand = mapper.readValue(json, SaveBenutzerCommand.class);
		benutzerController.saveBenutzer(authentication, saveBenutzerCommand);

		// assert
		Benutzer result = benutzerRepository.findByServiceBwId(ServiceBwId.of("irgendsoeineid-1")).get();
		assertThat(result)
			.extracting("vorname", "nachname", "status", "organisation", "mailadresse", "serviceBwId")
			.containsExactly(
				Name.of("Mickey"), Name.of("Realeperson"), BenutzerStatus.AKTIV, laEntenhausen,
				Mailadresse.of("mm@testRadvis.duck"),
				ServiceBwId.of("irgendsoeineid-1"));

		assertThat(result.getRollen()).containsExactly(Rolle.EXTERNER_DIENSTLEISTER);
	}

	@Test
	void testSaveBenutzer_aendereRollen_AutorisiertFuerRollenvergabe()
		throws Exception {
		// arrange
		Gebietskoerperschaft laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.build();

		Benutzer koordinator = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(laEntenhausen).build();

		Benutzer benutzer1 = BenutzerTestDataProvider.externerDienstleister(laEntenhausen)
			.vorname(Name.of("Mickey"))
			.mailadresse(Mailadresse.of("mm@testRadvis.duck"))
			.serviceBwId(ServiceBwId.of("irgendsoeineid-1"))
			.build();

		laEntenhausen = gebietskoerperschaftRepository.save(laEntenhausen);
		assertThat(benutzer1.getStatus()).isEqualTo(BenutzerStatus.AKTIV);
		benutzer1 = benutzerRepository.save(benutzer1);
		benutzerRepository.save(koordinator);

		entityManager.flush();
		entityManager.clear();

		String json = "{"
			+ "	\"id\":" + benutzer1.getId() + ","
			+ "	\"version\":" + benutzer1.getVersion() + ","
			+ "	\"vorname\":\"Mickey\","
			+ "	\"nachname\":\"Realeperson\"," // <-- geändert
			+ "	\"email\":\"mm@testRadvis.duck\"," // <-- geändert
			+ "	\"organisation\":" + laEntenhausen.getId() + ","
			+ " \"rollen\" : [ \"EXTERNER_DIENSTLEISTER\" , \"RADVERKEHRSBEAUFTRAGTER\" ]" // <-- geändert
			+ "}";

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(koordinator);

		// act + assert
		SaveBenutzerCommand saveBenutzerCommand = mapper.readValue(json,
			SaveBenutzerCommand.class);

		benutzerController.saveBenutzer(authentication, saveBenutzerCommand);

		Benutzer result = benutzerRepository.findByServiceBwId(ServiceBwId.of("irgendsoeineid-1")).get();
		assertThat(result)
			.extracting("vorname", "nachname", "status", "organisation", "mailadresse", "serviceBwId")
			.containsExactly(
				Name.of("Mickey"), Name.of("Realeperson"), BenutzerStatus.AKTIV, laEntenhausen,
				Mailadresse.of("mm@testRadvis.duck"),
				ServiceBwId.of("irgendsoeineid-1"));

		assertThat(result.getRollen())
			.containsExactlyInAnyOrder(Rolle.EXTERNER_DIENSTLEISTER, Rolle.RADVERKEHRSBEAUFTRAGTER);
	}

	@Test
	void testGetBenutzerorganisationen() {
		// arrange
		Gebietskoerperschaft gespeicherterGebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Öffentliche Organisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND).build());
		Organisation gespiecherteOrganisation = organisationRepository.save(
			VerwaltungseinheitTestDataProvider.defaultOrganisation().name("Private Organisation")
				.organisationsArt(OrganisationsArt.SONSTIGES).build());

		Benutzer benutzer = BenutzerTestDataProvider.admin(gespiecherteOrganisation)
			.vorname(Name.of("Testus"))
			.nachname(Name.of("Testperson"))
			.mailadresse(Mailadresse.of("meinemail@testRadvis.de"))
			.serviceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"))
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR, Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);

		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(benutzerController.getBenutzerOrganisationen(authentication)).extracting(
			VerwaltungseinheitView::getId)
			.containsExactlyInAnyOrder(gespeicherterGebietskoerperschaft.getId(), gespiecherteOrganisation.getId());
	}

	@Test
	void testGetAlleBenutzerFuerAdministratorSiehtAlles() {
		// arrange
		Gebietskoerperschaft laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();

		Benutzer benutzer1 = BenutzerTestDataProvider.adminInaktiv(laEntenhausen)
			.vorname(Name.of("Mickey"))
			.nachname(Name.of("Maus"))
			.status(BenutzerStatus.AKTIV)
			.mailadresse(Mailadresse.of("mm@testRadvis.duck"))
			.serviceBwId(ServiceBwId.of("irgendsoeineid-1"))
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR, Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();
		Benutzer benutzer2 = BenutzerTestDataProvider.adminInaktiv(laEntenhausen)
			.vorname(Name.of("Donald"))
			.nachname(Name.of("Duck"))
			.mailadresse(Mailadresse.of("dd@testRadvis.duck"))
			.serviceBwId(ServiceBwId.of("irgendsoeineid-2"))
			.build();
		Benutzer benutzer3 = BenutzerTestDataProvider.adminInaktiv(laEntenhausen)
			.vorname(Name.of("Goofy"))
			.nachname(Name.of("Wieheistdereigentlichmitnachname?"))
			.mailadresse(Mailadresse.of("gg@testRadvis.duck"))
			.serviceBwId(ServiceBwId.of("irgendsoeineid-3"))
			.build();

		gebietskoerperschaftRepository.save(laEntenhausen);
		benutzerRepository.save(benutzer1);
		benutzerRepository.save(benutzer2);
		benutzerRepository.save(benutzer3);

		entityManager.flush();
		entityManager.clear();

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer1);

		// act
		List<BenutzerListView> result = benutzerController.getAlleBenutzer(authentication);

		// assert
		assertThat(result)
			.flatExtracting("vorname", "nachname", "status", "organisation", "email")
			.containsExactlyInAnyOrder(
				Name.of("Mickey"), Name.of("Maus"), BenutzerStatus.AKTIV, Mailadresse.of("mm@testRadvis.duck"),
				"Landesamt Entenhausen (Bundesland)",
				Name.of("Donald"), Name.of("Duck"), BenutzerStatus.INAKTIV, Mailadresse.of("dd@testRadvis.duck"),
				"Landesamt Entenhausen (Bundesland)",
				Name.of("Goofy"), Name.of("Wieheistdereigentlichmitnachname?"), BenutzerStatus.INAKTIV,
				Mailadresse.of("gg@testRadvis.duck"),
				"Landesamt Entenhausen (Bundesland)");
	}

	@Test
	void testGetAlleBenutzerFuerKreiskoordinatorSiehtNurSichSelbst() {
		// arrange
		Gebietskoerperschaft laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();
		Gebietskoerperschaft laGaensehausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Gaensehausen")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();

		Benutzer benutzer1 = BenutzerTestDataProvider.kreiskoordinator(laEntenhausen)
			.vorname(Name.of("Mickey"))
			.nachname(Name.of("Maus"))
			.mailadresse(Mailadresse.of("mm@testRadvis.duck"))
			.serviceBwId(ServiceBwId.of("irgendsoeineid-1"))
			.build();
		Benutzer benutzer2 = BenutzerTestDataProvider.adminInaktiv(laEntenhausen)
			.vorname(Name.of("Donald"))
			.nachname(Name.of("Duck"))
			.mailadresse(Mailadresse.of("dd@testRadvis.duck"))
			.serviceBwId(ServiceBwId.of("irgendsoeineid-2"))
			.build();
		Benutzer benutzer3 = BenutzerTestDataProvider.adminInaktiv(laGaensehausen)
			.vorname(Name.of("Goofy"))
			.nachname(Name.of("Wieheistdereigentlichmitnachname?"))
			.mailadresse(Mailadresse.of("gg@testRadvis.duck"))
			.serviceBwId(ServiceBwId.of("irgendsoeineid-3"))
			.build();

		gebietskoerperschaftRepository.save(laEntenhausen);
		gebietskoerperschaftRepository.save(laGaensehausen);
		benutzerRepository.save(benutzer1);
		benutzerRepository.save(benutzer2);
		benutzerRepository.save(benutzer3);

		entityManager.flush();
		entityManager.clear();

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer1);

		// act
		List<BenutzerListView> result = benutzerController.getAlleBenutzer(authentication);

		// assert
		assertThat(result)
			.flatExtracting("vorname", "nachname", "status", "organisation", "email")
			.containsExactlyInAnyOrder(
				Name.of("Mickey"), Name.of("Maus"), BenutzerStatus.AKTIV, Mailadresse.of("mm@testRadvis.duck"),
				"Landesamt Entenhausen (Bundesland)");
	}

	@Test
	void testAendereBenutzerStatus() throws Exception {
		// arrange
		Gebietskoerperschaft laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();

		Benutzer admin = BenutzerTestDataProvider.admin(laEntenhausen).build();

		Benutzer benutzer1 = BenutzerTestDataProvider.adminInaktiv(laEntenhausen)
			.vorname(Name.of("Mickey"))
			.nachname(Name.of("Maus"))
			.mailadresse(Mailadresse.of("mm@testRadvis.duck"))
			.serviceBwId(ServiceBwId.of("irgendsoeineid-1"))
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR, Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();

		gebietskoerperschaftRepository.save(laEntenhausen);
		assertThat(benutzer1.getStatus()).isEqualTo(BenutzerStatus.INAKTIV);
		benutzer1 = benutzerRepository.save(benutzer1);
		admin = benutzerRepository.save(admin);

		entityManager.flush();
		entityManager.clear();

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(admin);

		// act
		BenutzerEditView result = benutzerController.benutzerStatusAendern(authentication, benutzer1.getId(),
			benutzer1.getVersion(), "AKTIV");

		// assert
		assertThat(result.getStatus()).hasToString("Aktiv");
		assertThat(result.getRollen()).containsExactlyInAnyOrder(Rolle.RADVIS_ADMINISTRATOR,
			Rolle.RADVERKEHRSBEAUFTRAGTER);
		assertThat(benutzerRepository.findByServiceBwId(ServiceBwId.of("irgendsoeineid-1")).get().getStatus())
			.isEqualTo(BenutzerStatus.AKTIV);
		String mailText = "Hallo Mickey Maus,\nIhr RadVIS-Account für mm@testRadvis.duck wurde aktiviert und kann nach einem erneuten Einloggen verwendet werden.\nSie können RadVIS unter https://radvis-dev.landbw.de/ erreichen.";
		verify(mailService).sendMail(List.of("mm@testRadvis.duck"), "Sie wurden für RadVIS aktiviert", mailText);

		/*
		 * Und Wieder Deaktivieren:
		 */

		// act
		result = benutzerController.benutzerStatusAendern(authentication, benutzer1.getId(),
			benutzer1.getVersion() + 1L, "INAKTIV");

		// assert
		assertThat(result.getStatus().toString()).isEqualTo("Inaktiv");
		assertThat(benutzerRepository.findByServiceBwId(ServiceBwId.of("irgendsoeineid-1")).get().getStatus())
			.isEqualTo(BenutzerStatus.INAKTIV);
	}

	@Test
	void aktiviereBenutzer_DatenNichtMehrAktuell_throwsException() {
		// arrange
		Gebietskoerperschaft laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(OrganisationsArt.BUNDESLAND)
			.build();

		Benutzer admin = BenutzerTestDataProvider.admin(laEntenhausen).serviceBwId(ServiceBwId.of("sbwid1")).build();
		Benutzer benutzer1 = BenutzerTestDataProvider.adminInaktiv(laEntenhausen).serviceBwId(ServiceBwId.of("sbwid2"))
			.version(10L).build();

		gebietskoerperschaftRepository.save(laEntenhausen);
		assertThat(benutzer1.getStatus()).isEqualTo(BenutzerStatus.INAKTIV);
		benutzerRepository.save(admin);
		benutzer1 = benutzerRepository.save(benutzer1);
		final long id = benutzer1.getId();

		entityManager.flush();
		entityManager.clear();

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(admin);

		// act +assert
		assertThatThrownBy(() -> benutzerController.benutzerStatusAendern(authentication, id, 9L, "AKTIV"))
			.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void testAendereBenutzerStatusBeiNichtvorhanden_throwsException() {
		// arrange
		Verwaltungseinheit laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();
		Benutzer admin = BenutzerTestDataProvider.admin(laEntenhausen).build();

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(admin);

		// act + assert
		assertThatThrownBy(() -> benutzerController.benutzerStatusAendern(authentication, 12L, 5L, "AKTIV"))
			.isInstanceOf(BenutzerIstNichtRegistriertException.class);
	}

	@Test
	void testoptimisticlocking_saveBenutzerInkrementiertKorrekt()
		throws Exception {
		// arrange
		Gebietskoerperschaft laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();

		Benutzer admin = BenutzerTestDataProvider.admin(laEntenhausen).build();

		Benutzer benutzer1 = BenutzerTestDataProvider.adminInaktiv(laEntenhausen)
			.serviceBwId(ServiceBwId.of("irgendsoeineid-1"))
			.version(5L)
			.build();

		gebietskoerperschaftRepository.save(laEntenhausen);
		assertThat(benutzer1.getStatus()).isEqualTo(BenutzerStatus.INAKTIV);
		benutzerRepository.save(admin);
		benutzer1 = benutzerRepository.save(benutzer1);

		entityManager.flush();
		entityManager.clear();

		Long alteVersion = benutzer1.getVersion();

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(admin);

		// act
		BenutzerEditView result = benutzerController.benutzerStatusAendern(authentication, benutzer1.getId(),
			benutzer1.getVersion(), "AKTIV");

		// assert
		assertThat(result.getStatus().toString()).isEqualTo("Aktiv");
		assertThat(benutzerRepository.findByServiceBwId(ServiceBwId.of("irgendsoeineid-1")).get().getStatus())
			.isEqualTo(BenutzerStatus.AKTIV);
		assertThat(benutzerRepository.findByServiceBwId(ServiceBwId.of("irgendsoeineid-1")).get().getVersion())
			.isEqualTo(alteVersion + 1);

		/*
		 * Und Wieder Deaktivieren:
		 */

		// act + assert
		final long benutzerId = benutzer1.getId();
		assertThatThrownBy(
			() -> benutzerController.benutzerStatusAendern(authentication, benutzerId, alteVersion, "INAKTIV"))
				.isInstanceOf(OptimisticLockException.class);
	}

	@Test
	void aktiviereBenutzer_NichtAutorisiertFuerRollen_throwsException() {
		// arrange
		Gebietskoerperschaft laEntenhausen = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Landesamt Entenhausen")
			.organisationsArt(OrganisationsArt.BUNDESLAND)
			.build();

		gebietskoerperschaftRepository.save(laEntenhausen);
		Benutzer regierunngserfasser = benutzerRepository.save(
			BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(laEntenhausen)
				.build());

		final long id = benutzerRepository.save(
			BenutzerTestDataProvider.bearbeiterinVmRadnetzAdminInaktiv(laEntenhausen)
				.version(10L)
				.build())
			.getId();

		entityManager.flush();
		entityManager.clear();

		Authentication authentication = Mockito.mock(Authentication.class);
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(regierunngserfasser);

		// act +assert
		assertThatThrownBy(() -> benutzerController.benutzerStatusAendern(authentication, id, 11L, "AKTIV"))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(
				"Sie haben nicht die Berechtigung Benutzer mit folgenden Rollen zu ändern: 'BearbeiterIn (VM)/RadNETZ-AdministratorIn'");
	}

}
