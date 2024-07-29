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
import { FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';
import { isArray } from 'rxjs/internal-compatibility';
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
  @ViewChild('fileUpload')
  fileUploadInput?: ElementRef<HTMLInputElement>;

  @Input()
  dateiEndung: string | string[] = '';

  @Input()
  maxFileSizeInMB: null | number = null;

  @Input()
  touchOnWrite = true;

  @Input()
  validating = false;

  formControl: FormControl<string>;

  fileName: string | null = null;
  fileSizeInMB: number | null = null;
  disabled = false;

  get acceptedFormat(): string | string[] {
    if (isArray(this.dateiEndung)) {
      return this.dateiEndung.map(dE => '.' + dE);
    }

    return this.dateiEndung === '' ? '' : '.' + this.dateiEndung;
  }

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl('', { nonNullable: true });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.dateiEndung && !changes.dateiEndung.firstChange) {
      invariant('Erlaubte Datei-Endung darf nachträglich nicht geändert werden.');
    }
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.formControl.setValue(file.name);
      this.fileName = file.name;
      this.fileSizeInMB = this.convertByteToMB(file.size);
      this.onChange(file);
    } else {
      this.formControl.setValue('');
    }
  }

  public writeValue(value: File | null): void {
    if (value !== null) {
      this.formControl.reset(value.name, { emitEvent: false });
      this.fileName = value.name;
    } else {
      this.formControl.reset('', { emitEvent: false });
      this.fileName = null;
      if (this.fileUploadInput) {
        this.fileUploadInput.nativeElement.value = '';
      }
    }

    if (this.touchOnWrite) {
      this.formControl.markAsTouched();
    }
    this.changeDetector.markForCheck();
  }

  public setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
    if (disabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  public validate(): ValidationErrors | null {
    const errors: ValidationErrors = {};

    const value = this.formControl.value;
    if (value) {
      if (isArray(this.dateiEndung) && this.dateiEndung.length && !this.dateiEndung.some(dE => value.endsWith(dE))) {
        errors.fileNameMismatch = `Unerlaubter Dateityp: ${value}. Erlaubt sind ${this.dateiEndung.join(', ')}`;
      } else if (!isArray(this.dateiEndung) && !value.endsWith(this.dateiEndung)) {
        errors.fileNameMismatch = `Unerlaubter Dateityp: ${value}. Erlaubt ist ${this.dateiEndung}`;
      }
      if (value.length > 255) {
        errors.fileNameMismatch = 'Dateiname zu lang, erlaubt sind maximal 255 Zeichen';
      }
    }

    if (this.maxFileSizeInMB !== null && this.fileSizeInMB !== null) {
      if (this.fileSizeInMB > this.maxFileSizeInMB) {
        errors.fileSizeTooLarge = 'Dateigröße ist zu groß. Es sind maximal ' + this.maxFileSizeInMB + 'MB erlaubt.';
      }
    }

    if (Object.keys(errors).length > 0) {
      return errors;
    }

    return null;
  }

  private convertByteToMB(size: number): number {
    return size / 1024 ** 2;
  }
}
