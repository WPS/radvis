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

package de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.view;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class KonfliktView {
	@JsonProperty("Attributname")
	private final String attributName;
	@JsonProperty("Betroffener Abschnitt")
	private final String betroffenerAbschnitt;
	@JsonProperty("Übernommener Wert")
	private final String uebernommenerWert;
	@JsonProperty("Nicht übernommene Werte")
	private final String nichtUebernommeneWerte;
	@JsonProperty("Seitenbezug")
	private final String seitenbezug;
	@JsonProperty("Bemerkung")
	private final String bemerkung;
}
