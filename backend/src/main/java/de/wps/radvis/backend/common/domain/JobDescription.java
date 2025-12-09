/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.common.domain;

public record JobDescription(
		String allgemeineBeschreibung,
		String auswirkungen,
		String abhaengigkeitenZuAnderenJobs,
		String warnungen,
		JobExecutionDurationEstimate laufzeitSchaetzung
	){

	public JobDescription(
		String allgemeineBeschreibung,
		String auswirkungen,
		JobExecutionDurationEstimate laufzeitSchaetzung
	) {
		this(allgemeineBeschreibung, auswirkungen, "", "", laufzeitSchaetzung);
	}

	public JobDescription(
		String allgemeineBeschreibung,
		String auswirkungen,
		String abhaengigkeitenZuAnderenJobs,
		JobExecutionDurationEstimate laufzeitSchaetzung
	) {
		this(allgemeineBeschreibung, auswirkungen, abhaengigkeitenZuAnderenJobs, "", laufzeitSchaetzung);
	}

	public String toHtml(boolean wiederholbar) {
		String rowFormatString = """
			<tr>
				<td>%s</td>
				<td>%s</td>
			</tr>
			""";
		String rows = "";

		if (!warnungen.isBlank()) {
			rows += String.format(rowFormatString, "<b style=\"color:red;\">WARNUNG</b>", warnungen);
		}
		if (!allgemeineBeschreibung.isBlank()) {
			rows += String.format(rowFormatString, "Beschreibung", allgemeineBeschreibung);
		}
		if (!auswirkungen.isBlank()) {
			rows += String.format(rowFormatString, "Auswirkungen", auswirkungen);
		}
		if (!abhaengigkeitenZuAnderenJobs.isBlank()) {
			rows += String.format(rowFormatString, "Abhängigkeiten", abhaengigkeitenZuAnderenJobs);
		}

		rows += String.format(rowFormatString, "Wiederholbar", wiederholbar ? "Ja" : "Nein");
		rows += String.format(rowFormatString, "Laufzeit", laufzeitSchaetzung.name() + "(" + laufzeitSchaetzung
			.getDisplayText() + ")");

		String tableFormatString = """
			<table>
			%s
			</table>""";
		return String.format(tableFormatString, rows);
	}
}
