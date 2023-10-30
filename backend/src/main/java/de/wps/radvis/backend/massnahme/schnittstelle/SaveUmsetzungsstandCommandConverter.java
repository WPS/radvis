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

package de.wps.radvis.backend.massnahme.schnittstelle;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;

public class SaveUmsetzungsstandCommandConverter {

	private final BenutzerResolver benutzerResolver;

	public SaveUmsetzungsstandCommandConverter(BenutzerResolver benutzerResolver) {
		require(benutzerResolver, notNullValue());
		this.benutzerResolver = benutzerResolver;
	}

	public void apply(Authentication authentication, SaveUmsetzungsstandCommand command,
		Umsetzungsstand umsetzungsstand, Massnahme massnahme) {

		require(!Umsetzungsstand.isUmsetzungsstandBearbeitungGesperrt(umsetzungsstand, massnahme),
			"Der Umsetzungsstand muss bearbeitbar sein!");

		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		umsetzungsstand.update(
			command.isUmsetzungGemaessMassnahmenblatt(),
			LocalDateTime.now(),
			aktiverBenutzer,
			command.getGrundFuerAbweichungZumMassnahmenblatt(),
			command.getPruefungQualitaetsstandardsErfolgt(),
			command.getBeschreibungAbweichenderMassnahme(),
			command.getKostenDerMassnahme(),
			command.getGrundFuerNichtUmsetzungDerMassnahme(),
			command.getAnmerkung(), massnahme.getUmsetzungsstatus());
	}
}
