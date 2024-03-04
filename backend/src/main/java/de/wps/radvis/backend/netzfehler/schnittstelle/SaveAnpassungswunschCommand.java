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

package de.wps.radvis.backend.netzfehler.schnittstelle;

import org.hibernate.validator.constraints.Length;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SaveAnpassungswunschCommand {
	@NotNull
	private Geometry geometrie;

	@NotNull
	@Length(min = 1, max = 1000)
	private String beschreibung;

	@NotNull
	private AnpassungswunschStatus status;

	private Long verantwortlicheOrganisation;

	@NotNull
	private AnpassungswunschKategorie kategorie;

	private String fehlerprotokollId;

	@AssertTrue
	public boolean isGeometrieTypPunkt() {
		return geometrie.getGeometryType().equals(Geometry.TYPENAME_POINT);
	}
}
