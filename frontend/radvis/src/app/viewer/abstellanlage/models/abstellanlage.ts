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
import { AbstellanlagenQuellSystem } from 'src/app/viewer/abstellanlage/models/abstellanlagen-quell-system';
import { Ueberwacht } from 'src/app/viewer/abstellanlage/models/ueberwacht';
import { Groessenklasse } from 'src/app/viewer/abstellanlage/models/groessenklasse';
import { Stellplatzart } from 'src/app/viewer/abstellanlage/models/stellplatzart';
import { AbstellanlagenStatus } from 'src/app/viewer/abstellanlage/models/abstellanlagen-status';

export interface Abstellanlage {
  geometrie: PointGeojson;
  betreiber: string;
  externeId?: string;

  quellSystem: AbstellanlagenQuellSystem;
  zustaendig?: Verwaltungseinheit;
  anzahlStellplaetze: number;
  anzahlSchliessfaecher?: number;
  anzahlLademoeglichkeiten?: number;
  ueberwacht: Ueberwacht;
  istBikeAndRide: boolean;
  groessenklasse?: Groessenklasse;
  stellplatzart: Stellplatzart;
  ueberdacht: boolean;
  gebuehrenProTag?: number;
  gebuehrenProMonat?: number;
  gebuehrenProJahr?: number;
  beschreibung?: string;
  weitereInformation?: string;
  status: AbstellanlagenStatus;

  id: number;
  version: number;
  darfBenutzerBearbeiten: boolean;
}
