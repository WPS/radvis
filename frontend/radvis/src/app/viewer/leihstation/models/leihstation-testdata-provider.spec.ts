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

import { Leihstation } from 'src/app/viewer/leihstation/models/leihstation';
import { LeihstationQuellSystem } from 'src/app/viewer/leihstation/models/leihstation-quell-system';
import { LeihstationStatus } from 'src/app/viewer/leihstation/models/leihstation-status';

export const defaultLeihstation: Leihstation = {
  betreiber: 'Meine Leihstation',
  geometrie: {
    coordinates: [0, 0],
    type: 'Point',
  },
  freiesAbstellen: true,
  anzahlFahrraeder: 100,
  anzahlPedelecs: 200,
  anzahlAbstellmoeglichkeiten: 300,
  buchungsUrl: 'https://some.wierdplaceinda.internets.com',
  status: LeihstationStatus.AKTIV,
  id: 1,
  version: 1,
  darfBenutzerBearbeiten: true,
  quellSystem: LeihstationQuellSystem.RADVIS,
};
