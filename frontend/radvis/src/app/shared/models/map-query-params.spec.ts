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

import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { Params } from '@angular/router';

describe(MapQueryParams.name, () => {
  describe('merge', () => {
    it('should replace all parameters', () => {
      const merge = new MapQueryParams(
        ['LAYER_2'],
        [Netzklassefilter.KOMMUNALNETZ, Netzklassefilter.KREISNETZ],
        [4, 5, 6, 7],
        false
      );
      const into = new MapQueryParams(['LAYER_1'], [Netzklassefilter.RADNETZ], [0, 1, 2, 3], true);

      const result = MapQueryParams.merge(merge, into);

      expect(result).toEqual(
        new MapQueryParams(
          ['LAYER_2'],
          [Netzklassefilter.KOMMUNALNETZ, Netzklassefilter.KREISNETZ],
          [4, 5, 6, 7],
          false
        )
      );
    });

    it('should remove parameter if empty', () => {
      const into = new MapQueryParams(['LAYER_1'], [Netzklassefilter.RADNETZ], [0, 1, 2, 3], false);
      const merge = new MapQueryParams(['LAYER_2'], [], null, null);

      const result = MapQueryParams.merge(merge, into);

      expect(result).toEqual(new MapQueryParams(['LAYER_2'], [], [0, 1, 2, 3], false));
    });

    it('should replace parameter for partial options literal', () => {
      const into = new MapQueryParams(['LAYER_1'], [Netzklassefilter.RADNETZ], [0, 1, 2, 3], false);

      const result = MapQueryParams.merge(
        { netzklassen: [Netzklassefilter.KREISNETZ, Netzklassefilter.KOMMUNALNETZ], mitVerlauf: false },
        into
      );

      expect(result).toEqual(
        new MapQueryParams(
          ['LAYER_1'],
          [Netzklassefilter.KREISNETZ, Netzklassefilter.KOMMUNALNETZ],
          [0, 1, 2, 3],
          false
        )
      );
    });
  });

  describe('route params conversion', () => {
    it('should be bidirectional if empty', () => {
      const leereParams = new MapQueryParams([], [], null, null, null, null);
      const asParams = leereParams.toRouteParams();
      expect(MapQueryParams.fromRoute(asParams)).toEqual(leereParams);
    });

    it('it should use empty sting instead of undefined', () => {
      //weil angular manchmal undefined als "undefined" interpretiert
      const leereParams = new MapQueryParams([], [], null, null, null, null);
      const asParams = leereParams.toRouteParams();
      expect(asParams).toEqual({
        layers: '',
        view: '',
        netzklassen: '',
        signatur: '',
        mitVerlauf: '',
        hintergrund: '',
      });
    });

    it('should handle booleans correctly', () => {
      const paramsMitFalseBool: Params = {
        layers: '',
        view: '',
        netzklassen: '',
        signatur: '',
        mitVerlauf: 'false',
        hintergrund: '',
      };
      const queryParams = MapQueryParams.fromRoute(paramsMitFalseBool);
      expect(queryParams.mitVerlauf).toBeFalse();
    });
  });
});
