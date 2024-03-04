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

package de.wps.radvis.backend.basicAuthentication.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.security.SecureRandom;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BasicAuthPasswortService {
	private final BasicAuthenticationConfigurationProperties basicAuthenticationConfigurationProperties;

	private final BCryptPasswordEncoder encoder;
	private final SecureRandom secureRandom;

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
		"!#$%&()*+,-./:;<=>?@[]";

	public BasicAuthPasswortService(BCryptPasswordEncoder encoder, SecureRandom secureRandom,
		BasicAuthenticationConfigurationProperties basicAuthenticationConfigurationProperties) {
		require(encoder, notNullValue());
		require(secureRandom, notNullValue());
		require(basicAuthenticationConfigurationProperties, notNullValue());

		this.encoder = encoder;
		this.secureRandom = secureRandom;
		this.basicAuthenticationConfigurationProperties = basicAuthenticationConfigurationProperties;
	}

	public String generateRandomPassword() {
		Integer passwordLength = basicAuthenticationConfigurationProperties.getPasswordLength();
		StringBuilder sb = new StringBuilder(passwordLength);
		for (int i = 0; i < passwordLength; i++) {
			int randomIndex = secureRandom.nextInt(CHARACTERS.length());
			sb.append(CHARACTERS.charAt(randomIndex));
		}
		return sb.toString();
	}

	public String hashPassword(String plainPassword) {
		return encoder.encode(plainPassword);
	}

	public boolean checkPassword(String inputPlain, String passwortHash) {
		return encoder.matches(inputPlain, passwortHash);
	}
}
