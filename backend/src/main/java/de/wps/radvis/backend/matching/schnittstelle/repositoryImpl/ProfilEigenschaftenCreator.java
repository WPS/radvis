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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import com.graphhopper.util.FetchMode;
import com.graphhopper.util.details.PathDetail;

import de.wps.radvis.backend.common.domain.valueObject.FractionIndexedLine;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.netz.domain.valueObject.SeitenbezogeneProfilEigenschaften;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProfilEigenschaftenCreator {
	public static Collector<LinearReferenzierteProfilEigenschaften, ?, List<LinearReferenzierteProfilEigenschaften>> fasseAbschnitteMitGleichenEigenschaftenZusammen() {
		return Collectors.reducing(
			new ArrayList<>(),
			a -> {
				List<LinearReferenzierteProfilEigenschaften> list = new ArrayList<>();
				list.add(a);
				return list;
			},
			(a, b) -> {
				if (a.isEmpty()) {
					return b;
				}
				if (a.get(a.size() - 1).getProfilEigenschaften().equals(b.get(0).getProfilEigenschaften())) {
					LinearReferenzierteProfilEigenschaften last = a.remove(a.size() - 1);
					a.add(new LinearReferenzierteProfilEigenschaften(last.getProfilEigenschaften(),
						LinearReferenzierterAbschnitt.of(last.getLinearReferenzierterAbschnitt().getVonValue(),
							b.get(0).getLinearReferenzierterAbschnitt().getBisValue())));
					return a;
				} else {
					a.addAll(b);
					return a;
				}
			});
	}

	/**
	 * @param gematchedteGeometrie
	 * 	in WGS84
	 * 	WICHTIG: Graphhopper arbeitet mit Lat/Lon vertauscht vs unser System
	 * 	(siehe GraphhopperRoutingRepository.extrahiereLinestring - tauscht Koordinaten um) und hier arbeiten wir mit
	 * 	dem Graphhopper Model - entsprechend muss auch der LineString aussehen
	 * @param fractionIndexedLine
	 * 	sollte gematchedteGeometrie enthalten - wird nur aus performance Gründen übergeben
	 */
	public static LinearReferenzierteProfilEigenschaften createLinearReferenzierteProfilEigenschaften(
		DlmMatchedGraphHopper graphHopper, LineString gematchedteGeometrie, FractionIndexedLine fractionIndexedLine,
		PathDetail pathDetail) {
		Integer edgeId = (Integer) pathDetail.getValue();
		SeitenbezogeneProfilEigenschaften
			seitenbezogeneProfilEigenschaften = graphHopper.getGraphHopperEdgesAufProfilEigenschaften()
			.get(edgeId);
		Coordinate coordinateVon = gematchedteGeometrie.getCoordinates()[pathDetail.getFirst()];
		double von = fractionIndexedLine.getFractionAtIndex(pathDetail.getFirst());
		Coordinate coordinateBis = gematchedteGeometrie.getCoordinates()[pathDetail.getLast()];
		double bis = fractionIndexedLine.getFractionAtIndex(pathDetail.getLast());

		if (von == bis) {
			return null;
		}

		LineString lineString = graphHopper.getGraphHopperStorage()
			.getEdgeIteratorState(edgeId, Integer.MIN_VALUE).fetchWayGeometry(FetchMode.ALL)
			.toLineString(false);

		boolean inOrder =
			lineString.getStartPoint().getCoordinate().equals(coordinateVon) || lineString.getEndPoint()
				.getCoordinate().equals(coordinateBis);

		return new LinearReferenzierteProfilEigenschaften(FahrradrouteProfilEigenschaften.of(
			inOrder ?
				seitenbezogeneProfilEigenschaften.getBelagArtRechts() :
				seitenbezogeneProfilEigenschaften.getBelagArtLinks(),
			inOrder ?
				seitenbezogeneProfilEigenschaften.getRadverkehrsfuehrungRechts() :
				seitenbezogeneProfilEigenschaften.getRadverkehrsfuehrungLinks()),
			LinearReferenzierterAbschnitt.of(von, bis));
	}

}
