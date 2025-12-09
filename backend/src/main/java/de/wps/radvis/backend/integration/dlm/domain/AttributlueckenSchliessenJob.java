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

package de.wps.radvis.backend.integration.dlm.domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.GeoPackageExportConverter;
import de.wps.radvis.backend.integration.dlm.domain.entity.Attributluecke;
import de.wps.radvis.backend.integration.dlm.domain.entity.AttributlueckenSchliessenJobStatistik;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteWithInitialStatesView;
import de.wps.radvis.backend.netz.domain.repository.KantenWithInitialStatesRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AttributlueckenSchliessenJob extends AbstractJob {

	private final KantenWithInitialStatesRepository kanteWithInitialStatesRepository;
	private final NetzService netzService;
	private final AttributlueckenService attributlueckenService;
	private final boolean writeResultsToFile;

	public AttributlueckenSchliessenJob(
		JobExecutionDescriptionRepository repository,
		KantenWithInitialStatesRepository kanteWithInitialStatesRepository,
		NetzService netzService,
		AttributlueckenService attributlueckenService,
		boolean writeResultsToFile) {
		super(repository);
		this.kanteWithInitialStatesRepository = kanteWithInitialStatesRepository;
		this.netzService = netzService;
		this.attributlueckenService = attributlueckenService;
		this.writeResultsToFile = writeResultsToFile;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.ATTRIBUTLUECKEN_SCHLIESSEN_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run() {
		return run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.ATTRIBUTLUECKEN_SCHLIESSEN_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Transactional
	@Override
	protected Optional<JobStatistik> doRun() {
		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Lücken ermitteln und nach von- & nach-Knoten gruppieren. Die Menge dieser Kanten wird unten auch als
		// "Lückennetz" bezeichnet, weil es eben ein Teil des normalen Netzes ist, aber nur aus potentiellen Lücken
		// besteht.
		log.debug("Lade alle potentiellen Lücken-Kanten aus Datenbank");
		Map<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = ladePotentielleLueckenKantenNachKnotenGruppiert(
			statistik);
		log.debug("{} potentielle Lücken-Kanten aus DB geladen", statistik.anzahlPotentielleLueckenKanten);

		// Wir holen uns alle Kanten zu Beginn, da man sonst tausendfach an die DB müsste, um Kanten zu holen. Bei der
		// Menge an Lücken ist das kein performanter Weg. Hiermit haben wir alles im Speicher und die Dauer des Zugriffs
		// auf Kanten kann vernachlässigt werden.
		log.debug("Hole alle Kanten aus DB und gruppiere diese nach Knoten");
		Map<Long, List<Kante>> knotenToAllKantenMap = ladeAlleKantenNachKnotenGruppiert(statistik);
		log.debug("{} Kanten aus DB geladen", statistik.anzahlBetrachteteKantenGesamt);

		log.debug("Ermittle Lücken");
		List<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAllKantenMap, statistik);
		log.debug("{} Lücken ermittelt", statistik.anzahlLueckenErmittelt);

		if (writeResultsToFile) {
			writeAttributlueckenToFile(attributluecken);
		}

		log.debug("Versuche Lücken zu schließen");
		attributlueckenService.schliesseLuecken(attributluecken, knotenToKantenViewMap);
		log.debug("Lücken schließen beendet");

		log.info("JobStatistik:\n{}", statistik.toPrettyJSON());

		return Optional.of(statistik);
	}

	private static void writeAttributlueckenToFile(List<Attributluecke> attributluecken) {
		GeoPackageExportConverter exporter = new GeoPackageExportConverter();
		List<ExportData> lueckenExportData = attributluecken.stream()
			.map(luecke -> {
				return new ExportData(
					KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createMultiLineString(
						luecke.getLueckeKantenPfad().stream().map(k -> k.getGeometry()).toArray(LineString[]::new)),
					Map.of(
						"start_knoten", luecke.getStartKnoten().getId() + "",
						"end_knoten", luecke.getEndKnoten().getId() + "",
						"kanten_ids",
						luecke.getLueckeKantenPfad().stream().map(k -> k.getId() + "")
							.collect(Collectors.joining(","))));
			})
			.toList();

		if (!lueckenExportData.isEmpty()) {
			try {
				File exportedFile = exporter.convertToFile(lueckenExportData);
				log.info("Attributen als GeoPackage-Datei nach {} exportiert", exportedFile.getAbsolutePath());
				Files.copy(exportedFile.toPath(), new File("./luecken.gpkg").toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				log.error("Fehler beim GeoPackage Export der Attributlücken", e);
			}
		}
	}

	/**
	 * Ermittelt eine Map von Knoten zu adjazenten Kanten, wobei die Kanten initiale Werte halten, also leer und
	 * potentielle Lücken sind.
	 */
	private @NotNull HashMap<Long, List<KanteWithInitialStatesView>> ladePotentielleLueckenKantenNachKnotenGruppiert(
		AttributlueckenSchliessenJobStatistik statistik) {
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		kanteWithInitialStatesRepository.findByStatusNot(Status.FIKTIV).forEach(k -> {
			if (!knotenToKantenViewMap.containsKey(k.getVonKnotenId())) {
				knotenToKantenViewMap.put(k.getVonKnotenId(), new ArrayList<>());
			}
			knotenToKantenViewMap.get(k.getVonKnotenId()).add(k);

			if (!knotenToKantenViewMap.containsKey(k.getNachKnotenId())) {
				knotenToKantenViewMap.put(k.getNachKnotenId(), new ArrayList<>());
			}
			knotenToKantenViewMap.get(k.getNachKnotenId()).add(k);
			statistik.anzahlPotentielleLueckenKanten++;
		});
		return knotenToKantenViewMap;
	}

	private @NotNull HashMap<Long, List<Kante>> ladeAlleKantenNachKnotenGruppiert(
		AttributlueckenSchliessenJobStatistik statistik) {
		HashMap<Long, List<Kante>> knotenToAllKantenMap = new HashMap<>();
		netzService.findKanteByStatusNotAndQuelleIn(Status.FIKTIV, List.of(QuellSystem.DLM, QuellSystem.RadVis))
			.forEach(k -> {
				if (!knotenToAllKantenMap.containsKey(k.getVonKnoten().getId())) {
					knotenToAllKantenMap.put(k.getVonKnoten().getId(), new ArrayList<>());
				}
				knotenToAllKantenMap.get(k.getVonKnoten().getId()).add(k);

				if (!knotenToAllKantenMap.containsKey(k.getNachKnoten().getId())) {
					knotenToAllKantenMap.put(k.getNachKnoten().getId(), new ArrayList<>());
				}
				knotenToAllKantenMap.get(k.getNachKnoten().getId()).add(k);
				statistik.anzahlBetrachteteKantenGesamt++;
			});
		return knotenToAllKantenMap;
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Sucht im Netz nach Lücken in Attributen, also Kanten ohne Attribute, die aber an Kanten mit Attributen grenzen, und schließt diese Attributlücken.",
			"Attribute im Netz werden verändert, Geometrien bleiben unangetastet.",
			"Sollte am besten nach einem DLM-Reimport ausgeführt werden, da dieser zu Lücken führen kann. Die Materialized Views sind hiernach veraltet.",
			JobExecutionDurationEstimate.LONG
		);
	}
}
