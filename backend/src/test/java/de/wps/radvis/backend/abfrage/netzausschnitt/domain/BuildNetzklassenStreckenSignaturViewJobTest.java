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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.NetzklassenStreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.event.KanteGeometrieChangedEvent;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitChangedEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzklassenStreckenViewService;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;

class BuildNetzklassenStreckenSignaturViewJobTest {

	@Nested
	class WithBuildNetzklassenStreckenSignatureViewJob {

		@Mock
		StreckeViewCacheRepository<List<NetzklassenStreckenSignaturView>, NetzklassenStreckeVonKanten> streckeViewCacheRepository;
		@Mock
		KantenRepository kantenRepository;
		@Mock
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
		@Mock
		NetzklassenStreckenViewService netzklassenStreckenViewService;
		@Mock
		EntityManager entityManager;
		@Mock
		DLMConfigurationProperties dlmConfigurationProperties;

		BuildNetzklassenStreckenSignaturViewJob buildNetzklassenStreckenSignaturViewJob;

		@BeforeEach
		void beforeEach() {
			MockitoAnnotations.openMocks(this);

			buildNetzklassenStreckenSignaturViewJob = new BuildNetzklassenStreckenSignaturViewJob(
				jobExecutionDescriptionRepository,
				kantenRepository, netzklassenStreckenViewService, streckeViewCacheRepository, entityManager,
				dlmConfigurationProperties);
		}

		@Test
		void reloadNetzMapView() {
			// Arrange
			final var kanteId = 42L;
			final var streckeVonKanten = mock(NetzklassenStreckeVonKanten.class);
			when(streckeVonKanten.getKanten()).thenReturn(
				List.of(KanteTestDataProvider.withDefaultValues().id(kanteId).build()));
			when(streckeViewCacheRepository.getStreckenVonKanten()).thenReturn(Set.of(streckeVonKanten));

			final var neueKante = mock(Kante.class);
			when(kantenRepository.findById(kanteId)).thenReturn(Optional.of(neueKante));

			// Act
			buildNetzklassenStreckenSignaturViewJob.onKanteGeometrieChanged(new KanteGeometrieChangedEvent(kanteId));

			// Assert
			verify(streckeVonKanten).updateKanteInStrecke(neueKante);
			verify(streckeViewCacheRepository).reloadCache();
		}

		@Test
		void netzklassenZugehoerigkeitAdded() {
			// Arrange
			final var kante = mock(Kante.class);
			final var kantenAttributGruppeId = 123L;
			when(kantenRepository.findByKantenAttributGruppeId(kantenAttributGruppeId)).thenReturn(kante);

			// Act
			buildNetzklassenStreckenSignaturViewJob.onNetzklassenZugehoerigkeitChanged(
				new RadNetzZugehoerigkeitChangedEvent(
					kantenAttributGruppeId, true));

			// Assert
			verify(streckeViewCacheRepository).addStrecke(any(NetzklassenStreckeVonKanten.class));
			verify(streckeViewCacheRepository).reloadCache();
		}

		@Test
		void netzklassenZugehoerigkeitRemoved_SplitCenter() {
			// Arrange
			List<Coordinate[]> streckenCoordinates = List.of(new Coordinate[] {
				new Coordinate(100, 100),
				new Coordinate(130, 135),
				new Coordinate(150, 150),
				new Coordinate(200, 150),
			}, new Coordinate[] {
				new Coordinate(200, 150),
				new Coordinate(250, 200),
				new Coordinate(300, 200),
			}, new Coordinate[] {
				new Coordinate(300, 200),
				new Coordinate(350, 250),
				new Coordinate(400, 250),
			});

			List<Kante> kanten = KanteTestDataProvider.createStreckeUeberCoordinates(
				streckenCoordinates, null, null, new AtomicLong(0), new AtomicLong(0));

			final var kantenAttributGruppeId = 123L;
			when(kantenRepository.findByKantenAttributGruppeId(kantenAttributGruppeId)).thenReturn(kanten.get(1));

			final var streckeVonKanten = new NetzklassenStreckeVonKanten(kanten.get(0), true, false);
			streckeVonKanten.addKante(kanten.get(1), false);
			streckeVonKanten.addKante(kanten.get(2), true);
			when(streckeViewCacheRepository.getStreckenVonKanten()).thenReturn(List.of(streckeVonKanten));

			// Act
			buildNetzklassenStreckenSignaturViewJob.onNetzklassenZugehoerigkeitChanged(
				new RadNetzZugehoerigkeitChangedEvent(
					kantenAttributGruppeId, false));

			// Assert
			verify(streckeViewCacheRepository).addStrecke(any(NetzklassenStreckeVonKanten.class));
			verify(streckeViewCacheRepository).reloadCache();
		}

		@Test
		void netzklassenZugehoerigkeitRemoved_KeineKanteUebrig() {
			// Arrange
			GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
			final var kante1 = KanteTestDataProvider.withDefaultValues()
				.id(1L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(100, 100),
					new Coordinate(130, 135),
					new Coordinate(150, 150),
					new Coordinate(200, 150),
				}))
				.vonKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).id(1L)
						.build())
				.nachKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 150), QuellSystem.DLM).id(2L)
						.build())
				.build();

			final var kantenAttributGruppeId = 123L;
			when(kantenRepository.findByKantenAttributGruppeId(kantenAttributGruppeId)).thenReturn(kante1);

			final var streckeVonKanten = new NetzklassenStreckeVonKanten(kante1, false, false);
			when(streckeViewCacheRepository.getStreckenVonKanten()).thenReturn(List.of(streckeVonKanten));

			// Act
			buildNetzklassenStreckenSignaturViewJob.onNetzklassenZugehoerigkeitChanged(
				new RadNetzZugehoerigkeitChangedEvent(
					kantenAttributGruppeId, false));

			// Assert
			verify(streckeViewCacheRepository).removeStrecke(streckeVonKanten);
			verify(streckeViewCacheRepository).reloadCache();
		}

	}
}