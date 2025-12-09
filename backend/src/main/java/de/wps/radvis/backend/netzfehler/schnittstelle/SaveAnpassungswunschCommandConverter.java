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

import java.util.Optional;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SaveAnpassungswunschCommandConverter {
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;

	public void apply(Anpassungswunsch anpassungswunsch, SaveAnpassungswunschCommand command, Benutzer benutzer) {
		String neueBeschreibung = command.getBeschreibung();
		AnpassungswunschStatus neuerStatus = command.getStatus();
		AnpassungswunschKategorie neueKategorie = command.getKategorie();
		Optional<Verwaltungseinheit> neueVerantwortlicheOrganisation = Optional
			.ofNullable(command.getVerantwortlicheOrganisation())
			.map(verwaltungseinheitResolver::resolve);
		Point neueGeometrie = (Point) command.getGeometrie();

		anpassungswunsch.update(
			neueBeschreibung,
			neuerStatus,
			neueKategorie,
			benutzer,
			neueVerantwortlicheOrganisation,
			neueGeometrie
		);
	}
}
