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

import { ControlValueAccessor } from '@angular/forms';

export abstract class AbstractFormControl<T> implements ControlValueAccessor {
  public registerOnChange(fn: (v: T | null) => void): void {
    this._onChange = fn;
  }

  public registerOnTouched(fn: any): void {
    this._onTouched = fn;
  }

  public onTouched(): void {
    this._onTouched();
  }

  public onChange(value: T | null): void {
    this._onChange(value);
  }

  private _onChange: (v: T | null) => void = () => {};
  private _onTouched: () => void = () => {};

  public abstract writeValue(value: T | null): void;

  public abstract setDisabledState(isDisabled: boolean): void;
}
