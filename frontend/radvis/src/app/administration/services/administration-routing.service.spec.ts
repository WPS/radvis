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
import { deepEqual, instance, mock, verify } from 'ts-mockito';
import { AdministrationRoutingService } from './administration-routing.service';

describe(AdministrationRoutingService.name, () => {
  let administrationRoutingService: AdministrationRoutingService;

  let router: Router;

  beforeEach(() => {
    router = mock(Router);

    administrationRoutingService = new AdministrationRoutingService(instance(router));
  });

  describe(AdministrationRoutingService.prototype.toAdministration.name, () => {
    beforeEach(() => {
      administrationRoutingService.toAdministration();
    });

    it('should invoke navigate', () => {
      verify(
        router.navigate(
          deepEqual([AdministrationRoutingService.ADMINISTRATION_BENUTZER_ROUTE]),
          deepEqual({ queryParamsHandling: 'merge' })
        )
      ).once();
      expect().nothing();
    });
  });

  describe(AdministrationRoutingService.prototype.toBenutzerEditor.name, () => {
    let benutzerId: number;

    beforeEach(() => {
      benutzerId = 42;

      administrationRoutingService.toBenutzerEditor(benutzerId);
    });

    it('should invoke navigate', () => {
      verify(
        router.navigate(
          deepEqual([AdministrationRoutingService.ADMINISTRATION_BENUTZER_ROUTE, benutzerId]),
          deepEqual({ queryParamsHandling: 'merge' })
        )
      ).once();
      expect().nothing();
    });
  });
});
