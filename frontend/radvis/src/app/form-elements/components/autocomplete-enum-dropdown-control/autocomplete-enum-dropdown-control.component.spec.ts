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

import { ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MockBuilder, MockedComponentFixture, MockRender, ngMocks } from 'ng-mocks';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { SharedModule } from 'src/app/shared/shared.module';

import { BreakpointObserver } from '@angular/cdk/layout';
import { AutocompleteEnumDropdownControlComponent } from './autocomplete-enum-dropdown-control.component';

describe('AutocompleteEnumDropdownControlComponent', () => {
  let component: AutocompleteEnumDropdownControlComponent;
  let fixture: MockedComponentFixture<AutocompleteEnumDropdownControlComponent>;

  beforeEach(() => {
    return MockBuilder(AutocompleteEnumDropdownControlComponent, SharedModule)
      .keep(ReactiveFormsModule)
      .keep(MatFormFieldModule)
      .keep(MatAutocompleteModule)
      .keep(MatInputModule)
      .keep(BreakpointObserver);
  });

  beforeEach(() => {
    fixture = MockRender(AutocompleteEnumDropdownControlComponent, ({
      options: [],
    } as unknown) as AutocompleteEnumDropdownControlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('update filteredOptions', () => {
    it('should update if @input change', () => {
      expect(component.filteredEnumOptions).toEqual([]);
      const testOptions: EnumOption[] = [{ name: 'TEST', displayText: 'Test' }];
      fixture.componentInstance.options = testOptions;
      fixture.detectChanges();
      expect(component.filteredEnumOptions).toEqual(testOptions);
    });

    it('should update on user input/case insensitiv', () => {
      const testOptions: EnumOption[] = [
        { name: 'TEST', displayText: 'TestText' },
        { name: 'SUPER', displayText: 'SuperText' },
      ];
      fixture.componentInstance.options = testOptions;
      fixture.detectChanges();

      component.formControl.setValue('testt');
      expect(component.filteredEnumOptions).toEqual([testOptions[0]]);
    });

    it('should update on write value/enum name', () => {
      const testOptions: EnumOption[] = [
        { name: 'TEST', displayText: 'TestText' },
        { name: 'SUPER', displayText: 'SuperText' },
      ];
      fixture.componentInstance.options = testOptions;
      fixture.detectChanges();

      component.writeValue('TEST');
      expect(component.filteredEnumOptions).toEqual([testOptions[0]]);
    });
  });

  describe('displayFn', () => {
    it('should use displayText', async () => {
      const testOptions: EnumOption[] = [
        { name: 'TEST', displayText: 'TestText' },
        { name: 'SUPER', displayText: 'SuperText' },
      ];
      fixture.componentInstance.options = testOptions;
      fixture.detectChanges();
      component.writeValue(testOptions[0].name);
      fixture.detectChanges();
      await fixture.whenStable().then(() => {
        const inputElement = ngMocks.find('input');
        expect((inputElement.nativeElement as HTMLFormElement).value).toEqual(testOptions[0].displayText);
      });
    });

    it('should update after @input change', async () => {
      const inputElement = ngMocks.find('input');
      const testOptions: EnumOption[] = [
        { name: 'TEST', displayText: 'TestText' },
        { name: 'SUPER', displayText: 'SuperText' },
      ];
      component.writeValue(testOptions[0].name);
      fixture.detectChanges();

      await fixture.whenStable().then(() => {
        expect((inputElement.nativeElement as HTMLFormElement).value).toEqual('');
      });

      fixture.componentInstance.options = testOptions;
      fixture.detectChanges();

      await fixture.whenStable().then(() => {
        expect((inputElement.nativeElement as HTMLFormElement).value).toEqual(testOptions[0].displayText);
      });
    });
  });
});
