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
import { TransformAttributeDialogComponent } from 'src/app/import/attribute/components/transform-attribute-dialog/transform-attribute-dialog.component';
import { TransformShpService } from 'src/app/import/services/transform-shp.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';
import { anything, capture, instance, mock, strictEqual, verify, when } from 'ts-mockito';

describe(TransformAttributeDialogComponent.name, () => {
  let component: TransformAttributeDialogComponent;
  let fixture: ComponentFixture<TransformAttributeDialogComponent>;
  let fileHandlingService: FileHandlingService;
  let transformShpService: TransformShpService;
  let manualRoutingService: ManualRoutingService;

  beforeEach(async () => {
    fileHandlingService = mock(FileHandlingService);
    transformShpService = mock(TransformShpService);
    manualRoutingService = mock(ManualRoutingService);

    await TestBed.configureTestingModule({
      declarations: [
        TransformAttributeDialogComponent,
        MockComponent(FileUploadControlComponent),
        MockComponent(ValidationErrorAnzeigeComponent),
        MockComponent(ActionButtonComponent),
      ],
      imports: [ReactiveFormsModule, MatSnackBarModule],
      providers: [
        { provide: FileHandlingService, useValue: instance(fileHandlingService) },
        { provide: TransformShpService, useValue: instance(transformShpService) },
        { provide: ManualRoutingService, useValue: instance(manualRoutingService) },
        { provide: MatDialogRef, useValue: instance(mock(MatDialogRef)) },
      ],
    }).compileComponents();
  });

  beforeEach(
    waitForAsync(() => {
      fixture = TestBed.createComponent(TransformAttributeDialogComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    })
  );

  describe(TransformAttributeDialogComponent.prototype.openManualEditorTransformation.name, () => {
    beforeEach(() => {
      component.openManualEditorTransformation();
    });

    it('should invoke openManualTransformation', () => {
      verify(manualRoutingService.openManualEditorTransformation()).once();
      expect().nothing();
    });
  });

  describe('Form', () => {
    beforeEach(() => {
      component.formGroup.patchValue({
        transformationFile: instance(mock(File)),
        shpFile: instance(mock(File)),
      });
    });

    it('should be valid', () => {
      expect(component.formGroup.valid).toBeTrue();
    });

    it('should be invalid if no shp-file', () => {
      component.formGroup.get('shpFile')?.reset();
      expect(component.formGroup.valid).toBeFalse();
    });

    it('should be invalid if no transformation-file', () => {
      component.formGroup.get('transformationFile')?.reset();
      expect(component.formGroup.valid).toBeFalse();
    });
  });

  describe('onTransform', () => {
    it('should send correct command', fakeAsync(() => {
      const blob = new Blob(['test123'], { type: 'text/plain' });
      const sFile = new File([blob], 'sfile.shp', { type: 'text/plain' });
      const tFile = new File([blob], 'tfile.csv', { type: 'text/plain' });

      when(transformShpService.startShpTransform(anything(), anything())).thenResolve(new Blob());

      component.formGroup.patchValue({
        shpFile: sFile,
        transformationFile: tFile,
      });

      component.onTransform();
      tick();

      verify(transformShpService.startShpTransform(anything(), anything())).once();
      expect(capture(transformShpService.startShpTransform).last()[0]).toEqual(sFile);
      expect(capture(transformShpService.startShpTransform).last()[1]).toEqual(tFile);
    }));
    it('should use correct name for filedownload', fakeAsync(() => {
      const mockFile = mock(File);
      when(mockFile.name).thenReturn('sakura.zip');
      const shpFile = instance(mockFile);

      const transformationFile = instance(mock(File));

      when(transformShpService.startShpTransform(anything(), anything())).thenResolve(new Blob());

      component.formGroup.patchValue({
        shpFile,
        transformationFile,
      });

      component.onTransform();
      tick();

      verify(fileHandlingService.downloadInBrowser(anything(), strictEqual('transformed_sakura.zip'))).once();
      expect().nothing();
    }));
  });
});
