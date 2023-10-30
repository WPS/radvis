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
import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.util.Pair;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.matching.domain.entity.MatchingJobStatistik;
import de.wps.radvis.backend.matching.domain.exception.GeometryLaengeMismatchException;
import de.wps.radvis.backend.matching.domain.exception.GeometryZuWeitEntferntException;
import de.wps.radvis.backend.matching.domain.exception.LinestringInvalidException;
import de.wps.radvis.backend.matching.domain.exception.MatchingFehlerException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchingKorrekturService {

	public static final double FLAT_LENGTH_DIFFERENCE_ERROR = 30;
	public static final double RATIO_LENGTH_DIFFERENCE_ERROR = 2.;
	public static final double MAX_DISTANCE_TO_MATCHED_GEOMETRY = 30;
	private final GeometryFactory geometryFactory;

	public MatchingKorrekturService() {
		this.geometryFactory = new GeometryFactory(new PrecisionModel(),
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
	}

	public LineString checkMatchingGeometrieAufFehlerUndKorrigiere(Long kanteId, LineString kanteGeometrie,
		LineString matchingLineString, MatchingJobStatistik statistik)
		throws GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		return checkMatchingGeometrieAufFehlerUndKorrigiere(kanteId, kanteGeometrie, matchingLineString,
			statistik,
			FLAT_LENGTH_DIFFERENCE_ERROR, RATIO_LENGTH_DIFFERENCE_ERROR, MAX_DISTANCE_TO_MATCHED_GEOMETRY);
	}

	public LineString checkGesamteStreckenGeometrieAufFehlerUndKorrigiere(Long kanteId, LineString kanteGeometrie,
		LineString matchingLineString, MatchingJobStatistik statistik, int anzahlKanten)
		throws GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		return checkMatchingGeometrieAufFehlerUndKorrigiere(kanteId, kanteGeometrie, matchingLineString,
			statistik,
			Math.min(3, anzahlKanten) * FLAT_LENGTH_DIFFERENCE_ERROR,
			Math.max(1 - anzahlKanten * 0.05, 0.7) * RATIO_LENGTH_DIFFERENCE_ERROR,
			MAX_DISTANCE_TO_MATCHED_GEOMETRY);
	}

	public LineString checkMatchingGeometrieAufFehlerUndKorrigiere(LineString kanteGeometrie,
		LineString matchingLineString)
		throws GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		return checkMatchingGeometrieAufFehlerUndKorrigiere(null, kanteGeometrie, matchingLineString, null);
	}

	public LineString checkMatchingGeometrieAufFehlerUndKorrigiere(Long kanteId, LineString kanteGeometrie,
		LineString matchingLineString, MatchingJobStatistik statistik, double erlaubteAbsoluteLaengenAbweichung,
		double erlaubterRelativerLaengenabweichungsfaktor, double erlaubteMaximaleDistanz)
		throws GeometryLaengeMismatchException, GeometryZuWeitEntferntException {

		double originalLaenge = kanteGeometrie.getLength();
		double matchingLaenge = matchingLineString.getLength();

		if (matchingLaenge == 0. || originalLaenge == 0.) {
			throw new GeometryLaengeMismatchException(
				"Die Originalgeometrie oder abgebildete Geometrie hat die Laenge 0",
				originalLaenge,
				matchingLaenge,
				kanteId, matchingLineString);
		}
		try {
			checkMatchingGeometrieAufFehler(originalLaenge, matchingLaenge, kanteId, kanteGeometrie,
				matchingLineString,
				erlaubteAbsoluteLaengenAbweichung, erlaubterRelativerLaengenabweichungsfaktor, erlaubteMaximaleDistanz);
			return matchingLineString;
		} catch (MatchingFehlerException e) {
			try {
				matchingLineString = entferneLoopsAusMatchingGeometrie(matchingLineString);
			} catch (LinestringInvalidException e1) {
				// Versuch, den bestehenden Fehler aufzuheben, ist fehlgeschlagen. Damit bleibt es bei Fehler e
				throw e;
			}
			originalLaenge = kanteGeometrie.getLength();
			matchingLaenge = matchingLineString.getLength();

			checkMatchingGeometrieAufFehler(originalLaenge, matchingLaenge, kanteId, kanteGeometrie,
				matchingLineString,
				erlaubteAbsoluteLaengenAbweichung, erlaubterRelativerLaengenabweichungsfaktor, erlaubteMaximaleDistanz);

			if (statistik != null) {
				statistik.anzahlKorrekturHatGeholfen++;
			}

			return matchingLineString;
		}
	}

	/**
	 * Entfernt unnötige loops die entweder die Länge künstlich aufblähen oder Umwege, die wir wahrscheinlich gar nicht
	 * haben wollen. Zum Beispiel kann das auftreten, wenn ein Stützpunkt falsch auf DLM gematched wurde, ein Umweg
	 * dahin genommen wird, und dann den weg wieder zurück läuft. Der Algorithmus schaut sich die Steps (Folge von 2
	 * Koordinaten im LineString) an und löscht einen Schritt, wenn wir diesen Schritt auch später wieder zurück gehen.
	 *
	 * @throws LinestringInvalidException
	 */
	public LineString entferneLoopsAusMatchingGeometrie(LineString matchingLineString)
		throws LinestringInvalidException {
		Coordinate[] coordinates = matchingLineString.getCoordinates();

		// Für performance des lookups (DLM-Geometrien können recht groß werden)
		Map<Pair<Coordinate, Coordinate>, Integer> lookupStepMap = new HashMap<>(coordinates.length);
		ArrayList<Coordinate> resultCoordinates = new ArrayList<>(coordinates.length);
		resultCoordinates.add(coordinates[0]);
		for (int i = 0; i < coordinates.length - 1; i++) {
			Coordinate current = coordinates[i];
			Coordinate next = coordinates[i + 1];
			Pair<Coordinate, Coordinate> step = Pair.of(current, next);
			Pair<Coordinate, Coordinate> backwardStep = Pair.of(step.getSecond(), step.getFirst());

			int anzahlVorkommenBackwardStep = lookupStepMap.getOrDefault(backwardStep, 0);
			if (anzahlVorkommenBackwardStep > 0) {
				removeErstesAuftretenDesSteps(resultCoordinates, backwardStep);
				if (anzahlVorkommenBackwardStep == 1) {
					lookupStepMap.remove(backwardStep);
				} else {
					lookupStepMap.put(backwardStep, anzahlVorkommenBackwardStep - 1);
				}
			} else {
				resultCoordinates.add(next);
				lookupStepMap.put(step, lookupStepMap.getOrDefault(step, 0) + 1);
			}
		}
		if (coordinates.length - resultCoordinates.size() > 0) {
			log.debug(
				"Beim entfernen von Loops aus einer abgebildeten Geometrie  wurden {} von {} Koordinaten entfernt. {} Bleiben übrig.",
				coordinates.length - resultCoordinates.size(), coordinates.length, resultCoordinates.size());
		}

		if (resultCoordinates.size() < 2) {
			log.debug(
				"Abgebildete Geometrie hat weniger als zwei Knoten nach Korrektur und ist kein valider LineString mehr.");
			throw new LinestringInvalidException();
		}

		return geometryFactory.createLineString(resultCoordinates.toArray(new Coordinate[0]));
	}

	private void removeErstesAuftretenDesSteps(
		ArrayList<Coordinate> resultCoordinates, Pair<Coordinate, Coordinate> step) {
		for (int i = 0; i < resultCoordinates.size() - 1; i++) {
			Coordinate current = resultCoordinates.get(i);
			Coordinate next = resultCoordinates.get(i + 1);
			if (Pair.of(current, next).equals(step)) {
				resultCoordinates.remove(i + 1);
				break;
			}
		}
	}

	private void checkMatchingGeometrieAufFehler(double originalLaenge, double matchLaenge, Long kanteId,
		LineString kanteGeometrie, LineString matchLineString, double erlaubteAbsoluteLaengenAbweichung,
		double erlaubterRelativerLaengenabweichungsfaktor, double erlaubteMaximaleDistanz)
		throws GeometryLaengeMismatchException, GeometryZuWeitEntferntException {
		if (Math.abs(originalLaenge - matchLaenge) > erlaubteAbsoluteLaengenAbweichung
			|| originalLaenge / matchLaenge > erlaubterRelativerLaengenabweichungsfaktor
			|| matchLaenge / originalLaenge > erlaubterRelativerLaengenabweichungsfaktor) {
			throw new GeometryLaengeMismatchException(
				"Die Laenge von abgebildeter Geometrie und Ursprungsgeometrie ist zu unterschiedlich.", originalLaenge,
				matchLaenge, kanteId, matchLineString);
		}

		if (!kanteGeometrie.buffer(erlaubteMaximaleDistanz).contains(matchLineString)) {
			throw new GeometryZuWeitEntferntException(
				"An mindestens einer Stelle ist die abgebildeter Geometrie zu weit entfernt von der Ursprungsgeometrie",
				kanteId, matchLineString);
		}
	}
}
