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
import { Color } from 'ol/color';
import TileLayer from 'ol/layer/Tile';
import { TileWMS } from 'ol/source';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FahrradrouteImportprotokoll } from 'src/app/viewer/importprotokolle/models/fahrradroute-importprotokoll';
import { FahrradrouteTyp } from 'src/app/viewer/viewer-shared/models/fahrradroute-typ';
import { importProtokollLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-fahrradroute-import-detail-view',
  templateUrl: './fahrradroute-import-detail-view.component.html',
  styleUrls: ['./fahrradroute-import-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FahrradrouteImportDetailViewComponent implements OnDestroy {
  importprotokoll$: Observable<FahrradrouteImportprotokoll>;
  FahrradrouteTyp = FahrradrouteTyp;

  tileLayer: TileLayer;

  public legende: Map<string, Color> = new Map([
    ['Alte Geometrie', MapStyles.FAHRRADROUTEN_GEOMETRIE_COLOR],
    ['Veränderte Abschnitte', [255, 0, 0, 1]],
  ]);

  constructor(
    private viewerRoutingService: ViewerRoutingService,
    private olMapService: OlMapService,
    activatedRoute: ActivatedRoute
  ) {
    this.importprotokoll$ = activatedRoute.data.pipe(map(data => data.protokollEintrag as FahrradrouteImportprotokoll));

    activatedRoute.paramMap.subscribe((paramMap: ParamMap) => {
      const id = paramMap.get('id');
      invariant(id);
      const source: TileWMS | undefined = this.tileLayer?.getSource() as TileWMS;
      source?.updateParams({ ...source.getParams(), CQL_FILTER: `job_id=${id}` });
    });

    this.tileLayer = new TileLayer({
      source: new TileWMS({
        url: `/api/geoserver/saml/radvis/wms`,
        params: {
          LAYERS: 'radvis:fahrradroute_import',
          TILED: true,
          CQL_FILTER: `job_id=${activatedRoute.snapshot.paramMap.get('id')}`,
        },
        serverType: 'geoserver',
        transition: 0,
      }),
      zIndex: importProtokollLayerZIndex,
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

  public onClose(): void {
    this.viewerRoutingService.toViewer();
  }
}
