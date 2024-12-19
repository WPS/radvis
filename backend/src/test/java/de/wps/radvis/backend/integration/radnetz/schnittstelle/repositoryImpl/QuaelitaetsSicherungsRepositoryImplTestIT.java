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

package de.wps.radvis.backend.integration.radnetz.schnittstelle.repositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
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
import de.wps.radvis.backend.integration.radnetz.IntegrationRadNetzConfiguration;
import de.wps.radvis.backend.integration.radnetz.domain.repository.QualitaetsSicherungsRepository;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group1")
@ContextConfiguration(classes = { NetzConfiguration.class, IntegrationRadNetzConfiguration.class,
	OrganisationConfiguration.class, BenutzerConfiguration.class, GeoConverterConfiguration.class,
	NetzfehlerConfiguration.class, CommonConfiguration.class,
	KommentarConfiguration.class, KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class
})
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
class QuaelitaetsSicherungsRepositoryImplTestIT extends DBIntegrationTestIT {
	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Autowired
	private QualitaetsSicherungsRepository qualitaetsSicherungsRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void testSetzeDLMAlsGrundnetz() {
		// arrange
		Verwaltungseinheit landkreis = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(createQuadratischerBereich(10, 10, 40, 40))
			.build();

		Kante grundnetzKanteRadNETZAusserhalb = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 9, 9,
			QuellSystem.RadNETZ)
			.isGrundnetz(true)
			.build();

		Kante kanteRadNETZAusserhalb = KanteTestDataProvider.withCoordinatesAndQuelle(42, 42, 99, 99,
			QuellSystem.RadNETZ)
			.isGrundnetz(false)
			.build();

		Kante grundnetzKanteRadNETZInnerhalb = KanteTestDataProvider.withCoordinatesAndQuelle(5, 5, 11, 11,
			QuellSystem.RadNETZ)
			.isGrundnetz(true)
			.build();

		Kante kanteDLMAusserhalb = KanteTestDataProvider.withCoordinatesAndQuelle(1, 1, 8, 8, QuellSystem.DLM)
			.isGrundnetz(false)
			.build();

		Kante grundnetzKanteDLMAusserhalb = KanteTestDataProvider.withCoordinatesAndQuelle(70, 70, 100, 100,
			QuellSystem.DLM)
			.isGrundnetz(true)
			.build();

		Kante kanteDLMInnerhalb = KanteTestDataProvider.withCoordinatesAndQuelle(6, 6, 12, 12, QuellSystem.DLM)
			.isGrundnetz(false)
			.build();

		Kante grundnetzKanteDLMInnerhalb = KanteTestDataProvider.withCoordinatesAndQuelle(12, 12, 20, 20,
			QuellSystem.DLM)
			.isGrundnetz(true)
			.build();

		kantenRepository.saveAll(List.of(
			grundnetzKanteRadNETZAusserhalb,
			kanteRadNETZAusserhalb,
			grundnetzKanteRadNETZInnerhalb,
			kanteDLMAusserhalb,
			grundnetzKanteDLMAusserhalb,
			kanteDLMInnerhalb,
			grundnetzKanteDLMInnerhalb));

		entityManager.flush();
		entityManager.clear();

		// act

		qualitaetsSicherungsRepository.setzeDLMAlsGrundnetz(landkreis);

		// assert

		assertThat(kantenRepository.findAll())
			.filteredOn(Kante::isGrundnetz)
			.extracting(Kante::getGeometry)
			.containsExactlyInAnyOrder(
				grundnetzKanteRadNETZAusserhalb.getGeometry(), // Grundnetzstatus unverändert
				grundnetzKanteDLMAusserhalb.getGeometry(), // Grundnetzstatus unverändert
				grundnetzKanteDLMInnerhalb.getGeometry(),
				kanteDLMInnerhalb.getGeometry());

	}

	@Test
	void testLiegenAlleInQualitaetsgesichertenLandkreisen() {
		// arrange
		Gebietskoerperschaft landkreisQualitaetsgesichert = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("landkreisQualitaetsgesichert")
			.organisationsArt(OrganisationsArt.KREIS)
			.bereich(createQuadratischerBereich(0, 0, 20, 20))
			.istQualitaetsgesichert(true)
			.build();

		Gebietskoerperschaft landkreisNichtQualitaetsgesichert = VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("landkreisNichtQualitaetsgesichert")
			.organisationsArt(OrganisationsArt.KREIS)
			.bereich(createQuadratischerBereich(20, 0, 40, 20))
			.istQualitaetsgesichert(false)
			.build();

		Kante kanteQualitaetsGesichert = KanteTestDataProvider
			.withCoordinatesAndQuelle(1, 1, 10, 10, QuellSystem.RadNETZ)
			.build();

		Kante kanteNichtQualitaetsGesichert = KanteTestDataProvider
			.withCoordinatesAndQuelle(21, 1, 30, 10, QuellSystem.RadNETZ)
			.build();

		Kante kanteHeterogen = KanteTestDataProvider
			.withCoordinatesAndQuelle(1, 1, 40, 40, QuellSystem.RadNETZ)
			.build();

		List<Long> ids = StreamSupport.stream(
			kantenRepository.saveAll(List.of(kanteQualitaetsGesichert, kanteNichtQualitaetsGesichert, kanteHeterogen))
				.spliterator(),
			false)
			.map(Kante::getId)
			.collect(Collectors.toList());

		gebietskoerperschaftRepository
			.saveAll(List.of(landkreisNichtQualitaetsgesichert, landkreisQualitaetsgesichert));

		// act & assert

		assertThat(qualitaetsSicherungsRepository.liegenAlleInQualitaetsgesichertenLandkreisen(ids)).isFalse();
		assertThat(qualitaetsSicherungsRepository.liegenAlleInQualitaetsgesichertenLandkreisen(
			List.of(kanteQualitaetsGesichert.getId()))).isTrue();
		assertThat(qualitaetsSicherungsRepository.liegenAlleInQualitaetsgesichertenLandkreisen(
			List.of(kanteNichtQualitaetsGesichert.getId()))).isFalse();

		assertThat(qualitaetsSicherungsRepository.liegenAlleInQualitaetsgesichertenLandkreisen(
			List.of(kanteHeterogen.getId()))).isTrue();

	}

	private MultiPolygon createQuadratischerBereich(double minX, double minY, double maxX, double maxY) {
		return GEO_FACTORY.createMultiPolygon(new Polygon[] {
			GEO_FACTORY.createPolygon(new Coordinate[] {
				new Coordinate(minX, minY),
				new Coordinate(minX, maxY),
				new Coordinate(maxX, maxY),
				new Coordinate(maxX, minY),
				new Coordinate(minX, minY)
			})
		});
	}
}
