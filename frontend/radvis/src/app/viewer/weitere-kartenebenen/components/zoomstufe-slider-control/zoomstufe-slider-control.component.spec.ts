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

import { ZoomstufeSliderControlComponent } from 'src/app/viewer/weitere-kartenebenen/components/zoomstufe-slider-control/zoomstufe-slider-control.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';

describe(ZoomstufeSliderControlComponent.name, () => {
  let component: ZoomstufeSliderControlComponent;
  let fixture: ComponentFixture<ZoomstufeSliderControlComponent>;

  beforeEach(() => {
    fixture = TestBed.createComponent(ZoomstufeSliderControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Massstab', () => {
    it('should match scales', () => {
      component.writeValue(6);
      expect(component.getMassStab()).toBeCloseTo(8_700_000, -5);
      component.writeValue(8.7);
      expect(component.getMassStab()).toBeCloseTo(1_300_000, -5);
      component.writeValue(10);
      expect(component.getMassStab()).toBeGreaterThanOrEqual(0);
      component.writeValue(15);
      expect(component.getMassStab()).toBeCloseTo(17_000, -3);
    });
  });
});
