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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FileUploadControlComponent } from './file-upload-control.component';

describe(FileUploadControlComponent.name, () => {
  let component: FileUploadControlComponent;
  let fixture: ComponentFixture<FileUploadControlComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [FileUploadControlComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(FileUploadControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('writeValue', () => {
    it('should update file name', () => {
      const fileName = 'test.gpg';
      component.writeValue(new File([], fileName));
      expect(component.formControl.value).toEqual(fileName);
    });

    it('should reset file name if no value', () => {
      component.formControl.setValue('test');
      component.writeValue(null);
      expect(component.formControl.value).toBeFalsy();
    });

    it('should reset file input if no value', () => {
      component.formControl.setValue('test');
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(new File([''], 'test'));
      component.fileUploadInput!.nativeElement.files = dataTransfer.files;

      component.writeValue(null);
      expect(component.formControl.value).toBeFalsy();
      expect(component.fileUploadInput?.nativeElement.value).toEqual('');
    });
  });

  describe('validate', () => {
    describe('dateiendung', () => {
      it('should be valid if matching dateiEndung', () => {
        component.dateiEndung = 'gpg';
        component.formControl.setValue('test.gpg');
        expect(component.validate()).toBeNull();
      });

      it('should be valid if no file', () => {
        component.dateiEndung = 'gpg';
        expect(component.validate()).toBeNull();
      });

      it('should be valid if no dateiEndung', () => {
        component.formControl.setValue('test.gpg');
        expect(component.validate()).toBeNull();
      });

      it('should be invalid if dateiEndung not matching', () => {
        component.dateiEndung = 'gpg';
        component.formControl.setValue('test.blubb');
        expect(component.validate()).not.toBeNull();
        expect(component.validate()?.fileNameMismatch).not.toBeUndefined();
      });

      describe('multiple Endungen allowed', () => {
        it('should be valid if matching dateiEndung exists', () => {
          component.dateiEndung = ['gpg', 'ddd'];
          component.formControl.setValue('test.gpg');
          expect(component.validate()).toBeNull();
        });

        it('should be valid if no file', () => {
          component.dateiEndung = ['gpg', 'ddd'];
          expect(component.validate()).toBeNull();
        });

        it('should be valid if empty List of dateiEndung', () => {
          component.dateiEndung = [];
          component.formControl.setValue('test.gpg');
          expect(component.validate()).toBeNull();
        });

        it('should be invalid if no dateiEndung is matching', () => {
          component.dateiEndung = ['gpg', 'ddd'];
          component.formControl.setValue('test.blubb');
          expect(component.validate()).not.toBeNull();
          expect(component.validate()?.fileNameMismatch).not.toBeUndefined();
        });
      });
    });
    describe('maxFileSize', () => {
      it('fileSize set and below maxFileSize no error ', () => {
        component.fileSizeInMB = 49.99;
        component.maxFileSizeInMB = 50;
        expect(component.validate()).toBeNull();
      });

      it('fileSize set and below maxFileSize', () => {
        component.fileSizeInMB = 50.1;
        component.maxFileSizeInMB = 50;
        expect(component.validate()).not.toBeNull();
        expect(component.validate()?.fileNameMismatch).toBeUndefined();
        expect(component.validate()?.fileSizeTooLarge).not.toBeUndefined();
      });

      it('fileSize set and maxFileSize null', () => {
        component.fileSizeInMB = 50.1;
        component.maxFileSizeInMB = null;
        expect(component.validate()).toBeNull();
      });

      it('fileSize null and maxFileSize set', () => {
        component.fileSizeInMB = null;
        component.maxFileSizeInMB = 50;
        expect(component.validate()).toBeNull();
      });
    });
    it('should show all validationErrors', () => {
      component.fileSizeInMB = 50.1;
      component.maxFileSizeInMB = 50;
      component.dateiEndung = 'gpg';
      component.formControl.setValue('test.blubb');
      expect(component.validate()).not.toBeNull();
      expect(component.validate()?.fileNameMismatch).not.toBeUndefined();
      expect(component.validate()?.fileSizeTooLarge).not.toBeUndefined();
    });
  });

  describe('onFileSelected', () => {
    it('should update file name', () => {
      const fileName = 'test.gpg';
      component.onFileSelected({ target: { files: [new File([], fileName)] } });
      expect(component.formControl.value).toEqual(fileName);
    });
    it('should update file size', () => {
      const fileName = 'test.gpg';
      component.onFileSelected({ target: { files: [new File(['12345678'], fileName)] } });
      expect(component.fileSizeInMB).toBeCloseTo(0.0000076294, 10);
    });

    it('should propagate changes', () => {
      const spy = spyOn(component, 'onChange');
      const file = new File([], 'kjhfehf');
      component.onFileSelected({ target: { files: [file] } });
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toBe(file);
    });
  });
});
