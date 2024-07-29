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

// !!! ACHTUNG !!!
// Folgende ZIndizes sind auch nochmal im Handbuch dokumentiert. Bitte bei Aenderung auch das Handbuch Kapitel
// "Anzeigeordnung der Kartenebenen" anpassen.

export const messwerkzeugZIndex = 2200;

export const weitereKartenebenenHighlightZIndex = 2000;
export const neueWeitereKartenebenenDefaultZIndex = 1500; // Der Wert der beim "komplett neue Karten Ebene Anlegen" angezeigt wird
// Der ZIndex der WeiterenKartenebenen ist durch den/die Nutzer:in individuell anpassbar
export const predefinedWeitereKartenebenenBaseZIndex = 1300; // Wird fuer die einzelnen predefinedLayer hochgezaehlt
// Kann bei der Einbindung als WeitereKartenebene pro Benutzer:in noch angepasst werden
export const defaultDateiLayerZIndex = 1000;

export const kanteHighlightLayerZIndex = 700;

export const anpassungswunschLayerZIndex = 600;
export const importProtokollLayerZIndex = 500;

export const fahrradroutenMatchingFehlerGeometriesLayerZIndex = 401;
export const fehlerprotokollLayerZIndex = 400;

export const infrastrukturLayerZIndex = 300;

export const signaturNetzklasseLayerZIndex = 201;
export const infrastrukturSignaturLayerZIndex = 200;

export const highlightNetzklasseLayerZIndex = 101;
export const defaultNetzklasseLayerZIndex = 100;

// Die Hintergrundkarte hat den zIndex 0 und wird im HintergrundLayerService fuer Viewer und Editor festgelegt
