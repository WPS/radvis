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

package de.wps.radvis.backend.fahrradzaehlstelle.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Channel;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.FahrradzaehlDatenEintrag;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Fahrradzaehlstelle;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.FahrradzaehlstellenMobiDataImportStatistik;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.BetreiberEigeneId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleGebietskoerperschaft;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Seriennummer;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlintervall;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstand;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstatus;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;

@Tag("group4")
@ContextConfiguration(classes = { FahrradzaehlstellenMobiDataImportJobTestIT.TestConfiguration.class,
	CommonConfiguration.class, GeoConverterConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class })
class FahrradzaehlstellenMobiDataImportJobTestIT extends DBIntegrationTestIT {

	@EnableJpaRepositories(basePackages = { "de.wps.radvis.backend.fahrradzaehlstelle" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.fahrradzaehlstelle", "de.wps.radvis.backend.common" })
	public static class TestConfiguration {
	}

	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	@Mock
	JobConfigurationProperties jobConfigurationProperties;

	@Autowired
	FahrradzaehlstelleRepository fahrradzaehlstelleRepository;
	@Autowired
	CsvRepository csvRepository;
	@Autowired
	CoordinateReferenceSystemConverter converter;

	@PersistenceContext
	EntityManager entityManager;

	private FahrradzaehlstellenMobiDataImportJob fahrradzaehlstellenMobiDataImportJob;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		fahrradzaehlstellenMobiDataImportJob = new FahrradzaehlstellenMobiDataImportJob(
			jobExecutionDescriptionRepository, jobConfigurationProperties, fahrradzaehlstelleRepository, csvRepository,
			converter);
	}

	@Test
	void doRun_1Datei_mehrereZaehlstellenChannelDaten() throws FactoryException, TransformException {
		// arrange
		File fileToImport = new File(
			"src/test/resources/fahrradzaehlstellenTestImportFiles/1Datei_mehrereStellenChannelDaten/");
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportBaseUrl()).thenReturn(
			fileToImport.toURI().toString());
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportStartDate()).thenReturn("202205");

		// act
		FahrradzaehlstellenMobiDataImportStatistik jobStatistik = (FahrradzaehlstellenMobiDataImportStatistik) fahrradzaehlstellenMobiDataImportJob.doRun()
			.get();

		// assert
		List<Fahrradzaehlstelle> alleZaehlstellen = StreamSupport.stream(
			fahrradzaehlstelleRepository.findAll().spliterator(),
			false).collect(Collectors.toList());

		assertThat(alleZaehlstellen).hasSize(2);

		List<Fahrradzaehlstelle> alleZaehlstellenMitId100003358 = fahrradzaehlstelleRepository.findAllByBetreiberEigeneIdIn(
			Set.of(BetreiberEigeneId.of(100003358L)));
		assertThat(alleZaehlstellenMitId100003358).hasSize(1);
		Fahrradzaehlstelle fahrradzaehlstelle1 = alleZaehlstellenMitId100003358.get(0);
		assertThat(fahrradzaehlstelle1.getSeriennummer().get()).isEqualTo(Seriennummer.of("YTG13063794"));
		assertThat(fahrradzaehlstelle1.getFahrradzaehlstelleBezeichnung().get())
			.isEqualTo(FahrradzaehlstelleBezeichnung.of("counterSite1"));
		assertThat(fahrradzaehlstelle1.getFahrradzaehlstelleGebietskoerperschaft().get())
			.isEqualTo(FahrradzaehlstelleGebietskoerperschaft.of("Tübingen"));
		assertThat(fahrradzaehlstelle1.getZaehlintervall().get())
			.isEqualTo(Zaehlintervall.of("15"));
		assertThat(fahrradzaehlstelle1.getGeometrie().getCoordinate())
			.isEqualTo(createCoordinateFromWG84InUtm32(9.048007, 48.518017));

		assertThat(fahrradzaehlstelle1.getNeusterZeitstempel()).isEqualTo(Zeitstempel.of("2021-01-03T10:00:00+0100"));

