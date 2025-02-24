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

import { KanteGrundgeometrieLayerComponent } from 'src/app/editor/editor-shared/components/kante-grundgeometrie-layer/kante-grundgeometrie-layer.component';
import { anything, instance, mock, when } from 'ts-mockito';
import { Feature, MapBrowserEvent } from 'ol';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { LineString } from 'ol/geom';

describe(KanteGrundgeometrieLayerComponent.name, () => {
  let olMapService: OlMapService;
  let component: KanteGrundgeometrieLayerComponent;

  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    component = new KanteGrundgeometrieLayerComponent(instance(olMapService));
  });

  describe('onMapClick', () => {
    let spyOnDeselected: jasmine.Spy<any>;
    let feature: Feature<LineString>;

    beforeEach(() => {
      feature = new Feature(
        new LineString([
          [23, 77],
          [34, 66],
        ])
      );
      component['olLayer'].getSource().addFeature(feature);
      spyOnDeselected = spyOn(component['deselected'], 'emit');
    });

    it('should do nothing when no features are under cursor', () => {
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([]);

      component['onMapClick']({ pixel: [0, 0] } as MapBrowserEvent<UIEvent>);

      expect(spyOnDeselected).not.toHaveBeenCalled();
    });

    it('should fire deselected event when features is under cursor', () => {
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([feature]);

      component['onMapClick']({ pixel: [0, 0] } as MapBrowserEvent<UIEvent>);

      expect(spyOnDeselected).toHaveBeenCalled();
    });
  });
});
