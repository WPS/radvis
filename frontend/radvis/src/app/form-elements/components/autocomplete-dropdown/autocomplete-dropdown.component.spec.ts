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

import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { skip } from 'rxjs/operators';
import { SharedModule } from 'src/app/shared/shared.module';
import { AutoCompleteOption, AutocompleteDropdownComponent } from './autocomplete-dropdown.component';

describe(AutocompleteDropdownComponent.name, () => {
  let component: AutocompleteDropdownComponent;
  let fixture: MockedComponentFixture<AutocompleteDropdownComponent>;

  beforeEach(() => {
    return MockBuilder(AutocompleteDropdownComponent, SharedModule);
  });

  beforeEach(() => {
    fixture = MockRender(AutocompleteDropdownComponent, {
      options: [],
    } as unknown as AutocompleteDropdownComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('filterOptions', () => {
    it('should filter options on option type', () => {
      component.options = [
        {
          id: 1,
          name: 'Test1',
          displayText: 'Test1 display',
        },
        {
          id: 2,
          name: 'Test2',
          displayText: 'Test2 display',
        },
        {
          id: 3,
          name: 'Blubb',
          displayText: 'Blubb display',
        },
      ];

      component.filteredOptions$.pipe(skip(1)).subscribe(filtered => {
        expect(filtered).toEqual([
          {
            id: 1,
            name: 'Test1',
            displayText: 'Test1 display',
          },
          {
            id: 2,
            name: 'Test2',
            displayText: 'Test2 display',
          },
        ]);
      });

      component.formControl.patchValue('Tes');
    });

    it('should emit all if value null', () => {
      component.options = [
        {
          name: 'Test1',
          displayText: 'Test1 display',
        },
        {
          name: 'Test2',
          displayText: 'Test2 display',
        },
        {
          name: 'Blubb',
          displayText: 'Blubb display',
        },
      ];

      component.formControl.patchValue('Tes');

      component.filteredOptions$.subscribe(filtered => {
        expect(filtered).toEqual([
          {
            name: 'Test1',
            displayText: 'Test1 display',
          },
          {
            name: 'Test2',
            displayText: 'Test2 display',
          },
          {
            name: 'Blubb',
            displayText: 'Blubb display',
          },
        ]);
      });
      component.formControl.patchValue(null);
    });

    it('should emit all if value is Organisation', () => {
      component.options = [
        {
          name: 'Test1',
          displayText: 'Test1 display',
        },
        {
          name: 'Test2',
          displayText: 'Test2 display',
        },
        {
          name: 'Blubb',
          displayText: 'Blubb display',
        },
      ];

      component.formControl.patchValue('Tes');

      component.filteredOptions$.subscribe(filtered => {
        expect(filtered).toEqual([
          {
            name: 'Test1',
            displayText: 'Test1 display',
          },
          {
            name: 'Test2',
            displayText: 'Test2 display',
          },
          {
            name: 'Blubb',
            displayText: 'Blubb display',
          },
        ]);
      });
      component.formControl.patchValue({
        name: 'Test2',
        displayText: 'Test2 display',
      });
    });
  });

  describe('options input', () => {
    it('should update filterOptions on changes', () => {
      const onChangesSpy = spyOn(component, 'onChange').and.callFake(() => {});
      const formValue: AutoCompleteOption = {
        name: 'Test1',
        displayText: 'Test1 display',
      };
      component.writeValue(formValue);
      expect(onChangesSpy).not.toHaveBeenCalled();

      component.options = [
        {
          name: 'Test1',
          displayText: 'Test1 display',
        },
        {
          name: 'Test2',
          displayText: 'Test2 display',
        },
        {
          name: 'Blubb',
          displayText: 'Blubb display',
        },
      ];

      component.filteredOptions$.subscribe(filteredOptions$ => {
        expect(filteredOptions$).toEqual(component.options);
        expect(onChangesSpy).not.toHaveBeenCalled();
        expect(component.formControl.value).toEqual(formValue);
      });
    });

    it('should consider current input', () => {
      const onChangesSpy = spyOn(component, 'onChange').and.callFake(() => {});
      const formValue = 'Tes';
      component.formControl.patchValue(formValue);
      expect(onChangesSpy).not.toHaveBeenCalled();

      component.options = [
        {
          name: 'Test1',
          displayText: 'Test1 display',
        },
        {
          name: 'Test2',
          displayText: 'Test2 display',
        },
        {
          name: 'Blubb',
          displayText: 'Blubb display',
        },
      ];

      component.ngOnInit();

      component.filteredOptions$.subscribe(filteredOptions$ => {
        expect(filteredOptions$).toEqual([
          {
            name: 'Test1',
            displayText: 'Test1 display',
          },
          {
            name: 'Test2',
            displayText: 'Test2 display',
          },
        ]);
        expect(onChangesSpy).not.toHaveBeenCalled();
        expect(component.formControl.value).toEqual(formValue);
      });
    });
  });
});
