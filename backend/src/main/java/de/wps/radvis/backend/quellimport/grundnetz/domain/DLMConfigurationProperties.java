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

package de.wps.radvis.backend.quellimport.grundnetz.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import de.wps.radvis.backend.common.domain.ExtentProperty;
import lombok.Getter;

@ConfigurationProperties("radvis.dlm")
public class DLMConfigurationProperties {

	@Getter
	private final String basisUrl;

	@Getter
	private final String username;

	@Getter
	private final String password;

	@Getter
	private final ExtentProperty extentProperty;

	@Getter
	private final int partitionenX;

	@Getter
	private final int pbfpartitionen;

	@ConstructorBinding
	public DLMConfigurationProperties(String basisUrl, String username, String password, ExtentProperty extent, int partitionenX,
		int partitionenY, int pbfpartitionen) {
		require(basisUrl, notNullValue());
		require(username, notNullValue());
		require(password, notNullValue());
		require(extent, notNullValue());
		require(partitionenX >= 1, "Partition muss größer 0 sein");
		require(pbfpartitionen >= 1, "Partition muss größer 0 sein");

		this.basisUrl = basisUrl;
		this.username = username;
		this.password = password;
		this.extentProperty = extent;
		this.partitionenX = partitionenX;
		this.pbfpartitionen = pbfpartitionen;
	}
}
