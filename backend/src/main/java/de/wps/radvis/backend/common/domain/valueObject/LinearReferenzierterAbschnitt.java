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

package de.wps.radvis.backend.common.domain.valueObject;

import static org.valid4j.Assertive.require;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modelliert eine lineare Referenzierung mittels des dezimalen Anteils (Fraction), von 0.0 bis 1.0, an einem linearen
 * gemessenen Feature bzw einer linearen Geometrie.
 */
@EqualsAndHashCode
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinearReferenzierterAbschnitt implements Serializable {
	private static final long serialVersionUID = 5204665787708601580L;

	private static final double PRECISION = 0.001;
	private static final DecimalFormat THREE_DECIMAL_PLACES = new DecimalFormat("#.###",
		DecimalFormatSymbols.getInstance(Locale.ENGLISH));

	// private setter für Hibernate (das Feld wird sonst beim Lesen aus der DB nicht gesetzt)
	@Setter(AccessLevel.PRIVATE)
	private LineareReferenz von;
	@Setter(AccessLevel.PRIVATE)
	private LineareReferenz bis;

	private LinearReferenzierterAbschnitt(double von, double bis) {
		require(von < bis, "%f < %f", von, bis);
		this.von = LineareReferenz.of(von);
		this.bis = LineareReferenz.of(bis);
	}

	@JsonCreator
	public static LinearReferenzierterAbschnitt of(@JsonProperty("von") double von, @JsonProperty("bis") double bis) {
		return new LinearReferenzierterAbschnitt(von, bis);
	}

	public static boolean fractionEqual(double d1, double d2) {
		return Math.abs(d1 - d2) < PRECISION;
	}

	public static boolean fractionEqual(LinearReferenzierterAbschnitt LR1, LinearReferenzierterAbschnitt LR2) {
		return LineareReferenz.fractionEqual(LR1.von, LR2.von) && LineareReferenz.fractionEqual(LR1.bis, LR2.bis);
	}

	public static boolean segmentsCoverFullLine(
		List<? extends LinearReferenzierterAbschnitt> linearReferenzierteAttribute) {
		if (linearReferenzierteAttribute.isEmpty()) {
			return false;
		}

		List<? extends LinearReferenzierterAbschnitt> sorted = linearReferenzierteAttribute.stream()
			.sorted(Comparator.comparingDouble(LinearReferenzierterAbschnitt::getVonValue))
			.collect(Collectors.toList());

		if (!fractionEqual(sorted.get(0).getVonValue(), 0.0)) {
			return false;
		}

		if (!fractionEqual(sorted.get(linearReferenzierteAttribute.size() - 1).getBisValue(),
			1.0)) {
			return false;
		}

		for (int i = 0; i < sorted.size() - 1; i++) {
			if (!fractionEqual(sorted.get(i).getBisValue(), sorted.get(i + 1).getVonValue())) {
				return false;
			}
		}

		return true;
	}

	public double relativeLaenge() {
		return bis.getAbschnittsmarke() - von.getAbschnittsmarke();
	}

	public LinearReferenzierterAbschnitt fuerUmgedrehteStrecke() {
		return LinearReferenzierterAbschnitt.of(1 - bis.getAbschnittsmarke(), 1 - von.getAbschnittsmarke());
	}

	/**
	 * Es werden auch Punkt-intersections berücksichtigt.
	 */
	public boolean intersects(LinearReferenzierterAbschnitt other) {
		return bis.getAbschnittsmarke() >= other.von.getAbschnittsmarke()
			&& von.getAbschnittsmarke() <= other.bis.getAbschnittsmarke();
	}

	/**
	 * Lücken mit <= maxDistance werden nicht berücksichtigt
	 */
	public boolean intersects(LinearReferenzierterAbschnitt other, double maxDistance) {
		return bis.getAbschnittsmarke() + maxDistance >= other.von.getAbschnittsmarke()
			&& von.getAbschnittsmarke() - maxDistance <= other.bis.getAbschnittsmarke();
	}

	/**
	 * Da eine Lineare Referenz eine Länge > 0 haben muss, werden Punkt-intersections nicht berücksichtigt
	 *
	 * @return Einen Optional mit einer Intersection der Länge größer 0 wenn vorhanden, sonst einen Empty Optional
	 */
	public Optional<LinearReferenzierterAbschnitt> intersection(LinearReferenzierterAbschnitt other) {
		if (!intersects(other)) {
			return Optional.empty();
		}
		if (von.equals(other.bis) || bis.equals(other.von)) {
			return Optional.empty();
		}
		return Optional.of(
			LinearReferenzierterAbschnitt.of(Math.max(von.getAbschnittsmarke(), other.von.getAbschnittsmarke()),
				Math.min(bis.getAbschnittsmarke(), other.bis.getAbschnittsmarke())));
	}

	public boolean contains(LinearReferenzierterAbschnitt other, double erlaubteAbweichung) {
		if (!intersects(other)) {
			return false;
		}
		return other.von.getAbschnittsmarke() - von.getAbschnittsmarke() >= -erlaubteAbweichung
			&& bis.getAbschnittsmarke() - other.bis.getAbschnittsmarke() >= -erlaubteAbweichung;
	}

	public boolean contains(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return contains(linearReferenzierterAbschnitt, 0);
	}

	/**
	 * Prüft, ob die metermarke im offenen Intervall ]von, bis[ liegt. d.h. von & bis sind ausgeschlossen
	 *
	 * @param metermarke
	 * @return
	 */
	public boolean containsStrictly(double metermarke) {
		return von.getAbschnittsmarke() < metermarke && bis.getAbschnittsmarke() > metermarke;
	}

	public Optional<LinearReferenzierterAbschnitt> union(LinearReferenzierterAbschnitt other) {
		if (!intersects(other)) {
			return Optional.empty();
		}
		return Optional.of(
			LinearReferenzierterAbschnitt.of(Math.min(von.getAbschnittsmarke(), other.von.getAbschnittsmarke()),
				Math.max(bis.getAbschnittsmarke(), other.bis.getAbschnittsmarke())));
	}

	public Optional<LinearReferenzierterAbschnitt> union(LinearReferenzierterAbschnitt other, double maxDistance) {
		if (!intersects(other, maxDistance)) {
			return Optional.empty();
		}
		return Optional.of(
			LinearReferenzierterAbschnitt.of(Math.min(von.getAbschnittsmarke(), other.von.getAbschnittsmarke()),
				Math.max(bis.getAbschnittsmarke(), other.bis.getAbschnittsmarke())));
	}

	public static LinearReferenzierterAbschnitt snappeAufEndpunkte(LinearReferenzierterAbschnitt abschnitt,
		double gesamtlaengeBezugsKante, double minimaleSegmentLaenge) {
		double newVon = abschnitt.getVonValue();
		if (abschnitt.getVonValue() * gesamtlaengeBezugsKante < minimaleSegmentLaenge) {
			newVon = 0;
		}

		double newBis = abschnitt.getBisValue();
		if (gesamtlaengeBezugsKante - abschnitt.getBisValue() * gesamtlaengeBezugsKante < minimaleSegmentLaenge) {
			newBis = 1;
		}

		return new LinearReferenzierterAbschnitt(newVon, newBis);
	}

	public static LinearReferenzierterAbschnitt of(LinearLocation anfang, LinearLocation ende,
		LocationIndexedLine locationIndexedLine) {

		double laengeBisAnfang = locationIndexedLine.extractLine(locationIndexedLine.getStartIndex(), anfang)
			.getLength();
		double laengeBisEnde = locationIndexedLine.extractLine(locationIndexedLine.getStartIndex(), ende).getLength();

		double gesamtLaenge = locationIndexedLine
			.extractLine(locationIndexedLine.getStartIndex(), locationIndexedLine.getEndIndex()).getLength();

		return new LinearReferenzierterAbschnitt(laengeBisAnfang / gesamtLaenge, laengeBisEnde / gesamtLaenge);
	}

	/**
	 * Berechnet die Lineare Referenz von einem Linestring auf einen anderen LineString. Bei der Projektion wird NICHT
	 * auf Distanz oder ähnliche Konsistenzbedingungen gecheckt. Es wird stumpf projiziert. Der einzige check der
	 * stattfindet, ist, ob eine valide Lineare Referenz rauskommt, also ob keine Punktprojektion rauskommt.
	 *
	 * @param auf
	 *     der LineString auf den sich die Lineare Referenz bezieht
	 * @param von
	 *     der LineString von dem aus auf den anderen LineString projiziert wird
	 * @return die LineareReferenz die von projiziert auf auf ergibt
	 */
	public static LinearReferenzierterAbschnitt of(LineString auf, LineString von) {
		LocationIndexedLine locationIndexedGrundnetzGeometrie = new LocationIndexedLine(auf);

		final LinearLocation projektionDesAnfangs = locationIndexedGrundnetzGeometrie
			.project(von.getStartPoint().getCoordinate());
		final LinearLocation projektionDesEndes = locationIndexedGrundnetzGeometrie
			.project(von.getEndPoint().getCoordinate());

		List<LinearLocation> projektionen = Arrays.asList(projektionDesAnfangs, projektionDesEndes);
		projektionen.sort(Comparator.comparing(LinearLocation::getSegmentIndex)
			.thenComparing(LinearLocation::getSegmentFraction));

		final LinearLocation startlocationDerUeberschneidung = projektionen.get(0);
		final LinearLocation endlocationDerUeberschneidung = projektionen.get(1);

		require(startlocationDerUeberschneidung.compareTo(endlocationDerUeberschneidung) != 0,
			"Lineare Referenz darf nicht Punktförmig sein");

		return LinearReferenzierterAbschnitt.of(startlocationDerUeberschneidung, endlocationDerUeberschneidung,
			locationIndexedGrundnetzGeometrie);
	}

	public static final Comparator<LinearReferenzierterAbschnitt> vonZuerst = (s1, s2) -> {
		int compareVon = Double.compare(s1.von.getAbschnittsmarke(), s2.von.getAbschnittsmarke());
		if (compareVon == 0) {
			return Double.compare(s1.bis.getAbschnittsmarke(), s2.bis.getAbschnittsmarke());
		} else {
			return compareVon;
		}
	};

	public static final Comparator<LinearReferenzierterAbschnitt> laengste = Comparator.comparingDouble(
		s -> s.bis.getAbschnittsmarke() - s.von.getAbschnittsmarke());

	@Override
	public String toString() {
		return "LineareReferenz{" +
			"von=" + THREE_DECIMAL_PLACES.format(von.getAbschnittsmarke()) +
			", bis=" + THREE_DECIMAL_PLACES.format(bis.getAbschnittsmarke()) +
			'}';
	}

	@Transient
	@JsonIgnore
	public double getVonValue() {
		return von.getAbschnittsmarke();
	}

	@Transient
	@JsonIgnore
	public double getBisValue() {
		return bis.getAbschnittsmarke();
	}
}
