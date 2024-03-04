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
import { fakeAsync, tick } from '@angular/core/testing';
import { MapBrowserEvent } from 'ol';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { BehaviorSubject } from 'rxjs';
import { ImportAttributeKonflikteLayerComponent } from 'src/app/import/attribute/components/import-attribute-konflikte-layer/import-attribute-konflikte-layer.component';
import { Property } from 'src/app/import/attribute/models/property';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { anything, instance, mock, when } from 'ts-mockito';

describe(ImportAttributeKonflikteLayerComponent.name, () => {
  let component: ImportAttributeKonflikteLayerComponent;
  let olMapService: OlMapService;
  let attributeImportService: AttributeImportService;
  const clickSubject: BehaviorSubject<MapBrowserEvent<UIEvent>> = new BehaviorSubject<MapBrowserEvent<UIEvent>>(
    ({} as unknown) as MapBrowserEvent<UIEvent>
  );

  beforeEach(fakeAsync(() => {
    olMapService = mock(OlMapComponent);
    attributeImportService = mock(AttributeImportService);
    when(olMapService.click$()).thenReturn(clickSubject);
    when(attributeImportService.getKonfliktprotokolle()).thenReturn(
      Promise.resolve(createDummyKonflikteFeatureCollection())
    );
    component = new ImportAttributeKonflikteLayerComponent(instance(olMapService), instance(attributeImportService));
    tick();
  }));

  describe('shouldLoadFeatures', () => {
    it('should add all features after loading to vectorSource', fakeAsync(() => {
      component = new ImportAttributeKonflikteLayerComponent(instance(olMapService), instance(attributeImportService));
      tick();
      expect(component['konflikteVectorSource'].getFeatures().length).toBe(2);
      expect(component['konflikteVectorSource'].getFeatures()[0].getId()).toBe('1');
      expect(component['konflikteVectorSource'].getFeatures()[1].getId()).toBe('2');
    }));
  });
  describe('clickSubscription', () => {
    it('should output all conflicts of clicked kante as Array of Array of Entries', () => {
      const feature = component['konflikteVectorSource'].getFeatures()[0];
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([feature]);
      const konflikteSpy = spyOn(component.selectKonflikt, 'emit');

      clickSubject.next(({
        pixel: [0, 0],
        originalEvent: { ctrlKey: false, metaKey: false } as PointerEvent,
      } as unknown) as MapBrowserEvent<UIEvent>);
      expect(konflikteSpy).toHaveBeenCalledWith([
        [
          { key: 'Betroffener Abschnitt', value: '10m bis 30,45m' } as Property,
          { key: 'Attributname', value: 'vereinbaru' } as Property,
          { key: 'Übernommener Wert', value: 'sowasvon vereinbart' } as Property,
          { key: 'Nicht Übernommene Werte', value: 'nicht so vereinbart, nicht so mega vereinbart' } as Property,
        ],
        [
          { key: 'Betroffener Abschnitt', value: '' } as Property,
          { key: 'Attributname', value: 'Belagart' } as Property,
          { key: 'Übernommener Wert', value: 'Zu viel Belag auf den Zähnen' } as Property,
          { key: 'Nicht Übernommene Werte', value: 'Besser Zähne putzen!' } as Property,
        ],
      ]);
    });
  });
});

const createDummyKonflikteFeatureCollection = (): GeoJSONFeatureCollection => {
  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: [
            [1, 1],
            [2, 2],
          ],
        },
        id: '1',
        properties: {
          konflikte: [
            {
              'Betroffener Abschnitt': '10m bis 30,45m',
              Attributname: 'vereinbaru',
              'Übernommener Wert': 'sowasvon vereinbart',
              'Nicht Übernommene Werte': 'nicht so vereinbart, nicht so mega vereinbart',
            },
            {
              'Betroffener Abschnitt': '',
              Attributname: 'Belagart',
              'Übernommener Wert': 'Zu viel Belag auf den Zähnen',
              'Nicht Übernommene Werte': 'Besser Zähne putzen!',
            },
          ],
        },
      },
      {
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: [
            [10, 10],
            [20, 20],
          ],
        },
        id: '2',
        properties: {
          konflikte: [
            {
              'Betroffener Abschnitt': '0m bis 50,30m',
              Attributname: 'grumpiness',
              'Übernommener Wert': 'Arin',
              'Nicht Übernommene Werte': 'Dan',
            },
          ],
        },
      },
    ],
  } as GeoJSONFeatureCollection;
};
