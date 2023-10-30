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

/* eslint-disable @typescript-eslint/dot-notation */
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { LineString, MultiLineString } from 'ol/geom';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FahrradrouteNetzbezugHighlightLayerComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-netzbezug-highlight-layer/fahrradroute-netzbezug-highlight-layer.component';
import { FahrradrouteNetzbezug } from 'src/app/viewer/fahrradroute/models/fahrradroute.netzbezug';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { KantenNetzbezug } from 'src/app/viewer/viewer-shared/models/kanten-netzbezug';
import { defaultKantenbezug } from 'src/app/viewer/viewer-shared/models/kantennetzbezug-test-data-provider.spec';
import { NetzAusblendenService } from 'src/app/viewer/viewer-shared/services/netz-ausblenden.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { instance, mock } from 'ts-mockito';

describe(FahrradrouteNetzbezugHighlightLayerComponent.name, () => {
  let fixture: MockedComponentFixture<
    FahrradrouteNetzbezugHighlightLayerComponent,
    { layerId: string; kantenBezug: KantenNetzbezug[]; fahrradRouteNetzbezug: FahrradrouteNetzbezug }
  >;
  let component: FahrradrouteNetzbezugHighlightLayerComponent;
  let netzausblendenService: NetzAusblendenService;
  let inputs: any;

  beforeEach(() => {
    netzausblendenService = new NetzAusblendenService();
    return MockBuilder(FahrradrouteNetzbezugHighlightLayerComponent, ViewerModule)
      .provide({
        provide: NetzAusblendenService,
        useValue: netzausblendenService,
      })
      .provide({
        provide: OlMapService,
        useValue: instance(mock(OlMapComponent)),
      });
  });

  beforeEach(() => {
    inputs = {
      kantenBezug: defaultKantenbezug,
      layerId: MASSNAHMEN.name,
      fahrradRouteNetzbezug: (undefined as unknown) as FahrradrouteNetzbezug,
    };
    fixture = MockRender(FahrradrouteNetzbezugHighlightLayerComponent, inputs, { detectChanges: false });

    component = fixture.point.componentInstance;
    // KEIN detectChanges, damit wir unterschiedliche initale Inputs testen können
  });

  describe('ngOnChanges', () => {
    describe('when fahrradRouteNetzbezug change', () => {
      it('should work for undefined -> value', () => {
        fixture.detectChanges();
        const fahrradrouteNetzbezug = {
          geometrie: {
            coordinates: [
              [0, 0],
              [100, 100],
            ],
            type: 'LineString',
          },
          kantenIDs: [2, 3],
          stuetzpunkte: [
            [0, 0],
            [99, 99],
          ],
        };
        inputs.fahrradRouteNetzbezug = fahrradrouteNetzbezug;
        fixture.detectChanges();

        expect(component['vectorSource'].getFeatures()).toHaveSize(1);
        expect((component['vectorSource'].getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual(
          fahrradrouteNetzbezug.geometrie.coordinates
        );
      });

      it('should work for value1 -> value2', () => {
        const fahrradrouteNetzbezug1 = {
          geometrie: {
            coordinates: [
              [0, 0],
              [100, 100],
            ],
            type: 'LineString',
          },
          kantenIDs: [2, 3],
          stuetzpunkte: [
            [0, 0],
            [99, 99],
          ],
        };
        inputs.fahrradRouteNetzbezug = fahrradrouteNetzbezug1;
        fixture.detectChanges();

        expect(component['vectorSource'].getFeatures()).toHaveSize(1);
        expect((component['vectorSource'].getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual(
          fahrradrouteNetzbezug1.geometrie.coordinates
        );

        const fahrradrouteNetzbezug2 = {
          geometrie: {
            coordinates: [
              [0, 0],
              [100, 100],
            ],
            type: 'LineString',
          },
          kantenIDs: [2, 3],
          stuetzpunkte: [
            [0, 0],
            [99, 99],
          ],
        };
        inputs.fahrradRouteNetzbezug = fahrradrouteNetzbezug2;
        fixture.detectChanges();

        expect(component['vectorSource'].getFeatures()).toHaveSize(1);
        expect((component['vectorSource'].getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual(
          fahrradrouteNetzbezug2.geometrie.coordinates
        );
      });

      it('should work for value -> undefined', () => {
        const fahrradrouteNetzbezug1 = {
          geometrie: {
            coordinates: [
              [0, 0],
              [100, 100],
            ],
            type: 'LineString',
          },
          kantenIDs: [2, 3],
          stuetzpunkte: [
            [0, 0],
            [99, 99],
          ],
        };
        inputs.fahrradRouteNetzbezug = fahrradrouteNetzbezug1;
        fixture.detectChanges();

        expect(component['vectorSource'].getFeatures()).toHaveSize(1);
        expect((component['vectorSource'].getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual(
          fahrradrouteNetzbezug1.geometrie.coordinates
        );

        inputs.fahrradRouteNetzbezug = undefined;
        fixture.detectChanges();

        expect(component['vectorSource'].getFeatures()).toHaveSize(1);
        expect((component['vectorSource'].getFeatures()[0].getGeometry() as MultiLineString).getCoordinates()).toEqual(
          defaultKantenbezug.map(k => k.geometrie.coordinates)
        );
      });
    });

    it('should work with initial value', () => {
      const fahrradrouteNetzbezug = {
        geometrie: {
          coordinates: [
            [0, 0],
            [100, 100],
          ],
          type: 'LineString',
        },
        kantenIDs: [2, 3],
        stuetzpunkte: [
          [0, 0],
          [99, 99],
        ],
      };
      inputs.fahrradRouteNetzbezug = fahrradrouteNetzbezug;
      fixture.detectChanges();

      expect(component['vectorSource'].getFeatures()).toHaveSize(1);
      expect((component['vectorSource'].getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual(
        fahrradrouteNetzbezug.geometrie.coordinates
      );
    });
  });
});
