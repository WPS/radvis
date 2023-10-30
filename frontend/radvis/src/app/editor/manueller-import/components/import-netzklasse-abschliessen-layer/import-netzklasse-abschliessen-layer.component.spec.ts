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

/* eslint-disable @typescript-eslint/dot-notation */
import { ImportNetzklasseAbschliessenLayerComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abschliessen-layer/import-netzklasse-abschliessen-layer.component';
import { Feature } from 'ol';

describe('ImportAbschlussLayerComponent', () => {
  describe('Set Netzklasse in Feature properties', () => {
    it('Dont set hasNetzklasse if Ids List is empty', () => {
      const feature = new Feature();
      feature.setId(2);
      expect(
        ImportNetzklasseAbschliessenLayerComponent['setHasNetzklasse'](feature, []).get('hasNetzklasse')
      ).toBeFalse();
    });

    const listWithIds = [1, 2, 9];
    it('Dont hasNetzklasse if feature has no id', () => {
      const feature = new Feature();
      expect(
        ImportNetzklasseAbschliessenLayerComponent['setHasNetzklasse'](feature, listWithIds).get('hasNetzklasse')
      ).toBeFalse();
    });

    it('Dont hasNetzklasse if Ids List does not contain feature id', () => {
      const feature = new Feature();
      feature.setId(3);
      expect(
        ImportNetzklasseAbschliessenLayerComponent['setHasNetzklasse'](feature, listWithIds).get('hasNetzklasse')
      ).toBeFalse();
    });

    it('Set hasNetzklasse if Ids List contains feature id', () => {
      const feature = new Feature();
      feature.setId(2);
      expect(
        ImportNetzklasseAbschliessenLayerComponent['setHasNetzklasse'](feature, listWithIds).get('hasNetzklasse')
      ).toBeTrue();
    });
  });
});
