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
import { ActivatedRoute } from '@angular/router';
import { Feature } from 'ol';
import { Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { NetzDetailFeatureTableLink } from 'src/app/viewer/viewer-shared/models/netzdetail-feature-table-link';
import { infrastrukturHighlightLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { WegweisendeBeschilderungListenView } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung-listen-view';
import { WEGWEISENDE_BESCHILDERUNG } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung.infrastruktur';

@Component({
  selector: 'rad-wegweisende-beschilderung-detail-view',
  templateUrl: './wegweisende-beschilderung-detail-view.component.html',
  styleUrls: ['./wegweisende-beschilderung-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WegweisendeBeschilderungDetailViewComponent implements OnDestroy {
  attributes$: Observable<Map<string, { [key: string]: string | NetzDetailFeatureTableLink }>>;
  private layer: VectorLayer;
  private source: VectorSource;

  constructor(
    activatedRoute: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    private olMapService: OlMapService
  ) {
    this.source = new VectorSource();
    this.layer = new VectorLayer({
      source: this.source,
      style: [
        MapStyles.defaultPointStyleLarge(MapStyles.FEATURE_SELECT_COLOR),
        ...MapStyles.getInfrastrukturIconStyle(WEGWEISENDE_BESCHILDERUNG.iconFileName, true),
      ],
      zIndex: infrastrukturHighlightLayerZIndex,
    });
    this.olMapService.addLayer(this.layer);

    this.attributes$ = activatedRoute.data.pipe(
      map(data => {
        const result = new Map<string, { [key: string]: string | NetzDetailFeatureTableLink }>();
        result.set('', WegweisendeBeschilderungListenView.getDetails(data.wegweisendeBeschilderung));
        return result;
      })
    );

    activatedRoute.data.subscribe(data => {
      this.source.clear(true);
      const coordinate = data.wegweisendeBeschilderung.geometrie.coordinates;
      this.source.addFeature(new Feature(new Point(coordinate)));
      this.source.changed();
      this.olMapService.scrollIntoViewByCoordinate(coordinate);
    });
  }

  @HostListener('keydown.escape')
  public onEscape(): void {
    this.onClose();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.layer);
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }
}
