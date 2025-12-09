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

package de.wps.radvis.backend.servicestation.schnittstelle;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import jakarta.validation.Valid;

public class SaveServicestationCommandConverter {
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;

	public SaveServicestationCommandConverter(VerwaltungseinheitResolver verwaltungseinheitResolver) {
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
	}

	public void apply(SaveServicestationCommand command, Servicestation servicestation) {
		servicestation.updateAttribute(
			(Point) command.getGeometrie(),
			command.getName(),
			command.getGebuehren(),
			command.getOeffnungszeiten(),
			command.getBetreiber(),
			command.getMarke(),
			command.getLuftpumpe(),
			command.getKettenwerkzeug(),
			command.getWerkzeug(),
			command.getFahrradhalterung(),
			command.getBeschreibung(),
			verwaltungseinheitResolver.resolve(command.getOrganisationId()),
			command.getTyp(),
			command.getStatus(), command.getRadkultur());
	}

	public Servicestation convert(@Valid SaveServicestationCommand command) {
		return new Servicestation(
			(Point) command.getGeometrie(),
			command.getName(),
			command.getGebuehren(),
			command.getOeffnungszeiten(),
			command.getBetreiber(),
			command.getMarke(),
			command.getLuftpumpe(),
			command.getKettenwerkzeug(),
			command.getWerkzeug(),
			command.getFahrradhalterung(),
			command.getBeschreibung(),
			verwaltungseinheitResolver.resolve(command.getOrganisationId()),
			command.getTyp(),
			command.getStatus(),
			new DokumentListe(), command.getRadkultur());
	}
}
