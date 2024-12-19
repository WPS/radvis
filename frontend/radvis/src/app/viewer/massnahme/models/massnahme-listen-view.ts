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

import { DatePipe } from '@angular/common';
import { BenutzerName } from 'src/app/shared/models/benutzer-name';
import { Durchfuehrungszeitraum } from 'src/app/shared/models/durchfuehrungszeitraum';
import { LineStringGeojson, MultiLineStringGeojson, PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { UmsetzungsstandStatus } from 'src/app/viewer/massnahme/models/umsetzungsstand-status';

export interface MassnahmeListenView {
  id: number;
  massnahmeKonzeptId: string;
  bezeichnung: string;
  massnahmenkategorien: string[];
  umsetzungsstatus: Umsetzungsstatus;
  umsetzungsstandStatus?: UmsetzungsstandStatus;
  veroeffentlicht: boolean;
  planungErforderlich: boolean;
  durchfuehrungszeitraum: Durchfuehrungszeitraum | null;
  baulastZustaendiger: Verwaltungseinheit | null;
  prioritaet: number | null;
  netzklassen: Set<Netzklasse>;
  letzteAenderung: string;
  benutzerLetzteAenderung: BenutzerName;
  zustaendiger: Verwaltungseinheit | null;
  unterhaltsZustaendiger: Verwaltungseinheit | null;
  sollStandard: SollStandard;
  handlungsverantwortlicher: Handlungsverantwortlicher;
  konzeptionsquelle: Konzeptionsquelle;
  archiviert: boolean;

  geometry: LineStringGeojson | MultiLineStringGeojson | PointGeojson;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace MassnahmeListenView {
  const EMPTY_FIELD_INDICATOR = '';

  export const getDisplayValueForKey = (item: MassnahmeListenView, key: string): string | string[] => {
    switch (key) {
      case 'bezeichnung':
        return item.bezeichnung ?? EMPTY_FIELD_INDICATOR;
      case 'massnahmeKonzeptId':
        return item.massnahmeKonzeptId ?? EMPTY_FIELD_INDICATOR;
      case 'massnahmenkategorien':
        return (
          item.massnahmenkategorien.map(Massnahmenkategorien.getDisplayTextForMassnahmenKategorie).join(', ') ??
          EMPTY_FIELD_INDICATOR
        );
      case 'durchfuehrungszeitraum':
        return item.durchfuehrungszeitraum?.geplanterUmsetzungsstartJahr?.toString() ?? EMPTY_FIELD_INDICATOR;
      case 'baulastZustaendiger':
        return Verwaltungseinheit.getDisplayName(item.baulastZustaendiger) ?? EMPTY_FIELD_INDICATOR;
      case 'zustaendiger':
        return Verwaltungseinheit.getDisplayName(item.zustaendiger) ?? EMPTY_FIELD_INDICATOR;
      case 'unterhaltsZustaendiger':
        return Verwaltungseinheit.getDisplayName(item.unterhaltsZustaendiger) ?? EMPTY_FIELD_INDICATOR;
      case 'netzklassen':
        return Array.from(item.netzklassen.values()).map(
          netzklasse =>
            Netzklasse.options.find(option => option.name === netzklasse)?.displayText ?? EMPTY_FIELD_INDICATOR
        );
      case 'prioritaet':
        return item.prioritaet?.toString() ?? EMPTY_FIELD_INDICATOR;
      case 'umsetzungsstatus':
        return (
          Umsetzungsstatus.options.find(option => option.name === item.umsetzungsstatus)?.displayText ??
          EMPTY_FIELD_INDICATOR
        );
      case 'umsetzungsstandStatus':
        return item.umsetzungsstandStatus ? UmsetzungsstandStatus.displayTextOf(item.umsetzungsstandStatus) : '';
      case 'veroeffentlicht':
        return item.veroeffentlicht ? 'ja' : 'nein';
      case 'planungErforderlich':
        return item.planungErforderlich ? 'ja' : 'nein';
      case 'letzteAenderung':
        return item.letzteAenderung
          ? (new DatePipe('en-US').transform(new Date(item.letzteAenderung), 'dd.MM.yy HH:mm') as string)
          : EMPTY_FIELD_INDICATOR;
      case 'benutzerLetzteAenderung':
        return item.benutzerLetzteAenderung
          ? `${item.benutzerLetzteAenderung.vorname.charAt(0)}. ${item.benutzerLetzteAenderung.nachname}`
          : EMPTY_FIELD_INDICATOR;
      case 'sollStandard':
        return (
          SollStandard.options.find(option => option.name === item.sollStandard)?.displayText ?? EMPTY_FIELD_INDICATOR
        );
      case 'handlungsverantwortlicher':
        return (
          Handlungsverantwortlicher.options.find(option => option.name === item.handlungsverantwortlicher)
            ?.displayText ?? EMPTY_FIELD_INDICATOR
        );
      case 'konzeptionsquelle':
        return (
          Konzeptionsquelle.options.find(o => o.name === item.konzeptionsquelle)?.displayText ?? EMPTY_FIELD_INDICATOR
        );
      case 'archiviert':
        return item.archiviert ? 'ja' : 'nein';
      default:
        throw Error(`Key ${key} nicht gefunden`);
    }
  };
  export const getSortingValueForKey = (item: MassnahmeListenView, key: string): string | number | string[] => {
    if (key === 'letzteAenderung') {
      return item.letzteAenderung ? new Date(item.letzteAenderung).getTime() : EMPTY_FIELD_INDICATOR;
    }
    return getDisplayValueForKey(item, key);
  };
}
