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

import { Richtung } from 'src/app/editor/kanten/models/richtung';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';

export interface KanteDetailView {
  id: number;
  geometrie: LineStringGeojson;
  attributeAufGanzerLaenge: { [key: string]: string };
  attributeAnPosition: { [key: string]: string };
  seite?: KantenSeite;
  verlaufLinks?: LineStringGeojson;
  verlaufRechts?: LineStringGeojson;
  trennstreifenAttribute?: { [key: string]: string };
  trennstreifenEinseitig: boolean;
  trennstreifenRichtungRechts: Richtung;
  trennstreifenRichtungLinks: Richtung;
}
