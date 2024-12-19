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

import de.wps.radvis.backend.common.domain.entity.JobStatistik;

public class AttributlueckenSchliessenJobStatistik extends JobStatistik {
	public int anzahlBetrachteteKantenGesamt = 0;
	public int anzahlPotentielleLueckenKanten = 0;

	public int anzahlPotentielleLueckenEndKnoten = 0;
	public int anzahlLueckenErmittelt = 0;

	/*
	Kantenzüge, die wir nicht als tatsächliche Lücken betrachten. Hier haben wir also angefangen Lücken zu suchen, aber
	abgebrochen. Somit sprechen wir hier von "potentiellen" Lücken, da wir ja abgebrochen und kein Ergebnis erzielt haben.
	 */

	// Hier wurde kein Pfad gefunden, weil die Kante zu groß ist und ein möglicher Pfad zu lang wäre.
	public int anzahlPotentielleLueckenIgnoriertDaAllePfadeZuLang = 0;

	// Hier wurde kein Pfad gefunden, weil die Lücke zu groß ist und zu viele Kanten nötig wären.
	public int anzahlPotentielleLueckenIgnoriertDaZuVieleKantenNoetig = 0;

	// Echte Sackgasse im Netz, was kein wirkliches Ende einer Lücke ist, da Lücken nur zwischen zwei Kanten existieren.
	public int anzahlPotentielleLueckenIgnoriertDaSackgasse = 0;

	// Am Ende der Lücke gehen mehrere Attribuierte Kanten ab, wodurch eine eindeutige Übernahme von Attributen nicht
	// möglich wäre.
	public int anzahlPotentielleLueckenIgnoriertDaKeineEindeutigeEndkante = 0;

	// Alle sonstigen Gründe, die es geben könnte, dass ein Pfad nicht gefunden wurde.
	public int anzahlPotentielleLueckenIgnoriertSonstigerGrund = 0;

	/*
	Kantenzüge, die wir als Lücke betrachten aber aus Gründen ignorieren:
	 */

	// Lücken, von deren Start-Knoten aus mehrere End-Knoten gefunden wurden.
	public int anzahlLueckenIgnoriertDaMehrdeutig = 0;

	// Lücken, bei denen nachträglich festgestellt wurde, dass sie sich in einem Knoten berühren. Letztendlich sind
	// das die gleichen Fälle wie anzahlLueckenIgnoriertDaMehrdeutig.
	public int anzahlLueckenIgnoriertDaGemeinsamerKnoten = 0;

	// Lücken, bei denen Start- und End-Knoten eine adjazente Kante gemeinsam haben. Die Lücke verbindet also eine Kante
	// mit sich selbst.
	public int anzahlLueckenIgnoriertDaGleicheKanteVerbunden = 0;
}