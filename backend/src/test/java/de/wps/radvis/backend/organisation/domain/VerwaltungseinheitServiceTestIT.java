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

package de.wps.radvis.backend.organisation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;

@Tag("group1")
@EnableJpaRepositories(basePackageClasses = OrganisationConfiguration.class)
@EntityScan(basePackageClasses = OrganisationConfiguration.class)
public class VerwaltungseinheitServiceTestIT extends DBIntegrationTestIT {
	private VerwaltungseinheitService service;

	@Autowired
	private VerwaltungseinheitRepository repository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private OrganisationRepository organisationRepository;
	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setup() {
		this.service = new VerwaltungseinheitService(repository, gebietskoerperschaftRepository,
			organisationRepository, OrganisationsArt.BUNDESLAND, "Baden-Württemberg");
	}

	@Test
	void getAllNames() {
		// arrange
		Gebietskoerperschaft oberste = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Oberste")
				.organisationsArt(OrganisationsArt.BUNDESLAND).uebergeordneteOrganisation(null).build());
		Gebietskoerperschaft untere = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Untere")
				.organisationsArt(OrganisationsArt.GEMEINDE).uebergeordneteOrganisation(oberste)
				.build());
		gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Nicht drin")
				.organisationsArt(OrganisationsArt.GEMEINDE).uebergeordneteOrganisation(oberste)
				.build());

		entityManager.flush();

		// act
		String result = service.getAllNames(List.of(oberste.getId(), untere.getId()));

		// assert
		assertThat(result).isEqualTo("Oberste (Bundesland), Untere (Gemeinde)");
	}
}
