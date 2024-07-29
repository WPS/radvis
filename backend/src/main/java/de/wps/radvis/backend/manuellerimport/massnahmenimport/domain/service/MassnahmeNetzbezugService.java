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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweisText;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MassnahmeNetzbezugService {

	public static final int GEOMETRIE_SUCHRADIUS_IN_METER = 30;
	private static final double KNOTEN_PREFERENCE_TOLERANZ = 2;
	private static final double MINIMALE_SEGMENT_LAENGE = 1;
	private final SimpleMatchingService simpleMatchingService;
	private final NetzService netzService;

	public MassnahmeNetzbezugService(
		SimpleMatchingService simpleMatchingService,
		NetzService netzService
	) {
		this.simpleMatchingService = simpleMatchingService;
		this.netzService = netzService;
	}

	public void bestimmeNetzbezugDerZuordnung(MassnahmenImportZuordnung zuordnung,
		MatchingStatistik matchingStatistik) {
		Optional<MassnahmeNetzBezug> netzbezug = bestimmeNetzbezugEntsprechendGeometrieTyp(
			(Geometry) zuordnung.getFeature().getDefaultGeometry(),
			matchingStatistik,
			zuordnung.getNetzbezugHinweise()
		);
		zuordnung.aktualisiereNetzbezug(netzbezug.orElse(null), false);
	}

	private Optional<MassnahmeNetzBezug> bestimmeNetzbezugEntsprechendGeometrieTyp(Geometry geometry,
		MatchingStatistik matchingStatistik, List<NetzbezugHinweis> netzbezugHinweise) {
		return switch (geometry.getGeometryType()) {
		case Geometry.TYPENAME_GEOMETRYCOLLECTION, Geometry.TYPENAME_MULTILINESTRING, Geometry.TYPENAME_MULTIPOINT -> bestimmeNetzbezug(
			(GeometryCollection) geometry, matchingStatistik, netzbezugHinweise);
		case Geometry.TYPENAME_LINESTRING -> bestimmeNetzbezug((LineString) geometry, matchingStatistik,
			netzbezugHinweise);
		case Geometry.TYPENAME_POINT -> bestimmeNetzbezug((Point) geometry, netzbezugHinweise);
		default -> {
			netzbezugHinweise.add(NetzbezugHinweis.ofWarnung(NetzbezugHinweisText.FALSCHER_GEOMETRIE_TYP));
			yield Optional.empty();
		}
		};
	}

	/**
	 * Diese Methode bearbeitet GeometryCollection, MultiLineString und MultiPoint.
	 * MultiLineString und MultiPoint sind beides Subtypen von GeometryCollection.
	 * Diese GeometryCollections werden hier entpackt und die entstehenden Netzbezüge zu einem vereinigt.
	 * Beim Entpacken wird bestimmeNetzbezugEntsprechendGeometrieTyp von hier indirekt rekursiv aufgerufen.
	 */
	private Optional<MassnahmeNetzBezug> bestimmeNetzbezug(GeometryCollection geometry,
		MatchingStatistik matchingStatistik, List<NetzbezugHinweis> netzbezugHinweise) {
		Set<Optional<MassnahmeNetzBezug>> netzbezuege = new HashSet<>();

		for (int i = 0; i < geometry.getNumGeometries(); i++) {
			netzbezuege.add(bestimmeNetzbezugEntsprechendGeometrieTyp(
				geometry.getGeometryN(i),
				matchingStatistik,
				netzbezugHinweise
			)
			);
		}

		Set<MassnahmeNetzBezug> vorhandeneNetzbezuege = netzbezuege.stream()
			.flatMap(Optional::stream)
			.collect(Collectors.toSet());

		if (vorhandeneNetzbezuege.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(MassnahmeNetzBezug.vereinige(vorhandeneNetzbezuege));
		}
	}

	private Optional<MassnahmeNetzBezug> bestimmeNetzbezug(LineString geometry, MatchingStatistik matchingStatistik,
		List<NetzbezugHinweis> netzbezugHinweise) {
		Optional<MassnahmeNetzBezug> netzbezug = simpleMatchingService.matche(geometry, matchingStatistik)
			.flatMap(this::createNetzbezugFromOsmMatchResult);

		if (netzbezug.isEmpty()) {
			netzbezugHinweise.add(NetzbezugHinweis.ofWarnung(NetzbezugHinweisText.NETZBEZUG_UNVOLLSTAENDIG));
		}

		return netzbezug;
	}

	private Optional<MassnahmeNetzBezug> bestimmeNetzbezug(Point geometry, List<NetzbezugHinweis> netzbezugHinweise) {
		Optional<MassnahmeNetzBezug> netzbezug = createNetzbezugForPoint(geometry);

		if (netzbezug.isEmpty()) {
			netzbezugHinweise.add(NetzbezugHinweis.ofWarnung(NetzbezugHinweisText.NETZBEZUG_UNVOLLSTAENDIG));
		}

		return netzbezug;
	}

	private Optional<MassnahmeNetzBezug> createNetzbezugFromOsmMatchResult(OsmMatchResult osmMatchResult) {
		Map<Long, Kante> kantenInMatching = new HashMap<>();

		Set<AbschnittsweiserKantenSeitenBezug> gefundeneAKSB = osmMatchResult.getOsmWayIds()
			.stream()
			.map(OsmWayId::getValue)
			.map(netzService::getKante)
			.peek(kante -> kantenInMatching.put(kante.getId(), kante))
			.filter(kante -> LineStrings.calculateUeberschneidungslinestring(
				kante.getGeometry(),
				osmMatchResult.getGeometrie())
				.isPresent()
			)
			.map(kante -> new MappedGrundnetzkante(
				kante.getGeometry(),
				kante.getId(),
				osmMatchResult.getGeometrie()))
			.map(mappedGrundnetzkante -> {
				Kante kante = kantenInMatching.get(mappedGrundnetzkante.getKanteId());
				return new AbschnittsweiserKantenSeitenBezug(
					kante,
					LinearReferenzierterAbschnitt.snappeAufEndpunkte(
						mappedGrundnetzkante.getLinearReferenzierterAbschnitt(), kante.getLaengeBerechnet().getValue(),
						MINIMALE_SEGMENT_LAENGE),
					Seitenbezug.BEIDSEITIG);
			}) // TODO RAD-6596: andere Seitenbezüge ermöglichen
			.collect(Collectors.toSet());

		if (gefundeneAKSB.isEmpty()) {
			log.warn(
				"Aus dem OsmMatchResult konnte für Kante(n) {} kein AbschnittsweiserKantenSeitenBezug erstellt werden",
				osmMatchResult.getOsmWayIds()
			);
			return Optional.empty();
		}

		gefundeneAKSB = AbschnittsweiserKantenSeitenBezug.fasseUeberlappendeBezuegeProKanteZusammen(gefundeneAKSB);

		return Optional.of(
			new MassnahmeNetzBezug(gefundeneAKSB, Set.of(), Set.of()));
	}

	Optional<MassnahmeNetzBezug> createNetzbezugForPoint(Point geometry) {
		Envelope envelope = geometry.getEnvelopeInternal();
		envelope.expandBy(GEOMETRIE_SUCHRADIUS_IN_METER);

		List<Kante> radVisNetzKantenInBereich = netzService.getRadVisNetzKantenInBereich(envelope).stream()
			.sorted(Comparator.comparing(
				Kante::getGeometry,
				Comparator.comparingDouble(linestring -> linestring.distance(geometry))))
			.toList();

		List<Knoten> radVisNetzKnotenInBereich = netzService.getRadVisNetzKnotenInBereich(envelope).stream()
			.sorted(Comparator.comparing(
				Knoten::getPoint,
				Comparator.comparingDouble(point -> point.distance(geometry))))
			.toList();

		if (radVisNetzKantenInBereich.isEmpty() && radVisNetzKnotenInBereich.isEmpty()) {
			return Optional.empty();
		}

		Set<Knoten> knotenNetzbezug = new HashSet<>();
		Set<PunktuellerKantenSeitenBezug> punktuellerKantenbezug = new HashSet<>();

		Knoten closestKnoten = radVisNetzKnotenInBereich.isEmpty() ? null : radVisNetzKnotenInBereich.get(0);
		Kante closestKante = radVisNetzKantenInBereich.isEmpty() ? null : radVisNetzKantenInBereich.get(0);

		if (hatKnotenNetzbezug(closestKante, geometry, radVisNetzKnotenInBereich, closestKnoten)) {
			knotenNetzbezug.add(closestKnoten);
		} else {
			LineareReferenz lineareReferenz = LineareReferenz.of(closestKante.getGeometry(), geometry.getCoordinate());
			PunktuellerKantenSeitenBezug pKSB = new PunktuellerKantenSeitenBezug(
				closestKante,
				lineareReferenz,
				Seitenbezug.BEIDSEITIG // TODO RAD-6596: andere Seitenbezüge ermöglichen
			);
			punktuellerKantenbezug.add(pKSB);
		}

		return Optional.of(new MassnahmeNetzBezug(Collections.emptySet(), punktuellerKantenbezug, knotenNetzbezug));
	}

	private static boolean hatKnotenNetzbezug(Kante closestKante, Point geometry,
		List<Knoten> radVisNetzKnotenInBereich, Knoten closestKnoten) {
		return closestKante == null ||
			(!radVisNetzKnotenInBereich.isEmpty() &&
			// nimm nächste Geometrie aber bevorzuge Knoten
				closestKante.getGeometry().distance(geometry) >
					closestKnoten.getPoint().distance(geometry) - KNOTEN_PREFERENCE_TOLERANZ);
	}
}
