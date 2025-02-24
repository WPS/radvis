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

import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { KantenNetzbezug } from 'src/app/shared/models/kanten-netzbezug';
import { KnotenNetzbezug } from 'src/app/shared/models/knoten-netzbezug';
import { PunktuellerKantenNetzBezug } from 'src/app/shared/models/punktueller-kanten-netzbezug';

/* eslint-disable @typescript-eslint/no-namespace */
export enum KantenSeitenbezug {
  LINKS = 'LINKS',
  RECHTS = 'RECHTS',
  BEIDSEITIG = 'BEIDSEITIG',
}

export interface AbschnittsweiserKantenSeitenBezug extends KantenNetzbezug {
  linearReferenzierterAbschnitt: LinearReferenzierterAbschnitt;
  kantenSeite: KantenSeitenbezug;
}

export interface Segment {
  von: number;
  bis: number;
  selected: boolean;
}

export interface Netzbezug {
  kantenBezug: AbschnittsweiserKantenSeitenBezug[];
  punktuellerKantenBezug: PunktuellerKantenNetzBezug[];
  knotenBezug: KnotenNetzbezug[];
}

export namespace Netzbezug {
  export const sindSegmenteIdentisch = (segmente1: Segment[], segmente2: Segment[]): boolean => {
    return (
      segmente1.length === segmente2.length &&
      segmente1.every(seg1 =>
        segmente2.some(seg2 => seg1.von === seg2.von && seg1.bis === seg2.bis && seg1.selected === seg2.selected)
      )
    );
  };

  export const sindAbschnitteIdentisch = (
    abschnitte1: AbschnittsweiserKantenSeitenBezug[],
    abschnitte2: AbschnittsweiserKantenSeitenBezug[]
  ): boolean => {
    return (
      abschnitte1.length === abschnitte2.length &&
      abschnitte1.every(abschnitt1 =>
        abschnitte2.some(
          abschnitt2 =>
            abschnitt1.linearReferenzierterAbschnitt.von === abschnitt2.linearReferenzierterAbschnitt.von &&
            abschnitt1.linearReferenzierterAbschnitt.bis === abschnitt2.linearReferenzierterAbschnitt.bis &&
            abschnitt1.kanteId === abschnitt2.kanteId &&
            abschnitt1.kantenSeite === abschnitt2.kantenSeite
        )
      )
    );
  };

  export const extractKantenSelektion = (kantenBezuege: AbschnittsweiserKantenSeitenBezug[]): Segment[] => {
    if (kantenBezuege.length === 0) {
      return [{ von: 0, bis: 1, selected: false }];
    }

    const abschnitte: Segment[] = [];
    kantenBezuege.sort((b1, b2) => b1.linearReferenzierterAbschnitt.von - b2.linearReferenzierterAbschnitt.von);
    kantenBezuege.forEach(bezug => {
      if (bezug.linearReferenzierterAbschnitt.von === 0) {
        abschnitte.push({ von: 0, bis: bezug.linearReferenzierterAbschnitt.bis, selected: true });
      } else {
        if (abschnitte.length > 0) {
          // wenn es segmente gibt, schließen sie an oder ist eine da eine Lücke?
          if (abschnitte[abschnitte.length - 1].bis === bezug.linearReferenzierterAbschnitt.von) {
            // Abschnitt schließen aneinander an
            abschnitte.push({
              von: bezug.linearReferenzierterAbschnitt.von,
              bis: bezug.linearReferenzierterAbschnitt.bis,
              selected: true,
            });
          } else {
            // es gibt eine Lücke -> Lückenfüller
            abschnitte.push({
              von: abschnitte[abschnitte.length - 1].bis,
              bis: bezug.linearReferenzierterAbschnitt.von,
              selected: false,
            });
            // selektierter Abschnitt
            abschnitte.push({
              von: bezug.linearReferenzierterAbschnitt.von,
              bis: bezug.linearReferenzierterAbschnitt.bis,
              selected: true,
            });
          }
        } else {
          // es ist nicht das Start-Segment, da von!==0 -> Lückenfüller
          abschnitte.push({
            von: 0,
            bis: bezug.linearReferenzierterAbschnitt.von,
            selected: false,
          });
          // selektierter Abschnitt
          abschnitte.push({
            von: bezug.linearReferenzierterAbschnitt.von,
            bis: bezug.linearReferenzierterAbschnitt.bis,
            selected: true,
          });
        }
      }
    });
    if (abschnitte[abschnitte.length - 1].bis !== 1) {
      abschnitte.push({ selected: false, bis: 1, von: abschnitte[abschnitte.length - 1].bis });
    }
    return abschnitte;
  };

  export const groupByKante = (
    bezuege: AbschnittsweiserKantenSeitenBezug[]
  ): Map<number, AbschnittsweiserKantenSeitenBezug[]> => {
    const result = new Map<number, AbschnittsweiserKantenSeitenBezug[]>();
    bezuege.forEach(bezug => {
      if (result.has(bezug.kanteId)) {
        result.get(bezug.kanteId)?.push(bezug);
      } else {
        result.set(bezug.kanteId, [bezug]);
      }
    });
    return result;
  };
}
