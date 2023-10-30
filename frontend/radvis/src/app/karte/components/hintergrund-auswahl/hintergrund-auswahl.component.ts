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

import { ChangeDetectionStrategy, Component, EventEmitter, Output, QueryList, ViewChildren } from '@angular/core';
import { MatRadioButton, MatRadioChange } from '@angular/material/radio';
import { Observable } from 'rxjs';
import { HintergrundLayerService } from 'src/app/karte/services/hintergrund-layer.service';
import { LayerAuswahl } from 'src/app/shared/models/layer-auswahl';
import { LayerId } from 'src/app/shared/models/layers/rad-vis-layer';

@Component({
  selector: 'rad-hintergrund-auswahl',
  templateUrl: './hintergrund-auswahl.component.html',
  styleUrls: ['./hintergrund-auswahl.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HintergrundAuswahlComponent {
  @ViewChildren(MatRadioButton) radioButtons: QueryList<MatRadioButton> | null = null;

  @Output()
  closed = new EventEmitter<void>();

  public backgrounds: LayerAuswahl[];
  public currentBackground$: Observable<LayerId>;

  constructor(private hintergrundLayerService: HintergrundLayerService) {
    this.backgrounds = this.hintergrundLayerService.allLayerAuswahl;
    this.currentBackground$ = this.hintergrundLayerService.currentAuswahl$;
  }

  onBackgroundSelect(event: MatRadioChange): void {
    this.hintergrundLayerService.setAuswahl(event.value);
  }

  setFocusOnSelectedButton(): void {
    this.radioButtons?.find(radioButton => radioButton.checked)?.focus();
  }

  close(): void {
    this.closed.emit();
  }
}
