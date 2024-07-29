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

import { waitForAsync } from '@angular/core/testing';
import { first, takeWhile } from 'rxjs/operators';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';

describe('NetzAusblendenService', () => {
  let service: NetzAusblendenService;

  beforeEach(() => {
    service = new NetzAusblendenService();
  });

  describe('Kanten', () => {
    it('should ausblenden', waitForAsync(() => {
      service.kanteAusblenden$.pipe(first()).subscribe(id => {
        expect(id).toBe(1);
      });
      service.kanteAusblenden(1);

      service.kanteAusblenden$.pipe(first()).subscribe(id => {
        expect(id).toBe(3);
      });
      service.kanteAusblenden(3);
    }));

    it('should einblenden', waitForAsync(() => {
      service.kanteEinblenden$.pipe(first()).subscribe(id => {
        expect(id).toBe(1);
      });

      service.kanteAusblenden(1);
      service.kanteEinblenden(1);
    }));

    it('should throw on einblenden if not ausgeblendet', waitForAsync(() => {
      service.kanteAusblenden(1);

      expect(() => service.kanteEinblenden(2)).toThrow();
    }));

    it('should count multiple einblenden/ausblenden', waitForAsync(() => {
      let einblendenCounter = 0;
      let ausblendenCounter = 0;
      service.kanteEinblenden$.pipe(takeWhile(id => id === 2)).subscribe(() => einblendenCounter++);
      service.kanteAusblenden$.pipe(takeWhile(id => id === 2)).subscribe(() => ausblendenCounter++);

      service.kanteAusblenden(2);
      expect(ausblendenCounter).toBe(1);

      service.kanteAusblenden(2);
      expect(ausblendenCounter).toBe(1);

      service.kanteEinblenden(2);
      expect(einblendenCounter).toBe(0);

      service.kanteEinblenden(2);
      expect(einblendenCounter).toBe(1);
    }));
  });

  describe('Knoten', () => {
    it('should ausblenden', waitForAsync(() => {
      service.knotenAusblenden$.pipe(first()).subscribe(id => {
        expect(id).toBe(1);
      });
      service.knotenAusblenden(1);

      service.knotenAusblenden$.pipe(first()).subscribe(id => {
        expect(id).toBe(3);
      });
      service.knotenAusblenden(3);
    }));

    it('should einblenden', waitForAsync(() => {
      service.knotenEinblenden$.pipe(first()).subscribe(id => {
        expect(id).toBe(1);
      });

      service.knotenAusblenden(1);
      service.knotenEinblenden(1);
    }));

    it('should throw on einblenden if not ausgeblendet', waitForAsync(() => {
      service.knotenAusblenden(1);

      expect(() => service.knotenEinblenden(2)).toThrow();
    }));

    it('should count multiple einblenden/ausblenden', waitForAsync(() => {
      let einblendenCounter = 0;
      let ausblendenCounter = 0;
      service.knotenEinblenden$.pipe(takeWhile(id => id === 2)).subscribe(() => einblendenCounter++);
      service.knotenAusblenden$.pipe(takeWhile(id => id === 2)).subscribe(() => ausblendenCounter++);

      service.knotenAusblenden(2);
      expect(ausblendenCounter).toBe(1);

      service.knotenAusblenden(2);
      expect(ausblendenCounter).toBe(1);

      service.knotenEinblenden(2);
      expect(einblendenCounter).toBe(0);

      service.knotenEinblenden(2);
      expect(einblendenCounter).toBe(1);
    }));
  });
});
