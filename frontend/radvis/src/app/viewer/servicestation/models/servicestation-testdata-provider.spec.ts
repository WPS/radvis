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

import { Servicestation } from 'src/app/viewer/servicestation/models/servicestation';
import { defaultGemeinden } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { ServicestationTyp } from 'src/app/viewer/servicestation/models/servicestation-typ';
import { ServicestationStatus } from 'src/app/viewer/servicestation/models/servicestation-status';

export const defaultServicestation: Servicestation = {
  name: 'Meine Servicestation',
  geometrie: {
    coordinates: [0, 0],
    type: 'Point',
  },
  gebuehren: true,
  oeffnungszeiten: 'Immer geschlossen',
  betreiber: 'default betreiber name',
  marke: 'default marke',
  luftpumpe: false,
  kettenwerkzeug: true,
  werkzeug: true,
  fahrradhalterung: true,
  beschreibung: 'beschreibungstext',
  organisation: defaultGemeinden[0],
  typ: ServicestationTyp.RADSERVICE_PUNKT_GROSS,
  status: ServicestationStatus.AKTIV,
  id: 1,
  version: 1,
  darfBenutzerBearbeiten: true,
};
