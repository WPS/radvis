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

import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { AbstellanlagenQuellSystem } from 'src/app/viewer/abstellanlage/models/abstellanlagen-quell-system';
import { defaultGemeinden } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { Ueberwacht } from 'src/app/viewer/abstellanlage/models/ueberwacht';
import { Stellplatzart } from 'src/app/viewer/abstellanlage/models/stellplatzart';
import { AbstellanlagenStatus } from 'src/app/viewer/abstellanlage/models/abstellanlagen-status';

export const defaultAbstellanlage: Abstellanlage = {
  betreiber: 'Mein Betreiber',
  externeId: 'externe default Id',
  geometrie: {
    coordinates: [0, 0],
    type: 'Point',
  },
  quellSystem: AbstellanlagenQuellSystem.RADVIS,
  zustaendig: defaultGemeinden[0],
  anzahlStellplaetze: 100,
  anzahlSchliessfaecher: 200,
  anzahlLademoeglichkeiten: 300,
  ueberwacht: Ueberwacht.KEINE,
  istBikeAndRide: false,
  groessenklasse: undefined,
  stellplatzart: Stellplatzart.DOPPELSTOECKIG,
  ueberdacht: true,
  gebuehrenProTag: 400,
  gebuehrenProMonat: 500,
  gebuehrenProJahr: 600,
  beschreibung: 'beschreibung default',
  weitereInformation: 'weitere Info default',
  status: AbstellanlagenStatus.AKTIV,

  id: 1,
  version: 1,
  darfBenutzerBearbeiten: true,
};
