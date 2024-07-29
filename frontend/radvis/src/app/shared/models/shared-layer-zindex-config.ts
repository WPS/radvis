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

export const lineareReferenzierungLayerZIndex = 805;
export const puntuelleKantenBezuegeVectorLayerZIndex = lineareReferenzierungLayerZIndex + 6;
export const knotenNetzVectorlayerZIndex = lineareReferenzierungLayerZIndex + 5;
export const kantenNetzVectorlayerZIndex = lineareReferenzierungLayerZIndex - 5;

export const originalGeometrieLayerZIndex = 402;

export const infrastrukturHighlightLayerZIndex = 301;
