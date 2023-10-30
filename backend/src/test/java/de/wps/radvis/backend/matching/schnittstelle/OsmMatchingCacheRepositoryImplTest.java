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

package de.wps.radvis.backend.matching.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.matching.domain.OsmMatchingCacheRepository;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.OsmMatchingCacheRepositoryImpl;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;

class OsmMatchingCacheRepositoryImplTest {
	private OsmMatchingCacheRepository osmMatchingCacheRepository;

	@BeforeEach
	public void setUp() {
		osmMatchingCacheRepository = new OsmMatchingCacheRepositoryImpl("target/test-mapping-cache");
	}

	@Test
	void save() {
		// arrange
		Map<Integer, LinearReferenzierteOsmWayId> mappingWayIds = new HashMap<>();
		mappingWayIds.put(1231, LinearReferenzierteOsmWayId.of(123, LinearReferenzierterAbschnitt.of(0, 1)));
		mappingWayIds.put(4561, LinearReferenzierteOsmWayId.of(456, LinearReferenzierterAbschnitt.of(0, 1)));

		// act
		osmMatchingCacheRepository.save(mappingWayIds);

		assertThat(osmMatchingCacheRepository.get()).isEqualTo(mappingWayIds);
	}
}
