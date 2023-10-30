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

import org.opengis.feature.simple.SimpleFeature;

public class MassnahmenPaketIdExtractor {
	public static String getMassnahmenPaketId(SimpleFeature simpleFeature) {
		Object massn_p = simpleFeature.getAttribute("MASSN_P");
		return massn_p == null ? null : normalize(massn_p.toString());
	}

	public static String normalize(String value) {
		return value
			.toUpperCase()
			.replace('_', ' ')
			.trim();
	}
}