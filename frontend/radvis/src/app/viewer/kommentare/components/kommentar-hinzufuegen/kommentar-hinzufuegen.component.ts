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

import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';

@Component({
  selector: 'rad-kommentar-hinzufuegen',
  templateUrl: './kommentar-hinzufuegen.component.html',
  styleUrls: ['./kommentar-hinzufuegen.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KommentarHinzufuegenComponent implements DiscardableComponent {
  public static readonly KOMMENTAR_MAX_LENGTH = 4000;

  @Output()
  sendKommentar = new EventEmitter<string>();

  formGroup: UntypedFormGroup;

  constructor() {
    this.formGroup = new UntypedFormGroup({
      kommentarText: new UntypedFormControl(
        null,
        RadvisValidators.maxLength(KommentarHinzufuegenComponent.KOMMENTAR_MAX_LENGTH)
      ),
    });
  }

  send(): void {
    if (!this.formGroup.valid) {
      return;
    }
    if (this.formGroup.value.kommentarText === null || this.formGroup.value.kommentarText.trim() === '') {
      return;
    }
    this.sendKommentar.emit(this.formGroup.value.kommentarText);
    this.formGroup.reset();
  }

  canDiscard(): boolean {
    return this.formGroup.value.kommentarText == null || this.formGroup.value.kommentarText === '';
  }
}
