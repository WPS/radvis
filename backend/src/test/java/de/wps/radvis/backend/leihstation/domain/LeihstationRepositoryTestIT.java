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

package de.wps.radvis.backend.leihstation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.leihstation.LeihstationConfiguration;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.valueObject.Anzahl;
import de.wps.radvis.backend.leihstation.domain.valueObject.ExterneLeihstationenId;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationStatus;
import de.wps.radvis.backend.leihstation.domain.valueObject.UrlAdresse;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;

@Tag("group6")
@ContextConfiguration(classes = { LeihstationConfiguration.class, })
@EnableConfigurationProperties(value = CommonConfigurationProperties.class)
@MockBeans({
	@MockBean(ZustaendigkeitsService.class),
	@MockBean(BenutzerResolver.class),
})
class LeihstationRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	private LeihstationRepository leihstationRepository;

	@MockBean
	VerwaltungseinheitService verwaltungseinheitService;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void testSaveAndGet() {
		Leihstation leihstation = new Leihstation(
			GeometryTestdataProvider.createPoint(new Coordinate(0, 0)),
			"betreiberString",
			Anzahl.of(1),
			Anzahl.of(2),
			Anzahl.of(3),
			false,
			UrlAdresse.of("https://someurl.com"),
			LeihstationStatus.AKTIV,
			LeihstationQuellSystem.RADVIS,
			null);
		Leihstation leihstationSaved = leihstationRepository.save(leihstation);

		entityManager.flush();
		entityManager.clear();

		Optional<Leihstation> byId = leihstationRepository.findById(leihstationSaved.getId());

		assertThat(byId).isPresent();
		assertThat(byId.get())
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringFields("id")
			.isEqualTo(leihstation);
	}

	@Test
	void findByPosition() {
		Leihstation leihstation = leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation().geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 100)))
			.build());

		entityManager.flush();
		entityManager.clear();

		Optional<Leihstation> byPosition = leihstationRepository.findByPositionAndQuellSystemRadVis(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 100)));
		assertThat(byPosition).isNotEmpty();
		assertThat(byPosition.get()).usingRecursiveComparison().usingOverriddenEquals().isEqualTo(leihstation);

		assertThat(leihstationRepository.findByPositionAndQuellSystemRadVis(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(98.9, 100))))
			.isEmpty();

		assertThat(leihstationRepository.findByPositionAndQuellSystemRadVis(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 98.9))))
			.isEmpty();

		assertThat(leihstationRepository.findByPositionAndQuellSystemRadVis(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(99.5, 99.5))))
			.isNotEmpty();
	}

	@Test
	void findByPosition_twoInRange() {
		leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation()
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(100, 100)))
			.build());
		leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation()
			.geometrie(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(99, 99)))
			.build());

		entityManager.flush();
		entityManager.clear();

		assertDoesNotThrow(() -> leihstationRepository.findByPositionAndQuellSystemRadVis(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(99.5, 99.5))));
	}

	@Test
	void findByExterneIdAndQuellSystem() {
		// Arrange
		Leihstation mobiStation1 = leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("MobiStation1"))
			.build());
		Leihstation mobiStation2 = leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("MobiStation2"))
			.build());
		Leihstation radvisStation = leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation()
			.quellSystem(LeihstationQuellSystem.RADVIS)
			.build());
		entityManager.flush();
		entityManager.clear();
		assertThat(leihstationRepository.findAll()).containsExactlyInAnyOrderElementsOf(Set.of(
			mobiStation1, mobiStation2, radvisStation));

		// Act + Assert
		assertThat(leihstationRepository.findByExterneIdAndQuellSystem(
			ExterneLeihstationenId.of("MobiStation1"), LeihstationQuellSystem.MOBIDATABW))
			.contains(mobiStation1);

	}

	@Test
	void deleteAlteMobidataLeihstationen() {
		// Arrange
		Leihstation alteRADVIS = leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation()
			.quellSystem(LeihstationQuellSystem.RADVIS)
			.build());
		Leihstation alteMobiBleibt = leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("bleibt1"))
			.build());
		Leihstation alteMobiFliegtraus1 = leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("fliegt2"))
			.build());
		Leihstation alteMobiFliegtraus2 = leihstationRepository.save(LeihstationTestDataProvider.defaultLeihstation()
			.quellSystem(LeihstationQuellSystem.MOBIDATABW)
			.externeId(ExterneLeihstationenId.of("fliegt3"))
			.build());

		entityManager.flush();
		entityManager.clear();

		assertThat(leihstationRepository.findAll()).containsExactlyInAnyOrderElementsOf(Set.of(
			alteRADVIS, alteMobiBleibt, alteMobiFliegtraus1, alteMobiFliegtraus2));

		//Act
		Set<ExterneLeihstationenId> aktuelleStationen = Set.of(
			ExterneLeihstationenId.of("bleibt1"),
			ExterneLeihstationenId.of("neu3"));

		int deleted = leihstationRepository.deleteByExterneIdNotInAndQuellSystem(aktuelleStationen,
			LeihstationQuellSystem.MOBIDATABW);

		entityManager.flush();
		entityManager.clear();

		//Assert
		assertThat(deleted).isEqualTo(2);
		final Iterable<Leihstation> result = leihstationRepository.findAll();
		assertThat(result).containsExactlyInAnyOrderElementsOf(Set.of(
			alteRADVIS,
			alteMobiBleibt
		));
		assertThat(result).doesNotContainAnyElementsOf(Set.of(alteMobiFliegtraus1, alteMobiFliegtraus2));

	}
}