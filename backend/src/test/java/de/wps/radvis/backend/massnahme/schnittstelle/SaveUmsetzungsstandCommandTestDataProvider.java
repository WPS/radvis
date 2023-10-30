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

import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerAbweichungZumMassnahmenblatt;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerNichtUmsetzungDerMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.schnittstelle.SaveUmsetzungsstandCommand.SaveUmsetzungsstandCommandBuilder;

public class SaveUmsetzungsstandCommandTestDataProvider {
	public static SaveUmsetzungsstandCommandBuilder defaultValue() {
		return SaveUmsetzungsstandCommand.builder()
			.umsetzungGemaessMassnahmenblatt(false)
			.grundFuerAbweichungZumMassnahmenblatt(GrundFuerAbweichungZumMassnahmenblatt.RADNETZ_WURDE_VERLEGT)
			.pruefungQualitaetsstandardsErfolgt(PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH)
			.beschreibungAbweichenderMassnahme("neue Beschreibung abweichender Massnahme")
			.kostenDerMassnahme(30000L)
			.grundFuerNichtUmsetzungDerMassnahme(GrundFuerNichtUmsetzungDerMassnahme.NOCH_IN_PLANUNG_UMSETZUNG)
			.anmerkung("neue Anmerkung");
	}
}
