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

package de.wps.radvis.backend.weitereKartenebenen.domain.valueobject;

import static org.valid4j.Assertive.require;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
public class Quellangabe {

	public static final int MAX_LENGTH = 1000;

	@Getter
	@JsonValue
	@NonNull
	String value;

	private Quellangabe(@NonNull String value) {
		require(isValid(value), "Quellangabe muss mindestens eins und darf maximal " + MAX_LENGTH + " Zeichen haben.");
		this.value = value;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Quellangabe of(String value) {
		return new Quellangabe(value);
	}

	public static boolean isValid(String value) {
		int length = value.trim().length();
		return length <= MAX_LENGTH && length >= 1;
	}

	@Override
	public String toString() {
		return value;
	}

}
