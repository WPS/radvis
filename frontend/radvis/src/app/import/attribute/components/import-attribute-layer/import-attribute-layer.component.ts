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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Feature, MapBrowserEvent } from 'ol';
import { FeatureLike } from 'ol/Feature';
import { Color } from 'ol/color';
import { Coordinate } from 'ol/coordinate';
import { GeoJSON } from 'ol/format';
import { GeoJSONFeature, GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Geometry, LineString, Point } from 'ol/geom';
import { Modify } from 'ol/interaction';
import { ModifyEvent } from 'ol/interaction/Modify';
import { Layer } from 'ol/layer';
import VectorImageLayer from 'ol/layer/VectorImage';
import { Source } from 'ol/source';
import VectorSource from 'ol/source/Vector';
import { Icon, Style } from 'ol/style';
import { StyleLike } from 'ol/style/Style';
import { Observable, Subject, Subscription } from 'rxjs';
import { concatAll } from 'rxjs/operators';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { DeleteMappedGrundnetzkanteCommand } from 'src/app/import/attribute/models/delete-mapped-grundnetzkante-command';
import { MappedGrundnetzkante } from 'src/app/import/attribute/models/mapped-grundnetzkante';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { LineStringOperations } from 'src/app/shared/models/line-string-operations';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

export enum FeatureTyp {
  IMPORTIERT_MIT_MAPPING = 'IMPORTIERT_MIT_MAPPING',
  IMPORTIERT_OHNE_MAPPING = 'IMPORTIERT_OHNE_MAPPING',
  MAPPED_GRUNDNETZKANTE = 'MAPPED_GRUNDNETZKANTE',
}

