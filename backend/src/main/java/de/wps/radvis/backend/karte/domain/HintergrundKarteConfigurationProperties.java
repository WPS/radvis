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

package de.wps.radvis.backend.karte.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Optional;

import de.wps.radvis.backend.karte.domain.valueObject.HintergrundKarteTyp;
import lombok.Getter;

@Getter
public class HintergrundKarteConfigurationProperties {
	private final String name;
	private final String url;
	private final Integer maxZoom;
	private final HintergrundKarteTyp typ;
	private final String quelle;
	private final HintergrundKarteCenterConfigurationProperties center;

	public HintergrundKarteConfigurationProperties(String name, String url, Integer maxZoom, HintergrundKarteTyp typ,
		String quelle, HintergrundKarteCenterConfigurationProperties center) {
		require(name, notNullValue());
		require(url, notNullValue());
		require(typ, notNullValue());

		this.name = name;
		this.url = url;
		this.maxZoom = maxZoom;
		this.typ = typ;
		this.quelle = quelle;
		this.center = center;
	}

	public Optional<String> getQuelle() {
		return Optional.ofNullable(quelle);
	}

	public Optional<Integer> getMaxZoom() {
		return Optional.ofNullable(maxZoom);
	}

	public Optional<HintergrundKarteCenterConfigurationProperties> getCenter() {
		return Optional.ofNullable(center);
	}
}
