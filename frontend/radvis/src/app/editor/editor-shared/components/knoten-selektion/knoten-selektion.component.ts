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

import { ChangeDetectionStrategy, Component, Input, NgZone, OnDestroy } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Feature, MapBrowserEvent } from 'ol';
import GeoJSON from 'ol/format/GeoJSON';
import { Geometry } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import Circle from 'ol/style/Circle';
import { Subscription } from 'rxjs';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { createVectorSource } from 'src/app/shared/services/vector-source.factory';
import { filter } from 'rxjs/operators';
import Fill from 'ol/style/Fill';

@Component({
  selector: 'rad-knoten-selektion',
  templateUrl: './knoten-selektion.component.html',
  styleUrls: ['./knoten-selektion.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KnotenSelektionComponent implements OnDestroy {
  @Input()
  knotenSelectable = true;

  knotenLayers: VectorLayer[] = [];

  selektionLayer: VectorLayer;
  private netzklassen: Netzklassefilter[] = [];
  private subscriptions: Subscription[] = [];

  constructor(
    private olMapService: OlMapService,
    private zone: NgZone,
    private errorHandlingService: ErrorHandlingService,
    private featureService: NetzausschnittService,
    public editorRoutingService: EditorRoutingService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    netzklassenAuswahlService: NetzklassenAuswahlService
  ) {
    netzklassenAuswahlService.currentAuswahl$.subscribe(selectedNetzklassen => {
      this.netzklassen = selectedNetzklassen;
      this.updateNetzklassenVisibility();
    });

    Netzklassefilter.getAll().forEach(nk => {
      const vectorSource = createVectorSource({
        getFeaturesObservable: extent => this.featureService.getKnotenForView(extent, [nk]),
        parseFeatures: featureCollection => {
          const features = new GeoJSON().readFeatures(featureCollection);
          for (const feature of features) {
            feature.set(FeatureProperties.KNOTEN_ID_PROPERTY_NAME, Number(feature.getId()));
          }
          return features;
        },
        onFeaturesLoaded: () => this.refreshSelektionLayer(),
        onError: error => this.errorHandlingService.handleError(error),
      });

      this.knotenLayers.push(
        new VectorLayer({
          source: vectorSource,
          // @ts-expect-error Migration von ts-ignore
          renderOrder: null,
          style: new Style({
            image: MapStyles.circleWithFill(5),
          }),
          minZoom: Math.max(nk.minZoom, MapStyles.DEFAULT_MIN_ZOOM_VALUE),
          zIndex: EditorLayerZindexConfig.KNOTEN_ANZEIGEN_LAYER,
          netzklasse: nk,
        })
      );
    });

    this.updateNetzklassenVisibility();

    this.knotenLayers.forEach(kl => this.olMapService.addLayer(kl));

    const selektierteFeaturesSource: VectorSource = new VectorSource();
    this.selektionLayer = new VectorLayer({
      source: selektierteFeaturesSource,
      style: new Style({
        image: new Circle({
          radius: MapStyles.POINT_WIDTH_THICK,
          fill: new Fill({
            color: MapStyles.FEATURE_SELECT_COLOR,
          }),
        }),
      }),
      minZoom: MapStyles.DEFAULT_MIN_ZOOM_VALUE,
      zIndex: EditorLayerZindexConfig.KNOTEN_SELECTION_LAYER,
    });
    this.olMapService.addLayer(this.selektionLayer);

    this.subscriptions.push(
      this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(() => {
        this.refreshSelektionLayer();
      })
    );

    this.subscriptions.push(
      this.olMapService.click$().subscribe(clickEvent => {
        this.onMapClick(clickEvent);
      })
    );
  }

  ngOnDestroy(): void {
    this.knotenLayers.forEach(kl => this.olMapService.removeLayer(kl));
    this.olMapService.removeLayer(this.selektionLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  private updateNetzklassenVisibility(): void {
    this.knotenLayers.forEach(kl => {
      kl.setVisible(this.netzklassen.includes(kl.getProperties().netzklasse));
    });
  }

  private onMapClick(clickEvent: MapBrowserEvent<PointerEvent | KeyboardEvent | WheelEvent>): void {
    const featuresAtPixel = this.olMapService.getFeaturesAtPixel(clickEvent.pixel, () => this.knotenSelectable);
    if (!featuresAtPixel || featuresAtPixel.length === 0) {
      return;
    }
    // Das erste Feature im Array ist das am nächsten zur Click-Position liegende
    const clickedFeature = featuresAtPixel[0] as Feature<Geometry>;
    if (this.knotenLayers.some(kl => kl.getSource()?.hasFeature(clickedFeature))) {
      this.zone.run(() => {
        this.editorRoutingService.toKnotenAttributeEditor(+(clickedFeature.getId() as number));
      });
    }
  }

  private refreshSelektionLayer(): void {
    this.selektionLayer.getSource()?.clear();
    const knotenId = this.activatedRoute.snapshot.firstChild?.params.id;
    if (knotenId) {
      let found = false;
      this.knotenLayers.forEach(kl => {
        kl.getSource()
          ?.getFeatures()
          .forEach(feature => {
            if (!found && +knotenId === +(feature.getId() as number)) {
              this.selektionLayer.getSource()?.addFeature(feature);
              found = true;
            }
          });
      });
    }
  }
}
