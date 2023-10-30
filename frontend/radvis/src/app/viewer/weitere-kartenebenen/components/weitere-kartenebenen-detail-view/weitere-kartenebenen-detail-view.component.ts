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

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Feature } from 'ol';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { WeitereWfsKartenebenenComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-wfs-kartenebenen/weitere-wfs-kartenebenen.component';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { weitereKartenebenenHighlightZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';

@Component({
  selector: 'rad-weitere-kartenebenen-detail-view',
  templateUrl: './weitere-kartenebenen-detail-view.component.html',
  styleUrls: ['./weitere-kartenebenen-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeitereKartenebenenDetailViewComponent implements OnDestroy {
  attributes$: Observable<Map<string, { [key: string]: string }>>;
  layerName$: Observable<string>;
  private layer: VectorLayer;
  private source: VectorSource;

  constructor(
    activatedRoute: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    weitereKartenebenenService: WeitereKartenebenenService,
    private olMapService: OlMapService
  ) {
    this.source = new VectorSource();
    this.layer = new VectorLayer({
      source: this.source,
      style: MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_SELECT_COLOR),
      zIndex: weitereKartenebenenHighlightZIndex,
    });
    this.olMapService.addLayer(this.layer);

    this.attributes$ = activatedRoute.data.pipe(
      map(data => {
        const attributListe: { [key: string]: string } = {};
        (data.feature as RadVisFeature).attribute.forEach(attr => {
          if (
            attr.key !== WeitereKartenebene.LAYER_ID_KEY &&
            attr.key !== 'geometry' &&
            attr.key !== WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME &&
            attr.key !== WeitereKartenebene.EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME
          ) {
            if (attr.value === undefined || attr.value?.toString() !== '[object Object]') {
              attributListe[attr.key] = attr.value;
            }
          }
        });
        const result = new Map<string, { [key: string]: string }>();
        result.set('', attributListe);
        return result;
      })
    );
    this.layerName$ = activatedRoute.data.pipe(
      map(data => {
        const layerId = (data.feature as RadVisFeature).attribute.find(
          attr => attr.key === WeitereKartenebene.LAYER_ID_KEY
        )?.value;
        let layerName;
        if (layerId) {
          layerName = weitereKartenebenenService.weitereKartenebenen.find(e => e.id === layerId)?.name;
        }
        return layerName ?? 'Weitere Kartenebenen';
      })
    );
    activatedRoute.data.subscribe(data => {
      this.source.clear();
      this.source.addFeature(new Feature((data.feature as RadVisFeature).geometry));
      this.source.changed();
    });
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.layer);
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }
}
