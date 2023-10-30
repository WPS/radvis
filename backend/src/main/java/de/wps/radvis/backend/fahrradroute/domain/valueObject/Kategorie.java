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

package de.wps.radvis.backend.fahrradroute.domain.valueObject;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Kategorie {
	TOURISTISCHE_ROUTE("Touristische Route"),
	LANDESRADFERNWEG("Landesradfernweg"),
	RADFERNWEG("Radfernweg"),
	RADSCHNELLWEG("Radschnellweg"),
	UEBERREGIONALER_RADWANDERWEG("Überregionaler Radwanderweg"),
	REGIONALER_RADWANDERWEG("Regionaler Radwanderweg"),
	VERBINDUNGSRADWANDERWEG("Verbindungsradwanderweg"),
	RADVERKEHRSNETZ("Radverkehrsnetz"),
	UNMARKIERTER_RADWANDERVORSCHLAG("Unmarkierter Radwandervorschlag"),
	SONSTIGER_RADWANDERWEG("Sonstiger Radwanderweg"),
	D_ROUTE("D-Route");

	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}
}