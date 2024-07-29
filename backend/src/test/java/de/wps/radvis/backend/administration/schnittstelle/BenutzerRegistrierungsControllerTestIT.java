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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import de.wps.radvis.backend.application.JacksonConfiguration;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.authentication.domain.exception.BenutzerNotAuthenticatedException;
import de.wps.radvis.backend.authentication.schnittstelle.BenutzerRegistrierungsController;
import de.wps.radvis.backend.authentication.schnittstelle.RegistriereBenutzerCommand;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerExistiertBereitsException;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group2")
@ContextConfiguration(classes = {
	BenutzerConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerRegistrierungsControllerTestIT.TestConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class BenutzerRegistrierungsControllerTestIT extends DBIntegrationTestIT {
	public static class TestConfiguration {
		@Autowired
		private CommonConfigurationProperties commonConfigurationProperties;
		@Autowired
		private BenutzerService benutzerService;
		@MockBean
		private MailService mailservice;

		@Bean
		public BenutzerRegistrierungsController benutzerRegistrierungsController() {
			MockitoAnnotations.openMocks(this);
			return new BenutzerRegistrierungsController(commonConfigurationProperties, benutzerService, mailservice);
		}
	}

	@Autowired
	private BenutzerRegistrierungsController benutzerRegistrierungsController;

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private MailService mailService;

	private final JsonMapper mapper = new JsonMapper();
	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	public void setUp() {
		mapper.registerModule(new JacksonConfiguration().customJacksonGeometryModule());
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void testValidierung_BenutzerVorhanden_wirdNichtAngelegt()
		throws JsonProcessingException {
		// arrange
		RadVisUserDetails userDetails = new RadVisUserDetails(ServiceBwId.of("Some ID"));
		Authentication authentication = Mockito.mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(userDetails);

		Gebietskoerperschaft testGebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Coole Organisation")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();
		Benutzer testBenutzer = new Benutzer(Name.of("AlterTestus"), Name.of("Testperson"), BenutzerStatus.INAKTIV,
			testGebietskoerperschaft, Mailadresse.of("gueltigeEmailAdresse@testRadvis.de"), ServiceBwId.of("Some ID"),
			Set.of(Rolle.RADVIS_ADMINISTRATOR));

		testGebietskoerperschaft = gebietskoerperschaftRepository.save(testGebietskoerperschaft);
		benutzerRepository.save(testBenutzer);
		entityManager.flush();
		entityManager.clear();

		String json = "{"
			+ "	\"vorname\":\"NeuerTestus\","
			+ "	\"nachname\":\"Testperson\","
			+ "	\"organisation\":" + testGebietskoerperschaft.getId()
			+ "}";

		// act
		RegistriereBenutzerCommand saveBenutzerCommand = mapper.readValue(json, RegistriereBenutzerCommand.class);
		assertThatThrownBy(
			() -> benutzerRegistrierungsController.registriereBenutzer(authentication, saveBenutzerCommand))
				.isInstanceOf(BenutzerExistiertBereitsException.class);
	}

	@Test
	void testRegistrierung_neuerBenutzer_BenutzerWirdAngelegt()
		throws BenutzerExistiertBereitsException, JsonProcessingException, BenutzerNotAuthenticatedException {
		// arrange
		RadVisUserDetails userDetails = new RadVisUserDetails(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"));
		Authentication authentication = Mockito.mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(userDetails);

		Gebietskoerperschaft zustaendigeLandesOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).build();
		Gebietskoerperschaft andereLandesOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Landesorganisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND).build();
		Gebietskoerperschaft zustaendigeKreisOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).uebergeordneteOrganisation(zustaendigeLandesOrganisation).build();
		Gebietskoerperschaft andereKreisOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Andere Kreisorganisation")
			.organisationsArt(OrganisationsArt.KREIS).uebergeordneteOrganisation(andereLandesOrganisation).build();
		Gebietskoerperschaft zustaendigeGemeindeOrganisation = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Zuständige Gemeindeorganisation")
			.organisationsArt(OrganisationsArt.GEMEINDE).uebergeordneteOrganisation(zustaendigeKreisOrganisation)
			.build();

		gebietskoerperschaftRepository.saveAll(List.of(
			zustaendigeLandesOrganisation,
			andereLandesOrganisation,
			zustaendigeKreisOrganisation,
			andereKreisOrganisation,
			zustaendigeGemeindeOrganisation));

		// benutzer - Nur der Kreiskoordinator soll eine Mail erhalten
		// zuständige Admins
		Benutzer kreiskoodinator = new Benutzer(Name.of("Kreis"), Name.of("Koordinator"), BenutzerStatus.AKTIV,
			zustaendigeKreisOrganisation, Mailadresse.of("kreiskoordinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130002"), Set.of(Rolle.KREISKOORDINATOREN));
		// andere Admins
		Benutzer radvisAdmin = new Benutzer(Name.of("Radvis"), Name.of("Admin"), BenutzerStatus.AKTIV,
			zustaendigeLandesOrganisation, Mailadresse.of("admin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130001"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		Benutzer nichtZustaendigKreiskoodinator = new Benutzer(Name.of("anderer"), Name.of("kreiskoodrinator"),
			BenutzerStatus.AKTIV,
			andereKreisOrganisation, Mailadresse.of("anderer.kreiskoodinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130005"), Set.of(Rolle.KREISKOORDINATOREN));
		// nicht zuständig sosntiges
		Benutzer poweruser = new Benutzer(Name.of("Max"), Name.of("Power"), BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("poweruser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130010"), Set.of(Rolle.EXTERNER_DIENSTLEISTER));

		benutzerRepository.saveAll(List.of(
			radvisAdmin,
			kreiskoodinator,
			nichtZustaendigKreiskoodinator,
			poweruser));

		String json = "{"
			+ "	\"vorname\":\"Testus\","
			+ "	\"nachname\":\"Testperson\","
			+ "	\"email\":\"testus.testperson@testRadvis.de\","
			+ "	\"organisation\":" + zustaendigeGemeindeOrganisation.getId() + ","
			+ " \"rollen\" : [ \"RADWEGE_ERFASSERIN\" ]"
			+ "}";

		// act
		RegistriereBenutzerCommand registriereBenutzerCommand = mapper.readValue(json,
			RegistriereBenutzerCommand.class);
		benutzerRegistrierungsController.registriereBenutzer(authentication, registriereBenutzerCommand);

		// assert
		Benutzer result = benutzerRepository.findByServiceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"))
			.get();
		assertThat(result)
			.extracting("vorname", "nachname", "status", "organisation", "mailadresse", "serviceBwId")
			.containsExactly(
				Name.of("Testus"), Name.of("Testperson"), BenutzerStatus.WARTE_AUF_FREISCHALTUNG,
				zustaendigeGemeindeOrganisation,
				Mailadresse.of("testus.testperson@testRadvis.de"),
				ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"));

		String mailText = String.format(
			"Der Benutzer 'Testus Testperson' wurde angelegt und wartet auf Freischaltung.\n" +
				"Die Bearbeitung des Benutzers kann erfolgen unter " +
				"http://localhost:8080/administration/benutzer/%s .", result.getId());

		verify(mailService).sendMail(List.of("kreiskoordinator@testRadvis.de"), "Neuer Benutzer wurde angelegt",
			mailText);

		assertThat(result.getRollen()).containsExactly(Rolle.RADWEGE_ERFASSERIN);
	}

	@Test
	void testRegistrierung_neuerBenutzer_MailNurAnAdminsDieFreischaltenKoennen()
		throws BenutzerExistiertBereitsException, JsonProcessingException, BenutzerNotAuthenticatedException {
		// arrange
		RadVisUserDetails userDetails = new RadVisUserDetails(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"));
		Authentication authentication = Mockito.mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(userDetails);

		Gebietskoerperschaft zustaendigeLandesOrganisation = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Zuständige Landesorganisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND).build());
		Gebietskoerperschaft zustaendigeKreisOrganisation = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Zuständige Kreisorganisation")
				.organisationsArt(OrganisationsArt.KREIS).uebergeordneteOrganisation(zustaendigeLandesOrganisation)
				.build());
		Gebietskoerperschaft zustaendigeGemeindeOrganisation = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Zuständige Gemeindeorganisation")
				.organisationsArt(OrganisationsArt.GEMEINDE).uebergeordneteOrganisation(zustaendigeKreisOrganisation)
				.build());

		// zuständige Admins
		Benutzer kreiskoodinator = new Benutzer(Name.of("Kreis"), Name.of("Koordinator"), BenutzerStatus.AKTIV,
			zustaendigeKreisOrganisation, Mailadresse.of("kreiskoordinator@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130002"), Set.of(Rolle.KREISKOORDINATOREN));
		// andere Admins
		Benutzer radvisAdmin = new Benutzer(Name.of("Radvis"), Name.of("Admin"), BenutzerStatus.AKTIV,
			zustaendigeLandesOrganisation, Mailadresse.of("admin@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130001"), Set.of(Rolle.RADVIS_ADMINISTRATOR));
		// nicht zuständig sosntiges
		Benutzer poweruser = new Benutzer(Name.of("Max"), Name.of("Power"), BenutzerStatus.AKTIV,
			zustaendigeGemeindeOrganisation, Mailadresse.of("poweruser@testRadvis.de"),
			ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130010"), Set.of(Rolle.EXTERNER_DIENSTLEISTER));

		benutzerRepository.saveAll(List.of(
			radvisAdmin,
			kreiskoodinator,
			poweruser));

		String json = "{"
			+ "	\"vorname\":\"Testus\","
			+ "	\"nachname\":\"Testperson\","
			+ "	\"email\":\"testus.testperson@testRadvis.de\","
			+ "	\"organisation\":" + zustaendigeGemeindeOrganisation.getId() + ","
			+ " \"rollen\" : [ \"RADVERKEHRSBEAUFTRAGTER\" ]"
			+ "}";

		// act
		RegistriereBenutzerCommand registriereBenutzerCommand = mapper.readValue(json,
			RegistriereBenutzerCommand.class);
		benutzerRegistrierungsController.registriereBenutzer(authentication, registriereBenutzerCommand);

		// assert
		Benutzer result = benutzerRepository.findByServiceBwId(ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"))
			.get();
		assertThat(result)
			.extracting("vorname", "nachname", "status", "organisation", "mailadresse", "serviceBwId")
			.containsExactly(
				Name.of("Testus"), Name.of("Testperson"), BenutzerStatus.WARTE_AUF_FREISCHALTUNG,
				zustaendigeGemeindeOrganisation,
				Mailadresse.of("testus.testperson@testRadvis.de"),
				ServiceBwId.of("69194c6a-0bf0-11ec-9a03-0242ac130003"));

		String mailText = String.format(
			"Der Benutzer 'Testus Testperson' wurde angelegt und wartet auf Freischaltung.\n" +
				"Die Bearbeitung des Benutzers kann erfolgen unter " +
				"http://localhost:8080/administration/benutzer/%s .", result.getId());

		verify(mailService).sendMail(List.of("admin@testRadvis.de"), "Neuer Benutzer wurde angelegt",
			mailText);

		assertThat(result.getRollen()).containsExactly(Rolle.RADVERKEHRSBEAUFTRAGTER);
	}
}
