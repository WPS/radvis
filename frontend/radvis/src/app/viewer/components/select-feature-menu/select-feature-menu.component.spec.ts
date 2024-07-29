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

import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { MockBuilder, MockRender } from 'ng-mocks';
import Point from 'ol/geom/Point';
import { NEVER } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { LocationSelectEvent } from 'src/app/shared/models/location-select-event';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { SelectFeatureMenuComponent } from 'src/app/viewer/components/select-feature-menu/select-feature-menu.component';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { LayerRegistryService } from 'src/app/viewer/services/layer-registry.service';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { toRadVisFeatureAttributesFromMap } from 'src/app/shared/models/rad-vis-feature-attributes';

describe(SelectFeatureMenuComponent.name, () => {
  let component: SelectFeatureMenuComponent;
  let fixture: ComponentFixture<SelectFeatureMenuComponent>;

  let layerRegistryService: LayerRegistryService;
  let featureHighlightService: FeatureHighlightService;
  let changeDetector: ChangeDetectorRef;
  let notifyUserService: NotifyUserService;
  let olMapService: OlMapService;

  // Testdata, die von vielen Tests benoetigt wird:
  const radVisFeature1 = new RadVisFeature(13, toRadVisFeatureAttributesFromMap(), MASSNAHMEN.name, new Point([1, 1]));
  const radVisFeature2 = new RadVisFeature(
    14,
    toRadVisFeatureAttributesFromMap(),
    FAHRRADROUTE.name,
    new Point([3, 4])
  );
  const radVisFeature3 = new RadVisFeature(
    14,
    toRadVisFeatureAttributesFromMap(),
    FAHRRADROUTE.name,
    new Point([3, 4])
  );

  beforeEach(() => {
    layerRegistryService = mock(LayerRegistryService);
    featureHighlightService = mock(FeatureHighlightService);
    changeDetector = mock(ChangeDetectorRef);
    notifyUserService = mock(NotifyUserService);
    olMapService = mock(OlMapComponent);

    when(olMapService.outsideMapClick$()).thenReturn(NEVER);

    return MockBuilder(SelectFeatureMenuComponent, ViewerModule)
      .provide({
        provide: LayerRegistryService,
        useValue: instance(layerRegistryService),
      })
      .provide({
        provide: FeatureHighlightService,
        useValue: instance(featureHighlightService),
      })
      .provide({
        provide: ChangeDetectorRef,
        useValue: instance(changeDetector),
      })
      .provide({
        provide: NotifyUserService,
        useValue: instance(notifyUserService),
      })
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(SelectFeatureMenuComponent);
    component = fixture.componentInstance;
  });

  describe('onLocationSelect', () => {
    it('should route to Element if only one under click', () => {
      // arrange
      const locationSelectEvent = {
        selectedFeatures: [radVisFeature1],
        coordinate: [2, 2],
      } as LocationSelectEvent;

      when(layerRegistryService.getUniqueKey(radVisFeature1)).thenReturn('radVisFeature_id13');

      // act
      component.onLocationSelect(locationSelectEvent);

      // assert
      verify(layerRegistryService.toEditor(anything(), anything())).once();
      expect(capture(layerRegistryService.toEditor).last()[0]).toEqual(radVisFeature1);
      expect(capture(layerRegistryService.toEditor).last()[1]).toEqual([2, 2]);
    });

    it('should apply location and slectedFeatures', () => {
      // arrange
      const locationSelectEvent = {
        selectedFeatures: [radVisFeature1, radVisFeature2],
        coordinate: [2, 2],
      } as LocationSelectEvent;

      when(layerRegistryService.getUniqueKey(radVisFeature1)).thenReturn('radVisFeature_id13');
      when(layerRegistryService.getUniqueKey(radVisFeature2)).thenReturn('radVisFeature_id14');

      // act
      component.onLocationSelect(locationSelectEvent);

      // assert
      verify(layerRegistryService.toEditor(anything(), anything())).never();
      expect(component.location).toEqual([2, 2]);
      expect(component.selectedFeatures).toEqual([radVisFeature1, radVisFeature2]);
    });

    it('should kick out doubled features', () => {
      // arrange
      const locationSelectEvent = {
        selectedFeatures: [radVisFeature1, radVisFeature2, radVisFeature3],
        coordinate: [2, 2],
      } as LocationSelectEvent;

      when(layerRegistryService.getUniqueKey(radVisFeature1)).thenReturn('radVisFeature_id13');
      when(layerRegistryService.getUniqueKey(radVisFeature2)).thenReturn('radVisFeature_id14');
      when(layerRegistryService.getUniqueKey(radVisFeature3)).thenReturn('radVisFeature_id14');

      // act
      component.onLocationSelect(locationSelectEvent);

      // assert
      verify(layerRegistryService.toEditor(anything(), anything())).never();
      expect(component.location).toEqual([2, 2]);
      expect(component.selectedFeatures).toEqual([radVisFeature1, radVisFeature3]);
    });
  });

  describe('onFeatureHover', () => {
    it('should call highlight if hovered is true', () => {
      // act
      component.onFeatureHover(true, radVisFeature1);

      // assert
      verify(featureHighlightService.highlight(anything())).once();
      verify(featureHighlightService.unhighlight(anything())).never();
      expect(capture(featureHighlightService.highlight).last()[0]).toEqual(radVisFeature1);
    });

    it('should call unhighlight if hovered is false', () => {
      // act
      component.onFeatureHover(false, radVisFeature1);

      // assert
      verify(featureHighlightService.highlight(anything())).never();
      verify(featureHighlightService.unhighlight(anything())).once();
      expect(capture(featureHighlightService.unhighlight).last()[0]).toEqual(radVisFeature1);
    });
  });

  describe('onSelectFeature', () => {
    it('call toEditor and reset the select menu', () => {
      // arrange state of component when menu is open and onSelectFeature can be evoked
      component.location = [2, 2];
      component.selectedFeatures = [radVisFeature1, radVisFeature2];

      // act
      component.onSelectFeature(radVisFeature1);

      // assert toEditor
      verify(layerRegistryService.toEditor(anything(), anything())).once();
      expect(capture(layerRegistryService.toEditor).last()[0]).toEqual(radVisFeature1);
      expect(capture(layerRegistryService.toEditor).last()[1]).toEqual([2, 2]);

      // assert reset
      expect(component.location).toBeNull();
      verify(featureHighlightService.unhighlight(anything())).twice();
      expect(capture(featureHighlightService.unhighlight).first()[0]).toEqual(radVisFeature1);
      expect(capture(featureHighlightService.unhighlight).last()[0]).toEqual(radVisFeature2);
      expect(component.selectedFeatures).toEqual([]);
    });
  });
});
