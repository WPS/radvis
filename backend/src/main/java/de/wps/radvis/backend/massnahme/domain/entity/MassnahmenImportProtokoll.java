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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.geotools.api.feature.simple.SimpleFeature;

import de.wps.radvis.backend.massnahme.domain.MassnahmenPaketIdExtractor;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MassnahmenImportProtokoll {
	public final MatchingStatistik matchingStatistik = new MatchingStatistik();

	@Getter
	private final Set<MassnahmenImportProblem> importFehler = new HashSet<>();
	public final AtomicInteger gesamtanzahlMassnahmen = new AtomicInteger();
	public final AtomicInteger countFiles = new AtomicInteger();

	public boolean hasFehler() {
		return !importFehler.isEmpty();
	}

	public HashMap<String, Set<MassnahmenImportProblem>> getMapVonIdAufFehler() {
		return importFehler.stream().collect(
			Collectors.groupingBy(
				MassnahmenImportProblem::getMassnahmenPaketId,
				HashMap::new,
				Collectors.toSet()));
	}

	public int getAnzahlFehlerhafterMassnahmen() {
		return getMapVonIdAufFehler().size();
	}

	public void reportKeineMassnahmePaketId(String id) {
		importFehler.add(new MassnahmenImportProblem(id,
			"SimpleFeature hat keine MassnahmenPaketId (MASSN_P),"
				+ " hier über Shp-Dateiname und Geometrie des Features referenziert"));
	}

	public void reportUngueltigeMassnahmePaketId(String id) {
		importFehler.add(new MassnahmenImportProblem(id,
			"SimpleFeature hat ungültige MassnahmenPaketId (MASSN_P)"));
	}

	public void reportDoppelteMassnahmenPaketId(String massnahmenPaketId) {
		importFehler.add(new MassnahmenImportProblem(massnahmenPaketId,
			"Doppelte MassnahmenPaketId (MASSN_P)!"));
	}

	public void reportLeererMultiLinestring(String massnahmenPaketId) {
		importFehler.add(new MassnahmenImportProblem(massnahmenPaketId,
			"MultiLineString enthält keine Geometrie"));
	}

	public void reportMatchUnvollstaendig(String massnahmenPaketId) {
		importFehler.add(new MassnahmenImportProblem(massnahmenPaketId,
			"Massnahme ohne Matching für mindestens einen Linestring!"));
	}

	public void reportMassnahmeOhneKategorie(SimpleFeature simpleFeature) {
		importFehler.add(new MassnahmenImportProblem(MassnahmenPaketIdExtractor.getMassnahmenPaketId(simpleFeature),
			"Massnahme ohne Kategorien"));
	}

	public void reportEmptyNetzbezug(String massnahmenPaketId) {
		importFehler.add(new MassnahmenImportProblem(massnahmenPaketId,
			"Netzbezug konnte nicht ermittelt werden"));
	}

	public void reportKeineRadVisNetzGeometrieInReichweite(String massnahmenPaketId) {
		importFehler.add(new MassnahmenImportProblem(massnahmenPaketId,
			"Es existiert keine RadVisNetzGeometrie in einem 30 Meter Radius der Massnahme"));
	}

	public void reportZielUndStartMassnahmeMitIdentischenKategorien(SimpleFeature simpleFeature) {
		importFehler.add(new MassnahmenImportProblem(MassnahmenPaketIdExtractor.getMassnahmenPaketId(simpleFeature),
			"Die Start- und Zielmassnahme haben identische Kategorien"));
	}

	public void log() {
		log.info("Gesamtzahl der importierten SHP-Dateien: {}", countFiles.get());
		log.info("Gesamtanzahl der importierten Massnahmen: {}",
			gesamtanzahlMassnahmen.get());
		log.info(matchingStatistik.toString());

		if (hasFehler()) {
			log.warn(
				"Beim Import der Massnahmen konnten {} Massnahmen nicht (vollständig) gematcht werden",
				getAnzahlFehlerhafterMassnahmen());
		}

		log.info("Es wurden {}% der Massnahmen ohne Fehler importiert", 100. - 100. *
			(!hasFehler() ? 1. :
				((double) getAnzahlFehlerhafterMassnahmen())
					/ (double) gesamtanzahlMassnahmen.get()));

		log.info("Import Fehler Details, gruppiert nach MASSN_P-Id:\n {}",
			getMapVonIdAufFehler());
	}
}
