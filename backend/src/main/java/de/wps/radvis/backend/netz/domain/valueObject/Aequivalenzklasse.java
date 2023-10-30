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

import org.locationtech.jts.geom.Coordinate;

import lombok.AllArgsConstructor;

@AllArgsConstructor(staticName = "of")
public class Aequivalenzklasse {
	// Reihenfolge der Coordinaten ist egal
	private Coordinate coordinate1;
	private Coordinate coordinate2;

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Aequivalenzklasse)) {
			return false;
		}
		Aequivalenzklasse other = (Aequivalenzklasse) o;
		return coordinate1.equals(other.coordinate1) && coordinate2.equals(other.coordinate2)
			|| coordinate1.equals(other.coordinate2) && coordinate2.equals(other.coordinate1);
	}

	@Override
	public int hashCode() {
		return coordinate1.hashCode() + coordinate2.hashCode();
	}
}
