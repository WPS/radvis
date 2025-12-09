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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.PostGisHelper;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.TfisImportProblem;
import de.wps.radvis.backend.fahrradroute.domain.entity.ToubizImportProblem;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteVarianteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.FahrtrichtungAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group1")
@ContextConfiguration(classes = { FahrradrouteRepositoryTestIT.TestConfiguration.class, CommonConfiguration.class,
	GeoConverterConfiguration.class, })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class, FeatureToggleProperties.class,
	PostgisConfigurationProperties.class })
public class FahrradrouteRepositoryTestIT extends DBIntegrationTestIT {
	@EnableEnversRepositories(basePackages = { "de.wps.radvis.backend.fahrradroute", "de.wps.radvis.backend.netz",
		"de.wps.radvis.backend.organisation" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.fahrradroute", "de.wps.radvis.backend.organisation",
		"de.wps.radvis.backend.netz", "de.wps.radvis.backend.benutzer", "de.wps.radvis.backend.common" })
	public static class TestConfiguration {
		@Autowired
		private KantenRepository kantenRepository;
		@Autowired
		private KnotenRepository knotenRepository;
		@MockitoBean
		private ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppenRepository;
		@MockitoBean
		private FahrtrichtungAttributGruppeRepository fahrtrichtungAttributGruppeRepository;
		@MockitoBean
		private GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;
		@MockitoBean
		private FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppenRepository;
		@MockitoBean
		private KantenAttributGruppeRepository kantenAttributGruppenRepository;
		@MockitoBean
		private VerwaltungseinheitResolver verwaltungseinheitResolver;
		@PersistenceContext
		EntityManager entityManager;

		@Bean
		NetzService netzService() {
			return new NetzService(kantenRepository, knotenRepository, zustaendigkeitAttributGruppenRepository,
				fahrtrichtungAttributGruppeRepository, geschwindigkeitAttributGruppeRepository,
				fuehrungsformAttributGruppenRepository, kantenAttributGruppenRepository, verwaltungseinheitResolver,
				entityManager, 1.0, Laenge.of(10), 10, 15.0, 0.5, 100);
		}

	}

	@Autowired
	FahrradrouteRepository fahrradrouteRepository;
	@Autowired
	NetzService netzService;
	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	KantenRepository kantenRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	public void findByKanteIdInFahrradrouteNetzBezug() {
		// arrange
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Kante kante1 = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());

