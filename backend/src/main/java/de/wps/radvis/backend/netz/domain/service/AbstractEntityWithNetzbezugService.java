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

package de.wps.radvis.backend.netz.domain.service;

import java.lang.reflect.ParameterizedType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.springframework.context.event.EventListener;
import org.springframework.data.repository.CrudRepository;

import com.google.common.base.Objects;

import de.wps.radvis.backend.common.domain.BatchedCollectionIterator;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.service.AbstractVersionierteEntityService;
import de.wps.radvis.backend.netz.domain.entity.AbstractEntityWithNetzbezug;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.event.KanteErsetztEvent;
import de.wps.radvis.backend.netz.domain.event.KantenDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractEntityWithNetzbezugService<T extends AbstractEntityWithNetzbezug>
	extends AbstractVersionierteEntityService<T> {

	private NetzService netzService;
	private CrudRepository<T, Long> repository;
	private final double erlaubteAbweichungKantenRematch;

	protected AbstractEntityWithNetzbezugService(CrudRepository<T, Long> repository, NetzService netzService,
		double erlaubteAbweichungKantenRematch) {
		super(repository);
		this.repository = repository;
		this.netzService = netzService;
		this.erlaubteAbweichungKantenRematch = erlaubteAbweichungKantenRematch;
	}

	private String getEntityType() {
		String result = "";
		try {
			result = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName();
		} catch (Exception e) {
		}

		String[] split = result.split("\\.");

		if (split.length > 0) {
			result = split[split.length - 1];
		}

		return result;
	}

	@EventListener
	public void onKantenGeloescht(KantenDeletedEvent event) {
		log.debug("Ändere Netzbezug für {}", getEntityType());

		List<T> entitiesWithKanteInNetzbezug = new ArrayList<>();

		// Batching, da Hibernate/Postgres nur eine gewisse Anzahl an Parametern in "someId IN (...)"-Queries zulässt.
		BatchedCollectionIterator.iterate(
			event.getKantenIds(),
			1000,
			(kantenIdBatch, startIndex, endIndex) -> {
				log.debug("Lade Entities für Kanten-Batch {} bis {}", startIndex, endIndex);
				entitiesWithKanteInNetzbezug.addAll(findByKantenIdsInNetzBezug(kantenIdBatch));
			});

		if (!Objects.equal(event.getAusloeser(), NetzAenderungAusloeser.RADVIS_KANTE_LOESCHEN)) {
			for (int i = 0; i < event.getKantenIds().size(); i++) {
				Long kanteId = event.getKantenIds().get(i);
				Geometry geometry = event.getGeometries().get(i);

				List<T> zuProtokollierendeEntities = entitiesWithKanteInNetzbezug.stream()
					.filter(m -> m.getNetzbezug().containsKante(kanteId))
					.toList();

				if (!zuProtokollierendeEntities.isEmpty()) {
					protokolliereNetzBezugAenderungFuerGeloeschteKanten(zuProtokollierendeEntities, kanteId,
						geometry, event.getDatum(), event.getAusloeser());
				}

			}

		}

		log.debug("Passe Netzbezüge von {} {} an", entitiesWithKanteInNetzbezug.size(), getEntityType());
		entitiesWithKanteInNetzbezug.forEach(entity -> {
			entity.removeKanteFromNetzbezug(event.getKantenIds());
			event.getStatistik().anzahlAngepassterNetzbezuege++;
		});

		repository.saveAll(entitiesWithKanteInNetzbezug);

		log.debug("Netzbezugänderung beendet");
	}

	protected abstract void protokolliereNetzBezugAenderungFuerGeloeschteKanten(List<T> entitiesWithKanteInNetzbezug,
		Long kanteId, Geometry geometry, LocalDateTime datum, NetzAenderungAusloeser ausloeser);

	@EventListener
	public void onKanteErsetzt(KanteErsetztEvent event) {
		List<T> entitiesWithNetzbezugOnKante = findByKantenIdsInNetzBezug(
			List.of(event.getZuErsetzendeKante().getId()));

		entitiesWithNetzbezugOnKante.forEach(entity -> {
			log.debug("Kante {} in {} {} wird ersetzt...", event.getZuErsetzendeKante().getId(),
				getEntityType(), entity.getId());
			entity.ersetzeKanteInNetzbezug(event.getZuErsetzendeKante(), event.getErsetztDurch(),
				erlaubteAbweichungKantenRematch);

			if (!entity.getNetzbezug().containsKante(event.getZuErsetzendeKante().getId())) {
				log.debug("Kante {} in {} {} wurde erfolgreich ersetzt.", event.getZuErsetzendeKante(), entity.getId());
				event.getStatistik().anzahlAngepassterNetzbezuege++;
			}
		});

		repository.saveAll(entitiesWithNetzbezugOnKante);
	}

	protected abstract List<T> findByKantenIdsInNetzBezug(Collection<Long> ids);

	@EventListener
	public void onKnotenGeloescht(KnotenDeletedEvent event) {
		log.debug("Ändere Netzbezug für {}", this.getEntityType());

		log.debug("Suche Ersatzknoten");
		Map<Long, Knoten> ersatzKnoten = new HashMap<Long, Knoten>();
		List<Long> knotenIds = event.getKnoten().stream().map(kn -> kn.getId()).toList();

		if (FeatureTogglz.NETZBEZUG_REMATCH.isActive()) {
			event.getKnoten().forEach(knoten -> {
				netzService.findErsatzKnoten(knoten.getId(), knotenIds)
					.ifPresent(kn -> ersatzKnoten.put(knoten.getId(), kn));
			});
		}
		log.debug("Für {} von {} Knoten wurde ein Ersatzknoten gefunden: {}", ersatzKnoten.size(),
			event.getKnoten().size(),
			ersatzKnoten.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue().getId()).toList());

		List<T> entitiesWithNetzbezugOnKnoten = new ArrayList<>();

		// Batching, da Hibernate/Postgres nur eine gewisse Anzahl an Parametern in "someId IN (...)"-Queries zulässt.
		BatchedCollectionIterator.iterate(knotenIds, 1000, (knotenIdBatch, startIndex, endIndex) -> {
			log.debug("Lade Entities für Knoten-Batch {} bis {}", startIndex, endIndex);
			entitiesWithNetzbezugOnKnoten.addAll(findByKnotenInNetzbezug(knotenIdBatch));
		});

		log.debug("Ersetze Knoten in {} {} ...", entitiesWithNetzbezugOnKnoten.size(),
			getEntityType());
		if (!ersatzKnoten.isEmpty()) {
			entitiesWithNetzbezugOnKnoten.forEach(entity -> {
				log.debug("Ersetze Knoten in Netzbezug für Entity {} ...", entity.getId());
				long anzahlZuErsetzendeKnotenInNetzbezugBefore = ersatzKnoten.keySet().stream()
					.filter(knotenId -> entity.getNetzbezug().containsKnoten(knotenId)).count();
				entity.ersetzeKnotenInNetzbezug(ersatzKnoten);
				long anzahlZuErsetzendeKnotenInNetzbezugAfter = ersatzKnoten.keySet().stream()
					.filter(knotenId -> entity.getNetzbezug().containsKnoten(knotenId)).count();
				long anzahlErsetzteKnoten = anzahlZuErsetzendeKnotenInNetzbezugBefore
					- anzahlZuErsetzendeKnotenInNetzbezugAfter;
				if (anzahlErsetzteKnoten > 0) {
					log.debug("Es wurden {} Knoten erfolgreich ersetzt.", anzahlErsetzteKnoten);
					event.getStatistik().anzahlKnotenbezuegeErsetzt++;
				}
			});
		}

		event.getKnoten().forEach(knoten -> {
			List<T> entitiesForKnoten = entitiesWithNetzbezugOnKnoten.stream()
				.filter(m -> m.getNetzbezug().containsKnoten(knoten.getId())).toList();
			if (!entitiesForKnoten.isEmpty()) {
				protokolliereNetzBezugAenderungFuerGeloeschteKnoten(entitiesForKnoten, knoten,
					event.getDatum(), event.getAusloeser());
			}
		});

		log.debug("Lösche verbliebene Knoten in {} Netzbezügen ...", entitiesWithNetzbezugOnKnoten.size());
		entitiesWithNetzbezugOnKnoten.forEach(m -> {
			int anzahlKnotenBefore = m.getNetzbezug().getImmutableKnotenBezug().size();
			m.removeKnotenFromNetzbezug(knotenIds);
			if (m.getNetzbezug().getImmutableKnotenBezug().size() < anzahlKnotenBefore) {
				event.getStatistik().anzahlKnotenbezuegeGeloescht++;
			}
		});

		log.debug("Speichere {} {}", entitiesWithNetzbezugOnKnoten.size(), getEntityType());
		repository.saveAll(entitiesWithNetzbezugOnKnoten);

		log.debug("Netzbezugänderung beendet");
	}

	protected abstract void protokolliereNetzBezugAenderungFuerGeloeschteKnoten(List<T> entitiesWithKnotenInNetzbezug,
		Knoten knoten, LocalDateTime datum, NetzAenderungAusloeser ausloeser);

	protected abstract Collection<? extends T> findByKnotenInNetzbezug(List<Long> knotenIds);
}
