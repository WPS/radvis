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
import Feature, { FeatureLike } from 'ol/Feature';
import { Geometry, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { Style } from 'ol/style';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { AnpassungswunschListenView } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-listen-view';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { AnpassungswunschFilterService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-filter.service';
import { AbstractInfrastrukturLayerComponent } from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { anpassungswunschLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';

@Component({
  selector: 'rad-anpassungswuensche-layer',
  templateUrl: './anpassungswuensche-layer.component.html',
  styleUrls: ['./anpassungswuensche-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnpassungswuenscheLayerComponent
  extends AbstractInfrastrukturLayerComponent<AnpassungswunschListenView>
  implements OnDestroy
{
  public readonly HIGHLIGHTED_PROPERTY = 'highlighted';

  private olLayer: VectorLayer;

  constructor(
    private olMapService: OlMapService,
    anpassungswunschRoutingService: AnpassungenRoutingService,
    anpassungswunschFilterService: AnpassungswunschFilterService,
    featureHighlightService: FeatureHighlightService
  ) {
    super(anpassungswunschRoutingService, anpassungswunschFilterService, featureHighlightService, ANPASSUNGSWUNSCH);

    this.olLayer = this.createLayer(0);
    this.olLayer.setStyle(this.anpassungswunschPointStyle);
    this.olLayer.setZIndex(anpassungswunschLayerZIndex);
    this.olMapService.addLayer(this.olLayer);

    this.initServiceSubscriptions();
  }

  private static getAnpassungswunschIconStyle(highlighted: boolean): Style[] {
    return AbstractInfrastrukturLayerComponent.infrastrukturIconStyle(highlighted, ANPASSUNGSWUNSCH);
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  protected convertToFeature(infrastruktur: AnpassungswunschListenView): Feature<Geometry>[] {
    const feature = new Feature(new Point(infrastruktur.geometrie.coordinates));

    feature.setId(infrastruktur.id);
    feature.set('status', infrastruktur.status);

    return [feature];
  }

  protected extractIdFromFeature(hf: RadVisFeature): number {
    return Number(hf.id);
  }

  private anpassungswunschPointStyle = (feature: FeatureLike): Style | Style[] => {
    const highlighted: boolean = feature.get(this.HIGHLIGHTED_PROPERTY);

    if (highlighted && this.selectedId === feature.getId()) {
      // dann ist der Editor für die Darstellung zuständig
      return new Style();
    }

    return AnpassungswuenscheLayerComponent.getAnpassungswunschIconStyle(highlighted);
  };
}
