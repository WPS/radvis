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

package de.wps.radvis.backend.common.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class AbstractBooleanVO {

	@Getter
	@JsonValue
	protected final Boolean value;

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static AbstractBooleanVO of(Boolean value) {
		return new AbstractBooleanVO(value);
	}

	protected AbstractBooleanVO(Boolean value) {
		require(value, notNullValue());
		this.value = value;
	}

	protected AbstractBooleanVO(String value) {
		require((value.toLowerCase().equals("ja") || value.toLowerCase().equals("nein")), "muss ja oder nein sein");
		this.value = value.toLowerCase().equals("ja");
	}

	public String getValueAsString() {
		return value ? "ja" : "nein";
	}
}
