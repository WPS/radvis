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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;

class RadNETZNetzViewCacheRepositoryTest {

	private StreckeViewCacheRepository<NetzMapView, StreckeVonKanten> netzMapViewCacheRepository;

	@BeforeEach
	void beforeEach() {
		MockitoAnnotations.openMocks(this);

		netzMapViewCacheRepository = new RadNETZNetzViewCacheRepository();
	}

	@Test
	void testGetCachedNetzMapView_doesNotExist() {
		// Act + Assert
		assertThatThrownBy(() -> netzMapViewCacheRepository.getCache()).isInstanceOf(
			RuntimeException.class);
	}

	@Nested
	class WithStreckenVonKanten {
		StreckeVonKanten streckeVonKanten1;
		StreckeVonKanten streckeVonKanten2;
		Set<StreckeVonKanten> streckeVonKantenSet;

		@BeforeEach
		void beforeEach() {
			streckeVonKanten1 = new StreckeVonKanten(
				KanteTestDataProvider.withDefaultValues().id(1L).build());
			streckeVonKanten2 = new StreckeVonKanten(
				KanteTestDataProvider.withDefaultValues().id(2L).build());

			streckeVonKantenSet = new HashSet<>();
			streckeVonKantenSet.add(streckeVonKanten1);
			streckeVonKantenSet.add(streckeVonKanten2);
		}

		@Test
		void testGetCachedNetzMapView_exists() {
			// Arrange
			netzMapViewCacheRepository.loadCache(streckeVonKantenSet);

			// Act
			final var result = netzMapViewCacheRepository.getCache();

			// Assert
			assertThat(result).isNotNull();
		}

		@Test
		void testLoadCache() {
			// Act
			netzMapViewCacheRepository.loadCache(streckeVonKantenSet);

			// Assert
			assertThat(netzMapViewCacheRepository.getStreckenVonKanten()).isEqualTo(streckeVonKantenSet);
		}

		@Test
		void testReloadNetzMapView() {
			// Arrange
			netzMapViewCacheRepository.loadCache(streckeVonKantenSet);

			// Act
			netzMapViewCacheRepository.reloadCache();

			// Assert
			assertThat(netzMapViewCacheRepository.getCache().getKanten().stream()
				.map(AbstractEntity::getId).collect(Collectors.toList())).contains(1L, 2L);
			assertThat(netzMapViewCacheRepository.getCache().getKnoten()).isEmpty();
		}

		@Test
		void testAddStrecke() {
			// Arrange
			final var streckeVonKanten = new StreckeVonKanten(
				KanteTestDataProvider.withDefaultValues().id(3L).build());
			netzMapViewCacheRepository.loadCache(streckeVonKantenSet);

			// Act
			netzMapViewCacheRepository.addStrecke(streckeVonKanten);

			// Assert
			assertThat(netzMapViewCacheRepository.getStreckenVonKanten()).contains(streckeVonKanten);
		}

		@Test
		void testRemove() {
			// Arrange
			netzMapViewCacheRepository.loadCache(streckeVonKantenSet);

			// Act
			netzMapViewCacheRepository.removeStrecke(streckeVonKanten1);

			// Assert
			assertThat(netzMapViewCacheRepository.getStreckenVonKanten()).containsExactly(streckeVonKanten2);
		}

	}

}