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

package de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.service;

import de.wps.radvis.backend.barriere.domain.BarriereService;
import de.wps.radvis.backend.common.domain.service.FehlerprotokollService;
import de.wps.radvis.backend.common.domain.valueObject.FehlerprotokollTyp;
import de.wps.radvis.backend.fahrradroute.domain.FahrradrouteService;
import de.wps.radvis.backend.furtKreuzung.domain.FurtKreuzungService;
import de.wps.radvis.backend.integration.dlm.domain.AttributlueckenService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.matching.domain.service.OsmAbbildungsFehlerService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FehlerprotokollServiceFactory {

	private final MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;
	private final AttributlueckenService attributlueckenService;
	private final FahrradrouteService fahrradrouteService;
	private final BarriereService barriereService;
	private final FurtKreuzungService furtKreuzungService;
	private final OsmAbbildungsFehlerService osmAbbildungsFehlerService;

	public FehlerprotokollServiceFactory(
		MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService,
		AttributlueckenService attributlueckenService, FahrradrouteService fahrradrouteService,
		BarriereService barriereService, FurtKreuzungService furtKreuzungService,
		OsmAbbildungsFehlerService osmAbbildungsFehlerService) {
		this.massnahmeNetzbezugAenderungProtokollierungsService = massnahmeNetzbezugAenderungProtokollierungsService;
		this.attributlueckenService = attributlueckenService;
		this.fahrradrouteService = fahrradrouteService;
		this.barriereService = barriereService;
		this.furtKreuzungService = furtKreuzungService;
		this.osmAbbildungsFehlerService = osmAbbildungsFehlerService;
	}

	public FehlerprotokollService getFehlerprotokollProvider(FehlerprotokollTyp fehlerprotokollTyp) {
		switch (fehlerprotokollTyp) {
		case DLM_REIMPORT_JOB_MASSNAHMEN:
			return massnahmeNetzbezugAenderungProtokollierungsService;
		case DLM_REIMPORT_JOB_BARRIEREN:
			return barriereService;
		case DLM_REIMPORT_JOB_FURTEN_KREUZUNGEN:
			return furtKreuzungService;
		case ATTRIBUTLUECKEN_SCHLIESSEN:
			return attributlueckenService;
		case TOUBIZ_IMPORT_FAHRRADROUTEN:
		case TFIS_IMPORT_LRFW:
		case DLM_REIMPORT_JOB_FAHRRADROUTEN:
		case TFIS_IMPORT_FAHRRADROUTEN:
			return fahrradrouteService;
		case OSM_ABBILDUNG_RADNETZ:
		case OSM_ABBILDUNG_KREISNETZ:
		case OSM_ABBILDUNG_KOMMUNALNETZ:
		case OSM_ABBILDUNG_SONSTIGE:
			return osmAbbildungsFehlerService;
		default:
			log.warn("Es ist kein FehlerprotokollService für {} implementiert.", fehlerprotokollTyp);
			return null;
		}
	}
}
