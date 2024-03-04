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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'rad-attribute-editor',
  templateUrl: './attribute-editor.component.html',
  styleUrls: ['./attribute-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttributeEditorComponent {
  @Input()
  titel = '';

  @Input()
  dirty = false;

  @Input()
  fetching = false;

  @Input()
  disabled = false;

  @Input()
  showHeader = true;

  @Input()
  canEdit = true;

  @Input()
  canDelete = false;

  @Output()
  dismiss = new EventEmitter<void>();

  @Output()
  resetForm = new EventEmitter<void>();

  @Output()
  save = new EventEmitter<void>();

  @Output()
  delete = new EventEmitter<void>();

  onClose(): void {
    this.dismiss.next();
  }

  onReset(): void {
    this.resetForm.next();
  }

  onSave(): void {
    this.save.next();
  }

  onDelete(): void {
    this.delete.next();
  }
}
