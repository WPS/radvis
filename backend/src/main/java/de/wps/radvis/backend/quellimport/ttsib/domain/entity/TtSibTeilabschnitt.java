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

package de.wps.radvis.backend.quellimport.ttsib.domain.entity;

import static org.valid4j.Assertive.require;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.quellimport.ttsib.domain.KeinMittelstreifenException;
import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibEinordnung;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class TtSibTeilabschnitt extends TtSibAbstractEntity {
	@Getter
	private int vonStation;

	@Getter
	private int bisStation;

	@Getter
	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "tt_sib_querschnitt_id", nullable = false)
	private TtSibQuerschnitt querschnitt;

	private static final GeometryFactory GEOMETRY_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N
		.getGeometryFactory();

	public TtSibTeilabschnitt(Integer vonStation, Integer bisStation) {
		require(vonStation < bisStation,
			String.format("Die vonStation muss kleiner sein als die bisStation. Erhalten: %d %d", vonStation,
				bisStation));
		this.vonStation = vonStation;
		this.bisStation = bisStation;
	}

	public TtSibTeilabschnitt(Integer vonStation, Integer bisStation, TtSibQuerschnitt ttSibQuerschnitt) {
		this(vonStation, bisStation);
		this.querschnitt = ttSibQuerschnitt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TtSibTeilabschnitt that = (TtSibTeilabschnitt) o;
		return vonStation == that.vonStation && bisStation == that.bisStation;
	}

	@Override
	public int hashCode() {
		return Objects.hash(vonStation, bisStation);
	}

	public boolean ueberschneidet(TtSibTeilabschnitt other) {
		boolean thisUebschneidetOtherNicht = vonStation < other.vonStation
			&& bisStation < other.bisStation
			&& bisStation <= other.vonStation;

		boolean otherUeberschneidetThisNicht = vonStation > other.vonStation
			&& bisStation > other.bisStation
			&& vonStation >= other.bisStation;

		return !thisUebschneidetOtherNicht && !otherUeberschneidetThisNicht;
	}

	public Set<LineString> ermittleRadwegverlaeufe(LineString mitteLineString) throws KeinMittelstreifenException {
		Set<Radwegstreifenversatz> radwegstreifenversatze = this.querschnitt.ermittleStreifenversatze();
		Set<LineString> radwegverlaeufe = new HashSet<>();
		radwegstreifenversatze.forEach((Radwegstreifenversatz versatz) -> {
			if (versatz.getEinordnung() == TtSibEinordnung.MITTE) {
				radwegverlaeufe.add(mitteLineString);
			} else {
				radwegverlaeufe.add(
					berechneVersetztenRadwegverlauf(mitteLineString,
						versatz.getAbstandZurMitteInCmStart(),
						versatz.getAbstandZurMitteInCmEnde(),
						versatz.getEinordnung()));
			}
		});
		return radwegverlaeufe;
	}

	private LineString berechneVersetztenRadwegverlauf(LineString mitteLineString, int abstandZurMitteInCmStart,
		int abstandZurMitteInCmEnde, TtSibEinordnung einordnung) {
		double distanceForJtsInMeterStart = ((double) abstandZurMitteInCmStart) / 100.0;
		double distanceForJtsInMeterEnde = ((double) abstandZurMitteInCmEnde) / 100.0;
		// JTS API fordert bei einem Versatz nach Rechts eine negative Distanz
		if (einordnung == TtSibEinordnung.RECHTS) {
			distanceForJtsInMeterStart = -distanceForJtsInMeterStart;
			distanceForJtsInMeterEnde = -distanceForJtsInMeterEnde;
		}

		Coordinate[] offsetCurve = generateOffset(mitteLineString.getCoordinates(), mitteLineString.getLength(),
			distanceForJtsInMeterStart, distanceForJtsInMeterEnde);
		return GEOMETRY_FACTORY.createLineString(offsetCurve);
	}

	private Coordinate[] generateOffset(Coordinate[] originalLine, double lineLength, double distanceStart,
		double distanceEnd) {
		if (originalLine.length == 0 || lineLength == 0) {
			return originalLine;
		}
		double distanceDiff = distanceEnd - distanceStart;
		double currentFractionLength = 0.0;

		LineSegment[] offsetLineSegments = new LineSegment[originalLine.length - 1];
		// Berechne versetzte LineSegments
		for (int i = 0; i < originalLine.length - 1; i++) {
			LineSegment originalLineSegment = new LineSegment(originalLine[i], originalLine[i + 1]);
			double startPointOffset = distanceStart + (currentFractionLength * distanceDiff);
			Coordinate offsetStartPoint = originalLineSegment.pointAlongOffset(0.0, startPointOffset);
			double fractionLength = originalLineSegment.getLength() / lineLength;
			currentFractionLength += fractionLength;
			double endPointOffset = distanceStart + (currentFractionLength * distanceDiff);
			Coordinate offsetEndPoint = originalLineSegment.pointAlongOffset(1.0, endPointOffset);
			offsetLineSegments[i] = new LineSegment(offsetStartPoint, offsetEndPoint);
		}

		Coordinate[] offsetCurve = new Coordinate[originalLine.length];
		// Erster Punkt ist Startpunkt des ersten verschobenen Segments
		offsetCurve[0] = offsetLineSegments[0].getCoordinate(0);
		// Letzter Punkt ist Endpunkt des letzten verschobenen Segments
		offsetCurve[originalLine.length - 1] = offsetLineSegments[offsetLineSegments.length - 1]
			.getCoordinate(1);
		// Punkte dazwischen sind Schnittpunkte von zwei aufeinanderfolgenden Segmenten
		for (int i = 0; i < offsetLineSegments.length - 1; i++) {
			offsetCurve[i + 1] = offsetLineSegments[i].lineIntersection(offsetLineSegments[i + 1]);
		}

		return offsetCurve;
	}

	@AllArgsConstructor
	@Getter
	@EqualsAndHashCode
	public static class Radwegstreifenversatz {
		private int abstandZurMitteInCmStart;
		private int abstandZurMitteInCmEnde;
		private TtSibEinordnung einordnung;
	}
}
