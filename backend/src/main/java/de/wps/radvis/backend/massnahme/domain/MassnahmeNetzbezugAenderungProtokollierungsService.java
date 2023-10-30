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

package de.wps.radvis.backend.massnahme.domain;

import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.service.FehlerprotokollService;
import de.wps.radvis.backend.common.domain.valueObject.FehlerprotokollTyp;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezugAenderung;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeNetzBezugAenderungRepository;
import de.wps.radvis.backend.netz.domain.entity.NetzBezugAenderung;
import de.wps.radvis.backend.netz.domain.event.KanteDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KanteTopologieChangedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;

public class MassnahmeNetzbezugAenderungProtokollierungsService implements FehlerprotokollService {

	private final BenutzerService benutzerService;

	private final MassnahmeNetzBezugAenderungRepository massnahmeNetzBezugAenderungRepository;

	public MassnahmeNetzbezugAenderungProtokollierungsService(BenutzerService benutzerService,
		MassnahmeNetzBezugAenderungRepository massnahmeNetzBezugAenderungRepository) {
		this.benutzerService = benutzerService;
		this.massnahmeNetzBezugAenderungRepository = massnahmeNetzBezugAenderungRepository;
	}

	public void protokolliereNetzBezugAenderungFuerVeraenderteKante(
		List<Massnahme> massnahmen,
		KanteTopologieChangedEvent event) {
		protokolliereNetzbezugAenderung(massnahmen,
			NetzBezugAenderungsArt.KANTE_VERAENDERT,
			event.getKanteId(),
			event.getDatum(),
			event.getAusloeser(),
			event.getGeometry());
	}

	public void protokolliereNetzBezugAenderungFuerGeloeschteKante(
		List<Massnahme> massnahmen,
		KanteDeletedEvent event) {
		protokolliereNetzbezugAenderung(massnahmen,
			NetzBezugAenderungsArt.KANTE_GELOESCHT,
			event.getKanteId(),
			event.getDatum(),
			event.getAusloeser(),
			event.getGeometry());
	}

	public void protokolliereNetzBezugAenderungFuerGeloeschteKnoten(
		List<Massnahme> massnahmen,
		KnotenDeletedEvent event) {
		protokolliereNetzbezugAenderung(massnahmen,
			NetzBezugAenderungsArt.KNOTEN_GELOESCHT,
			event.getKnotenId(),
			event.getDatum(),
			event.getAusloeser(),
			event.getGeometry());
	}

	private void protokolliereNetzbezugAenderung(List<Massnahme> massnahmen,
		NetzBezugAenderungsArt netzBezugAenderungsArt,
		Long kantenKnotenId,
		LocalDateTime datum,
		NetzAenderungAusloeser ausloeser,
		Geometry geometry) {
		List<MassnahmeNetzBezugAenderung> massnahmeNetzBezugAenderungen = massnahmen.stream()
			.map(massnahme -> new MassnahmeNetzBezugAenderung(
				netzBezugAenderungsArt,
				kantenKnotenId,
				massnahme,
				benutzerService.getTechnischerBenutzer(),
				datum,
				ausloeser,
				geometry))
			.collect(Collectors.toList());
		massnahmeNetzBezugAenderungRepository.saveAll(massnahmeNetzBezugAenderungen);
	}

	public List<MassnahmeNetzBezugAenderung> getAllMassnahmeNetzBezugAenderungenAfter(LocalDateTime from) {
		return massnahmeNetzBezugAenderungRepository.findMassnahmeNetzBezugAenderungByDatumAfter(from);
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolle(FehlerprotokollTyp typ) {
		require(typ == FehlerprotokollTyp.DLM_REIMPORT_JOB_MASSNAHMEN);
		return getAllMassnahmeNetzBezugAenderungenAfter(
			LocalDateTime.now().minusDays(1));
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolleInBereich(FehlerprotokollTyp typ,
		Envelope bereich) {
		require(typ == FehlerprotokollTyp.DLM_REIMPORT_JOB_MASSNAHMEN);
		return massnahmeNetzBezugAenderungRepository.findMassnahmeNetzBezugAenderungByDatumAfterInBereich(
			LocalDateTime.now().minusDays(1),
			EnvelopeAdapter.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
	}
}
