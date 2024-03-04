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

package de.wps.radvis.backend.basicAuthentication.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.basicAuthentication.domain.entity.BenutzerBasicAuth;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
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
	BasicAuthBenutzerRepositoryTestIT.TestConfiguration.class,
})
@MockBeans({
	@MockBean(CommonConfigurationProperties.class),
})
public class BasicAuthBenutzerRepositoryTestIT extends DBIntegrationTestIT {

	@Configuration
	@EnableJpaRepositories(basePackages = {
		"de.wps.radvis.backend.common",
		"de.wps.radvis.backend.benutzer",
		"de.wps.radvis.backend.organisation",
		"de.wps.radvis.backend.basicAuthentication",
	})
	@EntityScan(basePackageClasses = {
		BenutzerConfiguration.class,
		OrganisationConfiguration.class,
		BenutzerBasicAuth.class,
	})
	public static class TestConfiguration {
	}

	@Autowired
	BenutzerRepository benutzerRepository;

	@Autowired
	GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	BasicAuthBenutzerRepository basicAuthBenutzerRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testFindByBenutzer_MailadresseAndStatusAktiv_happy() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.defaultBenutzer()
			.organisation(gebietskoerperschaft)
			.build());

		basicAuthBenutzerRepository.save(new BenutzerBasicAuth(benutzer.getId(), "passwortHashed"));

		entityManager.flush();
		entityManager.clear();

		// act
		Benutzer loadedBenutzer = benutzerRepository.findById(benutzer.getId()).get();
		String basicAuthAnmeldeName = loadedBenutzer.getBasicAuthAnmeldeName();
		Optional<BenutzerBasicAuth> benutzerBasicAuth = basicAuthBenutzerRepository
			.findByBenutzerAnmeldenameAndStatusAktiv(basicAuthAnmeldeName);

		// assert
		assertThat(benutzerBasicAuth).isPresent();
		assertThat(benutzerBasicAuth.get().getBenutzerId()).isEqualTo(benutzer.getId());
		assertThat(benutzerBasicAuth.get().getPasswortHash()).isEqualTo("passwortHashed");
		assertThat(basicAuthAnmeldeName).isEqualTo("rr" + loadedBenutzer.getId());
	}

	@Test
	void testFindByBenutzer_MailadresseAndStatusAktiv_inaktiv_resultEmpty() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.defaultBenutzer()
			.organisation(gebietskoerperschaft)
			.status(BenutzerStatus.INAKTIV)
			.build());

		basicAuthBenutzerRepository.save(new BenutzerBasicAuth(benutzer.getId(), "passwortHashed"));

		entityManager.flush();
		entityManager.clear();

		// act + assert
		String basicAuthAnmeldeName = benutzerRepository.findById(benutzer.getId()).get().getBasicAuthAnmeldeName();
		assertThat(basicAuthBenutzerRepository.findByBenutzerAnmeldenameAndStatusAktiv(basicAuthAnmeldeName))
			.isEmpty();
	}

	@Test
	void testFindByBenutzer_MailadresseAndStatusAktiv_leereEmail() {
		// arrange
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Benutzer benutzer = benutzerRepository.save(BenutzerTestDataProvider.defaultBenutzer()
			.organisation(gebietskoerperschaft)
			.build());

		basicAuthBenutzerRepository.save(new BenutzerBasicAuth(benutzer.getId(), "passwortHashed"));

		entityManager.flush();
		entityManager.clear();

		// act + assert
		assertThat(basicAuthBenutzerRepository.findByBenutzerAnmeldenameAndStatusAktiv(null)).isEmpty();
	}
}
