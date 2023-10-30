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

import { PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { NetzDetailFeatureTableLink } from 'src/app/viewer/viewer-shared/models/netzdetail-feature-table-link';

export interface WegweisendeBeschilderungListenView {
  id: number;
  geometrie: PointGeojson;
  pfostenNr: string;
  wegweiserTyp: string;
  pfostenTyp: string;
  zustandsbewertung: string;
  defizit: string;
  pfostenzustand: string;
  pfostendefizit: string;
  gemeinde: string;
  kreis: string;
  land: string;
  zustaendig: Verwaltungseinheit | null;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace WegweisendeBeschilderungListenView {
  const EMPTY_FIELD_INDICATOR = '';

  export const getDisplayValueForKey = (item: WegweisendeBeschilderungListenView, key: string): string => {
    switch (key) {
      case 'id':
        return `${item.id}` ?? EMPTY_FIELD_INDICATOR;
      case 'pfostenNr':
        return item.pfostenNr ?? EMPTY_FIELD_INDICATOR;
      case 'wegweiserTyp':
        return item.wegweiserTyp ?? EMPTY_FIELD_INDICATOR;
      case 'pfostenTyp':
        return item.pfostenTyp ?? EMPTY_FIELD_INDICATOR;
      case 'zustandsbewertung':
        return item.zustandsbewertung ?? EMPTY_FIELD_INDICATOR;
      case 'defizit':
        return item.defizit ?? EMPTY_FIELD_INDICATOR;
      case 'pfostenzustand':
        return item.pfostenzustand ?? EMPTY_FIELD_INDICATOR;
      case 'pfostendefizit':
        return item.pfostendefizit ?? EMPTY_FIELD_INDICATOR;
      case 'gemeinde':
        return item.gemeinde ?? EMPTY_FIELD_INDICATOR;
      case 'kreis':
        return item.kreis ?? EMPTY_FIELD_INDICATOR;
      case 'land':
        return item.land ?? EMPTY_FIELD_INDICATOR;
      case 'zustaendig':
        return Verwaltungseinheit.getDisplayName(item.zustaendig) ?? EMPTY_FIELD_INDICATOR;
      default:
        throw Error(`Key ${key} nicht gefunden`);
    }
  };

  export const getDetails = (
    item: WegweisendeBeschilderungListenView
  ): { [key: string]: string | NetzDetailFeatureTableLink } => {
    return {
      Pfostennummer: item.pfostenNr,
      Wegweisertyp: item.wegweiserTyp,
      Pfostentyp: item.pfostenTyp,
      Zustandsbewertung: item.zustandsbewertung,
      Defizit: item.defizit,
      Pfostenzustand: item.pfostenzustand,
      Pfostendefizit: item.pfostendefizit,
      Gemeinde: item.gemeinde,
      Kreis: item.kreis,
      Land: item.land,
      'Zuständige Organisation': item.zustaendig?.name ?? '',
      Kataster: {
        text: item.pfostenNr + '.pdf',
        url: '/api/wegweisendebeschilderung/kataster/' + item.pfostenNr + '.pdf',
      },
    };
  };
}
