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
import { fakeAsync, tick } from '@angular/core/testing';
import { MockBuilder, MockRender } from 'ng-mocks';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { KantenModule } from 'src/app/editor/kanten/kanten.module';
import { defaultKante } from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { KantenAttributeCopiedLayerComponent } from './kanten-attribute-copied-layer.component';

describe(KantenAttributeCopiedLayerComponent.name, () => {
  let netzausblendenService: NetzAusblendenService;
  let netzService: NetzService;
  let olMapService: OlMapService;
  beforeEach(() => {
    netzausblendenService = mock(NetzAusblendenService);
    netzService = mock(NetzService);
    olMapService = mock(OlMapComponent);
    return MockBuilder(KantenAttributeCopiedLayerComponent, KantenModule)
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: NetzService,
        useValue: instance(netzService),
      })
      .provide({
        provide: NetzAusblendenService,
        useValue: instance(netzausblendenService),
      });
  });

  it('should kante aus/einblenden - onDestroy', fakeAsync(() => {
    when(netzService.getKanteForEdit(anything())).thenResolve(defaultKante);
    const fixture = MockRender<KantenAttributeCopiedLayerComponent>(KantenAttributeCopiedLayerComponent, {
      copiedKante: 1,
    });
    tick();

    verify(netzausblendenService.kanteAusblenden(anything())).once();
    expect(capture(netzausblendenService.kanteAusblenden).last()[0]).toBe(1);

    fixture.destroy();

    verify(netzausblendenService.kanteEinblenden(anything())).once();
    expect(capture(netzausblendenService.kanteEinblenden).last()[0]).toBe(1);
  }));

  it('should kante aus/einblenden - onChanges', fakeAsync(() => {
    when(netzService.getKanteForEdit(anything())).thenResolve(defaultKante);
    const fixture = MockRender<KantenAttributeCopiedLayerComponent>(KantenAttributeCopiedLayerComponent, {
      copiedKante: 1,
    });
    tick();

    verify(netzausblendenService.kanteAusblenden(anything())).once();
    expect(capture(netzausblendenService.kanteAusblenden).last()[0]).toBe(1);

    fixture.componentInstance.copiedKante = 2;
    fixture.detectChanges();
    tick();

    verify(netzausblendenService.kanteEinblenden(anything())).once();
    expect(capture(netzausblendenService.kanteEinblenden).last()[0]).toBe(1);

    verify(netzausblendenService.kanteAusblenden(anything())).twice();
    expect(capture(netzausblendenService.kanteAusblenden).last()[0]).toBe(2);
  }));

  it('should add and remove layer', () => {
    when(netzService.getKanteForEdit(anything())).thenResolve(defaultKante);
    const fixture = MockRender<KantenAttributeCopiedLayerComponent>(KantenAttributeCopiedLayerComponent, {
      copiedKante: 1,
    });

    verify(olMapService.addLayer(anything())).twice();
    const layer1 = capture(olMapService.addLayer).last()[0];
    const layer2 = capture(olMapService.addLayer).beforeLast()[0];

    fixture.destroy();

    verify(olMapService.removeLayer(anything())).twice();

    verify(olMapService.removeLayer(layer1)).once();
    verify(olMapService.removeLayer(layer2)).once();

    expect().nothing();
  });

  it('should fill layers', fakeAsync(() => {
    const coordinates = [
      [0, 0],
      [100, 100],
    ];
    when(netzService.getKanteForEdit(anything())).thenResolve({
      ...defaultKante,
      geometry: {
        coordinates,
        type: 'LineString',
      },
    });
    const fixture = MockRender<KantenAttributeCopiedLayerComponent>(KantenAttributeCopiedLayerComponent, {
      copiedKante: 1,
    });
    tick();

    expect(
      fixture.point.componentInstance['kanteVectorSource'].getFeatures()[0].getGeometry()?.getCoordinates()
    ).toEqual(coordinates);
    expect(
      fixture.point.componentInstance['symbolVectorSource'].getFeatures()[0].getGeometry()?.getCoordinates()
    ).toEqual([50, 50]);

    const coordinates2 = [
      [0, 0],
      [0, 100],
    ];
    when(netzService.getKanteForEdit(anything())).thenResolve({
      ...defaultKante,
      geometry: {
        coordinates: coordinates2,
        type: 'LineString',
      },
    });
    fixture.componentInstance.copiedKante = 2;
    fixture.detectChanges();
    tick();

    expect(
      fixture.point.componentInstance['kanteVectorSource'].getFeatures()[0].getGeometry()?.getCoordinates()
    ).toEqual(coordinates2);
    expect(
      fixture.point.componentInstance['symbolVectorSource'].getFeatures()[0].getGeometry()?.getCoordinates()
    ).toEqual([0, 50]);
  }));
});
