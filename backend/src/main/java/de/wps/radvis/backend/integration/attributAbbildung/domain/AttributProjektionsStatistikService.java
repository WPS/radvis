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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributProjektionsJobStatistik;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjizierteKanteIstIsoliertException;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AttributProjektionsStatistikService {
	private final AttributeProjektionsProtokollService attributeProjektionsProtokollService;
	private final NetzService netzService;
	private final ImportedFeaturePersistentRepository importedFeatureRepository;
	private final RadNetzNetzbildungService radNetzNetzbildungService;
	private final RadwegeDBNetzbildungService radwegeDBNetzbildungService;

	public double berechneAnteilFeaturesProjiziert(QuellSystem quellSystem) {
		Stream<ImportedFeature> features = importedFeatureRepository.getAllByQuelleAndArt(quellSystem, Art.Strecke);

		if (quellSystem.equals(QuellSystem.RadNETZ)) {
			features = features.filter(feature -> {
				try {
					return radNetzNetzbildungService.isAktuell(feature);
				} catch (Exception e) {
					return false;
				}
			});
		} else if (quellSystem.equals(QuellSystem.RadwegeDB)) {
			features = features.filter(radwegeDBNetzbildungService::sollFeatureUebernommenWerden);
		}

		return features.mapToDouble(
				feature -> (feature.getAnteilProjiziert() == null ? 0 : feature.getAnteilProjiziert()) * feature
					.getGeometrie().getLength())
			.sum();
	}

	public void ueberpruefeTopologieDerNetzklassen(Envelope envelope, Set<Long> bereitsAbgearbeiteteIDs,
		AttributProjektionsJobStatistik statistik, String jobZordnung) {

		Envelope biggerEnvelopeForSearch = envelope.copy();
		biggerEnvelopeForSearch.expandBy(2000);

		Set<Kante> radnetzKanten = netzService
			.getKantenInBereichMitNetzklassen(biggerEnvelopeForSearch, Set.of(
				NetzklasseFilter.RADNETZ), false);

		Map<Knoten, List<Kante>> knotenZuKanteMap = new HashMap<>();

		radnetzKanten.forEach(kante -> {
			knotenZuKanteMap
				.merge(kante.getVonKnoten(), new ArrayList<>(List.of(kante)), (kantenBase, kantenNew) -> {
					kantenBase.addAll(kantenNew);
					return kantenBase;
				});
		});
		radnetzKanten.forEach(kante -> {
			knotenZuKanteMap
				.merge(kante.getNachKnoten(), new ArrayList<>(List.of(kante)), (kantenBase, kantenNew) -> {
					kantenBase.addAll(kantenNew);
					return kantenBase;
				});
		});

		radnetzKanten.stream()
			.filter(kante -> envelope.intersects(kante.getGeometry().getEnvelopeInternal()))
			.filter(kante -> !bereitsAbgearbeiteteIDs.contains(kante.getId()))
			.forEach(kante -> {
				if (istKanteIsoliertImBezugAufNetzklasse(kante, knotenZuKanteMap)) {
					statistik.laengeKantenIsoliertInmeter += kante.getGeometry().getLength();
					attributeProjektionsProtokollService
						.handle(new ProjizierteKanteIstIsoliertException(kante.getId(), kante.getGeometry()),
							jobZordnung);
				}
				bereitsAbgearbeiteteIDs.add(kante.getId());
			});
	}

	private boolean istKanteIsoliertImBezugAufNetzklasse(Kante kante,
		Map<Knoten, List<Kante>> knotenZuKantenMitNetzklasseMap) {
		int maxSearchDepth = 3;

		Set<Kante> kantenSeen = new HashSet<>();
		AtomicInteger anzahlRadNETZKantenInsgesamt = new AtomicInteger();
		anzahlRadNETZKantenInsgesamt.set(0);
		Set<Kante> kantenToCheck = new HashSet<>();
		kantenToCheck
			.addAll(knotenZuKantenMitNetzklasseMap.getOrDefault(kante.getVonKnoten(), Collections.emptyList()));
		kantenToCheck
			.addAll(knotenZuKantenMitNetzklasseMap.getOrDefault(kante.getNachKnoten(), Collections.emptyList()));
		kantenSeen.add(kante);
		kantenToCheck.remove(kante);
		for (int currentSearchDepth = 0; currentSearchDepth < maxSearchDepth
			&& !kantenToCheck.isEmpty(); currentSearchDepth++) {
			Set<Kante> nextKantenToCheck = new HashSet<>();
			kantenToCheck.forEach(
				kanteToCheck -> {
					kantenSeen.add(kanteToCheck);
					anzahlRadNETZKantenInsgesamt.incrementAndGet();
					nextKantenToCheck
						.addAll(knotenZuKantenMitNetzklasseMap
							.getOrDefault(kanteToCheck.getNachKnoten(), Collections.emptyList()));
					nextKantenToCheck
						.addAll(knotenZuKantenMitNetzklasseMap
							.getOrDefault(kanteToCheck.getVonKnoten(), Collections.emptyList()));
				});

			nextKantenToCheck.removeAll(kantenSeen);
			kantenToCheck = nextKantenToCheck;
		}
		return anzahlRadNETZKantenInsgesamt.get() < 3;
	}

}