@Component({
  selector: 'rad-import-attribute-abschliessen-layer',
  template: '',
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportAttributeLayerComponent implements OnInit, OnDestroy {
  private static readonly GRUNDNETZ_COLOR = MapStyles.FEATURE_COLOR_LIGHTER;
  private static readonly MAPPED_GRUNDNETZKANTE_COLOR = MapStyles.FEATURE_COLOR;
  private static readonly IMPORTIERT_MIT_MAPPING_COLOR = MapStyles.FREMDNETZ_COLOR;
  private static readonly IMPORTIERT_OHNE_MAPPING_COLOR = MapStyles.FREMDNETZ_COLOR_LIGHTER;

  private static readonly HIDE_KNOTEN_RESOLUTION_THRESHOLD = 1;

  private static readonly FEATURE_TYP_KEY = 'featureTyp';
  private static readonly ASSOCIATED_KEY = 'associated';
  private static readonly MAPPED_GRUNDNETZKANTEN_KEY = 'mappedGrundnetzkanten';
  private static readonly FEATURE_MAPPING_ID_KEY = 'featureMappingId';
  private static readonly DISABLED_KEY = 'disabled';

  @Input()
  editable = false;

  @Input()
  radvisFeatures!: GeoJSONFeatureCollection;

  @Input()
  featureMappings!: GeoJSONFeatureCollection;

  @Output()
  loaded: EventEmitter<void> = new EventEmitter<void>();

  private radvisVectorSource: VectorSource = new VectorSource({ overlaps: false });
  private mappingsVectorSource: VectorSource = new VectorSource({ overlaps: false });
  private readonly modifySource: VectorSource = new VectorSource({ overlaps: false });

  private modifyInteraction: Modify | undefined;
  private readonly radvisLayer: VectorImageLayer;
  private readonly mappingsLayer: VectorImageLayer;
  private readonly modifyLayer: VectorImageLayer;

  private subscriptions: Subscription[] = [];
  private highlightedFeatureIds: string[] = [];

  // Serialisierung der Rematching-Requests
  private rematchingRequests$: Subject<Observable<GeoJSONFeature>> = new Subject();

  constructor(
    private olMapService: OlMapService,
    private attributeImportService: AttributeImportService
  ) {
    this.radvisLayer = new VectorImageLayer({
      source: this.radvisVectorSource,
      style: this.getStyleFunctionForRadvisAndModifyLayer(ImportAttributeLayerComponent.GRUNDNETZ_COLOR),
      zIndex: EditorLayerZindexConfig.MANUELLER_IMPORT_ATTRIBUTE_RADVIS_LAYER,
    });
    this.mappingsLayer = new VectorImageLayer({
      source: this.mappingsVectorSource,
      style: this.styleFunctionForMappingLayer,
      zIndex: EditorLayerZindexConfig.MANUELLER_IMPORT_ATTRIBUTE_MAPPINGS_LAYER,
    });
    this.modifyLayer = new VectorImageLayer({
      source: this.modifySource,
      style: this.getStyleFunctionForRadvisAndModifyLayer(MapStyles.FEATURE_SELECT_COLOR),
      zIndex: EditorLayerZindexConfig.MANUELLER_IMPORT_ATTRIBUTE_MODIFY_LAYER,
    });

    this.olMapService.addLayer(this.radvisLayer);
    this.olMapService.addLayer(this.mappingsLayer);
    this.olMapService.addLayer(this.modifyLayer);
    this.rematchingRequests$.pipe(concatAll()).subscribe(this.onModifySuccess);
  }

  private static getFilledEndpointsStyles(lineString: LineString, color: Color): Style[] {
    return [
      new Style({
        geometry: new Point(lineString.getFirstCoordinate()),
        image: MapStyles.circleWithFill(5, color),
      }),
      new Style({
        geometry: new Point(lineString.getLastCoordinate()),
        image: MapStyles.circleWithFill(5, color),
      }),
    ];
  }

  ngOnInit(): void {
    invariant(this.radvisFeatures);
    invariant(this.featureMappings);
    this.olMapService.onceOnPostRender(() => this.loaded.emit());
    this.radvisVectorSource.addFeatures(new GeoJSON().readFeatures(this.radvisFeatures));
    const featureMappingFeatures = new GeoJSON().readFeatures(this.featureMappings);
    featureMappingFeatures.forEach(featureMappingFeature => this.addFeatureMappingToLayer(featureMappingFeature));
    if (this.editable) {
      this.subscriptions.push(this.olMapService.click$().subscribe(clickEvent => this.onMapClick(clickEvent)));
    }
  }

  ngOnDestroy(): void {
    if (this.modifyInteraction) {
      this.olMapService.removeInteraction(this.modifyInteraction);
    }
    this.olMapService.removeLayer(this.radvisLayer);
    this.olMapService.removeLayer(this.mappingsLayer);
    this.olMapService.removeLayer(this.modifyLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  private addFeatureMappingToLayer(featureMapping: Feature<Geometry>, withAssociatedHighlighted?: boolean): void {
    const mappedGrundnetzkanten: MappedGrundnetzkante[] = featureMapping.get(
      ImportAttributeLayerComponent.MAPPED_GRUNDNETZKANTEN_KEY
    );
    if (!mappedGrundnetzkanten || mappedGrundnetzkanten.length === 0) {
      featureMapping.set(ImportAttributeLayerComponent.FEATURE_TYP_KEY, FeatureTyp.IMPORTIERT_OHNE_MAPPING);
    } else {
      featureMapping.set(ImportAttributeLayerComponent.FEATURE_TYP_KEY, FeatureTyp.IMPORTIERT_MIT_MAPPING);
      mappedGrundnetzkanten.forEach(mappedGrundnetzkante => {
        const mappedGrundnetzkanteFeature = this.buildFeatureFor(
          mappedGrundnetzkante,
          featureMapping.getId() as number
        );
        if (withAssociatedHighlighted) {
          mappedGrundnetzkanteFeature.set(ImportAttributeLayerComponent.ASSOCIATED_KEY, true);
          this.highlightedFeatureIds.push(mappedGrundnetzkanteFeature.getId() as string);
        }
        this.mappingsVectorSource.addFeature(mappedGrundnetzkanteFeature);
      });
    }
    this.mappingsVectorSource.addFeature(featureMapping);
  }

  private buildFeatureFor(mappedGrundnetzkante: MappedGrundnetzkante, featureMappingId: number): Feature<Geometry> {
    const radVisKantenFeature = this.radvisVectorSource.getFeatureById(mappedGrundnetzkante.kanteId);
    invariant(radVisKantenFeature, `Kante mit ID ${mappedGrundnetzkante.kanteId} nicht gefunden`);
    const subLineString = LineStringOperations.getSubLineString(
      radVisKantenFeature.getGeometry() as LineString,
      mappedGrundnetzkante.linearReferenzierterAbschnitt
    );
    const newFeature = new Feature<Geometry>(subLineString);
    newFeature.set(ImportAttributeLayerComponent.FEATURE_TYP_KEY, FeatureTyp.MAPPED_GRUNDNETZKANTE);
    newFeature.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, mappedGrundnetzkante.kanteId);
    newFeature.set(ImportAttributeLayerComponent.FEATURE_MAPPING_ID_KEY, featureMappingId);
    newFeature.setId(`${featureMappingId}-${mappedGrundnetzkante.kanteId}`);
    return newFeature;
  }

  private onMapClick(clickEvent: MapBrowserEvent<PointerEvent | KeyboardEvent | WheelEvent>): void {
    const linestringFeaturesAtPixel = this.olMapService
      .getFeaturesAtPixel(clickEvent.pixel, (layer: Layer<Source>) => layer === this.mappingsLayer)
      ?.filter(feat => feat.getGeometry()?.getType() === 'LineString');
    if (!linestringFeaturesAtPixel || linestringFeaturesAtPixel.length === 0) {
      this.removeHiglightAndDisableModify();
      return;
    }
    const pointerEvent = clickEvent.originalEvent as PointerEvent;
    const toggle = pointerEvent.ctrlKey || pointerEvent.metaKey;
    let clickedFeature: Feature<Geometry>;
    if (toggle) {
      clickedFeature = linestringFeaturesAtPixel.find(
        feature => feature.get(ImportAttributeLayerComponent.FEATURE_TYP_KEY) === FeatureTyp.MAPPED_GRUNDNETZKANTE
      ) as Feature<Geometry>;
    } else {
      clickedFeature = linestringFeaturesAtPixel.find(
        feature => feature.get(ImportAttributeLayerComponent.FEATURE_TYP_KEY) !== FeatureTyp.MAPPED_GRUNDNETZKANTE
      ) as Feature<Geometry>;
    }
    if (!clickedFeature || clickedFeature.get(ImportAttributeLayerComponent.DISABLED_KEY) === true) {
      this.removeHiglightAndDisableModify();
      return;
    }
    const selectedFeatureTyp: FeatureTyp = clickedFeature.get(ImportAttributeLayerComponent.FEATURE_TYP_KEY);
    if (selectedFeatureTyp === FeatureTyp.MAPPED_GRUNDNETZKANTE) {
      const overlappingMappedGrundnetzkanten = this.getOverlappingMappedGrundnetzkanten(
        clickedFeature,
        clickEvent.coordinate
      );
      this.deleteMappedGrundnetzkanten(overlappingMappedGrundnetzkanten);
    }
    if (selectedFeatureTyp !== FeatureTyp.MAPPED_GRUNDNETZKANTE) {
      this.highlightAndEnableModify(clickedFeature);
    }
  }

  private getOverlappingMappedGrundnetzkanten(
    clickedFeature: Feature<Geometry>,
    clickCoordinate: Coordinate
  ): Feature<Geometry>[] {
    const pointOnClickedFeature = (clickedFeature.getGeometry() as LineString).getClosestPoint(clickCoordinate);
    const featuresAtPoint = this.olMapService.getFeaturesAtCoordinate(
      pointOnClickedFeature,
      (layer: Layer<Source>) => layer === this.mappingsLayer,
      0
    );
    return featuresAtPoint!
      .filter(feat => feat.get(ImportAttributeLayerComponent.FEATURE_TYP_KEY) === FeatureTyp.MAPPED_GRUNDNETZKANTE)
      .map(feat => feat as Feature<Geometry>);
  }

  private deleteMappedGrundnetzkanten(overlappingMappedGrundnetzkanten: Feature<Geometry>[]): void {
    // Weitere Klicks auf die zu löschenden Features ignorieren, um Race Conditions zu verhindern
    overlappingMappedGrundnetzkanten.forEach(overlappingMappedGrundnetzkante =>
      overlappingMappedGrundnetzkante.set(ImportAttributeLayerComponent.DISABLED_KEY, true)
    );

    this.attributeImportService
      .deleteMappedGrundnetzkanten(
        overlappingMappedGrundnetzkanten.map(kante => ({
          featureMappingId: Number(kante.get(ImportAttributeLayerComponent.FEATURE_MAPPING_ID_KEY)),
          kanteId: Number(kante.get(FeatureProperties.KANTE_ID_PROPERTY_NAME)),
        })) as DeleteMappedGrundnetzkanteCommand[]
      )
      .then(modifiedFeatureMappings =>
        new GeoJSON().readFeatures(modifiedFeatureMappings).forEach(newFeatureMappingFeature => {
          this.deleteFeatureMappingFromLayer(newFeatureMappingFeature.getId() as number);
          this.addFeatureMappingToLayer(newFeatureMappingFeature);
        })
      )
      .catch(() =>
        overlappingMappedGrundnetzkanten.forEach(k => k.set(ImportAttributeLayerComponent.DISABLED_KEY, false))
      );
  }

  private deleteFeatureMappingFromLayer(featureMappingFeatureId: number): void {
    const featureMappingFeature = this.mappingsVectorSource.getFeatureById(featureMappingFeatureId);
    if (!featureMappingFeature) {
      return;
    }

    this.mappingsVectorSource.removeFeature(featureMappingFeature);
    const mappedGrundnetzkanten: MappedGrundnetzkante[] = featureMappingFeature.get(
      ImportAttributeLayerComponent.MAPPED_GRUNDNETZKANTEN_KEY
    );
    mappedGrundnetzkanten.forEach(mappedGrundnetzkante => {
      const mappedGrundnetzkanteId = `${featureMappingFeature.getId()}-${mappedGrundnetzkante.kanteId}`;
      const mappedGrundnetzkanteFeature = this.mappingsVectorSource.getFeatureById(mappedGrundnetzkanteId);
      if (mappedGrundnetzkanteFeature) {
        this.mappingsVectorSource.removeFeature(mappedGrundnetzkanteFeature);
      }
    });
  }

  private highlightAndEnableModify(clickedFeature: Feature<Geometry>): void {
    this.clearSelection();
    const selectedFeatureId = String(clickedFeature.getId());
    this.enableModify(clickedFeature);
    this.highlightedFeatureIds = [selectedFeatureId];
    const mappedGrundnetzkanten: MappedGrundnetzkante[] = clickedFeature.get(
      ImportAttributeLayerComponent.MAPPED_GRUNDNETZKANTEN_KEY
    );
    mappedGrundnetzkanten?.forEach(mappedGrundnetzkante => {
      const mappedGrundnetzkanteId = `${selectedFeatureId}-${mappedGrundnetzkante.kanteId}`;
      this.mappingsVectorSource
        .getFeatureById(mappedGrundnetzkanteId)
        ?.set(ImportAttributeLayerComponent.ASSOCIATED_KEY, true);
      this.highlightedFeatureIds.push(mappedGrundnetzkanteId);
    });
  }

  private removeHiglightAndDisableModify(): void {
    this.clearSelection();
    this.disableModify();
  }

  private disableModify(): void {
    if (this.modifyInteraction) {
      this.olMapService.removeInteraction(this.modifyInteraction);
    }
    if (this.modifySource.getFeatures().length > 0) {
      this.modifySource.clear();
    }
  }

  private enableModify(clickedFeature: Feature<Geometry>): void {
    this.disableModify();
    const modifyFeature = clickedFeature.clone();
    modifyFeature.setId(clickedFeature.getId());
    this.modifySource.addFeature(modifyFeature);
    this.modifyInteraction = new Modify({
      source: this.modifySource,
      style: MapStyles.getModifyPointStyle,
    });
    this.modifyInteraction.on('modifyend', this.onModifyEnd);
    this.olMapService.addInteraction(this.modifyInteraction);
  }

  private onModifyEnd = (event: ModifyEvent): void => {
    const modifiedFeature = event.features.item(0);
    const lineString = modifiedFeature.getGeometry() as LineString;
    const featuremappingID = Number(modifiedFeature.getId());
    const request$ = this.attributeImportService.updateMappedGrundnetzkanten({
      updatedLinestring: {
        coordinates: lineString.getCoordinates().map(coor => coor.slice(0, 2)),
        type: 'LineString',
      },
      featuremappingID,
    });
    this.rematchingRequests$.next(request$);
  };

  private onModifySuccess = (geojsonFeature: GeoJSONFeature): void => {
    const feature = new GeoJSON().readFeature(geojsonFeature);
    if (feature instanceof Feature) {
      this.deleteFeatureMappingFromLayer(feature.getId() as number);
      this.addFeatureMappingToLayer(feature, true);
    }
  };

  private clearSelection(): void {
    this.highlightedFeatureIds.forEach(id =>
      this.mappingsVectorSource.getFeatureById(id)?.set(ImportAttributeLayerComponent.ASSOCIATED_KEY, false)
    );
    this.highlightedFeatureIds = [];
  }

  private styleFunctionForMappingLayer = (feature: FeatureLike, resolution: number): Style | Style[] => {
    let lineStringcolor: Color;
    let pointColor: Color;
    let zIndex = EditorLayerZindexConfig.MANUELLER_IMPORT_ATTRIBUTE_MAPPINGS_LAYER;
    const styles: Style[] = [];
    const featureTyp: FeatureTyp = feature.get(ImportAttributeLayerComponent.FEATURE_TYP_KEY);
    if (featureTyp === FeatureTyp.IMPORTIERT_OHNE_MAPPING) {
      const style = new Style({
        geometry: new Point((feature.getGeometry() as LineString).getCoordinateAt(0.5)),
        image: new Icon({
          anchor: [0.5, 0.5],
          src: './assets/noMapping.svg',
        }),
      });
      style.setZIndex(EditorLayerZindexConfig.MANUELLER_IMPORT_ATTRIBUTE_MAPPINGS_LAYER + 2);
      styles.push(style);
      lineStringcolor = ImportAttributeLayerComponent.IMPORTIERT_OHNE_MAPPING_COLOR;
      pointColor = ImportAttributeLayerComponent.IMPORTIERT_MIT_MAPPING_COLOR;
      zIndex = EditorLayerZindexConfig.MANUELLER_IMPORT_ATTRIBUTE_MAPPINGS_LAYER + 1;
    } else if (featureTyp === FeatureTyp.IMPORTIERT_MIT_MAPPING) {
      lineStringcolor = ImportAttributeLayerComponent.IMPORTIERT_MIT_MAPPING_COLOR;
      pointColor = ImportAttributeLayerComponent.IMPORTIERT_MIT_MAPPING_COLOR;
      zIndex = EditorLayerZindexConfig.MANUELLER_IMPORT_ATTRIBUTE_MAPPINGS_LAYER + 1;
    } else if (featureTyp === FeatureTyp.MAPPED_GRUNDNETZKANTE) {
      lineStringcolor = ImportAttributeLayerComponent.MAPPED_GRUNDNETZKANTE_COLOR;
      pointColor = ImportAttributeLayerComponent.MAPPED_GRUNDNETZKANTE_COLOR;
    } else {
      throw new Error(`Unbekannter FeatureTyp: ${featureTyp}`);
    }
    if (feature.get(ImportAttributeLayerComponent.ASSOCIATED_KEY) === true) {
      lineStringcolor = MapStyles.FEATURE_HOVER_COLOR;
      pointColor = MapStyles.FEATURE_HOVER_COLOR;
      zIndex = EditorLayerZindexConfig.MANUELLER_IMPORT_ATTRIBUTE_MAPPINGS_LAYER + 1;
    }
    if (resolution < ImportAttributeLayerComponent.HIDE_KNOTEN_RESOLUTION_THRESHOLD) {
      const endpointStyles = ImportAttributeLayerComponent.getFilledEndpointsStyles(
        feature.getGeometry() as LineString,
        pointColor
      );
      endpointStyles.forEach(style => style.setZIndex(zIndex));
      styles.push(...endpointStyles);
    }
    const defaultStyle = MapStyles.getDefaultNetzStyleFunction(lineStringcolor)(feature, resolution);
    defaultStyle.setZIndex(zIndex);
    styles.push(defaultStyle);
    return styles;
  };

  private getStyleFunctionForRadvisAndModifyLayer(color: Color): StyleLike {
    return (feature: FeatureLike, resolution: number): Style | Style[] => {
      const styles = [MapStyles.getDefaultNetzStyleFunction(color)(feature, resolution)];
      return resolution < ImportAttributeLayerComponent.HIDE_KNOTEN_RESOLUTION_THRESHOLD
        ? [
            ...styles,
            ...ImportAttributeLayerComponent.getFilledEndpointsStyles(feature.getGeometry() as LineString, color),
          ]
        : styles;
    };
  }
}
