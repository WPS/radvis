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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.GeometrienVerlaufMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteFuehrungsformAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteGeschwindigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteZustaendigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzNetzklasseMapView;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.NetzklassenStreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;
import lombok.NonNull;

public class NetzausschnittService {
	private final NetzfehlerRepository netzfehlerRepository;
	private final KantenAbfrageRepository kantenAbfrageRepository;
	private final KnotenRepository knotenRepository;
	private final StreckeViewCacheRepository<List<NetzklassenStreckenSignaturView>, NetzklassenStreckeVonKanten> netzklassenStreckenSignaturViewRepository;
	private final StreckeViewCacheRepository<NetzMapView, StreckeVonKanten> radNETZNetzViewCacheRepository;

	public NetzausschnittService(@NonNull NetzfehlerRepository netzfehlerRepository,
		@NonNull KantenAbfrageRepository kantenAbfrageRepository, @NonNull KnotenRepository knotenRepository,
		@NonNull StreckeViewCacheRepository<NetzMapView, StreckeVonKanten> radNETZNetzViewCacheRepository,
		@NonNull StreckeViewCacheRepository<List<NetzklassenStreckenSignaturView>, NetzklassenStreckeVonKanten> netzklassenStreckenSignaturViewRepository) {
		super();
		this.knotenRepository = knotenRepository;
		this.netzfehlerRepository = netzfehlerRepository;
		this.kantenAbfrageRepository = kantenAbfrageRepository;
		this.radNETZNetzViewCacheRepository = radNETZNetzViewCacheRepository;
		this.netzklassenStreckenSignaturViewRepository = netzklassenStreckenSignaturViewRepository;
	}

	public NetzMapView findNetzAusschnitt(Envelope sichtbereich, Set<NetzklasseFilter> netzklasseFilter) {
		Set<KanteMapView> kantenMapViews = kantenAbfrageRepository
			.getKantenMapViewInBereich(sichtbereich, netzklasseFilter);
		List<Knoten> knoten = new ArrayList<>();
		return new NetzMapView(kantenMapViews, knoten);
	}

	public NetzNetzklasseMapView findNetzAusschnittDLM(Envelope sichtbereich) {
		Set<KanteNetzklasseMapView> kantenMapViews = kantenAbfrageRepository
			.getKantenMapViewInBereichDlm(sichtbereich);
		List<Knoten> knoten = knotenRepository.getKnotenInBereichFuerQuelle(sichtbereich, QuellSystem.DLM);
		return new NetzNetzklasseMapView(kantenMapViews, knoten);
	}

	public NetzNetzklasseMapView findNetzAusschnittDLMIstRadNETZZugeordnet(Envelope sichtbereich) {
		Set<KanteNetzklasseMapView> kantenMapViews = kantenAbfrageRepository
			.getKantenMapViewInBereichDlmIstRadNETZZugeordnet(sichtbereich);
		return new NetzNetzklasseMapView(kantenMapViews, List.of());
	}

	public NetzMapView findNetzAusschnitt(Envelope sichtbereich, QuellSystem quelle) {
		Set<KanteMapView> kantenMapViews = kantenAbfrageRepository
			.getKantenMapViewInBereichFuerQuelle(sichtbereich, quelle);
		List<Knoten> knoten = knotenRepository.getKnotenInBereichFuerQuelle(sichtbereich, quelle);
		return new NetzMapView(kantenMapViews, knoten);
	}

	public NetzMapView findNetzAusschnittNurKnoten(Envelope sichtbereich, Set<NetzklasseFilter> netzklasseFilter) {
		Set<KanteMapView> kantenMapViews = new HashSet<>();
		List<Knoten> knoten = knotenRepository.getKnotenInBereichNachNetzklassen(sichtbereich, netzklasseFilter);

		return new NetzMapView(kantenMapViews, knoten);
	}

	public Set<GeometrienVerlaufMapView> findGeometrienVerlaufInAusschnitt(Envelope sichtbereich,
		Set<NetzklasseFilter> netzklasseFilter) {
		return kantenAbfrageRepository.getGeometrienVerlaufMapViewInBereich(sichtbereich, netzklasseFilter);
	}

	public Iterable<Netzfehler> findNetzfehlerInAusschnitt(Envelope sichtbereich) {
		return netzfehlerRepository.getNetzfehlerInBereich(sichtbereich);
	}

	public Iterable<Netzfehler> findNetzfehlerInAusschnitt(Envelope sichtbereich, List<NetzfehlerTyp> netzfehlerTypen) {
		return netzfehlerRepository.getNetzfehlerInBereich(sichtbereich, netzfehlerTypen);
	}

	public Set<KanteGeschwindigkeitAttributeView> findKanteGeschwindigkeitAttributeViewInAuschnitt(
		Envelope sichtbereich,
		Set<NetzklasseFilter> netzklasseFilter, boolean showDLM) {
		return kantenAbfrageRepository
			.getKanteGeschwindigkeitAttributeViewInBereichNachNetzklasse(sichtbereich, netzklasseFilter, showDLM);
	}

	public Set<KanteFuehrungsformAttributeView> findKanteFuehrungsformAttributeViewInAuschnitt(Envelope sichtbereich,
		Set<NetzklasseFilter> netzklasseFilter, boolean showDLM) {
		return kantenAbfrageRepository
			.getKanteFuehrungsformAttributeViewInBereichNachNetzklasse(sichtbereich, netzklasseFilter, showDLM);
	}

	public Set<KanteZustaendigkeitAttributeView> findKanteZustaendigkeitAttributeViewInAuschnitt(Envelope sichtbereich,
		Set<NetzklasseFilter> netzklasseFilter, boolean showDLM) {
		return kantenAbfrageRepository
			.getKanteZustaendigkeitAttributeViewInBereichNachNetzklasse(sichtbereich, netzklasseFilter, showDLM);
	}

	public boolean hasCachedNetzMapView() {
		return radNETZNetzViewCacheRepository.hasCache();
	}

	public NetzMapView getCachedNetzMapView() {
		return radNETZNetzViewCacheRepository.getCache();
	}

	public List<NetzklassenStreckenSignaturView> getNetzklassenSignaturView() {
		return netzklassenStreckenSignaturViewRepository.getCache();
	}
}
