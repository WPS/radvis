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

import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { ActivatedRoute, ActivatedRouteSnapshot, Data } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender, ngMocks } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { defaultMassnahmenDateianhaengeSessionFehlerUeberpruefen } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-test-data-provider.spec';
import { MassnahmenDateianhaengeImportSessionView } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-view';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';
import { ConfirmationDialogComponent } from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { MassnahmenDateianhaengeDuplikateUeberpruefenComponent } from './massnahmen-dateianhaenge-duplikate-ueberpruefen.component';
import { MatomoTracker } from 'ngx-matomo-client';

describe(MassnahmenDateianhaengeDuplikateUeberpruefenComponent.name, () => {
  let component: MassnahmenDateianhaengeDuplikateUeberpruefenComponent;
  let fixture: MockedComponentFixture<MassnahmenDateianhaengeDuplikateUeberpruefenComponent>;

  let service: MassnahmenDateianhaengeService;
  let routingService: MassnahmenDateianhaengeRoutingService;
  let notifyUserService: NotifyUserService;
  let route: ActivatedRoute;
  let dialog: MatDialog;
  let matomoTracker: MatomoTracker;

  let importSessionSubject: Subject<MassnahmenDateianhaengeImportSessionView | null>;

  ngMocks.faster();

  beforeAll(() => {
    service = mock(MassnahmenDateianhaengeService);
    routingService = mock(MassnahmenDateianhaengeRoutingService);
    notifyUserService = mock(NotifyUserService);
    route = mock(ActivatedRoute);
    dialog = mock(MatDialog);
    matomoTracker = mock(MatomoTracker);

    return MockBuilder(MassnahmenDateianhaengeDuplikateUeberpruefenComponent, ImportModule)
      .provide({ provide: MassnahmenDateianhaengeService, useValue: instance(service) })
      .provide({ provide: MassnahmenDateianhaengeRoutingService, useValue: instance(routingService) })
      .provide({ provide: NotifyUserService, useValue: instance(notifyUserService) })
      .provide({ provide: ActivatedRoute, useValue: instance(route) })
      .provide({ provide: MatDialog, useValue: instance(dialog) })
      .provide({ provide: MatomoTracker, useValue: instance(matomoTracker) });
  });

  beforeEach(() => {
    importSessionSubject = new Subject();

    when(service.getImportSession()).thenReturn(importSessionSubject);

    fixture = MockRender(MassnahmenDateianhaengeDuplikateUeberpruefenComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('with working session', () => {
    beforeAll(() => {
      when(route.snapshot).thenReturn({
        data: {
          session: { ...defaultMassnahmenDateianhaengeSessionFehlerUeberpruefen, schritt: 3 },
        } as Data,
      } as unknown as ActivatedRouteSnapshot);
    });

    it('should create', () => {
      expect(component).toBeTruthy();
      expect(component.massnahmen.size).toBe(1);
      expect(component.massnahmen.has('PassendeMassnahme')).toBeTrue();
      expect(
        component.massnahmen.get('PassendeMassnahme')?.map(row => {
          return {
            massnahmeId: row.massnahmeId,
            massnahmeKonzeptId: row.massnahmeKonzeptId,
            datei: row.datei,
            duplicate: row.duplicate,
            selected: row.selectionControl.value,
          };
        })
      ).toEqual([
        {
          massnahmeId: 4,
          massnahmeKonzeptId: 'PassendeMassnahme',
          datei: 'test.pdf',
          duplicate: false,
          selected: true,
        },
        {
          massnahmeId: 4,
          massnahmeKonzeptId: 'PassendeMassnahme',
          datei: 'test-bild.png',
          duplicate: false,
          selected: true,
        },
      ]);
    });

    it('should navigate to previous onPrevious', () => {
      component.onPrevious();

      verify(routingService.navigateToPrevious(3)).once();
      expect().nothing();
    });

    it('should navigate to first onAbort', () => {
      when(service.deleteImportSession()).thenReturn(of(undefined));

      component.onAbort();

      verify(routingService.navigateToFirst()).once();
      expect().nothing();
    });

    it('should build command correctly', () => {
      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => of(true),
      } as MatDialogRef<ConfirmationDialogComponent>);

      when(service.saveSelectedDateianhaengeCommand(anything())).thenReturn(of(undefined));
      when(service.getImportSession()).thenReturn(
        of({ ...defaultMassnahmenDateianhaengeSessionFehlerUeberpruefen, schritt: 4 })
      );

      component.onSave();

      expect(capture(service.saveSelectedDateianhaengeCommand).last()[0]).toEqual([
        {
          massnahmeKonzeptId: 'PassendeMassnahme',
          dateien: [
            {
              datei: 'test.pdf',
              selected: true,
            },
            {
              datei: 'test-bild.png',
              selected: true,
            },
          ],
        },
      ]);

      verify(routingService.navigateToNext(3)).once();
    });

    it('should not be able to continue if nothing is selected', () => {
      component.massnahmen.forEach(rows =>
        rows.forEach(row => row.selectionControl.setValue(false, { emitEvent: false }))
      );

      expect(component.canContinue).toBeFalse();
    });
  });
});
