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

package de.wps.radvis.backend.netz.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum BauwerksmangelArt {
	GELAENDER_ZU_NIEDRIG("Geländer zu niedrig (< 1,30m)", List.of(KnotenForm.UEBERFUEHRUNG)),
	ZU_SCHMAL("zu schmal (< 4m)", List.of(KnotenForm.UEBERFUEHRUNG, KnotenForm.UNTERFUEHRUNG_TUNNEL)),
	RAMPE_MANGELHAFT("Rampe zu steil (> 6%) und/oder verwinkelt",
		List.of(KnotenForm.UEBERFUEHRUNG, KnotenForm.UNTERFUEHRUNG_TUNNEL)),
	ANDERER_MANGEL("Anderer Mangel", List.of(KnotenForm.UEBERFUEHRUNG, KnotenForm.UNTERFUEHRUNG_TUNNEL)),
	ZU_NIEDRIG("zu niedrig (< 2,5m)", List.of(KnotenForm.UNTERFUEHRUNG_TUNNEL)),
	;

	@NonNull
	@Getter
	private final String displayText;
	private final List<KnotenForm> validKnotenformen;

	public boolean isValidForKnotenform(KnotenForm knotenForm) {
		return validKnotenformen.contains(knotenForm);
	}

	public static boolean isRequiredForBauwerksmangel(Bauwerksmangel bauwerksmangel) {
		require(bauwerksmangel, notNullValue());
		return bauwerksmangel.equals(Bauwerksmangel.VORHANDEN);
	}
}
