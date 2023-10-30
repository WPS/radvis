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

package de.wps.radvis.backend.konsistenz.pruefung.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.transaction.Transactional;

import org.springframework.util.StopWatch;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RadVisStopWatchPrinter;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelPruefJobStatistik;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.konsistenz.pruefung.domain.event.KonsistenzregelVerletzungenDeletedEvent;
import de.wps.radvis.backend.konsistenz.regeln.domain.Konsistenzregel;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KonsistenzregelPruefJob extends AbstractJob {
	private List<Konsistenzregel> regeln;
	private KonsistenzregelVerletzungsRepository verletzungsRepository;

	public KonsistenzregelPruefJob(List<Konsistenzregel> regeln,
		KonsistenzregelVerletzungsRepository verletzungsRepository,
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository) {
		super(jobExecutionDescriptionRepository);
		this.verletzungsRepository = verletzungsRepository;
		this.regeln = regeln;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.KONSISTENZREGEL_PRUEFJOB)
	@SuppressChangedEvents
	public JobExecutionDescription run() {
		return run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.KONSISTENZREGEL_PRUEFJOB)
	@SuppressChangedEvents
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		KonsistenzregelPruefJobStatistik statistik = new KonsistenzregelPruefJobStatistik();
		LocalDateTime datum = LocalDateTime.now();
		StopWatch stopWatch = new StopWatch();

		regeln.forEach(regel -> {
			String regelClassSimpleName = regel.getClass().getSimpleName();
			log.info("Pruefe Konsistenzregel " + regelClassSimpleName);
			stopWatch.start(regelClassSimpleName);
			List<KonsistenzregelVerletzungsDetails> aktuelleVerletzungenDetails = regel.pruefen();
			statistik.anzahlAktuellProRegel.put(regelClassSimpleName, aktuelleVerletzungenDetails.size());
			log.info("Konsistenzregel " + regelClassSimpleName + " wurde geprüft.");
			stopWatch.stop();

			List<String> aktuelleVerletzungenIdentities = aktuelleVerletzungenDetails.stream().map(
				KonsistenzregelVerletzungsDetails::getIdentity).collect(Collectors.toList());
			List<KonsistenzregelVerletzung> bestehendeVerletzungen = verletzungsRepository.findAllByTyp(
				regel.getVerletzungsTyp());
			List<String> bestehendeVerletzungenIdentities = bestehendeVerletzungen.stream().map(
				KonsistenzregelVerletzung::getIdentity).collect(Collectors.toList());
			statistik.anzahlBestehendProRegel.put(regelClassSimpleName, bestehendeVerletzungen.size());

			// Lösche nicht mehr gefundene Verletzungen
			List<String> nichtMehrGefundeneVerletzungenIdentities = bestehendeVerletzungenIdentities.stream()
				.filter(bv -> !aktuelleVerletzungenIdentities.contains(bv)).collect(Collectors.toList());
			int deleteCount = verletzungsRepository.deleteAllByTypAndIdentityIn(
				regel.getVerletzungsTyp(),
				nichtMehrGefundeneVerletzungenIdentities);
			statistik.anzahlDeletedProRegel.put(regelClassSimpleName, deleteCount);

			if (FeatureTogglz.UMGESETZT_STATUS_AN_ANPASSUNGSWUENSCHEN_SCHREIBEN.isActive()) {
				RadVisDomainEventPublisher.publish(
					new KonsistenzregelVerletzungenDeletedEvent(regel.getVerletzungsTyp(),
						nichtMehrGefundeneVerletzungenIdentities));
			}

			// Update weiterhin bestehende Verletzungen
			List<KonsistenzregelVerletzung> zuAktualisierendeVerletzungen = bestehendeVerletzungen.stream().filter(
				weiterhinBestehendeVerletzung -> aktuelleVerletzungenDetails.stream().anyMatch(
					avd -> avd.getIdentity().equals(weiterhinBestehendeVerletzung.getIdentity())
						&& !weiterhinBestehendeVerletzung.hasEqualDetails(avd))).collect(Collectors.toList());

			zuAktualisierendeVerletzungen.forEach(
				zuAktualisierendeVerletzung -> aktuelleVerletzungenDetails.stream()
					.filter(avd -> avd.getIdentity().equals(zuAktualisierendeVerletzung.getIdentity()))
					.findFirst()
					.ifPresent(d -> zuAktualisierendeVerletzung.update(d, datum)));

			Iterable<KonsistenzregelVerletzung> aktualisierteVerletzungen = verletzungsRepository.saveAll(
				zuAktualisierendeVerletzungen);

			statistik.anzahlUpdatedProRegel.put(regelClassSimpleName,
				(int) StreamSupport.stream(aktualisierteVerletzungen.spliterator(), false).count());

			// Erzeuge neu gefundene Verletzungen
			List<KonsistenzregelVerletzung> neuGefundeneVerletzungen = aktuelleVerletzungenDetails.stream()
				.filter(av -> !bestehendeVerletzungenIdentities.contains(av.getIdentity()))
				.map(avd -> new KonsistenzregelVerletzung(avd, datum, regel.getTitel(),
					regel.getVerletzungsTyp())).collect(Collectors.toList());

			Iterable<KonsistenzregelVerletzung> erstellteVerletzungen = verletzungsRepository.saveAll(
				neuGefundeneVerletzungen);
			statistik.anzahlCreatedProRegel.put(regelClassSimpleName,
				(int) StreamSupport.stream(erstellteVerletzungen.spliterator(), false).count());
		});

		log.info("Verletzungen vorheriger Durchlauf " + statistik.anzahlBestehendProRegel);
		log.info("Verletzungen aktueller Durchlauf " + statistik.anzahlAktuellProRegel);
		log.info("Geloeschte Verletzungen " + statistik.anzahlDeletedProRegel);
		log.info("Aktualisierte Verletzungen " + statistik.anzahlUpdatedProRegel);
		log.info("Erzeugte Verletzungen " + statistik.anzahlCreatedProRegel);
		statistik.dauerProRegel = RadVisStopWatchPrinter.stringify(stopWatch);
		log.info("Dauer pro Regel \n{}", statistik.dauerProRegel);

		return Optional.of(statistik);
	}
}
