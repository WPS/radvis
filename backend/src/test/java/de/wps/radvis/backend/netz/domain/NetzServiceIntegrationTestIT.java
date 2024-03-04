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

package de.wps.radvis.backend.netz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.administration.AdministrationConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.schnittstelle.BenutzerResolverImpl;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.FahrtrichtungAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.schnittstelle.NetzToFeatureDetailsConverter;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group1")
@ContextConfiguration(classes = {
	NetzConfiguration.class,
	CommonConfiguration.class,
	AdministrationConfiguration.class,
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	GeoConverterConfiguration.class
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
public class NetzServiceIntegrationTestIT extends DBIntegrationTestIT {

	@Configuration
	@EnableEnversRepositories(basePackages = { "de.wps.radvis.backend.netz", })
	@EntityScan({
		"de.wps.radvis.backend.netz.domain.entity",
		"de.wps.radvis.backend.netz.domain.valueObject",
		"de.wps.radvis.backend.organisation.domain.entity",
		"de.wps.radvis.backend.common.domain.entity",
		"de.wps.radvis.backend.organisation.domain.valueObject",
		"de.wps.radvis.backend.common.domain.valueObject",
	})
	public static class TestConfiguration {

		@Autowired
		private KantenRepository kantenRepository;

		@Autowired
		private KnotenRepository knotenRepository;

		@Autowired
		private ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppeRepository;

		@Autowired
		private FahrtrichtungAttributGruppeRepository fahrtrichtungAttributGruppeRepository;

		@Autowired
		private GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;

		@Autowired
		private FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppeRepository;

		@Autowired
		private KantenAttributGruppeRepository kantenAttributGruppeRepository;

		@Autowired
		private VerwaltungseinheitResolver verwaltungseinheitResolver;

		@Bean
		NetzToFeatureDetailsConverter netzToFeatureDetailsConverter() {
			return new NetzToFeatureDetailsConverter();
		}

		@Bean
		BenutzerResolver benutzerResolver() {
			return new BenutzerResolverImpl();
		}

		@Bean
		NetzService netzService() {
			return new NetzService(kantenRepository, knotenRepository, zustaendigkeitAttributGruppeRepository,
				fahrtrichtungAttributGruppeRepository, geschwindigkeitAttributGruppeRepository,
				fuehrungsformAttributGruppeRepository, kantenAttributGruppeRepository, verwaltungseinheitResolver);
		}
	}

	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private NetzService netzService;

	@PersistenceContext
	EntityManager entityManager;

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Test
	public void deleteKante_keepsKnoten() {
		// Arrange
		QuellSystem quelle = QuellSystem.RadNETZ;

		LineString lineString1 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(1, 2), new Coordinate(3, 4) });

		LineString lineString2 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(3, 4), new Coordinate(8, 8) });

		Knoten knoten1 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getStartPoint().getCoordinate(), quelle).build();
		Knoten knoten2 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getEndPoint().getCoordinate(), quelle).build();
		Knoten knoten3 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString2.getEndPoint().getCoordinate(), quelle).build();

		Kante kante1 = netzService.saveKante(KanteTestDataProvider.withDefaultValuesAndQuelle(quelle).vonKnoten(knoten1)
			.nachKnoten(knoten2).geometry(lineString1).build());
		Kante kante2 = netzService.saveKante(KanteTestDataProvider.withDefaultValuesAndQuelle(quelle).vonKnoten(knoten2)
			.nachKnoten(knoten3).geometry(lineString2).build());

		// Act
		kantenRepository.delete(kante1);
		entityManager.flush();
		entityManager.clear();

		// Assert
		Kante kante3 = kantenRepository.findById(kante2.getId()).get();
		Optional<Kante> kante1Deleted = kantenRepository.findById(kante1.getId());
		assertThat(kante1Deleted).isEmpty();
		assertThat(kante3.getVonKnoten()).isNotNull();
	}

	@Test
	public void getAnzahlAdjazenteKanten() {
		// Arrange
		QuellSystem quelle = QuellSystem.RadNETZ;
		LineString lineString1 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(1, 2), new Coordinate(3, 4) });

		LineString lineString2 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(3, 4), new Coordinate(8, 8) });
		Knoten knoten1 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getStartPoint().getCoordinate(), quelle).build();
		Knoten knoten2 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getEndPoint().getCoordinate(), quelle).build();
		Knoten knoten3 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString2.getEndPoint().getCoordinate(), quelle).build();

		Kante kante1 = netzService.saveKante(KanteTestDataProvider.withDefaultValuesAndQuelle(quelle).vonKnoten(knoten1)
			.nachKnoten(knoten2).geometry(lineString1).build());
		Kante kante2 = netzService.saveKante(KanteTestDataProvider.withDefaultValuesAndQuelle(quelle).vonKnoten(knoten2)
			.nachKnoten(knoten3).geometry(lineString2).build());
		// Act
		long anzahlAdjazenterKanten1von = netzService.getAnzahlAdjazenterKanten(kante1.getVonKnoten());
		long anzahlAdjazenterKanten1nach = netzService.getAnzahlAdjazenterKanten(kante1.getNachKnoten());
		long anzahlAdjazenterKanten2nach = netzService.getAnzahlAdjazenterKanten(kante2.getNachKnoten());

		// Assert
		assertThat(anzahlAdjazenterKanten1von).isEqualTo(1);
		assertThat(anzahlAdjazenterKanten1nach).isEqualTo(2);
		assertThat(anzahlAdjazenterKanten2nach).isEqualTo(1);
	}

	@Test
	public void existsKante_existiertNicht() {
		// arrange
		Kante testkante = KanteTestDataProvider
			.withCoordinatesAndQuelle(15.0, 15.0, 20.0, 20.0, QuellSystem.RadNETZ).build();
		LineString lineString = testkante.getGeometry();
		KantenAttribute kantenAttribute = testkante.getKantenAttributGruppe().getKantenAttribute();

		// act + assert
		assertThat(netzService.existsKante(lineString, kantenAttribute)).isFalse();
	}

	@Test
	public void existsKante_existiert() {
		// arrange
		Kante testkante = KanteTestDataProvider
			.withCoordinatesAndQuelle(15.0, 15.0, 20.0, 20.0, QuellSystem.RadNETZ).build();
		LineString lineString = testkante.getGeometry();
		KantenAttribute kantenAttribute = testkante.getKantenAttributGruppe().getKantenAttribute();

		netzService.saveKante(testkante);

		// act + assert
		assertThat(netzService.existsKante(lineString, kantenAttribute)).isTrue();
	}

	@Test
	public void existsKante_gleicherLineString_unterschiedliche_attribute_existiertNicht() {
		// arrange
		Kante testkante = KanteTestDataProvider
			.withCoordinatesAndQuelle(15.0, 15.0, 20.0, 20.0, QuellSystem.RadNETZ).build();
		LineString lineString = testkante.getGeometry();

		KantenAttribute kantenAttribute2 = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.dtvFussverkehr(VerkehrStaerke.of(10423))
			.build();

		netzService.saveKante(testkante);

		// act + assert
		assertThat(netzService.existsKante(lineString, kantenAttribute2)).isFalse();
	}

}
