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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Haendigkeit {

	public static double ERLAUBTE_MAXIMALE_DISTANZ = 30;
	public static double SOFTMAX_FAKTOR = 3.;
	public static double NORMALISIERUNGSFAKTOR_FUER_ERLAUBTE_DISTANZ =
		(1 + ERLAUBTE_MAXIMALE_DISTANZ / SOFTMAX_FAKTOR) / (ERLAUBTE_MAXIMALE_DISTANZ / SOFTMAX_FAKTOR);

	Orientierung orientierung;
	double wahrscheinlichkeit;

	private Haendigkeit(Double raw) {
		switch ((int) Math.signum(raw)) {
		case 1:
			this.orientierung = Orientierung.LINKS;
			break;
		case -1:
			this.orientierung = Orientierung.RECHTS;
			break;
		default:
			this.orientierung = Orientierung.UNBESTIMMT;
			break;
		}
		this.wahrscheinlichkeit = Math.abs(raw);
	}

	public enum Orientierung {
		LINKS, RECHTS, UNBESTIMMT;
	}

	public double getVorzeichenbehafteteWahrscheinlichkeit() {
		return orientierung.equals(Orientierung.LINKS) ? wahrscheinlichkeit : -1 * wahrscheinlichkeit;
	}

	public static final Comparator<Haendigkeit> vonLinksNachRechts = (s1, s2) -> {
		if (s1.orientierung != s2.orientierung) {
			if (s1.orientierung.equals(Orientierung.LINKS)) {
				return -1;
			} else if (s1.orientierung.equals(Orientierung.UNBESTIMMT) && s2.orientierung.equals(Orientierung.RECHTS)) {
				return -1;
			} else {
				return 1;
			}
		} else {
			if (s1.orientierung.equals(Orientierung.LINKS)) {
				return -Double.compare(s1.wahrscheinlichkeit, s2.wahrscheinlichkeit);
			} else if (s1.orientierung.equals(Orientierung.RECHTS)) {
				return Double.compare(s1.wahrscheinlichkeit, s2.wahrscheinlichkeit);
			} else {
				return 0;
			}
		}
	};

	public static Haendigkeit of(Double raw) {
		return new Haendigkeit(raw);
	}

	public static Haendigkeit of(LineString vonLineString, LineString zuLineString) {
		LocationIndexedLine zuKanteIndexed = new LocationIndexedLine(zuLineString);

		double sum = 0.;
		Coordinate[] vonKanteCoordinates = vonLineString.getCoordinates();
		for (Coordinate coordinate : vonKanteCoordinates) {
			LinearLocation project = zuKanteIndexed.project(coordinate);
			LineSegment segment = project.getSegment(zuLineString);

			int richtungsbezug = (int) Math.signum(seitenBezugVonPunktZuSegment(coordinate, segment));
			double orthogonaleDistanz = segment.distancePerpendicular(coordinate);
			// entspricht softsign( richtungsbezug * (distanz / SOFTMAX_FAKTOR))
			// -> bei einem Wert von SOFTMAX_FAKTOR für die Distanz in Metern haben wir eine 50% confidence
			sum +=
				richtungsbezug * ((orthogonaleDistanz / SOFTMAX_FAKTOR) / (1. + (orthogonaleDistanz / SOFTMAX_FAKTOR)));
		}

		double averageNormalisiertAufErlaubteDistanz =
			sum * NORMALISIERUNGSFAKTOR_FUER_ERLAUBTE_DISTANZ / vonKanteCoordinates.length;

		return new Haendigkeit(averageNormalisiertAufErlaubteDistanz);
	}

	/**
	 * @return > 0 impliziert links, < 0 impliziert rechts, 0 impliziert c liegt auf seg
	 */
	private static double seitenBezugVonPunktZuSegment(Coordinate c, LineSegment seg) {
		Coordinate a = seg.p0;
		Coordinate b = seg.p1;
		return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
	}
}
