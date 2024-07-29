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

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteNetzBezugAenderung;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.FahrtrichtungAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group6")
@ContextConfiguration(classes = { FahrradrouteNetzBezugAenderungRepositoryTestIT.TestConfiguration.class,
	CommonConfiguration.class,
	GeoConverterConfiguration.class, })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class, FeatureToggleProperties.class,
	PostgisConfigurationProperties.class })
public class FahrradrouteNetzBezugAenderungRepositoryTestIT extends DBIntegrationTestIT {
	@EnableEnversRepositories(basePackages = { "de.wps.radvis.backend.fahrradroute", "de.wps.radvis.backend.netz",
		"de.wps.radvis.backend.organisation", "de.wps.radvis.backend.benutzer" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.fahrradroute", "de.wps.radvis.backend.organisation",
		"de.wps.radvis.backend.netz", "de.wps.radvis.backend.benutzer", "de.wps.radvis.backend.common" })
	public static class TestConfiguration {
		@Autowired
		private KantenRepository kantenRepository;
		@Autowired
		private KnotenRepository knotenRepository;
		@MockBean
		private ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppenRepository;
		@MockBean
		private FahrtrichtungAttributGruppeRepository fahrtrichtungAttributGruppeRepository;
		@MockBean
		private GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;
		@MockBean
		private FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppenRepository;
		@MockBean
		private KantenAttributGruppeRepository kantenAttributGruppenRepository;
		@MockBean
		private VerwaltungseinheitResolver verwaltungseinheitResolver;

		@Bean
		NetzService netzService() {
			return new NetzService(kantenRepository, knotenRepository, zustaendigkeitAttributGruppenRepository,
				fahrtrichtungAttributGruppeRepository, geschwindigkeitAttributGruppeRepository,
				fuehrungsformAttributGruppenRepository, kantenAttributGruppenRepository, verwaltungseinheitResolver);
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
	@Autowired
	FahrradrouteNetzBezugAenderungRepository fahrradrouteAenderungRepository;
	@Autowired
	BenutzerRepository benutzerRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	public void findFahrradrouteNetzBezugAenderungByDatumAfter() {
		// arrange
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());

		Fahrradroute fahrradroute = fahrradrouteRepository.save(FahrradrouteTestDataProvider.onKante(
			netzService.saveKante(KanteTestDataProvider.withDefaultValues().build()))
			.verantwortlich(gebietskoerperschaft).build());
		Benutzer technischerBenutzer = benutzerRepository.save(
			BenutzerTestDataProvider.admin(gebietskoerperschaft).build());

		// alte Aenderung (soll nicht gefunden werden)
		fahrradrouteAenderungRepository.save(
			new FahrradrouteNetzBezugAenderung(NetzBezugAenderungsArt.KANTE_GELOESCHT, 1L, fahrradroute,
				technischerBenutzer, LocalDateTime.of(2022, 10, 12, 9, 59), NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				GeometryTestdataProvider.createLineString(), true));

		// aktuelle Aenderung
		FahrradrouteNetzBezugAenderung aktuellereAenderung = fahrradrouteAenderungRepository.save(
			new FahrradrouteNetzBezugAenderung(NetzBezugAenderungsArt.KANTE_GELOESCHT, 1L, fahrradroute,
				technischerBenutzer, LocalDateTime.of(2022, 10, 13, 10, 0), NetzAenderungAusloeser.DLM_REIMPORT_JOB,
				GeometryTestdataProvider.createLineString(), true));

		entityManager.flush();

		// act
		List<FahrradrouteNetzBezugAenderung> result = fahrradrouteAenderungRepository
			.findFahrradrouteNetzBezugAenderungByDatumAfter(LocalDateTime.of(2022, 10, 12, 10, 0));

		// assert
		assertThat(fahrradrouteAenderungRepository.findAll()).hasSize(2);
		assertThat(result).hasSize(1);
		assertThat(result).containsExactly(aktuellereAenderung);
	}
}
