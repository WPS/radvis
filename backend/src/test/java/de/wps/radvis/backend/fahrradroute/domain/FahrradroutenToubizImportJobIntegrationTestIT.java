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

package de.wps.radvis.backend.fahrradroute.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.DlmMatchedGraphhopperTestdataProvider;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.ImportedToubizRouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.repository.ToubizRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.fahrradroute.schnittstelle.ToubizConfigurationProperties;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchingRepositoryImpl;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;

@Tag("group2")
@ContextConfiguration(classes = {
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	FahrradrouteConfiguration.class,
	CommonConfiguration.class,
	MatchingConfiguration.class,
	NetzfehlerConfiguration.class,
	KommentarConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class,
	BarriereConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	JobConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	DLMConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	CommonConfigurationProperties.class,
	ToubizConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
@ActiveProfiles(profiles = "test")
class FahrradroutenToubizImportJobIntegrationTestIT extends DBIntegrationTestIT {

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	private FahrradroutenToubizImportJob fahrradroutenToubizImportJob;

	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	ToubizRepository toubizRepository;

	@Autowired
	FahrradrouteService fahrradrouteService;

	@Autowired
	FahrradrouteRepository fahrradrouteRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	GraphhopperRoutingRepository graphhopperRoutingRepository;

	@Autowired
	VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	EntityManager entityManager;

	@TempDir
	public File temp;

	private Kante kante1;
	private Kante kante2;
	private Kante kante3;
	private Kante kante4;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		kante1 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(417704.57, 5288712.62, 417919.10, 5288811.05,
				QuellSystem.DLM).build());
		kante2 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(417919.10, 5288811.05, 417857.16, 5289062.56,
				QuellSystem.DLM).build());
		kante3 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(417857.16, 5289062.56, 417827.06, 5289146.35,
				QuellSystem.DLM).build());
		kante4 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(417827.06, 5289146.35, 417727.06, 5289046.35,
				QuellSystem.DLM).build());

		List<Kante> kanten = List.of(kante1, kante2, kante3, kante4);

		DlmMatchingRepositoryImpl dlmMatchingRepository = DlmMatchedGraphhopperTestdataProvider.reimportWithKanten(
			List.of(kanten), "test-toubiz-import-job", temp);

		FahrradroutenMatchingService fahrradroutenMatchingService = new FahrradroutenMatchingService(
			kantenRepository,
			org.springframework.data.util.Lazy.of(() -> dlmMatchingRepository),
			org.springframework.data.util.Lazy.of(() -> graphhopperRoutingRepository),
			verwaltungseinheitService);

		fahrradroutenToubizImportJob = new FahrradroutenToubizImportJob(
			jobExecutionDescriptionRepository,
			toubizRepository,
			verwaltungseinheitService, fahrradrouteRepository,
			fahrradroutenMatchingService);
	}

	@Test
	void run_fuegt_neue_hinzu_und_entfernt_alte_lrfw_wird_nicht_geloescht() {
		// arrange
		ToubizId neu = ToubizId.of("1 Neu");
		ToubizId vorhanden = ToubizId.of("2 Vorhanden");
		ToubizId veraltet = ToubizId.of("3 Veraltet");
		ToubizId lrfwId = ToubizId.of("Ich bin ein LRFW");

		Verwaltungseinheit testOrga = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(400000, 5000000, 500000, 6000000))
				.build());

		fahrradrouteRepository.save(
			FahrradrouteTestDataProvider.onKante(kante3)
				.toubizId(veraltet)
				.verantwortlich(testOrga)
				.build());
		fahrradrouteRepository.save(
			FahrradrouteTestDataProvider.onKante(kante2)
				.toubizId(vorhanden)
				.verantwortlich(testOrga)
				.beschreibung("alte Beschreibung")
				.build());

		fahrradrouteRepository.save(
			FahrradrouteTestDataProvider.onKante(kante4)
				.toubizId(lrfwId)
				.beschreibung("LRFW Beschreibung")
				.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
				.kategorie(Kategorie.LANDESRADFERNWEG)
				.verantwortlich(testOrga)
				.build());

		when(toubizRepository.importRouten()).thenReturn(List.of(
			ImportedToubizRouteTestDataProvider.withDefaultValues()
				.toubizId(neu)
				.originalGeometrie(GEO_FACTORY.createLineString(kante1.getGeometry().getCoordinates()))
				.build(),
			ImportedToubizRouteTestDataProvider.withDefaultValues()
				.toubizId(vorhanden)
				.beschreibung("neue Beschreibung")
				.originalGeometrie(GEO_FACTORY.createLineString(kante2.getGeometry().getCoordinates()))
				.build()));

		entityManager.flush();
		entityManager.clear();

		// act
		fahrradroutenToubizImportJob.doRun();

		// assert
		assertThat(fahrradrouteRepository.findAllToubizIdsWithoutLandesradfernwege()).containsExactlyInAnyOrder(neu,
			vorhanden);
		assertThat(fahrradrouteRepository.findByToubizId(vorhanden).get().getBeschreibung())
			.isEqualTo("neue Beschreibung");

		// Landesradfernwege dürfen nicht gelöscht werden beim Import
		assertThat(fahrradrouteRepository.findByToubizId(lrfwId)).isPresent();
	}

	@Test
	void testDoRun_landesradfernwege_bereitsAngelegteWerdenGeupdatetAndereIgnoriert() {
		// arrange
		ToubizId angelegterLrfwId = ToubizId.of("Ich bin ein angelegter LRFW");
		ToubizId andererLrfwId = ToubizId.of("Ich bin ein nicht angelegter LRFW");

		Verwaltungseinheit testOrga = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(400000, 5000000, 500000, 6000000))
				.build());

		Long idBestehenderLRFW = fahrradrouteRepository.save(
				FahrradrouteTestDataProvider.onKante(kante1)
					.toubizId(angelegterLrfwId)
					.beschreibung("Alte Beschreibung")
					.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
					.kategorie(Kategorie.LANDESRADFERNWEG)
					.verantwortlich(testOrga)
					.build())
			.getId();

		when(toubizRepository.importRouten()).thenReturn(List.of(
			ImportedToubizRouteTestDataProvider.withDefaultValues()
				.toubizId(angelegterLrfwId)
				.originalGeometrie(GEO_FACTORY.createLineString(kante2.getGeometry().getCoordinates()))
				.landesradfernweg(true)
				.beschreibung("Neue Beschreibung")
				.build(),
			ImportedToubizRouteTestDataProvider.withDefaultValues()
				.toubizId(andererLrfwId)
				.originalGeometrie(GEO_FACTORY.createLineString(kante2.getGeometry().getCoordinates()))
				.landesradfernweg(true)
				.beschreibung("Neue Beschreibung")
				.build()));

		entityManager.flush();
		entityManager.clear();

		// act
		fahrradroutenToubizImportJob.doRun();

		// assert
		Fahrradroute route = fahrradrouteRepository.findById(idBestehenderLRFW).get();
		assertThat(route.getBeschreibung()).isEqualTo("Neue Beschreibung");
		// Ändert nicht den Verlauf
		assertThat(route.getAbschnittsweiserKantenBezug().stream().findFirst().get().getKante().getId())
			.isEqualTo(kante1.getId());

		// Der nicht in RadVIS angelegte LRFW sollte nicht importiert werden
		assertThat(fahrradrouteRepository.findAll()).hasSize(1);

	}
}
