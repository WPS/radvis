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

package de.wps.radvis.backend.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.data.util.Lazy;
import org.springframework.test.context.ContextConfiguration;

import com.graphhopper.matching.MatchResult;

import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.matching.domain.entity.MatchingJobStatistik;
import de.wps.radvis.backend.matching.domain.entity.OsmAbbildungsFehler;
import de.wps.radvis.backend.matching.domain.exception.GeometryLaengeMismatchException;
import de.wps.radvis.backend.matching.domain.exception.GeometryZuWeitEntferntException;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.service.MatchingJobProtokollService;
import de.wps.radvis.backend.matching.domain.service.MatchingKorrekturService;
import de.wps.radvis.backend.matching.domain.valueObject.LinearReferenziertesOsmMatchResult;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometryView;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group3")
@ContextConfiguration(classes = {
	CommonConfiguration.class,
	MatchNetzAufOSMJobTestIT.TestConfiguration.class,
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	JobConfigurationProperties.class,
	CommonConfigurationProperties.class,
	PostgisConfigurationProperties.class,
})
class MatchNetzAufOSMJobTestIT extends DBIntegrationTestIT {
	@EnableEnversRepositories(basePackages = { "de.wps.radvis.backend.matching", "de.wps.radvis.backend.netz" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.matching", "de.wps.radvis.backend.netz" })
	public static class TestConfiguration {
		@Bean
		CoordinateReferenceSystemConverter coordinateReferenceSystemConverter() {
			return new CoordinateReferenceSystemConverter(
				new Envelope(
					new Coordinate(378073.54, 5255657.09),
					new Coordinate(633191.12, 5534702.95)));
		}
	}

	@Mock
	private NetzService netzService;

	@Mock
	private NetzfehlerRepository netzfehlerRepository;

	@Mock
	private MatchingKorrekturService korrekturService;

	@Mock
	private OsmMatchingRepository osmMatchingRepository;

	@Mock
	private MatchingJobProtokollService osmJobProtokollService;

	@Mock
	private DLMConfigurationProperties dlmConfigurationProperties;

	@PersistenceContext
	EntityManager entityManager;

	@Autowired
	private OsmAbbildungsFehlerRepository osmAbbildungsFehlerRepository;

	@Autowired
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	private MatchNetzAufOSMJob matchNetzAufOSMJob;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		matchNetzAufOSMJob = new MatchNetzAufOSMJob(jobExecutionDescriptionRepository, netzfehlerRepository,
			Lazy.of(osmMatchingRepository),
			netzService, korrekturService, osmJobProtokollService, entityManager, dlmConfigurationProperties,
			osmAbbildungsFehlerRepository);
		when(dlmConfigurationProperties.getExtentProperty()).thenReturn(new ExtentProperty(0, 30, 0, 30));
		when(dlmConfigurationProperties.getPartitionenX()).thenReturn(1);
	}

	@Test
	void doRun_erzeugtAbbildungsfehler_keinMatch_keineNetzklassen() throws KeinMatchGefundenException {
		// arrange
		KanteGeometryView kante = new KanteGeometryView(1, GeometryTestdataProvider.createLineString());
		when(netzService.getFuerOsmAbbildungRelevanteKanten(any())).thenReturn(List.of(kante));
		when(netzService.getNetzklassenVonKante(kante.getId())).thenReturn(Optional.empty());
		when(osmMatchingRepository.matchGeometry(kante.getGeometry()))
			.thenThrow(KeinMatchGefundenException.class);

		// act
		matchNetzAufOSMJob.doRun();

		// assert
		OsmAbbildungsFehler abbildungsFehler = osmAbbildungsFehlerRepository.findAll().iterator().next();
		assertThat(abbildungsFehler.getKanteId()).isEqualTo(kante.getId());
		assertThat(abbildungsFehler.getOriginalGeometry().getCoordinates())
			.containsExactly(kante.getGeometry().getCoordinates());
		assertThat(abbildungsFehler.isRadnetz()).isFalse();
		assertThat(abbildungsFehler.isKreisnetz()).isFalse();
		assertThat(abbildungsFehler.isKommunalnetz()).isFalse();
	}

	@Test
	void doRun_erzeugtAbbildungsfehler_richtigeNetzklassen_AlleNetzklassen() throws KeinMatchGefundenException {
		// arrange
		KanteGeometryView kante = new KanteGeometryView(1, GeometryTestdataProvider.createLineString());
		when(netzService.getFuerOsmAbbildungRelevanteKanten(any())).thenReturn(List.of(kante));
		when(netzService.getNetzklassenVonKante(kante.getId())).thenReturn(Optional.of(
			"RADNETZ_ALLTAG;KOMMUNALNETZ_ALLTAG;KREISNETZ_FREIZEIT"
		));
		when(osmMatchingRepository.matchGeometry(kante.getGeometry()))
			.thenThrow(KeinMatchGefundenException.class);

		// act
		matchNetzAufOSMJob.doRun();

		// assert
		OsmAbbildungsFehler abbildungsFehler = osmAbbildungsFehlerRepository.findAll().iterator().next();
		assertThat(abbildungsFehler.getKanteId()).isEqualTo(kante.getId());
		assertThat(abbildungsFehler.getOriginalGeometry().getCoordinates())
			.containsExactly(kante.getGeometry().getCoordinates());
		assertThat(abbildungsFehler.isRadnetz()).isTrue();
		assertThat(abbildungsFehler.isKreisnetz()).isTrue();
		assertThat(abbildungsFehler.isKommunalnetz()).isTrue();
	}

	@Test
	void doRun_erzeugtAbbildungsfehler_richtigeNetzklassen_RadNETZ() throws KeinMatchGefundenException {
		// arrange
		KanteGeometryView kante = new KanteGeometryView(1, GeometryTestdataProvider.createLineString());
		when(netzService.getFuerOsmAbbildungRelevanteKanten(any())).thenReturn(List.of(kante));
		when(netzService.getNetzklassenVonKante(kante.getId())).thenReturn(Optional.of("RADNETZ_ZIELNETZ"));
		when(osmMatchingRepository.matchGeometry(kante.getGeometry()))
			.thenThrow(KeinMatchGefundenException.class);

		// act
		matchNetzAufOSMJob.doRun();

		// assert
		OsmAbbildungsFehler abbildungsFehler = osmAbbildungsFehlerRepository.findAll().iterator().next();
		assertThat(abbildungsFehler.getKanteId()).isEqualTo(kante.getId());
		assertThat(abbildungsFehler.getOriginalGeometry().getCoordinates())
			.containsExactly(kante.getGeometry().getCoordinates());
		assertThat(abbildungsFehler.isRadnetz()).isTrue();
		assertThat(abbildungsFehler.isKreisnetz()).isFalse();
		assertThat(abbildungsFehler.isKommunalnetz()).isFalse();
	}

	@Test
	void doRun_erzeugtAbbildungsfehler_zuSchlechtesMatch()
		throws KeinMatchGefundenException, GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		// arrange
		KanteGeometryView kante = new KanteGeometryView(1, GeometryTestdataProvider.createLineString());

		when(netzService.getFuerOsmAbbildungRelevanteKanten(any())).thenReturn(List.of(kante));
		when(korrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(any(), any(), any(), any()))
			.thenThrow(GeometryLaengeMismatchException.class);

		// Erster Matching-Versuch
		MatchResult matchResult1Mock = mock(MatchResult.class);
		when(osmMatchingRepository.matchGeometry(kante.getGeometry())).thenReturn(matchResult1Mock);
		when(osmMatchingRepository.extrahiereLineString(eq(matchResult1Mock))).thenReturn(kante.getGeometry());

		// Matching-Versuch mit umgekehrter Geometrie
		when(osmMatchingRepository.matchGeometry(kante.getGeometry().reverse()))
			.thenThrow(KeinMatchGefundenException.class);

		// act
		MatchingJobStatistik jobStatistik = (MatchingJobStatistik) matchNetzAufOSMJob.doRun().get();

		// assert
		OsmAbbildungsFehler abbildungsFehler = osmAbbildungsFehlerRepository.findAll().iterator().next();
		assertThat(abbildungsFehler.getKanteId()).isEqualTo(kante.getId());
		assertThat(abbildungsFehler.getOriginalGeometry().getCoordinates())
			.containsExactly(kante.getGeometry().getCoordinates());
		assertThat(jobStatistik.anzahlKantenOhneGraphhopperMatch).isEqualTo(0);
		assertThat(jobStatistik.anzahlKantenMitZuSchlechtemGraphhopperMatch).isEqualTo(1);
	}

