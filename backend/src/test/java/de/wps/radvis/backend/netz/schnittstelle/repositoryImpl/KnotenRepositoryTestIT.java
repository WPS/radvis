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

package de.wps.radvis.backend.netz.schnittstelle.repositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.assertj.core.util.IterableUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.PostGisHelper;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.constraints.NotNull;

@Tag("group1")
@EnableJpaRepositories(basePackageClasses = { FahrradrouteConfiguration.class })
@EntityScan(basePackageClasses = { FahrradrouteConfiguration.class })
@ContextConfiguration(classes = { NetzConfiguration.class, OrganisationConfiguration.class,
	CommonConfiguration.class, BenutzerConfiguration.class, GeoConverterConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
public class KnotenRepositoryTestIT extends DBIntegrationTestIT {

	@Autowired
	private KnotenRepository knotenRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	FahrradrouteRepository fahrradrouteRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@PersistenceContext
	EntityManager entityManager;

	@Test
	public void testSaveAndGet() {
		// Arrange
		assertThat(knotenRepository).isNotNull();

		Knoten knoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.RadNETZ)
			.build();

		// Act
		Long savedKnotenId = knotenRepository.save(knoten).getId();

		entityManager.flush();
		entityManager.clear();

		Optional<Knoten> result = knotenRepository.findById(savedKnotenId);

		// Assert
		assertThat(result).isPresent();
		Knoten resultKnoten = result.get();

		assertThat(resultKnoten.getQuelle()).isEqualTo(QuellSystem.RadNETZ);
		assertThat(resultKnoten.getKoordinate()).isEqualTo(new Coordinate(10, 10));
	}

	@Test
	void testKnotenSindGleichOderUngleich() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.RadNETZ)
			.build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.RadNETZ)
			.build();
		Knoten knoten3 = knoten1;

		// Assert
		assertThat(knoten1.getId()).isNull();
		assertThat(knoten2.getId()).isNull();
		assertThat(knoten3.getId()).isNull();

		// Act
		knotenRepository.save(knoten1);
		knotenRepository.save(knoten2);
		// knoten3 kann gespeichert werden, aber in wirklichkeit wird es nicht abgelegt, weil knoten1 schon abgelegt ist
		knotenRepository.save(knoten3);
		entityManager.flush();
		entityManager.clear();

		// Assert
		assertThat(IterableUtil.sizeOf(knotenRepository.findAll())).isEqualTo(2);
	}

	@Test
	public void getKnotenFuerKanteIds() {
		assertThat(knotenRepository).isNotNull();
		assertThat(kantenRepository).isNotNull();

		Kante kante1 = KanteTestDataProvider.withDefaultValues().build();
		Kante kanteDieSichKnotenMitKante1Teilt = KanteTestDataProvider.withDefaultValues()
			.nachKnoten(kante1.getVonKnoten())
			.vonKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3., 4.), QuellSystem.DLM).build())
			.build();
		Kante kanteDerenIDNichtMitgegebenWird = KanteTestDataProvider
			.withCoordinatesAndQuelle(5, 15, 3, 23, QuellSystem.RadNETZ).build();

		kantenRepository.save(kante1);
		kantenRepository.save(kanteDieSichKnotenMitKante1Teilt);
		kantenRepository.save(kanteDerenIDNichtMitgegebenWird);

		entityManager.flush();
		entityManager.clear();

		List<Knoten> knotenFuerKanteIds = knotenRepository
			.getKnotenFuerKanteIds(Set.of(kante1.getId(), kanteDieSichKnotenMitKante1Teilt.getId()));

		assertThat(knotenFuerKanteIds).size().isEqualTo(3);
		assertThat(knotenFuerKanteIds)
			.containsExactlyInAnyOrder(kante1.getVonKnoten(), kante1.getNachKnoten(),
				kanteDieSichKnotenMitKante1Teilt.getVonKnoten());
	}

	@Test
	public void getKnotenNachNetzklasse() {
		assertThat(knotenRepository).isNotNull();
		assertThat(kantenRepository).isNotNull();

		Kante kommunalUndRadNetzKante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
				.build(), Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG), new HashSet<>()))
			.isGrundnetz(true)
			.build();
		Kante radnetzKanteDieSichKnotenMitKante1Teilt = KanteTestDataProvider.withDefaultValuesAndQuelle(
				QuellSystem.RadNETZ)
			.nachKnoten(kommunalUndRadNetzKante.getVonKnoten())
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3., 4.), QuellSystem.RadNETZ).build())
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
				.build(), Set.of(Netzklasse.RADNETZ_ALLTAG), new HashSet<>()))
			.isGrundnetz(true)
			.build();
		Kante kreisNetzKante = KanteTestDataProvider
			.withCoordinatesAndQuelle(5, 15, 3, 23, QuellSystem.DLM)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
				.build(), Set.of(Netzklasse.KREISNETZ_ALLTAG), new HashSet<>()))
			.isGrundnetz(true)
			.build();
		Kante ohneNetzklasse = KanteTestDataProvider.withCoordinatesAndQuelle(20, 20, 40, 40, QuellSystem.DLM)
			.isGrundnetz(true)
			.build();
		Kante kanteImNiemandslandOhneNetzklasse = KanteTestDataProvider
			.withCoordinatesAndQuelle(1000, 1000, 2000, 2000, QuellSystem.DLM)
			.isGrundnetz(true)
			.build();
		Kante kanteImNiemandslandRadNETZ = KanteTestDataProvider
			.withCoordinatesAndQuelle(1050, 1050, 2000, 2000, QuellSystem.RadNETZ)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
				.build(), Set.of(Netzklasse.RADNETZ_ALLTAG), new HashSet<>()))
			.isGrundnetz(true)
			.build();

		// sollte nicht geholt werden
		Kante kanteQuelleDLMmitRadNETZKlasse = KanteTestDataProvider
			.withCoordinatesAndQuelle(50, 50, 100, 100, QuellSystem.DLM)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttribute.builder()
				.build(), Set.of(Netzklasse.RADNETZ_ALLTAG), new HashSet<>()))
			.isGrundnetz(false)
			.build();

		kantenRepository.save(kommunalUndRadNetzKante);
		kantenRepository.save(radnetzKanteDieSichKnotenMitKante1Teilt);
		kantenRepository.save(kreisNetzKante);
		kantenRepository.save(ohneNetzklasse);
		kantenRepository.save(kanteImNiemandslandOhneNetzklasse);
		kantenRepository.save(kanteImNiemandslandRadNETZ);

		kantenRepository.save(kanteQuelleDLMmitRadNETZKlasse);

		entityManager.flush();
		entityManager.clear();

		List<Knoten> knotenNichtKlassifiziert = knotenRepository
			.getKnotenInBereichNachNetzklassen(new Envelope(0, 200, 0, 200),
				Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT));
		List<Knoten> knotenRadNETZ = knotenRepository
			.getKnotenInBereichNachNetzklassen(new Envelope(0, 200, 0, 200),
				Set.of(NetzklasseFilter.RADNETZ));
		List<Knoten> knotenRadNETZUndKommunal = knotenRepository
			.getKnotenInBereichNachNetzklassen(new Envelope(0, 200, 0, 200),
				Set.of(NetzklasseFilter.RADNETZ, NetzklasseFilter.KOMMUNALNETZ));
		List<Knoten> knotenRadNetzUndKreisNetz = knotenRepository
			.getKnotenInBereichNachNetzklassen(new Envelope(0, 200, 0, 200),
				Set.of(NetzklasseFilter.RADNETZ, NetzklasseFilter.KREISNETZ));

		assertThat(knotenNichtKlassifiziert)
			.containsExactlyInAnyOrder(ohneNetzklasse.getVonKnoten(), ohneNetzklasse.getNachKnoten());

		assertThat(knotenRadNETZ).size().isEqualTo(3);
		assertThat(knotenRadNETZ)
			.containsExactlyInAnyOrder(radnetzKanteDieSichKnotenMitKante1Teilt.getVonKnoten(),
				radnetzKanteDieSichKnotenMitKante1Teilt.getNachKnoten(), kommunalUndRadNetzKante.getNachKnoten());

		assertThat(knotenRadNETZUndKommunal).size().isEqualTo(3);
		assertThat(knotenRadNETZUndKommunal)
			.containsExactlyInAnyOrder(radnetzKanteDieSichKnotenMitKante1Teilt.getVonKnoten(),
				radnetzKanteDieSichKnotenMitKante1Teilt.getNachKnoten(), kommunalUndRadNetzKante.getNachKnoten());

		assertThat(knotenRadNetzUndKreisNetz).size().isEqualTo(5);
		assertThat(knotenRadNetzUndKreisNetz)
			.containsExactlyInAnyOrder(radnetzKanteDieSichKnotenMitKante1Teilt.getVonKnoten(),
				radnetzKanteDieSichKnotenMitKante1Teilt.getNachKnoten(), kommunalUndRadNetzKante.getNachKnoten(),
				kreisNetzKante.getVonKnoten(), kreisNetzKante.getNachKnoten());
	}

	@Test
	void test_deleteVerwaisteDLMKnoten() {
		// assert
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM)
			.build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 2), QuellSystem.DLM)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(4, 4), QuellSystem.DLM)
			.build();
		Knoten knoten5 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 5), QuellSystem.DLM)
			.build();

		knotenRepository.saveAll(List.of(knoten1, knoten2, knoten3, knoten4, knoten5));

		kantenRepository.save(KanteTestDataProvider
			.fromKnoten(knoten1, knoten2)
			.quelle(QuellSystem.DLM).build());
		kantenRepository.save(KanteTestDataProvider
			.fromKnoten(knoten2, knoten3)
			.quelle(QuellSystem.DLM).build());

		// act
		int numberOfDeletions = this.knotenRepository.deleteVerwaisteDLMKnoten();

		// assert
		assertThat(numberOfDeletions).isEqualTo(2);
		List<Knoten> remainingKnoten = StreamSupport.stream(knotenRepository.findAll().spliterator(), false)
			.collect(Collectors.toList());
		assertThat(remainingKnoten).containsExactlyInAnyOrder(knoten1, knoten2, knoten3);
	}

	@Test
	void test_deleteVerwaisteDLMKnoten_loeschtNurDLMKnoten() {
		// assert
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM)
			.build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 2), QuellSystem.DLM)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(4, 4), QuellSystem.DLM)
			.build();
		Knoten knoten5 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 5), QuellSystem.RadNETZ)
			.build();

		knotenRepository.saveAll(List.of(knoten1, knoten2, knoten3, knoten4, knoten5));

		kantenRepository.save(KanteTestDataProvider
			.fromKnoten(knoten1, knoten2)
			.quelle(QuellSystem.DLM).build());
		kantenRepository.save(KanteTestDataProvider
			.fromKnoten(knoten2, knoten3)
			.quelle(QuellSystem.DLM).build());

		// act
		int numberOfDeletions = this.knotenRepository.deleteVerwaisteDLMKnoten();

		// assert
		assertThat(numberOfDeletions).isEqualTo(1);
		List<Knoten> remainingKnoten = StreamSupport.stream(knotenRepository.findAll().spliterator(), false)
			.collect(Collectors.toList());
		assertThat(remainingKnoten).containsExactlyInAnyOrder(knoten1, knoten2, knoten3, knoten5);
	}

	@Test
	void test_deleteVerwaisteDLMKnoten_knotenMitNurRadVisKantenWerdenNichtGeloescht() {
		// assert
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM)
			.build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 2), QuellSystem.DLM)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(4, 4), QuellSystem.DLM)
			.build();
		Knoten knoten5 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 5), QuellSystem.DLM)
			.build();

		knotenRepository.saveAll(List.of(knoten1, knoten2, knoten3, knoten4, knoten5));

		kantenRepository.save(KanteTestDataProvider
			.fromKnoten(knoten1, knoten2)
			.quelle(QuellSystem.DLM).build());
		kantenRepository.save(KanteTestDataProvider
			.fromKnotenUndQuelle(knoten3, knoten4, QuellSystem.RadVis)
			.build());

		// act
		int numberOfDeletions = this.knotenRepository.deleteVerwaisteDLMKnoten();

		// assert that nur Knoten 5 wurde geloescht
		assertThat(numberOfDeletions).isEqualTo(1);
		List<Knoten> remainingKnoten = StreamSupport.stream(knotenRepository.findAll().spliterator(), false)
			.collect(Collectors.toList());
		assertThat(remainingKnoten).containsExactlyInAnyOrder(knoten1, knoten2, knoten3, knoten4);
	}

	@Test
	void findDlmKnotenWithOnlyRadvisKanten() {
		// assert
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1, 1), QuellSystem.DLM)
			.build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 2), QuellSystem.DLM)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(4, 4), QuellSystem.DLM)
			.build();
		Knoten knoten5 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(5, 5), QuellSystem.DLM)
			.build();

		knotenRepository.saveAll(List.of(knoten1, knoten2, knoten3, knoten4, knoten5));

		kantenRepository.save(KanteTestDataProvider
			.fromKnoten(knoten1, knoten2)
			.quelle(QuellSystem.DLM).build());
		kantenRepository.save(KanteTestDataProvider
			.fromKnotenUndQuelle(knoten1, knoten3, QuellSystem.RadVis)
			.build());
		kantenRepository.save(KanteTestDataProvider
			.fromKnotenUndQuelle(knoten3, knoten4, QuellSystem.RadVis)
			.build());

		entityManager.flush();
		entityManager.clear();

		// act
		List<Knoten> result = knotenRepository.findDlmKnotenWithoutDlmKanten();

		// assert
		assertThat(result).containsExactlyInAnyOrder(knoten3, knoten4, knoten5);
	}

	@Test
	public void geoserverBalmView_radnetzKanten() throws SQLException {
		// Arrange
		Kante kanteRadnetzAlltag = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 1, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)),
						new HashSet<>()
					)
				)
				.build());

		Kante kanteRadnetzFreizeit = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(1, 0, 1, 1, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_FREIZEIT)),
						new HashSet<>()
					)
				)
				.build());

		Kante kanteRadnetzZielnetz = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(2, 0, 2, 1, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ZIELNETZ)),
						new HashSet<>()
					)
				)
				.build());

		Kante kanteRadnetzFreizeitZielnetz = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(3, 0, 3, 1, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ZIELNETZ)),
						new HashSet<>()
					)
				)
				.build());

		Kante kanteRadnetzKreisnetz = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(4, 0, 4, 1, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.KREISNETZ_ALLTAG)),
						new HashSet<>()
					)
				)
				.build());

		Kante kanteOhneNetzklasse = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(5, 0, 5, 1, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(),
						new HashSet<>()
					)
				)
				.build());

		// Act
		kantenRepository.refreshRadVisNetzMaterializedView(); // knoten-view setzt auf mat. View der Kanten auf
		kantenRepository.refreshRadVisNetzAbschnitteMaterializedView();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KnotenRepository.GEOSERVER_BALM_KNOTEN_VIEW_NAME);

		// Assert
		Map<String, Object> expected;

		// RadNETZ Alltag
		expected = getBalmAttribute(kanteRadnetzAlltag.getVonKnoten().getId().toString(),
			kanteRadnetzAlltag.getVonKnoten().getPoint());
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteRadnetzAlltag.getNachKnoten().getId().toString(),
			kanteRadnetzAlltag.getNachKnoten().getPoint());
		assertThat(resultList).contains(expected);

		// RadNETZ Freizeit
		expected = getBalmAttribute(kanteRadnetzFreizeit.getVonKnoten().getId().toString(),
			kanteRadnetzFreizeit.getVonKnoten().getPoint());
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteRadnetzFreizeit.getNachKnoten().getId().toString(),
			kanteRadnetzFreizeit.getNachKnoten().getPoint());
		assertThat(resultList).contains(expected);

		// RadNETZ Freizeit+Zielnezt
		expected = getBalmAttribute(kanteRadnetzFreizeitZielnetz.getVonKnoten().getId().toString(),
			kanteRadnetzFreizeitZielnetz.getVonKnoten().getPoint());
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteRadnetzFreizeitZielnetz.getNachKnoten().getId().toString(),
			kanteRadnetzFreizeitZielnetz.getNachKnoten().getPoint());
		assertThat(resultList).contains(expected);

		// RadNETZ Kreisnetz
		expected = getBalmAttribute(kanteRadnetzKreisnetz.getVonKnoten().getId().toString(),
			kanteRadnetzKreisnetz.getVonKnoten().getPoint());
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteRadnetzKreisnetz.getNachKnoten().getId().toString(),
			kanteRadnetzKreisnetz.getNachKnoten().getPoint());
		assertThat(resultList).contains(expected);

		// Weitere sollten nicht noch zusätzlich vorhanden sein
		assertThat(resultList.size()).isEqualTo(8);
	}

	@Test
	public void geoserverBalmView_virtuelleKnoten() throws SQLException {
		// Arrange
		Kante kanteRadnetzAlltag = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 1, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)),
						new HashSet<>()
					)
				)
				.fuehrungsformAttributGruppe(
					new FuehrungsformAttributGruppe(
						List.of(
							FuehrungsformAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.01))
								.belagArt(BelagArt.ASPHALT)
								.build(),
							FuehrungsformAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.01, 0.4))
								.belagArt(BelagArt.BETON)
								.build(),
							FuehrungsformAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 1.0))
								.belagArt(BelagArt.NATURSTEINPFLASTER)
								.build()
						),
						false
					)
				)
				.build());

		Kante kanteRadnetzZielnetz = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(2, 0, 2, 1, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ZIELNETZ)),
						new HashSet<>()
					)
				)
				// Führungsformattribute wie oben
				.fuehrungsformAttributGruppe(
					new FuehrungsformAttributGruppe(
						List.of(
							FuehrungsformAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.01))
								.belagArt(BelagArt.ASPHALT)
								.build(),
							FuehrungsformAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.01, 0.4))
								.belagArt(BelagArt.BETON)
								.build(),
							FuehrungsformAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 1.0))
								.belagArt(BelagArt.NATURSTEINPFLASTER)
								.build()
						),
						false
					)
				)
				.build());

		Kante kanteOhneNetzklasse = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(5, 0, 5, 1, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(),
						new HashSet<>()
					)
				)
				// Führungsformattribute wie oben
				.fuehrungsformAttributGruppe(
					new FuehrungsformAttributGruppe(
						List.of(
							FuehrungsformAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.01))
								.belagArt(BelagArt.ASPHALT)
								.build(),
							FuehrungsformAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.01, 0.4))
								.belagArt(BelagArt.BETON)
								.build(),
							FuehrungsformAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 1.0))
								.belagArt(BelagArt.NATURSTEINPFLASTER)
								.build()
						),
						false
					)
				)
				.build());

		// Act
		kantenRepository.refreshRadVisNetzMaterializedView(); // knoten-view setzt auf mat. View der Kanten auf
		kantenRepository.refreshRadVisNetzAbschnitteMaterializedView();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KnotenRepository.GEOSERVER_BALM_KNOTEN_VIEW_NAME);

		// Assert
		Map<String, Object> expected;

		// RadNETZ Alltag
		expected = getBalmAttribute(kanteRadnetzAlltag.getVonKnoten().getId().toString(),
			kanteRadnetzAlltag.getVonKnoten().getPoint());
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteRadnetzAlltag.getId().toString() + "_0.01",
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(0, 0.01)));
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteRadnetzAlltag.getId().toString() + "_0.4",
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(0, 0.4)));
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteRadnetzAlltag.getNachKnoten().getId().toString(),
			kanteRadnetzAlltag.getNachKnoten().getPoint());
		assertThat(resultList).contains(expected);

		// Weitere sollten nicht noch zusätzlich vorhanden sein
		assertThat(resultList.size()).isEqualTo(4);
	}

	@Test
	public void geoserverBalmView_beruehrendeKanten() throws SQLException {
		// Arrange
		Kante kanteOhneNetzklasse = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 1, 0, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(),
						new HashSet<>()
					)
				)
				.build());

		Knoten kanteRadnetzAlltagNach = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 0), QuellSystem.DLM).build());
		Kante kanteRadnetzAlltag = kantenRepository.save(
			KanteTestDataProvider.fromKnoten(kanteOhneNetzklasse.getNachKnoten(), kanteRadnetzAlltagNach)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)),
						new HashSet<>()
					)
				)
				.build());

		Knoten kanteKommunalnetzFreizeitNach = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 0), QuellSystem.DLM).build());
		Kante kanteKommunalnetzFreizeit = kantenRepository.save(
			KanteTestDataProvider.fromKnoten(kanteRadnetzAlltag.getNachKnoten(), kanteKommunalnetzFreizeitNach)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.KOMMUNALNETZ_FREIZEIT)),
						new HashSet<>()
					)
				)
				.build());

		Knoten kanteRadnetzZielnetzNach = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(4, 0), QuellSystem.DLM).build());
		Kante kanteRadnetzZielnetz = kantenRepository.save(
			KanteTestDataProvider.fromKnoten(kanteKommunalnetzFreizeit.getNachKnoten(), kanteRadnetzZielnetzNach)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ZIELNETZ)),
						new HashSet<>()
					)
				)
				.build());

		// Act
		kantenRepository.refreshRadVisNetzMaterializedView(); // knoten-view setzt auf mat. View der Kanten auf
		kantenRepository.refreshRadVisNetzAbschnitteMaterializedView();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KnotenRepository.GEOSERVER_BALM_KNOTEN_VIEW_NAME);

		// Assert
		Map<String, Object> expected;

		expected = getBalmAttribute(kanteRadnetzAlltag.getVonKnoten().getId().toString(),
			kanteRadnetzAlltag.getVonKnoten().getPoint());
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteKommunalnetzFreizeit.getVonKnoten().getId().toString(),
			kanteKommunalnetzFreizeit.getVonKnoten().getPoint());
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteKommunalnetzFreizeit.getNachKnoten().getId().toString(),
			kanteKommunalnetzFreizeit.getNachKnoten().getPoint());
		assertThat(resultList).contains(expected);

		// RadNETZ Zielnezt: Sollte nicht noch zusätzlich vorhanden sein
		assertThat(resultList.size()).isEqualTo(3);
	}

	@Test
	public void geoserverBalmView_knotenVonRadrouten() throws SQLException {
		// Arrange
		Kante kanteOhneNetzklasse1 = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 1, 0, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(),
						new HashSet<>()
					)
				)
				.build());

		Knoten kanteOhneNetzklasse2Nach = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(2, 0), QuellSystem.DLM).build());
		Kante kanteOhneNetzklasse2 = kantenRepository.save(
			KanteTestDataProvider.fromKnoten(kanteOhneNetzklasse1.getNachKnoten(), kanteOhneNetzklasse2Nach)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(),
						new HashSet<>()
					)
				)
				.build());

		Knoten kanteRadnetzZielnetz1Nach = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 0), QuellSystem.DLM).build());
		Kante kanteRadnetzZielnetz1 = kantenRepository.save(
			KanteTestDataProvider.fromKnoten(kanteOhneNetzklasse2.getNachKnoten(), kanteRadnetzZielnetz1Nach)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ZIELNETZ)),
						new HashSet<>()
					)
				)
				.build());

		Knoten kanteRadnetzZielnetz2Nach = knotenRepository.save(
			KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(4, 0), QuellSystem.DLM).build());
		Kante kanteRadnetzZielnetz2 = kantenRepository.save(
			KanteTestDataProvider.fromKnoten(kanteRadnetzZielnetz1.getNachKnoten(), kanteRadnetzZielnetz2Nach)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder().build(),
						new HashSet<>(Set.of(Netzklasse.RADNETZ_ZIELNETZ)),
						new HashSet<>()
					)
				)
				.build());

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("tolle Organisation")
				.organisationsArt(
					OrganisationsArt.KREIS)
				.build());

		Fahrradroute radrouteOhneNetzklasse = FahrradrouteTestDataProvider.onKante(kanteOhneNetzklasse2)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.verantwortlich(gebietskoerperschaft)
			.name(FahrradrouteName.of("Mit ohne Netzklassen"))
			.build();
		fahrradrouteRepository.save(radrouteOhneNetzklasse);

		Fahrradroute radrouteZielnetz = FahrradrouteTestDataProvider.onKante(kanteRadnetzZielnetz1)
			.kategorie(Kategorie.D_ROUTE)
			.verantwortlich(gebietskoerperschaft)
			.name(FahrradrouteName.of("Das Ziel ist das Netz"))
			.build();
		fahrradrouteRepository.save(radrouteZielnetz);

		Fahrradroute radrouteIgnoriert = FahrradrouteTestDataProvider.onKante(kanteRadnetzZielnetz2)
			.kategorie(Kategorie.RADFERNWEG)
			.verantwortlich(gebietskoerperschaft)
			.name(FahrradrouteName.of("Nicht mal LRFW oder D-Route!? Pah!"))
			.build();
		fahrradrouteRepository.save(radrouteZielnetz);

		// Act
		kantenRepository.refreshRadVisNetzMaterializedView(); // knoten-view setzt auf mat. View der Kanten auf
		kantenRepository.refreshRadVisNetzAbschnitteMaterializedView();

		List<Map<String, Object>> resultList = jdbcTemplate.queryForList(
			"SELECT * FROM " + KnotenRepository.GEOSERVER_BALM_KNOTEN_VIEW_NAME);

		// Assert
		Map<String, Object> expected;

		// Radroute auf Kante mit ohne Netzklassen
		expected = getBalmAttribute(kanteOhneNetzklasse2.getVonKnoten().getId().toString(),
			kanteOhneNetzklasse2.getVonKnoten().getPoint());
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteOhneNetzklasse2.getNachKnoten().getId().toString(),
			kanteOhneNetzklasse2.getNachKnoten().getPoint());
		assertThat(resultList).contains(expected);

		// Radroute auf Zielnetz-Kante
		expected = getBalmAttribute(kanteRadnetzZielnetz1.getVonKnoten().getId().toString(),
			kanteRadnetzZielnetz1.getVonKnoten().getPoint());
		assertThat(resultList).contains(expected);

		expected = getBalmAttribute(kanteRadnetzZielnetz1.getNachKnoten().getId().toString(),
			kanteRadnetzZielnetz1.getNachKnoten().getPoint());
		assertThat(resultList).contains(expected);

		// Weitere sollten nicht noch zusätzlich vorhanden sein (3, da kanten sich berühren)
		assertThat(resultList.size()).isEqualTo(3);
	}

	@NotNull
	private static HashMap<String, Object> getBalmAttribute(String knotenId, Point knotenPoint) throws SQLException {
		return new HashMap<>() {{
			put("Quell-ID", knotenId);
			put("Knoten-ID", "08" + Math.round(knotenPoint.getX()) + Math.round(
				knotenPoint.getY()));
			put("Datum", null);
			put("GeometrieKnoten", PostGisHelper.getPGobject(knotenPoint));
			put("lebenszeitIntervallAnfang", null);
			put("lebenszeitIntervallEnde", null);
		}};
	}
}
