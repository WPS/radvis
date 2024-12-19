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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.valid4j.Assertive.require;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.dlm.domain.entity.UpdateDlmNetzStatistik;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DlmRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlmImportService {

	private final NetzService netzService;
	private final DlmRepository dlmRepository;

	public DlmImportService(DlmRepository dlmRepository, NetzService netzService) {
		this.netzService = netzService;
		this.dlmRepository = dlmRepository;
	}

	/**
	 * Importiert DLM neu und aktualisiert alle Kanten mit Quelle DLM. Alle Geometrien werden aktualisiert. Topologisch
	 * veränderte Kanten werden neu eingefügt. Es werden keine Kanten gelöscht, damit diese vorher weitergehend
	 * behandelt werden können.
	 * 
	 * @param statistik
	 */
	@SuppressChangedEvents
	public void importDlmNetz(UpdateDlmNetzStatistik statistik) {
		log.info("Start Import vom DLM-Netz");

		HashSet<String> processedDlmKanten = new HashSet<>();
		AtomicInteger partitionCounter = new AtomicInteger(1);

		List<Envelope> partitionen = dlmRepository.getPartitionen();
		partitionen.forEach(partition -> {
			log.debug("Verarbeite DLM-Kanten aus Partition {}/{} - {}", partitionCounter.getAndIncrement(), partitionen
				.size(), partition);

			HashSet<String> processedDlmKantenInThisPartition = new HashSet<>();

			List<ImportedFeature> importedDlmKanten = dlmRepository.getKanten(partition);
			Map<String, Kante> existingDlmKantenInBereich = new HashMap<>();
			netzService
				.getKantenInBereichNachQuelle(partition, QuellSystem.DLM)
				.forEach(kante -> {
					String dlmIdString = kante.getDlmId().getValue();
					// Bei Kanten, die neu erstellt / stark verändert wurden UND über Partitionen hinweg gehen, passiert
					// es, dass wir diese ggf. zwei mal bekommen: Einmal die veraltete Kante und einmal die neu
					// erstellte Kante aus der vorherigen Partition. Das ist aber legitim, da die veraltete Version der
					// Kante erst später im Import gelöscht wird. Daher diese Prüfung auf containsKey().
					if (!existingDlmKantenInBereich.containsKey(dlmIdString)) {
						existingDlmKantenInBereich.put(dlmIdString, kante);
					}
				});

			KnotenIndex knotenIndex = new KnotenIndex();
			netzService.getKnotenInBereichNachQuelle(partition, QuellSystem.DLM)
				.forEach(knotenIndex::fuegeEin);

			log.debug("{} existierende DLM-Kanten aus DB in Partition enthalten, {} DLM-Kanten aus Quelle geladen",
				existingDlmKantenInBereich.size(), importedDlmKanten.size());

			for (ImportedFeature importedDlmKante : importedDlmKanten) {
				if (processedDlmKantenInThisPartition.contains(importedDlmKante.getTechnischeId())) {
					// Die DLM-ID wurde innerhalb dieser Partition bereits behandelt, es handelt sich also um eindeutig
					// um ein Duplikat, welches wir übrspringen.
					statistik.anzahlImportierterKantenMitDoppelterDlmId += 1;
					continue;
				}
				if (processedDlmKanten.contains(importedDlmKante.getTechnischeId())) {
					// Kante wurde bereits in anderer Partition verarbeitet, was bei Kanten vorkommt, die Partitionen
					// überschreiten. Diese werden aus der Map entfernt, damit sie nicht als "gelöscht" interpretiert
					// werden. Wir haben die Kante bereits verarbeitet, daher brechen wir hier ab.
					existingDlmKantenInBereich.remove(importedDlmKante.getTechnischeId());
					continue;
				}

				statistik.anzahlImportierterKanten += 1;
				processedDlmKantenInThisPartition.add(importedDlmKante.getTechnischeId());

				Geometry geometry = importedDlmKante.getGeometrie();
				if (!geometry.getGeometryType().equals(Geometry.TYPENAME_LINESTRING)) {
					log.trace("DLM-Kante {} hat keine Liniengeometrie und wird ignoriert", importedDlmKante
						.getTechnischeId());
					statistik.anzahlImportierterKantenOhneLineStringGeometrie += 1;
					continue;
				}

				if (isAutobahn(importedDlmKante)) {
					log.trace("DLM-Kante {} ist eine Autobahn und wird ignoriert", importedDlmKante.getTechnischeId());
					statistik.anzahlImportierterAutobahnKanten += 1;
					continue;
				}

				if (isKreisGeometrie(importedDlmKante)) {
					log.trace("DLM-Kante {} hat eine Kreisgeometrie und wird ignoriert", importedDlmKante
						.getTechnischeId());
					statistik.anzahlImportierterKantenMitKreisgeometrie += 1;
					continue;
				}

				if (existingDlmKantenInBereich.containsKey(importedDlmKante.getTechnischeId())) {
					Kante existingKante = existingDlmKantenInBereich.get(importedDlmKante.getTechnischeId());
					if (existingKante.isGeometryUpdateValid((LineString) importedDlmKante.getGeometrie())) {
						boolean kanteVeraendert = false;
						if (!existingKante.isGeometryEqual((LineString) importedDlmKante.getGeometrie())) {
							log.trace(
								"DLM-Kante {} existiert in DB, wurde nicht wesentlich verändert -> Geometrie wird aktualisiert",
								importedDlmKante.getTechnischeId());
							existingKante.updateDLMGeometry((LineString) importedDlmKante.getGeometrie());
							kanteVeraendert = true;
						}
						if (!existingKante.getKantenAttributGruppe().getKantenAttribute().getStrassenName()
							.equals(getStrassenName(importedDlmKante))
							|| !existingKante.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer()
								.equals(getStrassenNummer(importedDlmKante))) {
							log.trace(
								"DLM-Kante {} existiert in DB, nur Straßenname und Straßennummer verändert -> Werte werden aktualisiert",
								importedDlmKante.getTechnischeId());
							existingKante.updateDlmInformationen(getStrassenName(importedDlmKante).orElse(null),
								getStrassenNummer(importedDlmKante).orElse(null));
							kanteVeraendert = true;
						}

						if (kanteVeraendert) {
							statistik.anzahlAktualisierterKanten += 1;
						} else {
							statistik.anzahlUnveraenderterKanten += 1;
						}

						existingDlmKantenInBereich.remove(existingKante.getDlmId().getValue());
					} else {
						log.trace(
							"DLM-Kante {} existiert in DB, wurde aber stark verändert -> wird neu erstellt und bisherige Kante zum löschen vorgemerkt",
							importedDlmKante.getTechnischeId());
						createKante(importedDlmKante, knotenIndex, partition, statistik);
						statistik.anzahlTopologischStarkVeraenderterKanten += 1;
					}
				} else {
					log.trace("DLM-Kante {} existiert nicht in DB -> wird erstellt", importedDlmKante
						.getTechnischeId());
					createKante(importedDlmKante, knotenIndex, partition, statistik);
					statistik.anzahlImDlmHinzugefuegterKanten += 1;
				}
			}

			// Alle verbleibenden Kanten wurden entweder topologisch stark verändert (und damit in RadVIS neu erzeugt)
			// oder aus DLM entfernt. Wir merken sie uns um später Attribute auf neue Kanten zu übertragen und die
			// Kanten danach zu löschen.
			statistik.zuLoeschendeKanten.addAll(existingDlmKantenInBereich.values());

			processedDlmKanten.addAll(processedDlmKantenInThisPartition);
		});

		log.info("Kanten aus DLM-Quelle importiert");
	}

	/**
	 * Erzeugt eine neue Kante für das gegebene Feature.
	 *
	 * @return Die neue Kante oder null, wenn keine Kante erzeugt werden konnte, weil z.B. von- und nach-Knoten gleich
	 *     gewesen wären.
	 */
	private void createKante(ImportedFeature dlmKante, KnotenIndex knotenIndex, Envelope partition,
		UpdateDlmNetzStatistik updateDlmNetzStatistik) {
		require(dlmKante.getGeometrie().getGeometryType().equals(Geometry.TYPENAME_LINESTRING));
		LineString lineString = (LineString) dlmKante.getGeometrie();

		Point vonPoint = lineString.getStartPoint();
		Point nachPoint = lineString.getEndPoint();

		if (!partition.contains(vonPoint.getCoordinate())) {
			Envelope envelope = new Envelope(vonPoint.getCoordinate());
			envelope.expandBy(KnotenIndex.SNAPPING_DISTANCE);
			netzService.getKnotenInBereichNachQuelle(envelope, QuellSystem.DLM).forEach(knotenIndex::fuegeEin);
		}

		if (!partition.contains(nachPoint.getCoordinate())) {
			Envelope envelope = new Envelope(nachPoint.getCoordinate());
			envelope.expandBy(KnotenIndex.SNAPPING_DISTANCE);
			netzService.getKnotenInBereichNachQuelle(envelope, QuellSystem.DLM).forEach(knotenIndex::fuegeEin);
		}

		Optional<Knoten> existingVonKnoten = knotenIndex.finde(vonPoint);
		Optional<Knoten> existingNachKnoten = knotenIndex.finde(nachPoint);

		if (existingVonKnoten.isPresent() && existingNachKnoten.isPresent() &&
			existingVonKnoten.get().getId() == existingNachKnoten.get().getId()) {
			// Es ist möglich, dass wir sehr kurze Kantenabschnitte bekommen, die zwar unterschiedliche Start- und End-
			// Koordinaten haben, aber so nah beieinander liegen, dass der selbe Knoten gefunden wird. Kurz heißt hier
			// auch durchauf <1m, obwohl wir beim DLM eigentlich von >1m langen Kanten ausgehen. Diese kurzen Kanten
			// können und wollen wir nicht importieren, da wir für uns intern immer nur Kanten >1m als valide ansehen.
			updateDlmNetzStatistik.anzahlImportierterKantenMitKreisgeometrie++;
			return;
		}

		Kante newKante = netzService
			.saveKante(new Kante(DlmId.of(dlmKante.getTechnischeId()), existingVonKnoten.orElse(new Knoten(vonPoint)),
				existingNachKnoten.orElse(new Knoten(nachPoint)), lineString,
				getStrassenName(dlmKante), getStrassenNummer(dlmKante)));

		if (existingVonKnoten.isEmpty()) {
			updateDlmNetzStatistik.anzahlHinzugefuegterKnoten += 1;
			knotenIndex.fuegeEin(newKante.getVonKnoten());
		}

		if (existingNachKnoten.isEmpty()) {
			updateDlmNetzStatistik.anzahlHinzugefuegterKnoten += 1;
			knotenIndex.fuegeEin(newKante.getNachKnoten());
		}

		updateDlmNetzStatistik.hinzugefuegteKanten.add(newKante);
	}

	private boolean isAutobahn(ImportedFeature importedFeature) {
		if (!importedFeature.hasAttribut("bezeichnung")) {
			return false;
		}
		// Die Bezeichnung ist entweder ein einzelner Strassenname z.B. A8 oder mit ; konkateniert z.B. E52;A8
		String[] strassenbezeichnungen = importedFeature.getAttribut("bezeichnung").toString().split(";");
		return Arrays.stream(strassenbezeichnungen)
			.anyMatch(strassenbezeichnung -> strassenbezeichnung.startsWith("A"));
	}

	private boolean isKreisGeometrie(ImportedFeature importedFeature) {
		return ((LineString) importedFeature.getGeometrie()).isClosed();
	}

	private Optional<StrassenNummer> getStrassenNummer(ImportedFeature feature) {
		if (feature.hasAttribut("bezeichnung")) {
			return Optional.of(StrassenNummer.of(feature.getAttribut("bezeichnung").toString()));
		}

		return Optional.empty();
	}

	private Optional<StrassenName> getStrassenName(ImportedFeature feature) {
		if (feature.hasAttribut("eigenname")) {
			return Optional.of(StrassenName.of(feature.getAttribut("eigenname").toString()));
		}

		return Optional.empty();
	}
}
