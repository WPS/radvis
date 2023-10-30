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

import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import { getTestMassnahmeListenViews } from 'src/app/viewer/massnahme/models/massnahme-listen-view-test-data-provider.spec';

describe('massnahme-listen-view', () => {
  const testMassnahmeListenView = getTestMassnahmeListenViews()[0];

  describe('getDisplayValueForKey', () => {
    it('should convert the fields of massnahmeListenView to readable strings', () => {
      const expecteds = Object.entries({
        bezeichnung: 'testMassnahme',
        massnahmeKonzeptId: 'konzeptId',
        massnahmenkategorien: 'Umwidmung in gemeinsamen Rad-/Gehweg',
        durchfuehrungszeitraum: '2050',
        baulastZustaendiger: 'testOrganisation (Bundesland)',
        markierungsZustaendiger: '',
        unterhaltsZustaendiger: '',
        netzklassen: ['Alltag (Kommunalnetz)', 'Freizeit (Kreisnetz)'],
        prioritaet: '5000',
        umsetzungsstatus: 'Idee',
        veroeffentlicht: 'nein',
        letzteAenderung: '10.01.22 15:28',
        benutzerLetzteAenderung: 'M. Mustermensch',
      });

      expecteds.forEach(([key, expectedValue]) =>
        expect(MassnahmeListenView.getDisplayValueForKey(testMassnahmeListenView, key)).toEqual(expectedValue)
      );
    });
  });
});
