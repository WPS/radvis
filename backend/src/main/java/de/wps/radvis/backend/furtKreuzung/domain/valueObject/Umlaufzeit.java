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

package de.wps.radvis.backend.furtKreuzung.domain.valueObject;

import static org.valid4j.Assertive.require;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Umlaufzeit {

	@JsonValue
	@Getter
	private Integer zeit;

	public Umlaufzeit(int zeit) {
		require(isValid(zeit));

		this.zeit = zeit;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Umlaufzeit of(int value) {
		return new Umlaufzeit(value);
	}

	public static boolean isValid(int value) {
		return 0 <= value && value <= 9999;
	}
}
