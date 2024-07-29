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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.springframework.test.util.ReflectionTestUtils;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class KantenSegmentTest {

	private static final Comparator<LinearReferenzierterAbschnitt> lineareReferenzComparator = (LR1, LR2) -> {
		if (Math.abs(LR1.getVonValue() - LR2.getVonValue()) + Math.abs(LR1.getBisValue() - LR2.getBisValue()) < 0.002) {
			return 0;
		} else {
			return LinearReferenzierterAbschnitt.vonZuerst.compare(LR1, LR2);
		}
	};

	GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Test
	@SuppressWarnings("unchecked")
	void testeKonstruktor() {
		// Arrange
		LinearReferenzierterAbschnitt linearReferenzierterAbschnittGrundnetzKante = LinearReferenzierterAbschnitt.of(
			0.4, 1);
		LinearReferenzierterAbschnitt linearReferenzierterAbschnittKanteMitZuProjizierendenAttributen = LinearReferenzierterAbschnitt
			.of(
				0.2, 0.6);

		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.INNERORTS)
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_50_KMH)
			.build());

		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0, 0.1),
				BelagArt.ASPHALT,
				Oberflaechenbeschaffenheit.UNBEKANNT,
				Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT,
				KfzParkenForm.PARKBUCHTEN,
				Laenge.of(2),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			),
			new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0.1, 0.4),
				BelagArt.SONSTIGER_BELAG,
				Oberflaechenbeschaffenheit.UNBEKANNT,
				Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT,
				KfzParkenForm.PARKBUCHTEN,
				Laenge.of(2),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			),
			new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0.4, 0.7),
				BelagArt.UNGEBUNDENE_DECKE,
				Oberflaechenbeschaffenheit.UNBEKANNT,
				Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT,
				KfzParkenForm.PARKBUCHTEN,
				Laenge.of(2),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			),
			new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0.7, 1),
				BelagArt.BETONSTEINPFLASTER_PLATTENBELAG,
				Oberflaechenbeschaffenheit.UNBEKANNT,
				Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT,
				KfzParkenForm.PARKBUCHTEN,
				Laenge.of(2),
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			));

		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List
			.of(new ZustaendigkeitAttribute(
				LinearReferenzierterAbschnitt.of(0, 1), null,
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build(),
				null,
				VereinbarungsKennung.of("123")));

		Kante kanteMitZuProjizierendenAttributen = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(
				new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
					.build(), Set.of(Netzklasse.RADNETZ_FREIZEIT), new HashSet<>()))
			.geschwindigkeitAttributGruppe(GeschwindigkeitAttributGruppe
				.builder()
				.geschwindigkeitAttribute(geschwindigkeitAttribute)
				.build())
			.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(fuehrungsformAttribute, false))
			.fahrtrichtungAttributGruppe(new FahrtrichtungAttributGruppe(Richtung.IN_RICHTUNG, false))
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(zustaendigkeitAttribute))
			.build();

		// Act
		KantenSegment kantenSegment = new KantenSegment(
			// muss umgedreht werden
			new LineareReferenzProjektionsergebnis(linearReferenzierterAbschnittGrundnetzKante,
				new LinearLocation(0, 0),
				new LinearLocation(0, 0), true),
			new LineareReferenzProjektionsergebnis(linearReferenzierterAbschnittKanteMitZuProjizierendenAttributen,
				new LinearLocation(0, 0), new LinearLocation(0, 0), false),
			kanteMitZuProjizierendenAttributen, KanteTestDataProvider.withDefaultValues().build());

		// Assert
		List<ZustaendigkeitAttribute> zustaendigkeitAttributeNormalisiertAufSegment = (List<ZustaendigkeitAttribute>) ReflectionTestUtils
			.getField(kantenSegment, "zustaendigkeitAttributeNormalisiertAufUeberschneidung");
		assertThat(zustaendigkeitAttributeNormalisiertAufSegment)
			.extracting(ZustaendigkeitAttribute::getLinearReferenzierterAbschnitt)
			.usingElementComparator(lineareReferenzComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 1));
		assertThat(zustaendigkeitAttributeNormalisiertAufSegment)
			.usingElementComparator((lr1, lr2) -> lr1.sindAttributeGleich(lr2) ? 0 : 1)
			.containsExactlyInAnyOrderElementsOf(zustaendigkeitAttribute);

		List<FuehrungsformAttribute> fuehrungsformAttributeNormalisiertAufSegment = (List<FuehrungsformAttribute>) ReflectionTestUtils
			.getField(kantenSegment, "fuehrungsformAttributeNormalisiertAufUeberschneidung");
		assertThat(fuehrungsformAttributeNormalisiertAufSegment)
			.extracting(FuehrungsformAttribute::getLinearReferenzierterAbschnitt)
			.usingElementComparator(lineareReferenzComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 2. / 4), LinearReferenzierterAbschnitt.of(2. / 4, 1));
		assertThat(fuehrungsformAttributeNormalisiertAufSegment)
			.usingElementComparator((lr1, lr2) -> lr1.sindAttributeGleich(lr2) ? 0 : 1)
			.containsExactlyInAnyOrderElementsOf(fuehrungsformAttribute.subList(1, 3));

		List<GeschwindigkeitAttribute> geschwindigkeitAttributeNormalisiertAufSegment = (List<GeschwindigkeitAttribute>) ReflectionTestUtils
			.getField(kantenSegment, "geschwindigkeitAttributeNormalisiertAufUeberschneidung");
		assertThat(geschwindigkeitAttributeNormalisiertAufSegment)
			.extracting(GeschwindigkeitAttribute::getLinearReferenzierterAbschnitt)
			.usingElementComparator(lineareReferenzComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 1));
		assertThat(geschwindigkeitAttributeNormalisiertAufSegment)
			.usingElementComparator((lr1, lr2) -> lr1.sindAttributeGleich(lr2) ? 0 : 1)
			.containsExactlyInAnyOrderElementsOf(
				//Müssen wir hier neu erstellen, da umgedreht wird und somit die Höchstgeschwindigkeiten vertauscht sind
				List.of(GeschwindigkeitAttribute.builder()
					.ortslage(KantenOrtslage.INNERORTS)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
						Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN)
					.build())
			);

		Set<Netzklasse> netzklassen = kantenSegment
			.getNetzklassen();
		assertThat(netzklassen).containsExactlyInAnyOrder(
			de.wps.radvis.backend.netz.domain.valueObject.Netzklasse.RADNETZ_FREIZEIT);
		// wurde umgedreht
		assertThat(kantenSegment.getFahrtrichtungLinks()).isEqualTo(Richtung.GEGEN_RICHTUNG);
		assertThat(kantenSegment.getFahrtrichtungRechts()).isEqualTo(Richtung.GEGEN_RICHTUNG);
	}

	@Test
	public void testeGetterDerLinearReferenzierteZugeschnittenAufGrundnetzkante() {
		// Arrange
		LinearReferenzierterAbschnitt linearReferenzierterAbschnittGrundnetzKante = LinearReferenzierterAbschnitt.of(
			0.4, 1);
		LinearReferenzierterAbschnitt linearReferenzierterAbschnittKanteMitZuProjizierendenAttributen = LinearReferenzierterAbschnitt
			.of(
				0.2, 0.6);

		GeschwindigkeitAttribute geschwindigkeitAttribute = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.INNERORTS)
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_50_KMH)
			.build();

		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0, 0.1), BelagArt.ASPHALT,
				Oberflaechenbeschaffenheit.UNBEKANNT, Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT, KfzParkenForm.PARKBUCHTEN, Laenge.of(2), Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			),
			new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0.1, 0.4), BelagArt.SONSTIGER_BELAG,
				Oberflaechenbeschaffenheit.UNBEKANNT, Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT, KfzParkenForm.PARKBUCHTEN, Laenge.of(2), Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			),
			new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0.4, 0.7), BelagArt.UNGEBUNDENE_DECKE,
				Oberflaechenbeschaffenheit.UNBEKANNT, Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT, KfzParkenForm.PARKBUCHTEN, Laenge.of(2), Benutzungspflicht.NICHT_VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			),
			new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0.7, 1),
				BelagArt.BETONSTEINPFLASTER_PLATTENBELAG,
				Oberflaechenbeschaffenheit.UNBEKANNT, Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.UNBEKANNT, KfzParkenForm.PARKBUCHTEN, Laenge.of(2), Benutzungspflicht.NICHT_VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			));

		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List
			.of(new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 1), null,
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(12L).build(),
				null,
				VereinbarungsKennung.of("123")));

		Kante kanteMitZuProjizierendenAttributen = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(
				new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build(),
					new HashSet<>(), new HashSet<>()))
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(List.of(geschwindigkeitAttribute))
					.build())
			.fuehrungsformAttributGruppe(new FuehrungsformAttributGruppe(fuehrungsformAttribute, false))
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.zustaendigkeitAttributGruppe(new ZustaendigkeitAttributGruppe(zustaendigkeitAttribute))
			.build();

		KantenSegment kantenSegment = new KantenSegment(
			new LineareReferenzProjektionsergebnis(linearReferenzierterAbschnittGrundnetzKante,
				new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			new LineareReferenzProjektionsergebnis(linearReferenzierterAbschnittKanteMitZuProjizierendenAttributen,
				new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			kanteMitZuProjizierendenAttributen, KanteTestDataProvider.withDefaultValues().build());

		// Act
		List<FuehrungsformAttribute> fuehrungsformAttributeZugeschnittenAufGrundnetzkante = kantenSegment
			.getFuehrungsformAttributeZugeschnittenAufGrundnetzkante();
		List<ZustaendigkeitAttribute> zustaendigkeitAttributeZugeschnittenAufGrundnetzkante = kantenSegment
			.getZustaendigkeitAttributeZugeschnittenAufGrundnetzkante();

		// Assert
		assertThat(fuehrungsformAttributeZugeschnittenAufGrundnetzkante)
			.extracting(FuehrungsformAttribute::getLinearReferenzierterAbschnitt)
			.usingElementComparator(lineareReferenzComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0.4, 0.7),
				LinearReferenzierterAbschnitt.of(0.7, 1));

		assertThat(zustaendigkeitAttributeZugeschnittenAufGrundnetzkante)
			.extracting(ZustaendigkeitAttribute::getLinearReferenzierterAbschnitt)
			.usingElementComparator(lineareReferenzComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0.4, 1.));
	}

	@Nested
	class HaendigkeitTest {

		LineString vertikaleKante;
		LineString horizontaleKante;
		LineString schraegeKante;

		@BeforeEach
		void setUp() {
			vertikaleKante = GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(10, 15),
				new Coordinate(10, 20)
			});

			horizontaleKante = GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(15, 10),
				new Coordinate(20, 10)
			});

			schraegeKante = GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(15, 15),
				new Coordinate(20, 20)
			});
		}

		@Test
		void testeHaendigkeitVonKanteZuKante_IstUnbestimmtFuerGleicheGeometrien() {

			assertThat(KantenSegment.haendigkeitVonKanteZuKante(vertikaleKante, vertikaleKante).wahrscheinlichkeit)
				.isEqualTo(0.);
			assertThat(KantenSegment.haendigkeitVonKanteZuKante(vertikaleKante, vertikaleKante).orientierung)
				.isEqualTo(Haendigkeit.Orientierung.UNBESTIMMT);
			assertThat(KantenSegment.haendigkeitVonKanteZuKante(horizontaleKante, horizontaleKante).wahrscheinlichkeit)
				.isEqualTo(0.);
			assertThat(KantenSegment.haendigkeitVonKanteZuKante(horizontaleKante, horizontaleKante).orientierung)
				.isEqualTo(Haendigkeit.Orientierung.UNBESTIMMT);
			assertThat(KantenSegment.haendigkeitVonKanteZuKante(schraegeKante, schraegeKante).wahrscheinlichkeit)
				.isEqualTo(0.);
			assertThat(KantenSegment.haendigkeitVonKanteZuKante(schraegeKante, schraegeKante).orientierung)
				.isEqualTo(Haendigkeit.Orientierung.UNBESTIMMT);
		}

		@Test
		void testeHaendigkeitVonKanteZuKante_LinksIstLinksUndRechtsIstRechts() {

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(vertikaleKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, 5, 0)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.LINKS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(vertikaleKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, -5, 0)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.RECHTS);

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 5, 0)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.LINKS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, -5, 0)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.RECHTS);

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 0, -5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.LINKS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 0, 5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.RECHTS);

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(horizontaleKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, -5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.LINKS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(horizontaleKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, 5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.RECHTS);
		}

		@Test
		void testeHaendigkeitVonKanteZuKante_UnabhaendigVonVonKanteStationierungsrichtung() {

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(vertikaleKante.reverse(),
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, 5, 0)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.LINKS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(vertikaleKante.reverse(),
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, -5, 0)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.RECHTS);

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante.reverse(),
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 5, 0)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.LINKS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante.reverse(),
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, -5, 0)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.RECHTS);

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante.reverse(),
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 0, -5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.LINKS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante.reverse(),
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 0, 5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.RECHTS);

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(horizontaleKante.reverse(),
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, -5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.LINKS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(horizontaleKante.reverse(),
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, 5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.RECHTS);
		}

		@Test
		void testeHaendigkeitVonKanteZuKante_OrientierungReversedWennZuKanteStationierungsrichtungReversed() {

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(vertikaleKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante.reverse(), 5,
						0)).orientierung)
							.isEqualTo(Haendigkeit.Orientierung.RECHTS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(vertikaleKante.reverse(),
					GeometryTestdataProvider
						.getLinestringVerschobenUmCoordinate(vertikaleKante.reverse(), -5, 0)).orientierung)
							.isEqualTo(Haendigkeit.Orientierung.LINKS);

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante.reverse(), 5,
						0)).orientierung)
							.isEqualTo(Haendigkeit.Orientierung.RECHTS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante.reverse(), -5,
						0)).orientierung)
							.isEqualTo(Haendigkeit.Orientierung.LINKS);

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante.reverse(), 0,
						-5)).orientierung)
							.isEqualTo(Haendigkeit.Orientierung.RECHTS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(schraegeKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante.reverse(), 0,
						5)).orientierung)
							.isEqualTo(Haendigkeit.Orientierung.LINKS);

			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(horizontaleKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, -5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.LINKS);
			assertThat(KantenSegment
				.haendigkeitVonKanteZuKante(horizontaleKante,
					GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, 5)).orientierung)
						.isEqualTo(Haendigkeit.Orientierung.RECHTS);
		}

		@Test
		void testeHaendigkeitVonKanteZuKante_komplizierteresBeispiel() {
			LineString komplizierteKante = GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(12, 12),
				new Coordinate(8, 14),
				new Coordinate(11, 18),
				new Coordinate(12, 22),
			});

			assertThat(KantenSegment.haendigkeitVonKanteZuKante(komplizierteKante, vertikaleKante).orientierung)
				.isEqualTo(Haendigkeit.Orientierung.RECHTS);
			assertThat(KantenSegment.haendigkeitVonKanteZuKante(komplizierteKante, vertikaleKante).wahrscheinlichkeit)
				.isLessThan(0.3);
			assertThat(KantenSegment.haendigkeitVonKanteZuKante(komplizierteKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, -5, 0)).wahrscheinlichkeit)
					.isGreaterThan(0.6);
		}

		@Test
		void haendigkeitRichtigAufgerufenVonkantenSegment() {
			Kante grundnetz = KanteTestDataProvider.withDefaultValues().geometry(
				GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(10, 10),
					new Coordinate(10, 12),
					new Coordinate(10, 14),
					new Coordinate(10, 18),
					new Coordinate(10, 20),
				})).build();
			Kante quellnetzRechts = KanteTestDataProvider.withDefaultValues().geometry(
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(grundnetz.getGeometry(), 2, 2)).build();

			KantenSegment kantenSegmentRechts = new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0.2, 1),
					new LinearLocation(1, 0),
					LinearLocation.getEndLocation(grundnetz.getGeometry()), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 0.8),
					new LinearLocation(0, 0),
					new LinearLocation(3, 0), false),
				quellnetzRechts, grundnetz);

			Kante quellnetzLinks = KanteTestDataProvider.withDefaultValues().geometry(
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(grundnetz.getGeometry(), -2, 2)).build();

			KantenSegment kantenSegmentLinks = new KantenSegment(
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0.2, 1),
					new LinearLocation(1, 0),
					LinearLocation.getEndLocation(grundnetz.getGeometry()), false),
				new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 0.8),
					new LinearLocation(0, 0),
					new LinearLocation(3, 0), false),
				quellnetzLinks, grundnetz);

			assertThat(kantenSegmentRechts.getHaendigkeit().orientierung).isEqualTo(Haendigkeit.Orientierung.RECHTS);
			assertThat(kantenSegmentLinks.getHaendigkeit().orientierung).isEqualTo(Haendigkeit.Orientierung.LINKS);
		}

		@Test
		void testeHaendigkeitVonKanteZuKante_robustAufOutlier() {
			LineString komplizierteKante = GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(10, 10),
				new Coordinate(12, 12),
				new Coordinate(2, 14),
				new Coordinate(11, 18),
				new Coordinate(12, 22),
			});

			assertThat(KantenSegment.haendigkeitVonKanteZuKante(komplizierteKante, vertikaleKante).orientierung)
				.isEqualTo(Haendigkeit.Orientierung.RECHTS);
			assertThat(KantenSegment.haendigkeitVonKanteZuKante(komplizierteKante, vertikaleKante).wahrscheinlichkeit)
				.isLessThan(0.3);
			assertThat(KantenSegment.haendigkeitVonKanteZuKante(komplizierteKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, -6, 0)).wahrscheinlichkeit)
					.isGreaterThan(0.5);
		}

	}

	@Test
	void testeOrderingLinksNachRechts() {
		ArrayList<Haendigkeit> haendigkeiten = new ArrayList<>();
		haendigkeiten.add(new Haendigkeit(-0.8));
		haendigkeiten.add(new Haendigkeit(0.4));
		haendigkeiten.add(new Haendigkeit(-0.2));
		haendigkeiten.add(new Haendigkeit(0.8));
		haendigkeiten.add(new Haendigkeit(0.1));
		haendigkeiten.add(new Haendigkeit(-0.05));
		haendigkeiten.add(new Haendigkeit(-0.9));
		haendigkeiten.add(new Haendigkeit(0.2));

		haendigkeiten.sort(Haendigkeit.vonLinksNachRechts);

		assertThat(haendigkeiten).extracting(Haendigkeit::getOrientierung)
			.containsExactly(Haendigkeit.Orientierung.LINKS, Haendigkeit.Orientierung.LINKS,
				Haendigkeit.Orientierung.LINKS, Haendigkeit.Orientierung.LINKS, Haendigkeit.Orientierung.RECHTS,
				Haendigkeit.Orientierung.RECHTS, Haendigkeit.Orientierung.RECHTS, Haendigkeit.Orientierung.RECHTS);

		assertThat(haendigkeiten).extracting(Haendigkeit::getWahrscheinlichkeit)
			.containsExactly(
				0.8, 0.4, 0.2, 0.1, // links
				0.05, 0.2, 0.8, 0.9 // rechts
			);
	}

}
