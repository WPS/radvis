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

import { ActivatedRoute, ActivatedRouteSnapshot, Data } from '@angular/router';
import { MockBuilder, MockRender, MockedComponentFixture, ngMocks } from 'ng-mocks';
import { of } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { defaultMassnahmenDateianhaengeSessionFehlerUeberpruefen } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-test-data-provider.spec';
import { MassnahmenDateianhaengeImportSessionView } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-view';
import {
  MassnahmenDateianhaengeImportMappingSeverity,
  MassnahmenDateianhaengeZuordnungStatus,
} from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-zuordnung';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';
import { instance, mock, verify, when } from 'ts-mockito';
import { MassnahmenDateianhaengeFehlerUeberpruefenComponent } from './massnahmen-dateianhaenge-fehler-ueberpruefen.component';

describe(MassnahmenDateianhaengeFehlerUeberpruefenComponent.name, () => {
  let component: MassnahmenDateianhaengeFehlerUeberpruefenComponent;
  let fixture: MockedComponentFixture<MassnahmenDateianhaengeFehlerUeberpruefenComponent>;

  let service: MassnahmenDateianhaengeService;
  let routingService: MassnahmenDateianhaengeRoutingService;
  let route: ActivatedRoute;

  ngMocks.faster();

  beforeAll(() => {
    service = mock(MassnahmenDateianhaengeService);
    routingService = mock(MassnahmenDateianhaengeRoutingService);
    route = mock(ActivatedRoute);

    return MockBuilder(MassnahmenDateianhaengeFehlerUeberpruefenComponent, ImportModule)
      .provide({ provide: MassnahmenDateianhaengeService, useValue: instance(service) })
      .provide({ provide: MassnahmenDateianhaengeRoutingService, useValue: instance(routingService) })
      .provide({ provide: ActivatedRoute, useValue: instance(route) });
  });

  describe('valid session zuordnungen', () => {
    beforeAll(() => {
      when(route.snapshot).thenReturn({
        data: {
          session: defaultMassnahmenDateianhaengeSessionFehlerUeberpruefen,
        } as Data,
      } as unknown as ActivatedRouteSnapshot);

      fixture = MockRender(MassnahmenDateianhaengeFehlerUeberpruefenComponent);
      component = fixture.point.componentInstance;
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should be able to continue to next step', () => {
      expect(component.canContinue).toBeTrue();
    });

    it('should delete session and navigate to first step onAbort', () => {
      when(service.deleteImportSession()).thenReturn(of(undefined));

      component.onAbort();

      verify(routingService.navigateToFirst()).once();
      expect().nothing();
    });

    it('should navigate to previous step onPrevious', () => {
      component.onPrevious();

      verify(routingService.navigateToPrevious(2)).once();
      expect().nothing();
    });

    it('should navigate to next step onNext', () => {
      when(service.continueAfterFehlerUeberpruefen()).thenReturn(of(undefined));

      component.onNext();

      verify(routingService.navigateToNext(2)).once();
      expect().nothing();
    });
  });

  describe('invalid session zuordnungen', () => {
    beforeAll(() => {
      when(route.snapshot).thenReturn({
        data: {
          session: {
            ...defaultMassnahmenDateianhaengeSessionFehlerUeberpruefen,
            zuordnungen: [
              {
                ordnername: 'NichtFindbareMassnahmenID',
                massnahmeId: null,
                status: MassnahmenDateianhaengeZuordnungStatus.FEHLERHAFT,
                dateien: [
                  {
                    dateiname: 'test.pdf',
                    isDuplicate: false,
                    isSelected: false,
                  },
                  {
                    dateiname: 'test-bild.png',
                    isDuplicate: false,
                    isSelected: false,
                  },
                ],
                hinweis: {
                  text: 'Maßnahme NichtFindbareMassnahmenID wurde nicht gefunden',
                  severity: MassnahmenDateianhaengeImportMappingSeverity.ERROR,
                },
              },
            ],
          } as MassnahmenDateianhaengeImportSessionView,
        } as Data,
      } as unknown as ActivatedRouteSnapshot);

      fixture = MockRender(MassnahmenDateianhaengeFehlerUeberpruefenComponent);
      component = fixture.point.componentInstance;
      fixture.detectChanges();
    });

    it('should not be able to continue to next step', () => {
      expect(component.canContinue).toBeFalse();
    });
  });
});