	@Test
	void doRun_erzeugtAbbildungsfehler_matchKlappt_matcheMitLR_nicht()
		throws KeinMatchGefundenException, GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		// arrange
		KanteGeometryView kante = new KanteGeometryView(1, GeometryTestdataProvider.createLineString());

		when(netzService.getFuerOsmAbbildungRelevanteKanten(any())).thenReturn(List.of(kante));

		// Erster Matching-Versuch klappt direkt und ist auch richtig
		MatchResult matchResult1Mock = mock(MatchResult.class);
		when(osmMatchingRepository.matchGeometry(kante.getGeometry())).thenReturn(matchResult1Mock);
		when(osmMatchingRepository.extrahiereLineString(eq(matchResult1Mock))).thenReturn(kante.getGeometry());
		when(korrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(any(), any(), any(), any()))
			.thenReturn(kante.getGeometry());

		// matching mit Linearen Referenzen schlaegt fehl
		when(osmMatchingRepository.extrahiereLineareReferenzierung(matchResult1Mock))
			.thenThrow(KeinMatchGefundenException.class);

		// act
		MatchingJobStatistik jobStatistik = (MatchingJobStatistik) matchNetzAufOSMJob.doRun().get();

		// assert
		verify(osmMatchingRepository, times(1)).extrahiereLineareReferenzierung(matchResult1Mock);