		assertThat(fahrradzaehlstelle1.getChannels()).hasSize(2);
		Optional<Channel> zaehlstelle1Channel1 = fahrradzaehlstelle1.getChannels().stream()
			.filter(channel -> channel.getChannelId().equals(ChannelId.of(101003358L))).findFirst();
		assertThat(zaehlstelle1Channel1).isPresent();
		assertThat(zaehlstelle1Channel1.get().getChannelBezeichnung().get()).isEqualTo(
			ChannelBezeichnung.of("Richtung Altstadt"));
		Map<Zeitstempel, FahrradzaehlDatenEintrag> expectedFahrradzaehlDaten11 = new HashMap<>();
		expectedFahrradzaehlDaten11.put(Zeitstempel.of("2021-01-01T01:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(11L), Zaehlstatus.of(0)));
		expectedFahrradzaehlDaten11.put(Zeitstempel.of("2021-01-01T02:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(22L), Zaehlstatus.of(0)));
		assertThat(zaehlstelle1Channel1.get().getFahrradzaehlDaten()).isEqualTo(expectedFahrradzaehlDaten11);

		Optional<Channel> zaehlstelle1Channel2 = fahrradzaehlstelle1.getChannels().stream()
			.filter(channel -> channel.getChannelId().equals(ChannelId.of(102003358L))).findFirst();
		assertThat(zaehlstelle1Channel2).isPresent();
		assertThat(zaehlstelle1Channel2.get().getChannelBezeichnung().get()).isEqualTo(
			ChannelBezeichnung.of("Richtung Bahnhof"));
		Map<Zeitstempel, FahrradzaehlDatenEintrag> expectedFahrradzaehlDaten12 = new HashMap<>();
		expectedFahrradzaehlDaten12.put(Zeitstempel.of("2021-01-03T09:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(33L), Zaehlstatus.of(0)));
		expectedFahrradzaehlDaten12.put(Zeitstempel.of("2021-01-03T10:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(44L), Zaehlstatus.of(0)));
		assertThat(zaehlstelle1Channel2.get().getFahrradzaehlDaten()).isEqualTo(expectedFahrradzaehlDaten12);

		List<Fahrradzaehlstelle> alleZaehlstellenMitId100003359 = fahrradzaehlstelleRepository.findAllByBetreiberEigeneIdIn(
			Set.of(BetreiberEigeneId.of(100003359L)));
		assertThat(alleZaehlstellenMitId100003359).hasSize(1);
		Fahrradzaehlstelle fahrradzaehlstelle2 = alleZaehlstellenMitId100003359.get(0);
		assertThat(fahrradzaehlstelle2.getSeriennummer().get()).isEqualTo(Seriennummer.of("Y2H17123962"));
		assertThat(fahrradzaehlstelle2.getFahrradzaehlstelleBezeichnung().get())
			.isEqualTo(FahrradzaehlstelleBezeichnung.of("counterSite2"));
		assertThat(fahrradzaehlstelle2.getFahrradzaehlstelleGebietskoerperschaft().get())
			.isEqualTo(FahrradzaehlstelleGebietskoerperschaft.of("Tübingen"));
		assertThat(fahrradzaehlstelle2.getZaehlintervall().get())
			.isEqualTo(Zaehlintervall.of("15"));
		assertThat(fahrradzaehlstelle2.getGeometrie().getCoordinate())
			.isEqualTo(createCoordinateFromWG84InUtm32(9.058865, 48.515434));

		assertThat(fahrradzaehlstelle2.getNeusterZeitstempel()).isEqualTo(Zeitstempel.of("2021-01-17T02:00:00+0100"));

		assertThat(fahrradzaehlstelle2.getChannels()).hasSize(1);
		Channel zaehlstelle2Channel1 = fahrradzaehlstelle2.getChannels().get(0);
		assertThat(zaehlstelle2Channel1.getChannelId()).isEqualTo(ChannelId.of(102003359L));
		assertThat(zaehlstelle2Channel1.getChannelBezeichnung().get()).isEqualTo(
			ChannelBezeichnung.of("Richtung Derendingen"));
		Map<Zeitstempel, FahrradzaehlDatenEintrag> expectedFahrradzaehlDaten21 = new HashMap<>();
		expectedFahrradzaehlDaten21.put(Zeitstempel.of("2021-01-17T01:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(55L), Zaehlstatus.of(0)));
		expectedFahrradzaehlDaten21.put(Zeitstempel.of("2021-01-17T02:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(66L), Zaehlstatus.of(0)));
		assertThat(zaehlstelle2Channel1.getFahrradzaehlDaten()).isEqualTo(expectedFahrradzaehlDaten21);
	}

	@Test
	void doRun_2Datei_1Zaehlstelle_1Channel_ProDatei1Datenzeile() throws FactoryException, TransformException {
		// arrange
		File fileToImport = new File(
			"src/test/resources/fahrradzaehlstellenTestImportFiles/2Dateien_1Stelle_1Channel_proDatei1Datenzeile/");
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportBaseUrl()).thenReturn(
			fileToImport.toURI().toString());
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportStartDate()).thenReturn("202205");

		// act
		FahrradzaehlstellenMobiDataImportStatistik jobStatistik = (FahrradzaehlstellenMobiDataImportStatistik) fahrradzaehlstellenMobiDataImportJob.doRun()
			.get();

		// assert
		List<Fahrradzaehlstelle> alleZaehlstellen = StreamSupport.stream(
			fahrradzaehlstelleRepository.findAll().spliterator(),
			false).collect(Collectors.toList());

		assertThat(alleZaehlstellen).hasSize(1);
		Fahrradzaehlstelle fahrradzaehlstelle = alleZaehlstellen.get(0);
		assertThat(fahrradzaehlstelle.getBetreiberEigeneId()).isEqualTo(BetreiberEigeneId.of(100003358L));
		assertThat(fahrradzaehlstelle.getSeriennummer().get()).isEqualTo(Seriennummer.of("YTG13063794"));
		assertThat(fahrradzaehlstelle.getFahrradzaehlstelleBezeichnung().get())
			.isEqualTo(FahrradzaehlstelleBezeichnung.of("Fuß- & Radtunnel Südportal - Derendinger Allee"));
		assertThat(fahrradzaehlstelle.getFahrradzaehlstelleGebietskoerperschaft().get())
			.isEqualTo(FahrradzaehlstelleGebietskoerperschaft.of("Stadt Tübingen"));
		assertThat(fahrradzaehlstelle.getZaehlintervall().get())
			.isEqualTo(Zaehlintervall.of("15"));
		assertThat(fahrradzaehlstelle.getGeometrie().getCoordinate())
			.isEqualTo(createCoordinateFromWG84InUtm32(9.048007, 48.518017));

		assertThat(fahrradzaehlstelle.getNeusterZeitstempel()).isEqualTo(Zeitstempel.of("2021-01-01T02:00:00+0100"));

		assertThat(fahrradzaehlstelle.getChannels()).hasSize(1);
		Channel zaehlstelle2Channel1 = fahrradzaehlstelle.getChannels().get(0);
		assertThat(zaehlstelle2Channel1.getChannelId()).isEqualTo(ChannelId.of(101003358L));
		assertThat(zaehlstelle2Channel1.getChannelBezeichnung().get()).isEqualTo(
			ChannelBezeichnung.of("Richtung Altstadt"));
		Map<Zeitstempel, FahrradzaehlDatenEintrag> expectedFahrradzaehlDaten = new HashMap<>();
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2021-01-01T01:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(1L), Zaehlstatus.of(0)));
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2021-01-01T02:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(10L), Zaehlstatus.of(0)));
		assertThat(zaehlstelle2Channel1.getFahrradzaehlDaten()).isEqualTo(expectedFahrradzaehlDaten);
	}

	@Test
	void doRun_2Dateien_1Stelle_1Channel_DoppelteMessdatenEintraege() throws FactoryException, TransformException {
		// arrange
		File fileToImport = new File(
			"src/test/resources/fahrradzaehlstellenTestImportFiles/2Dateien_1Stelle_1Channel_DoppelteMessdatenEintraege/");
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportBaseUrl()).thenReturn(
			fileToImport.toURI().toString());
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportStartDate()).thenReturn("202205");

		// act
		FahrradzaehlstellenMobiDataImportStatistik jobStatistik = (FahrradzaehlstellenMobiDataImportStatistik) fahrradzaehlstellenMobiDataImportJob.doRun()
			.get();

		entityManager.flush();
		entityManager.clear();

		List<Fahrradzaehlstelle> alleZaehlstellen = StreamSupport.stream(
			fahrradzaehlstelleRepository.findAll().spliterator(),
			false).collect(Collectors.toList());

		assertThat(alleZaehlstellen).hasSize(1);
		Fahrradzaehlstelle fahrradzaehlstelle = alleZaehlstellen.get(0);

		assertThat(fahrradzaehlstelle.getBetreiberEigeneId()).isEqualTo(BetreiberEigeneId.of(100003358L));
		assertThat(fahrradzaehlstelle.getSeriennummer().get()).isEqualTo(Seriennummer.of("YTG13063794"));
		assertThat(fahrradzaehlstelle.getFahrradzaehlstelleBezeichnung().get())
			.isEqualTo(FahrradzaehlstelleBezeichnung.of("Fuß- & Radtunnel Südportal - Derendinger Allee"));
		assertThat(fahrradzaehlstelle.getFahrradzaehlstelleGebietskoerperschaft().get())
			.isEqualTo(FahrradzaehlstelleGebietskoerperschaft.of("Stadt Tübingen"));
		assertThat(fahrradzaehlstelle.getZaehlintervall().get())
			.isEqualTo(Zaehlintervall.of("15"));
		assertThat(fahrradzaehlstelle.getGeometrie().getCoordinate())
			.isEqualTo(createCoordinateFromWG84InUtm32(9.048007, 48.518017));

		assertThat(fahrradzaehlstelle.getNeusterZeitstempel()).isEqualTo(Zeitstempel.of("2021-01-01T04:00:00+0100"));

		assertThat(fahrradzaehlstelle.getChannels()).hasSize(1);
		Channel zaehlstelle2Channel1 = fahrradzaehlstelle.getChannels().get(0);
		assertThat(zaehlstelle2Channel1.getChannelId()).isEqualTo(ChannelId.of(101003358L));
		assertThat(zaehlstelle2Channel1.getChannelBezeichnung().get()).isEqualTo(
			ChannelBezeichnung.of("Richtung Altstadt"));

		assertThat(zaehlstelle2Channel1.getFahrradzaehlDaten().keySet()).hasSize(4);
		Map<Zeitstempel, FahrradzaehlDatenEintrag> expectedFahrradzaehlDaten = new HashMap<>();
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2021-01-01T01:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(1L), Zaehlstatus.of(0)));
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2021-01-01T02:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(0L), Zaehlstatus.of(0)));
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2021-01-01T03:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(2L), Zaehlstatus.of(0)));
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2021-01-01T04:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(2L), Zaehlstatus.of(0)));
		assertThat(zaehlstelle2Channel1.getFahrradzaehlDaten()).isEqualTo(expectedFahrradzaehlDaten);
	}

	@Test
	void doRun_1Datei_Zeitumstellung() throws FactoryException, TransformException {
		// arrange
		File fileToImport = new File(
			"src/test/resources/fahrradzaehlstellenTestImportFiles/1Datei_Zeitumstellung/");
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportBaseUrl()).thenReturn(
			fileToImport.toURI().toString());
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportStartDate()).thenReturn("202103");

		// act
		FahrradzaehlstellenMobiDataImportStatistik jobStatistik = (FahrradzaehlstellenMobiDataImportStatistik) fahrradzaehlstellenMobiDataImportJob.doRun()
			.get();

		entityManager.flush();
		entityManager.clear();

		List<Fahrradzaehlstelle> alleZaehlstellen = StreamSupport.stream(
			fahrradzaehlstelleRepository.findAll().spliterator(),
			false).collect(Collectors.toList());

		assertThat(alleZaehlstellen).hasSize(1);
		Fahrradzaehlstelle fahrradzaehlstelle = alleZaehlstellen.get(0);

		assertThat(fahrradzaehlstelle.getBetreiberEigeneId()).isEqualTo(BetreiberEigeneId.of(100004595L));
		assertThat(fahrradzaehlstelle.getSeriennummer()).isEmpty();
		assertThat(fahrradzaehlstelle.getFahrradzaehlstelleBezeichnung().get())
			.isEqualTo(FahrradzaehlstelleBezeichnung.of("Wiwilibrücke Querschnitt"));
		assertThat(fahrradzaehlstelle.getFahrradzaehlstelleGebietskoerperschaft().get())
			.isEqualTo(FahrradzaehlstelleGebietskoerperschaft.of("Stadt Freiburg"));
		assertThat(fahrradzaehlstelle.getZaehlintervall().get())
			.isEqualTo(Zaehlintervall.of("15"));
		assertThat(fahrradzaehlstelle.getGeometrie().getCoordinate())
			.isEqualTo(createCoordinateFromWG84InUtm32(7.8407526, 47.995213));

		assertThat(fahrradzaehlstelle.getNeusterZeitstempel()).isEqualTo(Zeitstempel.of("2021-03-28T04:00:00+0200"));

		assertThat(fahrradzaehlstelle.getChannels()).hasSize(1);
		Channel zaehlstelle2Channel1 = fahrradzaehlstelle.getChannels().get(0);
		assertThat(zaehlstelle2Channel1.getChannelId()).isEqualTo(ChannelId.of(100004595L));
		assertThat(zaehlstelle2Channel1.getChannelBezeichnung().get()).isEqualTo(
			ChannelBezeichnung.of("Wiwilibrücke Querschnitt"));

		assertThat(zaehlstelle2Channel1.getFahrradzaehlDaten().keySet()).hasSize(3);
		Map<Zeitstempel, FahrradzaehlDatenEintrag> expectedFahrradzaehlDaten = new HashMap<>();
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2021-03-28T01:00:00+0100"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(22L), Zaehlstatus.of(0)));
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2021-03-28T03:00:00+0200"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(21L), Zaehlstatus.of(0)));
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2021-03-28T04:00:00+0200"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(11L), Zaehlstatus.of(0)));
		assertThat(zaehlstelle2Channel1.getFahrradzaehlDaten()).isEqualTo(expectedFahrradzaehlDaten);
	}

	@Test
	void doRun_schonWasInDerDatenBank_LetzerMonatWirdNochmalImportiert() throws FactoryException, TransformException {
		// arrange
		Zeitstempel importDatum = Zeitstempel.of("2023-04-28T01:00:00+0200");
		Fahrradzaehlstelle bereitZuvorImportierteZaehlstelle = fahrradzaehlstelleRepository.save(
			Fahrradzaehlstelle.builder()
				.betreiberEigeneId(BetreiberEigeneId.of(123L))
				.geometrie(GeometryTestdataProvider.createPoint(new Coordinate(15, 15)))
				.neusterZeitstempel(importDatum)
				.channels(List.of(
					Channel.builder()
						.channelId(ChannelId.of(456L))
						.fahrradzaehlDaten(Map.of(
								importDatum,
								FahrradzaehlDatenEintrag.builder().zaehlstand(Zaehlstand.of(25L)).build()
							)
						).build()
				)).build()
		);

		File fileToImport = new File(
			"src/test/resources/fahrradzaehlstellenTestImportFiles/2Dateien_unterschiedlicheMonate/");
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportBaseUrl()).thenReturn(
			fileToImport.toURI().toString());
		when(jobConfigurationProperties.getFahrradzaehlstellenMobiDataImportStartDate()).thenReturn("202301");

		entityManager.flush();
		entityManager.clear();

		// act
		FahrradzaehlstellenMobiDataImportStatistik jobStatistik = (FahrradzaehlstellenMobiDataImportStatistik) fahrradzaehlstellenMobiDataImportJob.doRun()
			.get();

		entityManager.flush();
		entityManager.clear();

		List<Fahrradzaehlstelle> alleZaehlstellen = StreamSupport.stream(
			fahrradzaehlstelleRepository.findAll().spliterator(),
			false).collect(Collectors.toList());
		assertThat(alleZaehlstellen).hasSize(2);

		List<Fahrradzaehlstelle> alteFahrradzaehlstellen = fahrradzaehlstelleRepository.findAllByBetreiberEigeneIdIn(
			Set.of(BetreiberEigeneId.of(123L)));
		assertThat(alteFahrradzaehlstellen).hasSize(1);

		List<Fahrradzaehlstelle> neueFahrradzaehlstellen = fahrradzaehlstelleRepository.findAllByBetreiberEigeneIdIn(
			Set.of(BetreiberEigeneId.of(100003358L)));
		assertThat(neueFahrradzaehlstellen).hasSize(1);
		Fahrradzaehlstelle fahrradzaehlstelle = neueFahrradzaehlstellen.get(0);
		assertThat(fahrradzaehlstelle.getSeriennummer().get()).isEqualTo(Seriennummer.of("YTG13063794"));
		assertThat(fahrradzaehlstelle.getFahrradzaehlstelleBezeichnung().get())
			.isEqualTo(FahrradzaehlstelleBezeichnung.of("Fuß- & Radtunnel Südportal - Derendinger Allee"));
		assertThat(fahrradzaehlstelle.getFahrradzaehlstelleGebietskoerperschaft().get())
			.isEqualTo(FahrradzaehlstelleGebietskoerperschaft.of("Stadt Tübingen"));
		assertThat(fahrradzaehlstelle.getZaehlintervall().get())
			.isEqualTo(Zaehlintervall.of("15"));
		assertThat(fahrradzaehlstelle.getGeometrie().getCoordinate())
			.isEqualTo(createCoordinateFromWG84InUtm32(9.048007, 48.518017));

		assertThat(fahrradzaehlstelle.getNeusterZeitstempel()).isEqualTo(Zeitstempel.of("2023-04-01T02:00:00+0200"));

		assertThat(fahrradzaehlstelle.getChannels()).hasSize(1);
		Channel zaehlstelle2Channel1 = fahrradzaehlstelle.getChannels().get(0);
		assertThat(zaehlstelle2Channel1.getChannelId()).isEqualTo(ChannelId.of(101003358L));
		assertThat(zaehlstelle2Channel1.getChannelBezeichnung().get()).isEqualTo(
			ChannelBezeichnung.of("Richtung Altstadt"));

		assertThat(zaehlstelle2Channel1.getFahrradzaehlDaten().keySet()).hasSize(1);
		Map<Zeitstempel, FahrradzaehlDatenEintrag> expectedFahrradzaehlDaten = new HashMap<>();
		expectedFahrradzaehlDaten.put(Zeitstempel.of("2023-04-01T02:00:00+0200"),
			new FahrradzaehlDatenEintrag(Zaehlstand.of(10L), Zaehlstatus.of(0)));
		assertThat(zaehlstelle2Channel1.getFahrradzaehlDaten()).isEqualTo(expectedFahrradzaehlDaten);
	}

	private Coordinate createCoordinateFromWG84InUtm32(double lon, double lat)
		throws FactoryException, TransformException {
		return converter.transformCoordinate(new Coordinate(lat, lon),
			KoordinatenReferenzSystem.WGS84,
			KoordinatenReferenzSystem.ETRS89_UTM32_N);
	}

}