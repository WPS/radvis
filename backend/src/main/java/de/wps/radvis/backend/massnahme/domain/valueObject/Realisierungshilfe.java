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

package de.wps.radvis.backend.massnahme.domain.valueObject;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Realisierungshilfe {
	NR_11_1_1("11.1-1 Markierung des Sinnbildes \"Fahrrad\""),
	NR_11_1_2("11.1-2 Markierung des Sinnbildes \"Fahrrad\" im RadNETZ"),
	NR_2_2_1("2.2-1 Sichtfelder an Knotenpunkten und Querungsstellen"),
	NR_3_2_1("3.2-1 Markierung beidseitiger Schutzstreifen"),
	NR_3_2_2("3.2-2 Furtmarkierung im Zuge von Schutzstreifen an Einmündungen"),
	NR_3_2_3("3.2-3 Furtmarkierung im Zuge von Schutzstreifen an Zufahrt"),
	NR_3_2_4("3.2-4 Furtmarkierung bei Schutzstreifen und Radfahren im Seitenraum (1)"),
	NR_3_2_5("3.2-5 Furtmarkierung bei Schutzstreifen und Radfahren im Seitenraum (2)"),
	NR_3_2_6("3.2-6 Beidseitige Schutzstreifen mit schmaler Kernfahrbahn"),
	NR_3_2_7("3.2-7 Markierung einseitiger Schutzstreifen"),
	NR_3_3_1("3.3-1 Markierung beidseitiger Radfahrstreifen"),
	NR_3_3_2("3.3-2 Furtmarkierung im Zuge von Radfahrstreifen"),
	NR_3_4_1("3.4-1 Anlage beidseitiger Radwege"),
	NR_3_4_2("3.4-2 Baulich geschütztes Radwegende"),
	NR_3_4_3("3.4-3 Sonderform für Radwegende (1)"),
	NR_3_4_4("3.4-4 Sonderform für Radwegende (2)"),
	NR_3_4_5("3.4-5 Furt mit Fahrradweiche"),
	NR_3_6_1("3.6-1 Gemeinsamer Geh- und Radweg"),
	NR_3_6_2("3.6-2 Furt an Fuß- und Radweg/Gehweg mit Zusatzzeichen 1022-10"),
	NR_3_6_3("3.6-3 Signalisierte Furt an Fuß- und Radweg/Gehweg mit Zusatzzeichen 1022-10"),
	NR_3_11_1("3.11-1 Radweg an Bushaltestelle"),
	NR_3_11_2("3.11-2 Schutzstreifen vor Bushaltestelle"),
	NR_4_3_1("4.3-1 Linksabbiegen aus übergeordneten Knotenpunktarmen"),
	NR_4_3_2("4.3-2 Knotenpunkt mit Vorfahrtregelung"),
	NR_4_4_1("4.4-1 Linksabbiegender Radverkehr - indirekte Führung"),
	NR_4_4_2("4.4-2 Fahrradweiche"),
	NR_4_4_3("4.4-3 Vorgezogene Haltlinie - Aufgeweiteter Radaufstellstreifen"),
	NR_4_4_4("4.4-4 Links abbiegender Radverkehr - direkte Führung"),
	NR_4_4_5("4.4-5 Auflösung Zweirichtungsradweg am signalisierten Knotenpunkt (Text)"),
	NR_4_4_6("4.4-6 Beginn Zweirichtungsradweg am signalisierten Knotenpunkt"),
	NR_4_4_7("4.4-7 Ende Zweirichtungsradweg am signalisierten Knotenpunkt"),
	NR_4_4_8("4.4-8 Erweiterte Fußgängersignalisierung zur Sicherung des Radverkehrs"),
	NR_4_5_1("4.5-1 Kreisverkehr - Führung des Radverkehrs auf der Fahrbahn"),
	NR_4_5_2("4.5-2 Kreisverkehr - Führung des Radverkehrs auf Radwegen"),
	NR_4_5_3("4.5-3 Minikreisel"),
	NR_4_5_4("4.5-4 Radweg am Kreisverkehr"),
	NR_4_5_5("4.5-5 Auflösung Zweirichtungsradweg vor Kreisverkehr - Querungsbedarf am Radweganfang"),
	NR_4_5_6("4.5-6 Auflösung Zweirichtungsradweg vor Kreisverkehr - Querungsbedarf am Radwegende"),
	NR_6_3_1("6.3-1 Gestaltung von Fahrradstraßen (1)"),
	NR_6_3_2("6.3-2 Gestaltung von Fahrradstraßen (2)"),
	NR_6_3_3("6.3-3 Gestaltung von Fahrradstraßen (3)"),
	NR_7_2_1("7.2-1 Einbahnstraßen mit Radverkehr in Gegenrichtung"),
	NR_9_2_1("9.2-1 Randmarkierung Fahrradroute (außerorts)"),
	NR_9_3_1("9.3-1 Bevorrechtigter straßenbegleitender Zweirichtungsradweg (1)"),
	NR_9_3_2("9.3-2 Bevorrechtigter straßenbegleitender Zweirichtungsradweg (2)"),
	NR_9_3_3("9.3-3 Untergeordneter straßenbegleitender Zweirichtungsradweg"),
	NR_9_4_1("9.4-1 Querungsstelle Radroute mit wartepflichtigem Radverkehr"),
	NR_9_4_2("9.4-2 Querungshilfen außerorts - großräumige Einbindung"),
	NR_9_4_3(
		"9.4-3 Geteilte Querungshilfe bei Radverkehrsführung im Zuge land- und forstwirtschaftlicher Wege sowie Anliegerstraßen"),
	NR_9_5_1("9.5-1 Auflösung Zweirichtungsradweg ohne Mittelinsel Querungsbedarf am Radwegende, Benutzungspflicht"),
	NR_9_5_2("9.5-2 Auflösung Zweirichtungsradweg ohne Mittelinsel Querungsbedarf am Radwegende, Benutzungsrecht"),
	NR_9_5_3("9.5-3 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radwegende, Benutzungspflicht (1)"),
	NR_9_5_4("9.5-4 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radwegende, Benutzungsrecht (1)"),
	NR_9_5_5("9.5-5 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radwegende, Benutzungspflicht (2)"),
	NR_9_5_6("9.5-6 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radwegende, Benutzungsrecht (2)"),
	NR_9_5_7("9.5-7 Auflösung Zweirichtungsradweg ohne Mittelinsel Querungsbedarf am Radweganfang, Benutzungspflicht"),
	NR_9_5_8("9.5-8 Auflösung Zweirichtungsradweg ohne Mittelinsel Querungsbedarf am Radweganfang, Benutzungsrecht"),
	NR_9_5_9(
		"9.5-9 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radweganfang, Benutzungspflicht (1)"),
	NR_9_5_10(
		"9.5-10 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radweganfang, Benutzungsrecht (1)"),
	NR_9_5_11(
		"9.5-11 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radweganfang, Benutzungspflicht (2)"),
	NR_9_5_12(
		"9.5-12 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radweganfang, Benutzungsrecht (2)"),
	NR_10_2_1("10.2-1 Querungsstelle Radroute mit bevorrechtigtem Radverkehr"),
	NR_11_1_3("11.1-3 Warnmarkierung mit Sperrpfosten"),
	NR_11_1_4("11.1-4 Querungsstelle/Einmündung Radroute mit Sicherung durch Befahren von Kfz"),
	RSV_M_1("RSV - M 1 Markierungen im Zuge von Radschnellverbindungen"),
	RSV_S_1("RSV - S 1 Bevorrechtigte Querung an einer untergeordneten Straße (ohne FGÜ) - innerorts/ außerorts"),
	RSV_S_2("RSV - S 2 Bevorrechtigte Querung an einer untergeordneten Straße (mit FGÜ) - innerorts"),
	RSV_S_3("RSV - S 3 Minikreisverkehr - innerorts"),
	RSV_S_4("RSV - S 4 Wartepflichtige Querung mit Mittelinsel - innerorts/ außerorts"),
	RSV_S_5("RSV - S 5 Querungsstelle mit nicht vollständiger Signalisierung - innerorts/ außerorts"),
	RSV_H_1("RSV - H 1 Führung auf baulichen Radwegen im Einrichtungsverkehr - innerorts"),
	RSV_H_2("RSV - H 2 Führung auf Radfahrstreifen - innerorts"),
	RSV_H_3("RSV - H 3 Bevorrechtigte Führung auf baulichen Radwegen im Einrichtungsverkehr - außerorts"),
	RSV_H_4("RSV - H 4 Bevorrechtigte Führung auf baulichen Radwegen im Zweirichtungsverkehr - außerorts"),
	RSV_H_5("RSV - H 5 Führung an Kreisverkehren (Fahrbahn) - innerorts"),
	RSV_H_6("RSV - H 6 Führung an Kreisverkehren (Seitenraum) - innerorts"),
	RSV_H_7("RSV - H 7 Führung an Kreisverkehren (Seitenraum) - außerorts"),
	RSV_N_1("RSV - N 1 Fahrradstraße innerhalb von Tempo-30-Zonen (Bevorrechtigung durch Beschilderung) - innerorts"),
	RSV_N_2("RSV - N 2 Fahrradstraße innerhalb von Tempo-30-Zonen (Bauliche Bevorrechtigung) - innerorts"),
	RSV_N_3("RSV - N 3 Aufgeweiterer Radaufstellstreifen - innerorts"),
	RSV_N_4("RSV - N 4 Rechts-vor-Links-Knoten mit Fahrbahnanhebung - innerorts"),
	RSV_F_1("RSV - F 1 Kreuzung mit Gemeinsamen Geh-/ Radweg und Radweg (selbstständig) - innerorts/ außerorts"),
	RSV_F_2("RSV - F 2 Kreuzung mit Gehweg (selbstständig) - innerorts");

	@Getter
	private String displayText;
}
