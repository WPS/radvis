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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.netz.domain.entity.Kante;

public class PbfErstellungsTestDataProvider {
	public static Map<Envelope, Stream<Kante>> getEnvelopeToKantenStreamMap(List<List<Kante>> kanten) {
		return kanten.stream().collect(
			Collectors.toMap(
				kantenInPartition -> GeometryTestdataProvider.createMultiLineString(
					kantenInPartition.stream().map(Kante::getGeometry).toArray(
						LineString[]::new)).getEnvelopeInternal(),
				Collection::stream));
	}
}
