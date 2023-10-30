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

package de.wps.radvis.backend.integration.attributAbbildung.domain.entity;

import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Modelliert ein Segment einer Kante, mit den auf diese Segment projizierten Attributen. Ein KantenSegment spiegelt
 * einen Bereich auf der Grundnetzkante wieder, welcher durch lineareReferenzAufGrundnetzKante ausgezeichnet ist. Die
 * Lineare Referenzierung der intern gehaltenen LR-Attribute bezieht sich aber auf das Segment, nicht (!) auf die
 * GrundnetzKante. Beim erstellen eines KantenSegments werden die Linearen Referenzen der LR-Attribute der
 * kanteMitZuProjizierendenAttributen auf das Segment umgerechnet. Diese Klasse bietet für jede der Linear
 * Referenzierten Attribute eine Methode an, um sie mit ihren Linearen Referenzen umgerechnet auf die
 * Grundnetzkantengeometrie (also zwischen lineareReferenzAufGrundnetzKante.getVon() und
 * lineareReferenzAufGrundnetzKante.getBis()) zu bekommen.
 */
public class KantenSegment {

	public static double ERLAUBTE_MAXIMALE_DISTANZ = 30;
	public static double SOFTMAX_FAKTOR = 3.;
	public static double NORMALISIERUNGSFAKTOR_FUER_ERLAUBTE_DISTANZ =
		(1 + ERLAUBTE_MAXIMALE_DISTANZ / SOFTMAX_FAKTOR) / (ERLAUBTE_MAXIMALE_DISTANZ / SOFTMAX_FAKTOR);

	@NonNull
	@Getter
	@Setter
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnittAufZielnetzKante;

	@NonNull
	@Getter
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnittAufQuellnetzKante;

	@NonNull
	@Getter
	private final Kante quellnetzKante;

	@NonNull
	@Getter
	private final KantenAttribute kantenAttribute;

	@Getter
	private final Richtung fahrtrichtungLinks;

	@Getter
	private final Richtung fahrtrichtungRechts;

	@NonNull
	@Getter
	private final Set<Netzklasse> netzklassen;

	@NonNull
	@Getter
	private final Set<IstStandard> istStandards;

	@NonNull
	private final List<GeschwindigkeitAttribute> geschwindigkeitAttributeNormalisiertAufUeberschneidung;

	@NonNull
	private final List<FuehrungsformAttribute> fuehrungsformAttributeNormalisiertAufUeberschneidung;

	@NonNull
	private final List<ZustaendigkeitAttribute> zustaendigkeitAttributeNormalisiertAufUeberschneidung;

	@NonNull
	@Getter
	private Haendigkeit haendigkeit;

	public KantenSegment(LineareReferenzProjektionsergebnis zielnetzProjektion,
		LineareReferenzProjektionsergebnis quellnetzProjektion,
		Kante quellnetzKante, Kante zielnetzKante) {
		require(zielnetzProjektion, Matchers.notNullValue());
		require(quellnetzProjektion, Matchers.notNullValue());
		require(quellnetzKante, Matchers.notNullValue());

		LineString quellKanteSegment = (LineString) new LocationIndexedLine(quellnetzKante.getGeometry())
			.extractLine(quellnetzProjektion.getAnfang(), quellnetzProjektion.getEnde());

		LineString grundnetzKanteSegment = (LineString) new LocationIndexedLine(zielnetzKante.getGeometry())
			.extractLine(zielnetzProjektion.getAnfang(), zielnetzProjektion.getEnde());

		haendigkeit = KantenSegment.haendigkeitVonKanteZuKante(quellKanteSegment, grundnetzKanteSegment);

		this.quellnetzKante = quellnetzKante;
		kantenAttribute = quellnetzKante.getKantenAttributGruppe().getKantenAttribute();

		netzklassen = quellnetzKante.getKantenAttributGruppe().getNetzklassen();
		istStandards = quellnetzKante.getKantenAttributGruppe().getIstStandards();

		linearReferenzierterAbschnittAufZielnetzKante = zielnetzProjektion.getErgebnisProjektion();
		linearReferenzierterAbschnittAufQuellnetzKante = quellnetzProjektion.getErgebnisProjektion();

		boolean muessenAttributeUmgedrehtWerden = zielnetzProjektion
			.isWurdenVonUndBisBeiProjektionVertauscht() != quellnetzProjektion
			.isWurdenVonUndBisBeiProjektionVertauscht();

		Richtung fahrtrichtungLinks = quellnetzKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks();
		this.fahrtrichtungLinks = muessenAttributeUmgedrehtWerden ? fahrtrichtungLinks.umgedreht() : fahrtrichtungLinks;
		Richtung fahrtrichtungRechts = quellnetzKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts();
		this.fahrtrichtungRechts = muessenAttributeUmgedrehtWerden ?
			fahrtrichtungRechts.umgedreht() : fahrtrichtungRechts;

		List<GeschwindigkeitAttribute> geschwindigkeitAttribute;
		if (muessenAttributeUmgedrehtWerden) {
			geschwindigkeitAttribute = quellnetzKante.getGeschwindigkeitAttributGruppe()
				.getGeschwindigkeitAttribute().stream()
				.map(
					zuDrehendeGeschwindigkeitAttribute -> zuDrehendeGeschwindigkeitAttribute.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung()
						.isPresent() ?
						new GeschwindigkeitAttribute(
							zuDrehendeGeschwindigkeitAttribute.getLinearReferenzierterAbschnitt(),
							zuDrehendeGeschwindigkeitAttribute.getOrtslage().orElse(null),
							zuDrehendeGeschwindigkeitAttribute.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung()
								.get(),
							zuDrehendeGeschwindigkeitAttribute.getHoechstgeschwindigkeit()
						) :
						zuDrehendeGeschwindigkeitAttribute).collect(Collectors.toList());
		} else {
			geschwindigkeitAttribute = quellnetzKante.getGeschwindigkeitAttributGruppe()
				.getImmutableGeschwindigkeitAttribute();
		}

		geschwindigkeitAttributeNormalisiertAufUeberschneidung = projiziereAuschnittLinearReferenzierterAttributeAufSegment(
			geschwindigkeitAttribute,
			quellnetzProjektion.getErgebnisProjektion(), muessenAttributeUmgedrehtWerden);
		fuehrungsformAttributeNormalisiertAufUeberschneidung = projiziereAuschnittLinearReferenzierterAttributeAufSegment(
			quellnetzKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks(),
			quellnetzProjektion.getErgebnisProjektion(), muessenAttributeUmgedrehtWerden);
		zustaendigkeitAttributeNormalisiertAufUeberschneidung = projiziereAuschnittLinearReferenzierterAttributeAufSegment(
			quellnetzKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute(),
			quellnetzProjektion.getErgebnisProjektion(), muessenAttributeUmgedrehtWerden);
	}

