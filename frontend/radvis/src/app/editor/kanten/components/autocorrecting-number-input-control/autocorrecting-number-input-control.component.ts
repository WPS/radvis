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
  forwardRef,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import {
  UndeterminedValue,
  UNDETERMINED_LABEL,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

// Diese Eingabekomponente korrigiert invalide, negative und 0,0 automatisch.
// Auf Wunsch (mit dem Input: resetToPreviousValueOnEmpty) werden auch nicht vorhandene Werte korrigiert

// Invalide Eingaben und "0,0" -> Feld wird auf vorherigen Wert zurueckgesetzt
// Negative Zahlen -> Minus wird abgeschnitten
// Nicht vorhandene Werte -> resetToPreviousValueOnEmpty ? zuruecksetzten : null
@Component({
  selector: 'rad-autocorrecting-number-input-control',
  templateUrl: './autocorrecting-number-input-control.component.html',
  styleUrls: ['./autocorrecting-number-input-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutocorrectingNumberInputControlComponent),
      multi: true,
    },
  ],
})
export class AutocorrectingNumberInputControlComponent
  extends AbstractFormControl<number | UndeterminedValue>
  implements OnChanges {
  @Input()
  value: number | null = null;
  // Bei direkter Zuweisung des Controls kommt kein neues "enabled" rein, daher binden wir das hier zusätzlich
  @Input()
  isDisabled = false;

  @Input()
  resetToPreviousValueOnEmpty = false;

  formControl: FormControl;

  previousValue: number | null | UndeterminedValue = null;

  readonly UNDETERMINED_LABEL = UNDETERMINED_LABEL;

  get displayValue(): string {
    // das !=undefined liegt daran, dass ng-mocks einen bug hat.
    // einfach nur if(this.value) geht nicht für den fall, dass es 0 ist
    if (this.formControl.value instanceof UndeterminedValue) {
      return UNDETERMINED_LABEL;
    }
    if (this.value !== null && this.value !== undefined) {
      return this.value.toFixed(2).replace('.', ',');
    }
    return '';
  }

  constructor(private changeDetector: ChangeDetectorRef, notifyUserService: NotifyUserService) {
    super();
    this.formControl = new FormControl('', { updateOn: 'blur' });
    this.formControl.valueChanges.subscribe(value => {
      let convertedValue: number | null = +Number.parseFloat(this.convertToNumberString(value)).toFixed(2);
      if (value === '' && !this.resetToPreviousValueOnEmpty) {
        convertedValue = null;
      } else if (isNaN(convertedValue) || convertedValue === 0.0) {
        convertedValue = this.previousValue as number | null;
        notifyUserService.warn('Fehlerhafter Wert. Eingabe wurde nicht übernommen.');
      }
      this.formControl.reset(convertedValue?.toFixed(2).replace('.', ','), { emitEvent: false });
      // Damit das Form nicht dirty wird bei ungültigen Eingaben, werden Änderungen nur bei tatsächlichen Änderungen nach oben gegeben
      if (this.previousValue !== convertedValue) {
        this.onChange(convertedValue);
      }
      this.previousValue = convertedValue;
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.isDisabled) {
      this.setDisabledState(this.isDisabled);
    }
  }

  public setDisabledState(disabled: boolean): void {
    if (disabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  public writeValue(value: number | UndeterminedValue | null): void {
    if (value instanceof UndeterminedValue) {
      this.formControl.reset(UNDETERMINED_LABEL, { emitEvent: false });
    } else if (value !== null) {
      this.formControl.reset(value.toFixed(2).replace('.', ','), { emitEvent: false });
    } else {
      this.formControl.reset('', { emitEvent: false });
    }
    this.changeDetector.markForCheck();
    this.previousValue = value;
  }

  private convertToNumberString(value: string): string {
    return value.replace(',', '.').replace(/[^\d\.]/g, '');
  }
}
