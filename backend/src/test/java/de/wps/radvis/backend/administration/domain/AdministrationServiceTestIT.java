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

package de.wps.radvis.backend.administration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group6")
@ContextConfiguration(classes = AdministrationServiceTestIT.AdministrationServiceTestConfiguration.class)
@EnableJpaRepositories(basePackageClasses = OrganisationConfiguration.class)
@EntityScan(basePackageClasses = OrganisationConfiguration.class)
class AdministrationServiceTestIT extends DBIntegrationTestIT {
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private OrganisationRepository organisationRepository;
	@Autowired
	private AdministrationService administrationService;
	@PersistenceContext
	private EntityManager entityManager;

	private Gebietskoerperschaft uebergeordneteVerwEinheit;
	private Gebietskoerperschaft verwEinheit;
	private Gebietskoerperschaft untergeordneteVerwEinheit;

	@Configuration
	static class AdministrationServiceTestConfiguration {
		@Autowired
		private VerwaltungseinheitRepository verwaltungseinheitRepository;
		@Autowired
		private OrganisationRepository organisationRepository;

		@Bean
		public AdministrationService administrationService() {
			return new AdministrationService(verwaltungseinheitRepository, organisationRepository);
		}
	}

	@BeforeEach
	void setup() {
		uebergeordneteVerwEinheit = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft().build();
		verwEinheit = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft().uebergeordneteOrganisation(uebergeordneteVerwEinheit).build();
		untergeordneteVerwEinheit = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft().uebergeordneteOrganisation(verwEinheit).build();

		gebietskoerperschaftRepository
			.saveAll(List.of(verwEinheit, untergeordneteVerwEinheit, uebergeordneteVerwEinheit));

		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void getAllZuweisbareOrganisationenForBenutzer_kreiskoordinator_nurUntergeordnete() {
		// Arrange
		Benutzer benutzer = BenutzerTestDataProvider.kreiskoordinator(verwEinheit).build();

		// Act
		List<VerwaltungseinheitDbView> allZuweisbareOrganisationenForBenutzer = administrationService
			.getAllZuweisbareOrganisationenForBenutzer(benutzer);

		// Assert
		assertThat(allZuweisbareOrganisationenForBenutzer).containsExactlyInAnyOrder(
			new VerwaltungseinheitDbView(verwEinheit), new VerwaltungseinheitDbView(untergeordneteVerwEinheit));
	}

	@Test
	public void getAllZuweisbareForBenutzer_admin_allGebietskoerperschaften() {
		// Arrange
		Benutzer benutzer = BenutzerTestDataProvider.admin(verwEinheit).build();

		// Act
		List<VerwaltungseinheitDbView> allZuweisbareOrganisationenForBenutzer = administrationService
			.getAllZuweisbareOrganisationenForBenutzer(benutzer);

		// Assert
		assertThat(allZuweisbareOrganisationenForBenutzer).containsExactlyInAnyOrder(
			new VerwaltungseinheitDbView(verwEinheit), new VerwaltungseinheitDbView(untergeordneteVerwEinheit),
			new VerwaltungseinheitDbView(uebergeordneteVerwEinheit));
	}

	@Test
	public void getAllZuweisbareForBenutzer_bearbeiterGebietskoerperschaft_ownOrg() {
		// Arrange
		Benutzer benutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(verwEinheit).build();

		// Act
		List<VerwaltungseinheitDbView> allZuweisbareOrganisationenForBenutzer = administrationService
			.getAllZuweisbareOrganisationenForBenutzer(benutzer);

		// Assert
		assertThat(allZuweisbareOrganisationenForBenutzer).containsExactlyInAnyOrder(
			new VerwaltungseinheitDbView(verwEinheit));

	}

	@Test
	public void getAllZuweisbareForBenutzer_bearbeiterOrg_noOrg() {
		// Arrange
		Organisation organisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().build();
		organisationRepository.save(organisation);
		entityManager.flush();
		entityManager.clear();

		Benutzer benutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(organisation).build();

		// Act
		List<VerwaltungseinheitDbView> allZuweisbareOrganisationenForBenutzer = administrationService
			.getAllZuweisbareOrganisationenForBenutzer(benutzer);

		// Assert
		assertThat(allZuweisbareOrganisationenForBenutzer).isEmpty();
	}

	@Test
	public void getAllZuweisbareForBenutzer_externerBearbeiter_noOrg() {
		// Arrange
		Organisation organisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().build();
		organisationRepository.save(organisation);
		entityManager.flush();
		entityManager.clear();

		Benutzer benutzer = BenutzerTestDataProvider.externerDienstleister(verwEinheit).build();

		// Act
		List<VerwaltungseinheitDbView> allZuweisbareOrganisationenForBenutzer = administrationService
			.getAllZuweisbareOrganisationenForBenutzer(benutzer);

		// Assert
		assertThat(allZuweisbareOrganisationenForBenutzer).isEmpty();
	}

}
