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

package de.wps.radvis.backend.fahrradroute.domain.valueObject;

import java.util.Optional;

import jakarta.persistence.Embeddable;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@Embeddable
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FahrradroutenMatchingAndRoutingInformation {
	private Geometry routedOrMatchedGeometry;
	private MultiPoint kehrtwenden;
	private MultiLineString abweichendeSegmente;
	private Boolean abbildungDurchRouting;

	// IntelliJ zeigt hier einen error an, jedoch compiliert und funktioniert es so
	// Fehler ist bekannt: https://youtrack.jetbrains.com/issue/IDEA-240844
	public Optional<Geometry> getRoutedOrMatchedGeometry() {
		return Optional.ofNullable(routedOrMatchedGeometry);
	}

	public Optional<MultiPoint> getKehrtwenden() {
		return Optional.ofNullable(kehrtwenden);
	}

	public Optional<MultiLineString> getAbweichendeSegmente() {
		return Optional.ofNullable(abweichendeSegmente);
	}

	public Optional<Boolean> getAbbildungDurchRouting() {
		return Optional.ofNullable(abbildungDurchRouting);
	}
}
