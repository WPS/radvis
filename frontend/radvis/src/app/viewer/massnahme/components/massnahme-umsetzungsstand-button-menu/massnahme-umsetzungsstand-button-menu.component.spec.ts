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

import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MockBuilder } from 'ng-mocks';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { getTestMassnahmeListenViews } from 'src/app/viewer/massnahme/models/massnahme-listen-view-test-data-provider.spec';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { MassnahmeUmsetzungsstandButtonMenuComponent } from './massnahme-umsetzungsstand-button-menu.component';

describe(MassnahmeUmsetzungsstandButtonMenuComponent.name, () => {
  let component: MassnahmeUmsetzungsstandButtonMenuComponent;
  let fixture: ComponentFixture<MassnahmeUmsetzungsstandButtonMenuComponent>;

  const testMassnahmen = getTestMassnahmeListenViews();
  const massnahmenIds = testMassnahmen.map(m => m.id);

  let massnahmeService: MassnahmeService;
  let massnahmeFilterService: MassnahmeFilterService;
  let fileHandlingService: FileHandlingService;
  let errorHandlingService: ErrorHandlingService;
  let notifyUserService: NotifyUserService;

  beforeEach(() => {
    massnahmeService = mock(MassnahmeService);
    massnahmeFilterService = mock(MassnahmeFilterService);
    fileHandlingService = mock(FileHandlingService);
    errorHandlingService = mock(ErrorHandlingService);
    notifyUserService = mock(NotifyUserService);

    return MockBuilder(MassnahmeUmsetzungsstandButtonMenuComponent, ViewerModule)
      .provide({ provide: MatDialog, useValue: mock(MatDialog) })
      .provide({ provide: MassnahmeService, useValue: instance(massnahmeService) })
      .provide({ provide: MassnahmeFilterService, useValue: instance(massnahmeFilterService) })
      .provide({ provide: FileHandlingService, useValue: instance(fileHandlingService) })
      .provide({ provide: NotifyUserService, useValue: instance(notifyUserService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(errorHandlingService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MassnahmeUmsetzungsstandButtonMenuComponent);
    component = fixture.componentInstance;
  });

  describe('downloadAuswertung', () => {
    let blob: Blob;
    const fileName = '2022-06-20_Auswertung.csv';
    const headers = new HttpHeaders({
      'content-disposition': 'attachment; filename=' + fileName,
      'content-type': 'text/csv;charset=utf-8',
    });

    beforeEach(() => {
      blob = new Blob();
      when(massnahmeFilterService.currentFilteredList).thenReturn(testMassnahmen);
      when(massnahmeService.auswertungHerunterladen(anything())).thenResolve(
        new HttpResponse({ body: blob, headers, status: 200 })
      );
    });

    it('should download Auswertung', fakeAsync(() => {
      component.downloadAuswertung();

      tick();
      expect(capture(massnahmeService.auswertungHerunterladen).last()[0]).toEqual(massnahmenIds);
      verify(fileHandlingService.downloadInBrowser(blob, fileName)).once();
    }));

    it('should notify user on server error', fakeAsync(() => {
      when(massnahmeService.auswertungHerunterladen(anything())).thenReject({ name: 'err', message: 'keine' });

      component.downloadAuswertung();

      tick();
      verify(fileHandlingService.downloadInBrowser(anything(), anything())).never();
      verify(errorHandlingService.handleError(anything(), anything())).once();
      expect().nothing();
    }));

    it('should notify user on empty response body', fakeAsync(() => {
      when(massnahmeService.auswertungHerunterladen(anything())).thenResolve(
        new HttpResponse({ headers, status: 200 })
      );

      component.downloadAuswertung();

      tick();
      verify(fileHandlingService.downloadInBrowser(anything(), anything())).never();
      verify(notifyUserService.warn(anything())).once();
      expect().nothing();
    }));

    it('should notify user on filehandling error', fakeAsync(() => {
      when(fileHandlingService.downloadInBrowser(anything(), anything())).thenThrow({ name: 'err', message: 'keine' });

      component.downloadAuswertung();

      tick();
      verify(notifyUserService.warn(anything())).once();
      expect().nothing();
    }));
  });
});
