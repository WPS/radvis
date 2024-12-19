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

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;

import de.wps.radvis.backend.common.domain.service.ExporterService;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.massnahme.domain.dbView.MassnahmeListenDbView;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class MassnahmenExporterService implements ExporterService {

	private MassnahmeViewRepository massnahmeRepository;

	public MassnahmenExporterService(MassnahmeViewRepository massnahmeRepository) {
		this.massnahmeRepository = massnahmeRepository;
	}

	@Override
	public List<ExportData> export(List<Long> ids) {
		GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
		return massnahmeRepository.findAllByIdIn(ids).stream()
			.map(massnahme -> {
				GeometryCollection geometry = massnahme.getGeometry() != null ? massnahme.getGeometry()
					: geometryFactory.createGeometryCollection();
				return new ExportData(geometry, convertProperties(massnahme));
			}).collect(Collectors.toList());
	}

	@Override
	public String getDateinamenPrefix() {
		return "massnahmen";
	}

	private Map<String, String> convertProperties(MassnahmeListenDbView massnahme) {
		Map<String, String> result = new HashMap<>();

		result.put("RADVIS_ID", massnahme.getId().toString());
		result.put("Bezeichnung", massnahme.getBezeichnung().getValue());
		result.put("Kategorien", massnahme.getMasshnahmenkategorienString());
		result.put("Durchführungszeitraum",
			massnahme.getDurchfuehrungszeitraum() == null ? ""
				: massnahme.getDurchfuehrungszeitraum().getGeplanterUmsetzungsstartJahr() == null ? ""
				: massnahme.getDurchfuehrungszeitraum().getGeplanterUmsetzungsstartJahr().toString());
		result.put("Umsetzungsstatus", massnahme.getUmsetzungsstatus().toString());
		result.put("Umsetzungsstand-Status",
			massnahme.getUmsetzungsstandStatus() == null ? "" : massnahme.getUmsetzungsstandStatus().toString());
		result.put("Veröffentlicht",
			massnahme.getVeroeffentlicht() != null && massnahme.getVeroeffentlicht() ? "Ja" : "Nein");
		result.put("Planung erforderlich", massnahme.getPlanungErforderlich() ? "Ja" : "Nein");
		result.put("Priorität",
			massnahme.getPrioritaet() == null ? "" : String.valueOf(massnahme.getPrioritaet().getValue()));
		result.put("Netzklassen", massnahme.getNetzklassenString());
		result.put("Baulastträger",
			Verwaltungseinheit.combineNameAndArt(massnahme.getBaulastName(), massnahme.getBaulastOrganisationsArt()));
		result.put("Zuständige/r",
			Verwaltungseinheit.combineNameAndArt(massnahme.getZustaendigName(),
				massnahme.getZustaendigOrganisationsArt()));
		result.put("Unterhaltszuständige/r",
			Verwaltungseinheit.combineNameAndArt(massnahme.getUnterhaltName(),
				massnahme.getUnterhaltOrganisationsArt()));
		result.put("Letzte Änderung",
			massnahme.getLetzteAenderung().format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")));
		result.put("Benutzer/in der letzten Änderung",
			massnahme.getBenutzerLetzteAenderungVorname() + " " + massnahme.getBenutzerLetzteAenderungNachname());
		result.put("Soll-Standard", massnahme.getSollStandard().toString());
		result.put("Wer soll tätig werden?", massnahme.getHandlungsverantwortlicher() == null ? ""
			: massnahme.getHandlungsverantwortlicher().toString());
		result.put("Massnahme-ID", massnahme.getMassnahmeKonzeptId() == null ? ""
			: massnahme.getMassnahmeKonzeptId().toString());
		result.put("Realisierungshilfe", massnahme.getRealisierungshilfe() == null ? ""
			: massnahme.getRealisierungshilfe().toString());
		result.put("Kostenannahme",
			massnahme.getKostenannahme() == null ? "" : massnahme.getKostenannahme().getKostenannahme().toString());
		result.put("MaViS-ID", massnahme.getMaViSID() == null ? "" : massnahme.getMaViSID().toString());
		result.put("Verba-ID", massnahme.getVerbaID() == null ? "" : massnahme.getVerbaID().toString());
		result.put("LGVFG-ID", massnahme.getLgvfgID() == null ? "" : massnahme.getLgvfgID().toString());
		result.put("Quelle", massnahme.getKonzeptionsquelle().toString());
		result.put("Archiviert", massnahme.isArchiviert() ? "Ja" : "Nein");
		return result;
	}
}
