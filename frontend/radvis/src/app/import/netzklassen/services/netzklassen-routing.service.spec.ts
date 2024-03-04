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

import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { Router } from '@angular/router';
import { anything, deepEqual, instance, mock, verify } from 'ts-mockito';

describe(NetzklassenRoutingService.name, () => {
  let service: NetzklassenRoutingService;
  let router: Router;

  beforeEach(() => {
    router = mock(Router);
    service = new NetzklassenRoutingService(instance(router));
  });

  describe(NetzklassenRoutingService.prototype.navigateToFirst.name, () => {
    it('should route to correct start step', () => {
      service.navigateToFirst();

      verify(
        router.navigate(deepEqual([anything(), anything(), NetzklassenRoutingService.DATEI_UPLOAD_ROUTE]), anything())
      ).once();
      expect().nothing();
    });
  });

  describe(NetzklassenRoutingService.prototype.navigateToNext.name, () => {
    it('should route to correct next step', () => {
      service.navigateToNext(2);

      verify(
        router.navigate(
          deepEqual([anything(), anything(), NetzklassenRoutingService.AUTOMATISCHE_ABBILDUNG_ROUTE]),
          anything()
        )
      ).once();
      expect().nothing();
    });
  });

  describe(NetzklassenRoutingService.prototype.navigateToPrevious.name, () => {
    it('should route to correct previous step', () => {
      service.navigateToPrevious(4);

      verify(
        router.navigate(
          deepEqual([anything(), anything(), NetzklassenRoutingService.AUTOMATISCHE_ABBILDUNG_ROUTE]),
          anything()
        )
      ).once();
      expect().nothing();
    });
  });

  describe(NetzklassenRoutingService.prototype.navigateToStep.name, () => {
    [
      {
        step: 1,
        path: NetzklassenRoutingService.DATEI_UPLOAD_ROUTE,
      },
      {
        step: 2,
        path: NetzklassenRoutingService.PARAMETER_EINGEBEN_ROUTE,
      },
      {
        step: 3,
        path: NetzklassenRoutingService.AUTOMATISCHE_ABBILDUNG_ROUTE,
      },
      {
        step: 4,
        path: NetzklassenRoutingService.KORREKTUR_ROUTE,
      },
      {
        step: 5,
        path: NetzklassenRoutingService.ABSCHLUSS_ROUTE,
      },
    ].forEach(({ step, path }) => {
      it(`should route to correct schritt ${step}`, () => {
        service.navigateToStep(step);
        verify(router.navigate(deepEqual([anything(), anything(), path]), anything())).once();
        expect().nothing();
      });
    });
  });
});
