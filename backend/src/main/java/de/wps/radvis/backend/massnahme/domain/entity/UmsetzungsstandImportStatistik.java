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

package de.wps.radvis.backend.massnahme.domain.entity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UmsetzungsstandImportStatistik extends JobStatistik {
	private int anzahlAllerCsvZeilen = 0;
	private int anzahlGefilterterCsvZeilen = 0;
	private int anzahlDeduplizierterCsvZeilen = 0;
	private int anzahlGefundenerZugehoerigerMassnahmenIds = 0;
	private int anzahlGefundenerZugehoerigerMassnahmen = 0;
	private int anzahlMassnahmenAufUmgesetztGesetzt = 0;
	private int anzahlAktualisierterUmsetzungsstaende = 0;
	private int anzahlAktualisierterBaulastZustaendiger = 0;

	@ToString.Exclude
	private List<String> nichtGefundeneMassnahmenIds = new LinkedList<>();
	@ToString.Exclude
	private List<String> statusNichtGeaendertWegenManuellerAenderung = new LinkedList<>();
	@ToString.Exclude
	private List<String> statusNichtGeaendertWegenInkonsistenz = new LinkedList<>();
	@ToString.Exclude
	private Map<String, List<String>> kostenMappingFehler = new HashMap<>();
	@ToString.Exclude
	private Map<String, List<String>> umsetzungErfolgtMappingFehler = new HashMap<>();

	public void incrementAnzahlGefundenerZugehoerigerMassnahmenIds() {
		this.anzahlGefundenerZugehoerigerMassnahmenIds++;
	}

	public void incrementAnzahlGefundenerZugehoerigerMassnahmen() {
		this.anzahlGefundenerZugehoerigerMassnahmen++;
	}

	public void incrementAnzahlAktualisierterUmsetzungsstaende() {
		this.anzahlAktualisierterUmsetzungsstaende++;
	}

	public void incrementAnzahlAktualisierteBaulastZustaendige() {
		this.anzahlAktualisierterBaulastZustaendiger++;
	}

	public void incrementAnzahlMassnahmenAufUmgesetztGesetzt() {
		this.anzahlMassnahmenAufUmgesetztGesetzt++;
	}

	public void addNichtGefundeneMassnahmenId(String id) {
		this.nichtGefundeneMassnahmenIds.add(id);
	}

	public void addStatusNichtGeaendertWegenManuellerAenderung(String massnahmenPaketId, Long id) {
		this.statusNichtGeaendertWegenManuellerAenderung.add(massnahmenPaketId + " / " + id);
	}

	public void addStatusNichtGeaendertWegenInkonsistenz(String massnahmenPaketId, Long id) {
		this.statusNichtGeaendertWegenInkonsistenz.add(massnahmenPaketId + " / " + id);
	}

	public void addKostenMappingFehler(String dateiname, String massnahmenPaketId) {
		if (!this.kostenMappingFehler.containsKey(dateiname)) {
			this.kostenMappingFehler.put(dateiname, new LinkedList<>());
		}
		this.kostenMappingFehler.get(dateiname).add(massnahmenPaketId);
	}

	public void addUmsetzungErfolgtMappingFehler(String dateiname, String massnahmenPaketId) {
		if (!this.umsetzungErfolgtMappingFehler.containsKey(dateiname)) {
			this.umsetzungErfolgtMappingFehler.put(dateiname, new LinkedList<>());
		}
		this.umsetzungErfolgtMappingFehler.get(dateiname).add(massnahmenPaketId);
	}

	public String getKostenMappingFehlerFormattedString() {
		return this.getFormattedString(this.kostenMappingFehler);
	}

	public String getUmsetzungErfolgtMappingFehlerFormattedString() {
		return this.getFormattedString(this.umsetzungErfolgtMappingFehler);
	}
	
	private String getFormattedString(Map<String, List<String>> map) {
		StringBuilder sb = new StringBuilder();
		map.keySet().forEach(key -> {
			sb.append(key).append(": ");
			map.get(key).forEach(id -> sb.append(id).append(", "));
			sb.append("\n");
		});
		return sb.toString();
	}
}
