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

export class EditorLayerZindexConfig {
  static MANUELLER_IMPORT_NETZKLASSE_ABSCHLIESSEN_LAYER = 999;
  static MANUELLER_IMPORT_NETZKLASSE_BEARBEITEN_LAYER = 999;
  static MANUELLER_IMPORT_ATTRIBUTE_MODIFY_LAYER = 902;
  static MANUELLER_IMPORT_ATTRIBUTE_MAPPINGS_LAYER = 901;
  static MANUELLER_IMPORT_ATTRIBUTE_RADVIS_LAYER = 900;
  static MANUELLER_IMPORT_MASSNAHMEN_ZUORDNUNG = 800;
  static KNOTEN_SELECTION_LAYER = 701;
  static KANTEN_SELECTION_LAYER = 701;
  static KNOTEN_ANZEIGEN_LAYER = 700;
  static KANTEN_CREATOR_KNOTEN_SELECTION_LAYER = 700;
  static LINEARE_REFERENZIERUNG_LAYER = 666;
  static MODIFY_GEOMETRY_LAYER = 400;
  static KANTE_GRUNDGEOMETRIE_LAYER = 400;
  static FEHLERPROTOKOLL_LAYER = 345;
  static KANTEN_ANZEIGEN_LAYER = 100;
}

// Die Hintergrundkarte hat den zIndex 0 und wird im HintergrundLayerService fuer Viewer und Editor festgelegt
