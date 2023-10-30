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

import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import {
  UNDETERMINED_LABEL,
  UndeterminedInvalidValue,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';

export class RadvisValidators {
  private static readonly URL_REGEX =
    '^(https?://)(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.?[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()!@:%_+.~#?&/=]*)$';
  private static readonly EMAIL_REGEX = '^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$';

  public static email(control: AbstractControl): ValidationErrors | null {
    if (Validators.pattern(RadvisValidators.EMAIL_REGEX)(control) === null) {
      return null;
    }
    return { email: 'Bitte gültige Email-Adresse angeben' };
  }

  public static url(control: AbstractControl): ValidationErrors | null {
    if (Validators.pattern(RadvisValidators.URL_REGEX)(control) === null) {
      return null;
    }
    return { url: 'Bitte gültige URL angeben' };
  }

  public static maxLength(length: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (Validators.maxLength(length)(control) === null) {
        return null;
      }
      return { maxLength: 'Maximal ' + length + ' Zeichen erlaubt' };
    };
  }

  public static between(min: number, max: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (Validators.min(min)(control) === null && Validators.max(max)(control) === null) {
        return null;
      }
      return { between: 'Muss zwischen ' + min + ' und ' + max + ' liegen' };
    };
  }

  public static min(min: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (Validators.min(min)(control) === null) {
        return null;
      }
      return { min: 'Muss mindestens ' + min + ' sein.' };
    };
  }

  public static max(max: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (Validators.max(max)(control) === null) {
        return null;
      }
      return { max: 'Darf maximal ' + max + ' sein.' };
    };
  }

  public static isSmallerThanIntegerMaxValue(ctrl: AbstractControl): ValidationErrors | null {
    return RadvisValidators.max(2000000000)(ctrl);
  }

  public static isPositiveInteger(ctrl: AbstractControl): ValidationErrors | null {
    if (!ctrl.value) {
      return null;
    }

    if (ctrl.value instanceof UndeterminedValue) {
      return null;
    }

    if (/^[0-9]+$/g.test(ctrl.value)) {
      return null;
    }

    return { isPositiveInteger: 'Nur positive Ganzzahl erlaubt' } as ValidationErrors;
  }

  public static isPositiveFloatNumber(ctrl: AbstractControl): ValidationErrors | null {
    if (ctrl.value === undefined || ctrl.value === null || ctrl.value instanceof UndeterminedValue) {
      return null;
    }

    if (ctrl.value instanceof UndeterminedInvalidValue) {
      return { isUndeterminedInvalid: 'Nur positive Kommazahlen erlaubt' } as ValidationErrors;
    }

    if (!isNaN(ctrl.value) && ctrl.value > 0) {
      return null;
    }
    return { isPositiveFloat: 'Nur positive Kommazahl erlaubt' } as ValidationErrors;
  }

  public static isPositiveFloatString(ctrl: AbstractControl): ValidationErrors | null {
    if (!ctrl.value) {
      return null;
    }

    if (ctrl.value === UNDETERMINED_LABEL) {
      return null;
    }

    if (/^([0-9]+,)?[0-9]+$/g.test(ctrl.value) && Number(ctrl.value.replace(',', '.')) > 0) {
      // Erlaubt: 123 und 123,4
      // Nicht erlaubt: 123, und ,3
      return null;
    }
    return { isFloat: 'Nur Kommazahlen erlaubt' } as ValidationErrors;
  }

  /**
   * Darf weder undefined, null noch Leerstring sein.
   */
  public static isNotNullOrEmpty(ctrl: AbstractControl): ValidationErrors | null {
    if (ctrl.value !== undefined && ctrl.value != null && ctrl.value !== '') {
      return null;
    }
    return { isNotSetOrBlank: 'Das Feld darf nicht leer sein' };
  }

  public static isNotEmpty(ctrl: AbstractControl): ValidationErrors | null {
    const value: string[] = ctrl.value;

    if (Array.isArray(value) && value.length) {
      return null;
    }
    return { isNotSetOrBlank: 'Die Liste darf nicht leer sein' };
  }

  public static isAlphanumeric(ctrl: AbstractControl): ValidationErrors | null {
    const value: string = ctrl.value;
    if (
      value === null ||
      [...value].every(character =>
        '0123456789abcdefghijklmnopqrstuvwxyzABCEDEFGHIJKLMNOPQRSTUVWXYZ'.includes(character)
      )
    ) {
      return null;
    }
    return { isAlphanumeric: 'Die Eingabe muss alphanumerisch sein' };
  }

  public static isValidMassnahmeKonzeptId(ctrl: AbstractControl): ValidationErrors | null {
    const value: string = ctrl.value;
    if (
      value === null ||
      [...value].every(character =>
        ('0123456789abcdefghijklmnopqrstuvwxyzäöüABCEDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ' + '_ .').includes(character)
      )
    ) {
      return null;
    }
    return {
      isValidMassnahmeKonzeptId:
        'Die Eingabe muss alphanumerisch bis auf folgende erlaubte Sonderzeichen: "_", " ", "."',
    };
  }

  public static isValidEuroString(ctrl: AbstractControl): ValidationErrors | null {
    if (!ctrl.value) {
      return null;
    }

    if (ctrl.value === UNDETERMINED_LABEL) {
      return null;
    }

    if (
      /^[0-9]+(,?)[0-9]{0,2}$/g.test(ctrl.value) &&
      Number(ctrl.value.replace(',', '.')) >= 0 &&
      Number(ctrl.value.replace(',', '.')) < 20000000
    ) {
      // Erlaubt: 123 und 123,45 und 123,4
      // Nicht erlaubt: 123, und ,3
      return null;
    }

    return {
      isValidEuroString: 'Nur Beträge in € erlaubt (2 Nachkommastellen für Eurocents, max 20000000€). ',
    } as ValidationErrors;
  }
}
