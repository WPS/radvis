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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-file-upload-control',
  templateUrl: './file-upload-control.component.html',
  styleUrls: ['./file-upload-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: FileUploadControlComponent, multi: true },
    { provide: NG_VALIDATORS, useExisting: FileUploadControlComponent, multi: true },
  ],
})
export class FileUploadControlComponent extends AbstractFormControl<File> implements OnChanges, Validator {
  @Input()
  dateiEndung = '';

  @Input()
  maxFileSizeInMB: null | number = null;

  @ViewChild('fileUpload', { read: ElementRef }) fileUploader: ElementRef | null = null;

  fileName: string | null = null;
  fileSizeInMB: number | null = null;
  disabled = false;

  get acceptedFormat(): string {
    return this.dateiEndung === '' ? '' : '.' + this.dateiEndung;
  }

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.dateiEndung && !changes.dateiEndung.firstChange) {
      invariant('Erlaubte Datei-Endung darf nachträglich nicht geändert werden.');
    }
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.fileName = file.name;
      this.fileSizeInMB = this.convertByteToMB(file.size);
      this.onChange(file);
    } else {
      this.fileName = '';
    }
  }

  public writeValue(value: File | null): void {
    if (value) {
      this.fileName = value.name;
    } else if (this.fileUploader) {
      this.fileName = '';
      this.fileUploader.nativeElement.value = '';
    }
    this.changeDetector.markForCheck();
  }

  public setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.changeDetector.markForCheck();
  }

  public validate(): ValidationErrors | null {
    let fileNameMismatch;
    let fileSizeTooLarge;

    if (this.fileName !== null) {
      if (!this.fileName.endsWith(this.dateiEndung)) {
        fileNameMismatch = 'Falscher Dateityp: Erlaubt ist ' + this.dateiEndung;
      }
      if (this.fileName.length > 255) {
        fileNameMismatch = 'Dateiname zu lang, erlaubt sind maximal 255 Zeichen';
      }
    }

    if (this.maxFileSizeInMB !== null && this.fileSizeInMB !== null) {
      if (this.fileSizeInMB > this.maxFileSizeInMB) {
        fileSizeTooLarge = 'Dateigröße ist zu groß. Es sind maximal ' + this.maxFileSizeInMB + 'MB erlaubt.';
      }
    }

    if (!fileNameMismatch && !fileSizeTooLarge) {
      return null;
    }

    return { fileNameMismatch, fileSizeTooLarge };
  }

  private convertByteToMB(size: number): number {
    return size / 1024 ** 2;
  }
}
