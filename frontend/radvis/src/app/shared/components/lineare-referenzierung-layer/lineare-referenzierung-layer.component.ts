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

/* eslint-disable @typescript-eslint/member-ordering */
import {
  ChangeDetectionStrategy,
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
import { Color } from 'ol/color';
import { Coordinate } from 'ol/coordinate';
import { FeatureLike } from 'ol/Feature';
import { Geometry, LineString, Point } from 'ol/geom';
import { Modify } from 'ol/interaction';
import { ModifyEvent } from 'ol/interaction/Modify';
import { Layer } from 'ol/layer';
import VectorLayer from 'ol/layer/Vector';
import { Source } from 'ol/source';
import VectorSource from 'ol/source/Vector';
import { Stroke } from 'ol/style';
import Fill from 'ol/style/Fill';
import Style from 'ol/style/Style';
import Text from 'ol/style/Text';
import { Subscription } from 'rxjs';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { LineStringOperations } from 'src/app/shared/models/line-string-operations';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { IS_SELECTABLE_LAYER } from 'src/app/shared/models/selectable-layer-property';
import { LineStringShifter } from 'src/app/shared/services/line-string-shifter';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

export interface SelectElementEvent {
  index: number;
  additiv: boolean;
  clickedCoordinate?: Coordinate;
}

@Component({
  selector: 'rad-lineare-referenzierung-layer',
  templateUrl: './lineare-referenzierung-layer.component.html',
  styleUrls: ['./lineare-referenzierung-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LineareReferenzierungLayerComponent implements OnDestroy, OnChanges, OnInit {
  @Input()
  originalGeometry!: LineString;
  @Input()
  segmentierung: number[] = [];
  @Input()
  selectedIndices: number[] | null = null;
  @Input()
  seitenbezug: Seitenbezug | undefined;
  @Input()
  modificationDisabled = false;
  @Input()
  hoveredSegmentIndex: number | null = null;
  @Input()
  minZoom: number | undefined = undefined;
  @Input()
  layerFilter: (l: Layer<Source>) => boolean = () => true;
  @Input()
  zIndex!: number;

  @Output()
  segmentierungChanged = new EventEmitter<number[]>();
  @Output()
  selectElement = new EventEmitter<SelectElementEvent>();
  @Output()
  deselectElement = new EventEmitter<number>();
  @Output()
  hoveredSegmentIndexChanged = new EventEmitter<number | null>();

  private shiftableSegmentPointsSource: VectorSource = new VectorSource();
  private readonly shiftableSegmentPointsLayer: VectorLayer;

  private selectableSegmentLinesSource = new VectorSource();
  private readonly selectableSegmentLinesLayer: VectorLayer;

  private readonly selektionLayer: VectorLayer;

  private readonly modifyInteraction: Modify;
  private interactionAdded = false;
  private modifiedFeatureIndex: number | null = null;

  private subscriptions: Subscription[] = [];

  constructor(private olMapService: OlMapService, private notifyUserService: NotifyUserService) {
    this.shiftableSegmentPointsLayer = new VectorLayer({
      source: this.shiftableSegmentPointsSource,
      style: this.getSegmentPointStyleFunctionWithColor(MapStyles.FEATURE_COLOR),
    });
    this.olMapService.addLayer(this.shiftableSegmentPointsLayer);

    this.selectableSegmentLinesLayer = new VectorLayer({
      source: this.selectableSegmentLinesSource,
      style: this.getStyleFunctionForSelectableSegmentLinesLayer(MapStyles.FEATURE_COLOR),
    });
    this.selectableSegmentLinesLayer.set(IS_SELECTABLE_LAYER, true);

    this.olMapService.addLayer(this.selectableSegmentLinesLayer);

    this.modifyInteraction = new Modify({
      source: this.shiftableSegmentPointsSource,
      style: this.getSegmentPointStyleFunctionWithColor(MapStyles.FEATURE_MODIFY_COLOR),
    });
    this.modifyInteraction.on('modifyend', this.onModifyEnd);
    this.modifyInteraction.on('modifystart', this.onModifyStart);
    this.olMapService.addInteraction(this.modifyInteraction);
    this.interactionAdded = true;

    const selektierteFeaturesSource: VectorSource = new VectorSource();
    this.selektionLayer = new VectorLayer({
      source: selektierteFeaturesSource,
      style: this.getStyleForSelektionLayer(MapStyles.FEATURE_SELECT_COLOR),
      minZoom: 0,
    });
    this.olMapService.addLayer(this.selektionLayer);

    this.subscriptions.push(
      this.olMapService.click$().subscribe(clickEvent => {
        this.onMapClick(clickEvent);
      })
    );
    this.subscriptions.push(this.olMapService.pointerMove$().subscribe(moveEvent => this.onMapPointerMove(moveEvent)));
    this.subscriptions.push(this.olMapService.pointerLeave$().subscribe(() => this.changeHoveredSegmentIndex(null)));
    this.subscriptions.push(
      this.olMapService.getResolution$().subscribe(resolution => this.applyMinZoomToInteraction(resolution))
    );
  }

  private get displayedGeometry(): LineString {
    if (this.seitenbezug) {
      let shiftDistanceInPixel = MapStyles.LINE_WIDTH_FOR_DOUBLE_LINE / 2 + MapStyles.LINE_GAP_FOR_DOUBLE_LINE / 2;
      if (this.seitenbezug === Seitenbezug.LINKS) {
        shiftDistanceInPixel = -shiftDistanceInPixel;
      } else if (this.seitenbezug !== Seitenbezug.RECHTS) {
        throw Error('Kein valider Seitenbezug');
      }
      return LineStringShifter.shiftLineStringByPixel(
        this.originalGeometry,
        shiftDistanceInPixel,
        this.olMapService.getCurrentResolution() as number
      );
    } else {
      return this.originalGeometry;
    }
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.shiftableSegmentPointsLayer);
    this.olMapService.removeLayer(this.selectableSegmentLinesLayer);
    this.olMapService.removeLayer(this.selektionLayer);
    this.modifyInteraction.un('modifyend', this.onModifyEnd);
    this.modifyInteraction.un('modifystart', this.onModifyStart);
    this.olMapService.removeInteraction(this.modifyInteraction);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  ngOnInit(): void {
    invariant(this.zIndex);
    this.shiftableSegmentPointsLayer.setZIndex(this.zIndex + 1);
    this.selectableSegmentLinesLayer.setZIndex(this.zIndex);
    this.selektionLayer.setZIndex(this.zIndex - 1);

    // Warum?
    this.subscriptions.push(this.olMapService.getResolution$().subscribe(() => this.redraw(this.segmentierung)));

    const currentResolution = this.olMapService.getCurrentResolution();
    if (currentResolution) {
      this.applyMinZoomToInteraction(currentResolution);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(this.originalGeometry, 'Geometrie muss gesetzt sein');
    invariant(this.segmentierung, 'Segmentierung muss gesetzt sein');
    invariant(this.segmentierung.length >= 2, 'mindestens 2 Segmentierungs-Grenzen');
    invariant(this.segmentierung[0] === 0, 'Segmentierung muss bei 0 beginnen');
    invariant(this.segmentierung[this.segmentierung.length - 1] === 1, 'Segmentierung muss bei 1 enden');
    invariant(
      this.segmentierung.every(v => v >= 0 && v <= 1),
      'Segmentierung ist relativ zwischen 0 und 1'
    );
    invariant(
      this.segmentierung.every((value, index) => {
        if (index > 0) {
          return this.segmentierung[index - 1] <= value;
        }
        return true;
      }),
      'Segmentierung muss sortiert sein'
    );

    if (changes.segmentierung) {
      this.redraw(this.segmentierung);
    }
    if (changes.selectedIndices) {
      this.selectSegmentsOnIndices(this.selectedIndices);
    }
    if (changes.modificationDisabled) {
      this.modifyInteraction.setActive(!this.modificationDisabled);
    }
    if (changes.hoveredSegmentIndex) {
      let previous = changes.hoveredSegmentIndex.previousValue;
      if (previous === undefined) {
        previous = null;
      }
      let next = changes.hoveredSegmentIndex.currentValue;
      if (next === undefined) {
        next = null;
      }
      // Achtung, 0 ist auch falsy!
      this.handleHoverChange(previous, next);
    }

    if (changes.minZoom) {
      this.shiftableSegmentPointsLayer.setMinZoom(this.minZoom || 0);
      this.selectableSegmentLinesLayer.setMinZoom(this.minZoom || 0);
      this.selektionLayer.setMinZoom(this.minZoom || 0);
      const currentResolution = this.olMapService.getCurrentResolution();
      if (currentResolution) {
        this.applyMinZoomToInteraction(currentResolution);
      }
    }
  }

  private onMapPointerMove(pointerMoveEvent: MapBrowserEvent<UIEvent>): void {
    const featuresAtPixel = this.olMapService.getFeaturesAtPixel(pointerMoveEvent.pixel, undefined, 5);
    if (!featuresAtPixel || featuresAtPixel.length === 0) {
      this.changeHoveredSegmentIndex(null);
    } else {
      const hoveredFeature = featuresAtPixel[0] as Feature<Geometry>;
      const index = this.selectableSegmentLinesSource.getFeatures().findIndex(f => f === hoveredFeature);
      if (index >= 0) {
        this.changeHoveredSegmentIndex(index);
      } else {
        this.changeHoveredSegmentIndex(null);
      }
    }
  }

  private changeHoveredSegmentIndex(newIndex: number | null): void {
    if (newIndex !== this.hoveredSegmentIndex) {
      this.hoveredSegmentIndex = newIndex;
      this.hoveredSegmentIndexChanged.emit(newIndex);
    }
  }

  private onMapClick(clickEvent: MapBrowserEvent<UIEvent>): void {
    const featuresAtPixel = this.olMapService.getFeaturesAtPixel(clickEvent.pixel, this.layerFilter);
    if (!featuresAtPixel || featuresAtPixel.length === 0) {
      return;
    }
    // Das erste Feature im Array ist das am nähesten zur Click-Position liegende
    const clickedFeature = featuresAtPixel[0] as Feature<Geometry>;
    if (this.selectableSegmentLinesLayer.getSource().hasFeature(clickedFeature)) {
      //<-- ist nicht unbedingt durch LayerFilter gegeben (und darf auch nicht darüber gemacht werden)
      const pointerEvent = clickEvent.originalEvent as PointerEvent;
      const toggle = pointerEvent.ctrlKey || pointerEvent.metaKey;
      const index = this.selectableSegmentLinesSource.getFeatures().findIndex(f => f === clickedFeature);
      if (!toggle) {
        this.selectElement.emit({ index, additiv: false, clickedCoordinate: clickEvent.coordinate });
      } else {
        if (this.selectedIndices?.includes(index)) {
          this.deselectElement.emit(index);
        } else {
          this.selectElement.emit({ index, additiv: true, clickedCoordinate: clickEvent.coordinate });
        }
      }
    }
  }

  private redraw(segmentierung: number[]): void {
    const shiftableSegmentPointFeatures = segmentierung
      .map(fraction => {
        const coordinate = this.displayedGeometry.getCoordinateAt(fraction);
        return new Feature(new Point(coordinate));
      })
      .slice(1, -1); // ohne Start- und Endpunkt

    this.shiftableSegmentPointsSource.clear();

    this.shiftableSegmentPointsSource.addFeatures(shiftableSegmentPointFeatures);
    this.modifyInteraction.changed();

    const lineStringFeatures: Feature[] = LineStringOperations.splitLinestring(
      segmentierung,
      this.displayedGeometry
    ).map(line => new Feature(line));
    this.selectableSegmentLinesSource.clear();
    this.selectableSegmentLinesSource.addFeatures(lineStringFeatures);
    this.selectSegmentsOnIndices(this.selectedIndices);
  }

  private selectSegmentsOnIndices(selectedIndices: number[] | null): void {
    this.selektionLayer.getSource().clear();
    if (selectedIndices !== null) {
      selectedIndices.forEach(selectedIndex => {
        const selectedFeature = this.selectableSegmentLinesSource.getFeatures()[selectedIndex];
        selectedFeature.set('segmentIndex', selectedIndex);
        this.selektionLayer.getSource().addFeature(selectedFeature);
      });
      this.selektionLayer.changed();
    }
  }

  private handleHoverChange(previous: number | null, next: number | null): void {
    // Achtung, 0 ist auch falsy
    if (previous !== null) {
      // unhover
      this.setColorForSegment(previous, MapStyles.FEATURE_COLOR, MapStyles.FEATURE_SELECT_COLOR);
    }
    if (next !== null) {
      // hover
      this.setColorForSegment(next, MapStyles.FEATURE_HOVER_COLOR, MapStyles.FEATURE_HOVER_COLOR);
    }
  }

  private setColorForSegment(segmentIndex: number, kantenLayerColor: Color, selektionLayerColor: Color): void {
    const selectableFeature = this.selectableSegmentLinesSource.getFeatures()[segmentIndex];
    selectableFeature.setStyle(this.getStyleFunctionForSelectableSegmentLinesLayer(kantenLayerColor));
    const selectedFeature = this.selektionLayer
      .getSource()
      .getFeatures()
      .find(feature => feature.get('segmentIndex') === segmentIndex);
    if (selectedFeature) {
      selectedFeature.setStyle(this.getStyleForSelektionLayer(selektionLayerColor));
    }
  }

  private onModifyStart = (event: ModifyEvent): void => {
    invariant(
      event.features.getLength() === 1,
      'Es muss ein Feature im Modify Event vorhanden sein, es sind aber ' + event.features.getLength()
    );
    const featureIndex = this.shiftableSegmentPointsSource
      .getFeatures()
      .findIndex(f => f === event.features.getArray()[0]);
    if (featureIndex > -1) {
      this.modifiedFeatureIndex = featureIndex + 1; // startpunkt ist in shiftableSPointsSource nicht enthalten
    } else {
      throw new Error('Cannot find modified Feature');
    }
  };

  private onModifyEnd = (event: ModifyEvent): void => {
    invariant(event.features.getLength() === 1);
    invariant(this.modifiedFeatureIndex !== null);
    const newFraction = LineStringOperations.getFractionOfPointOnLineString(
      (event.features.getArray()[0].getGeometry() as Point).getCoordinates(),
      this.displayedGeometry
    );

    if (
      newFraction < this.segmentierung[this.modifiedFeatureIndex + 1] &&
      newFraction > this.segmentierung[this.modifiedFeatureIndex - 1]
    ) {
      const newSegmentierung = this.segmentierung.slice();
      newSegmentierung[this.modifiedFeatureIndex] = newFraction;
      this.segmentierungChanged.next(newSegmentierung);
    } else {
      this.redraw(this.segmentierung);
      this.notifyUserService.inform('Ungültige Eingabe. Der Marker wurde zurückgesetzt.');
    }

    this.modifiedFeatureIndex = null;
  };

  private getSegmentPointStyleFunctionWithColor(color: Color) {
    return (feature: FeatureLike): Style => {
      const point = feature.getGeometry() as Point;
      const segmentOfPoint = LineStringOperations.getSegmentOfPointOnLineString(point, this.displayedGeometry);
      let offsetX = 0;
      if (this.seitenbezug === Seitenbezug.LINKS) {
        offsetX = -3;
      } else if (this.seitenbezug === Seitenbezug.RECHTS) {
        offsetX = 3;
      }
      return new Style({
        text: new Text({
          text: '-',
          font: 'bold 30px Roboto',
          fill: new Fill({
            color,
          }),
          rotation: LineStringOperations.getPerpendicularAngleOfLine(segmentOfPoint),
          offsetX,
          offsetY: 3,
        }),
      });
    };
  }

  private getStyleFunctionForSelectableSegmentLinesLayer(
    featureColor: Color
  ): (feature: FeatureLike, resolution: number) => Style | Style[] {
    return (feature: FeatureLike): Style | Style[] => {
      const index = this.selectableSegmentLinesSource.getFeatures().findIndex(f => f === feature);
      if (this.selectedIndices?.includes(index)) {
        return new Style();
      } else {
        return new Style({
          stroke: new Stroke({
            color: featureColor,
            width: MapStyles.LINE_WIDTH_FOR_DOUBLE_LINE,
          }),
        });
      }
    };
  }

  private getStyleForSelektionLayer(featureColor: Color): Style {
    return new Style({
      stroke: new Stroke({
        color: featureColor,
        width: MapStyles.LINE_WIDTH_FOR_DOUBLE_LINE,
      }),
    });
  }

  private applyMinZoomToInteraction(resolution: number): void {
    const zoom = this.olMapService.getZoomForResolution(resolution);
    if (zoom && this.minZoom && this.interactionAdded && zoom < this.minZoom) {
      this.olMapService.removeInteraction(this.modifyInteraction);
      this.interactionAdded = false;
    }
    if (zoom && this.minZoom && !this.interactionAdded && zoom >= this.minZoom) {
      this.olMapService.addInteraction(this.modifyInteraction);
      this.interactionAdded = true;
    }
  }
}
