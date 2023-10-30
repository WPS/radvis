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

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ValidationErrors } from '@angular/forms';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-validation-error-anzeige',
  templateUrl: './validation-error-anzeige.component.html',
  styleUrls: ['./validation-error-anzeige.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ValidationErrorAnzeigeComponent {
  @Input()
  errors: ValidationErrors | null = null;

  get messages(): string[] {
    if (this.errors === null) {
      return [];
    }

    return Object.keys(this.errors).map(key => {
      invariant(this.errors);
      return this.errors[key] as string;
    });
  }
}
