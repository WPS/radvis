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
import { Feature } from 'ol';
import * as OlColors from 'ol/color';
import { FeatureLike } from 'ol/Feature';
import { Geometry, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { Style } from 'ol/style';
import Fill from 'ol/style/Fill';
import Stroke from 'ol/style/Stroke';
import TextStyle from 'ol/style/Text';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { SignaturLegende } from 'src/app/shared/models/signatur-legende';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { ABSTELLANLAGEN } from 'src/app/viewer/abstellanlage/models/abstellanlage.infrastruktur';
import { AbstellanlagenQuellSystem } from 'src/app/viewer/abstellanlage/models/abstellanlagen-quell-system';
import { AbstellanlageFilterService } from 'src/app/viewer/abstellanlage/services/abstellanlage-filter.service';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { AbstractInfrastrukturLayerComponent } from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';

@Component({
  selector: 'rad-abstellanlage-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class AbstellanlageLayerComponent
  extends AbstractInfrastrukturLayerComponent<Abstellanlage>
  implements OnDestroy
{
  private readonly LEGENDE: SignaturLegende = {
    name: 'Abstellanlagen',
    entries: [
      { name: 'Quelle RadVIS', color: OlColors.toString(MapStyles.INFRASTRUKTUR_ICON_COLOR) },
      { name: 'Quelle MobiDATA', color: OlColors.toString(MapStyles.MOBIDATA_COLOR) },
    ],
  };
  private layer: VectorLayer;
  private readonly IS_MOBIDATA_PROPERTY_NAME = 'isMobidata';

  constructor(
    routingService: AbstellanlageRoutingService,
    filterService: AbstellanlageFilterService,
    featureHighlightService: FeatureHighlightService,
    private olMapService: OlMapService
  ) {
    super(routingService, filterService, featureHighlightService, ABSTELLANLAGEN);

    this.layer = this.createLayer();
    this.layer.setStyle(this.styleWithHighlightCircleFn);
    this.olMapService.addLayer(this.layer);
    this.olMapService.updateLegende(this.layer, this.LEGENDE);
    this.initServiceSubscriptions();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.layer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  protected convertToFeature(infrastruktur: Abstellanlage): Feature<Geometry>[] {
    const feature = new Feature(new Point(infrastruktur.geometrie.coordinates));
    feature.setId(infrastruktur.id);
    feature.set(
      AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME,
      `Abstellanlage (${infrastruktur.betreiber})`
    );
    if (infrastruktur.quellSystem === AbstellanlagenQuellSystem.MOBIDATABW) {
      feature.set(this.IS_MOBIDATA_PROPERTY_NAME, true);
      feature.set(this.ICON_COLOR_PROPERTY_NAME, MapStyles.MOBIDATA_COLOR);
    }
    return [feature];
  }

  protected override styleFn = (feature: FeatureLike, resolution: number): Style | Style[] => {
    const defaultStyles = AbstractInfrastrukturLayerComponent.infrastrukturIconStyle(
      feature.get(this.HIGHLIGHTED_PROPERTY_NAME),
      ABSTELLANLAGEN,
      feature.get(this.ICON_COLOR_PROPERTY_NAME)
    );
    if (feature.get(this.IS_MOBIDATA_PROPERTY_NAME) && resolution < MapStyles.RESOLUTION_VERY_SMALL) {
      defaultStyles.push(
        new Style({
          text: new TextStyle({
            fill: new Fill({
              color: 'white',
            }),
            text: ' MobiData',
            backgroundFill: new Fill({
              color: MapStyles.MOBIDATA_COLOR,
            }),
            backgroundStroke: new Stroke({
              color: 'white',
            }),
            textAlign: 'left',
            offsetY: 2,
          }),
        })
      );
    }
    return defaultStyles;
  };

  protected extractIdFromFeature(hf: RadVisFeature): number {
    return hf.id!;
  }
}
