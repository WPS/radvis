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
import { AutocorrectingNumberInputControlComponent } from 'src/app/editor/kanten/components/autocorrecting-number-input-control/autocorrecting-number-input-control.component';

describe('NumberInputControlComponent', () => {
  let component: AutocorrectingNumberInputControlComponent;
  let fixture: ComponentFixture<AutocorrectingNumberInputControlComponent>;

  beforeEach(() => {
    return MockBuilder(AutocorrectingNumberInputControlComponent, EditorModule);
  });

  beforeEach(() => {
    fixture = MockRender(AutocorrectingNumberInputControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('write Value', () => {
    it('should not trigger valueChanges', () => {
      const spy = spyOn(component, 'onChange');
      component.writeValue(1);
      component.writeValue(null);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should display 2 digits', () => {
      component.writeValue(10.0457);
      expect(component.formControl.value).toEqual('10,05');
    });
  });

  describe('on Change', () => {
    it('should return 2 digits', () => {
      const spy = spyOn(component, 'onChange');
      component.formControl.setValue('10,756');
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual(10.76);
    });

    it('should filter non-digits and non-comma characters', () => {
      const spy = spyOn(component, 'onChange');
      component.formControl.setValue('103kk,75#6');
      expect(spy).toHaveBeenCalled();
      expect(spy.calls.mostRecent().args[0]).toEqual(103.76);
    });
  });
});
