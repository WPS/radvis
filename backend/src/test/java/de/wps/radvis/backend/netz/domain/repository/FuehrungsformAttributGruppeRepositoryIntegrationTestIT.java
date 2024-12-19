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
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import jakarta.persistence.EntityManager;

@Tag("group1")
@EntityScan(basePackageClasses = { NetzConfiguration.class, BenutzerConfiguration.class,
	OrganisationConfiguration.class })
@EnableJpaRepositories(basePackageClasses = { NetzConfiguration.class, BenutzerConfiguration.class,
	OrganisationConfiguration.class }, repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@MockBean({ FeatureToggleProperties.class, NetzConfigurationProperties.class })
@EnableEnversRepositories(basePackages = { "de.wps.radvis.backend.netz", })
class FuehrungsformAttributGruppeRepositoryIntegrationTestIT extends DBIntegrationTestIT {
	@Autowired
	KantenRepository kantenRepository;
	@Autowired
	FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppeRepository;
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
		Optional<FuehrungsformAttributGruppe> findById = fuehrungsformAttributGruppeRepository
			.findById(kante1.getFuehrungsformAttributGruppe().getId());

		// assert
		assertThat(findById).contains(kante1.getFuehrungsformAttributGruppe());
		assertThat(findById.get()).isNotEqualTo(kante2.getFuehrungsformAttributGruppe());
	}

	@Test
	void findById_empty() {
		// arrange
		Kante kante1 = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());

		entityManager.flush();
		entityManager.clear();

		// act
		Optional<FuehrungsformAttributGruppe> findById = fuehrungsformAttributGruppeRepository
			.findById(kante1.getFuehrungsformAttributGruppe().getId() + 1);

		// assert
		assertThat(findById).isEmpty();
	}

	@Test
	void findAllWithSegmenteKleinerAls_filtersCorrectly() {
		// arrange
		Kante kanteWithMiniSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.01, 1).build()),
					false))
				.build());
		Kante kanteWithOneSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build()), false))
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<FuehrungsformAttributGruppe> findAllWithSegmenteKleinerAls = fuehrungsformAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(1.1);

		// assert
		assertThat(findAllWithSegmenteKleinerAls).hasSize(1);
		assertThat(findAllWithSegmenteKleinerAls).contains(kanteWithMiniSegment.getFuehrungsformAttributGruppe());
		assertThat(findAllWithSegmenteKleinerAls).doesNotContain(kanteWithOneSegment.getFuehrungsformAttributGruppe());
	}

	@Test
	void findAllWithSegmenteKleinerAls_filtersEqualSegmentlength() {
		// arrange
		Kante kanteWithMiniSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.01, 1).build()),
					false))
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<FuehrungsformAttributGruppe> findAllWithSegmenteKleinerAls = fuehrungsformAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(1.0);

		// assert
		assertThat(findAllWithSegmenteKleinerAls).hasSize(0);
		assertThat(findAllWithSegmenteKleinerAls).doesNotContain(kanteWithMiniSegment.getFuehrungsformAttributGruppe());
	}

	@Test
	void findAllWithSegmenteKleinerAls_withZweiseitig() {
		// arrange
		Kante kanteWithMiniSegmentLinks = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.isZweiseitig(true)
				.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.01, 1).build()),
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build()),
					true))
				.build());
		Kante kanteWithMiniSegmentRechts = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
				.isZweiseitig(true)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build()),
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.01, 1).build()),
					true))
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<FuehrungsformAttributGruppe> findAllWithSegmenteKleinerAls = fuehrungsformAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(1.1);

		// assert
		assertThat(findAllWithSegmenteKleinerAls).containsExactlyInAnyOrder(
			kanteWithMiniSegmentLinks.getFuehrungsformAttributGruppe(),
			kanteWithMiniSegmentRechts.getFuehrungsformAttributGruppe());
	}

	@Test
	void findAllWithSegmenteKleinerAls_filtersQuelle() {
		// arrange
		Kante kanteDlm = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.01, 1).build()),
					false))
				.build());
		Kante kanteRadnetz = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.RadVis)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.01, 1).build()),
					false))
				.build());
		Kante kanteSonstige = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.BietigheimBissingen)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.01, 1).build()),
					false))
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<FuehrungsformAttributGruppe> findAllWithSegmenteKleinerAls = fuehrungsformAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(1.1);

		// assert
		assertThat(findAllWithSegmenteKleinerAls).contains(kanteDlm.getFuehrungsformAttributGruppe(),
			kanteRadnetz.getFuehrungsformAttributGruppe());
		assertThat(findAllWithSegmenteKleinerAls).doesNotContain(kanteSonstige.getFuehrungsformAttributGruppe());
	}

	@Test
	void findAllWithSegmenteKleinerAls_usesLengthFromParam() {
		// arrange
		Kante kanteWithMiniSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 1).build()),
					false))
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<FuehrungsformAttributGruppe> findAllWithSegmenteKleinerAls20 = fuehrungsformAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(20.0);
		List<FuehrungsformAttributGruppe> findAllWithSegmenteKleinerAls5 = fuehrungsformAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(5.0);

		// assert
		assertThat(findAllWithSegmenteKleinerAls20).hasSize(1);
		assertThat(findAllWithSegmenteKleinerAls20).contains(kanteWithMiniSegment.getFuehrungsformAttributGruppe());

		assertThat(findAllWithSegmenteKleinerAls5).isEmpty();
	}

	@Test
	void findAllWithSegmenteKleinerAls_returnsDistinct() {
		// arrange
		Kante kanteWithMiniSegment = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(
					List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.01).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.01, 0.02).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.02, 1).build()),
					false))
				.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<FuehrungsformAttributGruppe> findAllWithSegmenteKleinerAls = fuehrungsformAttributGruppeRepository
			.findAllWithSegmenteKleinerAls(1.1);

		// assert
		assertThat(findAllWithSegmenteKleinerAls).hasSize(1);
		assertThat(findAllWithSegmenteKleinerAls).contains(kanteWithMiniSegment.getFuehrungsformAttributGruppe());
	}
}
