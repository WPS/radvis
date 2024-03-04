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

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import lombok.Getter;

@ConfigurationProperties("radvis.mail")
@Getter
public class MailConfigurationProperties {

	private final String host;
	private final int port;
	private final String sender;
	private final String radvisSupportMail;
	private final String protocol;
	private final boolean authenticationEnabled;
	private final boolean startTlsEnabled;
	private final List<String> wpsTestmails;
	private final String wpsTestmailsRedirectTarget;

	@ConstructorBinding
	public MailConfigurationProperties(String host, int port, String sender, String radvisSupportMail, String protocol,
		boolean authenticationEnabled, boolean startTlsEnabled, List<String> wpsTestmails,
		String wpsTestmailsRedirectTarget) {
		require(host, notNullValue());
		require(sender, notNullValue());
		require(radvisSupportMail, notNullValue());

		this.host = host;
		this.port = port;
		this.sender = sender;
		this.radvisSupportMail = radvisSupportMail;
		this.protocol = protocol;
		this.authenticationEnabled = authenticationEnabled;
		this.startTlsEnabled = startTlsEnabled;
		this.wpsTestmails = wpsTestmails;
		this.wpsTestmailsRedirectTarget = wpsTestmailsRedirectTarget;
	}
}
