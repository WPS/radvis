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

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import lombok.Getter;

@ConfigurationProperties("radvis.karte")
@Getter
public class KarteConfigurationProperties {
	private final Map<String, String> hintergrundKartenProxy;
	private final Map<String, HintergrundKarteConfigurationProperties> hintergrundKarten;
	private final String defaultHintergrundKarte;

	@ConstructorBinding
	public KarteConfigurationProperties(Map<String, String> hintergrundKartenProxy,
		Map<String, HintergrundKarteConfigurationProperties> hintergrundKarten, String defaultHintergrundKarte) {
		require(hintergrundKartenProxy, notNullValue());
		require(hintergrundKarten, notNullValue());
		require(defaultHintergrundKarte, notNullValue());
		require(hintergrundKarten.size() > 0, "Es muss mindestens eine Hintergrundkarte konfiguriert sein.");
		require(hintergrundKarten.keySet().contains(defaultHintergrundKarte),
			"Keine Karte zu default Hintergrundkarte gefunden");

		this.defaultHintergrundKarte = defaultHintergrundKarte;
		this.hintergrundKarten = hintergrundKarten;
		this.hintergrundKartenProxy = hintergrundKartenProxy;
	}
}
