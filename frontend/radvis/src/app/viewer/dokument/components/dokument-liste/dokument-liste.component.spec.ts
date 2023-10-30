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

import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { ActivatedRoute, ActivatedRouteSnapshot, convertToParamMap, Data } from '@angular/router';
import { MockBuilder } from 'ng-mocks';
import { BehaviorSubject, Subject } from 'rxjs';
import { ConfirmationDialogComponent } from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { MassnahmeNetzbezugDisplayService } from 'src/app/viewer/massnahme/services/massnahme-netzbezug-display.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { DokumentListeComponent } from 'src/app/viewer/dokument/components/dokument-liste/dokument-liste.component';
import { DokumentView } from 'src/app/viewer/dokument/models/dokument-view';
import { DokumentListeView } from 'src/app/viewer/dokument/models/dokument-liste-view';
import { DokumentService } from 'src/app/viewer/dokument/services/dokument.service';
import { AddDokumentCommand } from 'src/app/viewer/dokument/models/add-dokument-command';

class TestDokumentService implements DokumentService {
  // eslint-disable-next-line no-unused-vars
  addDokument(command: AddDokumentCommand): Promise<void> {
    throw new Error('not implemented');
  }

  // eslint-disable-next-line no-unused-vars
  deleteDokument(dokumentId: number): Promise<void> {
    throw new Error('not implemented');
  }

  // eslint-disable-next-line no-unused-vars
  downloadDokument(dokumentId: number): Promise<Blob> {
    throw new Error('not implemented');
  }

  // eslint-disable-next-line no-unused-vars
  getDokumentListe(): Promise<DokumentListeView> {
    throw new Error('not implemented');
  }

  afterDokumentListeInit(): void {
    throw new Error('not implemented');
  }
}

describe(DokumentListeComponent.name, () => {
  let component: DokumentListeComponent;
  let fixture: ComponentFixture<DokumentListeComponent>;
  let dokumentService: DokumentService;
  let fileHandlingService: FileHandlingService;
  let notifyUserService: NotifyUserService;
  let activatedRoute: ActivatedRoute;
  let changeDetectorRef: ChangeDetectorRef;
  let errorHandlingService: ErrorHandlingService;
  let dataSubject: BehaviorSubject<Data>;
  let massnahmeNetzbezugDisplayService: MassnahmeNetzbezugDisplayService;
  let dialog: MatDialog;

  const massnahmeId = 33;
  let dokumentView: DokumentView;

  beforeEach(() => {
    fileHandlingService = mock(FileHandlingService);
    activatedRoute = mock(ActivatedRoute);

    const dokumente: DokumentListeView = { dokumente: [], canEdit: true };
    dataSubject = new BehaviorSubject({ dokumente } as Data);

    massnahmeNetzbezugDisplayService = {
      showNetzbezug: (): void => {},
    };
    dokumentView = {
      dokumentId: 2,
      dateiname: 'file.txt',
      dateigroesseInBytes: 80,
      benutzerVorname: 'Vorname',
      benutzerNachname: 'Nachname',
      datum: '2022-06-16T14:12:07.988447',
    } as DokumentView;

    when(activatedRoute.data).thenReturn(dataSubject.asObservable());
    when(activatedRoute.parent).thenReturn({
      snapshot: {
        paramMap: convertToParamMap({ id: `${massnahmeId}` }),
      } as ActivatedRouteSnapshot,
    } as ActivatedRoute);

    dokumentService = mock(TestDokumentService);
    changeDetectorRef = mock(ChangeDetectorRef);
    notifyUserService = mock(NotifyUserService);
    errorHandlingService = mock(ErrorHandlingService);
    dialog = mock(MatDialog);

    return MockBuilder(DokumentListeComponent, ViewerModule)
      .provide({
        provide: FileHandlingService,
        useValue: instance(fileHandlingService),
      })
      .provide({
        provide: ActivatedRoute,
        useValue: instance(activatedRoute),
      })
      .provide({
        provide: DokumentService,
        useValue: instance(dokumentService),
      })
      .provide({
        provide: ChangeDetectorRef,
        useValue: instance(changeDetectorRef),
      })
      .provide({
        provide: ErrorHandlingService,
        useValue: instance(errorHandlingService),
      })
      .provide({
        provide: NotifyUserService,
        useValue: instance(notifyUserService),
      })
      .provide({
        provide: MassnahmeNetzbezugDisplayService,
        useValue: massnahmeNetzbezugDisplayService,
      })
      .provide({
        provide: MatDialog,
        useValue: instance(dialog),
      });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DokumentListeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('upload', () => {
    beforeEach(fakeAsync(() => {
      const dokumente: DokumentListeView = { dokumente: [dokumentView], canEdit: true };
      when(dokumentService.getDokumentListe()).thenReturn(Promise.resolve(dokumente));
    }));

    it('should handle positive response correctly', fakeAsync(() => {
      when(dokumentService.addDokument(anything(), anything())).thenReturn(Promise.resolve());

      component.formGroup.patchValue({ file: new File([], 'file.txt') });
      component.onUpload();
      tick();
      verify(dokumentService.addDokument(anything(), anything())).once();
      verify(dokumentService.getDokumentListe()).once();
      expect(component.dokumentListeView.dokumente).toHaveSize(1);
      expect(component.dokumentListeView.dokumente).toContain(dokumentView);
    }));

    it('should handle error response correctly', fakeAsync(() => {
      when(dokumentService.addDokument(anything(), anything())).thenReturn(Promise.reject());

      component.formGroup.patchValue({ file: new File([], 'file.txt') });
      component.onUpload();
      tick();
      verify(dokumentService.addDokument(anything(), anything())).once();
      verify(dokumentService.getDokumentListe()).never();
      expect(component.dokumentListeView.dokumente).toHaveSize(0);
    }));
  });

  describe('download', () => {
    it('should download file from service', fakeAsync(() => {
      const dokumente: DokumentListeView = { canEdit: true, dokumente: [dokumentView] };
      dataSubject.next({
        dokumente,
      });
      tick();

      when(dokumentService.downloadDokument(anything())).thenReturn(Promise.resolve(new Blob([])));
      component.onDownload(dokumentView);

      tick();

      verify(dokumentService.downloadDokument(anything())).once();
      expect(capture(dokumentService.downloadDokument).last()).toEqual([2]);
    }));
  });

  describe('delete', () => {
    let closeSubject: Subject<boolean>;
    let dokumentListe: DokumentView[];

    it('should delete and update liste', fakeAsync(() => {
      dokumentListe = [dokumentView, { dokumentId: 123, datum: '2022-06-16T14:12:07.988447' } as DokumentView];
      closeSubject = new Subject<boolean>();

      const dokumente1: DokumentListeView = { canEdit: true, dokumente: [dokumentView] };
      dataSubject.next({
        dokumente: dokumente1,
      });

      tick();

      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => closeSubject.asObservable(),
      } as MatDialogRef<ConfirmationDialogComponent>);

      when(dokumentService.deleteDokument(anything())).thenReturn(Promise.resolve());

      const dokumente2: DokumentListeView = { dokumente: dokumentListe, canEdit: true };
      when(dokumentService.getDokumentListe()).thenReturn(Promise.resolve(dokumente2));

      component.onDelete(dokumentView);
      closeSubject.next(true);

      tick();

      verify(dokumentService.deleteDokument(anything())).once();
      expect(capture(dokumentService.deleteDokument).last()).toEqual([2]);
      verify(notifyUserService.inform(anything())).once();
      expect(component.dokumentListeView.dokumente).toEqual(dokumentListe);
    }));
  });
});
