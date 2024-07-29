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
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { Feature } from 'ol';
import { FeatureLike } from 'ol/Feature';
import { Coordinate } from 'ol/coordinate';
import { Extent } from 'ol/extent';
import { MultiPoint } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { bbox } from 'ol/loadingstrategy';
import VectorSource from 'ol/source/Vector';
import { Icon, Style } from 'ol/style';
import IconOrigin from 'ol/style/IconOrigin';
import { Subscription } from 'rxjs';
import { FehlerprotokollDetailViewComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-detail-view/fehlerprotokoll-detail-view.component';
import { FehlerprotokollView } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-view';
import { FehlerprotokollSelectionService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll-selection.service';
import { Geojson, geojsonGeometryToFeatureGeometry } from 'src/app/shared/models/geojson-geometrie';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { LocationSelectEvent } from 'src/app/shared/models/location-select-event';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-fehlerprotokoll-layer',
  templateUrl: './fehlerprotokoll-layer.component.html',
  styleUrls: ['./fehlerprotokoll-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FehlerprotokollLayerComponent implements OnInit, OnDestroy {
  public static readonly LAYER_ID = 'fehlerprotokoll';
  public static readonly HIGHLIGHTED_PROPERTY_NAME = 'highlighted';
  private static readonly PROTOKOLL_ID_PROPERTYNAME = 'protokollIdentifier';
  private static readonly ORIGINAL_GEOMETRY_PROPERTYNAME = 'originalGeometry';

  @Input()
  canCreateAnpassungswunsch = false;

  @Input()
  zIndex!: number;

  @ViewChild(FehlerprotokollDetailViewComponent)
  detailView: FehlerprotokollDetailViewComponent | undefined;

  public selectedFeature: RadVisFeature | null = null;

  private subscriptions: Subscription[] = [];
  private iconLayer: VectorLayer;
  private geometryLayer: VectorLayer;
  private iconVectorSource = new VectorSource();
  private geometryVectorSource = new VectorSource();

  constructor(
    private olMapService: OlMapService,
    private fehlerprotokollSelectionService: FehlerprotokollSelectionService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.iconVectorSource = new VectorSource({ strategy: bbox });
    this.geometryVectorSource = new VectorSource();

    this.iconLayer = new VectorLayer({
      source: this.iconVectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: this.iconStyleFct,
    });

    this.geometryLayer = new VectorLayer({
      source: this.geometryVectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: MapStyles.getDefaultNetzStyleFunction(MapStyles.FEHLERPROTOKOLL_COLOR),
    });

    this.iconLayer.set(OlMapService.LAYER_ID, FehlerprotokollLayerComponent.LAYER_ID);
    this.olMapService.addLayer(this.iconLayer);
    this.olMapService.addLayer(this.geometryLayer);

    this.subscriptions.push(
      this.fehlerprotokollSelectionService.fehlerprotokollLoader$.subscribe(fehlerprotokollLoader => {
        this.iconVectorSource.setLoader((extent: Extent) => {
          fehlerprotokollLoader(extent).subscribe(fVs => this.showFehlerprotokollIcons(fVs));
        });
        this.iconVectorSource.clear(true);
        this.iconVectorSource.refresh();
      }),
      this.fehlerprotokollSelectionService.minZoom$.subscribe(minZoom => {
        this.iconLayer.setMinZoom(minZoom);
        this.iconLayer.changed();
      }),
      this.olMapService
        .locationSelected$()
        .subscribe(locationSelectedEvent => this.onLocationSelected(locationSelectedEvent))
    );
  }

  ngOnInit(): void {
    invariant(this.zIndex);
    this.iconLayer.setZIndex(this.zIndex);
    this.geometryLayer.setZIndex(this.zIndex);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(value => value.unsubscribe());
    this.olMapService.removeLayer(this.iconLayer);
    this.olMapService.removeLayer(this.geometryLayer);
  }

  public onSelect(feature: RadVisFeature, clickedCoordinate: Coordinate): void {
    invariant(feature.layer === FehlerprotokollLayerComponent.LAYER_ID);
    this.clearSelection();
    this.select(feature, clickedCoordinate);
    this.changeDetectorRef.markForCheck();
  }

  public onCloseDetailView(): void {
    this.clearSelection();
  }

  private showFehlerprotokollIcons(fehlerprotokollViews: FehlerprotokollView[]): void {
    const features = fehlerprotokollViews.map(fehlerprotokoll => this.convertToFeature(fehlerprotokoll));
    this.iconVectorSource.clear(true);
    this.iconVectorSource.addFeatures(features);
    this.iconVectorSource.changed();
  }

  private iconStyleFct = (feature: FeatureLike): Style | Style[] => {
    const opacity = feature.get(FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME) ? 1 : 0.8;
    const color = feature.get(FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME)
      ? MapStyles.INFRASTRUKTUR_ICON_HIGHLIGHT_COLOR
      : MapStyles.INFRASTRUKTUR_ICON_COLOR;
    return [
      new Style({
        image: new Icon({
          anchor: [0.6, 0.5],
          anchorOrigin: IconOrigin.BOTTOM_LEFT,
          src: './assets/konflikt-background.svg',
          color,
          opacity,
        }),
      }),
      new Style({
        image: new Icon({
          anchor: [0.6, 0.5],
          anchorOrigin: IconOrigin.BOTTOM_LEFT,
          src: './assets/konflikt.svg',
          opacity,
        }),
      }),
    ];
  };

  private convertToFeature(fehlerprotokoll: FehlerprotokollView): Feature {
    const feature = geojsonGeometryToFeatureGeometry(fehlerprotokoll.iconPosition);
    invariant(feature);
    const id = `${fehlerprotokoll.fehlerprotokollKlasse}/${fehlerprotokoll.id}`;
    feature.setId(id);
    feature.set(FehlerprotokollLayerComponent.PROTOKOLL_ID_PROPERTYNAME, id);
    feature.set(FehlerprotokollLayerComponent.ORIGINAL_GEOMETRY_PROPERTYNAME, fehlerprotokoll.originalGeometry);
    feature.set('titel', fehlerprotokoll.titel);
    feature.set('beschreibung', fehlerprotokoll.beschreibung);
    feature.set('entityLink', fehlerprotokoll.entityLink);
    feature.set('datum', fehlerprotokoll.datum);
    feature.set(
      FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME,
      this.selectedFeature?.attributes.get(FehlerprotokollLayerComponent.PROTOKOLL_ID_PROPERTYNAME) === id ?? false
    );
    return feature;
  }

  private select(selectedFeature: RadVisFeature, clickedCoordinate: Coordinate): void {
    const selectedOLFeature = this.iconVectorSource
      .getFeatures()
      .find(f => f.getId() === selectedFeature.attributes.get(FehlerprotokollLayerComponent.PROTOKOLL_ID_PROPERTYNAME));

    if (selectedOLFeature) {
      selectedOLFeature.set(FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME, true);
      selectedOLFeature.changed();
    }

    this.selectedFeature = selectedFeature;

    this.showFehlerprotokollGeometry(
      this.selectedFeature?.attributes.get(FehlerprotokollLayerComponent.ORIGINAL_GEOMETRY_PROPERTYNAME)
    );

    this.showDetailView(selectedFeature, clickedCoordinate);
  }

  private showDetailView(selectedFeature: RadVisFeature, clickedCoordinate: Coordinate): void {
    invariant(this.detailView);
    const location = (selectedFeature.geometry as MultiPoint).getClosestPoint(clickedCoordinate);

    this.detailView.showFehlerprotokoll(
      location,
      selectedFeature.attributes.get(FehlerprotokollLayerComponent.PROTOKOLL_ID_PROPERTYNAME),
      selectedFeature.attributes.get('titel'),
      selectedFeature.attributes.get('beschreibung'),
      selectedFeature.attributes.get('entityLink'),
      selectedFeature.attributes.get('datum')
    );
  }

  private clearSelection(): void {
    this.iconVectorSource
      .getFeatures()
      .find(feature => feature.get(FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME))
      ?.set(FehlerprotokollLayerComponent.HIGHLIGHTED_PROPERTY_NAME, false);

    this.selectedFeature = null;

    this.geometryVectorSource.clear();
  }

  private showFehlerprotokollGeometry(geojson: Geojson): void {
    const feature = geojsonGeometryToFeatureGeometry(geojson);
    if (feature) {
      this.geometryVectorSource.addFeature(feature);
    }
  }

  private onLocationSelected(locationSelectEvent: LocationSelectEvent): void {
    if (
      locationSelectEvent.selectedFeatures.length > 0 &&
      locationSelectEvent.selectedFeatures[0].layer === FehlerprotokollLayerComponent.LAYER_ID
    ) {
      this.onSelect(locationSelectEvent.selectedFeatures[0], locationSelectEvent.coordinate);
    }
  }
}
