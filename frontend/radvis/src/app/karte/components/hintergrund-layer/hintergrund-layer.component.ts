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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import TileLayer from 'ol/layer/Tile';
import { Subscription } from 'rxjs';
import { HintergrundLayerService } from 'src/app/karte/services/hintergrund-layer.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-hintergrund-layer',
  templateUrl: './hintergrund-layer.component.html',
  styleUrls: ['./hintergrund-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class HintergrundLayerComponent implements OnDestroy {
  olLayer: TileLayer | null = null;
  private subscription: Subscription;

  constructor(
    private olMapService: OlMapService,
    changeDetector: ChangeDetectorRef,
    hintergrundLayerService: HintergrundLayerService
  ) {
    this.subscription = hintergrundLayerService.currentAuswahl$.subscribe(auswahl => {
      if (this.olLayer) {
        this.olMapService.removeLayer(this.olLayer);
      }
      this.olLayer = hintergrundLayerService.getLayer(auswahl);
      this.olMapService.addLayer(this.olLayer, hintergrundLayerService.getQuelle(auswahl));
      changeDetector.markForCheck();
    });
  }

  ngOnDestroy(): void {
    if (this.olLayer) {
      this.olMapService.removeLayer(this.olLayer);
    }
    this.subscription.unsubscribe();
  }
}
