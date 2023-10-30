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

import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatToolbar } from '@angular/material/toolbar';
import { MockComponent } from 'ng-mocks';
import Style from 'ol/style/Style';
import { LayerAuswahlComponent } from 'src/app/radnetzmatching/components/layer-auswahl/layer-auswahl.component';
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { ViewerModule } from 'src/app/viewer/viewer.module';

@Component({
  template: '<rad-layer-auswahl  [layers]="layers"  [visibleLayers]="visibleLayers"></rad-layer-auswahl>',
})
class TestWrapperComponent {
  @ViewChild(LayerAuswahlComponent)
  component!: LayerAuswahlComponent;

  layers: RadVisLayer[] = [];
  visibleLayers: RadVisLayer[] = [];
}

describe('LayerAuswahlComponent', () => {
  let hostComponent: TestWrapperComponent;
  let hostFixture: ComponentFixture<TestWrapperComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        TestWrapperComponent,
        LayerAuswahlComponent,
        MockComponent(MatCheckbox),
        MockComponent(MatToolbar),
      ],
      imports: [ReactiveFormsModule, ViewerModule],
    }).compileComponents();
  });

  beforeEach(() => {
    hostFixture = TestBed.createComponent(TestWrapperComponent);
    hostComponent = hostFixture.componentInstance;
  });

  it('should create', () => {
    hostFixture.detectChanges();
    expect(hostComponent.component).toBeTruthy();
  });

  describe('show visible layers', () => {
    let component: LayerAuswahlComponent;
    beforeEach(() => {
      hostComponent.layers = [
        new RadVisLayer(
          '1',
          'Test1',
          '',
          RadVisLayerTyp.GEO_JSON,
          new Style({}),
          [255, 255, 255, 100],
          LayerTypes.QUELLE
        ),
        new RadVisLayer(
          '2',
          'Test2',
          '',
          RadVisLayerTyp.GEO_JSON,
          new Style({}),
          [255, 255, 255, 100],
          LayerTypes.QUELLE
        ),
      ];
      hostComponent.visibleLayers = [hostComponent.layers[0]];
      hostFixture.detectChanges();
      component = hostComponent.component;
    });

    it('should initialize visible Layers correct', () => {
      expect(component.formArray.value).toEqual([
        { name: 'Test1', selected: true, color: 'rgba(255,255,255,100)', minZoom: 0 },
        { name: 'Test2', selected: false, color: 'rgba(255,255,255,100)', minZoom: 0 },
      ]);
    });

    it('should select layers', () => {
      const emitterSpy = spyOn(component.showLayer, 'emit');

      hostComponent.visibleLayers = [...hostComponent.layers];
      hostFixture.detectChanges();

      expect(component.formArray.value).toEqual([
        { name: 'Test1', selected: true, color: 'rgba(255,255,255,100)', minZoom: 0 },
        { name: 'Test2', selected: true, color: 'rgba(255,255,255,100)', minZoom: 0 },
      ]);
      expect(emitterSpy).not.toHaveBeenCalled();
    });

    it('should unselect layers', () => {
      const emitterSpy = spyOn(component.hideLayer, 'emit');

      hostComponent.visibleLayers = [];
      hostFixture.detectChanges();

      expect(component.formArray.value).toEqual([
        { name: 'Test1', selected: false, color: 'rgba(255,255,255,100)', minZoom: 0 },
        { name: 'Test2', selected: false, color: 'rgba(255,255,255,100)', minZoom: 0 },
      ]);
      expect(emitterSpy).not.toHaveBeenCalled();
    });
  });

  describe('emit changes', () => {
    let component: LayerAuswahlComponent;
    beforeEach(() => {
      hostComponent.layers = [
        new RadVisLayer(
          '1',
          'Test1',
          '',
          RadVisLayerTyp.GEO_JSON,
          new Style({}),
          [255, 255, 255, 100],
          LayerTypes.QUELLE
        ),
        new RadVisLayer(
          '2',
          'Test2',
          '',
          RadVisLayerTyp.GEO_JSON,
          new Style({}),
          [255, 255, 255, 100],
          LayerTypes.QUELLE
        ),
      ];
      hostFixture.detectChanges();
      component = hostComponent.component;
    });

    it('should emit show', () => {
      const emitterSpy = spyOn(component.showLayer, 'emit');
      hostComponent.visibleLayers = [];
      hostFixture.detectChanges();
      component.formArray.controls[0].get('selected')?.setValue(true);
      expect(emitterSpy).toHaveBeenCalled();
      expect(emitterSpy.calls.mostRecent().args[0]).toBe(component.layers[0].id);
    });

    it('should emit hide', () => {
      const emitterSpy = spyOn(component.hideLayer, 'emit');
      component.formArray.controls[0].get('selected')?.setValue(false);
      expect(emitterSpy).toHaveBeenCalled();
      expect(emitterSpy.calls.mostRecent().args[0]).toBe(component.layers[0].id);
    });
  });
});
