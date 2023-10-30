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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import de.wps.radvis.backend.matching.domain.entity.MatchingJobStatistik;
import de.wps.radvis.backend.matching.domain.entity.OsmAbbildungsFehler;
import de.wps.radvis.backend.matching.domain.exception.GeometryLaengeMismatchException;
import de.wps.radvis.backend.matching.domain.exception.GeometryZuWeitEntferntException;
import de.wps.radvis.backend.matching.domain.exception.KanteNichtGematchedException;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.exception.MatchingFehlerException;
import de.wps.radvis.backend.matching.domain.service.MatchingJobProtokollService;
import de.wps.radvis.backend.matching.domain.service.MatchingKorrekturService;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometryView;
import de.wps.radvis.backend.netz.domain.entity.KanteOsmWayIdsInsert;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchNetzAufOSMJob extends AbstractJob {

	private final Lazy<OsmMatchingRepository> osmMatchingRepositorySupplier;
	private final NetzService netzService;
	private final NetzfehlerRepository netzfehlerRepository;
	private final MatchingKorrekturService korrekturService;
	private final MatchingJobProtokollService osmJobProtokollService;
	private final EntityManager entityManager;
	private final DLMConfigurationProperties config;
	private final OsmAbbildungsFehlerRepository osmAbbildungsFehlerRepository;

	public MatchNetzAufOSMJob(JobExecutionDescriptionRepository repository,
		NetzfehlerRepository netzfehlerRepository,
		Lazy<OsmMatchingRepository> osmMatchingRepositorySupplier,
		NetzService netzService,
		MatchingKorrekturService korrekturService,
		MatchingJobProtokollService osmJobProtokollService,
		EntityManager entityManager,
		DLMConfigurationProperties config,
		OsmAbbildungsFehlerRepository osmAbbildungsFehlerRepository) {
		super(repository);

		require(netzfehlerRepository, Matchers.notNullValue());
		require(osmMatchingRepositorySupplier, Matchers.notNullValue());
		require(netzService, Matchers.notNullValue());
		require(korrekturService, Matchers.notNullValue());
		require(osmJobProtokollService, Matchers.notNullValue());
		require(entityManager, Matchers.notNullValue());
		require(config, Matchers.notNullValue());
		require(osmAbbildungsFehlerRepository, Matchers.notNullValue());

		this.netzfehlerRepository = netzfehlerRepository;
		this.osmMatchingRepositorySupplier = osmMatchingRepositorySupplier;
		this.netzService = netzService;
		this.korrekturService = korrekturService;
		this.osmJobProtokollService = osmJobProtokollService;
		this.entityManager = entityManager;
		this.config = config;
		this.osmAbbildungsFehlerRepository = osmAbbildungsFehlerRepository;
	}

	@Override
	public String getName() {
		return super.getName();
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MATCH_NETZ_AUF_OSM_JOB)
	public JobExecutionDescription run() {
		return super.run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MATCH_NETZ_AUF_OSM_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {

		LocalDateTime startTimeJob = LocalDateTime.now();

		MatchingJobStatistik statistik = new MatchingJobStatistik();

		OsmMatchingRepository osmMatchingRepository = osmMatchingRepositorySupplier.get();
		List<OsmAbbildungsFehler> osmAbbildungsFehlerList = new ArrayList<>();

		int indexFuerFortschritt = 0;

		netzfehlerRepository.deleteAllByjobZuordnung(getName());

		String[] additionalProfiles = { "foot", "car" };

		List<Envelope> partitionen = getPartitionen(config.getExtentProperty(), config.getPartitionenX());

		Set<Long> bereitsAbgearbeitet = new HashSet<>();
		int currentPartition = 1;
		entityManager.createNativeQuery("ALTER TABLE kante_osm_way_ids DISABLE TRIGGER ALL").executeUpdate();
		entityManager.createNativeQuery("DROP INDEX IF EXISTS kante_osm_way_ids_kante").executeUpdate();
		entityManager.createNativeQuery("DROP INDEX IF EXISTS kante_osm_way_ids_value").executeUpdate();

		log.info("TRUNCATE OsmWayIds");
		netzService.truncateOsmWayIds();

		for (Envelope partition : partitionen) {
			log.info("Hole Kanten für Partition {} / {}", currentPartition++, partitionen.size());
			List<KanteGeometryView> kanten = netzService.getFuerOsmAbbildungRelevanteKanten(partition);
			log.info("... done. Anzahl Kanten mit vom default abweichenden Attributen: " + kanten.size());

			List<KanteOsmWayIdsInsert> inserts = new ArrayList<>();
			for (KanteGeometryView kante : kanten) {
				boolean hatZuSchlechtesOsmMatchBekommen = false;
				LineString match = null;
				if (bereitsAbgearbeitet.add(kante.getId())) {
					try {
						match = osmMatchingRepository.matchGeometry(kante.getGeometry(), "bike");

						try {
							match = korrekturService
								.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(), kante.getGeometry(),
									match,
									statistik);
						} catch (MatchingFehlerException eIntern) {
							// Nochmal probieren mit umgedrehter Geometrie.
							// Hilft bei vielen Einbahnstraßen wo die Orientierung falsch ist.
							match = osmMatchingRepository.matchGeometry(kante.getGeometry().reverse(), "bike");
							match = korrekturService
								.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(), kante.getGeometry(),
									match,
									statistik);
							statistik.anzahlUmdrehenHatGeholfen++;
						}

					} catch (KeinMatchGefundenException e) {
						statistik.anzahlOhneMatch++;
						osmJobProtokollService.handle(
							new KanteNichtGematchedException(kante.getId(), kante.getGeometry(), e.getMessage()),
							getName());
						hatZuSchlechtesOsmMatchBekommen = true;
					} catch (GeometryLaengeMismatchException e) {
						osmJobProtokollService.handle(e, getName());
						hatZuSchlechtesOsmMatchBekommen = true;
						statistik.anzahlLaengeMismatch++;
						statistik
							.reportLaengeMismatch(
								Math.round(e.getAbgebildeteGeometryLaenge() - e.getOriginalGeometryLaenge()));
						statistik.reportLaengeMismatchKanteLaenge(kante.getGeometry().getLength());
					} catch (GeometryZuWeitEntferntException e) {
						statistik.anzahlZuWeitEntfernteMatches++;
						osmJobProtokollService.handle(e, getName());
						hatZuSchlechtesOsmMatchBekommen = true;
					}

					if (match == null || hatZuSchlechtesOsmMatchBekommen) {
						for (String profile : additionalProfiles) {
							try {
								match = osmMatchingRepository.matchGeometry(kante.getGeometry(), profile);

								try {
									match = korrekturService
										.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(),
											kante.getGeometry(),
											match, statistik,
											MatchingKorrekturService.FLAT_LENGTH_DIFFERENCE_ERROR / 3, 1.4,
											MatchingKorrekturService.MAX_DISTANCE_TO_MATCHED_GEOMETRY / 4);
								} catch (MatchingFehlerException eIntern) {
									// Nochmal probieren mit umgedrehter Geometrie.
									// Hilft bei vielen Einbahnstraßen wo die Orientierung falsch ist.
									match = osmMatchingRepository.matchGeometry(kante.getGeometry().reverse(), profile);
									match = korrekturService
										.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(),
											kante.getGeometry(),
											match, statistik,
											MatchingKorrekturService.FLAT_LENGTH_DIFFERENCE_ERROR / 3, 1.4,
											MatchingKorrekturService.MAX_DISTANCE_TO_MATCHED_GEOMETRY / 4);
								}
								if (profile.equals("foot")) {
									statistik.anzahlMatchesMitFoot++;
								} else if (profile.equals("car")) {
									statistik.anzahlMatchesMitCar++;
								}
								hatZuSchlechtesOsmMatchBekommen = false;
								break;
							} catch (MatchingFehlerException e) {
								log.debug("Zu schlechtes Match gefunden für profile '" + profile + "'");
							} catch (KeinMatchGefundenException e) {
								log.debug("Kein Match gefunden für profile '" + profile + "'");
							}
						}
					}

					if (match == null) {
						statistik.anzahlKantenOhneGraphhopperMatch++;
						osmAbbildungsFehlerList.add(createOsmAbbildungsFehler(startTimeJob, kante));
						continue;
					}
					if (hatZuSchlechtesOsmMatchBekommen) {
						statistik.anzahlKantenMitZuSchlechtemGraphhopperMatch++;
						osmAbbildungsFehlerList.add(createOsmAbbildungsFehler(startTimeJob, kante));
						continue;
					}

					List<LinearReferenzierteOsmWayId> osmWayIds;
					try {
						log.debug("Kante {} / {}", kante.getId(), kante.getGeometry());
						osmWayIds = osmMatchingRepository.matchGeometryLinearReferenziert(match, "foot")
							.getLinearReferenzierteOsmWayIds();
					} catch (KeinMatchGefundenException e) {
						statistik.anzahlKantenOhneGraphhopperMatch++;
						osmAbbildungsFehlerList.add(createOsmAbbildungsFehler(startTimeJob, kante));
						continue;
					}

					if (!osmWayIds.isEmpty()) {
						inserts.add(new KanteOsmWayIdsInsert(kante.getId(), osmWayIds));
					}

					if (++indexFuerFortschritt % 10000 == 0) {
						log.info("Es wurde für {} Kanten versucht ein Match zu finden", indexFuerFortschritt);
					}
				}
			}
			netzService.insertOsmWayIds(inserts);

		}

		// Wir wollen alle Osm Abbildungsfehler vom vorherigen Durchlauf loeschen, sodass nur die aktuellen in der Table stehen
		osmAbbildungsFehlerRepository.deleteAll();
		log.info("Alte OsmAbbildungsfehler wurden gelöscht.");
		osmAbbildungsFehlerRepository.saveAll(osmAbbildungsFehlerList);
		log.info("Anzahl an neu geschriebenen OsmAbbildungsfehlern: " + osmAbbildungsFehlerList.size());

		long fehlerCountRadNETZ = osmAbbildungsFehlerList.stream().filter(OsmAbbildungsFehler::isRadnetz).count();
		long fehlerCountKreisnetz = osmAbbildungsFehlerList.stream().filter(OsmAbbildungsFehler::isKreisnetz).count();
		long fehlerCountKommunalnetz = osmAbbildungsFehlerList.stream().filter(OsmAbbildungsFehler::isKommunalnetz)
			.count();
		long fehlerCountUnklassifiziert = osmAbbildungsFehlerList.stream()
			.filter(OsmAbbildungsFehler::isSonstigeNetzklasse).count();
		log.info("Davon " + fehlerCountRadNETZ + " Abbildungsfehler von Kanten mit einer RadNETZ Netzklasse");
		log.info("Davon " + fehlerCountKreisnetz + " Abbildungsfehler von Kanten mit einer Kreisnetz Netzklasse");
		log.info("Davon " + fehlerCountKommunalnetz + " Abbildungsfehler von Kanten mit einer Kommunalnetz Netzklasse");
		log.info("Davon " + fehlerCountUnklassifiziert
			+ " Abbildungsfehler von Kanten mit keiner Netzklasse oder Radschnellverbindung oder Radvorrangrouten");

		log.info("Erstelle Index auf kante_osm_way_ids und value (osmWayId)...");
		entityManager.createNativeQuery("CREATE INDEX kante_osm_way_ids_kante ON kante_osm_way_ids (kante_id)")
			.executeUpdate();
		entityManager.createNativeQuery("CREATE INDEX kante_osm_way_ids_value ON kante_osm_way_ids (value)")
			.executeUpdate();
		entityManager.createNativeQuery("ALTER TABLE kante_osm_way_ids ENABLE TRIGGER ALL").executeUpdate();
		log.info("... Done.");

		statistik.gesamtzahlKanten = indexFuerFortschritt;
		statistik.anzahlKantenOhneMatchInsgesamt = statistik.anzahlKantenOhneGraphhopperMatch
			+ statistik.anzahlKantenMitZuSchlechtemGraphhopperMatch;
		log.info(statistik.toString());

		return Optional.of(statistik);
	}

	private OsmAbbildungsFehler createOsmAbbildungsFehler(LocalDateTime startTimeJob, KanteGeometryView kante) {
		boolean radNETZ = false;
		boolean kreisnetz = false;
		boolean kommunalnetz = false;

		Optional<String> netzklassenVonKante = netzService.getNetzklassenVonKante(kante.getId());
		if (netzklassenVonKante.isPresent()) {
			if (netzklassenVonKante.get().contains("RADNETZ")) {
				radNETZ = true;
			}
			if (netzklassenVonKante.get().contains("KREISNETZ")) {
				kreisnetz = true;
			}
			if (netzklassenVonKante.get().contains("KOMMUNALNETZ")) {
				kommunalnetz = true;
			}
		}

		return new OsmAbbildungsFehler(kante.getId(), kante.getGeometry(), startTimeJob, radNETZ, kreisnetz,
			kommunalnetz);
	}
}
