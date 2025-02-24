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
import { Geometry, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { AbstractInfrastrukturLayerComponent } from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { FahrradzaehlstelleListenView } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle-listen-view';
import { FahrradzaehlstelleRoutingService } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle-routing.service';
import { FahrradzaehlstelleFilterService } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle-filter.service';
import { FAHRRADZAEHLSTELLE } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle.infrastruktur';

@Component({
  selector: 'rad-fahrradzaehlstelle-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class FahrradzaehlstelleLayerComponent
  extends AbstractInfrastrukturLayerComponent<FahrradzaehlstelleListenView>
  implements OnDestroy
{
  private layer: VectorLayer;

  constructor(
    routingService: FahrradzaehlstelleRoutingService,
    filterService: FahrradzaehlstelleFilterService,
    featureHighlightService: FeatureHighlightService,
    private olMapService: OlMapService
  ) {
    super(routingService, filterService, featureHighlightService, FAHRRADZAEHLSTELLE);

    this.layer = this.createLayer();
    this.layer.setStyle(this.styleWithHighlightCircleFn);
    this.olMapService.addLayer(this.layer);
    this.initServiceSubscriptions();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.layer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  protected convertToFeature(infrastruktur: FahrradzaehlstelleListenView): Feature<Geometry>[] {
    const feature = new Feature(new Point(infrastruktur.geometrie.coordinates));
    feature.setId(infrastruktur.id);
    feature.set(
      AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME,
      'Fahrradzählstelle' +
        (infrastruktur.fahrradzaehlstelleBezeichnung ? ` (${infrastruktur.fahrradzaehlstelleBezeichnung})` : '')
    );
    return [feature];
  }

  protected extractIdFromFeature(hf: RadVisFeature): number {
    return hf.id!;
  }
}
