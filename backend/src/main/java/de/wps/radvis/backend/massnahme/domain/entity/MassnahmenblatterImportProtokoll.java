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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MassnahmenblatterImportProtokoll {

	public final AtomicInteger countVerarbeiteteMassnahmenBlaetter = new AtomicInteger();
	public long massnahmenblaetterGesammt = 0;
	public final AtomicInteger countVerarbeiteteDokuBlaetter = new AtomicInteger();
	public long dokublaetterGesammt = 0;
	public final AtomicInteger erfolgreicheZuordnung = new AtomicInteger();

	public final AtomicInteger undgueltigeMPID = new AtomicInteger();
	@Getter
	private final Set<MassnahmenImportProblem> importFehler = new HashSet<>();
	public final AtomicInteger blaetterOhneMassnahme = new AtomicInteger();
	private final Set<String> mehrdeutigeMPID = new HashSet<>();
	public final AtomicInteger blaetterMitMehrdeutigerMPID = new AtomicInteger();

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

	public void reportUngueltigeMassnahmePaketId(String id) {
		importFehler.add(new MassnahmenImportProblem(id,
			"Datei hat ungültige MassnahmenPaketId (MASSN_P)"));
	}

	public void reportMehrdeutigeMassnahmenPaketId(String massnahmenPaketId) {
		mehrdeutigeMPID.add(massnahmenPaketId);
	}

	public void reportKeinePassendeMassnahme(String massnahmenPaketId) {
		importFehler.add(new MassnahmenImportProblem(massnahmenPaketId,
			"Keine passende Massname gefunden für MassnahmenPaketId (MASSN_P)!"));
	}

	public void reportDokumentNichtVerarbeitbar(String massnahmenPaketId) {
		importFehler.add(new MassnahmenImportProblem(massnahmenPaketId,
			"Dokumentdatei konnte nicht verarbeitet werden für MassnahmenPaketId (MASSN_P)!"));
	}

	public void log() {
		log.info("Gesamtzahl der verarbeiteten Massnahmenblaetter: {} von {}",
			countVerarbeiteteMassnahmenBlaetter.get(), massnahmenblaetterGesammt);
		log.info("Gesamtzahl der verarbeiteten Dokumentationsblaetter: {} von {}", countVerarbeiteteDokuBlaetter.get(),
			dokublaetterGesammt);
		log.info("Gesamtzahl vollständig erfolgreicher zuordnungen: {}", erfolgreicheZuordnung.get());

		if (hasFehler()) {
			log.warn(
				"Beim Import der Massnahmenblätter konnten {} Dateien nicht (eindeutig) gematcht werden",
				getAnzahlFehlerhafterMassnahmen());
		}

		log.info("Blättern mit ungültiger MPID gefunden: {}", undgueltigeMPID.get());
		log.info("Blättern ohne Massnahme (keine mit passender MPID gefunden): {}", blaetterOhneMassnahme.get());
		log.info("Blättern mit mehr als einer Massnahme mit passender MPID: {}", blaetterMitMehrdeutigerMPID.get());

		log.info("Es wurden {}% der Blätter ohne Fehler importiert", 100. *
			(Double.valueOf(erfolgreicheZuordnung.get()) /
				(Double.valueOf(massnahmenblaetterGesammt)
					+ dokublaetterGesammt
				)));

		log.info("Import Fehler Details, gruppiert nach MASSN_P-Id:\n {}",
			getMapVonIdAufFehler());
		log.info("Mehrere Massnahmen mit gleicher MassnahmenPID, gruppiert nach MASSN_P-Id:\n {}",
			mehrdeutigeMPID);
	}
}
