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

package de.wps.radvis.backend.common.domain;

public class FrontendLinks {

	public static String fahrradrouteDetailView(Long id) {
		return String.format("/viewer/fahrradrouten/%d", id);
	}

	public static String massnahmeDetailView(Long id) {
		return String.format("/viewer/massnahmen/%d", id);
	}

	public static String barriereDetailView(Long id) {
		return String.format("/viewer/barriere/%d", id);
	}

	public static String furtKreuzungDetailView(Long id) {
		return String.format("/viewer/furten-kreuzungen/%d", id);
	}

	public static String infrastrukturTabelleWithFilter(String infrastruktur, String filter) {
		return String.format("/viewer?infrastrukturen=%s&tabellenVisible=true&filter_massnahmen=%s", infrastruktur,
			filter);
	}

	public static String kanteDetailView(Long id) {
		return String.format("/viewer/kante/%d", id);
	}

	public static String knotenDetailView(Long id) {
		return String.format("/viewer/knoten/%d", id);
	}

	public static String leihstationDetails(Long id) {
		return String.format("/viewer/leihstation/%d", id);
	}

	public static String abstellanlageDetails(Long id) {
		return String.format("/viewer/abstellanlage/%d", id);
	}

	public static String servicestationDetails(Long id) {
		return String.format("/viewer/servicestation/%d", id);
	}

	public static String benutzerAdministration(Long id) {
		return String.format("/administration/benutzer/%d", id);
	}
}
