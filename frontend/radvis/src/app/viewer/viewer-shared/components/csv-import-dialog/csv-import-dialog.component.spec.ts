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

import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MockComponent } from 'ng-mocks';
import { ActionButtonComponent } from 'src/app/form-elements/components/action-button/action-button.component';
import { FileUploadControlComponent } from 'src/app/form-elements/components/file-upload-control/file-upload-control.component';
import { ValidationErrorAnzeigeComponent } from 'src/app/form-elements/components/validation-error-anzeige/validation-error-anzeige.component';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';
import { CsvImportDialogComponent } from 'src/app/viewer/viewer-shared/components/csv-import-dialog/csv-import-dialog.component';
import { CsvImportService } from 'src/app/viewer/viewer-shared/services/csv-import.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

class TestCsvImportService implements CsvImportService {
  // eslint-disable-next-line no-unused-vars
  uploadCsv(file: File): Promise<Blob> {
    throw new Error('Method not implemented.');
  }

  afterUpload(): void {
    throw new Error('Method not implemented.');
  }

  openManual(): void {
    throw new Error('Method not implemented.');
  }
}

describe(CsvImportDialogComponent.name, () => {
  let component: CsvImportDialogComponent;
  let fixture: ComponentFixture<CsvImportDialogComponent>;
  let fileHandlingService: FileHandlingService;
  let csvImportService: CsvImportService;
  let manualRoutingService: ManualRoutingService;

  beforeEach(async () => {
    fileHandlingService = mock(FileHandlingService);
    csvImportService = mock(TestCsvImportService);
    manualRoutingService = mock(ManualRoutingService);

    await TestBed.configureTestingModule({
      declarations: [
        CsvImportDialogComponent,
        MockComponent(FileUploadControlComponent),
        MockComponent(ValidationErrorAnzeigeComponent),
        MockComponent(ActionButtonComponent),
      ],
      imports: [ReactiveFormsModule, MatSnackBarModule],
      providers: [
        { provide: FileHandlingService, useValue: instance(fileHandlingService) },
        { provide: CsvImportService, useValue: instance(csvImportService) },
        { provide: ManualRoutingService, useValue: instance(manualRoutingService) },
        { provide: MatDialogRef, useValue: instance(mock(MatDialogRef)) },
      ],
    }).compileComponents();
  });

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(CsvImportDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  describe('Form', () => {
    beforeEach(() => {
      component.formGroup.patchValue({
        csvImportFile: instance(mock(File)),
      });
    });

    it('should be valid', () => {
      expect(component.formGroup.valid).toBeTrue();
    });

    it('should be invalid if no csv-file', () => {
      component.formGroup.get('csvImportFile')?.reset();
      expect(component.formGroup.valid).toBeFalse();
    });
  });

  describe('onImport', () => {
    let mockFile: File;
    let csvImportFile: File;
    beforeEach(() => {
      mockFile = mock(File);
      csvImportFile = instance(mockFile);

      when(csvImportService.uploadCsv(anything())).thenResolve(new Blob());
      component.formGroup.patchValue({ csvImportFile });
    });

    it('should send correct command', fakeAsync(() => {
      component.onImport();
      tick();

      verify(csvImportService.uploadCsv(anything())).once();
      expect(capture(csvImportService.uploadCsv).last()[0]).toEqual(csvImportFile);
    }));

    it('should use correct name for filedownload', fakeAsync(() => {
      when(mockFile.name).thenReturn('leihst.csv');

      component.onImport();
      tick();

      verify(fileHandlingService.downloadInBrowser(anything(), anything())).once();
      expect(capture(fileHandlingService.downloadInBrowser).last()[1]).toEqual('Importprotokoll_leihst.csv');
    }));

    it('should refresh table if import button is clicked', fakeAsync(() => {
      component.onImport();
      tick();

      verify(csvImportService.afterUpload()).once();
      expect().nothing();
    }));
  });
});
