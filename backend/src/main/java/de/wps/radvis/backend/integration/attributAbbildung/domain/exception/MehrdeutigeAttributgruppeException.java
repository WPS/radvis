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

package de.wps.radvis.backend.integration.attributAbbildung.domain.exception;

import java.util.Collection;
import java.util.stream.Collectors;

public class MehrdeutigeAttributgruppeException extends MehrdeutigeProjektionException {
	private static final long serialVersionUID = -5154166308761927064L;

	public MehrdeutigeAttributgruppeException(Collection<Double> anteile, String attributGruppenName) {
		super(String.format(
			"Die zu projizierenden %s sind mehrdeutig. Verteilung der Anteile in Prozent: %s.",
			attributGruppenName,
			anteile.stream().map(anteil -> Math.round(anteil * 1000.) / 10.).collect(Collectors.toList())));
	}
}
