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

package de.wps.radvis.backend.application;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer;
import org.locationtech.spatial4j.io.jackson.ShapeAsGeoJSONSerializer;
import org.locationtech.spatial4j.io.jackson.ShapeDeserializer;
import org.locationtech.spatial4j.shape.Shape;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.wps.radvis.backend.application.schnittstelle.SridGeometryDeserializer;

@Configuration
public class JacksonConfiguration {

	// Registriere Serializer und Deserializer für Geometry und Shape
	@Bean
	public Module customJacksonGeometryModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Geometry.class, new SridGeometryDeserializer());
		module.addDeserializer(Shape.class, new ShapeDeserializer());
		module.addSerializer(Geometry.class, new GeometryAsGeoJSONSerializer());
		module.addSerializer(Shape.class, new ShapeAsGeoJSONSerializer());
		return module;
	}

}