	public List<FuehrungsformAttribute> getFuehrungsformAttributeZugeschnittenAufGrundnetzkante() {
		return projiziereLinearReferenzierteAttributeAufZielnetzKante(
			fuehrungsformAttributeNormalisiertAufUeberschneidung);
	}

	public List<ZustaendigkeitAttribute> getZustaendigkeitAttributeZugeschnittenAufGrundnetzkante() {
		return projiziereLinearReferenzierteAttributeAufZielnetzKante(
			zustaendigkeitAttributeNormalisiertAufUeberschneidung);
	}

	public List<GeschwindigkeitAttribute> getGeschwindigkeitAttributeZugeschnittenAufGrundnetzkante() {
		return projiziereLinearReferenzierteAttributeAufZielnetzKante(
			geschwindigkeitAttributeNormalisiertAufUeberschneidung);
	}

	static Haendigkeit haendigkeitVonKanteZuKante(LineString vonKante, LineString zuKante) {
		LocationIndexedLine zuKanteIndexed = new LocationIndexedLine(zuKante);

		double sum = 0.;
		Coordinate[] vonKanteCoordinates = vonKante.getCoordinates();
		for (Coordinate coordinate : vonKanteCoordinates) {
			LinearLocation project = zuKanteIndexed.project(coordinate);
			LineSegment segment = project.getSegment(zuKante);

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

	@SuppressWarnings("unchecked")
	private <T extends LinearReferenzierteAttribute> List<T> projiziereLinearReferenzierteAttributeAufZielnetzKante(
		List<T> attribute) {
		return attribute.stream().map(t -> {
			double segementLaenge = linearReferenzierterAbschnittAufZielnetzKante.getBisValue()
				- linearReferenzierterAbschnittAufZielnetzKante.getVonValue();
			LinearReferenzierterAbschnitt projizierteLinearReferenzierterAbschnitt = LinearReferenzierterAbschnitt
				.of(t.getLinearReferenzierterAbschnitt().getVonValue() * segementLaenge
						+ linearReferenzierterAbschnittAufZielnetzKante.getVonValue(),
					t.getLinearReferenzierterAbschnitt().getBisValue() * segementLaenge
						+ linearReferenzierterAbschnittAufZielnetzKante.getVonValue());

			return (T) t.withLinearReferenzierterAbschnitt(projizierteLinearReferenzierterAbschnitt);
		}).collect(Collectors.toList());
	}

	// Das subset der LinearReferenzierten Attribute der KanteMitZuProjizierendenAttributen für den Ausschnitt der
	// lineareReferenzAufQuellnetzKante ermitteln und diese normalisieren.
	private <T extends LinearReferenzierteAttribute> List<T> projiziereAuschnittLinearReferenzierterAttributeAufSegment(
		List<T> linearReferenzierteAttribute,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnittAufQuellnetzKante,
		boolean muessenAttributeUmgedrehtWerden) {
		return LinearReferenzierteAttribute.projiziereAuschnittLinearReferenzierterAttributeAufLineareReferenz(
			linearReferenzierteAttribute, linearReferenzierterAbschnittAufQuellnetzKante,
			muessenAttributeUmgedrehtWerden);
	}

}
