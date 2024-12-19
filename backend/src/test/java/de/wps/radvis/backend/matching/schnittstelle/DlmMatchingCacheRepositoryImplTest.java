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

import de.wps.radvis.backend.matching.domain.repository.DlmMatchingCacheRepository;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchingCacheRepositoryImpl;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.SeitenbezogeneProfilEigenschaften;

class DlmMatchingCacheRepositoryImplTest {
	private DlmMatchingCacheRepository dlmMatchingCacheRepository;

	@BeforeEach
	public void setUp() {
		dlmMatchingCacheRepository = new DlmMatchingCacheRepositoryImpl("target/test-mapping-cache");
	}

	@Test
	void save() {
		// arrange
		Map<Integer, OsmWayId> mappingWayIds = new HashMap<>();
		mappingWayIds.put(1231, OsmWayId.of(123));
		mappingWayIds.put(4561, OsmWayId.of(456));

		Map<Integer, SeitenbezogeneProfilEigenschaften> mappingProfilEigenschaften = new HashMap<>();
		mappingProfilEigenschaften.put(1231,
			SeitenbezogeneProfilEigenschaften.of(BelagArt.BETON, BelagArt.ASPHALT, Radverkehrsfuehrung.BEGEGNUNBSZONE,
				Radverkehrsfuehrung.SCHUTZSTREIFEN));
		mappingProfilEigenschaften.put(4561,
			SeitenbezogeneProfilEigenschaften.of(BelagArt.NATURSTEINPFLASTER, BelagArt.SONSTIGER_BELAG,
				Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE, Radverkehrsfuehrung.BETRIEBSWEG_FORST));

		// act
		dlmMatchingCacheRepository.save(mappingWayIds, mappingProfilEigenschaften);

		assertThat(dlmMatchingCacheRepository.getWayIds()).isEqualTo(mappingWayIds);
		assertThat(dlmMatchingCacheRepository.getProfilEigenschaften()).isEqualTo(mappingProfilEigenschaften);
	}
}
