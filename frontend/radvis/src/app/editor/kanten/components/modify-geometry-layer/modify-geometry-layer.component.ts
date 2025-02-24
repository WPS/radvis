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

import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, Output } from '@angular/core';
import { Feature } from 'ol';
import { Color } from 'ol/color';
import Geometry from 'ol/geom/Geometry';
import LineString from 'ol/geom/LineString';
import { Modify } from 'ol/interaction';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Observable, Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-modify-geometry-layer',
  templateUrl: './modify-geometry-layer.component.html',
  styleUrls: ['./modify-geometry-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ModifyGeometryLayerComponent implements OnChanges, OnDestroy {
  @Input()
  geometry!: Geometry;

  @Input()
  color!: Color;

  @Input()
  withDirectionalArrows!: boolean;

  @Output()
  modify: Observable<Geometry>;

  private featureSource: VectorSource = new VectorSource();
  private olLayer: VectorLayer = new VectorLayer({
    source: this.featureSource,
    declutter: true,
  });
  private readonly interaction!: Modify;
  private modifySubject = new Subject<Geometry>();

  constructor(private olMapService: OlMapService) {
    this.modify = this.modifySubject.pipe(debounceTime(150));

    this.olLayer.setZIndex(EditorLayerZindexConfig.MODIFY_GEOMETRY_LAYER);
    this.olMapService.addLayer(this.olLayer);

    this.interaction = new Modify({
      source: this.featureSource,
      style: MapStyles.getModifyPointStyle,
    });
    this.interaction.on('modifystart', () => this.setStyle(false));
    this.interaction.on('modifyend', () => this.setStyle(this.withDirectionalArrows));
    this.olMapService.addInteraction(this.interaction);
  }

  ngOnChanges(): void {
    invariant(this.geometry);

    if (this.featureSource.getFeatures().length > 0) {
      const previousFeature = this.featureSource.getFeatures()[0];
      this.featureSource.removeFeature(previousFeature);
      previousFeature.un('change', this.onFeatureModify);
    }

    const feature = new Feature(this.geometry);
    this.featureSource.addFeature(feature);
    feature.on('change', this.onFeatureModify);

    this.setStyle(this.withDirectionalArrows);
    this.interaction.changed();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.olMapService.removeInteraction(this.interaction);
  }

  private onFeatureModify = (): void => {
    this.modifySubject.next(this.featureSource.getFeatures()[0].getGeometry());
  };

  private setStyle(withArrows: boolean): void {
    const geometry = this.geometry as LineString;
    const coords = geometry.getCoordinates();
    const styles = MapStyles.getDefaultHighlightStyle(this.color, withArrows, coords);
    this.olLayer.setStyle(styles);
  }
}
