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

import { MockBuilder, MockRender } from 'ng-mocks';
import { LineString, Point } from 'ol/geom';
import GeometryType from 'ol/geom/GeometryType';
import VectorLayer from 'ol/layer/Vector';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { GeometryCollectionGeojson, LineStringGeojson, PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { MassnahmeModule } from 'src/app/viewer/massnahme/massnahme.module';
import { anything, capture, instance, mock, verify } from 'ts-mockito';
import { MassnahmeNetzbezugSnapshotLayerComponent } from './massnahme-netzbezug-snapshot-layer.component';

describe(MassnahmeNetzbezugSnapshotLayerComponent.name, () => {
  let olMapService: OlMapService;
  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    return MockBuilder(MassnahmeNetzbezugSnapshotLayerComponent, MassnahmeModule).provide({
      provide: OlMapService,
      useValue: instance(olMapService),
    });
  });

  it('should add and remove layer', () => {
    const defaultGeometrie: GeometryCollectionGeojson = {
      type: 'GeometryCollection',
      geometries: [],
    };
    const fixture = MockRender<MassnahmeNetzbezugSnapshotLayerComponent>(MassnahmeNetzbezugSnapshotLayerComponent, {
      geometrie: defaultGeometrie,
    });

    verify(olMapService.addLayer(anything())).once();

    fixture.destroy();

    verify(olMapService.removeLayer(anything())).once();
    expect(capture(olMapService.removeLayer).last()[0]).toBe(capture(olMapService.addLayer).last()[0]);
  });

  it('should add features onInit', () => {
    const defaultGeometrie: GeometryCollectionGeojson = {
      type: 'GeometryCollection',
      geometries: [
        { type: 'Point', coordinates: [0, 0] } as PointGeojson,
        {
          type: 'LineString',
          coordinates: [
            [100, 100],
            [200, 200],
          ],
        } as LineStringGeojson,
      ],
    };
    const fixture = MockRender<MassnahmeNetzbezugSnapshotLayerComponent>(MassnahmeNetzbezugSnapshotLayerComponent, {
      geometrie: defaultGeometrie,
    });
    fixture.detectChanges();
    const layer = capture(olMapService.addLayer).last()[0];
    const features = (layer as VectorLayer).getSource().getFeatures();

    expect(features.length).toBe(2);
    expect(features[0].getGeometry()?.getType()).toBe(GeometryType.POINT);
    expect((features[0].getGeometry() as Point).getCoordinates()).toEqual([0, 0]);
    expect(features[1].getGeometry()?.getType()).toBe(GeometryType.LINE_STRING);
    expect((features[1].getGeometry() as LineString).getCoordinates()).toEqual([
      [100, 100],
      [200, 200],
    ]);
  });

  it('should change features onChanges', () => {
    const defaultGeometrie: GeometryCollectionGeojson = {
      type: 'GeometryCollection',
      geometries: [{ type: 'Point', coordinates: [800, 800] } as PointGeojson],
    };
    const fixture = MockRender<MassnahmeNetzbezugSnapshotLayerComponent>(MassnahmeNetzbezugSnapshotLayerComponent, {
      geometrie: defaultGeometrie,
    });
    fixture.detectChanges();
    const changedGeometry: GeometryCollectionGeojson = {
      type: 'GeometryCollection',
      geometries: [
        { type: 'Point', coordinates: [0, 0] } as PointGeojson,
        {
          type: 'LineString',
          coordinates: [
            [100, 100],
            [200, 200],
          ],
        } as LineStringGeojson,
      ],
    };
    fixture.componentInstance.geometrie = changedGeometry;
    fixture.detectChanges();
    const layer = capture(olMapService.addLayer).last()[0];
    const features = (layer as VectorLayer).getSource().getFeatures();

    expect(features.length).toBe(2);
    expect(features[0].getGeometry()?.getType()).toBe(GeometryType.POINT);
    expect((features[0].getGeometry() as Point).getCoordinates()).toEqual([0, 0]);
    expect(features[1].getGeometry()?.getType()).toBe(GeometryType.LINE_STRING);
    expect((features[1].getGeometry() as LineString).getCoordinates()).toEqual([
      [100, 100],
      [200, 200],
    ]);
  });
});
