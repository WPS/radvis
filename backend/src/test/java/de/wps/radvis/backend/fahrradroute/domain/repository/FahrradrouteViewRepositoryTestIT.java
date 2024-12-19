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

package de.wps.radvis.backend.fahrradroute.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.dbView.FahrradrouteListenDbView;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.schnittstelle.ToubizConfigurationProperties;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group2")
@ContextConfiguration(classes = {
	FahrradrouteViewRepositoryTestIT.TestConfiguration.class,
	FahrradrouteConfiguration.class,
	OrganisationConfiguration.class,
	NetzConfiguration.class,
	BenutzerConfiguration.class,
	CommonConfiguration.class,
	GeoConverterConfiguration.class,
	MatchingConfiguration.class,
	NetzfehlerConfiguration.class, KommentarConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class,
	BarriereConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	JobConfigurationProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	DLMConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
@ActiveProfiles(profiles = "test")
class FahrradrouteViewRepositoryTestIT extends DBIntegrationTestIT {
	@Configuration
	public static class TestConfiguration {
		@MockBean
		ToubizConfigurationProperties toubizConfigurationProperties;
	}

	@Autowired
	private FahrradrouteViewRepository fahrradrouteViewRepository;
	@Autowired
	private FahrradrouteRepository fahrradrouteRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private KantenRepository kantenRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void findAllAsView_eineKanteNetzbezug() {
		// arrange
		Kante kante = KanteTestDataProvider
			.withCoordinatesAndQuelle(417704.57, 5288712.62, 417919.10, 5288811.05, QuellSystem.DLM)
			.build();
		LineString originalGeometrie = kante.getGeometry();
		AbschnittsweiserKantenBezug abschnittsweiserKantenBezug = new AbschnittsweiserKantenBezug(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
			List.of(abschnittsweiserKantenBezug),
			abschnittsweiserKantenBezug.getKante().getGeometry(),
			originalGeometrie)
			.iconLocation(originalGeometrie.getStartPoint())
			.build();

		gebietskoerperschaftRepository.save((Gebietskoerperschaft) fahrradroute.getVerantwortlich().get());
		kantenRepository.save(kante);
		fahrradrouteRepository.save(fahrradroute);

		entityManager.flush();

		// act
		List<FahrradrouteListenDbView> fahrradrouteListenDbView = fahrradrouteViewRepository.findAll();

		// assert
		assertThat(fahrradrouteListenDbView).hasSize(1);
		assertThat(fahrradrouteListenDbView.get(0).getName()).isEqualTo(fahrradroute.getName());
		assertThat(fahrradrouteListenDbView.get(0).getVerantwortlicheOrganisationName()).isEqualTo(
			fahrradroute.getVerantwortlich().get().getName());
		assertThat(fahrradrouteListenDbView.get(0).getKategorie()).isEqualTo(fahrradroute.getKategorie());
		assertThat(fahrradrouteListenDbView.get(0).getFahrradrouteTyp()).isEqualTo(fahrradroute.getFahrradrouteTyp());

		// Geometrien
		assertThat(fahrradrouteListenDbView.get(0).getIconLocation().getCoordinate())
			.isEqualTo(fahrradroute.getIconLocation().get().getCoordinate());
		assertThat(fahrradrouteListenDbView.get(0).getGeometry().getCoordinates())
			.isEqualTo(fahrradroute.getOriginalGeometrie().get().getCoordinates());
	}

	@Test
	void findAllAsView_mehrereKantenNetzbezug() {
		// arrange
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug = List.of(new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withCoordinatesAndQuelle(417704.57, 5288712.62, 417919.10, 5288811.05,
				QuellSystem.RadNETZ).build(),
			LinearReferenzierterAbschnitt.of(0, 1)),
			new AbschnittsweiserKantenBezug(
				KanteTestDataProvider.withCoordinatesAndQuelle(417919.10, 5288811.05, 417857.16, 5289062.56,
					QuellSystem.RadNETZ).build(),
				LinearReferenzierterAbschnitt.of(0, 1)));
		kantenRepository.saveAll(abschnittsweiserKantenBezug.stream()
			.map(AbschnittsweiserKantenBezug::getKante)
			.collect(Collectors.toList()));

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugList = new ArrayList<>(
			abschnittsweiserKantenBezug);

		LineString netzbezugLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(417704.57, 5288712.62),
			new Coordinate(417919.10, 5288811.05),
			new Coordinate(417919.10, 5288811.05),
			new Coordinate(417857.16, 5289062.56));

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
			abschnittsweiserKantenBezugList,
			netzbezugLineString,
			netzbezugLineString)
			.build();

		gebietskoerperschaftRepository.save((Gebietskoerperschaft) fahrradroute.getVerantwortlich().get());
		fahrradrouteRepository.save(fahrradroute);
		entityManager.flush();

		// act
		List<FahrradrouteListenDbView> fahrradrouteListenDbView = fahrradrouteViewRepository.findAll();

		// assert
		assertThat(fahrradrouteListenDbView).hasSize(1);
		assertThat(fahrradrouteListenDbView.get(0).getGeometry().getCoordinates())
			.containsExactly(
				new Coordinate(417704.57, 5288712.62),
				new Coordinate(417919.10, 5288811.05),
				new Coordinate(417919.10, 5288811.05),
				new Coordinate(417857.16, 5289062.56));
	}

	@Test
	void findAllAsView_mehrereKanten_mitLineareRef() {
		// arrange
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug = List.of(new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withCoordinatesAndQuelle(417704.57, 5288712.62, 417919.10, 5288811.05,
				QuellSystem.RadNETZ).build(),
			LinearReferenzierterAbschnitt.of(0.5, 1)),
			new AbschnittsweiserKantenBezug(
				KanteTestDataProvider.withCoordinatesAndQuelle(417919.10, 5288811.05, 417857.16, 5289062.56,
					QuellSystem.RadNETZ).build(),
				LinearReferenzierterAbschnitt.of(0, 0.5)));
		kantenRepository.saveAll(abschnittsweiserKantenBezug.stream()
			.map(AbschnittsweiserKantenBezug::getKante)
			.collect(Collectors.toList()));

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugList = new ArrayList<>(
			abschnittsweiserKantenBezug);

		LineString netzbezugLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(417811.83499999996, 5288761.835),
			new Coordinate(417919.10, 5288811.05),
			new Coordinate(417919.10, 5288811.05),
			new Coordinate(417888.13, 5288936.805));

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
			abschnittsweiserKantenBezugList,
			netzbezugLineString,
			netzbezugLineString)
			.build();

		gebietskoerperschaftRepository.save((Gebietskoerperschaft) fahrradroute.getVerantwortlich().get());
		fahrradrouteRepository.save(fahrradroute);
		entityManager.flush();

		// act
		List<FahrradrouteListenDbView> fahrradrouteListenDbView = fahrradrouteViewRepository.findAll();

		// assert
		assertThat(fahrradrouteListenDbView).hasSize(1);
		assertThat(fahrradrouteListenDbView.get(0).getGeometry().getCoordinates())
			.containsExactly(
				new Coordinate(417811.83499999996, 5288761.835),
				new Coordinate(417919.10, 5288811.05),
				new Coordinate(417919.10, 5288811.05),
				new Coordinate(417888.13, 5288936.805));
	}

	@Test
	void findAllAsView_geloeschtBeachtet() {
		// arrange
		Kante kante = KanteTestDataProvider
			.withCoordinatesAndQuelle(417704.57, 5288712.62, 417919.10, 5288811.05, QuellSystem.DLM)
			.build();
		LineString originalGeometrie = kante.getGeometry();
		AbschnittsweiserKantenBezug abschnittsweiserKantenBezug = new AbschnittsweiserKantenBezug(kante,
			LinearReferenzierterAbschnitt.of(0, 1));

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.originalGeometrie(originalGeometrie)
			.iconLocation(originalGeometrie.getStartPoint())
			.abschnittsweiserKantenBezug(List.of(abschnittsweiserKantenBezug))
			.build();

		fahrradroute.alsGeloeschtMarkieren();

		gebietskoerperschaftRepository.save((Gebietskoerperschaft) fahrradroute.getVerantwortlich().get());
		kantenRepository.save(kante);
		fahrradrouteRepository.save(fahrradroute);

		entityManager.flush();

		// act
		List<FahrradrouteListenDbView> fahrradrouteListenDbView = fahrradrouteViewRepository.findAll();

		// assert
		assertThat(fahrradrouteListenDbView).hasSize(0);
	}

	@Test
	void findAllAsView_geometry_NetzbezugLineStringVorhanden_alsGeometryUebernehmen() {
		// arrange
		Kante kante = KanteTestDataProvider
			.withCoordinatesAndQuelle(417704.57, 5288712.62, 417919.10, 5288811.05, QuellSystem.DLM)
			.build();

		AbschnittsweiserKantenBezug abschnittsweiserKantenBezug = new AbschnittsweiserKantenBezug(kante,
			LinearReferenzierterAbschnitt.of(0, 1));
		LineString netzbezugLineString = GeometryTestdataProvider.createLineString(
			new Coordinate(200.0, 200.0),
			new Coordinate(500.0, 500.0));
		LineString originalGeometrie = GeometryTestdataProvider.createLineString(
			new Coordinate(0.0, 0.0),
			new Coordinate(100.0, 100.0));

		Fahrradroute mitNetzbezugLineString = FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
			List.of(abschnittsweiserKantenBezug), netzbezugLineString, originalGeometrie)
			.name(FahrradrouteName.of("mitNetzbezugLineString"))
			.build();

		gebietskoerperschaftRepository.save((Gebietskoerperschaft) mitNetzbezugLineString.getVerantwortlich().get());
		kantenRepository.save(kante);
		fahrradrouteRepository.save(mitNetzbezugLineString);

		entityManager.flush();

		// act
		List<FahrradrouteListenDbView> fahrradrouteListenDbView = fahrradrouteViewRepository.findAll();

		// assert
		assertThat(fahrradrouteListenDbView).hasSize(1);
		assertThat(fahrradrouteListenDbView.get(0).getName()).isEqualTo(FahrradrouteName.of("mitNetzbezugLineString"));
		assertThat(fahrradrouteListenDbView.get(0).getGeometry().getCoordinates())
			.containsExactly(new Coordinate(200.0, 200.0),
				new Coordinate(500.0, 500.0));
	}

	@Test
	void findAllAsView_geometry_NullNetzbezugLineString_kantenBezugVerwenden() {
		// arrange
		Kante kante = KanteTestDataProvider
			.withCoordinatesAndQuelle(417704.57, 5288712.62, 417919.10, 5288811.05, QuellSystem.DLM)
			.build();

		AbschnittsweiserKantenBezug abschnittsweiserKantenBezug = new AbschnittsweiserKantenBezug(kante,
			LinearReferenzierterAbschnitt.of(0, 1));
		LineString originalGeometrie = GeometryTestdataProvider.createLineString(
			new Coordinate(0.0, 0.0),
			new Coordinate(100.0, 100.0));

		Fahrradroute ohneNetzbezugLineString = FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
			List.of(abschnittsweiserKantenBezug), null, originalGeometrie)
			.name(FahrradrouteName.of("ohneNetzbezugLineString"))
			.build();

		gebietskoerperschaftRepository.save((Gebietskoerperschaft) ohneNetzbezugLineString.getVerantwortlich().get());
		kantenRepository.save(kante);
		fahrradrouteRepository.save(ohneNetzbezugLineString);

		entityManager.flush();

		// act
		List<FahrradrouteListenDbView> fahrradrouteListenDbView = fahrradrouteViewRepository.findAll();

		// assert
		assertThat(fahrradrouteListenDbView).hasSize(1);
		assertThat(fahrradrouteListenDbView.get(0).getName()).isEqualTo(FahrradrouteName.of("ohneNetzbezugLineString"));
		assertThat(fahrradrouteListenDbView.get(0).getGeometry().getCoordinates())
			.containsExactly(new Coordinate(417704.57, 5288712.62),
				new Coordinate(417919.10, 5288811.05));
	}
}
