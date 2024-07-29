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

package de.wps.radvis.backend.common.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;

@ConfigurationProperties("spring.security")
@Validated
@Getter
public class SecurityConfigurationProperties {

	public final boolean localAuthSetup;
	public final String localAuthSuccessUrl;

	public final String serviceBwIdKey;

	private final String ACS;

	private final String externerApiUserName;
	private final String externerApiUserPassword;

	private final String prometheusWhitelistIP;

	@ConstructorBinding
	public SecurityConfigurationProperties(boolean localAuthSetup,
		String localAuthSuccessUrl,
		String ACS,
		String externerApiUserName,
		String externerApiUserPassword,
		String prometheusWhitelistIP,
		String serviceBwIdKey
	) {
		require(ACS, notNullValue());
		require(externerApiUserName, notNullValue());
		require(externerApiUserPassword, notNullValue());
		this.localAuthSetup = localAuthSetup;
		this.localAuthSuccessUrl = localAuthSuccessUrl;
		this.ACS = ACS;
		this.externerApiUserName = externerApiUserName;
		this.externerApiUserPassword = externerApiUserPassword;
		this.prometheusWhitelistIP = prometheusWhitelistIP;
		this.serviceBwIdKey = serviceBwIdKey;
	}
}
