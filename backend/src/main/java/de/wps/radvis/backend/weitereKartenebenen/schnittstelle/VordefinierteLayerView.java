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

package de.wps.radvis.backend.weitereKartenebenen.schnittstelle;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;

import de.wps.radvis.backend.weitereKartenebenen.domain.VordefinierteLayerConfigurationProperties;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.VordefinierteLayerQuelle;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.WeitereKartenebeneTyp;
import lombok.Getter;

@Getter
public class VordefinierteLayerView {
	private final String name;
	private final VordefinierteLayerQuelle quelle;
	private final String url;
	private final WeitereKartenebeneTyp typ;
	private final String farbe;
	private final Double deckkraft;
	private final Double zoomstufe;
	private final String quellangabe;
	private final List<String> path;

	public VordefinierteLayerView(VordefinierteLayerConfigurationProperties configurationProperties) {
		require(configurationProperties, notNullValue());

		name = configurationProperties.getName();
		quelle = configurationProperties.getQuelle();
		url = configurationProperties.getUrl();
		typ = configurationProperties.getTyp();
		farbe = configurationProperties.getFarbe();
		deckkraft = configurationProperties.getDeckkraft();
		zoomstufe = configurationProperties.getZoomstufe();
		quellangabe = configurationProperties.getQuellangabe();
		path = configurationProperties.getPath();
	}
}
