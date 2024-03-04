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

package de.wps.radvis.backend.matching.domain;

import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.springframework.data.util.Lazy;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.matching.domain.entity.MatchingJobStatistik;
import de.wps.radvis.backend.matching.domain.exception.GeometryLaengeMismatchException;
import de.wps.radvis.backend.matching.domain.exception.GeometryZuWeitEntferntException;
import de.wps.radvis.backend.matching.domain.exception.KanteNichtGematchedException;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.exception.MatchingFehlerException;
import de.wps.radvis.backend.matching.domain.service.MatchingJobProtokollService;
import de.wps.radvis.backend.matching.domain.service.MatchingKorrekturService;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.StreckenEinerPartition;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.StreckenViewService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchNetzAufDLMJob extends AbstractJob {

	private final Lazy<DlmMatchingRepository> dlmMatchingRepositorySupplier;
	private final NetzService netzService;
	private final NetzfehlerRepository netzfehlerRepository;
	private final MatchingKorrekturService korrekturService;
	private final StreckenViewService streckenViewService;
	private final MatchingJobProtokollService matchingJobProtokollService;
	private final EntityManager entityManager;
	private final DLMConfigurationProperties config;

	private final QuellSystem quellSystem;

	public MatchNetzAufDLMJob(JobExecutionDescriptionRepository repository,
		NetzfehlerRepository netzfehlerRepository,
		Lazy<DlmMatchingRepository> dlmMatchingRepositorySupplier,
		NetzService netzService,
		StreckenViewService streckenViewService,
		MatchingKorrekturService korrekturService,
		MatchingJobProtokollService matchingJobProtokollService,
		EntityManager entityManager,
		DLMConfigurationProperties config, QuellSystem quellSystem) {
		super(repository);

		require(netzfehlerRepository, Matchers.notNullValue());
		require(dlmMatchingRepositorySupplier, Matchers.notNullValue());
		require(netzService, Matchers.notNullValue());
		require(streckenViewService, Matchers.notNullValue());
		require(korrekturService, Matchers.notNullValue());
		require(matchingJobProtokollService, Matchers.notNullValue());
		require(entityManager, Matchers.notNullValue());
		require(quellSystem, Matchers.notNullValue());

		this.netzfehlerRepository = netzfehlerRepository;
		this.dlmMatchingRepositorySupplier = dlmMatchingRepositorySupplier;
		this.netzService = netzService;
		this.streckenViewService = streckenViewService;
		this.korrekturService = korrekturService;
		this.matchingJobProtokollService = matchingJobProtokollService;
		this.entityManager = entityManager;
		this.config = config;
		this.quellSystem = quellSystem;
	}

	@Override
	public String getName() {
		return super.getName() + " " + quellSystem;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MATCH_NETZ_AUF_DLM_JOB)
	public JobExecutionDescription run() {
		return super.run();
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MATCH_NETZ_AUF_DLM_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		log.info("Match Strecken");

		MatchingJobStatistik kantenStatistik = new MatchingJobStatistik();

		DlmMatchingRepository dlmMatchingRepository = dlmMatchingRepositorySupplier.get();

		AtomicInteger anzahlKantenAbgearbeitet = new AtomicInteger();
		anzahlKantenAbgearbeitet.set(0);
		netzfehlerRepository.deleteAllByjobZuordnung(getName());

		List<Envelope> partitionen = getPartitionen(config.getExtentProperty(), config.getPartitionenX());

		Set<Long> bereitsAbgearbeitet = new HashSet<>();
		List<StreckeVonKanten> unvollstaendigeStreckenUeberPartitionenHinweg = new ArrayList<>();

		for (Envelope partition : partitionen) {
			Envelope biggerEnvelopeForTopology = partition.copy();
			biggerEnvelopeForTopology.expandBy(2000);

			List<Kante> kanten = netzService.getKantenInBereichNachQuelleList(biggerEnvelopeForTopology, quellSystem);

			final StreckenEinerPartition<StreckeVonKanten> streckenDieserPartition = streckenViewService
				.createStreckenEinerPartition(kanten, partition, bereitsAbgearbeitet);

			unvollstaendigeStreckenUeberPartitionenHinweg.addAll(streckenDieserPartition.unvollstaendig);
			final StreckenEinerPartition<StreckeVonKanten> nochNichtBearbeiteteStreckenAllerPartitionen = streckenViewService
				.mergeUnvollstaendigeStrecken(unvollstaendigeStreckenUeberPartitionenHinweg);
			unvollstaendigeStreckenUeberPartitionenHinweg = nochNichtBearbeiteteStreckenAllerPartitionen.unvollstaendig;

			streckenDieserPartition.vollstaendig.addAll(nochNichtBearbeiteteStreckenAllerPartitionen.vollstaendig);

			streckenDieserPartition.vollstaendig.forEach(strecke -> {
				LineString streckenMatch = null;
				boolean streckenMatchZuSchlecht = false;
				try {
					streckenMatch = dlmMatchingRepository.matchGeometry(strecke.getStrecke(), "bike").getGeometrie();
					List<LineString> matchProKante = getMatchProKanteAusStreckenMatch(strecke, streckenMatch);
					for (int i = 0; i < strecke.getKanten().size(); i++) {
						Kante kante = strecke.getKanten().get(i);
						LineString matchDerKante = matchProKante.get(i);
						try {
							matchDerKante = korrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(
								kante.getId(), kante.getGeometry(), matchDerKante, null);
							kante.setAufDlmAbgebildeteGeometry(matchDerKante);
							netzService.saveKante(kante);
						} catch (MatchingFehlerException e) {
							// Konsistenzprüfung für Ausschnitt aus Strecke für die Kante hat nicht funktioniert
							// Probiere individuellen match
							matcheKanteIndividuell(dlmMatchingRepository, kante, kantenStatistik);
						}
					}
				} catch (KeinMatchGefundenException e) {
					kantenStatistik.anzahlKeineStreckenmatches++;
					streckenMatchZuSchlecht = true;
				} catch (GeometryLaengeMismatchException e) {
					kantenStatistik.anzahlStreckenmatchesZuLang++;
					streckenMatchZuSchlecht = true;
				} catch (GeometryZuWeitEntferntException e) {
					kantenStatistik.anzahlStreckenmatchesZuWeitEntfernt++;
					streckenMatchZuSchlecht = true;
				} catch (Exception e) {
					log.error("Es ist ein unbekannter fehler beim Matching aufgetreten: ", e);
					throw new RuntimeException(
						String.format("Es ist ein unbekannter fehler beim Matching aufgetreten:"), e);
				} finally {
					if (streckenMatchZuSchlecht) {
						strecke.getKanten()
							.forEach(kante -> matcheKanteIndividuell(dlmMatchingRepository, kante, kantenStatistik));
						kantenStatistik.anzahlOhneValideStreckenmatchesInsgesamt++;
					}
					anzahlKantenAbgearbeitet.addAndGet(strecke.getKanten().size());
					kantenStatistik.anzahlStrecken++;
				}
			});
			entityManager.flush();
			entityManager.clear();
			log.info("Partition {}/{} beendet. Es wurden bereits {} Kanten abgearbeitet",
				partitionen.indexOf(partition), partitionen.size(), anzahlKantenAbgearbeitet.get());
		}

		if (!unvollstaendigeStreckenUeberPartitionenHinweg.isEmpty()) {
			log.warn("Es gibt Strecken, die ueber alle Partitionen hinweg unvollstaendig sind. Dies ist wahrscheinlich"
				+ "ein Bug.");
			throw new RuntimeException(
				"Es gibt Strecken, die über alle Partitionen hinweg unvollstaendig sind. Dies ist wahrscheinlich"
					+ "ein Bug.");
		} else {
			log.info("Alle Strecken vollstaendig");
		}

		kantenStatistik.gesamtzahlKanten = anzahlKantenAbgearbeitet.get();
		kantenStatistik.anzahlKantenOhneMatchInsgesamt = kantenStatistik.anzahlKantenOhneGraphhopperMatch
			+ kantenStatistik.anzahlKantenMitZuSchlechtemGraphhopperMatch;
		kantenStatistik.anzahlOhneValideStreckenmatchesInsgesamt = kantenStatistik.anzahlStreckenmatchesZuLang
			+ kantenStatistik.anzahlStreckenmatchesZuWeitEntfernt
			+ kantenStatistik.anzahlKeineStreckenmatches;
		log.info(kantenStatistik.toString());

		return Optional.of(kantenStatistik);
	}

	private void matcheKanteIndividuell(DlmMatchingRepository dlmMatchingRepository, Kante kante,
		MatchingJobStatistik statistik) {
		boolean hatZuSchlechtesMatchBekommen = false;
		LineString match = null;

		try {
			match = matchGeometryBeideRichtungen(dlmMatchingRepository, kante.getGeometry(), kante.getId(), statistik);
		} catch (KeinMatchGefundenException e) {
			statistik.anzahlOhneMatch++;
			matchingJobProtokollService.handle(
				new KanteNichtGematchedException(kante.getId(), kante.getGeometry(), e.getMessage()), getName());
		} catch (GeometryLaengeMismatchException e) {
			matchingJobProtokollService.handle(e, getName());
			hatZuSchlechtesMatchBekommen = true;
			statistik.anzahlLaengeMismatch++;
			statistik
				.reportLaengeMismatch(Math.round(e.getAbgebildeteGeometryLaenge() - e.getOriginalGeometryLaenge()));
			statistik.reportLaengeMismatchKanteLaenge(kante.getGeometry().getLength());

		} catch (GeometryZuWeitEntferntException e) {
			statistik.anzahlZuWeitEntfernteMatches++;
			matchingJobProtokollService.handle(e, getName());
			hatZuSchlechtesMatchBekommen = true;
		}

		if (hatZuSchlechtesMatchBekommen) {
			statistik.anzahlKantenMitZuSchlechtemGraphhopperMatch++;
			return;
		}
		if (match == null) {
			statistik.anzahlKantenOhneGraphhopperMatch++;
			return;
		}
		kante.setAufDlmAbgebildeteGeometry(match);
		netzService.saveKante(kante);
	}

	private List<LineString> getMatchProKanteAusStreckenMatch(StreckeVonKanten strecke, LineString streckenMatch)
		throws GeometryZuWeitEntferntException, GeometryLaengeMismatchException {

		streckenMatch = korrekturService
			.checkGesamteStreckenGeometrieAufFehlerUndKorrigiere(null, strecke.getStrecke(), streckenMatch,
				null, strecke.getKanten().size());

		return streckenViewService
			.getSublinestringsDurchProjektionVonKnoten(streckenMatch, strecke.getKanten(), strecke.getVonKnoten());
	}

	private LineString matchGeometryBeideRichtungen(DlmMatchingRepository dlmMatchingRepository, LineString geometry,
		Long ID, MatchingJobStatistik statistik)
		throws KeinMatchGefundenException, GeometryZuWeitEntferntException, GeometryLaengeMismatchException {
		LineString match = null;

		match = dlmMatchingRepository.matchGeometry(geometry, "bike").getGeometrie();

		try {
			match = korrekturService
				.checkMatchingGeometrieAufFehlerUndKorrigiere(ID, geometry,
					match, statistik);
		} catch (MatchingFehlerException eIntern) {
			// Nochmal probieren mit umgedrehter Geometrie.
			// Hilft bei vielen Einbahnstraßen wo die Orientierung falsch ist.
			match = dlmMatchingRepository.matchGeometry(geometry.reverse(), "bike").getGeometrie();

			match = korrekturService
				.checkMatchingGeometrieAufFehlerUndKorrigiere(ID, geometry,
					match, statistik);
			statistik.anzahlUmdrehenHatGeholfen++;
		}
		return match;
	}
}
