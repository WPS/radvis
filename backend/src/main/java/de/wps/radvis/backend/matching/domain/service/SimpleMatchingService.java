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

package de.wps.radvis.backend.matching.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.springframework.data.util.Lazy;

import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.matching.domain.exception.GeometryLaengeMismatchException;
import de.wps.radvis.backend.matching.domain.exception.GeometryZuWeitEntferntException;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.exception.LinestringInvalidException;
import de.wps.radvis.backend.matching.domain.exception.MatchingFehlerException;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;

public class SimpleMatchingService {
	private final Lazy<DlmMatchingRepository> dlmMatchingRepositorySupplier;

	private final MatchingKorrekturService korrekturService;

	public SimpleMatchingService(
		Lazy<DlmMatchingRepository> dlmMatchingRepositorySupplier, MatchingKorrekturService korrekturService) {
		this.dlmMatchingRepositorySupplier = dlmMatchingRepositorySupplier;
		this.korrekturService = korrekturService;
	}

	public Optional<OsmMatchResult> matche(LineString lineString, MatchingStatistik statistik) {
		boolean hatZuSchlechtesMatchBekommen = false;
		OsmMatchResult match = null;

		try {
			match = matchGeometryBeideRichtungen(lineString, statistik);
		} catch (KeinMatchGefundenException e) {
			statistik.anzahlOhneMatch++;
		} catch (GeometryLaengeMismatchException e) {
			hatZuSchlechtesMatchBekommen = true;
			statistik.anzahlLaengeMismatch++;
			statistik
				.reportLaengeMismatch(Math.round(e.getAbgebildeteGeometryLaenge() - e.getOriginalGeometryLaenge()));
			statistik.reportLaengeMismatchKanteLaenge(lineString.getLength());
		} catch (GeometryZuWeitEntferntException e) {
			statistik.anzahlZuWeitEntfernteMatches++;
			hatZuSchlechtesMatchBekommen = true;
		} catch (LinestringInvalidException e) {
			statistik.anzahlKorrekturInvalid++;
		}

		if (hatZuSchlechtesMatchBekommen) {
			statistik.anzahlKantenMitZuSchlechtemGraphhopperMatch++;
			return Optional.empty();
		}
		if (match == null) {
			statistik.anzahlKantenOhneGraphhopperMatch++;
			return Optional.empty();
		}

		if (!match.getGeometrie().isSimple()) {
			statistik.matchAberNichtSimple++;
		}

		return Optional.of(match);
	}

	private OsmMatchResult matchGeometryBeideRichtungen(LineString geometry,
		MatchingStatistik statistik)
		throws KeinMatchGefundenException, GeometryZuWeitEntferntException, GeometryLaengeMismatchException,
		LinestringInvalidException {
		OsmMatchResult match;
		DlmMatchingRepository dlmMatchingRepository = dlmMatchingRepositorySupplier.get();

		match = dlmMatchingRepository.matchGeometry(geometry, "bike");

		try {
			if (!match.getGeometrie().isSimple()) {
				match.setGeometrie(korrekturService.entferneLoopsAusMatchingGeometrie(match.getGeometrie()));
			}
			match = new OsmMatchResult(korrekturService
				.checkMatchingGeometrieAufFehlerUndKorrigiere(geometry, match.getGeometrie()),
				match.getOsmWayIdsAsOrderedList());

		} catch (MatchingFehlerException | LinestringInvalidException eInOrderGeometry) {
			try {
				// Nochmal probieren mit umgedrehter Geometrie.
				// Hilft bei vielen Einbahnstraßen wo die Orientierung falsch ist.
				match = dlmMatchingRepository.matchGeometry(geometry.reverse(), "bike");

				if (!match.getGeometrie().isSimple()) {
					match.setGeometrie(korrekturService.entferneLoopsAusMatchingGeometrie(match.getGeometrie()));
				}

				match = new OsmMatchResult(korrekturService
					.checkMatchingGeometrieAufFehlerUndKorrigiere(geometry, match.getGeometrie()),
					match.getOsmWayIdsAsOrderedList());
				statistik.anzahlUmdrehenHatGeholfen++;
			} catch (MatchingFehlerException | LinestringInvalidException eReversedGeometry) {
				throw eInOrderGeometry;
			}
		}
		return match;
	}

	public List<OsmMatchResult> matchMultiLinestring(MultiLineString geometry,
		MatchingStatistik matchingStatistik) {
		if (geometry.getNumGeometries() == 0) {
			throw new RuntimeException("Empty MultiLinestring");
		}

		ArrayList<OsmMatchResult> result = new ArrayList<>();

		for (int i = 0; i < geometry.getNumGeometries(); i++) {
			LineString lineString = (LineString) geometry.getGeometryN(i);
			Optional<OsmMatchResult> matchResult = matche(lineString, matchingStatistik);
			matchResult.ifPresent(result::add);
		}

		return result;
	}
}
