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

import { FehlerprotokollTyp } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-typ';
import { LineStringGeojson, PointGeojson } from 'src/app/shared/models/geojson-geometrie';

export const defaultFehlerpotokoll = {
  beschreibung: 'Testbeschreibung',
  datum: '2020-11-03T10:20',
  entityLink: '/viewer/test',
  fehlerprotokollKlasse: 'MassnahmeNetzBezugAenderung',
  fehlerprotokollTyp: FehlerprotokollTyp.DLM_REIMPORT_JOB_MASSNAHMEN,
  iconPosition: { coordinates: [0, 0], type: 'Point' } as PointGeojson,
  id: 135,
  originalGeometry: {
    coordinates: [
      [0, 0],
      [100, 0],
    ],
    type: 'LineString',
  } as LineStringGeojson,
  titel: 'Testtitel',
};
