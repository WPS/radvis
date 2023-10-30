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

package de.wps.radvis.backend.quellimport.ttsib.domain.valueObject;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum TtSibQuerschnittArt {
	FAHRBAHN_GLEISFREI("Fahrbahn (gleisfrei)"),
	HAUPTFAHRSTREIFEN("Hauptfahrstreifen"),
	OFFENE_RINNE("offene Rinne"),
	FAHRBAHN_MIT_3_ODER_MEHR_GLEISEN("Fahrbahn mit 3 oder mehr Gleisen ***"),
	FAHRBAHNTEIL_DER_DEM_SCHIENENVERKEHR_VORBEHALTEN_IST("Fahrbahnteil, der dem Schienenverkehr vorbehalten ist"),
	KRIECHSPUR("Kriechspur"),
	MEHRZWECKSPUR_STAND_UND_FAHRSPUR("Mehrzweckspur (Stand- und Fahrspur) ***"),
	STANDSPUR_PARKSPUR_NICHT_PARKPLATZ("Standspur, Parkspur (nicht Parkplatz) ***"),
	RADWEG_AUCH_RAD_UND_GEHWEG("Radweg (auch Rad- und Gehweg) ***"),
	GEHWEG("Gehweg"),
	UNBEFESTIGTER_SEITENSTREIFEN_BANKETT_EBENES_GELAENDE("unbefestigter Seitenstreifen (Bankett), ebenes Gelände ***"),
	UNBEFESTIGTER_TRENNSTREIFEN_Z_B_MITTELSTREIFEN_SCHUTZSTR(
		"unbefestigter Trennstreifen z.B. Mittelstreifen/Schutzstr.**"),
	BEFESTIGTER_TRENNSTREIFEN_Z_B_MITTELSTREIFEN_INSELN("befestigter Trennstreifen z.B. Mittelstreifen, Inseln ***"),
	TRENNSCHWELLE_TRENNBORD_PLANKE_BAUWERK("Trennschwelle (Trennbord), -planke, -bauwerk"),
	EIGENER_GLEISKOERPER("eigener Gleiskörper"),
	RANDSTREIFEN_LEITSTREIFEN_GETRENNT("Randstreifen (Leitstreifen) - getrennt"),
	RANDSTREIFEN_LEITSTREIFEN_NICHT_GETRENNT("Randstreifen (Leitstreifen) - nicht getrennt"),
	OFFENE_VOLLRINNE_REGELFORM("offene Vollrinne (Regelform) ***"),
	RASENMULDE_BEFESTIGTE_MULDE("Rasenmulde, befestigte Mulde ***"),
	STRAFLENGRABEN("Straﬂengraben"),
	KANTENSTEIN_RABATTENSTEIN("Kantenstein (Rabattenstein)"),
	TIEFBORD_FLACHBORD_PFLASTERSTEIN("Tiefbord (Flachbord), Pflasterstein"),
	SCHRAEGBORD("Schrägbord"),
	HOCHBORD_STEILBORD_HOHLBORD("Hochbord (Steilbord), Hohlbord"),
	DAMMBOESCHUNG_ABFALLENDES_GELAENDE("Dammböschung, abfallendes Gelände"),
	EINSCHNITTSBOESCHUNG_ANSTEIGENDES_GELAENDE("Einschnittsböschung, ansteigendes Gelände"),
	BRUECKENKAPPE_DIENSTGEHWEG("Brückenkappe, Dienstgehweg"),
	DOPPEL_UND_TRENNLINIE("Doppel- und Trennlinie"),
	RAD_UND_GEHWEG_Z_240("Rad- und Gehweg Z 240"),
	RADWEG("Radweg ***"),
	WIRTSCHAFTSWEG_UNBEFESTIGT("Wirtschaftsweg, unbefestigt"),
	WIRTSCHAFTSWEG_BEFESTIGT("Wirtschaftsweg, befestigt"),
	VERZOEGERUNGS_BESCHLEUNIGUNGSSTREIFEN_AN_PARKPLAETZEN_BAB(
		"Verzögerungs-/ Beschleunigungsstreifen an Parkplätzen BAB"),
	BEDARFSFAHRSTREIFEN_IM_KREISVERKEHR("Bedarfsfahrstreifen im Kreisverkehr"),
	SEITENSTREIFEN_BEFESTIGT("Seitenstreifen, befestigt"),
	SEITENSTREIFEN_BEFESTIGT_TEMPORAER_ALS_FAHRSTREIFEN_GENUTZT(
		"Seitenstreifen, befestigt, temporär als Fahrstreifen genutzt"),
	BANKETT("Bankett"),
	SEITENSTREIFEN_UNBEFESTIGT_EBENES_GELAENDE("Seitenstreifen, unbefestigt; ebenes Gelände"),
	MITTELSTREIFEN("Mittelstreifen"),
	MITTELSTREIFENUEBERFAHRT("Mittelstreifenüberfahrt"),
	SEITENTRENNSTREIFEN("Seitentrennstreifen"),
	VERKEHRSINSEL_QUERUNGSHILFE("Verkehrsinsel/Querungshilfe"),
	HALTESTELLENINSEL("Haltestelleninsel"),
	HALTEBUCHT_ALLGEMEIN("Haltebucht allgemein ***"),
	BUSHALTEBUCHT("Bushaltebucht"),
	NOTHALTEBUCHT("Nothaltebucht"),
	MEHRZWECKSTREIFEN_OHNE_FAHRRADBENUTZUNG("Mehrzweckstreifen ohne Fahrradbenutzung"),
	MEHRZWECKSTREIFEN_MIT_FAHRRADBENUTZUNG("Mehrzweckstreifen mit Fahrradbenutzung"),
	PARKSTREIFEN_NICHT_PARKPLATZ("Parkstreifen (nicht Parkplatz)"),
	ZUSATZFAHRSTREIFEN_ZFS("Zusatzfahrstreifen (ZFS)"),
	SONDERFAHRSTREIFEN_Z_B_BUSSE("Sonderfahrstreifen (z. B. Busse)"),
	ANLIEGERFLAECHEN_FLAECHEN_DRITTER("Anliegerflächen (Flächen Dritter)"),
	PARKSTREIFEN_MIT_GRASFLAECHEN_ZWISCHEN_DEN_PARKFELDERN("Parkstreifen mit Grasflächen zwischen den Parkfeldern"),
	MULDE("Mulde"),
	RECHTSABBIEGEFAHRSTREIFEN("Rechtsabbiegefahrstreifen"),
	LINKSABBIEGEFAHRSTREIFEN("Linksabbiegefahrstreifen"),
	KASTENRINNE("Kastenrinne"),
	SCHLITZRINNE("Schlitzrinne"),
	MARKIERUNGS_UND_SPERRFLAECHE("Markierungs- und Sperrfläche"),
	SONSTIGER_STREIFEN_IM_SEITENRAUM("sonstiger Streifen im Seitenraum"),
	RADWEG_Z_237_Z_241_30_Z_241_31("Radweg Z 237, Z 241-30, Z 241-31"),
	ANDERER_RADWEG_Z_250_1022_10("anderer Radweg Z 250 + 1022-10"),
	RADFAHRSTREIFEN_Z_295_MIT_Z_237("Radfahrstreifen Z 295 mit Z 237"),
	KASSELER_BORDE_BUSHALTESTELLE_BARRIEREFREI("Kasseler Borde, Bushaltestelle barrierefrei"),
	KREISINSEL("Kreisinsel");

	@NonNull
	private final String displayText;

	public static TtSibQuerschnittArt fromArtHref(String artHref) {
		if (artHref.equals("#S32E36682FC70452880DDDF4E940D15F1")) {
			return TtSibQuerschnittArt.FAHRBAHN_GLEISFREI;
		}
		if (artHref.equals("#S6DC9A9627A1E4E31BFA0538DB1361FE9")) {
			return TtSibQuerschnittArt.HAUPTFAHRSTREIFEN;
		}
		if (artHref.equals("#S4F043C0C06F84D49953D4DB2A24CF10E")) {
			return TtSibQuerschnittArt.OFFENE_RINNE;
		}
		if (artHref.equals("#S853CF912279F4FD0A7E0CBD6FEF0413F")) {
			return TtSibQuerschnittArt.FAHRBAHN_MIT_3_ODER_MEHR_GLEISEN;
		}
		if (artHref.equals("#E30088917B6342A6BA0EEAAB2D8AD7D0")) {
			return TtSibQuerschnittArt.FAHRBAHNTEIL_DER_DEM_SCHIENENVERKEHR_VORBEHALTEN_IST;
		}
		if (artHref.equals("#S5D4489F550464D93A5D61CD711BA72BD")) {
			return TtSibQuerschnittArt.KRIECHSPUR;
		}
		if (artHref.equals("#C466C3D55B95414591547AE654A113A9")) {
			return TtSibQuerschnittArt.MEHRZWECKSPUR_STAND_UND_FAHRSPUR;
		}
		if (artHref.equals("#S517BEF77E56C46669EE02B1F12AD4ADF")) {
			return TtSibQuerschnittArt.STANDSPUR_PARKSPUR_NICHT_PARKPLATZ;
		}
		if (artHref.equals("#C56B77CA83B041F8BBC6DFDA0CBBE08E")) {
			return TtSibQuerschnittArt.RADWEG_AUCH_RAD_UND_GEHWEG;
		}
		if (artHref.equals("#S3B60224DB14D4469AD40A92DDE9A93DE")) {
			return TtSibQuerschnittArt.GEHWEG;
		}
		if (artHref.equals("#DC7623E8B41E442B993578252116AFA2")) {
			return TtSibQuerschnittArt.UNBEFESTIGTER_SEITENSTREIFEN_BANKETT_EBENES_GELAENDE;
		}
		if (artHref.equals("#F02E005D9EBC495D827657D11CE6A09E")) {
			return TtSibQuerschnittArt.UNBEFESTIGTER_TRENNSTREIFEN_Z_B_MITTELSTREIFEN_SCHUTZSTR;
		}
		if (artHref.equals("#S94C2AD036F014E94B6805051E0A27507")) {
			return TtSibQuerschnittArt.BEFESTIGTER_TRENNSTREIFEN_Z_B_MITTELSTREIFEN_INSELN;
		}
		if (artHref.equals("#EB5871D05F4A4B46B69A8D22BCF124F9")) {
			return TtSibQuerschnittArt.TRENNSCHWELLE_TRENNBORD_PLANKE_BAUWERK;
		}
		if (artHref.equals("#S797C4484F52E4EF7AB1544599193774E")) {
			return TtSibQuerschnittArt.EIGENER_GLEISKOERPER;
		}
		if (artHref.equals("#S611D63C35ACE42E792AEB0C850B21792")) {
			return TtSibQuerschnittArt.RANDSTREIFEN_LEITSTREIFEN_GETRENNT;
		}
		if (artHref.equals("#B1A676652136487CB5F49AB177CD2D0D")) {
			return TtSibQuerschnittArt.RANDSTREIFEN_LEITSTREIFEN_NICHT_GETRENNT;
		}
		if (artHref.equals("#A9FB74EBD23746118361C79760BF029D")) {
			return TtSibQuerschnittArt.OFFENE_VOLLRINNE_REGELFORM;
		}
		if (artHref.equals("#S76079F8799EA4DD49CC375BE2C81FC73")) {
			return TtSibQuerschnittArt.RASENMULDE_BEFESTIGTE_MULDE;
		}
		if (artHref.equals("#C6C8CAE25979421A9C4ABEB7903BF1E1")) {
			return TtSibQuerschnittArt.STRAFLENGRABEN;
		}
		if (artHref.equals("#S4EC81898CE0B40809AFACED4A9685504")) {
			return TtSibQuerschnittArt.KANTENSTEIN_RABATTENSTEIN;
		}
		if (artHref.equals("#A94D0F664AB246B0B9B79DD4E0203DA7")) {
			return TtSibQuerschnittArt.TIEFBORD_FLACHBORD_PFLASTERSTEIN;
		}
		if (artHref.equals("#S3BB727FB7255480F82CE10303288663E")) {
			return TtSibQuerschnittArt.SCHRAEGBORD;
		}
		if (artHref.equals("#S5A0288D1D443490D82408EEF437D9E36")) {
			return TtSibQuerschnittArt.HOCHBORD_STEILBORD_HOHLBORD;
		}
		if (artHref.equals("#S91B3A49B1397476980EE6B9B22F321A8")) {
			return TtSibQuerschnittArt.DAMMBOESCHUNG_ABFALLENDES_GELAENDE;
		}
		if (artHref.equals("#E19D44552564485EA1737E90A4FFC3EE")) {
			return TtSibQuerschnittArt.EINSCHNITTSBOESCHUNG_ANSTEIGENDES_GELAENDE;
		}
		if (artHref.equals("#E11214BCB4AE41EF9AE2B4B4DBC9D08B")) {
			return TtSibQuerschnittArt.BRUECKENKAPPE_DIENSTGEHWEG;
		}
		if (artHref.equals("#S016DB02002EC4C098C604E0E167F5220")) {
			return TtSibQuerschnittArt.DOPPEL_UND_TRENNLINIE;
		}
		if (artHref.equals("#B1EA9C48B36C4D0492502495496EBFD4")) {
			return TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240;
		}
		if (artHref.equals("#S8DA434A5379442849C1E5BFA1BFE2B3B")) {
			return TtSibQuerschnittArt.RADWEG;
		}
		if (artHref.equals("#S38D47F5DDC224B10A2A9E2ED12B89353")) {
			return TtSibQuerschnittArt.WIRTSCHAFTSWEG_UNBEFESTIGT;
		}
		if (artHref.equals("#DA47084A899847A78BEDC60405B35DDD")) {
			return TtSibQuerschnittArt.WIRTSCHAFTSWEG_BEFESTIGT;
		}
		if (artHref.equals("#S970DBB0F549B4EF29F3CE4CCD4959A17")) {
			return TtSibQuerschnittArt.VERZOEGERUNGS_BESCHLEUNIGUNGSSTREIFEN_AN_PARKPLAETZEN_BAB;
		}
		if (artHref.equals("#ff8080823175d5c701324d885e660ac5")) {
			return TtSibQuerschnittArt.BEDARFSFAHRSTREIFEN_IM_KREISVERKEHR;
		}
		if (artHref.equals("#ff80808239e3946c013a9169148b7888")) {
			return TtSibQuerschnittArt.SEITENSTREIFEN_BEFESTIGT;
		}
		if (artHref.equals("#ff80808239e3946c013a916914917889")) {
			return TtSibQuerschnittArt.SEITENSTREIFEN_BEFESTIGT_TEMPORAER_ALS_FAHRSTREIFEN_GENUTZT;
		}
		if (artHref.equals("#ff80808239e3946c013a91691497788a")) {
			return TtSibQuerschnittArt.BANKETT;
		}
		if (artHref.equals("#ff80808239e3946c013a9169149c788b")) {
			return TtSibQuerschnittArt.SEITENSTREIFEN_UNBEFESTIGT_EBENES_GELAENDE;
		}
		if (artHref.equals("#ff80808239e3946c013a916914a1788c")) {
			return TtSibQuerschnittArt.MITTELSTREIFEN;
		}
		if (artHref.equals("#ff80808239e3946c013a916914a6788d")) {
			return TtSibQuerschnittArt.MITTELSTREIFENUEBERFAHRT;
		}
		if (artHref.equals("#ff80808239e3946c013a916914ab788e")) {
			return TtSibQuerschnittArt.SEITENTRENNSTREIFEN;
		}
		if (artHref.equals("#ff80808239e3946c013a916914b1788f")) {
			return TtSibQuerschnittArt.VERKEHRSINSEL_QUERUNGSHILFE;
		}
		if (artHref.equals("#ff80808239e3946c013a916914b57890")) {
			return TtSibQuerschnittArt.HALTESTELLENINSEL;
		}
		if (artHref.equals("#ff8080823e4678ce013e8e499a5a0c9b")) {
			return TtSibQuerschnittArt.HALTEBUCHT_ALLGEMEIN;
		}
		if (artHref.equals("#ff8080823e4678ce013e8e499a5f0c9c")) {
			return TtSibQuerschnittArt.BUSHALTEBUCHT;
		}
		if (artHref.equals("#ff8080823e4678ce013e8e499a640c9d")) {
			return TtSibQuerschnittArt.NOTHALTEBUCHT;
		}
		if (artHref.equals("#ff8080823e4678ce013e8e48531e0c99")) {
			return TtSibQuerschnittArt.MEHRZWECKSTREIFEN_OHNE_FAHRRADBENUTZUNG;
		}
		if (artHref.equals("#ff8080823e4678ce013e8e4853230c9a")) {
			return TtSibQuerschnittArt.MEHRZWECKSTREIFEN_MIT_FAHRRADBENUTZUNG;
		}
		if (artHref.equals("#ff8080824280521c0143e1dc900c4655")) {
			return TtSibQuerschnittArt.PARKSTREIFEN_NICHT_PARKPLATZ;
		}
		if (artHref.equals("#ff8080825231774d0152876f12fb6d80")) {
			return TtSibQuerschnittArt.ZUSATZFAHRSTREIFEN_ZFS;
		}
		if (artHref.equals("#ff8080825231774d0152876f8e666d81")) {
			return TtSibQuerschnittArt.SONDERFAHRSTREIFEN_Z_B_BUSSE;
		}
		if (artHref.equals("#ff8080825231774d0152ab9e58d538b8")) {
			return TtSibQuerschnittArt.ANLIEGERFLAECHEN_FLAECHEN_DRITTER;
		}
		if (artHref.equals("#ff8080823659c9f20137308346f61973")) {
			return TtSibQuerschnittArt.PARKSTREIFEN_MIT_GRASFLAECHEN_ZWISCHEN_DEN_PARKFELDERN;
		}
		if (artHref.equals("#ff8080823b8a7d03013b941d4f6c417d")) {
			return TtSibQuerschnittArt.MULDE;
		}
		if (artHref.equals("#ff808082333b22ed013440a5e03e7633")) {
			return TtSibQuerschnittArt.RECHTSABBIEGEFAHRSTREIFEN;
		}
		if (artHref.equals("#ff808082333b22ed013440a63d6f76a1")) {
			return TtSibQuerschnittArt.LINKSABBIEGEFAHRSTREIFEN;
		}
		if (artHref.equals("#ff8080823e4678ce013e8e46376c0c97")) {
			return TtSibQuerschnittArt.KASTENRINNE;
		}
		if (artHref.equals("#ff8080823e4678ce013e8e4697910c98")) {
			return TtSibQuerschnittArt.SCHLITZRINNE;
		}
		if (artHref.equals("#ff8080823e4678ce013e9cc3c3773bdc")) {
			return TtSibQuerschnittArt.MARKIERUNGS_UND_SPERRFLAECHE;
		}
		if (artHref.equals("#ff8080823e4678ce013e9cc3c37c3bdd")) {
			return TtSibQuerschnittArt.SONSTIGER_STREIFEN_IM_SEITENRAUM;
		}
		if (artHref.equals("#ff8080823e4678ce013e9d31d1be430e")) {
			return TtSibQuerschnittArt.RADWEG_Z_237_Z_241_30_Z_241_31;
		}
		if (artHref.equals("#ff8080823e4678ce013e9d31d1c6430f")) {
			return TtSibQuerschnittArt.ANDERER_RADWEG_Z_250_1022_10;
		}
		if (artHref.equals("#ff8080823e4678ce013e9d31d1ce4310")) {
			return TtSibQuerschnittArt.RADFAHRSTREIFEN_Z_295_MIT_Z_237;
		}
		if (artHref.equals("#S8aaca1355a8c038a015a8f2c4bf85ee5")) {
			return TtSibQuerschnittArt.KASSELER_BORDE_BUSHALTESTELLE_BARRIEREFREI;
		}
		if (artHref.equals("#ff80808263a3bdce0163a55ec0da0210")) {
			return TtSibQuerschnittArt.KREISINSEL;
		}

		throw new RuntimeException("Unbekannter TT-SIB QuerschnittArt: " + artHref);
	}

	@Override
	public String toString() {
		return this.displayText;
	}

	public boolean isRadweg() {
		return this == RADWEG_AUCH_RAD_UND_GEHWEG ||
			this == RAD_UND_GEHWEG_Z_240 ||
			this == RADWEG ||
			this == RADWEG_Z_237_Z_241_30_Z_241_31 ||
			this == MEHRZWECKSTREIFEN_MIT_FAHRRADBENUTZUNG ||
			this == ANDERER_RADWEG_Z_250_1022_10 ||
			this == RADFAHRSTREIFEN_Z_295_MIT_Z_237;
	}
}