		OsmAbbildungsFehler abbildungsFehler = osmAbbildungsFehlerRepository.findAll().iterator().next();
		assertThat(abbildungsFehler.getKanteId()).isEqualTo(kante.getId());
		assertThat(abbildungsFehler.getOriginalGeometry().getCoordinates())
			.containsExactly(kante.getGeometry().getCoordinates());
		assertThat(jobStatistik.anzahlKantenOhneGraphhopperMatch).isEqualTo(1);
		assertThat(jobStatistik.anzahlKantenMitZuSchlechtemGraphhopperMatch).isEqualTo(0);
	}

	@Test
	void doRun_keineAbbildungsfehler_vorherigeWerdenGeloescht_keineNeuAngelegt()
		throws KeinMatchGefundenException, GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		// arrange
		osmAbbildungsFehlerRepository.save(new OsmAbbildungsFehler(
			19L,
			GeometryTestdataProvider.createLineString(new Coordinate(14, 14), new Coordinate(19, 19)),
			LocalDateTime.now(), false, false, false
		));

		KanteGeometryView kante = new KanteGeometryView(1, GeometryTestdataProvider.createLineString());
		when(netzService.getFuerOsmAbbildungRelevanteKanten(any())).thenReturn(List.of(kante));

		// Erster Matching-Versuch klappt direkt und ist auch richtig
		MatchResult matchResult1Mock = mock(MatchResult.class);
		when(osmMatchingRepository.matchGeometry(kante.getGeometry())).thenReturn(matchResult1Mock);
		when(osmMatchingRepository.extrahiereLineString(eq(matchResult1Mock))).thenReturn(kante.getGeometry());
		when(korrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(any(), any(), any(), any()))
			.thenReturn(kante.getGeometry());

		// matching mit Linearen Referenzen
		LinearReferenzierteOsmWayId lrOsmWayId = LinearReferenzierteOsmWayId.of(123,
			LinearReferenzierterAbschnitt.of(0.0, 0.5));
		when(osmMatchingRepository.extrahiereLineareReferenzierung(matchResult1Mock))
			.thenReturn(new LinearReferenziertesOsmMatchResult(kante.getGeometry(),
				List.of(lrOsmWayId)));

		// act
		MatchingJobStatistik jobStatistik = (MatchingJobStatistik) matchNetzAufOSMJob.doRun().get();

		// assert
		verify(netzService, times(1)).insertOsmWayIds(any());

		assertThat(osmAbbildungsFehlerRepository.findAll().iterator().hasNext()).isFalse();
		assertThat(jobStatistik.anzahlKantenOhneGraphhopperMatch).isEqualTo(0);
		assertThat(jobStatistik.anzahlKantenMitZuSchlechtemGraphhopperMatch).isEqualTo(0);
	}
}
