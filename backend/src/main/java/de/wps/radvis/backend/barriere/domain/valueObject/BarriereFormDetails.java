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

package de.wps.radvis.backend.barriere.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public enum BarriereFormDetails {
	SPERRPFOSTEN_UNGESICHERT("Sperrpfosten ungesichert", BarrierenForm.SPERRPFOSTEN),
	SPERRPFOSTEN_GESICHERT("Sperrpfosten gesichert", BarrierenForm.SPERRPFOSTEN),
	SPERRPFOSTEN_GESICHERT_MIT_BODENMARKIERUNG("Sperrpfosten gesichert & mit Bodenmarkierung versehen",
		BarrierenForm.SPERRPFOSTEN),
	UMLAUFSPERRE_NICHT_BEFAHRBAR("Umlaufsperre nicht-befahrbar", BarrierenForm.UMLAUFSPERREN),
	UMLAUFSPERRE_REGELKONFORM("Umlaufsperre regelkonform", BarrierenForm.UMLAUFSPERREN),
	SCHRANKE_NICHT_UMFAHRBAR("Schranke nicht umfahrbar", BarrierenForm.SCHRANKE),
	SCHRANKE_NICHT_REGELKONFORM("Schranke regelkonform (es gelten die gleichen Maße wie bei Umlaufgittern)",
		BarrierenForm.SCHRANKE),
	SONSTIGE_GEFAHRENSTELLE("sonstige Gefahrenstelle", BarrierenForm.SONSTIGE_BARRIERE),
	SONSTIGE_BARRIERE("sonstige Barriere", BarrierenForm.SONSTIGE_BARRIERE),
	;

	@NonNull
	private final String displayText;

	@NonNull
	private final BarrierenForm forBarrierenForm;

	@Override
	public String toString() {
		return displayText;
	}

	public boolean isValidForBarrierenForm(BarrierenForm barriereForm) {
		require(barriereForm, notNullValue());

		return barriereForm == forBarrierenForm;
	}
}
