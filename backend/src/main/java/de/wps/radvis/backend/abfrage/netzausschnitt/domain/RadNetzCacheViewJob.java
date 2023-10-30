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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain;

import static org.valid4j.Assertive.ensure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.locationtech.jts.geom.Envelope;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.event.TransactionalEventListener;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.event.PostDlmReimportJobEvent;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.StreckenEinerPartition;
import de.wps.radvis.backend.netz.domain.event.KanteGeometrieChangedEvent;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitChangedEvent;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.StreckenViewAbstractService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class RadNetzCacheViewJob<CacheTyp, StreckenTyp extends StreckeVonKanten> extends AbstractJob {

	private final KantenRepository kantenRepository;
	private final StreckenViewAbstractService<StreckenTyp> streckenViewService;
	private final EntityManager entityManager;
	protected final StreckeViewCacheRepository<CacheTyp, StreckenTyp> streckeViewCacheRepository;

	public RadNetzCacheViewJob(
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		KantenRepository kantenRepository,
		StreckenViewAbstractService<StreckenTyp> streckenViewService,
		StreckeViewCacheRepository<CacheTyp, StreckenTyp> streckeViewCacheRepository,
		EntityManager entityManager
	) {
		super(jobExecutionDescriptionRepository);
		this.kantenRepository = kantenRepository;
		this.streckenViewService = streckenViewService;
		this.streckeViewCacheRepository = streckeViewCacheRepository;
		this.entityManager = entityManager;
	}

	// alle 4 Stunden
	@Scheduled(cron = "${radVis.schedule.radNetzViewCacheJob}", zone = "Europe/Berlin")
	@Transactional
	// das Transactional ist wichtig, weil Jobs immer transactional ausgeführt werden sollten
	// In diesem Fall fliegt uns sonst z.B. das repository um die Ohren
	// Das Transactional in dem run() selbst greift hier nicht, weil wir das von *innerhalb* des Jobs aufrufen
	public void rebuildCacheCronJob() {
		log.info("Running RebuildCacheCronJob: {}", this.getClass().getSimpleName());
		run(true);
	}

	@NonNull
	protected List<StreckenTyp> getStreckenVonKanten(List<Envelope> partitions) {
		Set<Long> bereitsAbgearbeitet = new HashSet<>();
		List<StreckenTyp> unvollstaendigeStreckenUeberPartitionenHinweg = new ArrayList<>();
		List<StreckenTyp> streckenVonKanten = new ArrayList<>();

		for (Envelope partition : partitions) {
			Envelope biggerEnvelopeForTopology = partition.copy();
			biggerEnvelopeForTopology.expandBy(2000);

			List<Kante> kanten = new ArrayList<>(kantenRepository
				.getKantenInBereichNachNetzklasse(biggerEnvelopeForTopology, getNetzklassenFilter(), true));

			final StreckenEinerPartition<StreckenTyp> streckenDieserPartition = streckenViewService
				.createStreckenEinerPartition(kanten, partition, bereitsAbgearbeitet);

			List<StreckenTyp> langeStrecken = streckenDieserPartition.vollstaendig.stream()
				.filter(streckenTyp -> !biggerEnvelopeForTopology.intersects(streckenTyp.getVonKnoten().getKoordinate())
					|| !biggerEnvelopeForTopology.intersects(streckenTyp.getNachKnoten().getKoordinate()))
				.peek(streckenTyp -> {
					long countVonKnoten = kantenRepository.getAdjazenteKanten(streckenTyp.getVonKnoten()).stream()
						.filter(streckenTyp::passtAnStreckeRan).count();

					long countNachKnoten = kantenRepository.getAdjazenteKanten(streckenTyp.getNachKnoten()).stream()
						.filter(streckenTyp::passtAnStreckeRan).count();

					if (countVonKnoten == 2) {
						streckenTyp.setVonKnotenEndpunkt(false);
					}
					if (countNachKnoten == 2) {
						streckenTyp.setNachKnotenEndpunkt(false);
					}
				})
				.collect(Collectors.toList());

			unvollstaendigeStreckenUeberPartitionenHinweg.addAll(streckenDieserPartition.unvollstaendig);
			unvollstaendigeStreckenUeberPartitionenHinweg.addAll(langeStrecken);
			final StreckenEinerPartition<StreckenTyp> nochNichtBearbeiteteStreckenAllerPartitionen = streckenViewService
				.mergeUnvollstaendigeStrecken(unvollstaendigeStreckenUeberPartitionenHinweg);
			unvollstaendigeStreckenUeberPartitionenHinweg = nochNichtBearbeiteteStreckenAllerPartitionen.unvollstaendig;

			streckenDieserPartition.vollstaendig.addAll(nochNichtBearbeiteteStreckenAllerPartitionen.vollstaendig);


			streckenVonKanten.addAll(streckenDieserPartition.vollstaendig.stream().filter(StreckenTyp::abgeschlossen).collect(
				Collectors.toList()));

			entityManager.flush();
			entityManager.clear();
			log.info("Partition {}/{} beendet.", partitions.indexOf(partition) + 1, partitions.size());
		}
		log.info("Es blieben {} Strecken unvollständig und werden unverändert ausgeleitet", unvollstaendigeStreckenUeberPartitionenHinweg.size());
		streckenVonKanten.addAll(unvollstaendigeStreckenUeberPartitionenHinweg);
		return streckenVonKanten;
	}

	@EventListener
	public void onPostDlmReimport(PostDlmReimportJobEvent postDlmReimportJobEvent) {
		this.doRun();
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onKanteGeometrieChanged(KanteGeometrieChangedEvent kanteGeometrieChangedEvent) {
		this.streckeViewCacheRepository.getStreckenVonKanten().stream()
			.filter(streckeVonKanten -> streckeVonKanten.getKanten().stream()
				.anyMatch(kante -> kante.getId().equals(kanteGeometrieChangedEvent.getKanteId())))
			.forEach(streckeVonKanten -> {
				final Kante kante = kantenRepository.findById(kanteGeometrieChangedEvent.getKanteId()).orElseThrow();
				streckeVonKanten.updateKanteInStrecke(kante);
			});
		this.streckeViewCacheRepository.reloadCache();
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onNetzklassenZugehoerigkeitChanged(
		RadNetzZugehoerigkeitChangedEvent radNetzZugehoerigkeitChangedEvent) {
		final Kante modifizierteKante = kantenRepository.findByKantenAttributGruppeId(
			radNetzZugehoerigkeitChangedEvent.getKantenAttributGruppeId());

		if (radNetzZugehoerigkeitChangedEvent.isRadnetzZugehoerig()) {
			final StreckenTyp streckeVonKanten = this.createStreckeVonKanten(modifizierteKante);

			streckeVonKanten.setzeVonKnotenAlsEndknoten();
			streckeVonKanten.setzeNachKnotenAlsEndknoten();

			streckeViewCacheRepository.addStrecke(streckeVonKanten);
		} else {
			final Optional<StreckenTyp> result = this.streckeViewCacheRepository.getStreckenVonKanten().stream()
				.filter(streckeVonKanten -> streckeVonKanten.getKanten().stream()
					.anyMatch(kante -> kante.getId().equals(modifizierteKante.getId())))
				.findFirst();

			result.ifPresent(streckeVonKanten -> {
				// Modifiziert bestehende Strecke und returned die von der Strecke abgespaltenen Kanten
				final List<Kante> splitKanten = streckeVonKanten.splitAt(modifizierteKante);
				if (streckeVonKanten.getKanten().isEmpty()) {
					streckeViewCacheRepository.removeStrecke(streckeVonKanten);
				}
				if (!splitKanten.isEmpty()) {
					final StreckenTyp neueStreckeVonKanten = this.createStreckeVonKanten(splitKanten.get(0));
					for (int i = 1; i < splitKanten.size(); ++i) {
						neueStreckeVonKanten.addKante(splitKanten.get(i), false);
					}

					neueStreckeVonKanten.setzeVonKnotenAlsEndknoten();
					neueStreckeVonKanten.setzeNachKnotenAlsEndknoten();

					streckeViewCacheRepository.addStrecke(neueStreckeVonKanten);
				}
			});
		}

		this.streckeViewCacheRepository.reloadCache();
	}

	protected abstract StreckenTyp createStreckeVonKanten(Kante kante);

	protected abstract Set<NetzklasseFilter> getNetzklassenFilter();
}
