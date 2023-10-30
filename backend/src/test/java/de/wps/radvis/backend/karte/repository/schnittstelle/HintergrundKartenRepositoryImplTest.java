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

package de.wps.radvis.backend.karte.repository.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.karte.domain.HintergrundKartenRepository;
import de.wps.radvis.backend.karte.domain.entity.HintergrundKarte;
import de.wps.radvis.backend.karte.schnittstelle.HintergrundKartenRepositoryImpl;

public class HintergrundKartenRepositoryImplTest {

	@Test
	public void testFind() {
		//arrange
		Map<String, String> map = new HashMap<>();
		map.put("key1", "https://url1.de/seg/");
		map.put("key2", "https://url2.de?param=value");

		HintergrundKartenRepository hintergrundKartenRepository = new HintergrundKartenRepositoryImpl(map);

		// act
		Optional<HintergrundKarte> hintergrundKarte1 = hintergrundKartenRepository.find("key1");
		Optional<HintergrundKarte> hintergrundKarte2 = hintergrundKartenRepository.find("key2");

		//assert
		assertThat(hintergrundKarte1).isPresent();
		assertThat(hintergrundKarte2).isPresent();

		assertThat(hintergrundKarte1.get().getDomain()).isEqualTo("https://url1.de");
		assertThat(hintergrundKarte1.get().getPath()).isEqualTo("/seg/");
		assertThat(hintergrundKarte1.get().getQuery()).isNull();

		assertThat(hintergrundKarte2.get().getDomain()).isEqualTo("https://url2.de");
		assertThat(hintergrundKarte2.get().getPath()).isEqualTo("");
		assertThat(hintergrundKarte2.get().getQuery()).isEqualTo("param=value");
	}
}
