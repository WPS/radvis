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

package de.wps.radvis.backend.netz.domain.valueObject;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class OsmWayIdWithLRJsonSerializer extends JsonSerializer<LinearReferenzierteOsmWayId> {

	@Override
	public void serialize(LinearReferenzierteOsmWayId valueObject, JsonGenerator jsonGenerator,
		SerializerProvider serializers)
		throws IOException {
		jsonGenerator.writeStartObject();
		jsonGenerator.writeNumberField("i", valueObject.getValue());
		jsonGenerator.writeNumberField("v", valueObject.getLinearReferenzierterAbschnitt().getVonValue());
		jsonGenerator.writeNumberField("b", valueObject.getLinearReferenzierterAbschnitt().getBisValue());
		jsonGenerator.writeEndObject();
	}
}
