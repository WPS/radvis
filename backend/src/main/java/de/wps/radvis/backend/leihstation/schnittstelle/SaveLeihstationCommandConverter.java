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

package de.wps.radvis.backend.leihstation.schnittstelle;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import jakarta.validation.Valid;

public class SaveLeihstationCommandConverter {

	public Leihstation convert(@Valid SaveLeihstationCommand command) {
		return new Leihstation(
			(Point) command.getGeometrie(),
			command.getBetreiber(),
			command.getAnzahlFahrraeder(),
			command.getAnzahlPedelecs(),
			command.getAnzahlAbstellmoeglichkeiten(),
			command.isFreiesAbstellen(),
			command.getBuchungsUrl(),
			command.getStatus(),
			LeihstationQuellSystem.RADVIS,
			null);
	}

	public void apply(Leihstation leihstation, @Valid SaveLeihstationCommand command) {
		leihstation.update(
			(Point) command.getGeometrie(),
			command.getBetreiber(),
			command.getAnzahlFahrraeder(),
			command.getAnzahlPedelecs(),
			command.getAnzahlAbstellmoeglichkeiten(),
			command.isFreiesAbstellen(),
			command.getBuchungsUrl(),
			command.getStatus());
	}

}
