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

package de.wps.radvis.backend.massnahme.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmenConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.UmsetzungsstandsabfrageConfigurationProperties;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group1")
@ContextConfiguration(classes = {
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	MassnahmeConfiguration.class,
	DokumentConfiguration.class,
	KommentarConfiguration.class,
	CommonConfiguration.class,
	MailConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	JobConfigurationProperties.class,
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	MailConfigurationProperties.class,
	UmsetzungsstandsabfrageConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	MassnahmenConfigurationProperties.class,
	NetzConfigurationProperties.class
})
class MassnahmeGeoserverViewTestIT extends DBIntegrationTestIT {

	private Gebietskoerperschaft gebietskoerperschaft;

	@MockitoBean
	private ShapeFileRepository shapeFileRepository;

	@MockitoBean
	private SimpleMatchingService simpleMatchingService;

	@MockitoBean
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@MockitoBean
	private BenutzerResolver benutzerResolver;

	@MockitoBean
	private MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;

	@MockitoBean
	private FahrradrouteRepository fahrradrouteRepository;

	@Autowired
	private MassnahmeRepository massnahmeRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	private BenutzerRepository benutzerRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Coole Organisation")
				.organisationsArt(
					OrganisationsArt.BUNDESLAND)
				.build());
	}

	@Test
	void divideLinesAndPoints() {
		// arrange
		Kante kante = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).build());

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 0.3), Seitenbezug.LINKS),
				new AbschnittsweiserKantenSeitenBezug(
					kante, LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Set.of(kante.getVonKnoten()));

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.netzbezug(netzbezug)
			.build();

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act
		List<Map<String, Object>> punktMassnahmen = jdbcTemplate
			.queryForList("Select st_astext(geometry) as geometry from geoserver_massnahme_points_view");
		List<Map<String, Object>> linienMassnahmen = jdbcTemplate
			.queryForList("Select st_astext(geometry) as geometry from geoserver_massnahme_lines_view");

		// assert
		assertThat(punktMassnahmen).hasSize(1);
		assertThat(punktMassnahmen.get(0).get("geometry"))
			.matches(g -> g.equals("MULTIPOINT(25 0,0 0)") || g.equals("MULTIPOINT(0 0,25 0)"));
		assertThat(linienMassnahmen).hasSize(1);
		assertThat(linienMassnahmen.get(0).get("geometry"))
			.matches(g -> g.equals("MULTILINESTRING((0 0,30 0),(50 0,80 0))")
				|| g.equals("MULTILINESTRING((50 0,80 0),(0 0,30 0))"));
	}

	@Test
	void prefersSnapshotsIfArchiviert() {
		// arrange
		Kante kante = kantenRepository.save(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).build());

		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 0.3), Seitenbezug.LINKS),
				new AbschnittsweiserKantenSeitenBezug(
					kante, LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Set.of(kante.getVonKnoten()));

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.baulastZustaendiger(gebietskoerperschaft)
			.unterhaltsZustaendiger(gebietskoerperschaft)
			.zustaendiger(gebietskoerperschaft)
			.benutzerLetzteAenderung(
				benutzerRepository.save(BenutzerTestDataProvider.admin(gebietskoerperschaft).build()))
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.netzbezug(netzbezug)
			.build();

		massnahme.archivieren();

		massnahme.removeKanteFromNetzbezug(Set.of(kante.getId()));
		massnahme.removeKnotenFromNetzbezug(Set.of(kante.getVonKnoten().getId()));

		massnahme = massnahmeRepository.save(massnahme);

		entityManager.flush();

		// act
		List<Map<String, Object>> punktMassnahmen = jdbcTemplate
			.queryForList("Select st_astext(geometry) as geometry, archiviert from geoserver_massnahme_points_view");
		List<Map<String, Object>> linienMassnahmen = jdbcTemplate
			.queryForList("Select st_astext(geometry) as geometry, archiviert from geoserver_massnahme_lines_view");

		// assert
		assertThat(punktMassnahmen).hasSize(1);
		assertThat((Boolean) punktMassnahmen.get(0).get("archiviert")).isTrue();
		assertThat(punktMassnahmen.get(0).get("geometry"))
			.matches(g -> g.equals("MULTIPOINT(25 0,0 0)") || g.equals("MULTIPOINT(0 0,25 0)"));

		assertThat(linienMassnahmen).hasSize(1);
		assertThat((Boolean) linienMassnahmen.get(0).get("archiviert")).isTrue();
		assertThat(linienMassnahmen.get(0).get("geometry"))
			.matches(g -> g.equals("MULTILINESTRING((0 0,30 0),(50 0,80 0))")
				|| g.equals("MULTILINESTRING((50 0,80 0),(0 0,30 0))"));
	}
}
