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

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonValue;

import de.wps.radvis.backend.common.domain.valueObject.AllgemeineMusterloesungen;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FurtKreuzungMusterloesung {
	private static final AllgemeineMusterloesungen[] wertebereich = new AllgemeineMusterloesungen[] {
			AllgemeineMusterloesungen.NR_2_2_1,
			AllgemeineMusterloesungen.NR_3_2_2,
			AllgemeineMusterloesungen.NR_3_2_3,
			AllgemeineMusterloesungen.NR_3_2_4,
			AllgemeineMusterloesungen.NR_3_2_5,
			AllgemeineMusterloesungen.NR_3_3_2,
			AllgemeineMusterloesungen.NR_3_4_5,
			AllgemeineMusterloesungen.NR_3_6_3,
			AllgemeineMusterloesungen.NR_4_3_1,
			AllgemeineMusterloesungen.NR_4_3_2,
			AllgemeineMusterloesungen.NR_4_4_1,
			AllgemeineMusterloesungen.NR_4_4_2,
			AllgemeineMusterloesungen.NR_4_4_3,
			AllgemeineMusterloesungen.NR_4_4_4,
			AllgemeineMusterloesungen.NR_4_4_5,
			AllgemeineMusterloesungen.NR_4_4_6,
			AllgemeineMusterloesungen.NR_4_4_7,
			AllgemeineMusterloesungen.NR_4_4_8,
			AllgemeineMusterloesungen.NR_4_5_1,
			AllgemeineMusterloesungen.NR_4_5_2,
			AllgemeineMusterloesungen.NR_4_5_3,
			AllgemeineMusterloesungen.NR_4_5_4,
			AllgemeineMusterloesungen.NR_4_5_5,
			AllgemeineMusterloesungen.NR_4_5_6
	};

	@Getter
	@JsonValue
	private AllgemeineMusterloesungen value;

	@JsonCreator(mode = Mode.DELEGATING)
	public static FurtKreuzungMusterloesung of(String value) {
		for (int i = 0; i < wertebereich.length; i++) {
			if (wertebereich[i].name().equals(value)) {
				return new FurtKreuzungMusterloesung(wertebereich[i]);
			}
		}

		throw new RuntimeException(
			value + " ist kein gültiger Wert für " + FurtKreuzungMusterloesung.class.getSimpleName());

	}

	public static List<AllgemeineMusterloesungen> getWertebereich() {
		return Arrays.asList(wertebereich);
	}
}
