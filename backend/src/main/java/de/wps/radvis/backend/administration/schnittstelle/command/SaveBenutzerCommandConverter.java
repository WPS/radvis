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

package de.wps.radvis.backend.administration.schnittstelle.command;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;

public class SaveBenutzerCommandConverter {
	private VerwaltungseinheitResolver verwaltungseinheitResolver;

	public SaveBenutzerCommandConverter(VerwaltungseinheitResolver verwaltungseinheitResolver) {
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
	}

	public void apply(Benutzer benutzer, SaveBenutzerCommand command) {
		benutzer.setNachname(command.getNachname());
		benutzer.setVorname(command.getVorname());
		benutzer.setMailadresse(command.getEmail());
		benutzer.setOrganisation(verwaltungseinheitResolver.resolve(command.getOrganisation()));
		benutzer.setRollen(command.getRollen());
	}
}
