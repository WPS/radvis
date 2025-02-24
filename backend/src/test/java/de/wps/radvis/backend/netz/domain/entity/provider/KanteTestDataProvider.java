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

package de.wps.radvis.backend.netz.domain.entity.provider;

import static com.google.common.collect.ImmutableList.of;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Kante.KanteBuilder;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;

public class KanteTestDataProvider {

	public final static GeometryFactory FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	public static KanteBuilder withCoordinatesAndQuelle(double x1, double y1, double x2, double y2,
		QuellSystem quelle) {

		Coordinate vonKoordinate = new CoordinateXY(x1, y1);
		Coordinate nachKoordinate = new CoordinateXY(x2, y2);
		LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(new Coordinate[] { vonKoordinate, nachKoordinate });

		KanteBuilder kante = Kante.builder()
			.quelle(quelle)
			.ursprungsfeatureTechnischeID("ursprungId")
			.geometry(lineString)
			.vonKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, quelle).build())
			.nachKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, quelle).build())
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitsAttributeTestDataProvider.gruppeWithGrundnetzDefaultwerte().build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte().build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute().build())
			.aufDlmAbgebildeteGeometry(lineString)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().build());

		if (quelle.equals(QuellSystem.DLM)) {
			kante = kante.dlmId(DlmId.of("dlm-id"));
		}

		return kante;
	}

	public static KanteBuilder withCoordinates(Coordinate[] coordinates) {
		Coordinate vonKoordinate = coordinates[0];
		Coordinate nachKoordinate = coordinates[coordinates.length - 1];
		LineString lineString = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(coordinates);

		KanteBuilder kante = withDefaultValues()
			.vonKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, QuellSystem.DLM).build())
			.nachKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, QuellSystem.DLM).build())
			.geometry(lineString);

		return kante;
	}

	public static KanteBuilder createWithValues(Gebietskoerperschaft gemeinde) {
		return KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttributeTestDataProvider.createWithValues(gemeinde).build())
				.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_ALLTAG))
				.istStandards(Set.of(IstStandard.BASISSTANDARD, IstStandard.RADSCHNELLVERBINDUNG))
				.build())
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppeTestDataProvider
					.withAttribute(GeschwindigkeitsAttributeTestDataProvider.createWithValues()).build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider
					.withAttribute(FuehrungsformAttributeTestDataProvider.createWithValues()).build())
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withAttribute(
					ZustaendigkeitAttributGruppeTestDataProvider.createWithValues(gemeinde))
					.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider.createWithValues().build());
	}

	public static KanteBuilder createZweiseitigWithValues(Gebietskoerperschaft gemeinde) {
		return KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttributeTestDataProvider.createWithValues(gemeinde).build())
				.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_ALLTAG))
				.istStandards(Set.of(IstStandard.BASISSTANDARD, IstStandard.RADSCHNELLVERBINDUNG))
				.build())
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppeTestDataProvider
					.withAttribute(GeschwindigkeitsAttributeTestDataProvider.createWithValues()).build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider
					.withAttribute(FuehrungsformAttributeTestDataProvider.createWithValues()).isZweiseitig(true)
					.build())
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withAttribute(
					ZustaendigkeitAttributGruppeTestDataProvider.createWithValues(gemeinde))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppeTestDataProvider.createWithValues().isZweiseitig(true).build());
	}

	public static KanteBuilder withDefaultValues() {
		return withDefaultValuesAndQuelle(QuellSystem.DLM);
	}

	public static KanteBuilder withDefaultValuesAndZweiseitig() {
		return withDefaultValuesAndQuelle(QuellSystem.DLM)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build());
	}

	public static KanteBuilder withDefaultValuesAndQuelle(QuellSystem quelle) {
		return withCoordinatesAndQuelle(0, 0, 100, 100, quelle);
	}

	public static KanteBuilder fromKnoten(Knoten vonKnoten, Knoten bisKnoten) {
		return fromKnotenUndQuelle(vonKnoten, bisKnoten, vonKnoten.getQuelle());
	}

	public static KanteBuilder fromKnotenUndQuelle(Knoten vonKnoten, Knoten bisKnoten, QuellSystem quelle) {
		Coordinate[] coordinates = new Coordinate[2];
		coordinates[0] = vonKnoten.getKoordinate();
		coordinates[1] = bisKnoten.getKoordinate();

		GeometryFactory etrs89Utm32NGeometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N
			.getGeometryFactory();
		LineString lineString = etrs89Utm32NGeometryFactory.createLineString(coordinates);

		return KanteTestDataProvider.withDefaultValuesAndQuelle(quelle)
			.vonKnoten(vonKnoten)
			.nachKnoten(bisKnoten)
			.geometry(lineString)
			.isZweiseitig(false)
			.kantenAttributGruppe(KantenAttributGruppe.builder().build())
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder().build())
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe.builder().build());
	}

	public static List<Kante> createStreckeUeberCoordinates(List<Coordinate[]> coordinatesDerKanten, Knoten startKnoten,
		Knoten endKnoten,
		AtomicLong currentKnotenId, AtomicLong currentKanteId) {

		Knoten vorherigerKnoten = KnotenTestDataProvider
			.withCoordinateAndQuelle(coordinatesDerKanten.get(0)[0], QuellSystem.DLM)
			.id(currentKnotenId.incrementAndGet()).build();

		if (startKnoten != null) {
			vorherigerKnoten = startKnoten;
		}

		List<Kante> result = new ArrayList<>();
		for (int i = 0, coordinatesDerKantenSize = coordinatesDerKanten.size(); i < coordinatesDerKantenSize; i++) {
			Coordinate[] coordinates = coordinatesDerKanten.get(i);

			Knoten nachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(coordinates[coordinates.length - 1], QuellSystem.DLM)
				.id(currentKnotenId.incrementAndGet()).build();

			if (i == coordinatesDerKantenSize - 1 && endKnoten != null) {
				nachKnoten = endKnoten;
			}

			Kante next = KanteTestDataProvider.withDefaultValues()
				.id(currentKanteId.incrementAndGet())
				.geometry(FACTORY.createLineString(coordinates))
				.vonKnoten(vorherigerKnoten)
				.nachKnoten(nachKnoten).build();
			vorherigerKnoten = next.getNachKnoten();

			result.add(next);
		}

		return result;
	}

	public static KanteBuilder withFahrtrichtung(double x1, double y1, double x2, double y2, Richtung fahrtrichtung) {
		return KanteTestDataProvider.withCoordinatesAndQuelle(x1, y1, x2, y2, QuellSystem.DLM)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.fahrtrichtungLinks(fahrtrichtung).fahrtrichtungRechts(fahrtrichtung).build());
	}

	public static KanteBuilder withFahrtrichtungFromKnoten(Knoten von, Knoten bis, Richtung fahrtrichtung) {
		return KanteTestDataProvider.fromKnoten(von, bis)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.fahrtrichtungLinks(fahrtrichtung).fahrtrichtungRechts(fahrtrichtung).build());
	}

	public static KanteBuilder withRichtungRadverkehrsfuehrungIstStandardBreiteQuellsystem(Richtung richtung,
		Radverkehrsfuehrung radverkehrsfuehrung, IstStandard istStandard, double breite, QuellSystem quellSystem,
		boolean isZweiseitig) {
		return KanteTestDataProvider.withDefaultValuesAndQuelle(quellSystem)
			.isZweiseitig(isZweiseitig)
			.kantenAttributGruppe(
				KantenAttributGruppeTestDataProvider.defaultValue()
					.istStandards(Set.of(istStandard))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(isZweiseitig)
					.fahrtrichtungLinks(richtung)
					.fahrtrichtungRechts(richtung)
					.build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(isZweiseitig)
					.fuehrungsformAttributeLinks(of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.radverkehrsfuehrung(radverkehrsfuehrung)
							.breite(Laenge.of(breite))
							.build()))
					.fuehrungsformAttributeRechts(of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.radverkehrsfuehrung(radverkehrsfuehrung)
							.breite(Laenge.of(breite))
							.build()))
					.build());
	}

	public static Kante.KanteBuilder withCoordinatesAndRadverkehrsfuehrung(double x1, double y1, double x2, double y2,
		Radverkehrsfuehrung radverkehrsfuehrung) {
		return withCoordinatesAndRadverkehrsfuehrung(new Coordinate[] { new Coordinate(x1, y1),
			new Coordinate(x2, y2) },
			radverkehrsfuehrung);
	}

	public static Kante.KanteBuilder withCoordinatesAndRadverkehrsfuehrung(Coordinate[] coordinates,
		Radverkehrsfuehrung radverkehrsfuehrung) {
		Kante.KanteBuilder kanteBuilder = KanteTestDataProvider.withCoordinates(coordinates);
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformBuilder = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte().radverkehrsfuehrung(radverkehrsfuehrung);
		return kanteBuilder.fuehrungsformAttributGruppe(
			FuehrungsformAttributGruppe.builder()
				.fuehrungsformAttributeRechts(List.of(fuehrungsformBuilder.build()))
				.fuehrungsformAttributeLinks(List.of(fuehrungsformBuilder.build()))
				.build());
	}
}
