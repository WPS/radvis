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

import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

public abstract class MehrdeutigeProjektionException extends Exception {
	private static final long serialVersionUID = 4957589503353176711L;
	@Getter
	@Setter
	private Long grundnetzKantenID;
	@Getter
	@Setter
	private Geometry grundnetzKanteGeometry;
	@Getter
	@Setter
	private List<Long> projizierteKanteIds;

	@Setter
	protected String seite = "";

	public MehrdeutigeProjektionException(String message) {
		super(message);
	}

	@Override
	public String getMessage() {
		String seitenbezugInfo = !seite.isEmpty() ?
			"Auf der Seite " + seite + " der Kante ist ein Problem aufgetreten: " : "";

		String additionalInfo = grundnetzKantenID != null && projizierteKanteIds != null ?
			String.format(" Es wurden die Attribute der Kanten mit IDs %s auf die GrundnetzKante mit ID %s projiziert.",
				projizierteKanteIds.stream().map(Object::toString).collect(
					Collectors.joining(",")), grundnetzKantenID) :
			"";
		return super.getMessage() + seitenbezugInfo + additionalInfo;
	}
}
