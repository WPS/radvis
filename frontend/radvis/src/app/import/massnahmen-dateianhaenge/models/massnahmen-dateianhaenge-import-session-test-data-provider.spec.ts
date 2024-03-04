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

import { MassnahmenDateianhaengeImportSessionView } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-view';
import {
  MassnahmenDateianhaengeImportMappingSeverity,
  MassnahmenDateianhaengeZuordnungStatus,
} from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-zuordnung';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';

export const defaultMassnahmenDateianhaengeSessionFehlerUeberpruefen: MassnahmenDateianhaengeImportSessionView = {
  schritt: 2,
  log: [],
  executing: false,
  gebietskoerperschaften: [1, 2, 3],
  konzeptionsquelle: Konzeptionsquelle.KREISKONZEPT,
  sollStandard: SollStandard.BASISSTANDARD,
  zuordnungen: [
    {
      ordnername: 'PassendeMassnahme',
      massnahmeId: 4,
      status: MassnahmenDateianhaengeZuordnungStatus.GEMAPPT,
      dateien: ['test.pdf', 'test-bild.png'],
      hinweise: [],
    },
    {
      ordnername: 'NichtFindbareMassnahmenID',
      massnahmeId: null,
      status: MassnahmenDateianhaengeZuordnungStatus.FEHLERHAFT,
      dateien: ['test.pdf', 'test-bild.png'],
      hinweise: [
        {
          text: 'Maßnahme NichtFindbareMassnahmenID wurde nicht gefunden',
          severity: MassnahmenDateianhaengeImportMappingSeverity.ERROR,
        },
      ],
    },
    {
      ordnername: 'LeererOrdner',
      massnahmeId: 5,
      status: MassnahmenDateianhaengeZuordnungStatus.IGNORIERT,
      dateien: [],
      hinweise: [
        {
          text: 'Ordner ist leer',
          severity: MassnahmenDateianhaengeImportMappingSeverity.ERROR,
        },
      ],
    },
    {
      ordnername: 'KeineEindeutigeZuordnung',
      massnahmeId: null,
      status: MassnahmenDateianhaengeZuordnungStatus.FEHLERHAFT,
      dateien: ['datei.png'],
      hinweise: [
        {
          text: 'Keine eindeutige Zuordnung möglich, da 2 potentielle Maßnahmen gefunden wurden',
          severity: MassnahmenDateianhaengeImportMappingSeverity.ERROR,
        },
      ],
    },
    {
      ordnername: 'UngültigeMassnahmeID',
      massnahmeId: null,
      status: MassnahmenDateianhaengeZuordnungStatus.IGNORIERT,
      dateien: ['datei.pdf'],
      hinweise: [
        {
          text: 'Ordnername ist keine gültige Maßnahme-ID',
          severity: MassnahmenDateianhaengeImportMappingSeverity.ERROR,
        },
      ],
    },
  ],
};
