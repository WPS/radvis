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

import { DefaultRenderComponent, MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { Feature } from 'ol';
import { Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { NetzDetailsModule } from 'src/app/viewer/netz-details/netz-details.module';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import { anything, capture, instance, mock, reset, verify } from 'ts-mockito';
import { KnotenHighlightLayerComponent } from './knoten-highlight-layer.component';

describe(KnotenHighlightLayerComponent.name, () => {
  let component: DefaultRenderComponent<KnotenHighlightLayerComponent>;
  let fixture: MockedComponentFixture<KnotenHighlightLayerComponent>;
  let olMapService: OlMapService;
  let netzAusblendenService: NetzAusblendenService;
  let layer: VectorLayer;

  const initialKnotenId = 1;
  const initialGeometrie: PointGeojson = { coordinates: [0, 0], type: 'Point' };

  beforeEach(async () => {
    olMapService = mock(OlMapComponent);
    netzAusblendenService = mock(NetzAusblendenService);
    return MockBuilder(KnotenHighlightLayerComponent, NetzDetailsModule)
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: NetzAusblendenService,
        useValue: instance(netzAusblendenService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(KnotenHighlightLayerComponent, {
      geometrie: initialGeometrie,
      id: initialKnotenId,
    } as any);
    component = fixture.componentInstance;
    fixture.detectChanges();
    layer = capture(olMapService.addLayer).last()[0] as VectorLayer;
  });

  it('should initialize correct', () => {
    verify(netzAusblendenService.knotenAusblenden(anything())).once();
    expect(capture(netzAusblendenService.knotenAusblenden).last()[0]).toEqual(initialKnotenId);
    expect(layer.getSource().getFeatures()).toHaveSize(1);
    expect((layer.getSource().getFeatures()[0] as Feature<Point>).getGeometry()?.getCoordinates()).toEqual(
      initialGeometrie.coordinates
    );
  });

  it('should einblenden on change', () => {
    reset(netzAusblendenService);
    const neueGeometrie: PointGeojson = { coordinates: [1, 1], type: 'Point' };
    const neueId = 2;
    component.geometrie = neueGeometrie;
    component.id = neueId;
    fixture.detectChanges();
    verify(netzAusblendenService.knotenEinblenden(anything())).once();
    expect(capture(netzAusblendenService.knotenEinblenden).last()[0]).toEqual(initialKnotenId);

    verify(netzAusblendenService.knotenAusblenden(anything())).once();
    expect(capture(netzAusblendenService.knotenAusblenden).last()[0]).toEqual(neueId);
    expect(layer.getSource().getFeatures()).toHaveSize(1);
    expect((layer.getSource().getFeatures()[0] as Feature<Point>).getGeometry()?.getCoordinates()).toEqual(
      neueGeometrie.coordinates
    );
  });
});
