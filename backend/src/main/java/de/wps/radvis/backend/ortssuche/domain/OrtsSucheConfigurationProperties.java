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

package de.wps.radvis.backend.ortssuche.domain;

import static de.wps.radvis.backend.common.domain.Validators.isValidURL;
import static org.valid4j.Assertive.require;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import lombok.Getter;

@Getter
@ConfigurationProperties("radvis.ortssuche")
public class OrtsSucheConfigurationProperties {
	public static final String B_BOX_REGEX = "([\\d\\.]+),([\\d\\.]+),([\\d\\.]+),([\\d\\.]+)";

	private final String baseUrl;
	private final String token;
	private final String file;

	private final int anzahlSuchergebnisse;

	private final String bBox;

	@ConstructorBinding
	public OrtsSucheConfigurationProperties(String baseUrl, String token, String file,
		int anzahlSuchergebnisse,
		String bBox) {
		require(isValidURL(baseUrl), "url muss URL-Struktur haben");
		require(anzahlSuchergebnisse >= 1, "anzahlSuchergebnisse muss mindestens 1 sein");
		require(isValidBBox(bBox), "bBox muss B-Box-Struktur haben");
		this.baseUrl = baseUrl;
		this.token = token;
		this.file = file;
		this.anzahlSuchergebnisse = anzahlSuchergebnisse;
		this.bBox = bBox;
	}

	public static boolean isValidBBox(String value) {
		return value != null && value.matches(B_BOX_REGEX);
	}

}