		Fahrradroute fahrradroute1 = fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante1).verantwortlich(gebietskoerperschaft).build());
		fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante2).verantwortlich(gebietskoerperschaft).build());
		Fahrradroute fahrradroute3 = fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante1).verantwortlich(gebietskoerperschaft).build());

		// act
		List<Fahrradroute> findByKanteIdInNetzBezug = fahrradrouteRepository.findByKanteIdInNetzBezug(List.of(kante1
			.getId()));

		// assert
		assertThat(findByKanteIdInNetzBezug).containsExactlyInAnyOrder(fahrradroute1, fahrradroute3);
	}

	@Test
	public void getKantenWithKnotenByFahrradroute() {
		// arrange
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Kante kante1 = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());
		Kante kanteAndereFahrradrooute = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());
		@SuppressWarnings("unused")
		Kante kanteNichtEnthalten = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());

		Fahrradroute fahrradroute1 = fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kante1, kante2).verantwortlich(gebietskoerperschaft).build());
		fahrradrouteRepository
			.save(FahrradrouteTestDataProvider.onKante(kanteAndereFahrradrooute).verantwortlich(gebietskoerperschaft)
				.build());

		// act
		Set<Kante> result = fahrradrouteRepository.getKantenWithKnotenByFahrradroute(fahrradroute1.getId());

		// assert
		assertThat(result).containsExactlyInAnyOrder(kante1, kante2);
	}

	@Test
	public void findByKanteIdInFahrradrouteVariantenNetzBezuege() {
		// arrange
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Kante kante1 = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());
		Kante kante2 = netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());

		// kante 1 in Hauptroute
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.onKante(kante1).verantwortlich(gebietskoerperschaft)
			.build();

		// kante 2 in Variante
		AbschnittsweiserKantenBezug abschnittsweiserKantenBezugKante2 = new AbschnittsweiserKantenBezug(kante2,
			LinearReferenzierterAbschnitt.of(0, 1));
		fahrradroute.replaceFahrradrouteVarianten(List.of(FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(List.of(abschnittsweiserKantenBezugKante2)).build()));

		fahrradrouteRepository.save(fahrradroute);

		// act
		List<Fahrradroute> findByKanteIdInNetzBezug = fahrradrouteRepository.findByKanteIdInNetzBezug(List.of(kante2
			.getId()));

		// assert
		assertThat(findByKanteIdInNetzBezug).containsExactlyInAnyOrder(fahrradroute);
	}

	@Test
	void erstelleFahrradroute_wirdErstellt() {
		// arrange
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues().build();

		// act
		Fahrradroute savedFahrradroute = fahrradrouteRepository.save(fahrradroute);

		// assert
		assertThat(savedFahrradroute).usingRecursiveComparison().ignoringFields("id").isEqualTo(fahrradroute);
	}

	@Test
	void saveFahrradroute_netzbezug_inOrder() {
		// arrange
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug = List.of(new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withCoordinatesAndQuelle(50, 50, 40, 40, QuellSystem.RadNETZ).build(),
			LinearReferenzierterAbschnitt.of(0, 1)),
			new AbschnittsweiserKantenBezug(
				KanteTestDataProvider.withCoordinatesAndQuelle(20, 20, 10, 10, QuellSystem.RadNETZ).build(),
				LinearReferenzierterAbschnitt.of(0, 1)),
			new AbschnittsweiserKantenBezug(
				KanteTestDataProvider.withCoordinatesAndQuelle(30, 30, 20, 20, QuellSystem.RadNETZ).build(),
				LinearReferenzierterAbschnitt.of(0, 1)));

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.abschnittsweiserKantenBezug(new ArrayList<>(
				abschnittsweiserKantenBezug))
			.build();

		saveInRepoOrgaKantenUndFahrradrouteFrom(List.of(fahrradroute));

		// act
		Fahrradroute loadedFahrradroute = StreamSupport.stream(fahrradrouteRepository.findAll().spliterator(), false)
			.findFirst().get();

		// assert
		assertThat(loadedFahrradroute.getAbschnittsweiserKantenBezug())
			.extracting(AbschnittsweiserKantenBezug::getKante)
			.extracting(Kante::getGeometry)
			.extracting(Geometry::getCoordinates)
			.containsExactly(new Coordinate[] {
				new Coordinate(50, 50),
				new Coordinate(40, 40)
			},
				new Coordinate[] {
					new Coordinate(20, 20),
					new Coordinate(10, 10)
				},
				new Coordinate[] {
					new Coordinate(30, 30),
					new Coordinate(20, 20)
				});

	}

	@Test
	void testFindAllToubizImportProbleme() {
		// arrange
		Fahrradroute lrfw = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.build();

		Fahrradroute tfis = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.build();

		Fahrradroute toubizOhneProbleme = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("toubizOhneProbleme"))
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.netzbezugLineString(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(100, 100)))
			.build();

		Fahrradroute toubizOhneNetzbezugLineString = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("toubizOhneNetzbezugLineString"))
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.netzbezugLineString(null)
			.build();

		Fahrradroute toubizMitAbweichendenSegmenten = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("toubizMitAbweichendenSegmenten"))
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.netzbezugLineString(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(100, 100)))
			.fahrradroutenMatchingAndRoutingInformation(
				FahrradroutenMatchingAndRoutingInformation.builder()
					.abweichendeSegmente(GeometryTestdataProvider.createMultiLineString(
						GeometryTestdataProvider.createLineString(new Coordinate(15, 15), new Coordinate(20, 20))))
					.build())
			.build();

		List<Fahrradroute> fahrradrouten = List.of(
			lrfw,
			tfis,
			toubizOhneProbleme,
			toubizOhneNetzbezugLineString,
			toubizMitAbweichendenSegmenten);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		List<Long> toubizImportProblemIds = fahrradrouteRepository.findAllToubizImportProbleme().stream()
			.map(ToubizImportProblem::getId).collect(Collectors.toList());

		// assert
		assertThat(toubizImportProblemIds).containsExactlyInAnyOrder(toubizOhneNetzbezugLineString.getId(),
			toubizMitAbweichendenSegmenten.getId());
	}

	@Test
	void testFindAllToubizImportProbleme_ohneGeoUndIcons_aussortiert() {
		// arrange
		Fahrradroute lrfw = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.build();

		Fahrradroute tfis = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.build();

		Fahrradroute toubizOhneProbleme = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("toubizOhneProbleme"))
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.netzbezugLineString(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(100, 100)))
			.build();

		Fahrradroute toubizOhneNetzbezugLineString = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("toubizOhneNetzbezugLineString"))
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.originalGeometrie(null)
			.build();

		Fahrradroute toubizMitAbweichendenSegmenten = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("toubizMitAbweichendenSegmenten"))
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.netzbezugLineString(
				GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(100, 100)))
			.fahrradroutenMatchingAndRoutingInformation(
				FahrradroutenMatchingAndRoutingInformation.builder()
					.abweichendeSegmente(GeometryTestdataProvider.createMultiLineString(
						GeometryTestdataProvider.createLineString(new Coordinate(15, 15), new Coordinate(20, 20))))
					.build())
			.iconLocation(null)
			.build();

		List<Fahrradroute> fahrradrouten = List.of(
			lrfw,
			tfis,
			toubizOhneProbleme,
			toubizOhneNetzbezugLineString,
			toubizMitAbweichendenSegmenten);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		List<Long> toubizImportProblemIds = fahrradrouteRepository.findAllToubizImportProbleme().stream()
			.map(ToubizImportProblem::getId).collect(
				Collectors.toList());

		// assert
		assertThat(toubizImportProblemIds).isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void findAllTfisImportProbleme() {
		// arrange
		Fahrradroute fahrradrouteMitNetzbezugLineString = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("fahrradrouteMitNetzbezugLineString")).fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.build();

		Fahrradroute fahrradrouteOhneNetzbezugLineStringTfis = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("fahrradrouteOhneNetzbezugLineString"))
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.netzbezugLineString(null)
			.build();

		Fahrradroute fahrradrouteOhneNetzbezugLineStringToubiz = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("fahrradrouteOhneNetzbezugLineString"))
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.netzbezugLineString(null)
			.build();

		List<Fahrradroute> fahrradrouten = List.of(
			fahrradrouteMitNetzbezugLineString,
			fahrradrouteOhneNetzbezugLineStringTfis, fahrradrouteOhneNetzbezugLineStringToubiz);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		List<TfisImportProblem> tfisImportProbleme = fahrradrouteRepository.findAllTfisImportProbleme();

		assertThat(tfisImportProbleme).hasSize(1);
		assertThat(tfisImportProbleme.get(0).getId())
			.isEqualTo(fahrradrouteOhneNetzbezugLineStringTfis.getId());
		assertThat(tfisImportProbleme.get(0).getOriginalGeometry())
			.isEqualTo(fahrradrouteOhneNetzbezugLineStringTfis.getOriginalGeometrie().get());
		assertThat(tfisImportProbleme.get(0).getDatum())
			.isEqualTo(fahrradrouteOhneNetzbezugLineStringTfis.getZuletztBearbeitet());
		assertThat(tfisImportProbleme.get(0).isHasNetzbezug()).isTrue();
	}

	@Test
	public void findAllTfisImportProbleme_keinNetzbezug() {
		// arrange
		Fahrradroute keinNetzbezug = FahrradrouteTestDataProvider.withDefaultValues()
			.abschnittsweiserKantenBezug(List.of()).netzbezugLineString(null)
			.name(FahrradrouteName.of("keinNetzbezug")).fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.build();

		List<Fahrradroute> fahrradrouten = List.of(keinNetzbezug);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		List<TfisImportProblem> tfisImportProbleme = fahrradrouteRepository.findAllTfisImportProbleme();

		assertThat(tfisImportProbleme).hasSize(1);
		assertThat(tfisImportProbleme.get(0).isHasNetzbezug()).isFalse();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void findAllLrfwImportProbleme() {
		// arrange
		Fahrradroute fahrradrouteMitNetzbezugLineString = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("fahrradrouteMitNetzbezugLineString")).kategorie(Kategorie.LANDESRADFERNWEG)
			.build();

		Fahrradroute fahrradrouteOhneNetzbezugLineStringLrfw = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("fahrradrouteOhneNetzbezugLineString")).kategorie(Kategorie.LANDESRADFERNWEG)
			.netzbezugLineString(null)
			.build();

		Fahrradroute fahrradrouteOhneNetzbezugLineStringSonst = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("fahrradrouteOhneNetzbezugLineString")).kategorie(Kategorie.RADSCHNELLWEG)
			.netzbezugLineString(null)
			.build();

		List<Fahrradroute> fahrradrouten = List.of(
			fahrradrouteMitNetzbezugLineString,
			fahrradrouteOhneNetzbezugLineStringLrfw, fahrradrouteOhneNetzbezugLineStringSonst);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		List<TfisImportProblem> lrfwImportProbleme = fahrradrouteRepository.findAllLrfwImportProbleme();

		assertThat(lrfwImportProbleme).hasSize(1);
		assertThat(lrfwImportProbleme.get(0).getId())
			.isEqualTo(fahrradrouteOhneNetzbezugLineStringLrfw.getId());
		assertThat(lrfwImportProbleme.get(0).getOriginalGeometry())
			.isEqualTo(fahrradrouteOhneNetzbezugLineStringLrfw.getOriginalGeometrie().get());
		assertThat(lrfwImportProbleme.get(0).getDatum())
			.isEqualTo(fahrradrouteOhneNetzbezugLineStringLrfw.getZuletztBearbeitet());
	}

	@Test
	public void findAllLrfwImportProbleme_keinNetzbezug() {
		// arrange
		Fahrradroute keinNetzbezug = FahrradrouteTestDataProvider.withDefaultValues()
			.abschnittsweiserKantenBezug(List.of()).netzbezugLineString(null)
			.name(FahrradrouteName.of("keinNetzbezug")).kategorie(Kategorie.LANDESRADFERNWEG)
			.build();

		List<Fahrradroute> fahrradrouten = List.of(keinNetzbezug);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		List<TfisImportProblem> tfisImportProbleme = fahrradrouteRepository.findAllLrfwImportProbleme();

		assertThat(tfisImportProbleme).hasSize(1);
		assertThat(tfisImportProbleme.get(0).isHasNetzbezug()).isFalse();
	}

	@Test
	public void findAllWithoutNetzbezugLineString() {
		// arrange
		Fahrradroute fahrradrouteMitNetzbezugLineString = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("fahrradrouteMitNetzbezugLineString"))
			.build();

		Fahrradroute fahrradrouteOhneNetzbezugLineString = FahrradrouteTestDataProvider.withDefaultValues()
			.name(FahrradrouteName.of("fahrradrouteOhneNetzbezugLineString"))
			.netzbezugLineString(null)
			.build();

		List<Fahrradroute> fahrradrouten = List.of(
			fahrradrouteMitNetzbezugLineString,
			fahrradrouteOhneNetzbezugLineString);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		List<Fahrradroute> fahrradroutes = fahrradrouteRepository.findAllWithoutNetzbezugLineString()
			.collect(Collectors.toList());

		assertThat(fahrradroutes).hasSize(1);
		assertThat(fahrradroutes.get(0).getName()).isEqualTo(fahrradrouteOhneNetzbezugLineString.getName());
	}

	@Test
	void findAllToubizIdsOfLandesradfernwege() {
		// arrange
		Fahrradroute fahrradrouteLRFWohneTid = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.toubizId(null)
			.build();

		Fahrradroute fahrradrouteLRFWmitTid = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.toubizId(ToubizId.of("testToubizId"))
			.build();

		Fahrradroute fahrradrouteTfis = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.kategorie(Kategorie.REGIONALER_RADWANDERWEG)
			.build();

		Fahrradroute fahrradrouteToubiz = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.kategorie(Kategorie.TOURISTISCHE_ROUTE)
			.toubizId(ToubizId.of("testToubizId2"))
			.build();

		List<Fahrradroute> fahrradrouten = List.of(
			fahrradrouteLRFWohneTid,
			fahrradrouteLRFWmitTid,
			fahrradrouteTfis,
			fahrradrouteToubiz);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		Set<ToubizId> toubizIds = fahrradrouteRepository.findAllToubizIdsOfLandesradfernwege();

		// assert
		assertThat(toubizIds).containsExactly(ToubizId.of("testToubizId"));
	}

	@Test
	void findAllTfisIdsOfLandesradfernwege() {
		// arrange
		Fahrradroute fahrradrouteLRFWohneTid = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.tfisId(null)
			.build();

		Fahrradroute fahrradrouteLRFWmitTid = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.tfisId(TfisId.of("testTfis"))
			.build();

		Fahrradroute fahrradrouteTfis = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.kategorie(Kategorie.REGIONALER_RADWANDERWEG)
			.tfisId(TfisId.of("testTfis2"))
			.build();

		Fahrradroute fahrradrouteToubiz = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.kategorie(Kategorie.TOURISTISCHE_ROUTE)
			.build();

		List<Fahrradroute> fahrradrouten = List.of(
			fahrradrouteLRFWohneTid,
			fahrradrouteLRFWmitTid,
			fahrradrouteTfis,
			fahrradrouteToubiz);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		Set<TfisId> tfisIds = fahrradrouteRepository.findAllTfisIdsOfLandesradfernwege();

		// assert
		assertThat(tfisIds).containsExactly(TfisId.of("testTfis"));
	}

	@Test
	void findAllTfisIdsWithoutLandesradfernwege() {
		// arrange
		Fahrradroute fahrradrouteOhneTid = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.tfisId(null)
			.build();

		Fahrradroute fahrradrouteLRFWmitTid = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.tfisId(TfisId.of("testTfis"))
			.build();

		Fahrradroute fahrradrouteTfis = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TFIS_ROUTE)
			.kategorie(Kategorie.REGIONALER_RADWANDERWEG)
			.tfisId(TfisId.of("testTfis2"))
			.build();

		Fahrradroute fahrradrouteToubiz = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.TOUBIZ_ROUTE)
			.kategorie(Kategorie.TOURISTISCHE_ROUTE)
			.build();

		List<Fahrradroute> fahrradrouten = List.of(
			fahrradrouteOhneTid,
			fahrradrouteLRFWmitTid,
			fahrradrouteTfis,
			fahrradrouteToubiz);

		saveInRepoOrgaKantenUndFahrradrouteFrom(fahrradrouten);

		// act
		Set<TfisId> tfisIds = fahrradrouteRepository.findAllTfisIdsWithoutLandesradfernwege();

		// assert
		assertThat(tfisIds).containsExactly(TfisId.of("testTfis2"));
	}

	@Test
	public void findAllTfisIdsWithoutNetzbezugLineString() {
		// arrange
		Fahrradroute fahrradrouteNoTfisWithLinestring = FahrradrouteTestDataProvider.withDefaultValues().tfisId(null)
			.netzbezugLineString(GeometryTestdataProvider.createLineString()).build();
		Fahrradroute fahrradrouteFromTfisNoLinestring = FahrradrouteTestDataProvider.withDefaultValues()
			.tfisId(TfisId.of("test")).netzbezugLineString(null).buildTfisRoute();
		Fahrradroute lrfwFromTfisNoLinestring = FahrradrouteTestDataProvider.withDefaultValues()
			.tfisId(TfisId.of("test2")).netzbezugLineString(null).buildLandesradfernweg();
		Fahrradroute fahrradrouteFromTfisWithLinestring = FahrradrouteTestDataProvider.withDefaultValues()
			.tfisId(TfisId.of("test3"))
			.netzbezugLineString(GeometryTestdataProvider.createLineString()).build();

		saveInRepoOrgaKantenUndFahrradrouteFrom(List.of(fahrradrouteFromTfisNoLinestring,
			fahrradrouteFromTfisWithLinestring, fahrradrouteNoTfisWithLinestring, lrfwFromTfisNoLinestring));

		// act+assert
		assertThat(fahrradrouteRepository.findAllTfisIdsWithoutNetzbezugLineString())
			.containsExactly(fahrradrouteFromTfisNoLinestring.getTfisId(), lrfwFromTfisNoLinestring.getTfisId());
	}

	@Test
	public void findAllTfisIdsWithVariantenWithoutGeometrie() {
		// arrange
		Fahrradroute withoutTfisId = FahrradrouteTestDataProvider.withDefaultValues().tfisId(null)
			.varianten(List.of(FahrradrouteVarianteTestDataProvider.defaultTfis().geometrie(null).build()))
			.build();

		Fahrradroute withoutVarianten = FahrradrouteTestDataProvider.withDefaultValues()
			.tfisId(TfisId.of("Ohne Varianten"))
			.build();

		Fahrradroute withGeometrie = FahrradrouteTestDataProvider.withDefaultValues()
			.tfisId(TfisId.of("Variante mit Geometrie"))
			.varianten(List.of(FahrradrouteVarianteTestDataProvider.defaultTfis()
				.geometrie(GeometryTestdataProvider.createLineString()).build()))
			.build();

		// dieser Fall ist fachlich eigentlich nicht möglich, aber der Vollständigkeit halber
		Fahrradroute withoutGeometrieWithoutTfisId = FahrradrouteTestDataProvider.withDefaultValues()
			.tfisId(TfisId.of("Variante ohne Geometrie, ohne TfisId"))
			.varianten(List.of(FahrradrouteVarianteTestDataProvider.defaultTfis()
				.geometrie(GeometryTestdataProvider.createLineString()).build()))
			.build();

		Fahrradroute withoutGeometrie = FahrradrouteTestDataProvider.withDefaultValues()
			.tfisId(TfisId.of("Varianten mit und ohne Geometrie"))
			.varianten(List.of(
				FahrradrouteVarianteTestDataProvider.defaultTfis()
					.geometrie(GeometryTestdataProvider.createLineString()).build(),
				FahrradrouteVarianteTestDataProvider.defaultTfis().geometrie(null).build(),
				FahrradrouteVarianteTestDataProvider.defaultTfis().geometrie(null).build()))
			.build();

		saveInRepoOrgaKantenUndFahrradrouteFrom(
			List.of(withoutTfisId, withoutVarianten, withGeometrie, withoutGeometrieWithoutTfisId, withoutGeometrie));

		// act+assert
		assertThat(fahrradrouteRepository.findAllTfisIdsWithVariantenWithoutGeometrie())
			.containsExactly(withoutGeometrie.getTfisId());
	}

	@Test
	public void geoserverBalmFahrradrouteView() throws SQLException {
		// Arrange
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("tolle Organisation")
				.organisationsArt(OrganisationsArt.KREIS)
				.build());

		Kante kanteRadnetz = kantenRepository.save(
			KanteTestDataProvider.withDefaultValues()
				.geometry(
					GeometryTestdataProvider.createLineString(
						new Coordinate(1, 1),
						new Coordinate(10, 10)))
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)),
						new HashSet<>()))
				.build());

		Kante kanteKreisnetz = kantenRepository.save(
			KanteTestDataProvider.withDefaultValues()
				.geometry(
					GeometryTestdataProvider.createLineString(
						new Coordinate(20, 20),
						new Coordinate(30, 30)))
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.KREISNETZ_ALLTAG)),
						new HashSet<>()))
				.build());

		Kante kanteKommunalnetz = kantenRepository.save(
			KanteTestDataProvider.withDefaultValues()
				.geometry(
					GeometryTestdataProvider.createLineString(
						new Coordinate(30, 30),
						new Coordinate(40, 40)))
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.KOMMUNALNETZ_FREIZEIT)),
						new HashSet<>()))
				.build());

		entityManager.flush();
		entityManager.clear();

		final Fahrradroute lrfwRadnetz = fahrradrouteRepository.save(
			FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
				List.of(new AbschnittsweiserKantenBezug(
					kanteRadnetz,
					LinearReferenzierterAbschnitt.of(0, 1))),
				kanteRadnetz.getGeometry(),
				kanteRadnetz.getGeometry())
				.kategorie(Kategorie.LANDESRADFERNWEG)
				.verantwortlich(gebietskoerperschaft)
				.name(FahrradrouteName.of("LRFW-RadNetz"))
				.build());
		final Fahrradroute lrfwKreisnetz = fahrradrouteRepository.save(
			FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
				List.of(new AbschnittsweiserKantenBezug(
					kanteKreisnetz,
					LinearReferenzierterAbschnitt.of(0, 1))),
				kanteKreisnetz.getGeometry(),
				kanteKreisnetz.getGeometry())
				.kategorie(Kategorie.LANDESRADFERNWEG)
				.verantwortlich(gebietskoerperschaft)
				.name(FahrradrouteName.of("LRFW-Kreisnetz"))
				.build());
		final Fahrradroute drouteKommunalnetz = fahrradrouteRepository.save(
			FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
				List.of(new AbschnittsweiserKantenBezug(
					kanteKommunalnetz,
					LinearReferenzierterAbschnitt.of(0, 1))),
				kanteKommunalnetz.getGeometry(),
				kanteKommunalnetz.getGeometry())
				.kategorie(Kategorie.D_ROUTE)
				.verantwortlich(gebietskoerperschaft)
				.name(FahrradrouteName.of("LRFW-Kommunalnetz"))
				.build());
		final Fahrradroute originalGeometrieNull = fahrradrouteRepository.save(
			FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
				List.of(new AbschnittsweiserKantenBezug(
					kanteKommunalnetz,
					LinearReferenzierterAbschnitt.of(0, 1))),
				kanteKommunalnetz.getGeometry(),
				null)
				.kategorie(Kategorie.D_ROUTE)
				.verantwortlich(gebietskoerperschaft)
				.name(FahrradrouteName.of("originalGeometrieNull"))
				.build());
		final Fahrradroute netzbezugLineStringNull = fahrradrouteRepository.save(
			FahrradrouteTestDataProvider.defaultWithCustomNetzbezug(
				List.of(new AbschnittsweiserKantenBezug(
					kanteKommunalnetz,
					LinearReferenzierterAbschnitt.of(0, 1))),
				null,
				kanteKommunalnetz.getGeometry())
				.kategorie(Kategorie.D_ROUTE)
				.verantwortlich(gebietskoerperschaft)
				.name(FahrradrouteName.of("netzbezugLineStringNull"))
				.build());

		entityManager.flush();
		entityManager.clear();

		// Act
		kantenRepository.refreshNetzMaterializedViews();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + FahrradrouteRepository.GEOSERVER_BALM_FAHRRADROUTEN_VIEW_NAME);

		// Assert
		PGobject kanteRadnetzGeometry = PostGisHelper.getPGobject(lrfwRadnetz.getNetzbezugLineString().get());
		PGobject kanteKreisnetzGeometry = PostGisHelper.getPGobject(lrfwKreisnetz.getNetzbezugLineString().get());
		PGobject kanteKommunalnetzGeometry = PostGisHelper.getPGobject(
			drouteKommunalnetz.getNetzbezugLineString().get());

		Map<String, Object> expectedLrfwRadnetz = new HashMap<>() {
			{
				put("Name", lrfwRadnetz.getName().getName());
				put("Routen-ID", "08-" + lrfwRadnetz.getId());
				put("D-Route", null);
				put("EuroVelo", 0);
				put("Landesnetz", 1);
				put("KommuNetz", null);
				put("Radnetz_D", 0);
				put("geometry", kanteRadnetzGeometry);
			}
		};

		Map<String, Object> expectedLrfwKreisnetz = new HashMap<>() {
			{
				put("Name", lrfwKreisnetz.getName().getName());
				put("Routen-ID", "08-" + lrfwKreisnetz.getId());
				put("D-Route", null);
				put("EuroVelo", 0);
				put("Landesnetz", 0);
				put("KommuNetz", null);
				put("Radnetz_D", 0);
				put("geometry", kanteKreisnetzGeometry);
			}
		};

		Map<String, Object> expectedDrouteKommunalnetz = new HashMap<>() {
			{
				put("Name", drouteKommunalnetz.getName().getName());
				put("Routen-ID", "08-" + drouteKommunalnetz.getId());
				put("D-Route", null); // Trotz D-Route "null", da kein Werte-Mapping bereitgestellt wurde
				put("EuroVelo", 0);
				put("Landesnetz", 0);
				put("KommuNetz", null);
				put("Radnetz_D", 1);
				put("geometry", kanteKommunalnetzGeometry);
			}
		};

		Map<String, Object> expectedOrigGeoNull = new HashMap<>() {
			{
				put("Name", originalGeometrieNull.getName().getName());
				put("Routen-ID", "08-" + originalGeometrieNull.getId());
				put("D-Route", null); // Trotz D-Route "null", da kein Werte-Mapping bereitgestellt wurde
				put("EuroVelo", 0);
				put("Landesnetz", 0);
				put("KommuNetz", null);
				put("Radnetz_D", 1);
				put("geometry", kanteKommunalnetzGeometry);
			}
		};

		Map<String, Object> expectedNetzbezugLineStringNull = new HashMap<>() {
			{
				put("Name", netzbezugLineStringNull.getName().getName());
				put("Routen-ID", "08-" + netzbezugLineStringNull.getId());
				put("D-Route", null); // Trotz D-Route "null", da kein Werte-Mapping bereitgestellt wurde
				put("EuroVelo", 0);
				put("Landesnetz", 0);
				put("KommuNetz", null);
				put("Radnetz_D", 1);
				put("geometry", kanteKommunalnetzGeometry);
			}
		};

		assertThat(resultList).containsExactlyInAnyOrder(
			expectedLrfwKreisnetz,
			expectedLrfwRadnetz,
			expectedDrouteKommunalnetz,
			expectedOrigGeoNull,
			expectedNetzbezugLineStringNull);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getAllGeometries_shouldFilter() {
		// arrange
		LineString lineStringInnerhalb = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(0, 100));
		Fahrradroute fahrradroute1 = FahrradrouteTestDataProvider.withDefaultValues().netzbezugLineString(
			lineStringInnerhalb).build();

		LineString lineStringAusserhalb = GeometryTestdataProvider.createLineString(new Coordinate(0, 100),
			new Coordinate(100, 100));
		Fahrradroute fahrradroute2 = FahrradrouteTestDataProvider.withDefaultValues().netzbezugLineString(
			lineStringAusserhalb).build();

		saveInRepoOrgaKantenUndFahrradrouteFrom(List.of(fahrradroute1, fahrradroute2));

		// act
		List<Geometry> result = fahrradrouteRepository.getAllGeometries(List.of(fahrradroute1.getId()));

		// assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getNumGeometries()).isEqualTo(1);
		assertThat(result.get(0).getGeometryN(0)).isEqualTo(lineStringInnerhalb);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getAllGeometries_shouldFallBackToOriginalGeometry() {
		// arrange
		LineString netzBezugLinestring = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(0, 100));
		Fahrradroute fahrradroute1 = FahrradrouteTestDataProvider.withDefaultValues().netzbezugLineString(
			netzBezugLinestring).build();

		LineString originalLineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 100),
			new Coordinate(100, 100));
		Fahrradroute fahrradroute2 = FahrradrouteTestDataProvider.withDefaultValues().netzbezugLineString(null)
			.originalGeometrie(
				originalLineString)
			.build();

		saveInRepoOrgaKantenUndFahrradrouteFrom(List.of(fahrradroute1, fahrradroute2));

		// act
		List<Geometry> result = fahrradrouteRepository
			.getAllGeometries(List.of(fahrradroute1.getId(), fahrradroute2.getId()));

		// assert
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getNumGeometries()).isEqualTo(1);
		assertThat(result.get(0).getGeometryN(0)).isEqualTo(netzBezugLinestring);
		assertThat(result.get(1).getNumGeometries()).isEqualTo(1);
		assertThat(result.get(1).getGeometryN(0)).isEqualTo(originalLineString);
	}

	@Test
	void getAllGeometries_shouldFilterNullGeometry() {
		// arrange
		Fahrradroute fahrradroute1 = FahrradrouteTestDataProvider.withDefaultValues().netzbezugLineString(
			null).originalGeometrie(null).build();

		saveInRepoOrgaKantenUndFahrradrouteFrom(List.of(fahrradroute1));

		// act
		List<Geometry> result = fahrradrouteRepository.getAllGeometries(List.of(fahrradroute1.getId()));

		// assert
		assertThat(result).isEmpty();
	}

	private void saveInRepoOrgaKantenUndFahrradrouteFrom(List<Fahrradroute> fahrradrouten) {
		fahrradrouten.forEach(route -> {
			if (route.getVerantwortlich().isPresent()) {
				gebietskoerperschaftRepository.save((Gebietskoerperschaft) route.getVerantwortlich().get());
			}
			Stream<AbschnittsweiserKantenBezug> alleKantenbezuege = Stream
				.concat(
					route.getAbschnittsweiserKantenBezug().stream(),
					route.getVarianten().stream().flatMap(v -> v.getAbschnittsweiserKantenBezug().stream()));
			kantenRepository
				.saveAll(alleKantenbezuege.map(AbschnittsweiserKantenBezug::getKante).collect(Collectors.toList()));
		});

		fahrradrouteRepository.saveAll(fahrradrouten);

		entityManager.flush();
		entityManager.clear();
	}
}
