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

package de.wps.radvis.backend.application.schnittstelle;

import java.io.IOException;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.io.jackson.GeometryDeserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;

/**
 * Der GeometryDeserializer von spatial4j setzt leider keine SRID, daher wird diese Funktionalität hier hinzugefügt
 */
public class SridGeometryDeserializer extends GeometryDeserializer {

	@Override
	public Geometry deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
		Geometry geometry = super.deserialize(jp, ctxt);
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createGeometry(geometry);
	}

}