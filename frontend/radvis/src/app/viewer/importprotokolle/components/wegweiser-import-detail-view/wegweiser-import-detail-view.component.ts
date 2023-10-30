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

import { ChangeDetectionStrategy, Component, HostListener, OnDestroy } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { WegweiserImportprotokoll } from 'src/app/viewer/importprotokolle/models/wegweiser-importprotokoll';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import TileLayer from 'ol/layer/Tile';
import invariant from 'tiny-invariant';
import { TileWMS } from 'ol/source';
import { infrastrukturLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { Color } from 'ol/color';

@Component({
  selector: 'rad-wegweiser-import-detail-view',
  templateUrl: './wegweiser-import-detail-view.component.html',
  styleUrls: ['./wegweiser-import-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WegweiserImportDetailViewComponent implements OnDestroy {
  importprotokoll$: Observable<WegweiserImportprotokoll>;

  tileLayer: TileLayer;

  public legende: Map<string, Color> = new Map([
    ['Hinzugefügt', [0, 255, 0, 1]],
    ['Verändert', [0, 0, 255, 1]],
    ['Entfernt', [255, 0, 0, 1]],
  ]);

  constructor(
    route: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    private olMapService: OlMapService
  ) {
    this.importprotokoll$ = route.data.pipe(map(data => data.protokollEintrag));

    route.paramMap.subscribe((paramMap: ParamMap) => {
      const id = paramMap.get('id');
      invariant(id);
      const source: TileWMS | undefined = this.tileLayer?.getSource() as TileWMS;
      source?.updateParams({ ...source.getParams(), CQL_FILTER: `job_id=${id}` });
    });

    this.tileLayer = new TileLayer({
      source: new TileWMS({
        url: `/api/geoserver/saml/radvis/wms`,
        params: {
          LAYERS: 'radvis:geoserver_wegweisende_beschilderung_diff',
          TILED: true,
          CQL_FILTER: `job_id=${route.snapshot.paramMap.get('id')}`,
        },
        serverType: 'geoserver',
        transition: 0,
      }),
      zIndex: infrastrukturLayerZIndex,
    });

    this.olMapService.addLayer(this.tileLayer);
  }

  @HostListener('keydown.escape')
  public onEscape(): void {
    this.onClose();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.tileLayer);
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }
}
