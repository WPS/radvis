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
import { animate, state, style, transition, trigger } from '@angular/animations';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'rad-collapse-down-button',
  templateUrl: './collapse-down-button.component.html',
  styleUrl: './collapse-down-button.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('collapseExpand', [
      state(
        'collapsed',
        style({
          transform: 'rotate(-180deg)',
        })
      ),
      state(
        'expanded',
        style({
          transform: 'rotate(0deg)',
        })
      ),
      transition('collapsed <=> expanded', [animate('0.3s')]),
    ]),
  ],
  standalone: false,
})
export class CollapseDownButtonComponent {
  @Input()
  expanded = false;

  @Output()
  toggleExpansion = new EventEmitter<void>();
}
