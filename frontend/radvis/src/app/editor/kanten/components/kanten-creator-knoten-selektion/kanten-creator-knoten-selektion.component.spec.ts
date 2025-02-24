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

import { NgZone } from '@angular/core';
import { Feature } from 'ol';
import { Point } from 'ol/geom';
import { SelectEvent } from 'ol/interaction/Select';
import { of } from 'rxjs';
import { KantenCreatorKnotenSelektionComponent } from 'src/app/editor/kanten/components/kanten-creator-knoten-selektion/kanten-creator-knoten-selektion.component';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { anyFunction, instance, mock, when } from 'ts-mockito';

describe(KantenCreatorKnotenSelektionComponent.name, () => {
  let component: KantenCreatorKnotenSelektionComponent;
  const olMapServiceMock = mock(OlMapComponent);
  const zoneMock = mock(NgZone);
  const errorHandlingService = mock(ErrorHandlingService);
  const featureServiceMock = mock(NetzausschnittService);
  const mapQueryParamsServiceMock = mock(MapQueryParamsService);
  const featureA = new Feature(new Point([0, 1]));
  const featureB = new Feature(new Point([0, 2]));
  const featureC = new Feature(new Point([0, 3]));

  beforeEach(() => {
    // zoneMock should execute the passed function synchronously like the original ngZone
    when(zoneMock.run(anyFunction())).thenCall(<T>(fn: (...args: any[]) => T) => fn());
    when(mapQueryParamsServiceMock.netzklassen$).thenReturn(of([]));
    when(olMapServiceMock.click$()).thenReturn(of());
    component = new KantenCreatorKnotenSelektionComponent(
      instance(olMapServiceMock),
      instance(zoneMock),
      instance(errorHandlingService),
      instance(featureServiceMock),
      instance(mapQueryParamsServiceMock)
    );
    featureA.setId('1');
    featureB.setId('2');
    featureC.setId('3');
  });

  describe('onSelect', () => {
    it('should select A Knoten', () => {
      const selectVonKnotenEventSpy = spyOn(component.selectVonKnoten, 'emit');
      const selectBisKnotenEventSpy = spyOn(component.selectBisKnoten, 'emit');

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: false } },
        selected: [featureA],
        deselected: [],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(undefined);
    });

    it('should select B Knoten', () => {
      const selectVonKnotenEventSpy = spyOn(component.selectVonKnoten, 'emit');
      const selectBisKnotenEventSpy = spyOn(component.selectBisKnoten, 'emit');

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: true } },
        selected: [featureA],
        deselected: [],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(undefined);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);
    });

    it('should select another A Knoten', () => {
      const selectVonKnotenEventSpy = spyOn(component.selectVonKnoten, 'emit');
      const selectBisKnotenEventSpy = spyOn(component.selectBisKnoten, 'emit');

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: false } },
        selected: [featureA],
        deselected: [],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(undefined);

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: false } },
        selected: [featureB],
        deselected: [featureA],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(featureB.getId() as string);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(undefined);
    });

    it('should select another B Knoten', () => {
      const selectVonKnotenEventSpy = spyOn(component.selectVonKnoten, 'emit');
      const selectBisKnotenEventSpy = spyOn(component.selectBisKnoten, 'emit');

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: true } },
        selected: [featureA],
        deselected: [],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(undefined);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: true } },
        selected: [featureB],
        deselected: [featureA],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(undefined);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(featureB.getId() as string);
    });

    it('should select A Knoten then B Knoten', () => {
      const selectVonKnotenEventSpy = spyOn(component.selectVonKnoten, 'emit');
      const selectBisKnotenEventSpy = spyOn(component.selectBisKnoten, 'emit');

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: false } },
        selected: [featureA],
        deselected: [],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(undefined);

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: true } },
        selected: [featureB],
        deselected: [featureA],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(featureB.getId() as string);
    });

    it('should select B Knoten then A Knoten', () => {
      const selectVonKnotenEventSpy = spyOn(component.selectVonKnoten, 'emit');
      const selectBisKnotenEventSpy = spyOn(component.selectBisKnoten, 'emit');

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: true } },
        selected: [featureA],
        deselected: [],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(undefined);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: false } },
        selected: [featureB],
        deselected: [featureA],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(featureB.getId() as string);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);
    });

    it('should clear selection', () => {
      const selectVonKnotenEventSpy = spyOn(component.selectVonKnoten, 'emit');
      const selectBisKnotenEventSpy = spyOn(component.selectBisKnoten, 'emit');

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: true } },
        selected: [featureA],
        deselected: [],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(undefined);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: false } },
        selected: [featureB],
        deselected: [featureA],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(featureB.getId() as string);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(featureA.getId() as string);

      component['onSelect']({
        mapBrowserEvent: { originalEvent: { ctrlKey: false } },
        selected: [],
        deselected: [featureA, featureB],
      } as unknown as SelectEvent);

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(undefined);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(undefined);
    });
  });

  describe('resetSelection', () => {
    it('should clear interaction and emit events', () => {
      // @ts-expect-error Migration von ts-ignore
      component.onSelect({
        mapBrowserEvent: { originalEvent: { ctrlKey: false } },
        selected: [featureA],
        deselected: [],
      } as SelectEvent);
      // @ts-expect-error Migration von ts-ignore
      component.onSelect({
        mapBrowserEvent: { originalEvent: { ctrlKey: true } },
        selected: [featureB],
        deselected: [],
      } as SelectEvent);
      component.selectInteraction.getFeatures().push(featureA);
      component.selectInteraction.getFeatures().push(featureB);
      const selectVonKnotenEventSpy = spyOn(component.selectVonKnoten, 'emit');
      const selectBisKnotenEventSpy = spyOn(component.selectBisKnoten, 'emit');

      component.resetSelection();

      expect(selectVonKnotenEventSpy).toHaveBeenCalledWith(undefined);
      expect(selectBisKnotenEventSpy).toHaveBeenCalledWith(undefined);
      expect(component.selectInteraction.getFeatures().getLength()).toEqual(0);
    });
  });
});
