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

package de.wps.radvis.backend.barriere.domain;

import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.entity.BarriereNetzBezugAenderung;
import de.wps.radvis.backend.barriere.domain.repository.BarriereNetzBezugAenderungRepository;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.service.FehlerprotokollService;
import de.wps.radvis.backend.common.domain.valueObject.FehlerprotokollTyp;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.service.AbstractEntityWithNetzbezugService;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BarriereService extends AbstractEntityWithNetzbezugService<Barriere> implements FehlerprotokollService {
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final BarriereRepository repository;
	private BarriereNetzBezugAenderungRepository netzBezugAenderungRepository;
	private BenutzerService benutzerService;

	public BarriereService(BarriereRepository repository, VerwaltungseinheitService verwaltungseinheitService,
		NetzService netzService, double erlaubteAbweichungKantenRematch,
		BarriereNetzBezugAenderungRepository netzBezugAenderungRepository, BenutzerService benutzerService) {
		super(repository, netzService, erlaubteAbweichungKantenRematch);
		this.repository = repository;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.netzBezugAenderungRepository = netzBezugAenderungRepository;
		this.benutzerService = benutzerService;
	}

	public boolean darfNutzerBearbeiten(Benutzer benutzer, Barriere barriere) {
		if (benutzer.hatRecht(Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			return verwaltungseinheitService.istUebergeordnet(benutzer.getOrganisation(),
				barriere.getVerantwortlich());
		}
		return false;
	}

	@Override
	protected void protokolliereNetzBezugAenderungFuerGeloeschteKnoten(List<Barriere> entitiesWithKnotenInNetzbezug,
		Knoten knoten, LocalDateTime datum, NetzAenderungAusloeser ausloeser) {
		log.debug("Erstelle Protokoll-Einträge für Löschung von Knoten {} in Barrieren {}", knoten.getId(),
			entitiesWithKnotenInNetzbezug.stream().map(e -> e.getId()).toList());
		entitiesWithKnotenInNetzbezug.stream()
			.forEach(e -> netzBezugAenderungRepository
				.save(new BarriereNetzBezugAenderung(NetzBezugAenderungsArt.KNOTEN_GELOESCHT, knoten.getId(), e,
					benutzerService.getTechnischerBenutzer(), datum, ausloeser, e.getNetzbezug().getGeometrie())));

	}

	@Override
	protected Collection<? extends Barriere> findByKnotenInNetzbezug(List<Long> knotenIds) {
		return repository.findByKnotenInNetzBezug(knotenIds);
	}

	@Override
	protected void protokolliereNetzBezugAenderungFuerGeloeschteKanten(List<Barriere> entitiesWithKanteInNetzbezug,
		Long kanteId, Geometry geometry, LocalDateTime datum, NetzAenderungAusloeser ausloeser) {
		log.debug("Erstelle Protokoll-Einträge für Löschung von Kante {} in Barrieren {}", kanteId,
			entitiesWithKanteInNetzbezug.stream().map(e -> e.getId()).toList());
		entitiesWithKanteInNetzbezug.stream()
			.forEach(e -> netzBezugAenderungRepository
				.save(new BarriereNetzBezugAenderung(NetzBezugAenderungsArt.KANTE_GELOESCHT, kanteId, e,
					benutzerService.getTechnischerBenutzer(), datum, ausloeser, e.getNetzbezug().getGeometrie())));
	}

	@Override
	protected List<Barriere> findByKantenIdsInNetzBezug(Collection<Long> ids) {
		return repository.findByKantenInNetzBezug(ids);
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolle(FehlerprotokollTyp fehlerprotokollTyp) {
		require(fehlerprotokollTyp == FehlerprotokollTyp.DLM_REIMPORT_JOB_BARRIEREN);
		return netzBezugAenderungRepository.findBarriereNetzBezugAenderungByDatumAfter(LocalDateTime.now().minusDays(
			1));
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolleInBereich(
		FehlerprotokollTyp fehlerprotokollTyp, Envelope bereich) {
		require(fehlerprotokollTyp == FehlerprotokollTyp.DLM_REIMPORT_JOB_BARRIEREN);
		return netzBezugAenderungRepository.findBarriereNetzBezugAenderungByDatumAfterInBereich(
			LocalDateTime.now().minusDays(1),
			EnvelopeAdapter.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
	}
}
