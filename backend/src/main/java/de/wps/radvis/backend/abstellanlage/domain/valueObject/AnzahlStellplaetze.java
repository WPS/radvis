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

package de.wps.radvis.backend.abstellanlage.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class AnzahlStellplaetze {
	@Getter
	@JsonValue
	private final Integer value;

	private AnzahlStellplaetze(Integer value) {
		this.value = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static AnzahlStellplaetze of(Integer value) {
		require(value, notNullValue());
		return new AnzahlStellplaetze(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static AnzahlStellplaetze of(String value) {
		require(value, notNullValue());
		return new AnzahlStellplaetze(Integer.valueOf(value));
	}

	@Override
	public String toString() {
		return String.format("AnzahlStellplaetze{value='%s'}", value);
	}
}
