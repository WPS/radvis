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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { Observable } from 'rxjs';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { WeitereKartenebeneTyp } from 'src/app/viewer/weitere-kartenebenen/models/weitereKartenebeneTyp';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';

@Component({
  selector: 'rad-weitere-kartenebenen-display-layer',
  templateUrl: './weitere-kartenebenen-display-layer.component.html',
  styleUrls: ['./weitere-kartenebenen-display-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeitereKartenebenenDisplayLayerComponent {
  public selectedLayers$: Observable<WeitereKartenebene[]>;

  constructor(private weitereKartenebenenService: WeitereKartenebenenService, private changeRef: ChangeDetectorRef) {
    this.selectedLayers$ = this.weitereKartenebenenService.selectedWeitereKartenebenen$;

    this.selectedLayers$.subscribe(() => changeRef.markForCheck());
  }

  isWMS(layer: WeitereKartenebene): boolean {
    return layer.weitereKartenebeneTyp === WeitereKartenebeneTyp.WMS;
  }

  isWFS(layer: WeitereKartenebene): boolean {
    return layer.weitereKartenebeneTyp === WeitereKartenebeneTyp.WFS;
  }
}
