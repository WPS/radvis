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

import de.wps.radvis.backend.karte.domain.HintergrundKarteConfigurationProperties;
import de.wps.radvis.backend.karte.domain.valueObject.HintergrundKarteTyp;
import lombok.Getter;

@Getter
public class HintergrundKarteView {
	private final String id;
	private final String name;
	private final String url;
	private final Integer maxZoom;
	private final HintergrundKarteTyp typ;
	private final String quelle;
	private final Double centerX;
	private final Double centerY;
	private final boolean defaultKarte;

	public HintergrundKarteView(String id, HintergrundKarteConfigurationProperties configurationProperties,
		boolean defaultKarte) {
		require(id, notNullValue());
		require(configurationProperties, notNullValue());

		this.id = id;
		name = configurationProperties.getName();
		url = configurationProperties.getUrl();
		maxZoom = configurationProperties.getMaxZoom().orElse(null);
		typ = configurationProperties.getTyp();
		quelle = configurationProperties.getQuelle().orElse(null);
		centerX = configurationProperties.getCenter().map(c -> c.getX()).orElse(null);
		centerY = configurationProperties.getCenter().map(c -> c.getY()).orElse(null);
		this.defaultKarte = defaultKarte;
	}

}
