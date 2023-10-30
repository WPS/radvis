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
import { FormGroup } from '@angular/forms';
import {
  UndeterminedInvalidValue,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';

export const readEqualValuesFromForm = (form: FormGroup): { [id: string]: any } => {
  const formKeys = Object.keys(form.value);
  const result: { [id: string]: any } = {};
  formKeys.forEach(key => {
    if (!(form.value[key] instanceof UndeterminedValue) && !(form.value[key] instanceof UndeterminedInvalidValue)) {
      result[key] = form.value[key];
    }
  });
  return result;
};
