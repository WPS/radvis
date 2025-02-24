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
import { FeatureLike } from 'ol/Feature';
import { Color, fromString } from 'ol/color';
import { getCenter } from 'ol/extent';
import GeoJSON from 'ol/format/GeoJSON';
import { GeometryCollection, LineString, MultiLineString, MultiPoint, Point } from 'ol/geom';
import Geometry from 'ol/geom/Geometry';
import GeometryType from 'ol/geom/GeometryType';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import Stroke from 'ol/style/Stroke';
import { Subscription } from 'rxjs';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { MassnahmenImportZuordnungStatus } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-status';
import { MassnahmenImportZuordnungenService } from 'src/app/import/massnahmen/services/massnahmen-import-zuordnungen.service';
import { geojsonGeometryToFeature } from 'src/app/shared/models/geojson-geometrie';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-import-massnahmen-import-ueberpruefen-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportMassnahmenImportUeberpruefenLayerComponent implements OnDestroy {
  static readonly featureIdField = 'id';
  static readonly featureStatusField = 'status';
  static readonly featureSelectedField = 'selected';

  private readonly layerId = 'massnahmen-import-zuordnungen-layer';
  private readonly vectorSource: VectorSource<Geometry>;
  private readonly vectorLayer: VectorLayer;

  private readonly subscriptions: Subscription[] = [];

  constructor(
    private olMapService: OlMapService,
    private massnahmenImportZuordnungenService: MassnahmenImportZuordnungenService
  ) {
    this.vectorSource = new VectorSource();

    this.vectorLayer = this.createLayer();
    this.olMapService.addLayer(this.vectorLayer);

    this.subscriptions.push(
      massnahmenImportZuordnungenService.zuordnungen$.subscribe(zuordnungen => {
        this.vectorSource.clear();

        zuordnungen.forEach(zuordnung => {
          if (!!zuordnung.netzbezugGeometrie) {
            const netzbezugFeature = geojsonGeometryToFeature(zuordnung.netzbezugGeometrie);
            if (netzbezugFeature != null) {
              netzbezugFeature.set(ImportMassnahmenImportUeberpruefenLayerComponent.featureIdField, zuordnung.id);
              netzbezugFeature.set(
                ImportMassnahmenImportUeberpruefenLayerComponent.featureStatusField,
                zuordnung.status
              );
              this.vectorSource.addFeature(netzbezugFeature);
            }
          }

          this.updateSelectedFeatureFields(massnahmenImportZuordnungenService.selektierteZuordnungsId);
        });
      }),
      massnahmenImportZuordnungenService.selektierteZuordnungsId$.subscribe(selectedZuordnungsId => {
        this.focusMassnahmeIntoView();
        this.updateSelectedFeatureFields(selectedZuordnungsId);
      }),
      this.olMapService.locationSelected$().subscribe(locationSelectEvent => {
        if (
          locationSelectEvent.selectedFeatures.length > 0 &&
          locationSelectEvent.selectedFeatures[0].layer === this.layerId
        ) {
          const featureZuordnungId = locationSelectEvent.selectedFeatures[0].attributes.get(
            ImportMassnahmenImportUeberpruefenLayerComponent.featureIdField
          );
          const zuordnung = this.massnahmenImportZuordnungenService.zuordnungen.find(z => z.id === featureZuordnungId);
          if (zuordnung) {
            this.massnahmenImportZuordnungenService.selektiereZuordnung(zuordnung.id);
          }
        } else {
          this.massnahmenImportZuordnungenService.deselektiereZuordnung();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
    this.olMapService.removeLayer(this.vectorLayer);
  }

  private createLayer(): VectorLayer {
    const olLayer = new VectorLayer({
      source: this.vectorSource,
      style: this.styleFn,
      zIndex: EditorLayerZindexConfig.MANUELLER_IMPORT_MASSNAHMEN_ZUORDNUNG,
    });
    olLayer.set(OlMapService.LAYER_ID, this.layerId);
    return olLayer;
  }

  private updateSelectedFeatureFields(selectedZuordnungsId: number | undefined): void {
    this.vectorSource.getFeatures().forEach(feature => {
      if (
        !!selectedZuordnungsId &&
        selectedZuordnungsId === feature.get(ImportMassnahmenImportUeberpruefenLayerComponent.featureIdField)
      ) {
        feature.set(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField, true);
      } else {
        feature.set(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField, false);
      }
    });
  }

  private styleFn(feature: FeatureLike, resolution: number): Style | Style[] {
    const geometry = feature.getGeometry() as Geometry;
    if (!(feature instanceof Feature) || !geometry) {
      return [];
    }

    let color: Color | undefined = undefined;
    let iconFile = 'icon-massnahmen.svg';
    switch (feature.get(ImportMassnahmenImportUeberpruefenLayerComponent.featureStatusField)) {
      case MassnahmenImportZuordnungStatus.NEU:
        iconFile = 'icon-massnahme-zuordnung-karte-add.svg';
        color = fromString('#4aba09');
        break;
      case MassnahmenImportZuordnungStatus.ZUGEORDNET:
      case MassnahmenImportZuordnungStatus.FEHLERHAFT:
        iconFile = 'icon-massnahme-zuordnung-karte-edit.svg';
        color = fromString('#80aed5');
        break;
      case MassnahmenImportZuordnungStatus.GELOESCHT:
        iconFile = 'icon-massnahme-zuordnung-karte-delete.svg';
        color = fromString('#ac005e');
        break;
    }

    const highlighted = feature.get(ImportMassnahmenImportUeberpruefenLayerComponent.featureSelectedField);
    if (highlighted) {
      color = MapStyles.INFRASTRUKTUR_ICON_HIGHLIGHT_COLOR;
    }

    // Den zentralen Punkt für das Icon ermitteln
    const iconStyles: Style[] = [];
    const iconBaseGeometries: Geometry[] = unwrapGeometries(geometry);
    let iconPoint: Geometry | undefined = undefined;

    const lineStrings = iconBaseGeometries.filter(geom => geom.getType() == GeometryType.LINE_STRING);
    if (lineStrings.length > 0) {
      iconPoint = new Point((lineStrings[0] as LineString).getCoordinateAt(0.5));
    } else {
      const points = iconBaseGeometries.filter(geom => geom.getType() == GeometryType.POINT);
      if (points.length > 0) {
        iconPoint = points[0] as Point;
      }
    }

    if (!iconPoint) {
      return [];
    }

    const newIconStyles = MapStyles.getInfrastrukturIconStyle(iconFile, highlighted, color);
    newIconStyles.forEach(style => style.setGeometry(iconPoint));
    iconStyles.push(...newIconStyles);

    return [
      // Icon Styles
      ...iconStyles,
      // Line & Point Style
      new Style({
        stroke: new Stroke({
          width: MapStyles.LINE_WIDTH_MEDIUM,
          color: color,
        }),
        image: MapStyles.circle(MapStyles.LINE_WIDTH_THICK, color),
      }),
    ];
  }

  public focusMassnahmeIntoView(): void {
    const originalGeometrie = this.massnahmenImportZuordnungenService.selektierteZuordnungsOriginalGeometrie;
    if (originalGeometrie) {
      const toFocus = new GeoJSON().readFeature(originalGeometrie).getGeometry()?.getExtent();

      if (toFocus) {
        this.olMapService.scrollIntoViewByCoordinate(getCenter(toFocus));
      }
    }
  }
}

export const unwrapGeometries = (geometry: Geometry): Geometry[] => {
  if (geometry.getType() == GeometryType.GEOMETRY_COLLECTION) {
    return (geometry as GeometryCollection).getGeometries().flatMap(geom => unwrapGeometries(geom));
  } else if (geometry.getType() == GeometryType.MULTI_LINE_STRING) {
    return (geometry as MultiLineString).getLineStrings().flatMap(geom => unwrapGeometries(geom));
  } else if (geometry.getType() == GeometryType.MULTI_POINT) {
    return (geometry as MultiPoint).getPoints().flatMap(geom => unwrapGeometries(geom));
  }

  return [geometry];
};
