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
import { Ueberwacht } from 'src/app/viewer/abstellanlage/models/ueberwacht';
import { AbstellanlagenOrt } from 'src/app/viewer/abstellanlage/models/abstellanlagen-ort';
import { Groessenklasse } from 'src/app/viewer/abstellanlage/models/groessenklasse';
import { Stellplatzart } from 'src/app/viewer/abstellanlage/models/stellplatzart';
import { AbstellanlagenStatus } from 'src/app/viewer/abstellanlage/models/abstellanlagen-status';

export interface SaveAbstellanlageCommand {
  geometrie: PointGeojson;
  betreiber: string;
  externeId: string | null;

  zustaendigId: number | null;
  anzahlStellplaetze: number | null;
  anzahlSchliessfaecher: number | null;
  anzahlLademoeglichkeiten: number | null;
  ueberwacht: Ueberwacht;
  abstellanlagenOrt: AbstellanlagenOrt;
  groessenklasse: Groessenklasse | null;
  stellplatzart: Stellplatzart;
  ueberdacht: boolean;
  gebuehrenProTag: number | null;
  gebuehrenProMonat: number | null;
  gebuehrenProJahr: number | null;
  beschreibung: string | null;
  weitereInformation: string | null;
  status: AbstellanlagenStatus;

  version?: number;
}
