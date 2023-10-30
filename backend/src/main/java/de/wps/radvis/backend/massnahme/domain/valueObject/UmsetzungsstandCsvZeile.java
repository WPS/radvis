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

package de.wps.radvis.backend.massnahme.domain.valueObject;

import java.util.List;

import com.opencsv.bean.CsvBindByName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class UmsetzungsstandCsvZeile {
	@CsvBindByName(column = "Dateiname")
	private String dateiname;
	@CsvBindByName(column = "Dateidatum")
	private String dateidatum;
	@CsvBindByName(column = "Maßnahmennummer")
	private String massnahmennummer;
	@CsvBindByName(column = "Stadt-/ Landkreis")
	private String stadtLandkreis;
	@CsvBindByName(column = "Gemeinde")
	private String gemeinde;
	@CsvBindByName(column = "Baulastträger laut Maßnahmenblatt")
	private String baullasttraegerLautMassnahmenBlatt;
	@CsvBindByName(column = "Baulastträger korrigiert")
	private String baullasttraegerKorrigiert;
	@CsvBindByName(column = "interne Nummer")
	private String interneNummer;
	@CsvBindByName(column = "Ist die Umsetzung erfolgt?")
	private String umsetzungErfolgt;
	@CsvBindByName(column = "Erfolgte die Umsetzung laut Maßnahmeblatt?")
	private String umsetzungLautMassnahmenblatt;
	@CsvBindByName(column = "Welchen Grund gibt es für das Abweichen vom Maßnahmenblatt?")
	private String grundFuerAbweichungZumMassnahmenblatt;
	@CsvBindByName(column = "Wurde die Einhaltung des Qualitätsstandards geprüft?")
	private String einhaltungQualitaetsstandardGeprueft;
	@CsvBindByName(column = "Beschreibung der abweichenden Maßnahme (Option)")
	private String beschreibungAbweichenderMassnahme;
	@CsvBindByName(column = "Bitte Kosten für die Maßnahme angeben (Option)")
	private String kostenFuerMassnahme;
	@CsvBindByName(column = "Anmerkungsfeld")
	private String anmerkungsfeld;
	@CsvBindByName(column = "Grund für Nicht-Umsetzung der Maßnahme")
	private String grundFuerNichtUmsetzungDerMassnahme;
	@CsvBindByName(column = "Anmerkungsfeld2")
	private String anmerkungsfeld2;

	public boolean isAlleFragenUnbeantwortet() {
		return this.getFragenWerte().stream().allMatch(fw -> fw == null || fw.isBlank());
	}

	public long getAnzahlBeantworteterFragen() {
		return this.getFragenWerte().stream().filter(fw -> fw != null && !fw.isBlank()).count();
	}

	public String getReferenceString() {
		return this.massnahmennummer + " (" + this.getDateiname() + ")";
	}

	private List<String> getFragenWerte() {
		return List.of(this.baullasttraegerKorrigiert, this.umsetzungErfolgt, this.umsetzungLautMassnahmenblatt,
			this.grundFuerAbweichungZumMassnahmenblatt, this.einhaltungQualitaetsstandardGeprueft,
			this.beschreibungAbweichenderMassnahme, this.kostenFuerMassnahme, this.anmerkungsfeld,
			this.grundFuerNichtUmsetzungDerMassnahme, this.anmerkungsfeld2);
	}
}
