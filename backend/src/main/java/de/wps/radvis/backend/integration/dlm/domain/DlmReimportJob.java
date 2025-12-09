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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanInitializationException;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.integration.dlm.domain.entity.DlmReimportJobStatistik;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.matching.domain.repository.CustomDlmMatchingRepositoryFactory;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.service.CustomGrundnetzMappingServiceFactory;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GrundnetzMappingService;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteDeleteStatistik;
import de.wps.radvis.backend.netz.domain.entity.KanteErsetzenStatistik;
import de.wps.radvis.backend.netz.domain.entity.KnotenDeleteStatistik;
import de.wps.radvis.backend.netz.domain.event.GrundnetzAktualisiertEvent;
import de.wps.radvis.backend.netz.domain.event.KanteErsetztEvent;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithFehlercode(Fehlercode.DLM_REIMPORT)
public class DlmReimportJob extends AbstractJob {

	// Gleicher Wert wie im Vernetzungskorrekturjob. Hat da beim Ausprobieren gut hingehauen.
	private static final double TOLERANZ_RADVIS_KNOTEN = 18.0;

	private final DlmPbfErstellungService dlmPbfErstellungService;
	private final KantenAttributeUebertragungService kantenAttributeUebertragungService;
	private final VernetzungService vernetzungService;
	private final NetzService netzService;
	private final DlmImportService dlmImportService;
	private final CustomDlmMatchingRepositoryFactory customDlmMatchingRepositoryFactory;
	private final CustomGrundnetzMappingServiceFactory customGrundnetzMappingServiceFactory;

