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
import { CurrencyInputControlComponent } from 'src/app/form-elements/components/currency-input-control/currency-input-control.component';

describe(CurrencyInputControlComponent.name, () => {
  let component: CurrencyInputControlComponent;
  let fixture: ComponentFixture<CurrencyInputControlComponent>;

  beforeEach(() => {
    return MockBuilder(CurrencyInputControlComponent, EditorModule);
  });

  beforeEach(() => {
    fixture = MockRender(CurrencyInputControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('write value', () => {
    it('should not trigger value changes', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue(3);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should set form correct', () => {
      component.writeValue(340);
      expect(component.formControl.value).toBe('3,40');

      component.writeValue(820);
      expect(component.formControl.value).toBe('8,20');
    });
  });

  describe('onChange', () => {
    it('should convert string to number', () => {
      const spy = spyOn(component, 'onChange');
      component.formControl.setValue('2,45');
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual(245);
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
      expect(component.validate()).toEqual({
        isValidEuroString: 'Nur Beträge in € erlaubt (2 Nachkommastellen für Eurocents, max 20000000€). ',
      });
    });
  });
});
