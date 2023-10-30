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

public class Validators {
	public static final String URL_PATTERN = "^(https?://)(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.?[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()!@:%_+.,~#?&/=]*)$";
	public static final String EMAIL_PATTERN = "^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
	public static final String DATEIPFAD_ILLEGAL_PATTERN = ".*[\"*?<>|]+.*";

	public static boolean isValidURL(String value) {
		return value != null && value.matches(URL_PATTERN);
	}

	public static boolean isValidEmail(String value) {
		return value != null && value.matches(EMAIL_PATTERN);
	}

	public static boolean isValidDateipfad(String value) {
		return value != null && !value.matches(DATEIPFAD_ILLEGAL_PATTERN);
	}
}
