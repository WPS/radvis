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
import { FormControl, FormGroup } from '@angular/forms';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Bauwerksmangel } from 'src/app/shared/models/bauwerksmangel';
import { BauwerksmangelArt } from 'src/app/shared/models/bauwerksmangel-art';
import { QuerungshilfeDetails } from 'src/app/shared/models/querungshilfe-details';

export class SharedKnotenFormGroup extends FormGroup<{
  knotenForm: FormControl<string | null>;
  querungshilfeDetails: FormControl<QuerungshilfeDetails | null>;
  bauwerksmangel: FormGroup<{
    vorhanden: FormControl<Bauwerksmangel | null>;
    bauwerksmangelArt: FormControl<BauwerksmangelArt[] | null>;
  }>;
}> {
  constructor(knotenFormRequired = false) {
    super({
      knotenForm: new FormControl<string | null>(null),
      querungshilfeDetails: new FormControl<QuerungshilfeDetails | null>(
        { value: null, disabled: true },
        { validators: RadvisValidators.isNotNullOrEmpty }
      ),
      bauwerksmangel: new FormGroup({
        vorhanden: new FormControl<Bauwerksmangel | null>(
          { value: null, disabled: true },
          { validators: RadvisValidators.isNotNullOrEmpty }
        ),
        bauwerksmangelArt: new FormControl<BauwerksmangelArt[] | null>(
          { value: null, disabled: true },
          { validators: [RadvisValidators.isNotNullOrEmpty, RadvisValidators.isNotEmpty] }
        ),
      }),
    });

    if (knotenFormRequired) {
      this.controls.knotenForm.addValidators(RadvisValidators.isNotNullOrEmpty);
    }

    this.controls.knotenForm.valueChanges.subscribe(value => {
      this.onKnotenformChanged(value);
    });

    this.controls.bauwerksmangel.controls.vorhanden.valueChanges.subscribe(value => {
      this.onBauwerksmangelChanged(value);
    });
  }

  private onBauwerksmangelChanged(value: Bauwerksmangel | null): void {
    this.controls.bauwerksmangel.controls.bauwerksmangelArt.reset();
    if (value === Bauwerksmangel.VORHANDEN) {
      this.controls.bauwerksmangel.controls.bauwerksmangelArt.enable();
    } else {
      this.controls.bauwerksmangel.controls.bauwerksmangelArt.disable();
    }
  }

  private onKnotenformChanged(value: string | null): void {
    this.controls.querungshilfeDetails.reset();
    this.controls.bauwerksmangel.reset();
    if (value) {
      if (QuerungshilfeDetails.isEnabledForKnotenform(value)) {
        this.controls.querungshilfeDetails.enable();
      } else {
        this.controls.querungshilfeDetails.disable();
      }

      if (Bauwerksmangel.isEnabledForKnotenform(value)) {
        this.controls.bauwerksmangel.controls.vorhanden.enable();
      } else {
        this.controls.bauwerksmangel.disable();
      }
    } else {
      this.controls.querungshilfeDetails.disable();
      this.controls.bauwerksmangel.disable();
    }
  }
}
