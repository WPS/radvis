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

public class GeometrieTypNichtUnterstuetztException extends Exception {
	private static final long serialVersionUID = 2373574234896165654L;

	@Getter
	private final Geometry geometry;

	public GeometrieTypNichtUnterstuetztException(String technischeId, Geometry geometry) {
		super("Der Geometrietyp " + geometry.getGeometryType() + " des Features mit technischer ID " + technischeId
			+ " wird nicht unterstützt");
		this.geometry = geometry;
	}
}
