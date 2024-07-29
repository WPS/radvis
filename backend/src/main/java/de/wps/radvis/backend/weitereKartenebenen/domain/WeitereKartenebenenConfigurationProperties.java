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

package de.wps.radvis.backend.weitereKartenebenen.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import lombok.Getter;

@ConfigurationProperties("radvis.weiterekartenebenen")
@Getter
public class WeitereKartenebenenConfigurationProperties {

	private final String geoserverDateiLayerHost;
	private final String geoserverDateiLayerUsername;
	private final String geoserverDateiLayerPassword;
	private final String geoserverDateiLayerWorkspace;
	private final DataSize maxSldFileSize;
	private final List<VordefinierteLayerConfigurationProperties> vordefinierteLayer;

	public WeitereKartenebenenConfigurationProperties(String geoserverDateiLayerHost,
		String geoserverDateiLayerUsername, String geoserverDateiLayerPassword, String geoserverDateiLayerWorkspace,
		DataSize maxSldFileSize, List<VordefinierteLayerConfigurationProperties> vordefinierteLayer) {
		require(geoserverDateiLayerHost, notNullValue());
		require(geoserverDateiLayerUsername, notNullValue());
		require(geoserverDateiLayerPassword, notNullValue());
		require(geoserverDateiLayerWorkspace, notNullValue());
		require(maxSldFileSize, notNullValue());
		require(vordefinierteLayer, notNullValue());

		this.geoserverDateiLayerHost = geoserverDateiLayerHost;
		this.geoserverDateiLayerUsername = geoserverDateiLayerUsername;
		this.geoserverDateiLayerPassword = geoserverDateiLayerPassword;
		this.geoserverDateiLayerWorkspace = geoserverDateiLayerWorkspace;
		this.maxSldFileSize = maxSldFileSize;
		this.vordefinierteLayer = vordefinierteLayer;
	}
}
