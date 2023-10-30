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

package de.wps.radvis.backend.karte.schnittstelle;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.wps.radvis.backend.karte.domain.HintergrundKartenRepository;
import de.wps.radvis.backend.karte.domain.entity.HintergrundKarte;

public class HintergrundKartenRepositoryImpl implements HintergrundKartenRepository {

	private Map<String, HintergrundKarte> hintergrundKarten;

	public HintergrundKartenRepositoryImpl(Map<String, String> layerNameToUrlMap) {
		require(layerNameToUrlMap, notNullValue());

		this.hintergrundKarten = layerNameToUrlMap.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey,
				e -> new HintergrundKarte(e.getValue())));
	}

	@Override
	public Optional<HintergrundKarte> find(String key) {
		require(key, notNullValue());

		return Optional.ofNullable(hintergrundKarten.get(key));
	}
}
