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
import { skip } from 'rxjs/operators';
import { FehlerprotokollSelectionService } from './fehlerprotokoll-selection.service';

describe(FehlerprotokollSelectionService.name, () => {
  let service: FehlerprotokollSelectionService;

  beforeEach(() => {
    service = new FehlerprotokollSelectionService();
  });

  describe('selection', () => {
    it('should not populate internal array (Fehlerprotokoll)', () => {
      const typ1 = { displayText: '', name: 'TEST1', minZoom: 9 };
      service.selectFehlerprotokoll(typ1);

      service.selectedFehlerprotokollTypen.push(typ1);

      expect(service.selectedFehlerprotokollTypen).toHaveSize(1);
    });

    it('should not populate internal array (Konsistenzregel)', () => {
      const konsistenzregel = { verletzungsTyp: 'Test', regelGruppe: '', titel: '' };
      service.selectKonsistenzregel(konsistenzregel);

      service.selectedKonsistenzregelVerletzungen.push(konsistenzregel.verletzungsTyp);

      expect(service.selectedKonsistenzregelVerletzungen).toHaveSize(1);
    });
  });

  describe('minZoom', () => {
    it('should use max minZoom from selected FehlerprotokollTypen', () => {
      const typ1 = { displayText: '', name: 'TEST1', minZoom: 9 };
      service.selectFehlerprotokoll(typ1);
      expect(service.minZoom).toBe(9);

      const typ2 = { displayText: '', name: 'TEST2', minZoom: 8 };
      service.selectFehlerprotokoll(typ2);
      expect(service.minZoom).toBe(9);

      service.unselectFehlerprotokoll(typ1);
      expect(service.minZoom).toBe(8);

      const typ3 = { displayText: '', name: 'TEST3', minZoom: 10 };
      service.selectFehlerprotokoll(typ3);
      expect(service.minZoom).toBe(10);
    });

    it('should notify observable if typ selected', (done: DoneFn) => {
      service.minZoom$.pipe(skip(1)).subscribe(minZoom => {
        expect(minZoom).toBe(9);
        done();
      });
      const typ1 = { displayText: '', name: 'TEST1', minZoom: 9 };
      service.selectFehlerprotokoll(typ1);
    });

    it('should notify observable if konsistenzregel selected', (done: DoneFn) => {
      const typ1 = { displayText: '', name: 'TEST1', minZoom: 9 };
      service.selectFehlerprotokoll(typ1);

      service.minZoom$.pipe(skip(1)).subscribe(minZoom => {
        expect(minZoom).toBe(service['DEFAULT_MIN_ZOOM']);
        done();
      });

      service.selectKonsistenzregel({ verletzungsTyp: 'Test', regelGruppe: '', titel: '' });
    });

    it('should not notify observable if zoom not changed', waitForAsync(() => {
      let counter = 0;
      service.minZoom$.pipe(skip(1)).subscribe(minZoom => {
        counter++;
      });
      const typ1 = { displayText: '', name: 'TEST1', minZoom: 9 };
      service.selectFehlerprotokoll(typ1);

      const typ2 = { displayText: '', name: 'TEST2', minZoom: 8 };
      service.selectFehlerprotokoll(typ2);

      expect(counter).toBe(1);
    }));

    it('should use max minZoom from Konsistenzregel And Fehlerprotokoll', () => {
      const typ1 = { displayText: '', name: 'TEST1', minZoom: 9 };
      service.selectFehlerprotokoll(typ1);
      expect(service.minZoom).toBe(9);

      const konsistenzregel = { verletzungsTyp: 'Test', regelGruppe: '', titel: '' };
      service.selectKonsistenzregel(konsistenzregel);
      expect(service.minZoom).toBe(service['DEFAULT_MIN_ZOOM']);

      service.unselectKonsistenzregel(konsistenzregel);
      expect(service.minZoom).toBe(9);
    });

    it('should have default if no Fehlerprotokoll selected', () => {
      expect(service.minZoom).toBe(service['DEFAULT_MIN_ZOOM']);

      service.selectKonsistenzregel({ verletzungsTyp: 'Test', regelGruppe: '', titel: '' });

      expect(service.minZoom).toBe(service['DEFAULT_MIN_ZOOM']);
    });
  });
});
