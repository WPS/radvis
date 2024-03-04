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

package de.wps.radvis.backend.common.domain.valueObject;

import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ExportData {
	@NonNull
	private final Geometry geometry;
	@NonNull
	private final Map<String, String> properties;
	@NonNull
	private final List<String> headers;

	public ExportData(@NonNull Geometry geometry, @NonNull Map<String, String> properties) {
		this(geometry, properties, properties.keySet().stream().collect(Collectors.toList()));
	}

	/**
	 * Über das Feld Headers kann die Reihenfolge beim Rausschreiben gesteuert werden, falls relevant
	 *
	 * @param geometry
	 * @param properties
	 * @param headers
	 */
	public ExportData(@NonNull Geometry geometry, @NonNull Map<String, String> properties,
		@NonNull List<String> headers) {
		require(properties.keySet().containsAll(headers));
		this.geometry = geometry;
		this.properties = properties;
		this.headers = headers;
	}
}
