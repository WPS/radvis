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

import { Component, ChangeDetectionStrategy, Input } from '@angular/core';

@Component({
  selector: 'rad-expandable-content',
  templateUrl: './expandable-content.component.html',
  styleUrls: ['./expandable-content.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ExpandableContentComponent {
  private static readonly MAX_ARRAY_ELEMENTS = 3;
  private static readonly MAX_STRING_LENGTH = 50;

  @Input()
  content?: any;
  expanded = false;

  get contentForDisplay(): any {
    if (!this.content) {
      return '';
    }
    if (this.expanded) {
      return this.content;
    }

    if (this.isListContent) {
      return this.content.slice(0, ExpandableContentComponent.MAX_ARRAY_ELEMENTS);
    } else {
      let stringForDisplay = this.content;
      if (this.exceedsMaxLength) {
        stringForDisplay = this.content.slice(0, ExpandableContentComponent.MAX_STRING_LENGTH - 4).trim();
        stringForDisplay += ' ...';
      }
      return stringForDisplay;
    }
  }

  get exceedsMaxLength(): boolean {
    if (this.isListContent) {
      return this.content?.length > ExpandableContentComponent.MAX_ARRAY_ELEMENTS;
    } else {
      return this.content?.length > ExpandableContentComponent.MAX_STRING_LENGTH;
    }
  }

  get isListContent(): boolean {
    return Array.isArray(this.content);
  }
}
