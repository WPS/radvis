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

import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy } from '@angular/core';
import { Coordinate, equals } from 'ol/coordinate';
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Icon } from 'ol/style';
import Style from 'ol/style/Style';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-location-selection-layer',
  templateUrl: './location-selection-layer.component.html',
  styleUrls: ['./location-selection-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationSelectionLayerComponent implements OnChanges, OnDestroy {
  @Input()
  location: Coordinate | null = null;

  private featureSource: VectorSource = new VectorSource();
  private olLayer!: VectorLayer;

  constructor(private olMapService: OlMapService) {}

  ngOnChanges(): void {
    if (!this.olLayer) {
      this.olLayer = new VectorLayer({
        source: this.featureSource,
        style: new Style({
          image: new Icon({
            src: 'assets/location.svg',
            anchor: [0.5, 1],
          }),
        }),
      });
      this.olMapService.addLayer(this.olLayer);
      this.olLayer.setZIndex(666);
    }

    if (this.featureSource.getFeatures().length > 0) {
      const feature = this.featureSource.getFeatures()[0];
      this.featureSource.removeFeature(feature);
      if (this.location && equals((feature.getGeometry() as Point).getCoordinates(), this.location)) {
        return;
      }
    }

    if (this.location != null) {
      this.featureSource.addFeature(new Feature(new Point(this.location)));
    }
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
  }
}
