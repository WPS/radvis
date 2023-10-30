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
import { Coordinate } from 'ol/coordinate';
import { Geometry, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { isPoint } from 'src/app/shared/models/geojson-geometrie';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FurtKreuzungListenView } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-listen-view';
import { FURTEN_KREUZUNGEN } from 'src/app/viewer/furten-kreuzungen/models/furten-kreuzungen.infrastruktur';
import { FurtenKreuzungenFilterService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-filter.service';
import { FurtenKreuzungenRoutingService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-routing.service';
import { AbstractInfrastrukturLayerComponent } from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';

@Component({
  selector: 'rad-furten-kreuzungen-layer',
  templateUrl: './furten-kreuzungen-layer.component.html',
  styleUrls: ['./furten-kreuzungen-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FurtenKreuzungenLayerComponent
  extends AbstractInfrastrukturLayerComponent<FurtKreuzungListenView>
  implements OnDestroy {
  private olLayer: VectorLayer;

  constructor(
    private olMapService: OlMapService,
    featureHighlightService: FeatureHighlightService,
    furtenKreuzungenRoutingService: FurtenKreuzungenRoutingService,
    furtenKreuzungenFilterService: FurtenKreuzungenFilterService
  ) {
    super(furtenKreuzungenRoutingService, furtenKreuzungenFilterService, featureHighlightService, FURTEN_KREUZUNGEN);

    this.olLayer = this.createLayer();
    this.olMapService.addLayer(this.olLayer);

    this.initServiceSubscriptions();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  protected convertToFeature(infrastruktur: FurtKreuzungListenView): Feature<Geometry>[] {
    if (!infrastruktur.iconPosition || !isPoint(infrastruktur.iconPosition)) {
      throw new Error('Icongeometrie ist nicht vorhanden oder kein Punkt');
    }

    const feature = new Feature(new Point((infrastruktur.iconPosition.coordinates as unknown) as Coordinate));
    feature.setId(infrastruktur.id);
    feature.set(AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME, 'Furt/Kreuzung');
    return [feature];
  }

  protected extractIdFromFeature(hf: RadVisFeature): number {
    return Number(hf.id);
  }
}
