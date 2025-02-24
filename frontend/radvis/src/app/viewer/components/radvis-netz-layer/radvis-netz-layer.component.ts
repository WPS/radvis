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

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Observable } from 'rxjs';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { RADVIS_NETZ_LAYER_PREFIX } from 'src/app/viewer/viewer-shared/models/radvis-netz-layer-prefix';

@Component({
  selector: 'rad-radvis-netz-layer',
  templateUrl: './radvis-netz-layer.component.html',
  styleUrls: ['./radvis-netz-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class RadvisNetzLayerComponent {
  @Input()
  mitVerlauf: boolean | null = false;

  public selectedNetzklassen$: Observable<Netzklassefilter[]>;
  public layerPrefix = RADVIS_NETZ_LAYER_PREFIX;

  constructor(netzklassenAuswahlService: NetzklassenAuswahlService) {
    this.selectedNetzklassen$ = netzklassenAuswahlService.currentAuswahl$;
  }
}
