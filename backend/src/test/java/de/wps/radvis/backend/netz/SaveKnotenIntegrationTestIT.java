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

package de.wps.radvis.backend.netz;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;

import org.jaitools.jts.CoordinateSequence2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import de.wps.radvis.backend.administration.AdministrationConfiguration;
import de.wps.radvis.backend.application.JacksonConfiguration;
import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.schnittstelle.BenutzerResolverImpl;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.schnittstelle.NetzController;
import de.wps.radvis.backend.netz.schnittstelle.NetzGuard;
import de.wps.radvis.backend.netz.schnittstelle.NetzToFeatureDetailsConverter;
import de.wps.radvis.backend.netz.schnittstelle.SaveKanteCommandConverter;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKnotenCommand;
import de.wps.radvis.backend.netz.schnittstelle.view.KnotenEditView;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group6")
@ContextConfiguration(classes = {
	NetzConfiguration.class,
	AdministrationConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	SaveKnotenIntegrationTestIT.TestConfiguration.class,
	BenutzerConfiguration.class,
	CommonConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
class SaveKnotenIntegrationTestIT extends DBIntegrationTestIT {
	public static class TestConfiguration {

		@Autowired
		private NetzService netzService;
		@MockBean
		private SaveKanteCommandConverter saveKanteCommandConverter;
		@MockBean
		private NetzToFeatureDetailsConverter netzToFeatureDetailsConverter;
		@Autowired
		private NetzGuard netzAutorisierungsService;
		@Autowired
		private ZustaendigkeitsService zustaendigkeitsService;

		@Bean
		public NetzController netzController() {
			return new NetzController(netzService, netzAutorisierungsService,
				new BenutzerResolverImpl(), zustaendigkeitsService, saveKanteCommandConverter,
				netzToFeatureDetailsConverter);
		}

	}

	@Autowired
	private KnotenRepository knotenRepository;

	@Autowired
	private NetzController netzController;

	@PersistenceContext
	EntityManager entityManager;

	private final JsonMapper mapper = new JsonMapper();

	GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	private Benutzer adminBundesland;
	private RadVisAuthentication authentication;

	@BeforeEach
	public void setup() {
		mapper.registerModule(new JacksonConfiguration().customJacksonGeometryModule());

		Verwaltungseinheit bundesland = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(101L)
			.name("Bundesland").organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.bereich(geometryFactory
				.createMultiPolygon(new Polygon[] { geometryFactory.createPolygon(
					geometryFactory.createLinearRing(new CoordinateSequence2D(0, 0, 0, 10, 10, 10, 10, 0, 0, 0))) }))
			.build();
		adminBundesland = BenutzerTestDataProvider.admin(bundesland).build();

		List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
		grantedAuthorities.add(() -> adminBundesland.getStatus().name());

		adminBundesland.getRechte().forEach(recht -> {
			GrantedAuthority tmpGrantedAuthority = recht::name;
			grantedAuthorities.add(tmpGrantedAuthority);
		});

		UserDetails userDetails = new RadVisUserDetails(adminBundesland, grantedAuthorities);
		authentication = new RadVisAuthentication(userDetails);
	}

	@Test
	void testSaveKnoten() throws JsonProcessingException {
		// Arrange
		KnotenAttribute testAttribute = KnotenAttribute.builder()
			.kommentar(Kommentar.of("Alter Kommentar"))
			.knotenForm(KnotenForm.ABKNICKENDE_VORFAHRT)
			.build();

		Point point = geometryFactory.createPoint(new Coordinate(1.0, 2.0));
		Knoten testKnoten = Knoten.builder()
			.quelle(QuellSystem.DLM)
			.knotenAttribute(testAttribute)
			.point(point)
			.build();

		Knoten alterKnoten = knotenRepository.save(testKnoten);
		entityManager.flush();
		entityManager.clear();

		assertThat(alterKnoten.getPoint()).isEqualTo(point);

		KnotenAttribute alteAttribute = alterKnoten.getKnotenAttribute();
		assertThat(alteAttribute.getKommentar()).contains(Kommentar.of("Alter Kommentar"));
		assertThat(alteAttribute.getKnotenForm()).contains(KnotenForm.ABKNICKENDE_VORFAHRT);

		SaveKnotenCommand command = mapper.readValue(
			"{\"id\":" + alterKnoten.getId() + ","
				+ "\"kommentar\":\"Neuer Kommentar!\","
				+ "\"knotenVersion\":0,"
				+ "\"knotenForm\":\"MINIKREISVERKEHR_24_M\""
				+ "}",
			SaveKnotenCommand.class);

		KnotenEditView updatedKnoten = netzController.saveKnoten(authentication, command);

		assertThat(updatedKnoten.getId()).isEqualTo(alterKnoten.getId());
		assertThat(updatedKnoten.getKnotenForm()).isEqualTo(KnotenForm.MINIKREISVERKEHR_24_M);
		assertThat(updatedKnoten.getKommentar()).isEqualTo(Kommentar.of("Neuer Kommentar!"));
	}

	@Test
	void testSaveRadNETZKnoten_wirftResponseException() throws JsonProcessingException {
		// Arrange
		KnotenAttribute testAttribute = KnotenAttribute.builder()
			.kommentar(Kommentar.of("Alter Kommentar"))
			.knotenForm(KnotenForm.ABKNICKENDE_VORFAHRT)
			.build();

		Knoten testKnoten = Knoten.builder()
			.quelle(QuellSystem.RadNETZ)
			.knotenAttribute(testAttribute)
			.point(geometryFactory.createPoint(new Coordinate(1.0, 2.0)))
			.build();

		Knoten alterKnoten = knotenRepository.save(testKnoten);
		entityManager.flush();
		entityManager.clear();

		assertThat(alterKnoten.getPoint()).isEqualTo(geometryFactory.createPoint(new Coordinate(1.0, 2.0)));

		KnotenAttribute alteAttribute = alterKnoten.getKnotenAttribute();
		assertThat(alteAttribute.getKommentar()).contains(Kommentar.of("Alter Kommentar"));
		assertThat(alteAttribute.getKnotenForm()).contains(KnotenForm.ABKNICKENDE_VORFAHRT);

		SaveKnotenCommand command = mapper.readValue(
			"{\"id\":" + alterKnoten.getId() + ","
				+ "\"kommentar\":\"Neuer Kommentar!\","
				+ "\"knotenVersion\":0,"
				+ "\"knotenForm\":\"MINIKREISVERKEHR_24_M\""
				+ "}",
			SaveKnotenCommand.class);

		assertThatExceptionOfType(AccessDeniedException.class)
			.isThrownBy(() -> netzController.saveKnoten(authentication, command));
	}
}
