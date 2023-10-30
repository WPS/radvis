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

import { ComponentFixture } from '@angular/core/testing';
import { MockBuilder, MockRender } from 'ng-mocks';
import { EditorModule } from 'src/app/editor/editor.module';
import {
  UndeterminedValue,
  UNDETERMINED_LABEL,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { NumberInputControlComponent } from 'src/app/form-elements/components/number-input-control/number-input-control.component';

describe('NumberInputControlComponent', () => {
  let component: NumberInputControlComponent;
  let fixture: ComponentFixture<NumberInputControlComponent>;

  beforeEach(() => {
    return MockBuilder(NumberInputControlComponent, EditorModule);
  });

  beforeEach(() => {
    fixture = MockRender(NumberInputControlComponent);
    component = fixture.componentInstance;
    component.anzahlNachkommastellen = 2;
    fixture.detectChanges();
  });

  describe('write value', () => {
    it('should not trigger value changes', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue(3);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should handle undetermined value', () => {
      component.writeValue(new UndeterminedValue());
      expect(component.formControl.value).toEqual(UNDETERMINED_LABEL);
    });

    it('should set form correct', () => {
      component.writeValue(3.4);
      expect(component.formControl.value).toBe('3,40');

      component.writeValue(3.416);
      expect(component.formControl.value).toBe('3,42');
    });
  });

  describe('onChange', () => {
    it('should convert string to number', () => {
      const spy = spyOn(component, 'onChange');
      component.formControl.setValue('2,457');
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual(2.46);
    });

    it('should return NaN for invalid input', () => {
      const spy = spyOn(component, 'onChange');
      component.formControl.setValue('abc');
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toBeNaN();
    });
  });

  describe('validate', () => {
    it('should be invalid for string input', () => {
      component.formControl.setValue('abc');
      expect(component.formControl.valid).toBeFalse();
      expect(component.validate()).toEqual({ isFloat: 'Nur Kommazahlen erlaubt' });
    });

    it('should not be invalid for undeterminedValue', () => {
      component.writeValue(new UndeterminedValue());
      component.formControl.updateValueAndValidity();
      expect(component.formControl.valid).toBeTrue();
      expect(component.validate()).toEqual(null);
    });
  });
});
