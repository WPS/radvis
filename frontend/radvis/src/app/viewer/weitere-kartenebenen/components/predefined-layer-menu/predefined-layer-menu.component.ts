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
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { MatMenuPanel } from '@angular/material/menu';
import { PredefinedKartenMenu } from 'src/app/viewer/weitere-kartenebenen/models/predefined-karten-menu';
import { SaveWeitereKartenebeneCommand } from 'src/app/viewer/weitere-kartenebenen/models/save-weitere-kartenebene-command';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-predefined-layer-menu',
  templateUrl: './predefined-layer-menu.component.html',
  styleUrl: './predefined-layer-menu.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class PredefinedLayerMenuComponent implements OnInit {
  @Input()
  menu!: PredefinedKartenMenu[];
  @Output()
  add = new EventEmitter<SaveWeitereKartenebeneCommand>();

  @ViewChild('kartenMenu', { static: true }) public kartenMenu: MatMenuPanel | null = null;

  ngOnInit(): void {
    invariant(this.menu);
  }

  onAddPredefinedWeitereKartenebenen(layer: SaveWeitereKartenebeneCommand): void {
    this.add.next(layer);
  }

  public getLayerItems(): SaveWeitereKartenebeneCommand[] {
    return this.menu.filter(m => !m.hasSubMenu()).map(m => m.item as SaveWeitereKartenebeneCommand);
  }

  public getSubMenuItems(): PredefinedKartenMenu[] {
    return this.menu.filter(m => m.hasSubMenu());
  }
}
