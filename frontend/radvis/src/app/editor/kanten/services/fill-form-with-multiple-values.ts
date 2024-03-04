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

import { UntypedFormGroup } from '@angular/forms';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import invariant from 'tiny-invariant';

export const fillFormWithMultipleValues = (form: UntypedFormGroup, values: any[], emitEvent = true): void => {
  invariant(values.length > 0);
  const formKeys = Object.keys(form.getRawValue());
  const modelKeys = Object.keys(values[0]);

  invariant(
    formKeys.every(fk => modelKeys.includes(fk)),
    'Form und Entity müssen gleiche ObjectKeys haben: ' + formKeys.find(fk => !modelKeys.includes(fk))
  );

  formKeys.forEach(key => {
    const keyValues = values.map(value => value[key]);
    let formValue;
    if (Array.isArray(keyValues[0])) {
      formValue = keyValues.every(
        v => v.length === keyValues[0].length && v.every((item: any) => keyValues[0].includes(item))
      )
        ? keyValues[0]
        : new UndeterminedValue();
    } else {
      formValue = keyValues.every(v => v === keyValues[0]) ? keyValues[0] : new UndeterminedValue();
    }
    form.get(key)?.reset(formValue, { emitEvent });
  });
};
