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

import { LineStringGeojson, MultiLineStringGeojson, PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { AbschnittsweiserKantenNetzbezug } from 'src/app/viewer/fahrradroute/models/abschnittsweiser-kanten-netzbezug';
import { AbweichendeSegmente } from 'src/app/viewer/fahrradroute/models/abweichende-segmente';
import { FahrradrouteVariante } from 'src/app/viewer/fahrradroute/models/fahrradroute-variante';
import { ProfilEigenschaften } from 'src/app/viewer/fahrradroute/models/profil-eigenschaften';
import { Tourenkategorie } from 'src/app/viewer/fahrradroute/models/tourenkategorie';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { FahrradrouteTyp } from 'src/app/viewer/viewer-shared/models/fahrradroute-typ';

export interface FahrradrouteDetailView {
  id: number;
  version: number;
  toubizId: string;
  fahrradrouteTyp: FahrradrouteTyp;
  name: string;
  kurzbeschreibung: string;
  beschreibung: string;
  fahrradrouteKategorie: FahrradrouteKategorie;
  tourenkategorie: Tourenkategorie;
  laengeHauptstrecke: number;
  offizielleLaenge: number;
  homepage: string;
  verantwortlich: Verwaltungseinheit | null;
  emailAnsprechpartner: string;
  lizenz: string;
  lizenzNamensnennung: string;
  abstieg?: number;
  anstieg?: number;
  info: string;
  zuletztBearbeitet: Date;
  veroeffentlicht: boolean;
  customProfileId: number;

  kantenBezug: AbschnittsweiserKantenNetzbezug[];
  originalGeometrie?: LineStringGeojson | MultiLineStringGeojson;
  stuetzpunkte?: LineStringGeojson;
  geometrie?: LineStringGeojson;

  // Geometrien zum Anzeigen/Erkennen von Matching/Routing Fehlern
  routedOrMatchedGeometry?: LineStringGeojson;
  kehrtwenden?: PointGeojson[];
  abweichendeSegmente?: AbweichendeSegmente;
  abbildungDurchRouting: boolean;

  canEditAttribute: boolean;
  canChangeVeroeffentlicht: boolean;

  varianten: FahrradrouteVariante[];

  profilEigenschaften: ProfilEigenschaften[];
}
