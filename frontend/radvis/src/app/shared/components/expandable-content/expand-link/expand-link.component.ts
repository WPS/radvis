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

import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'rad-expand-link',
  templateUrl: './expand-link.component.html',
  styleUrls: ['./expand-link.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExpandLinkComponent {
  @Input()
  expanded = false;
  @Output()
  expandedChange: EventEmitter<boolean> = new EventEmitter<boolean>();

  expandClicked($event: MouseEvent): void {
    $event.preventDefault();
    $event.stopPropagation();
    this.expanded = !this.expanded;
    this.expandedChange.emit(this.expanded);
  }
}
