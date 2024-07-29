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
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;

public class CustomGeometryAsGeoJsonSerializer extends GeometryAsGeoJSONSerializer {
	@Override
	public void serialize(Geometry geom, JsonGenerator gen, SerializerProvider serializers)
		throws IOException, JsonProcessingException {
		// Die Lib hat einen Bug beim Schreiben von Multipoints, der Fix im Repo ist allerdings nicht auf mvn verfügbar,
		// daher fixen wir hier von Hand:
		// https://github.com/locationtech/spatial4j/blob/master/CHANGES.md -> Pullrequest #210
		if (geom instanceof MultiPoint) {
			gen.writeStartObject();
			gen.writeFieldName("type");
			gen.writeString(geom.getClass().getSimpleName());
			MultiPoint v = (MultiPoint) geom;
			gen.writeFieldName("coordinates");
			write(gen, v.getCoordinates());
			gen.writeEndObject();
		} else {
			super.serialize(geom, gen, serializers);
		}
	}
}
