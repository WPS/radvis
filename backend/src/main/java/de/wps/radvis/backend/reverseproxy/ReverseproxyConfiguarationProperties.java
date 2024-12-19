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

package de.wps.radvis.backend.reverseproxy;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;

@ConfigurationProperties("spring.reverseproxy")
@Validated
@Getter
public class ReverseproxyConfiguarationProperties {

	private final String geoserverUrl;
	private final String geoserverApiUserName;
	private final String geoserverApiUserPassword;
	private final String geoserverDateiLayerUrl;

	private final String grafanaUrl;
	private final String matomoUrl;

	private final String beschilderungsKatasterDomain;
	private final String beschilderungsKatasterPath;

	@ConstructorBinding
	public ReverseproxyConfiguarationProperties(
		String geoserverUrl,
		String geoserverApiUserName,
		String geoserverApiUserPassword,
		String geoserverDateiLayerUrl,
		String grafanaUrl,
		String matomoUrl,
		String beschilderungsKatasterDomain,
		String beschilderungsKatasterPath) {
		require(geoserverUrl, notNullValue());
		require(beschilderungsKatasterDomain, notNullValue());

		this.beschilderungsKatasterDomain = beschilderungsKatasterDomain;
		this.beschilderungsKatasterPath = beschilderungsKatasterPath;
		this.geoserverUrl = geoserverUrl;
		this.matomoUrl = matomoUrl;
		this.geoserverApiUserName = geoserverApiUserName;
		this.geoserverApiUserPassword = geoserverApiUserPassword;
		this.geoserverDateiLayerUrl = geoserverDateiLayerUrl;
		this.grafanaUrl = grafanaUrl;
	}

}
