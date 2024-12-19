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

package de.wps.radvis.backend.netz.domain.entity.provider;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Knoten;

public class KnotenTestDataProvider {

	private static GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	public static Knoten.KnotenBuilder withCoordinateAndQuelle(Coordinate coordinate, QuellSystem quelle) {
		return Knoten.builder().point(geometryFactory.createPoint(coordinate)).quelle(quelle);
	}

	public static Knoten.KnotenBuilder withPosition(double x, double y) {
		return Knoten.builder().point(geometryFactory.createPoint(new Coordinate(x, y))).quelle(QuellSystem.DLM);
	}

	public static Knoten.KnotenBuilder withDefaultValues() {
		return Knoten.builder().point(geometryFactory.createPoint(new Coordinate(0, 0))).quelle(QuellSystem.LGL);
	}
}
