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
import { AbstractEventTrackedEditor } from 'src/app/form-elements/components/abstract-event-tracked-editor';
import { MatomoTracker } from 'ngx-matomo-client';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'rad-attribute-editor',
  templateUrl: './attribute-editor.component.html',
  styleUrls: ['./attribute-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttributeEditorComponent extends AbstractEventTrackedEditor {
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

  @Output()
  dismiss = new EventEmitter<void>();

  @Output()
  resetForm = new EventEmitter<void>();

  @Output()
  save = new EventEmitter<void>();

  constructor(matomoTracker: MatomoTracker, activatedRoute: ActivatedRoute) {
    super(matomoTracker, activatedRoute);
  }

  onClose(): void {
    this.dismiss.next();
  }

  onReset(): void {
    this.resetForm.next();
  }

  onSave(): void {
    this.trackSpeichernEvent();
    this.save.next();
  }
}
