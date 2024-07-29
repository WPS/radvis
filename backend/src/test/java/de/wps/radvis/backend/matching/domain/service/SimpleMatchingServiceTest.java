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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.matching.domain.exception.GeometryLaengeMismatchException;
import de.wps.radvis.backend.matching.domain.exception.GeometryZuWeitEntferntException;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.exception.LinestringInvalidException;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;

class SimpleMatchingServiceTest {

	private SimpleMatchingService service;

	@Mock
	private DlmMatchingRepository dlmMatchingRepository;
	@Mock
	private MatchingKorrekturService matchingKorrekturService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new SimpleMatchingService(org.springframework.data.util.Lazy.of(() -> dlmMatchingRepository),
			matchingKorrekturService);
	}

	@Test
	void testeMatche_KeinMatchGefundenException() throws KeinMatchGefundenException {
		// arrange
		MatchingStatistik matchingStatistik = new MatchingStatistik();

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(120, 120));
		when(dlmMatchingRepository.matchGeometry(lineString, "bike")).thenThrow(new KeinMatchGefundenException("oh no",
			new Throwable()));

		// act
		Optional<OsmMatchResult> result = service.matche(lineString, matchingStatistik);

		// assert
		assertThat(result).isEmpty();
		assertThat(matchingStatistik.anzahlOhneMatch).isEqualTo(1);
	}

	@Test
	void testeMatche_ErstesMatchZuSchlechtAberZweitesGutGenug()
		throws KeinMatchGefundenException, GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		// arrange
		MatchingStatistik matchingStatistik = new MatchingStatistik();

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(120, 120));
		LineString matching1 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(
			lineString, 0, 1);
		LineString matching2 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(
			lineString, 0, -1).reverse();
		when(dlmMatchingRepository.matchGeometry(any(), eq("bike"))).thenReturn(
			new OsmMatchResult(matching1, Collections.emptyList()))
			.thenReturn(new OsmMatchResult(matching2, Collections.emptyList()));

		when(matchingKorrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(lineString, matching1)).thenThrow(
			new GeometryLaengeMismatchException("ヤバイっすね", Integer.MIN_VALUE, Integer.MAX_VALUE, 10L, matching1));
		when(matchingKorrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(lineString, matching2)).thenReturn(
			matching2);

		// act
		Optional<OsmMatchResult> result = service.matche(lineString, matchingStatistik);

		// assert
		assertThat(result).isPresent();
		assertThat(result.get().getGeometrie().getCoordinates()).containsExactly(matching2.getCoordinates());
		assertThat(matchingStatistik.anzahlUmdrehenHatGeholfen).isEqualTo(1);
		verify(dlmMatchingRepository, times(2)).matchGeometry(any(), eq("bike"));
		verify(matchingKorrekturService, times(2)).checkMatchingGeometrieAufFehlerUndKorrigiere(any(), any());
	}

	@Test
	void testeMatche_BeideMatchesZuSchlecht()
		throws KeinMatchGefundenException, GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		// arrange
		MatchingStatistik matchingStatistik = new MatchingStatistik();

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(120, 120));
		LineString matching1 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(
			lineString, 0, 1);
		LineString matching2 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(
			lineString, 0, -1).reverse();
		when(dlmMatchingRepository.matchGeometry(any(), eq("bike"))).thenReturn(
			new OsmMatchResult(matching1, Collections.emptyList()))
			.thenReturn(new OsmMatchResult(matching2, Collections.emptyList()));

		when(matchingKorrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(lineString, matching1)).thenThrow(
			new GeometryLaengeMismatchException("ヤバイっすね", Integer.MIN_VALUE, Integer.MAX_VALUE, 10L, matching1));
		when(matchingKorrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(lineString, matching2)).thenThrow(
			new GeometryLaengeMismatchException("ヤバイっすね²", Integer.MIN_VALUE, Integer.MAX_VALUE, 10L, matching2));

		// act
		Optional<LineString> result = service.matche(lineString, matchingStatistik)
			.map(OsmMatchResult::getGeometrie);

		// assert
		assertThat(result).isEmpty();

		assertThat(matchingStatistik.anzahlKantenMitZuSchlechtemGraphhopperMatch).isEqualTo(1);
		assertThat(matchingStatistik.anzahlLaengeMismatch).isEqualTo(1);
		assertThat(matchingStatistik.anzahlLaengeMismatchMoreThan100m).isEqualTo(1);
		assertThat(matchingStatistik.laengenmismatchKanteKuerzer50m).isEqualTo(1);

		verify(dlmMatchingRepository, times(2)).matchGeometry(any(), eq("bike"));
		verify(matchingKorrekturService, times(2)).checkMatchingGeometrieAufFehlerUndKorrigiere(any(), any());
	}

	@Test
	void testeMatche_NonSimpleMatch_wirdKorrigiert()
		throws KeinMatchGefundenException, GeometryLaengeMismatchException, GeometryZuWeitEntferntException,
		LinestringInvalidException {
		// arrange
		MatchingStatistik matchingStatistik = new MatchingStatistik();

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(100, 121),
			new Coordinate(120, 121), new Coordinate(140, 121));
		LineString matching1 = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(120, 120),
			// loop
			new Coordinate(120, 140), new Coordinate(120, 160), new Coordinate(120, 140),
			new Coordinate(120, 120), new Coordinate(140, 120));
		LineString matching2 = matching1.reverse();
		when(dlmMatchingRepository.matchGeometry(any(), eq("bike"))).thenReturn(
			new OsmMatchResult(matching1, Collections.emptyList()))
			.thenReturn(new OsmMatchResult(matching2, Collections.emptyList()));

		LineString matchOhneLoop = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(120, 120), new Coordinate(140, 120));
		when(matchingKorrekturService.entferneLoopsAusMatchingGeometrie(matching1)).thenReturn(matchOhneLoop);

		LineString reverseMatchOhneLoop = matchOhneLoop.reverse();
		when(matchingKorrekturService.entferneLoopsAusMatchingGeometrie(matching2)).thenReturn(reverseMatchOhneLoop);

		when(
			matchingKorrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(lineString, matchOhneLoop)).thenThrow(
				new GeometryLaengeMismatchException("ヤバイっすね", Integer.MIN_VALUE, Integer.MAX_VALUE, 10L, matching1));
		when(matchingKorrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(lineString,
			reverseMatchOhneLoop)).thenReturn(reverseMatchOhneLoop);

		// act
		Optional<LineString> result = service.matche(lineString, matchingStatistik)
			.map(OsmMatchResult::getGeometrie);

		// assert
		assertThat(result).isPresent();
		assertThat(result.get().getCoordinates()).containsExactly(reverseMatchOhneLoop.getCoordinates());

		assertThat(matchingStatistik.anzahlUmdrehenHatGeholfen).isEqualTo(1);
		assertThat(matchingStatistik.matchAberNichtSimple).isEqualTo(0);

		verify(dlmMatchingRepository, times(2)).matchGeometry(any(), eq("bike"));
		verify(matchingKorrekturService, times(2)).checkMatchingGeometrieAufFehlerUndKorrigiere(any(), any());
		verify(matchingKorrekturService).entferneLoopsAusMatchingGeometrie(matching1);
		verify(matchingKorrekturService).entferneLoopsAusMatchingGeometrie(matching2);
	}

	@Test
	void testeMatche_MatchesZuSchlechtUndReverse_LoggtInOrderMatchVersuch()
		throws KeinMatchGefundenException, GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		// arrange
		MatchingStatistik matchingStatistik = new MatchingStatistik();

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(120, 120));
		LineString matching1 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(
			lineString, 0, 1);
		LineString matching2 = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(
			lineString, 0, -1).reverse();
		when(dlmMatchingRepository.matchGeometry(any(), eq("bike"))).thenReturn(
			new OsmMatchResult(matching1, Collections.emptyList()))
			.thenReturn(new OsmMatchResult(matching2, Collections.emptyList()));

		when(matchingKorrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(lineString, matching1)).thenThrow(
			new GeometryLaengeMismatchException("ヤバイっすね", Integer.MIN_VALUE, Integer.MAX_VALUE, 10L, matching1));

		// wird nicht gelogged
		when(matchingKorrekturService.checkMatchingGeometrieAufFehlerUndKorrigiere(lineString, matching2)).thenThrow(
			new GeometryZuWeitEntferntException("lol", 1L, matching2));

		// act
		service.matche(lineString, matchingStatistik).map(OsmMatchResult::getGeometrie);

		// assert
		assertThat(matchingStatistik.anzahlKantenMitZuSchlechtemGraphhopperMatch).isEqualTo(1);
		assertThat(matchingStatistik.anzahlLaengeMismatch).isEqualTo(1);
		assertThat(matchingStatistik.anzahlLaengeMismatchMoreThan100m).isEqualTo(1);
		assertThat(matchingStatistik.laengenmismatchKanteKuerzer50m).isEqualTo(1);
		// wird NICHT geloggt
		assertThat(matchingStatistik.anzahlZuWeitEntfernteMatches).isEqualTo(0);
	}
}