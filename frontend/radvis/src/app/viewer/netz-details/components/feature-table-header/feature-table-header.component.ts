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
  selector: 'rad-feature-table-header',
  templateUrl: './feature-table-header.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class FeatureTableHeaderComponent {
  @Input()
  public leereAttributeVisible = false;

  @Output()
  public toggleLeereAttribute = new EventEmitter<void>();

  @Output()
  public closeClick = new EventEmitter<void>();

  public onToggleLeereAttribute(): void {
    this.toggleLeereAttribute.emit();
  }

  public onClose(): void {
    this.closeClick.emit();
  }

  public getVisibilityTooltip(): string {
    return 'Leere Attribute ' + (this.leereAttributeVisible ? 'ausblenden' : 'einblenden');
  }
}
