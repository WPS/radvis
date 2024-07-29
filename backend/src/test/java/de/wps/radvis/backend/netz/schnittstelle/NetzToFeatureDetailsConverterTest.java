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

package de.wps.radvis.backend.netz.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.view.AttributeView;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.schnittstelle.view.KanteDetailView;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@SuppressWarnings("deprecation")
public class NetzToFeatureDetailsConverterTest {

	private NetzToFeatureDetailsConverter netzToFeatureDetailsConverter;

	@BeforeEach
	public void setup() {
		netzToFeatureDetailsConverter = new NetzToFeatureDetailsConverter();
	}

	@Test
	public void testConvertKanteToKanteDetailView_kanteEinseitig() {
		// Arrange
		double x1 = 100;
		double y1 = 0;
		double x2 = 121;
		double y2 = 0;

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppe.builder()
			.kantenAttribute(
				KantenAttribute.builder()
					.dtvPkw(VerkehrStaerke.of(42))
					.kommentar(Kommentar.of("Anmerkungen"))
					.gemeinde(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Tuttlingen")
							.organisationsArt(OrganisationsArt.BUNDESLAND)
							.build())
					.beleuchtung(Beleuchtung.VORHANDEN)
					.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
					.umfeld(Umfeld.UNBEKANNT)
					.status(Status.defaultWert())
					.build())
			.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
			.build();

		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(
				List.of(GeschwindigkeitAttribute.builder()
					.ortslage(KantenOrtslage.INNERORTS)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_40_KMH)
					.build()))
			.build();
		ArrayList<FuehrungsformAttribute> fuehrungsformAttribute = new ArrayList<>(
			List.of(new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0, 1),
				BelagArt.SONSTIGER_BELAG,
				Oberflaechenbeschaffenheit.NEUWERTIG,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.LAENGS_PARKEN,
				KfzParkenForm.FAHRBAHNPARKEN_MARKIERT,
				null,
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			)));
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(x1, y1, x2, y2, QuellSystem.LGL)
			.id(1337L)
			.kantenAttributGruppe(kantenAttributGruppe)
			.isZweiseitig(false)
			.fahrtrichtungAttributGruppe(
				new FahrtrichtungAttributGruppe(Richtung.GEGEN_RICHTUNG, false))
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder()
				.zustaendigkeitAttribute(new ArrayList<>(List.of(
					new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 1), null,
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Freiburg").build(),
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Stuttgart").build(),
						null))))
				.build())
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppe)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(false)
				.fuehrungsformAttributeLinks(
					fuehrungsformAttribute)
				.fuehrungsformAttributeRechts(
					fuehrungsformAttribute)
				.build())
			.build();

		// act
		KanteDetailView kanteDetailViewLinks = netzToFeatureDetailsConverter.convertKantetoKanteDetailView(kante,
			kante.getGeometry().getStartPoint().getCoordinate(), "LINKS");
		KanteDetailView kanteDetailViewRechts = netzToFeatureDetailsConverter.convertKantetoKanteDetailView(kante,
			kante.getGeometry().getStartPoint().getCoordinate(), "RECHTS");
		// assert

		assertThat(kanteDetailViewRechts).isEqualTo(kanteDetailViewLinks);

		assertThat(kanteDetailViewLinks.getSeite()).isNull();

		assertThat(kanteDetailViewLinks.getAttributeAnPosition())
			// Geschwindigkeit
			.containsEntry("Höchstgeschwindigkeit", "30 km/h")
			.containsEntry("Abweichende Höchstgeschwindigkeit in Gegenrichtung", "40 km/h")

			// Führungsform
			.containsEntry("Ortslage", "Innerorts")
			.containsEntry("Belagart", "Sonstiger Belag")
			.containsEntry("Oberflächenbeschaffenheit", "Neuwertig")
			.containsEntry("Bordstein", "Keine Absenkung")
			.containsEntry("Radverkehrsführung", "Geh-/Radweg gemeinsam (straßenbegleitend)")
			.containsEntry("Kfz-Parken-Form", "Fahrbahnparken (markiert)")
			.containsEntry("Kfz-Parken-Typ", "Parken in Längsaufstellung")
			.containsEntry("Breite", null)
			.containsEntry("Benutzungspflicht", "Vorhanden")

			// Zustaendigkeit
			.containsEntry("Baulastträger", null)
			.containsEntry("Vereinbarungskennung", null)
			.containsEntry("Unterhaltszuständiger", "Freiburg")
			.containsEntry("Erhaltszuständiger", "Stuttgart")
			.hasSize(15);

		assertThat(kanteDetailViewLinks.getAttributeAufGanzerLaenge())
			// gemappte Attribute haben Werte
			.containsEntry("ID", "1337")
			.containsEntry("Länge (berechnet)", "21,00 m")
			.containsEntry("Kommentar", "Anmerkungen")
			.containsEntry("Umfeld", "Unbekannt")
			.containsEntry("DTV PKW", "42 Fz/Tag")
			.containsEntry("Straßenquerschnitte nach RASt 06", "Unbekannt")
			.containsEntry("Gemeinde", "Tuttlingen")
			.containsEntry("Beleuchtung", "Vorhanden")
			.containsEntry("Status", "unter Verkehr")
			.containsEntry("Quelle", QuellSystem.LGL.toString())
			.containsEntry("Richtung", "Gegen Stationierungsrichtung")

			// Netzklassen
			.containsEntry("Netzklassen", Netzklasse.RADNETZ_ALLTAG.toString())
			.containsEntry("Ist-Standards", "")

			// Attribute, die nicht gemappt werden, sind NULL
			.containsEntry("Wegeniveau", null)
			.containsEntry("Straßenkategorie nach RIN", null)
			.containsEntry("Länge (manuell)", null)
			.containsEntry("DTV Fußverkehr", null)
			.containsEntry("DTV Radverkehr", null)
			.containsEntry("SV", null)
			.containsEntry("Straßenname", null)
			.containsEntry("Straßennummer", null)
			.containsEntry("Landkreis", null)
			.hasSize(22);
	}

	@Test
	public void testConvertKanteToKanteDetailView_kanteZweiseitig() {
		// Arrange
		double x1 = 100;
		double y1 = 0;
		double x2 = 121;
		double y2 = 0;

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppe.builder()
			.kantenAttribute(
				KantenAttribute.builder()
					.dtvPkw(VerkehrStaerke.of(42))
					.kommentar(Kommentar.of("Anmerkungen"))
					.gemeinde(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Tuttlingen")
							.organisationsArt(OrganisationsArt.BUNDESLAND)
							.build())
					.beleuchtung(Beleuchtung.VORHANDEN)
					.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
					.umfeld(Umfeld.UNBEKANNT)
					.status(Status.defaultWert())
					.build())
			.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
			.build();

		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(
				List.of(GeschwindigkeitAttribute.builder()
					.ortslage(KantenOrtslage.INNERORTS)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_40_KMH)
					.build()))
			.build();
		ArrayList<FuehrungsformAttribute> fuehrungsformAttribute = new ArrayList<>(
			List.of(new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0, 1),
				BelagArt.SONSTIGER_BELAG,
				Oberflaechenbeschaffenheit.NEUWERTIG,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.LAENGS_PARKEN,
				KfzParkenForm.FAHRBAHNPARKEN_MARKIERT,
				null,
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			)));
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(x1, y1, x2, y2, QuellSystem.LGL)
			.id(1337L)
			.kantenAttributGruppe(kantenAttributGruppe)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(
				new FahrtrichtungAttributGruppe(Richtung.GEGEN_RICHTUNG, Richtung.IN_RICHTUNG, true))
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder()
				.zustaendigkeitAttribute(new ArrayList<>(List.of(
					new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 1), null,
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Freiburg").build(),
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Stuttgart").build(),
						null))))
				.build())
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppe)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true)
				.fuehrungsformAttributeLinks(
					fuehrungsformAttribute)
				.fuehrungsformAttributeRechts(
					fuehrungsformAttribute)
				.build())
			.build();

		// act
		KanteDetailView kanteDetailViewLinks = netzToFeatureDetailsConverter.convertKantetoKanteDetailView(kante,
			kante.getGeometry().getStartPoint().getCoordinate(), "LINKS");
		KanteDetailView kanteDetailViewRechts = netzToFeatureDetailsConverter.convertKantetoKanteDetailView(kante,
			kante.getGeometry().getStartPoint().getCoordinate(), "RECHTS");

		// assert
		assertThat(kanteDetailViewRechts).isNotEqualTo(kanteDetailViewLinks);

		assertThat(kanteDetailViewLinks.getSeite()).isEqualTo("LINKS");
		assertThat(kanteDetailViewRechts.getSeite()).isEqualTo("RECHTS");

		for (KanteDetailView kanteDetailView : List.of(kanteDetailViewLinks, kanteDetailViewRechts)) {
			assertThat(kanteDetailView.getAttributeAnPosition())
				// Geschwindigkeit
				.containsEntry("Höchstgeschwindigkeit", "30 km/h")
				.containsEntry("Abweichende Höchstgeschwindigkeit in Gegenrichtung", "40 km/h")

				// Führungsform
				.containsEntry("Ortslage", "Innerorts")
				.containsEntry("Belagart", "Sonstiger Belag")
				.containsEntry("Oberflächenbeschaffenheit", "Neuwertig")
				.containsEntry("Bordstein", "Keine Absenkung")
				.containsEntry("Radverkehrsführung", "Geh-/Radweg gemeinsam (straßenbegleitend)")
				.containsEntry("Kfz-Parken-Form", "Fahrbahnparken (markiert)")
				.containsEntry("Kfz-Parken-Typ", "Parken in Längsaufstellung")
				.containsEntry("Breite", null)
				.containsEntry("Benutzungspflicht", "Vorhanden")

				// Zustaendigkeit
				.containsEntry("Baulastträger", null)
				.containsEntry("Vereinbarungskennung", null)
				.containsEntry("Unterhaltszuständiger", "Freiburg")
				.containsEntry("Erhaltszuständiger", "Stuttgart")
				.hasSize(15);

			assertThat(kanteDetailView.getAttributeAufGanzerLaenge())
				// gemappte Attribute haben Werte
				.containsEntry("ID", "1337")
				.containsEntry("Länge (berechnet)", "21,00 m")
				.containsEntry("Kommentar", "Anmerkungen")
				.containsEntry("Umfeld", "Unbekannt")
				.containsEntry("DTV PKW", "42 Fz/Tag")
				.containsEntry("Straßenquerschnitte nach RASt 06", "Unbekannt")
				.containsEntry("Gemeinde", "Tuttlingen")
				.containsEntry("Beleuchtung", "Vorhanden")
				.containsEntry("Status", "unter Verkehr")
				.containsEntry("Quelle", QuellSystem.LGL.toString())

				// Netzklassen
				.containsEntry("Netzklassen", Netzklasse.RADNETZ_ALLTAG.toString())
				.containsEntry("Ist-Standards", "")

				// Attribute, die nicht gemappt werden, sind NULL
				.containsEntry("Wegeniveau", null)
				.containsEntry("Straßenkategorie nach RIN", null)
				.containsEntry("Länge (manuell)", null)
				.containsEntry("DTV Fußverkehr", null)
				.containsEntry("DTV Radverkehr", null)
				.containsEntry("SV", null)
				.containsEntry("Straßenname", null)
				.containsEntry("Straßennummer", null)
				.containsEntry("Landkreis", null)
				.hasSize(22);
		}
		assertThat(kanteDetailViewLinks.getAttributeAufGanzerLaenge())
			.containsEntry("Richtung", "Gegen Stationierungsrichtung");
		assertThat(kanteDetailViewRechts.getAttributeAufGanzerLaenge())
			.containsEntry("Richtung", "In Stationierungsrichtung");
	}

	@Test
	public void testConvertKanteToFeatureDetails() {
		// Arrange
		double x1 = 100;
		double y1 = 0;
		double x2 = 121;
		double y2 = 0;

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppe.builder()
			.kantenAttribute(
				KantenAttribute.builder()
					.dtvPkw(VerkehrStaerke.of(42))
					.kommentar(Kommentar.of("Anmerkungen"))
					.gemeinde(
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Tuttlingen")
							.organisationsArt(OrganisationsArt.BUNDESLAND)
							.build())
					.beleuchtung(Beleuchtung.VORHANDEN)
					.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
					.umfeld(Umfeld.UNBEKANNT)
					.status(Status.defaultWert())
					.build())
			.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
			.build();

		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(
				List.of(GeschwindigkeitAttribute.builder()
					.ortslage(KantenOrtslage.INNERORTS)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
					.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_40_KMH)
					.build()))
			.build();
		ArrayList<FuehrungsformAttribute> fuehrungsformAttribute = new ArrayList<>(
			List.of(new FuehrungsformAttribute(LinearReferenzierterAbschnitt.of(0, 1),
				BelagArt.SONSTIGER_BELAG,
				Oberflaechenbeschaffenheit.NEUWERTIG,
				Bordstein.KEINE_ABSENKUNG,
				Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
				KfzParkenTyp.LAENGS_PARKEN,
				KfzParkenForm.FAHRBAHNPARKEN_MARKIERT,
				null,
				Benutzungspflicht.VORHANDEN,
				null,
				null,
				null,
				null,
				TrennstreifenForm.UNBEKANNT,
				TrennstreifenForm.UNBEKANNT
			)));
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(x1, y1, x2, y2, QuellSystem.LGL)
			.id(1337L)
			.kantenAttributGruppe(kantenAttributGruppe)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(
				new FahrtrichtungAttributGruppe(Richtung.GEGEN_RICHTUNG, Richtung.IN_RICHTUNG, true))
			.zustaendigkeitAttributGruppe(ZustaendigkeitAttributGruppe.builder()
				.zustaendigkeitAttribute(new ArrayList<>(List.of(
					new ZustaendigkeitAttribute(LinearReferenzierterAbschnitt.of(0, 1), null,
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Freiburg").build(),
						VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Stuttgart").build(),
						null))))
				.build())
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppe)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true)
				.fuehrungsformAttributeLinks(
					fuehrungsformAttribute)
				.fuehrungsformAttributeRechts(
					fuehrungsformAttribute)
				.build())
			.build();

		// act
		List<AttributeView> properties = netzToFeatureDetailsConverter.convertKanteToFeatureDetails(kante,
			kante.getGeometry().getStartPoint().getCoordinate());

		List<AttributeView> links = netzToFeatureDetailsConverter.convertKanteToFeatureDetails(kante,
			kante.getGeometry().getStartPoint().getCoordinate(), KantenSeite.LINKS);

		List<AttributeView> rechts = netzToFeatureDetailsConverter.convertKanteToFeatureDetails(kante,
			kante.getGeometry().getStartPoint().getCoordinate(), KantenSeite.RECHTS);

		// assert
		assertThat(properties).isEqualTo(links);
		assertThat(properties).isNotEqualTo(rechts);

		assertThat(extractRichtung(links)).isEqualTo("Gegen Stationierungsrichtung");
		assertThat(extractRichtung(rechts)).isEqualTo("In Stationierungsrichtung");

		Map<String, String> propertyMap = new HashMap<>();
		properties.forEach(a -> propertyMap.put(a.getKey(), a.getValue()));

		assertThat(propertyMap)
			// gemappte Attribute haben Werte
			.containsEntry("Ortslage", "Innerorts")
			.containsEntry("Richtung", "Gegen Stationierungsrichtung")
			.containsEntry("Länge (berechnet)", "21,00 m")
			.containsEntry("Kommentar", "Anmerkungen")
			.containsEntry("DTV PKW", "42 Fz/Tag")
			.containsEntry("Gemeinde", "Tuttlingen")
			.containsEntry("Beleuchtung", "Vorhanden")
			.containsEntry("Status", "unter Verkehr")

			// Attribute, die nicht gemappt werden, sind NULL
			.containsEntry("Wegeniveau", null)
			.containsEntry("Straßenkategorie nach RIN", null)
			.containsEntry("Länge (manuell)", null)
			.containsEntry("DTV Fußverkehr", null)
			.containsEntry("DTV Radverkehr", null)
			.containsEntry("SV", null)
			.containsEntry("Vereinbarungskennung", null)
			.containsEntry("Straßenname", null)
			.containsEntry("Straßennummer", null)
			.containsEntry("Baulastträger", null)
			.containsEntry("Landkreis", null)

			// Geschwindigkeit
			.containsEntry("Höchstgeschwindigkeit", "30 km/h")
			.containsEntry("Abweichende Höchstgeschwindigkeit in Gegenrichtung", "40 km/h")

			// Führungsform
			.containsEntry("Belagart", "Sonstiger Belag")
			.containsEntry("Oberflächenbeschaffenheit", "Neuwertig")
			.containsEntry("Bordstein", "Keine Absenkung")
			.containsEntry("Radverkehrsführung", "Geh-/Radweg gemeinsam (straßenbegleitend)")
			.containsEntry("Umfeld", "Unbekannt")
			.containsEntry("Straßenquerschnitte nach RASt 06", "Unbekannt")
			.containsEntry("Kfz-Parken-Form", "Fahrbahnparken (markiert)")
			.containsEntry("Kfz-Parken-Typ", "Parken in Längsaufstellung")
			.containsEntry("Breite", null)
			.containsEntry("Benutzungspflicht", "Vorhanden")

			// Zustaendigkeit
			.containsEntry("Baulastträger", null)
			.containsEntry("Vereinbarungskennung", null)
			.containsEntry("Unterhaltszuständiger", "Freiburg")
			.containsEntry("Erhaltszuständiger", "Stuttgart")

			// Netzklassen
			.containsEntry("Netzklassen", Netzklasse.RADNETZ_ALLTAG.toString())

			.hasSize(37);
	}

	private String extractRichtung(List<AttributeView> propertiesSeiteLinks) {
		Optional<AttributeView> richtungAttribut = propertiesSeiteLinks.stream()
			.filter(attributeView -> "Richtung".equals(attributeView.getKey()))
			.findFirst();
		assertThat(richtungAttribut).isPresent();
		return richtungAttribut.get().getValue();
	}

	@Test
	public void testConvertKanteToFeatureDetails_mitLinearerReferenz() {
		// Arrange
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(List.of(
				GeschwindigkeitAttribute.builder()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
					.build(),
				GeschwindigkeitAttribute.builder()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
					.build()))
			.build();

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 200, 0, QuellSystem.LGL)
			.id(1234L)
			.geschwindigkeitAttributGruppe(geschwindigkeitAttributGruppe)
			.build();

		// act
		List<AttributeView> properties1 = netzToFeatureDetailsConverter.convertKanteToFeatureDetails(kante,
			new Coordinate(125D, 0D)); // Punkt auf vorderem Segment
		List<AttributeView> properties2 = netzToFeatureDetailsConverter.convertKanteToFeatureDetails(kante,
			new Coordinate(175D, 0D)); // Punkt auf Hinterem Segment
		List<AttributeView> propertiesMitte = netzToFeatureDetailsConverter.convertKanteToFeatureDetails(kante,
			new Coordinate(150D, 0D)); // Punkt genau in der Mitte, wo sich beide Segmente Treffen

		// assert
		assertThat(properties1).anyMatch(
			propertie -> propertie.getKey().equals("Höchstgeschwindigkeit") && propertie.getValue().equals("30 km/h"));
		assertThat(properties2).anyMatch(
			propertie -> propertie.getKey().equals("Höchstgeschwindigkeit") && propertie.getValue().equals("50 km/h"));
		// Dort, wo sich beide Segmente Treffen werden die daten des Vorderen Segmentes zurück gegeben
		assertThat(propertiesMitte).containsExactlyInAnyOrderElementsOf(properties1);

	}

}
