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

package de.wps.radvis.backend.basicAuthentication.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.schnittstelle.RadVisUserDetailsService;
import de.wps.radvis.backend.basicAuthentication.BasicAuthenticationConfiguration;
import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthBenutzerRepository;
import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthPasswortService;
import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthenticationConfigurationProperties;
import de.wps.radvis.backend.basicAuthentication.domain.BenutzerBasicAuthView;
import de.wps.radvis.backend.basicAuthentication.domain.entity.BenutzerBasicAuth;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.schnittstelle.BenutzerResolverImpl;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group4")
@ContextConfiguration(classes = {
	BenutzerBasicAuthControllerTestIT.TestConfiguration.class,
})
@MockBeans({
	@MockBean(CommonConfigurationProperties.class),
})
public class BenutzerBasicAuthControllerTestIT extends DBIntegrationTestIT {

	@Configuration
	@EnableJpaRepositories(basePackages = {
		"de.wps.radvis.backend.basicAuthentication",
		"de.wps.radvis.backend.common",
		"de.wps.radvis.backend.benutzer",
		"de.wps.radvis.backend.organisation",
	})
	@EntityScan(basePackageClasses = {
		BasicAuthenticationConfiguration.class,
		BenutzerConfiguration.class,
		OrganisationConfiguration.class,
	})
	public static class TestConfiguration {
		@Bean
		public BasicAuthPasswortService basicAuthPasswortService() {
			BCryptPasswordEncoder encoderMock = mock(BCryptPasswordEncoder.class);
			when(encoderMock.encode(any())).thenReturn("testPasswortHashed");

			BasicAuthenticationConfigurationProperties basicAuthenticationConfigurationProperties = mock(
				BasicAuthenticationConfigurationProperties.class);
			when(basicAuthenticationConfigurationProperties.getPasswordLength()).thenReturn(20);

			return new BasicAuthPasswortService(encoderMock, new SecureRandom(),
				basicAuthenticationConfigurationProperties);
		}

		@Bean
		public BenutzerResolver benutzerResolver() {
			return new BenutzerResolverImpl();
		}
	}

	@Autowired
	BenutzerRepository benutzerRepository;

	@Autowired
	GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	BenutzerResolver benutzerResolver;

	@Autowired
	BasicAuthBenutzerRepository basicAuthBenutzerRepository;

	@Autowired
	BasicAuthPasswortService basicAuthPasswortService;

	BenutzerBasicAuthController benutzerBasicAuthController;

	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		benutzerBasicAuthController = new BenutzerBasicAuthController(benutzerResolver,
			basicAuthPasswortService, basicAuthBenutzerRepository);
	}

	@Test
	void testGenerateBasicAuth() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.defaultBenutzer()
			.organisation(gebietskoerperschaft)
			.build());

		entityManager.flush();
		entityManager.clear();

		Benutzer loadedBenutzer = benutzerRepository.findById(benutzer.getId()).get();
		Authentication authentication = new RadVisAuthentication(RadVisUserDetailsService.fromUser(loadedBenutzer));

		// act
		BenutzerBasicAuthView benutzerBasicAuthView = benutzerBasicAuthController
			.generateBasicAuth(authentication);

		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(benutzerBasicAuthView.getBenutzername()).isEqualTo(loadedBenutzer.getBasicAuthAnmeldeName());

		List<BenutzerBasicAuth> benutzerBasicAuthList = StreamSupport
			.stream(basicAuthBenutzerRepository.findAll().spliterator(), false)
			.collect(Collectors.toList());
		assertThat(benutzerBasicAuthList).hasSize(1);

		assertThat(benutzerBasicAuthList.get(0).getPasswortHash()).isEqualTo("testPasswortHashed");

		Benutzer benutzerAnDerBasicAuthEntity = benutzerBasicAuthList.get(0).getBenutzer();
		assertThat(benutzerAnDerBasicAuthEntity).isNotNull();
		assertThat(benutzerAnDerBasicAuthEntity.getId())
			.isEqualTo(benutzerBasicAuthList.get(0).getBenutzerId())
			.isEqualTo(benutzer.getId());
	}

	@Test
	void testGenerateBasicAuth_alreadyExistingForBenutzer_replacyByNew() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.defaultBenutzer()
			.organisation(gebietskoerperschaft)
			.build());

		basicAuthBenutzerRepository.save(new BenutzerBasicAuth(benutzer.getId(), "OldPasswortHashed"));

		entityManager.flush();
		entityManager.clear();

		Benutzer loadedBenutzer = benutzerRepository.findById(benutzer.getId()).get();
		Authentication authentication = new RadVisAuthentication(RadVisUserDetailsService.fromUser(loadedBenutzer));

		// act
		BenutzerBasicAuthView benutzerBasicAuthView = benutzerBasicAuthController
			.generateBasicAuth(authentication);

		entityManager.flush();
		entityManager.clear();

		// assert
		assertThat(benutzerBasicAuthView.getBenutzername()).isEqualTo(loadedBenutzer.getBasicAuthAnmeldeName());

		List<BenutzerBasicAuth> benutzerBasicAuthList = StreamSupport
			.stream(basicAuthBenutzerRepository.findAll().spliterator(), false)
			.collect(Collectors.toList());
		assertThat(benutzerBasicAuthList).hasSize(1);

		assertThat(benutzerBasicAuthList.get(0).getPasswortHash()).isEqualTo("testPasswortHashed");

		Benutzer benutzerAnDerBasicAuthEntity = benutzerBasicAuthList.get(0).getBenutzer();
		assertThat(benutzerAnDerBasicAuthEntity).isNotNull();
		assertThat(benutzerAnDerBasicAuthEntity.getId())
			.isEqualTo(benutzerBasicAuthList.get(0).getBenutzerId())
			.isEqualTo(benutzer.getId());
	}
}
