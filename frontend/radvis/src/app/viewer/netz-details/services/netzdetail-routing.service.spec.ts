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

import { NetzdetailRoutingService } from 'src/app/viewer/netz-details/services/netzdetail-routing.service';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';

describe(NetzdetailRoutingService.name, () => {
  describe('Params', () => {
    it('should handle Seitenbezug', () => {
      expect(NetzdetailRoutingService.buildParams([1, 4], 'LINKS').seite).toBe('LINKS');
      expect(NetzdetailRoutingService.buildParams([1, 4], 'RECHTS').seite).toBe('RECHTS');
      expect(NetzdetailRoutingService.buildParams([1, 4], KantenSeite.LINKS).seite).toBe('LINKS');
      expect(NetzdetailRoutingService.buildParams([1, 4], KantenSeite.RECHTS).seite).toBe('RECHTS');
      expect(NetzdetailRoutingService.buildParams([1, 4]).seite).toBe(undefined);
      expect(() => NetzdetailRoutingService.buildParams([1, 4], 'oben')).toThrow();
    });
  });
});
