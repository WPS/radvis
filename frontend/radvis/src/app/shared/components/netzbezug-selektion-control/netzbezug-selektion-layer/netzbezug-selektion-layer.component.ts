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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import { Feature, MapBrowserEvent } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { Geometry, LineString, Point } from 'ol/geom';
import GeometryType from 'ol/geom/GeometryType';
import { Subscription } from 'rxjs';
import { SelectElementEvent } from 'src/app/shared/components/lineare-referenzierung-layer/lineare-referenzierung-layer.component';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { LineStringGeojson, PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { LineStringOperations } from 'src/app/shared/models/line-string-operations';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { KantenNetzVectorlayer } from 'src/app/shared/components/netzbezug-selektion-control/kanten-netz-vectorlayer';
import { KnotenNetzVectorLayer } from 'src/app/shared/components/netzbezug-selektion-control/knoten-netz-vectorlayer';
import { NetzbezugSelektion } from 'src/app/shared/components/netzbezug-selektion-control/netzbezug-selektion-layer/netzbezug-selektion';
import { PunktuellerKantenBezuegeVectorLayer } from 'src/app/shared/components/netzbezug-selektion-control/punktueller-kanten-bezuege-vector-layer';
import { KnotenNetzbezug } from 'src/app/shared/models/knoten-netzbezug';
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { PunktuellerKantenNetzBezug } from 'src/app/shared/models/punktueller-kanten-netzbezug';
import { NetzbezugAuswahlModusService } from 'src/app/shared/services/netzbezug-auswahl-modus.service';
import invariant from 'tiny-invariant';
import { lineareReferenzierungLayerZIndex } from 'src/app/shared/models/shared-layer-zindex-config';

@Component({
  selector: 'rad-netzbezug-selektion-layer',
  templateUrl: './netzbezug-selektion-layer.component.html',
  styleUrls: ['./netzbezug-selektion-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NetzbezugSelektionLayerComponent implements OnDestroy, OnInit, OnChanges {
  @Input()
  netzbezug: Netzbezug | null = null;
  @Input()
  pointSelectionMode = false;
  @Input()
  schereMode = false;
  @Input()
  netzZweiseitig = false;

  @Output()
  netzbezugChange: EventEmitter<Netzbezug> = new EventEmitter<Netzbezug>();

  selectedNetzbezug: NetzbezugSelektion = new NetzbezugSelektion();

  RECHTS = KantenSeite.RECHTS;
  LINKS = KantenSeite.LINKS;
  minZoom = 16;
  lineareReferenzierungZIndex = lineareReferenzierungLayerZIndex;

  private kantenNetzLayer: KantenNetzVectorlayer | undefined;
  private knotenNetzLayer: KnotenNetzVectorLayer | undefined;
  private punktuelleKantenBezuegeLayer: PunktuellerKantenBezuegeVectorLayer | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    private olMapService: OlMapService,
    private radvisNetzFeatureService: NetzausschnittService,
    private errorHandlingService: ErrorHandlingService,
    private changeDetectorRef: ChangeDetectorRef,
    private netzbezugAuswahlModusService: NetzbezugAuswahlModusService,
    private notifyUserService: NotifyUserService
  ) {
    this.netzbezugAuswahlModusService.startNetzbezugAuswahl();
    this.subscriptions.push(this.olMapService.click$().subscribe(clickEvent => this.onMapClick(clickEvent)));
    this.displayZoomWarningIfLayersNotVisible();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // unhighlight previous netzbezug
    if (changes.netzbezug && !changes.netzbezug.isFirstChange() && changes.netzbezug.previousValue !== null) {
      this.selectedNetzbezug = new NetzbezugSelektion();
      const netzbezug = changes.netzbezug.previousValue as Netzbezug;
      this.kantenNetzLayer?.kanteEinblenden(...netzbezug.kantenBezug.map(bezug => bezug.kanteId));
      netzbezug.knotenBezug.forEach(knoten => this.knotenNetzLayer?.toggleHighlightKnoten(knoten.knotenId));
    }

    if (changes.netzbezug) {
      this.highlightNetzbezug();
    }

    if (changes.netzZweiseitig) {
      this.kantenNetzLayer?.setZweiseitigeNetzanzeige(this.netzZweiseitig);
    }

    this.changeDetectorRef.markForCheck();
  }

  ngOnDestroy(): void {
    if (this.kantenNetzLayer) {
      this.olMapService.removeLayer(this.kantenNetzLayer);
    }
    if (this.knotenNetzLayer) {
      this.olMapService.removeLayer(this.knotenNetzLayer);
    }
    if (this.punktuelleKantenBezuegeLayer) {
      this.olMapService.removeLayer(this.punktuelleKantenBezuegeLayer);
    }
    this.subscriptions.forEach(s => s.unsubscribe());
    this.netzbezugAuswahlModusService.stopNetzbezugAuswahl();
  }

  ngOnInit(): void {
    this.kantenNetzLayer = new KantenNetzVectorlayer(
      this.radvisNetzFeatureService,
      this.errorHandlingService,
      this.netzZweiseitig,
      this.minZoom
    );
    this.knotenNetzLayer = new KnotenNetzVectorLayer(
      this.radvisNetzFeatureService,
      this.errorHandlingService,
      this.minZoom
    );
    this.punktuelleKantenBezuegeLayer = new PunktuellerKantenBezuegeVectorLayer(this.minZoom);
    this.highlightNetzbezug();
    this.olMapService.addLayer(this.kantenNetzLayer);
    this.olMapService.addLayer(this.knotenNetzLayer);
    this.olMapService.addLayer(this.punktuelleKantenBezuegeLayer);
  }

  onSegmentierungChanged(newSegmentierung: number[], kanteId: number, kantenSeite?: KantenSeite): void {
    this.selectedNetzbezug.updateSegmentierung(kanteId, newSegmentierung, kantenSeite);
    this.netzbezugChange.emit(this.selectedNetzbezug.toNetzbezug());
  }

  onSelectSegment(event: SelectElementEvent, kanteId: number, kantenSeite?: KantenSeite): void {
    invariant(event.clickedCoordinate);
    if (this.pointSelectionMode) {
      const geometry = new LineString(this.selectedNetzbezug.getSelektionForKante(kanteId).geometrie.coordinates);
      this.selectPointOnKante(kanteId, geometry, event.clickedCoordinate);
    } else if (this.schereMode) {
      this.selectedNetzbezug.kanteSchneiden(kanteId, event.clickedCoordinate, kantenSeite);
    } else {
      this.selectedNetzbezug.selectSegment(kanteId, event.index, kantenSeite);
    }

    this.netzbezugChange.emit(this.selectedNetzbezug.toNetzbezug());
  }

  onDeselectSegment(index: number, kanteId: number, kantenSeite?: KantenSeite): void {
    if (this.pointSelectionMode) {
      return;
    }

    this.selectedNetzbezug.deselectSegment(kanteId, index, kantenSeite);

    this.netzbezugChange.emit(this.selectedNetzbezug.toNetzbezug());
  }

  private highlightNetzbezug(): void {
    if (this.netzbezug !== null) {
      this.selectedNetzbezug = new NetzbezugSelektion(this.netzbezug);

      this.kantenNetzLayer?.kanteAusblenden(...this.selectedNetzbezug.kantenSeitenAbschnitte.map(k => k.kanteId));
      for (const knotenNetzbezug of this.selectedNetzbezug.knoten) {
        this.knotenNetzLayer?.toggleHighlightKnoten(knotenNetzbezug.knotenId);
      }
      this.punktuelleKantenBezuegeLayer?.updatePuntuelleKantenNetzbezuege(this.netzbezug.punktuellerKantenBezug);
    }
  }

  private onMapClick(clickEvent: MapBrowserEvent<UIEvent>): void {
    const removeSelectedPointKeyActive =
      (clickEvent.originalEvent as PointerEvent).ctrlKey || (clickEvent.originalEvent as PointerEvent).metaKey;

    let featuresAtPixel = this.olMapService.getFeaturesAtPixel(clickEvent.pixel)?.map(f => f as Feature<Geometry>);

    if (this.pointSelectionMode && removeSelectedPointKeyActive) {
      featuresAtPixel = featuresAtPixel?.filter(f => f.getGeometry()?.getType() === GeometryType.POINT);
    }

    if (!featuresAtPixel || featuresAtPixel.length === 0) {
      return;
    }

    // Das erste Feature im Array ist das am nächsten zur Click-Position liegende
    const clickedFeature = featuresAtPixel[0];
    if (
      !(
        this.knotenNetzLayer?.hasFeature(clickedFeature) ||
        this.kantenNetzLayer?.hasFeature(clickedFeature) ||
        this.punktuelleKantenBezuegeLayer?.hasFeature(clickedFeature)
      )
    ) {
      return;
    }

    if (clickedFeature.get(FeatureProperties.STRECKE_PROPERTY_NAME)) {
      this.notifyUserService.inform('Um Kanten auszuwählen, zoomen Sie weiter rein.');
      return;
    }

    if (this.knotenNetzLayer?.getSource().hasFeature(clickedFeature)) {
      this.selectKnoten(clickedFeature);
    } else if (this.pointSelectionMode) {
      if (this.kantenNetzLayer?.getSource().hasFeature(clickedFeature)) {
        const kanteId = +(clickedFeature.get(FeatureProperties.KANTE_ID_PROPERTY_NAME) as number);
        this.selectPointOnKante(kanteId, clickedFeature.getGeometry() as LineString, clickEvent.coordinate);
      }
      if (this.punktuelleKantenBezuegeLayer?.getSource().hasFeature(clickedFeature) && removeSelectedPointKeyActive) {
        this.removePointOnKante(clickedFeature);
      }
    } else if (this.kantenNetzLayer?.getSource().hasFeature(clickedFeature)) {
      this.selectKante(clickedFeature);
    }

    this.changeDetectorRef.detectChanges();
    this.netzbezugChange.emit(this.selectedNetzbezug.toNetzbezug());
  }

  private selectKante(clickedFeature: Feature<Geometry>): void {
    const kanteId = +(clickedFeature.get(FeatureProperties.KANTE_ID_PROPERTY_NAME) as number);

    let seitenbezug;
    if (clickedFeature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)) {
      seitenbezug = clickedFeature.get(FeatureProperties.SEITE_PROPERTY_NAME);
    }

    const geometrie = {
      coordinates: (clickedFeature.getGeometry() as LineString).getCoordinates(),
      type: 'LineString',
    } as LineStringGeojson;
    this.selectedNetzbezug.selectKante(kanteId, geometrie, seitenbezug);
  }

  private selectKnoten(clickedFeature: Feature<Geometry>): void {
    const knoten = {
      knotenId: +(clickedFeature.getId() as number),
      geometrie: {
        coordinates: (clickedFeature.getGeometry() as Point).getCoordinates(),
        type: 'Point',
      } as PointGeojson,
    } as KnotenNetzbezug;
    if (this.selectedNetzbezug.isKnotenSelected(knoten.knotenId)) {
      this.selectedNetzbezug.removeKnoten(knoten);
    } else {
      this.selectedNetzbezug.addKnotenNetzbezug(knoten);
    }
  }

  private selectPointOnKante(kanteId: number, geometry: LineString, clickedCoordinate: Coordinate): void {
    const closestPoint = geometry.getClosestPoint(clickedCoordinate);

    if (!closestPoint) {
      return;
    }

    const lineareReferenz: number = LineStringOperations.getFractionOfPointOnLineString(closestPoint, geometry);

    const punktuellerKantenNetzBezug = {
      kanteId,
      lineareReferenz,
      geometrie: {
        coordinates: closestPoint,
        type: 'Point',
      } as PointGeojson,
    } as PunktuellerKantenNetzBezug;

    this.selectedNetzbezug.addPunktuellerKantenBezug(punktuellerKantenNetzBezug);
  }

  private removePointOnKante(clickedFeature: Feature<Geometry>): void {
    const punktuellerKantenNetzBezug = {
      kanteId: clickedFeature.get(PunktuellerKantenBezuegeVectorLayer.KANTE_ID_PROPERTY_KEY),
      lineareReferenz: clickedFeature.get(PunktuellerKantenBezuegeVectorLayer.LINEARE_REFERENZ_PROPERTY_KEY),
      geometrie: {
        coordinates: (clickedFeature.getGeometry() as Point).getCoordinates(),
        type: 'Point',
      } as PointGeojson,
    } as PunktuellerKantenNetzBezug;

    this.selectedNetzbezug.removePunktuellerKantenBezug(punktuellerKantenNetzBezug);
  }

  private displayZoomWarningIfLayersNotVisible(): void {
    const resolution = this.olMapService.getCurrentResolution();
    if (resolution) {
      const zoom = this.olMapService.getZoomForResolution(resolution) ?? 0;
      if (zoom <= this.minZoom) {
        this.notifyUserService.inform('Bitte zoomen Sie hinein um den Netzbezug zu bearbeiten!');
      }
    }
  }
}
