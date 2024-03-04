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

package de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.MultiLineString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteFuehrungsformAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteGeschwindigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteZustaendigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenkategorieRIN;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

class NetzToGeoJsonConverterTest {

	private NetzToGeoJsonConverter netzToGeoJsonConverterService;

	private static GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		netzToGeoJsonConverterService = new NetzToGeoJsonConverter();
	}

	@Test
	public void testConvertNetzAusschnitt() {
		// Arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues().id(1L)
			.vonKnoten(KnotenTestDataProvider.withDefaultValues().id(2L).build())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.LGL).id(3L).build())
			.build();

		KanteMapView kanteMapView = new KanteMapView(kante1.getId(), kante1.getGeometry(), null, null, false);

		NetzMapView netzAusschnitt = new NetzMapView(Set.of(kanteMapView),
			Arrays.asList(kante1.getVonKnoten(), kante1.getNachKnoten()));

		// Act
		FeatureCollection result = netzToGeoJsonConverterService.convertNetzAusschnitt(netzAusschnitt);

		// Assert
		List<Feature> features = result.getFeatures();
		assertThat(features).hasSize(3);
		assertThat(features).extracting(Feature::getId).containsExactlyInAnyOrder("1", "2", "3");
	}

	@Test
	public void testConvertNetzAusschnitt_mitVerlauf() {
		// Arrange

		org.locationtech.jts.geom.LineString verlaufLinks = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(20, 3), new Coordinate(10, 10) });
		org.locationtech.jts.geom.LineString verlaufRechts = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(20, 3), new Coordinate(12, 12) });
		org.locationtech.jts.geom.LineString geometrie = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(20, 9), new Coordinate(9, 9) });

		Kante kanteLinksRechts = KanteTestDataProvider.withDefaultValuesAndZweiseitig().geometry(geometrie).id(1L)
			.verlaufLinks(verlaufLinks).verlaufRechts(verlaufRechts)
			.build();

		Kante kanteLinks = KanteTestDataProvider.withDefaultValuesAndZweiseitig().geometry(geometrie).id(2L)
			.verlaufLinks(verlaufLinks)
			.build();

		Kante kanteRechts = KanteTestDataProvider.withDefaultValuesAndZweiseitig().geometry(geometrie).id(3L)
			.verlaufRechts(verlaufRechts)
			.build();

		Kante kanteOhne = KanteTestDataProvider.withDefaultValues().geometry(geometrie).id(4L).build();

		List<KanteMapView> kantenMapView = List.of(kanteLinksRechts, kanteLinks, kanteRechts, kanteOhne).stream()
			.map(kante -> new KanteMapView(kante.getId(), kante.getGeometry(), kante.getVerlaufLinks().orElse(null),
				kante.getVerlaufRechts().orElse(null), false))
			.collect(Collectors.toList());

		NetzMapView netzAusschnitt = new NetzMapView(Set.copyOf(kantenMapView), List.of());

		// Act
		FeatureCollection result = netzToGeoJsonConverterService.convertNetzAusschnitt(netzAusschnitt, true);

		// Assert
		List<Feature> features = result.getFeatures();

		features.sort((f1, f2) -> f1.getId().compareTo(f2.getId()));

		assertThat(((MultiLineString) features.get(0).getGeometry()).getCoordinates().get(0).get(1).getLongitude())
			.isEqualTo(10.0);
		assertThat(((MultiLineString) features.get(0).getGeometry()).getCoordinates().get(1).get(1).getLongitude())
			.isEqualTo(12.0);

		assertThat(((LineString) features.get(1).getGeometry()).getCoordinates().get(1).getLongitude()).isEqualTo(10.0);

		assertThat(((LineString) features.get(2).getGeometry()).getCoordinates().get(1).getLongitude()).isEqualTo(12.0);

		assertThat(((LineString) features.get(3).getGeometry()).getCoordinates().get(1).getLongitude()).isEqualTo(9.0);
	}

	@Test
	public void testConvertZustaendigkeitAttribute() {
		// Arrange
		ZustaendigkeitAttribute atStart = new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 0.3),
			null, null, null, null);
		ZustaendigkeitAttribute atMiddle = new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0.3, 0.4),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build(),
			null,
			VereinbarungsKennung.of("123"));
		ZustaendigkeitAttribute atEnd = new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0.4, 1),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build(),
			null,
			VereinbarungsKennung.of("456"));

		ZustaendigkeitAttributGruppe gruppe = ZustaendigkeitAttributGruppe.builder()
			.zustaendigkeitAttribute(List.of(atStart, atMiddle, atEnd)).id(1L).build();

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.RadNETZ)
			.id(1234L)
			.zustaendigkeitAttributGruppe(gruppe).build();

		KanteZustaendigkeitAttributeView kanteZustaendigkeitAttributeView = new KanteZustaendigkeitAttributeView();
		ReflectionTestUtils.setField(kanteZustaendigkeitAttributeView, "geometry", kante.getGeometry());
		ReflectionTestUtils.setField(kanteZustaendigkeitAttributeView, "id", kante.getId());
		ReflectionTestUtils
			.setField(kanteZustaendigkeitAttributeView, "zustaendigkeitAttributGruppe",
				kante.getZustaendigkeitAttributGruppe());

		// Act
		FeatureCollection result = netzToGeoJsonConverterService
			.convertZustaendigkeitAttributeView(Set.of(kanteZustaendigkeitAttributeView),
				List.of("vereinbarungs_kennung"));

		// Assert
		List<Feature> features = result.getFeatures().stream()
			.sorted(Comparator.comparing(
				feature -> ((LineString) feature.getGeometry()).getCoordinates().get(0).getLongitude()))
			.collect(Collectors.toList());
		assertThat(features).hasSize(3);
		assertThat(features).extracting(p -> p.getProperty("vereinbarungs_kennung")).containsExactlyInAnyOrder(
			null, "123", "456");
		assertThat(features).extracting(p -> p.getProperty(NetzToGeoJsonConverter.KANTE_ID_KEY))
			.containsExactly("1234", "1234", "1234");

		LineString lineString = (LineString) features.get(0).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(0, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(30, 0));

		lineString = (LineString) features.get(1).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(30, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(40, 0));

		lineString = (LineString) features.get(2).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(40, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(100, 0));
	}

	@Test
	public void testConvertFuehrungsformAttribute() {
		// Arrange
		FuehrungsformAttribute atStart = new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0, 0.3),
			BelagArt.ASPHALT, Oberflaechenbeschaffenheit.UNBEKANNT, Bordstein.UNBEKANNT,
			Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND, KfzParkenTyp.PARKEN_VERBOTEN,
			KfzParkenForm.PARKBUCHTEN, Laenge.of(1), Benutzungspflicht.VORHANDEN,
			null,
			null,
			null,
			null,
			TrennstreifenForm.UNBEKANNT,
			TrennstreifenForm.UNBEKANNT
		);
		FuehrungsformAttribute atMiddle = new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0.3, 0.4),
			BelagArt.BETON,
			Oberflaechenbeschaffenheit.UNBEKANNT, Bordstein.UNBEKANNT, Radverkehrsfuehrung.RADFAHRSTREIFEN,
			KfzParkenTyp.PARKEN_VERBOTEN,
			KfzParkenForm.PARKBUCHTEN, Laenge.of(1), Benutzungspflicht.VORHANDEN,
			null,
			null,
			null,
			null,
			TrennstreifenForm.UNBEKANNT,
			TrennstreifenForm.UNBEKANNT
		);
		FuehrungsformAttribute atEnd = new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0.4, 1),
			BelagArt.BETONSTEINPFLASTER_PLATTENBELAG, Oberflaechenbeschaffenheit.UNBEKANNT, Bordstein.UNBEKANNT,
			Radverkehrsfuehrung.SCHUTZSTREIFEN, KfzParkenTyp.PARKEN_VERBOTEN, KfzParkenForm.PARKBUCHTEN,
			Laenge.of(1), Benutzungspflicht.VORHANDEN,
			null,
			null,
			null,
			null,
			TrennstreifenForm.UNBEKANNT,
			TrennstreifenForm.UNBEKANNT
		);

		FuehrungsformAttributGruppe gruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(List.of(atStart, atMiddle, atEnd))
			.fuehrungsformAttributeRechts(List.of(atStart, atMiddle, atEnd)).build();

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.RadNETZ)
			.id(1234l)
			.fuehrungsformAttributGruppe(gruppe).build();

		KanteFuehrungsformAttributeView kanteFuehrungsformAttributeView = new KanteFuehrungsformAttributeView();
		ReflectionTestUtils.setField(kanteFuehrungsformAttributeView, "geometry", kante.getGeometry());
		ReflectionTestUtils.setField(kanteFuehrungsformAttributeView, "id", kante.getId());
		ReflectionTestUtils
			.setField(kanteFuehrungsformAttributeView, "fuehrungsformAttributGruppe",
				kante.getFuehrungsformAttributGruppe());

		// Act
		FeatureCollection result = netzToGeoJsonConverterService
			.convertFuehrungsformAttribute(Set.of(kanteFuehrungsformAttributeView), List.of("radverkehrsfuehrung"));

		// Assert
		List<Feature> features = result.getFeatures().stream()
			.sorted(Comparator.comparing(f -> ((LineString) f.getGeometry()).getCoordinates().get(0).getLongitude()))
			.collect(Collectors.toList());
		assertThat(features).hasSize(3);
		assertThat(features).extracting(p -> p.getProperty("radverkehrsfuehrung")).containsExactly(
			Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND, Radverkehrsfuehrung.RADFAHRSTREIFEN,
			Radverkehrsfuehrung.SCHUTZSTREIFEN);
		assertThat(features).extracting(p -> p.getProperty(NetzToGeoJsonConverter.KANTE_ID_KEY))
			.containsExactly("1234", "1234", "1234");

		LineString lineString = (LineString) features.get(0).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(0, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(30, 0));

		lineString = (LineString) features.get(1).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(30, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(40, 0));

		lineString = (LineString) features.get(2).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(40, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(100, 0));
	}

	@Test
	public void testConvertKantenAttribute() {
		Verwaltungseinheit uebergeordneteOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.fachId(2)
			.name("Landkreis Stuttgart")
			.organisationsArt(OrganisationsArt.KREIS).build();
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().fachId(1)
			.name("Stadt Stuttgart")
			.organisationsArt(OrganisationsArt.GEMEINDE).uebergeordneteOrganisation(uebergeordneteOrganisation).build();

		// Arrange
		KantenAttribute kantenAttribute = KantenAttribute.builder()
			.wegeNiveau(WegeNiveau.FAHRBAHN)
			.beleuchtung(Beleuchtung.VORHANDEN)
			.laengeManuellErfasst(Laenge.of(15))
			.dtvFussverkehr(VerkehrStaerke.of(155))
			.dtvRadverkehr(VerkehrStaerke.of(190))
			.dtvPkw(VerkehrStaerke.of(210))
			.sv(VerkehrStaerke.of(199))
			.kommentar(Kommentar.of("Das ist ein Kommentar"))
			.strassenName(StrassenName.of("Eppendorfer Weg"))
			.strassenNummer(StrassenNummer.of("B273"))
			.gemeinde(organisation)
			.umfeld(Umfeld.UNBEKANNT)
			.strassenkategorieRIN(StrassenkategorieRIN.REGIONAL)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
			.status(Status.defaultWert())
			.build();

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.RadNETZ)
			.kantenAttributGruppe(new KantenAttributGruppe(kantenAttribute,
				Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG), new HashSet<>()))
			.id(1234l)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.build();

		// Act
		FeatureCollection result = netzToGeoJsonConverterService
			.convertKantenAttribute(Set.of(kante), List.of(
				"beleuchtung",
				"umfeld",
				"strassenquerschnittrast06",
				"dtv_radverkehr",
				"landkreis_name",
				"hoechsteNetzklasse",
				"status"));

		// Assert
		List<Feature> features = result.getFeatures();
		assertThat(features).hasSize(1);
		assertThat(features.get(0).getProperties()).hasSize(8);
		assertThat(features).extracting(p -> p.getProperty("beleuchtung")).containsExactly(Beleuchtung.VORHANDEN);
		assertThat(features).extracting(p -> p.getProperty("umfeld")).containsExactly(Umfeld.UNBEKANNT);
		assertThat(features).extracting(p -> p.getProperty("strassenquerschnittrast06"))
			.containsExactly(StrassenquerschnittRASt06.UNBEKANNT);
		assertThat(features).extracting(p -> p.getProperty("dtv_radverkehr")).contains(190);
		assertThat(features).extracting(p -> p.getProperty("landkreis_name"))
			.contains(uebergeordneteOrganisation.getName());
		assertThat(features).extracting(p -> p.getProperty("hoechsteNetzklasse")).containsExactly("Alltag (RadNETZ)");
		assertThat(features).extracting(p -> p.getProperty(NetzToGeoJsonConverter.KANTE_ID_KEY))
			.containsExactly("1234");
		assertThat(features).extracting(p -> p.getProperty("status")).containsExactly(Status.defaultWert());

		LineString lineString = (LineString) features.get(0).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(0, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(100, 0));
	}

	@Test
	public void testConvertKantenAttribute_hoechsteNetzklasseZusammengesetzt() {

		// Arrange

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.RadNETZ)
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.KOMMUNALNETZ_ALLTAG))
				.build())
			.id(1234L)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.build();

		// Act
		FeatureCollection result = netzToGeoJsonConverterService.convertKantenAttribute(Set.of(kante),
			List.of("hoechsteNetzklasse"));

		// Assert
		List<Feature> features = result.getFeatures();
		assertThat(features).hasSize(1);
		assertThat(features.get(0).getProperties()).hasSize(2);
		assertThat(features).extracting(p -> p.getProperty("hoechsteNetzklasse"))
			.containsExactly("Alltag und Freizeit (RadNETZ)");
	}

	@Test
	public void testConvertGeschwindigkeitAttribute() {
		// Arrange
		GeschwindigkeitAttribute atStart = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.INNERORTS)
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_30_KMH)
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.3))
			.build();

		GeschwindigkeitAttribute atMiddle = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.AUSSERORTS)
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_50_KMH)
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.3, 0.4))
			.build();

		GeschwindigkeitAttribute atEnd = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.INNERORTS)
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_70_KMH)
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 1))
			.build();

		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe
			.builder()
			.id(234L)
			.geschwindigkeitAttribute(List.of(atStart, atMiddle, atEnd))
			.build();

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.RadNETZ)
			.id(1234l)
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppe)
			.build();

		KanteGeschwindigkeitAttributeView kanteGeschwindigkeitAttributeView = new KanteGeschwindigkeitAttributeView();
		ReflectionTestUtils.setField(kanteGeschwindigkeitAttributeView, "geometry", kante.getGeometry());
		ReflectionTestUtils.setField(kanteGeschwindigkeitAttributeView, "id", kante.getId());
		ReflectionTestUtils.setField(kanteGeschwindigkeitAttributeView, "geschwindigkeitAttributGruppe",
			kante.getGeschwindigkeitAttributGruppe());

		// Act
		FeatureCollection result = netzToGeoJsonConverterService.convertGeschwindigkeitattribute(
			Set.of(kanteGeschwindigkeitAttributeView),
			List.of("ortslage", "hoechstgeschwindigkeit",
				"abweichende_hoechstgeschwindigkeit_gegen_stationierungsrichtung"));

		// Assert
		// Features nach Reihenfolge der Linestrings sortieren, da die Reihenfolge der GeschwindigkeitAttributGruppen
		// nicht bei jedem Durchlauf gleich ist (werden als Set gespeichert)
		List<Feature> features = result.getFeatures().stream().sorted(Comparator.comparing(
				feature -> ((LineString) feature.getGeometry()).getCoordinates().get(0).getLongitude()))
			.collect(Collectors.toList());

		assertThat(features).hasSize(3);

		assertThat(features).extracting(p -> p.getProperty("ortslage")).containsExactly(
			KantenOrtslage.INNERORTS, KantenOrtslage.AUSSERORTS, KantenOrtslage.INNERORTS);

		assertThat(features).extracting(p -> p.getProperty("hoechstgeschwindigkeit")).containsExactly(
			Hoechstgeschwindigkeit.MAX_20_KMH, Hoechstgeschwindigkeit.MAX_30_KMH, Hoechstgeschwindigkeit.MAX_50_KMH);

		assertThat(features)
			.extracting(p -> p.getProperty("abweichende_hoechstgeschwindigkeit_gegen_stationierungsrichtung"))
			.containsExactly(
				Hoechstgeschwindigkeit.MAX_30_KMH,
				Hoechstgeschwindigkeit.MAX_50_KMH,
				Hoechstgeschwindigkeit.MAX_70_KMH);

		assertThat(features).extracting(p -> p.getProperty(NetzToGeoJsonConverter.KANTE_ID_KEY))
			.containsExactly("1234", "1234", "1234");

		LineString lineString = (LineString) features.get(0).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(0, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(30, 0));

		lineString = (LineString) features.get(1).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(30, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(40, 0));

		lineString = (LineString) features.get(2).getGeometry();
		assertThat(lineString.getCoordinates().get(0)).isEqualTo(new LngLatAlt(40, 0));
		assertThat(lineString.getCoordinates().get(1)).isEqualTo(new LngLatAlt(100, 0));

	}
}
