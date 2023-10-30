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

package de.wps.radvis.backend.integration.netzbildung.domain.exception;

import org.locationtech.jts.geom.Geometry;

import lombok.Getter;

public class AttributNichtImportiertException extends Exception {
	private static final long serialVersionUID = 7006557768290764246L;

	@Getter
	private Geometry geometry;

	public AttributNichtImportiertException(Geometry geometry, String details) {
		super("Von diesem feature konnte ein Attribut nicht importiert werden. Details: " + details);
		this.geometry = geometry;
	}
}
