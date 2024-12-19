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

package de.wps.radvis.backend.konsistenz.pruefung.domain;

import static de.wps.radvis.backend.netz.domain.valueObject.IstStandard.STARTSTANDARD_RADNETZ;
import static de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND;
import static de.wps.radvis.backend.netz.domain.valueObject.Richtung.IN_RICHTUNG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mockStatic;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelPruefJobStatistik;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.konsistenz.regeln.domain.MindestbreiteKonsistenzregel;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group3")
@ContextConfiguration(classes = {
	KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	FeatureToggleProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
class KonsistenzregelPruefJobTestIT extends DBIntegrationTestIT {

	@MockBean
	public JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private KonsistenzregelVerletzungsRepository verletzungsRepository;

	@Autowired
	private KantenRepository kantenRepository;

	private KonsistenzregelPruefJob konsistenzregelPruefJob;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	private String regelName = MindestbreiteKonsistenzregel.class.getSimpleName();

	private MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@BeforeEach
	void setUp() {
		konsistenzregelPruefJob = new KonsistenzregelPruefJob(List.of(new MindestbreiteKonsistenzregel(jdbcTemplate)),
			verletzungsRepository, jobExecutionDescriptionRepository);
		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@SuppressWarnings("unchecked")
	@Test
	void doRun_legeNeueVerletzungAn() {
		kantenRepository.save(
			KanteTestDataProvider.withRichtungRadverkehrsfuehrungIstStandardBreiteQuellsystem(
				IN_RICHTUNG, SONDERWEG_RADWEG_STRASSENBEGLEITEND, STARTSTANDARD_RADNETZ, 1.39, QuellSystem.DLM,
				false).build());
		entityManager.flush();
		entityManager.clear();

		// Act
		KonsistenzregelPruefJobStatistik jobStatistik = (KonsistenzregelPruefJobStatistik) konsistenzregelPruefJob
			.doRun()
			.get();

		// Assert
		assertThat(jobStatistik.anzahlBestehendProRegel.get(regelName)).isEqualTo(0);
		assertThat(jobStatistik.anzahlAktuellProRegel.get(regelName)).isEqualTo(1);
		assertThat(jobStatistik.anzahlDeletedProRegel.get(regelName)).isEqualTo(0);
		assertThat(jobStatistik.anzahlUpdatedProRegel.get(regelName)).isEqualTo(0);
		assertThat(jobStatistik.anzahlCreatedProRegel.get(regelName)).isEqualTo(1);
		assertThat(verletzungsRepository.findAllByTyp(MindestbreiteKonsistenzregel.VERLETZUNGS_TYP).size()).isEqualTo(
			1);
	}

	@Test
	void doRun_kanteExistiertNichtMehr_entferneVerletzung() {
		// Arrange
		kantenRepository.save(
			KanteTestDataProvider.withRichtungRadverkehrsfuehrungIstStandardBreiteQuellsystem(
				IN_RICHTUNG, SONDERWEG_RADWEG_STRASSENBEGLEITEND, STARTSTANDARD_RADNETZ, 1.39, QuellSystem.DLM,
				false).build());
		entityManager.flush();
		entityManager.clear();

		konsistenzregelPruefJob.doRun();
		entityManager.flush();
		entityManager.clear();

		kantenRepository.deleteAll();
		entityManager.flush();
		entityManager.clear();

		// Act
		KonsistenzregelPruefJobStatistik jobStatistik = (KonsistenzregelPruefJobStatistik) konsistenzregelPruefJob
			.doRun()
			.get();

		// Assert
		assertThat(jobStatistik.anzahlBestehendProRegel.get(regelName)).isEqualTo(1);
		assertThat(jobStatistik.anzahlAktuellProRegel.get(regelName)).isEqualTo(0);
		assertThat(jobStatistik.anzahlDeletedProRegel.get(regelName)).isEqualTo(1);
		assertThat(jobStatistik.anzahlUpdatedProRegel.get(regelName)).isEqualTo(0);
		assertThat(jobStatistik.anzahlCreatedProRegel.get(regelName)).isEqualTo(0);
		assertThat(verletzungsRepository.findAllByTyp(MindestbreiteKonsistenzregel.VERLETZUNGS_TYP).size()).isEqualTo(
			0);
	}

	@Test
	void doRun_verletzungTrittWeiterhinAuf_verletzungWirdAktualisiert() {
		// Arrange
		Coordinate vorhandeneStartKoordinate = new Coordinate(100, 100);
		Coordinate vorhandeneEndKoordinate = new Coordinate(200, 200);
		Kante kante = kantenRepository.save(
			KanteTestDataProvider.withRichtungRadverkehrsfuehrungIstStandardBreiteQuellsystem(
				IN_RICHTUNG, SONDERWEG_RADWEG_STRASSENBEGLEITEND, STARTSTANDARD_RADNETZ, 1.39, QuellSystem.DLM,
				false)
				.geometry(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createLineString(new Coordinate[] { vorhandeneStartKoordinate, vorhandeneEndKoordinate }))
				.vonKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(vorhandeneStartKoordinate, QuellSystem.DLM).build())
				.nachKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(vorhandeneEndKoordinate, QuellSystem.DLM).build())
				.build());
		entityManager.flush();
		entityManager.clear();

		konsistenzregelPruefJob.doRun();
		entityManager.flush();
		entityManager.clear();

		LocalDateTime datumVorherigerDurchlauf = verletzungsRepository.findAllByTyp(
			MindestbreiteKonsistenzregel.VERLETZUNGS_TYP).get(0).getDatum();

		// neue Geometrie ist ganz leicht verschoben
		kante.updateDLMGeometry(
			GeometryTestdataProvider.createLineString(vorhandeneStartKoordinate, new Coordinate(200, 200.01)));
		kantenRepository.save(kante);

		entityManager.flush();
		entityManager.clear();

		// Act
		KonsistenzregelPruefJobStatistik jobStatistik = (KonsistenzregelPruefJobStatistik) konsistenzregelPruefJob
			.doRun()
			.get();

		// Assert
		regelName = regelName;
		assertThat(jobStatistik.anzahlBestehendProRegel.get(regelName)).isEqualTo(1);
		assertThat(jobStatistik.anzahlAktuellProRegel.get(regelName)).isEqualTo(1);
		assertThat(jobStatistik.anzahlDeletedProRegel.get(regelName)).isEqualTo(0);
		assertThat(jobStatistik.anzahlUpdatedProRegel.get(regelName)).isEqualTo(1);
		assertThat(jobStatistik.anzahlCreatedProRegel.get(regelName)).isEqualTo(0);
		assertThat(verletzungsRepository.findAllByTyp(MindestbreiteKonsistenzregel.VERLETZUNGS_TYP).get(0)
			.getDatum()).isNotEqualTo(datumVorherigerDurchlauf);
	}
}
