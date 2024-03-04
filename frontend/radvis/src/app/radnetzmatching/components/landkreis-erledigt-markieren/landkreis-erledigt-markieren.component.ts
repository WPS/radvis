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

import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Feature, MapBrowserEvent } from 'ol';
import { FeatureLike } from 'ol/Feature';
import GeoJSON from 'ol/format/GeoJSON';
import { Geometry } from 'ol/geom';
import { Layer } from 'ol/layer';
import VectorLayer from 'ol/layer/Vector';
import { bbox } from 'ol/loadingstrategy';
import { Source } from 'ol/source';
import VectorSource from 'ol/source/Vector';
import { Fill, Stroke, Style } from 'ol/style';
import { Subscription } from 'rxjs';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { LadeZustandService } from 'src/app/shared/services/lade-zustand.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';

@Component({
  selector: 'rad-landkreis-erledigt-markieren',
  templateUrl: './landkreis-erledigt-markieren.component.html',
  styleUrls: ['./landkreis-erledigt-markieren.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LandkreisErledigtMarkierenComponent implements OnChanges, OnDestroy {
  @Input()
  erledigtMarkierenActive = false;

  private readonly vectorSource!: VectorSource;
  private readonly olLayer!: VectorLayer;
  private subscriptions: Subscription[] = [];

  constructor(
    private olMapService: OlMapService,
    private organisationenService: OrganisationenService,
    private errorHandlingService: ErrorHandlingService,
    private dialog: MatDialog,
    private ladeZustandService: LadeZustandService,
    private notifyUserService: NotifyUserService
  ) {
    this.vectorSource = this.createSource();
    this.olLayer = this.createLayer();
    this.olMapService.addLayer(this.olLayer);
    this.subscriptions.push(this.olMapService.click$().subscribe(clickEvent => this.onMapClick(clickEvent)));
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.erledigtMarkierenActive) {
      this.olLayer.setVisible(this.erledigtMarkierenActive);
    }
  }

  private onMapClick(clickEvent: MapBrowserEvent<UIEvent>): void {
    const featuresAtPixel = this.olMapService.getFeaturesAtPixel(
      clickEvent.pixel,
      (layer: Layer<Source>) => layer === this.olLayer
    );
    if (!featuresAtPixel || featuresAtPixel.length === 0) {
      return;
    }
    const clickedFeature = featuresAtPixel[0] as Feature<Geometry>;
    if (clickedFeature.get('istQualitaetsgesichert')) {
      this.notifyUserService.warn(
        `Der Landkreis "${clickedFeature.get('name')}" wurde bereits als qualitätsgesichert markiert.`
      );
      return;
    }
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question: `Wollen Sie den Landkreis "${clickedFeature.get('name')}" als qualitätsgesichert markieren?`,
        labelYes: '✓ Qualitätsgesichert',
        labelNo: 'Abbrechen',
        inverseButtonColorCoding: true,
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        this.ladeZustandService.startLoading();
        this.organisationenService
          .markAsQualitaetsgesichert(Number(clickedFeature.get('id')))
          .then(() => {
            this.ladeZustandService.finishedLoading();
            this.vectorSource.refresh();
          })
          .catch(() => this.ladeZustandService.finishedLoading());
      }
    });
  }

  private createSource(): VectorSource {
    return new VectorSource({
      format: new GeoJSON(),
      useSpatialIndex: false,
      loader: (): void => {
        this.ladeZustandService.startLoading();
        this.organisationenService
          .getKreiseAlsFeatures()
          .then(kreiseAlsFeatures => {
            this.vectorSource.clear(true);
            this.vectorSource.addFeatures(new GeoJSON().readFeatures(kreiseAlsFeatures));
          })
          .finally(() => {
            this.ladeZustandService.finishedLoading();
          });
      },
      strategy: bbox,
    });
  }

  private createLayer(): VectorLayer {
    return new VectorLayer({
      source: this.vectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: (feature: FeatureLike): Style => {
        const greenTransparent = [0, 255, 0, 0.1];
        const redTransparent = [255, 0, 0, 0.1];
        const fillColor = feature.get('istQualitaetsgesichert') ? greenTransparent : redTransparent;
        return new Style({
          stroke: new Stroke({
            color: [0, 0, 0, 1],
            width: 3,
          }),
          fill: new Fill({
            color: fillColor,
          }),
        });
      },
      minZoom: 6,
      zIndex: 15,
    });
  }
}
