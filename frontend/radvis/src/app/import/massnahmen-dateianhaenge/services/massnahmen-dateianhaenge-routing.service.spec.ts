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
import { ImportRoutes } from 'src/app/import/models/import-routes';
import { anything, deepEqual, instance, mock, verify } from 'ts-mockito';
import { MassnahmenDateianhaengeRoutingService } from './massnahmen-dateianhaenge-routing.service';

describe(MassnahmenDateianhaengeRoutingService.name, () => {
  let service: MassnahmenDateianhaengeRoutingService;
  let router: Router;

  beforeEach(() => {
    router = mock(Router);
    service = new MassnahmenDateianhaengeRoutingService(instance(router));
  });

  describe(MassnahmenDateianhaengeRoutingService.prototype.navigateToFirst.name, () => {
    it('should route to correct start step', () => {
      service.navigateToFirst();

      verify(
        router.navigate(
          deepEqual([
            ImportRoutes.IMPORT_ROUTE,
            ImportRoutes.MASSNAHMEN_DATEIANHAENGE_IMPORT_ROUTE,
            MassnahmenDateianhaengeRoutingService.DATEI_UPLOAD_ROUTE,
          ]),
          anything()
        )
      ).once();
      expect().nothing();
    });
  });

  describe(MassnahmenDateianhaengeRoutingService.prototype.navigateToStep.name, () => {
    [
      { step: 1, path: MassnahmenDateianhaengeRoutingService.DATEI_UPLOAD_ROUTE },
      { step: 2, path: MassnahmenDateianhaengeRoutingService.FEHLER_UEBERPRUEFEN_ROUTE },
      { step: 3, path: MassnahmenDateianhaengeRoutingService.DUPLIKATE_UEBERPRUEFEN_ROUTE },
      { step: 4, path: MassnahmenDateianhaengeRoutingService.FEHLERPROTOKOLL_HERUNTERLADEN_ROUTE },
    ].forEach(({ step, path }) => {
      it(`should route to correct step ${step}`, () => {
        service.navigateToStep(step);

        verify(
          router.navigate(
            deepEqual([ImportRoutes.IMPORT_ROUTE, ImportRoutes.MASSNAHMEN_DATEIANHAENGE_IMPORT_ROUTE, path]),
            anything()
          )
        ).once();
        expect().nothing();
      });
    });
  });

  describe(MassnahmenDateianhaengeRoutingService.prototype.navigateToNext.name, () => {
    it('should route to correct next step', () => {
      service.navigateToNext(1);

      verify(
        router.navigate(
          deepEqual([
            ImportRoutes.IMPORT_ROUTE,
            ImportRoutes.MASSNAHMEN_DATEIANHAENGE_IMPORT_ROUTE,
            MassnahmenDateianhaengeRoutingService.FEHLER_UEBERPRUEFEN_ROUTE,
          ]),
          anything()
        )
      ).once();
      expect().nothing();
    });
  });

  describe(MassnahmenDateianhaengeRoutingService.prototype.navigateToPrevious.name, () => {
    it('should route to correct previous step', () => {
      service.navigateToPrevious(2);

      verify(
        router.navigate(
          deepEqual([
            ImportRoutes.IMPORT_ROUTE,
            ImportRoutes.MASSNAHMEN_DATEIANHAENGE_IMPORT_ROUTE,
            MassnahmenDateianhaengeRoutingService.DATEI_UPLOAD_ROUTE,
          ]),
          anything()
        )
      ).once();
      expect().nothing();
    });
  });
});