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

package de.wps.radvis.backend.netz.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import jakarta.persistence.EntityManager;

@Tag("group1")
@EntityScan(basePackageClasses = { NetzConfiguration.class, BenutzerConfiguration.class,
	OrganisationConfiguration.class })
@EnableJpaRepositories(basePackageClasses = { NetzConfiguration.class, BenutzerConfiguration.class,
	OrganisationConfiguration.class }, repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@MockBean({ FeatureToggleProperties.class, NetzConfigurationProperties.class })
@EnableEnversRepositories(basePackages = { "de.wps.radvis.backend.netz", })
class ZustaendigkeitAttributGruppeRepositoryIntegrationTestIT extends DBIntegrationTestIT {
	@Autowired
	KantenRepository kantenRepository;
	@Autowired
	ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppeRepository;
	@Autowired
	EntityManager entityManager;

	@Test
	void findById_returnsExisting() {
		// arrange
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		entityManager.flush();
		entityManager.clear();

		// act
		Optional<ZustaendigkeitAttributGruppe> findById = zustaendigkeitAttributGruppeRepository
			.findById(kante1.getZustaendigkeitAttributGruppe().getId());

		// assert
		assertThat(findById).contains(kante1.getZustaendigkeitAttributGruppe());
		assertThat(findById.get()).isNotEqualTo(kante2.getZustaendigkeitAttributGruppe());
	}

	@Test
	void findById_empty() {
		// arrange
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		entityManager.flush();
		entityManager.clear();

		// act
		Optional<ZustaendigkeitAttributGruppe> findById = zustaendigkeitAttributGruppeRepository
			.findById(kante1.getFuehrungsformAttributGruppe().getId() + 1);

		// assert
		assertThat(findById).isEmpty();
	}

	@Test
	void findAllWithSegmenteKleinerAls_filtersCorrectly() {
		// arrange
		Kante kanteWithMiniSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.01, 1).build()))
					.build())
				.build());
		Kante kanteWithOneSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1).build())).build())
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<ZustaendigkeitAttributGruppe> findAllWithSegmenteKleinerAls = zustaendigkeitAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(1.1);

		// assert
		assertThat(findAllWithSegmenteKleinerAls).hasSize(1);
		assertThat(findAllWithSegmenteKleinerAls).contains(kanteWithMiniSegment.getZustaendigkeitAttributGruppe());
		assertThat(findAllWithSegmenteKleinerAls)
			.doesNotContain(kanteWithOneSegment.getZustaendigkeitAttributGruppe());
	}

	@Test
	void findAllWithSegmenteKleinerAls_filtersEqualSegmentlength() {
		// arrange
		Kante kanteWithMiniSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.01, 1).build()))
					.build())
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<ZustaendigkeitAttributGruppe> findAllWithSegmenteKleinerAls = zustaendigkeitAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(1.0);

		// assert
		assertThat(findAllWithSegmenteKleinerAls).hasSize(0);
		assertThat(findAllWithSegmenteKleinerAls)
			.doesNotContain(kanteWithMiniSegment.getZustaendigkeitAttributGruppe());
	}

	@Test
	void findAllWithSegmenteKleinerAls_filtersQuelle() {
		// arrange
		Kante kanteDlm = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.01, 1).build()))
					.build())
				.build());
		Kante kanteRadnetz = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.RadVis)
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.01, 1).build()))
					.build())
				.build());
		Kante kanteSonstige = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.BietigheimBissingen)
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.01, 1).build()))
					.build())
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<ZustaendigkeitAttributGruppe> findAllWithSegmenteKleinerAls = zustaendigkeitAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(1.1);

		// assert
		assertThat(findAllWithSegmenteKleinerAls).contains(kanteDlm.getZustaendigkeitAttributGruppe(),
			kanteRadnetz.getZustaendigkeitAttributGruppe());
		assertThat(findAllWithSegmenteKleinerAls).doesNotContain(kanteSonstige.getZustaendigkeitAttributGruppe());
	}

	@Test
	void findAllWithSegmenteKleinerAls_usesLengthFromParam() {
		// arrange
		Kante kanteWithMiniSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.1).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.1, 1).build()))
					.build())
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<ZustaendigkeitAttributGruppe> findAllWithSegmenteKleinerAls20 = zustaendigkeitAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(20.0);
		List<ZustaendigkeitAttributGruppe> findAllWithSegmenteKleinerAls5 = zustaendigkeitAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(5);

		// assert
		assertThat(findAllWithSegmenteKleinerAls20).hasSize(1);
		assertThat(findAllWithSegmenteKleinerAls20).contains(kanteWithMiniSegment.getZustaendigkeitAttributGruppe());

		assertThat(findAllWithSegmenteKleinerAls5).isEmpty();
	}

	@Test
	void findAllWithSegmenteKleinerAls_returnsDistinct() {
		// arrange
		Kante kanteWithMiniSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.01, 0.02).build(),
						ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.02, 1).build()))
					.build())
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<ZustaendigkeitAttributGruppe> findAllWithSegmenteKleinerAls = zustaendigkeitAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(1.1);

		// assert
		assertThat(findAllWithSegmenteKleinerAls).hasSize(1);
		assertThat(findAllWithSegmenteKleinerAls).contains(kanteWithMiniSegment.getZustaendigkeitAttributGruppe());
	}
}
