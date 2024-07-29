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

import { MassnahmenImportZuordnungUeberpruefung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung';
import { MassnahmenImportZuordnungStatus } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-status';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { defaultNetzbezug } from 'src/app/shared/models/netzbezug-test-data-provider.spec';

export const getDefaultZuordnung = (): MassnahmenImportZuordnungUeberpruefung => {
  return {
    id: 1,
    status: MassnahmenImportZuordnungStatus.ZUGEORDNET,
    massnahmeKonzeptId: '123',
    netzbezugHinweise: [{ tooltip: 'Ein Tooltip', text: 'Ein Text', severity: 'WARN' }],
    netzbezug: defaultNetzbezug,
    originalGeometrie: {
      type: 'LineString',
      coordinates: [
        [0, 1],
        [0, 3],
      ],
    } as LineStringGeojson,
    netzbezugGeometrie: {
      type: 'LineString',
      coordinates: [
        [0, 10],
        [0, 20],
      ],
    } as LineStringGeojson,
    selected: false,
  };
};
