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

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class Zindex {

	@Getter
	@JsonValue
	private Integer zindex;

	private Zindex(Integer zindex) {
		require(isValid(zindex), "ZIndex nicht valide (Muss positiv sein)");
		this.zindex = zindex;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Zindex of(Integer value) {
		return new Zindex(value);
	}

	private static boolean isValid(Integer value) {
		return value != null && value >= 0;
	}

	@Override
	public String toString() {
		return Integer.toString(zindex);
	}
}
