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
import { take } from 'rxjs/operators';
import { isPoint } from 'src/app/shared/models/geojson-geometrie';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { AbstractInfrastrukturLayerComponent } from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { WegweisendeBeschilderungListenView } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung-listen-view';
import { WEGWEISENDE_BESCHILDERUNG } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung.infrastruktur';
import { WegweisendeBeschilderungFilterService } from 'src/app/viewer/wegweisende-beschilderung/services/wegweisende-beschilderung-filter.service';
import { WegweisendeBeschilderungRoutingService } from 'src/app/viewer/wegweisende-beschilderung/services/wegweisende-beschilderung-routing.service';

@Component({
  selector: 'rad-wegweisende-beschilderung-layer',
  templateUrl: './wegweisende-beschilderung-layer.component.html',
  styleUrls: ['./wegweisende-beschilderung-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class WegweisendeBeschilderungLayerComponent
  extends AbstractInfrastrukturLayerComponent<WegweisendeBeschilderungListenView>
  implements OnDestroy
{
  private olLayer: VectorLayer;

  constructor(
    private notifyUserService: NotifyUserService,
    private olMapService: OlMapService,
    featureHighlightService: FeatureHighlightService,
    wegweisendeBeschilderungRoutingService: WegweisendeBeschilderungRoutingService,
    wegweisendeBeschilderungFilterService: WegweisendeBeschilderungFilterService
  ) {
    super(
      wegweisendeBeschilderungRoutingService,
      wegweisendeBeschilderungFilterService,
      featureHighlightService,
      WEGWEISENDE_BESCHILDERUNG
    );

    this.olLayer = this.createLayer(12);
    this.olMapService.addLayer(this.olLayer);

    const currentMapResolution = this.olMapService.getCurrentResolution();
    if (!currentMapResolution) {
      // Karte ist noch nicht initialisiert. Zeige die Meldung, wenn die Karte fertig ist und eine Resolution besitzt.
      this.olMapService
        .getResolution$()
        .pipe(take(1))
        .subscribe(resolution => {
          this.showZoomInNotification(resolution);
        });
    } else {
      this.showZoomInNotification(currentMapResolution);
    }

    this.initServiceSubscriptions();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  protected convertToFeature(infrastruktur: WegweisendeBeschilderungListenView): Feature<Geometry>[] {
    if (!infrastruktur.geometrie || !isPoint(infrastruktur.geometrie)) {
      throw new Error('Geometrie ist nicht vorhanden oder kein Punkt');
    }
    const feature = new Feature(new Point(infrastruktur.geometrie.coordinates as unknown as Coordinate));
    feature.setId(infrastruktur.id);
    feature.set(
      AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME,
      `Wegw. Beschild. ${infrastruktur.pfostenNr}`
    );
    return [feature];
  }

  protected extractIdFromFeature(hf: RadVisFeature): number {
    return Number(hf.id);
  }

  private showZoomInNotification(resolution: number): void {
    const zoom = this.olMapService.getZoomForResolution(resolution) ?? 0;
    if (zoom <= this.olLayer.getMinZoom()) {
      this.notifyUserService.inform('Bitte zoomen Sie hinein um Wegweisende Beschilderung anzuzeigen!');
    }
  }
}
