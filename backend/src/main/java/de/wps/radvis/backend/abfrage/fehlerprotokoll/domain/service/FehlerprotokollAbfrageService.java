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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.valueObject.FehlerprotokollTyp;

public class FehlerprotokollAbfrageService {

	private final FehlerprotokollServiceFactory fehlerprotokollServiceFactory;

	public FehlerprotokollAbfrageService(FehlerprotokollServiceFactory fehlerprotokollServiceFactory) {
		this.fehlerprotokollServiceFactory = fehlerprotokollServiceFactory;
	}

	public List<FehlerprotokollEintrag> getAlleFehlerprotokolle() {
		return getAlleFehlerprotokolleFuerTypen(Arrays.asList(FehlerprotokollTyp.values()));
	}

	public List<FehlerprotokollEintrag> getAlleFehlerprotokolleFuerTypen(
		List<FehlerprotokollTyp> fehlerprotokollTypen) {

		return fehlerprotokollTypen.stream()
			.flatMap(selectedTyp -> fehlerprotokollServiceFactory.getFehlerprotokollProvider(selectedTyp)
				.getAktuelleFehlerprotokolle(selectedTyp).stream())
			.collect(Collectors.toList());
	}

	public List<FehlerprotokollEintrag> getAlleFehlerprotokolleFuerTypenInBereich(
		List<FehlerprotokollTyp> fehlerprotokollTypen, Envelope sichtbereich
	) {

		return fehlerprotokollTypen.stream()
			.flatMap(selectedTyp -> fehlerprotokollServiceFactory.getFehlerprotokollProvider(selectedTyp)
				.getAktuelleFehlerprotokolleInBereich(selectedTyp, sichtbereich).stream())
			.collect(Collectors.toList());
	}
}
