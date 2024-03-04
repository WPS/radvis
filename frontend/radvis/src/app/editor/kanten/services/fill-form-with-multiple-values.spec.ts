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

import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { fillFormWithMultipleValues } from 'src/app/editor/kanten/services/fill-form-with-multiple-values';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';

describe('fillFormWithMultipleValues', () => {
  it('should throw error if formKey is missing in object', () => {
    const form = new UntypedFormGroup({ text: new UntypedFormControl(null) });
    const object = { etwasAnderes: 'test' };

    expect(() => fillFormWithMultipleValues(form, [object])).toThrow();
  });

  it('should consider all form keys, including disabled', () => {
    const form = new UntypedFormGroup({
      text: new UntypedFormControl(null),
      notEnabled: new UntypedFormControl({ value: null, disabled: true }),
    });
    expect(form.get('notEnabled')?.disabled).toBeTrue();

    const object = { text: 'test', notEnabled: false };
    fillFormWithMultipleValues(form, [object]);
    expect(form.getRawValue()).toEqual(object);
  });

  it('should handle array values correctly', () => {
    const form = new UntypedFormGroup({
      arr: new UntypedFormControl([]),
    });

    const object = { arr: ['item1'] };
    fillFormWithMultipleValues(form, [object]);
    expect(form.getRawValue()).toEqual(object);
  });

  it('should fill Undetermined value correct', () => {
    const form = new UntypedFormGroup({
      text: new UntypedFormControl(null),
      arr: new UntypedFormControl([]),
      arrUndetermined: new UntypedFormControl([]),
      undetermined: new UntypedFormControl(null),
    });
    const object1 = { text: 'test', arr: ['item1', 'item2'], arrUndetermined: ['some'], undetermined: '1' };
    const object2 = { text: 'test', arr: ['item1', 'item2'], arrUndetermined: ['some', 'other'], undetermined: '2' };

    fillFormWithMultipleValues(form, [object1, object2]);
    expect(form.getRawValue().text).toEqual('test');
    expect(form.getRawValue().arr).toEqual(['item1', 'item2']);
    expect(form.getRawValue().arrUndetermined).toBeInstanceOf(UndeterminedValue);
    expect(form.getRawValue().undetermined).toBeInstanceOf(UndeterminedValue);
  });
});
