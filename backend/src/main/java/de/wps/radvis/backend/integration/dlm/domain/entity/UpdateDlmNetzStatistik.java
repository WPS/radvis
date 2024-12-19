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

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.wps.radvis.backend.netz.domain.entity.Kante;

public class UpdateDlmNetzStatistik {
	public int anzahlImportierterKanten;

	public int anzahlImportierterKantenOhneLineStringGeometrie;
	public int anzahlImportierterAutobahnKanten;
	public int anzahlImportierterKantenMitKreisgeometrie;
	public int anzahlImportierterKantenMitDoppelterDlmId;

	public int anzahlAktualisierterKanten;
	public int anzahlTopologischStarkVeraenderterKanten;
	public int anzahlUnveraenderterKanten;

	public int anzahlHinzugefuegterKnoten;
	public int anzahlImDlmHinzugefuegterKanten;

	// Alle Kanten, die neu erstellt wurden. Das heißt nicht, dass sie auch im DLM neu sind. Hier sind auch Kanten
	// enthalten, die stark verändert und somit bei uns gelöscht und neu erzeugt wurden.
	@JsonIgnore
	public Set<Kante> hinzugefuegteKanten;
	// Alle Kanten, die aus der DB zu löschen sind. Das umfasst auch alle stark veränderten Kanten, die hier bereits
	// enthalten sind.
	@JsonIgnore
	public Set<Kante> zuLoeschendeKanten;

	public UpdateDlmNetzStatistik() {
		zuLoeschendeKanten = new HashSet<>();
		hinzugefuegteKanten = new HashSet<>();
	}

	public int getAnzahlImDlmGeloeschterKanten() {
		return zuLoeschendeKanten.size() - anzahlTopologischStarkVeraenderterKanten;
	}
}
