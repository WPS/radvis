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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class NetzbezugHinweis {

	private final String text;

	private final String tooltip;

	private final Severity severity;

	private NetzbezugHinweis(String text, String tooltip, Severity severity) {
		require(text, notNullValue());
		require(tooltip, notNullValue());
		require(severity, notNullValue());

		this.text = text;
		this.tooltip = tooltip;
		this.severity = severity;
	}

	public static NetzbezugHinweis ofError(String text, String tooltip) {
		return new NetzbezugHinweis(text, tooltip, Severity.ERROR);
	}

	public static NetzbezugHinweis ofWarnung(String text, String tooltip) {
		return new NetzbezugHinweis(text, tooltip, Severity.WARN);
	}

	public enum Severity {
		ERROR, WARN
	}
}