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

import { Router } from '@angular/router';
import { MatomoTracker } from 'ngx-matomo-client';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';
import { ManualRoutingService } from './manual-routing.service';

describe(ManualRoutingService.name, () => {
  let manualRoutingService: ManualRoutingService;
  let router: Router;

  beforeEach(() => {
    router = mock(Router);
    manualRoutingService = new ManualRoutingService(instance(router), instance(mock(MatomoTracker)));
  });

  describe('with url and openSpy', () => {
    let url: string;

    beforeEach(() => {
      url = 'WAMBO!';
      when(router.serializeUrl(anything())).thenReturn(url);
      window.open = jasmine.createSpy();
    });

    describe(ManualRoutingService.prototype.openManual.name, () => {
      beforeEach(() => {
        manualRoutingService.openManual();
      });

      it('should create url with manual url', () => {
        verify(router.createUrlTree(deepEqual([ManualRoutingService.MANUAL_URL]))).once();
        expect().nothing();
      });

      it('should open url with target _blank', () => {
        expect(window.open).toHaveBeenCalledWith(url, '_blank');
      });
    });

    describe(ManualRoutingService.prototype.openManualImportTransformation.name, () => {
      beforeEach(() => {
        manualRoutingService.openManualImportTransformation();
      });

      it('should create url with manual url', () => {
        verify(
          router.createUrlTree(
            deepEqual([ManualRoutingService.MANUAL_IMPORT_URL]),
            deepEqual({ fragment: 'transformation' })
          )
        ).once();
        expect().nothing();
      });

      it('should open url with target _blank', () => {
        expect(window.open).toHaveBeenCalledWith(url, '_blank');
      });
    });
  });
});