	public DlmReimportJob(JobExecutionDescriptionRepository repository,
		DlmPbfErstellungService dlmPbfErstellungService,
		KantenAttributeUebertragungService kantenAttributeUebertragungService,
		VernetzungService vernetzungService,
		NetzService netzService,
		DlmImportService dlmImportService,
		CustomDlmMatchingRepositoryFactory customDlmMatchingRepositoryFactoryImpl,
		CustomGrundnetzMappingServiceFactory customGrundnetzMappingServiceFactory) {
		super(repository);
		this.dlmPbfErstellungService = dlmPbfErstellungService;
		this.kantenAttributeUebertragungService = kantenAttributeUebertragungService;
		this.vernetzungService = vernetzungService;
		this.netzService = netzService;
		this.dlmImportService = dlmImportService;
		this.customDlmMatchingRepositoryFactory = customDlmMatchingRepositoryFactoryImpl;
		this.customGrundnetzMappingServiceFactory = customGrundnetzMappingServiceFactory;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.DLM_REIMPORT_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run() {
		return run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.DLM_REIMPORT_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Transactional
	@Override
	protected Optional<JobStatistik> doRun() {
		DlmReimportJobStatistik dlmReimportJobStatistik = new DlmReimportJobStatistik();

		dlmImportService.importDlmNetz(dlmReimportJobStatistik.updateDlmNetzStatistik);

		// Nur, wenn es neu erstellte Kanten gibt, müssen wir auch etwas matchen. Wenn es keine gibt, haben wir
		// schließlich nichts auf das wir irgendwelche Attribute übertragen könnten.
		if (!dlmReimportJobStatistik.updateDlmNetzStatistik.hinzugefuegteKanten.isEmpty()) {
			log.info("Erstellt und lädt PBF-Datei mit {} neu erstellten Kanten",
				dlmReimportJobStatistik.updateDlmNetzStatistik.hinzugefuegteKanten.size());
			File dlmPbfFile;
			try {
				dlmPbfFile = createNewTempPbfFile();
				dlmPbfErstellungService.erstellePbfForKanten(dlmPbfFile,
					dlmReimportJobStatistik.updateDlmNetzStatistik.hinzugefuegteKanten);
			} catch (IOException e) {
				throw new RuntimeException(
					"Schreiben der PBF zum Übertragen der Attribute beim DLM-Reimport fehlgeschlagen", e);
			}

			// Erst jetzt Services erzeugen, da das DlmMatchingRepository davon ausgeht, dass eine PBF-Datei existiert.
			// Diese ist ab hier vorhanden, daher können wir die Services erzeugen.
			log.info("Erstelle Services und sonstige Abhängigkeiten");

			try (DlmMatchingRepository customMatchingRepository = customDlmMatchingRepositoryFactory
				.createCustomMatchingRepository(dlmPbfFile)) {

				GrundnetzMappingService grundnetzMappingService = customGrundnetzMappingServiceFactory
					.createGrundnetzMappingService(customMatchingRepository);

				log.info("Überträgt Attribute auf abzubildende Grundnetz-Kanten");
				Map<Long, Kante> inMemoryKantenRepository = new HashMap<>();

				for (Kante zuLoeschendeKante : dlmReimportJobStatistik.updateDlmNetzStatistik.zuLoeschendeKanten) {
					MatchingStatistik matchingStatistik = new MatchingStatistik();
					List<MappedGrundnetzkante> mappedGrundnetzKanten = grundnetzMappingService.mappeAufGrundnetz(
						zuLoeschendeKante.getGeometry(), matchingStatistik, (ids) -> {
							return dlmReimportJobStatistik.updateDlmNetzStatistik.hinzugefuegteKanten
								.stream()
								.filter(k -> ids.contains(k.getId()))
								.peek(k -> {
									if (inMemoryKantenRepository.containsKey(k.getId())) {
										dlmReimportJobStatistik.anzahlMoeglicherKollisionen += 1;
									} else {
										inMemoryKantenRepository.put(k.getId(), k);
									}
								})
								.toList();
						});

					if (mappedGrundnetzKanten.isEmpty()) {
						dlmReimportJobStatistik.anzahlKantenOhneAttributuebertragung += 1;
						log.debug("Kante {} konnte nicht abgebildet werden: kein Match gefunden", zuLoeschendeKante
							.getId());
						log.trace("Matching Statistik für Kante {}:\n{}", zuLoeschendeKante.getId(), matchingStatistik);
					} else {
						dlmReimportJobStatistik.anzahlKantenMitAttributuebertragung += 1;
						Set<Kante> matchingKanten = mappedGrundnetzKanten.stream()
							.map(mappedKante -> inMemoryKantenRepository.get(mappedKante.getKanteId()))
							.collect(Collectors.toSet());
						kantenAttributeUebertragungService.uebertrageAttribute(zuLoeschendeKante, matchingKanten);

						KanteErsetzenStatistik kanteErsetzenStatistik = new KanteErsetzenStatistik();
						if (FeatureTogglz.NETZBEZUG_REMATCH.isActive()) {
							RadVisDomainEventPublisher
								.publish(
									new KanteErsetztEvent(zuLoeschendeKante, matchingKanten, kanteErsetzenStatistik));
						}
						dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKanteErsetzt += kanteErsetzenStatistik.anzahlAngepassterNetzbezuege;
					}
				}
				log.info("Attributübernahme abgeschlossen");
			}
			dlmPbfFile.delete();
		} else {
			log.info("Keine neuen Kanten erstellt. PBF-Erstellung und Attributeübernahme wird übersprungen.");
		}

		entferneZuLoeschendeKantenUndKnoten(dlmReimportJobStatistik.updateDlmNetzStatistik.zuLoeschendeKanten,
			dlmReimportJobStatistik);

		vernetzungService.vernetzeAlleRadvisKantenNeu(
			dlmReimportJobStatistik.radvisKantenVernetzungStatistik, TOLERANZ_RADVIS_KNOTEN,
			NetzAenderungAusloeser.DLM_REIMPORT_JOB);

		dlmReimportJobStatistik.anzahlGeloeschterKnoten += dlmReimportJobStatistik.radvisKantenVernetzungStatistik.anzahlKnotenGeloescht;

		dlmReimportJobStatistik.anzahlVernetzungsfehlerNachJobausfuehrung = netzService
			.countAndLogVernetzungFehlerhaft();
		if (dlmReimportJobStatistik.anzahlVernetzungsfehlerNachJobausfuehrung > 0) {
			throw new RuntimeException("Vernetzung wurde kompromittiert! Rollback ...\n"
				+ dlmReimportJobStatistik.toString());
		}

		RadVisDomainEventPublisher.publish(new GrundnetzAktualisiertEvent());

		log.info("JobStatistik:\n{}", dlmReimportJobStatistik.toPrettyJSON());

		return Optional.of(dlmReimportJobStatistik);
	}

	@SuppressChangedEvents
	public void entferneZuLoeschendeKantenUndKnoten(Set<Kante> zuLoeschendeKanten,
		DlmReimportJobStatistik dlmReimportJobStatistik) {
		log.info("Entferne {} zu löschende Grundnetz-Kanten und verwaiste Knoten", zuLoeschendeKanten.size());

		KanteDeleteStatistik kanteDeleteStatistik = new KanteDeleteStatistik();
		netzService.deleteAll(zuLoeschendeKanten, NetzAenderungAusloeser.DLM_REIMPORT_JOB, kanteDeleteStatistik);
		KnotenDeleteStatistik knotenDeleteStatistik = new KnotenDeleteStatistik();
		dlmReimportJobStatistik.anzahlGeloeschterKnoten += netzService
			.deleteVerwaisteDLMKnoten(NetzAenderungAusloeser.DLM_REIMPORT_JOB, knotenDeleteStatistik);

		dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKanteGeloescht += kanteDeleteStatistik.anzahlAngepassterNetzbezuege;
		dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKnotenErsetzt += knotenDeleteStatistik.anzahlKnotenbezuegeErsetzt;
		dlmReimportJobStatistik.netzbezugAnpassungStatistik.anzahlMitKnotenGeloescht += knotenDeleteStatistik.anzahlKnotenbezuegeGeloescht;

		log.info("Entfernen von Grundnetz-Kanten und verwaisten Knoten abgeschlossen");
	}

	public File createNewTempPbfFile() {
		try {
			return File.createTempFile("dlm-reimport-filtered-pbf", ".osm.pbf");
		} catch (IOException e) {
			throw new BeanInitializationException(
				"Temporäre PBF-Datei für DLM-Reimport GraphHopper konnte nicht angelegt werden", e);
		}
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Importiert DLM-Daten aus externer Quelle und integriert diese in das bestehende Netz. Kanten können dadurch gelöscht, geometrisch verändert oder neu erstellt werden. Netzbezüge werden neu gematcht (je nach Einstellung vom Toggle "
				+ FeatureTogglz.NETZBEZUG_REMATCH.name() + ").",
			"Geometrien von Kanten und Knoten verändern sich und auch einige Attribute (z.B. Straßenname).",
			"Materialized Views und auf dem Netz aufbauende Daten (z.B. Profileigenschaften von Fahrradrouten) sind hiernach veraltet.",
			JobExecutionDurationEstimate.VERY_LONG
		);
	}
}
