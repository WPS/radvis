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

package de.wps.radvis.backend.organisation.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.assertj.core.groups.Tuple;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

@Tag("group1")
@ContextConfiguration(classes = { VerwaltungseinheitRepositoryTestIT.TestConfiguration.class })
class VerwaltungseinheitRepositoryTestIT extends DBIntegrationTestIT {

	@Configuration
	@EnableJpaRepositories(basePackages = "de.wps.radvis.backend.organisation")
	@EntityScan({ "de.wps.radvis.backend.organisation.domain.entity",
		"de.wps.radvis.backend.organisation.domain.valueObject" })
	public static class TestConfiguration {
	}

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Autowired
	private VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	private OrganisationRepository organisationRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@PersistenceContext
	EntityManager entityManager;

	@SuppressWarnings("unchecked")
	@Test
	public void saveAndGet() {
		// Arrange
		Polygon teilBereich = GEO_FACTORY.createPolygon(
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 10), new Coordinate(10, 10),
				new Coordinate(10, 0), new Coordinate(0, 0) });
		MultiPolygon bereich = GEO_FACTORY.createMultiPolygon(new Polygon[] { teilBereich });

		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Utopia")
			.fachId(12)
			.organisationsArt(OrganisationsArt.BUNDESLAND).bereich(bereich).build();

		// Act
		Long gebietsKoerperschaftId = gebietskoerperschaftRepository.save(gebietskoerperschaft).getId();

		entityManager.flush();
		entityManager.clear();

		Optional<Verwaltungseinheit> result = verwaltungseinheitRepository.findById(gebietsKoerperschaftId);

		// Assert
		assertThat(result).isPresent();
		Gebietskoerperschaft resultOrganisation = (Gebietskoerperschaft) result.get();
		assertThat(resultOrganisation.getBereich()).isPresent();
		assertThat(resultOrganisation.getBereich().get()).isEqualTo(bereich);
		assertThat(resultOrganisation.getFachId()).isEqualTo(12);
		assertThat(resultOrganisation.getName()).isEqualTo("Utopia");
		assertThat(resultOrganisation.getUebergeordneteVerwaltungseinheit()).isEmpty();
	}

	@Test
	public void OrganisationView() {
		// Arrange
		Polygon teilBereich = GEO_FACTORY.createPolygon(
			new Coordinate[] { new Coordinate(0, 0), new Coordinate(0, 10), new Coordinate(10, 10),
				new Coordinate(10, 0), new Coordinate(0, 0) });
		MultiPolygon bereich = GEO_FACTORY.createMultiPolygon(new Polygon[] { teilBereich });

		Gebietskoerperschaft gebietskoerperschaft1 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Utopia")
			.fachId(12)
			.organisationsArt(OrganisationsArt.BUNDESLAND).bereich(bereich).build();
		Long gebietskoerperschaftId1 = gebietskoerperschaftRepository.save(gebietskoerperschaft1).getId();

		Gebietskoerperschaft gebietskoerperschaft2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Dystopia")
			.fachId(13)
			.uebergeordneteOrganisation(gebietskoerperschaft1)
			.organisationsArt(OrganisationsArt.KREIS).bereich(bereich).build();
		Long gebietskoerperschaftId2 = gebietskoerperschaftRepository.save(gebietskoerperschaft2).getId();

		// Act

		entityManager.flush();
		entityManager.clear();

		List<VerwaltungseinheitDbView> result = verwaltungseinheitRepository.findAllAsView();

		// Assert
		assertThat(result)
			.extracting("id", "name", "organisationsArt", "idUebergeordneteOrganisation", "aktiv")
			.containsExactlyInAnyOrder(
				Tuple.tuple(gebietskoerperschaftId1, "Utopia", OrganisationsArt.BUNDESLAND, null, true),
				Tuple.tuple(gebietskoerperschaftId2, "Dystopia", OrganisationsArt.KREIS, gebietskoerperschaftId1,
					true));
	}

	@Test
	public void testFindAllByOrganisationsArtContainingGeometry() {
		// Arrange
		MultiPolygon bereichDerDieGeometrieNichtEnthaelt = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 10,
			10);

		Gebietskoerperschaft gebietskoerperschaft1 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Utopia")
			.fachId(12)
			.organisationsArt(OrganisationsArt.KREIS).bereich(bereichDerDieGeometrieNichtEnthaelt).build();
		gebietskoerperschaftRepository.save(gebietskoerperschaft1).getId();

		MultiPolygon bereichDerDieGeometrieEnthaelt = GeometryTestdataProvider.createQuadratischerBereich(30, 30, 50,
			50);
		Gebietskoerperschaft gebietskoerperschaft2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Dystopia")
			.fachId(13)
			.uebergeordneteOrganisation(gebietskoerperschaft1)
			.organisationsArt(OrganisationsArt.KREIS).bereich(bereichDerDieGeometrieEnthaelt).build();
		Long organisationDieDieGeometrieEnthaelt = gebietskoerperschaftRepository.save(gebietskoerperschaft2).getId();

		// Act

		entityManager.flush();
		entityManager.clear();

		List<Verwaltungseinheit> result = verwaltungseinheitRepository.findAllByOrganisationsArtContainingGeometry(
			OrganisationsArt.KREIS,
			GeometryTestdataProvider.createLineString(new Coordinate(20, 20), new Coordinate(40, 40)));

		// Assert
		assertThat(result)
			.extracting(Verwaltungseinheit::getId)
			.containsExactly(organisationDieDieGeometrieEnthaelt);
	}

	@Test
	public void testFindAllUntergeordnet() {
		// Arrange

		Verwaltungseinheit top1 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Verwaltungseinheit sub1_1 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().uebergeordneteOrganisation(top1)
				.build());

		Verwaltungseinheit sub1_2 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().uebergeordneteOrganisation(top1)
				.build());

		Verwaltungseinheit sub1_2_1 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().uebergeordneteOrganisation(sub1_2)
				.build());

		Verwaltungseinheit top2 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Verwaltungseinheit sub2_1 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().uebergeordneteOrganisation(top2)
				.build());

		Verwaltungseinheit sub2_2 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().uebergeordneteOrganisation(top2)
				.build());

		Verwaltungseinheit sub2_1_1 = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().uebergeordneteOrganisation(sub2_1)
				.build());

		// Act

		entityManager.flush();
		entityManager.clear();

		List<Verwaltungseinheit> untergeordnetTop1 = verwaltungseinheitRepository.findAllUntergeordnet(top1);
		List<Verwaltungseinheit> untergeordnetTop2 = verwaltungseinheitRepository.findAllUntergeordnet(top2);
		List<Verwaltungseinheit> untergeordnetSub2_1 = verwaltungseinheitRepository.findAllUntergeordnet(sub2_1);

		// Assert
		assertThat(untergeordnetTop1)
			.containsExactlyInAnyOrder(top1, sub1_1, sub1_2, sub1_2_1);

		assertThat(untergeordnetTop2)
			.containsExactlyInAnyOrder(top2, sub2_1, sub2_2, sub2_1_1);

		assertThat(untergeordnetSub2_1)
			.containsExactlyInAnyOrder(sub2_1, sub2_1_1);
	}

	// Dieser Test dient dazu, das Verhalten von Hibernate zu erklären, das Problem lässt sich nicht global verhindern
	@Test
	void proxyproblem() {
		Verwaltungseinheit topOrg = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Verwaltungseinheit subOrg = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().uebergeordneteOrganisation(topOrg)
				.build());

		entityManager.flush();
		entityManager.clear();

		// Wir holen die Untergeordnete in den Context,
		// dadurch ist mit der ID der übergeordneten der Proxy registriert
		verwaltungseinheitRepository.findById(subOrg.getId());
		// Wir holen die übergeordnete und kriegen den Proxy
		Optional<Verwaltungseinheit> findById = verwaltungseinheitRepository.findById(topOrg.getId());

		assertThat(findById).isPresent();
		// wir müssen das unproxy-en
		assertThat(Hibernate.unproxy(findById.get())).isInstanceOf(Gebietskoerperschaft.class);
	}

	@Test
	void findById_isEmptyForWrongType() {
		Gebietskoerperschaft gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		Organisation organisation = organisationRepository
			.save(VerwaltungseinheitTestDataProvider.defaultOrganisation().build());

		assertThat(organisationRepository.findById(gebietskoerperschaft.getId())).isEmpty();
		assertThat(gebietskoerperschaftRepository.findById(organisation.getId())).isEmpty();

		assertThat(organisationRepository.findById(organisation.getId())).isNotEmpty();
		assertThat(gebietskoerperschaftRepository.findById(gebietskoerperschaft.getId())).isNotEmpty();
	}
}
