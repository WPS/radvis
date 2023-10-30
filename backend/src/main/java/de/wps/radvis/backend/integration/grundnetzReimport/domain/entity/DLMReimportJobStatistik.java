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

package de.wps.radvis.backend.integration.grundnetzReimport.domain.entity;

import de.wps.radvis.backend.integration.grundnetzReimport.domain.RadvisKantenVernetzungStatistik;

public class DLMReimportJobStatistik extends AbstractDLMImportStatistik {
	public int abgearbeiteteStrassen;
	public int abgearbeiteteWege;

	public int geometrieAenderungenMitTopologischerKonsequenz;
	public int geometrieAenderungen;
	public int strassenNamenAenderung;
	public int strassenNummerAenderung;
	public int topologieAenderungOhneSplit;

	public int anzahlSplits;
	public int anzahlTopologischeUpdatesOhneValideKanteAnVonUndNachKnoten;
	public int anzahlLoopsWaehrendSplitSuche;
	public int anzahlKantenInSplits;
	public int anzahlSplitsMitEinerKanteGefunden;

	public int nachSimpleTopologicalUpdateKeineUeberschneidung;
	public int durchSimpleTopologicalUpdateSehrWeitEntferntVonUrsprungskante;

	public int anzahlGeloescheterVerwaisterKnoten;
	public int geloeschteKanten;

	public int topologischesUpdateLiegtAusserhalbVonSplitBuffer;

	public int umkehrDerStationierungsrichtungNichtErkennbar;

	public boolean fatalErrorOccurred;
	public int isProjektionsReihenfolgeReversed;

	public RadvisKantenVernetzungStatistik radvisKantenVernetzungStatistik;
	public int anzahlVernetzungsfehlerNachJobausfuehrung;

	public void reset() {
		abgearbeiteteStrassen = 0;
		abgearbeiteteWege = 0;
		autobahnen = 0;
		strassenNamenAenderung = 0;
		strassenNummerAenderung = 0;
		nichtunterstuetzterGeometrietyp = 0;
		startUndEndpunktGleich = 0;
		anzahlSplits = 0;
		neueKanteHinzugefuegt = 0;
		topologieAenderungOhneSplit = 0;
		anzahlGeloescheterVerwaisterKnoten = 0;
		anzahlLoopsWaehrendSplitSuche = 0;
		anzahlTopologischeUpdatesOhneValideKanteAnVonUndNachKnoten = 0;
		anzahlKantenInSplits = 0;
		anzahlSplitsMitEinerKanteGefunden = 0;

		nachSimpleTopologicalUpdateKeineUeberschneidung = 0;
		durchSimpleTopologicalUpdateSehrWeitEntferntVonUrsprungskante = 0;
		geloeschteKanten = 0;

		topologischesUpdateLiegtAusserhalbVonSplitBuffer = 0;

		umkehrDerStationierungsrichtungNichtErkennbar = 0;

		fatalErrorOccurred = false;

		radvisKantenVernetzungStatistik = new RadvisKantenVernetzungStatistik();
		anzahlVernetzungsfehlerNachJobausfuehrung = 0;
	}
}