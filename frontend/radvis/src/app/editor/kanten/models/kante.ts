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

import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { FahrtrichtungAttributGruppe } from 'src/app/editor/kanten/models/fahrtrichtung-attributgruppe';
import { FuehrungsformAttributGruppe } from 'src/app/editor/kanten/models/fuehrungsform-attribut-gruppe';
import { GeschwindigkeitsAttributGruppe } from 'src/app/editor/kanten/models/geschwindigkeits-attribut-gruppe';
import { KantenAttributGruppe } from 'src/app/editor/kanten/models/kanten-attribut-gruppe';
import { ZustaendigkeitAttributGruppe } from 'src/app/editor/kanten/models/zustaendigkeit-attribut-gruppe';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import invariant from 'tiny-invariant';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';

export interface Kante {
  id: number;
  geometry: LineStringGeojson;
  verlaufLinks: LineStringGeojson | null;
  verlaufRechts: LineStringGeojson | null;
  laengeBerechnet: number;
  zweiseitig: boolean;
  geometrieAenderungErlaubt: boolean;
  liegtInZustaendigkeitsbereich: boolean;

  quelle: QuellSystem;

  kantenAttributGruppe: KantenAttributGruppe;
  geschwindigkeitAttributGruppe: GeschwindigkeitsAttributGruppe;
  fuehrungsformAttributGruppe: FuehrungsformAttributGruppe;
  zustaendigkeitAttributGruppe: ZustaendigkeitAttributGruppe;
  fahrtrichtungAttributGruppe: FahrtrichtungAttributGruppe;

  kantenVersion: number;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Kante {
  export const getAnzahlSegmente = (
    attributGruppe: AttributGruppe,
    kante: Kante,
    seitenbezug?: Seitenbezug
  ): number => {
    if (attributGruppe === AttributGruppe.FUEHRUNGSFORM) {
      invariant(seitenbezug, 'Seitenbezug muss bei Führungsform angegeben werden');
      if (seitenbezug === Seitenbezug.LINKS) {
        return kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks.length;
      } else if (seitenbezug === Seitenbezug.RECHTS) {
        return kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts.length;
      }
    } else if (attributGruppe === AttributGruppe.ZUSTAENDIGKEIT) {
      return kante.zustaendigkeitAttributGruppe.zustaendigkeitAttribute.length;
    } else if (attributGruppe === AttributGruppe.GESCHWINDIGKEIT) {
      return kante.geschwindigkeitAttributGruppe.geschwindigkeitAttribute.length;
    }
    return 1;
  };
}
