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

package de.wps.radvis.backend.fahrradroute.domain;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import de.wps.radvis.backend.common.domain.service.ExporterService;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.fahrradroute.domain.dbView.FahrradrouteListenDbView;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteViewRepository;

public class FahrradroutenExporterService implements ExporterService {

	private FahrradrouteViewRepository fahrradrouteViewRepository;

	public FahrradroutenExporterService(FahrradrouteViewRepository fahrradrouteViewRepository) {
		this.fahrradrouteViewRepository = fahrradrouteViewRepository;
	}

	@Override
	public List<ExportData> export(List<Long> ids) {
		GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
		return fahrradrouteViewRepository.findAllByIdIn(ids).stream()
			.map(fahrradroute -> {
				Geometry geometry = fahrradroute.getGeometry() != null ?
					fahrradroute.getGeometry() :
					geometryFactory.createMultiLineString();
				return new ExportData(geometry, convertProperties(fahrradroute));
			}).collect(Collectors.toList());
	}

	@Override
	public String getDateinamenPrefix() {
		return "fahrradrouten";
	}

	private Map<String, String> convertProperties(FahrradrouteListenDbView fahrradroute) {
		Map<String, String> result = new HashMap<>();

		result.put("VerantwortlicheOrganisation", fahrradroute.getVerantwortlicheOrganisationName());
		result.put("Typ", fahrradroute.getFahrradrouteTyp().toString());
		result.put("Name", fahrradroute.getName().getName());
		result.put("Kategorie", fahrradroute.getKategorie().toString());
		result.put("Abstieg", String.format(Locale.GERMANY, "%.2f", fahrradroute.getAbstieg()));
		result.put("Anstieg", String.format(Locale.GERMANY, "%.2f", fahrradroute.getAnstieg()));
		result.put("Kurzbeschreibung", fahrradroute.getKurzbeschreibung());
		result.put("Homepage", fahrradroute.getHomepage());
		result.put("Lizenz", fahrradroute.getLizenz());
		result.put("Lizenz Namensnennung", fahrradroute.getLizenzNamensnennung());
		result.put("Zeitpunkt der letzten Änderung",
			fahrradroute.getZuletztBearbeitet().format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")));

		return result;
	}
}
