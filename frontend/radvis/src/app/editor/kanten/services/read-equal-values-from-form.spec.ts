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

/* eslint-disable @typescript-eslint/dot-notation */

import { FormControl, FormGroup } from '@angular/forms';
import { readEqualValuesFromForm } from 'src/app/editor/kanten/services/read-equal-values-from-form';
import {
  UndeterminedInvalidValue,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';

describe('readEqualValuesFromForm', () => {
  it('should not consider UndeterminedValue', () => {
    const form = new FormGroup({
      testEqual: new FormControl('test'),
      testUndetermined: new FormControl(new UndeterminedValue()),
    });

    expect(readEqualValuesFromForm(form)).toEqual({
      testEqual: 'test',
    });
  });

  it('should not consider UndeterminedInvalidValue', () => {
    const form = new FormGroup({
      testEqual: new FormControl('test'),
      testUndetermined: new FormControl(new UndeterminedInvalidValue()),
    });

    expect(readEqualValuesFromForm(form)).toEqual({
      testEqual: 'test',
    });
  });

  it('should not consider disabled values', () => {
    const form = new FormGroup({
      testEqual: new FormControl({ value: 'test', disabled: true }),
      testUndetermined: new FormControl(new UndeterminedValue()),
    });

    expect(form.get('testEqual')?.disabled).toBeTrue();
    expect(readEqualValuesFromForm(form)).toEqual({});
  });
});
