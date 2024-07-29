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

package de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzausschnittService;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.GeometrienVerlaufMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzNetzklasseMapView;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.common.schnittstelle.RadvisViewController;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingService;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.NonNull;

@RestController
@RequestMapping("/api/netzausschnitt")
@RadvisViewController
public class NetzausschnittController {

	@Autowired
	private final NetzToGeoJsonConverter netzToGeoJsonConverter;
	private final NetzService netzService;
	private final NetzausschnittService netzausschnittService;
	private final KantenMappingService kantenMappingService;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final NetzausschnittGuard netzausschnittGuard;

	public NetzausschnittController(
		@NonNull NetzToGeoJsonConverter netzToGeoJsonConverter,
		@NonNull NetzService netzService,
		@NonNull NetzausschnittService netzausschnittService,
		@NonNull KantenMappingService kantenMappingService,
		@NonNull VerwaltungseinheitResolver verwaltungseinheitResolver,
		@NonNull NetzausschnittGuard netzausschnittGuard
	) {
		super();
		this.netzToGeoJsonConverter = netzToGeoJsonConverter;
		this.netzausschnittService = netzausschnittService;
		this.netzService = netzService;
		this.kantenMappingService = kantenMappingService;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.netzausschnittGuard = netzausschnittGuard;
	}

	@GetMapping("quelle/{quelle}")
	public FeatureCollection getGeoJson(@ModelAttribute("sichtbereich") Envelope sichtbereich,
		@PathVariable QuellSystem quelle) {
		NetzMapView netzAusschnitt = this.netzausschnittService.findNetzAusschnitt(sichtbereich, quelle);
		return netzToGeoJsonConverter.convertNetzAusschnitt(netzAusschnitt);
	}

	@GetMapping("kanten")
	public FeatureCollection getKantenGeoJson(@ModelAttribute("sichtbereich") Envelope sichtbereich,
		@RequestParam("netzklasseFilter") Set<NetzklasseFilter> netzklasseFilterQueryParams,
		@RequestParam boolean mitVerlauf) {
		NetzMapView netzAusschnitt = this.netzausschnittService.findNetzAusschnitt(sichtbereich,
			netzklasseFilterQueryParams);
		return netzToGeoJsonConverter.convertNetzAusschnitt(netzAusschnitt, mitVerlauf);
	}

	@GetMapping("kantenDLM")
	public FeatureCollection getKantenGeoJsonDLM(@ModelAttribute("sichtbereich") Envelope sichtbereich) {
		NetzNetzklasseMapView netzAusschnitt = this.netzausschnittService.findNetzAusschnittDLM(sichtbereich);
		Map<Long, List<Long>> zuordnungen = this.kantenMappingService.getZuordnungen(
			netzAusschnitt.getKanten().stream().map(KanteNetzklasseMapView::getId).collect(Collectors.toList()));
		return netzToGeoJsonConverter.convertNetzAusschnitt(netzAusschnitt, zuordnungen);
	}

	@GetMapping("kantenDLMRadNETZZugeordnet")
	public FeatureCollection getKantenGeoJsonDLMIstRadNETZZugeordnet(
		@ModelAttribute("sichtbereich") Envelope sichtbereich) {
		NetzNetzklasseMapView netzAusschnitt = this.netzausschnittService
			.findNetzAusschnittDLMIstRadNETZZugeordnet(sichtbereich);
		return netzToGeoJsonConverter.convertNetzAusschnitt(netzAusschnitt);
	}

	@GetMapping("alleRadNETZStrecken")
	public FeatureCollection getStreckenGeoJson(@RequestParam boolean mitVerlauf) {
		if (!this.netzausschnittService.hasCachedNetzMapView()) {
			return GeoJsonConverter.createFeatureCollection();
		}
		NetzMapView netzAusschnitt = this.netzausschnittService.getCachedNetzMapView();
		return netzToGeoJsonConverter.convertNetzAusschnitt(netzAusschnitt, mitVerlauf);
	}

	@GetMapping("knoten")
	public FeatureCollection getNurKnotenAsGeoJson(@ModelAttribute("sichtbereich") Envelope sichtbereich,
		@RequestParam("netzklasseFilter") Set<NetzklasseFilter> netzklasseFilterQueryParams) {
		NetzMapView netzAusschnitt = this.netzausschnittService
			.findNetzAusschnittNurKnoten(sichtbereich, netzklasseFilterQueryParams);
		return netzToGeoJsonConverter.convertNetzAusschnitt(netzAusschnitt);
	}

	@GetMapping("netzfehler")
	public FeatureCollection getNetzfehlerGeoJson(Authentication authentication,
		@ModelAttribute("sichtbereich") Envelope sichtbereich) throws AccessDeniedException {
		netzausschnittGuard.getNetzfehlerGeoJson(authentication, sichtbereich);
		Iterable<Netzfehler> netzfehler = this.netzausschnittService.findNetzfehlerInAusschnitt(sichtbereich);
		return netzToGeoJsonConverter.convertNetzfehler(netzfehler);
	}

	@GetMapping("netzfehler/{netzfehlerTypen}")
	public FeatureCollection getNetzfehlerGeoJsonFuerTyp(Authentication authentication,
		@ModelAttribute("sichtbereich") Envelope sichtbereich,
		@PathVariable List<NetzfehlerTyp> netzfehlerTypen) throws AccessDeniedException {
		netzausschnittGuard.getNetzfehlerGeoJsonFuerTyp(authentication, sichtbereich, netzfehlerTypen);
		Iterable<Netzfehler> netzfehler = this.netzausschnittService.findNetzfehlerInAusschnitt(sichtbereich,
			netzfehlerTypen);
		return netzToGeoJsonConverter.convertNetzfehler(netzfehler);
	}

	@GetMapping("verlauf")
	public FeatureCollection getKantenVerlaufGeoJson(@ModelAttribute("sichtbereich") Envelope sichtbereich,
		@RequestParam("netzklasseFilter") Set<NetzklasseFilter> netzklasseFilterQueryParams) {
		Set<GeometrienVerlaufMapView> netzAusschnitt = this.netzausschnittService
			.findGeometrienVerlaufInAusschnitt(sichtbereich, netzklasseFilterQueryParams);
		return netzToGeoJsonConverter.convertNetzAusschnittGeometrienVerlauf(netzAusschnitt);
	}

	@GetMapping("kanten-in-organisationsbereich/{orgaId}")
	public FeatureCollection getKantenInOrganisationsbereich(@PathVariable("orgaId") Long orgaId,
		@RequestParam(value = "hasNetzklasse", required = false) Netzklasse hasNetzklasse) {
		Verwaltungseinheit organisation = verwaltungseinheitResolver.resolve(orgaId);
		if (hasNetzklasse != null) {
			Set<Kante> kanten = netzService.getKantenInOrganisationsbereichEagerFetchNetzklassen(organisation);
			return netzToGeoJsonConverter.convertKanten(kanten, hasNetzklasse);
		}
		Set<Kante> kanten = netzService.getKantenInOrganisationsbereich(organisation);
		return netzToGeoJsonConverter.convertKanten(kanten);
	}
}
