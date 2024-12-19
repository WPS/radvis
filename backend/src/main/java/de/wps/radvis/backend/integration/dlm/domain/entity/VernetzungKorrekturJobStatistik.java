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

package de.wps.radvis.backend.integration.dlm.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wps.radvis.backend.common.domain.entity.JobStatistik;

public class VernetzungKorrekturJobStatistik extends JobStatistik {
	public int anzahlKnotenGeloescht = 0;
	public int anzahlDlmKantenVernetzungKorrigiert = 0;
	public int anzahlKnotenGeometrieDurchEntferneFalscheVernetzungKorrigiert = 0;
	public int anzahlKorrigiereDlmKnotenGeometrieRowsAffected = 0;
	public int anzahlVernetzungsfehlerVorJobausfuehrung = 0;
	public int anzahlVernetzungsfehlerNachJobausfuehrung = 0;
	public int anzahlVonDlmKantenVerwaisteKnotenVorher = 0;
	public int anzahlVonDlmKantenVerwaisteKnotenNachher = 0;
	public int anzahlKomplettVerwaisterKnotenVorher = 0;
	public int anzahlKomplettVerwaisterKnotenNachher = 0;
	public int anzahlKnotenMitNurRadvisKantenVorher = 0;
	public int anzahlKnotenMitNurRadvisKantenNachher = 0;
	public RadvisKantenVernetzungStatistik radvisKantenVernetzungStatistik = new RadvisKantenVernetzungStatistik();

	public int anzahlKnotenMitAuseinanderLiegendenKantenEndenVorEntfernungFalscherVernetzung = 0;
	public int anzahlFalscheVernetzungEntfernt = 0;
	public int anzahlKnotenNeuAngelegt = 0;
	public int anzahlKnotenCluster = 0;
	public int anzahlDurchMergeKnotenKorrigierteKanten = 0;
	public int anzahlKnotenKonntenNichtGemergedWerden = 0;

	@Override
	public String toString() {
		DefaultPrettyPrinter p = new DefaultPrettyPrinter();
		DefaultPrettyPrinter.Indenter i = new DefaultIndenter("  ", "\n");
		p.indentArraysWith(i);
		p.indentObjectsWith(i);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(p);
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}
}
