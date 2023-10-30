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

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { EditorModule } from 'src/app/editor/editor.module';
import { GroupedEnumDropdownControlComponent } from 'src/app/form-elements/components/grouped-enum-dropdown-control/grouped-enum-dropdown-control.component';
import { GroupedEnumOptions } from 'src/app/form-elements/models/grouped-enum-options';

describe('GroupedEnumDropdownControlComponent', () => {
  let component: GroupedEnumDropdownControlComponent;
  let fixture: ComponentFixture<GroupedEnumDropdownControlComponent>;

  beforeEach(() => {
    return MockBuilder(GroupedEnumDropdownControlComponent, EditorModule);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupedEnumDropdownControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('updateFilteredOptions', () => {
    it('should filter correctly', fakeAsync(() => {
      component.groupedOptions = {
        GRUPPE_A: {
          displayText: 'Gruppe A',
          options: [
            {
              name: 'OPTION_1',
              displayText: 'TestText',
            },
            {
              name: 'OPTION_2',
              displayText: 'Test',
            },
          ],
        },
        GRUPPE_B: {
          displayText: 'Gruppe B',
          options: [
            {
              name: 'OPTION_3',
              displayText: 'Text',
            },
          ],
        },
      };

      component.formControl.patchValue('Text');

      tick();
      fixture.detectChanges();

      expect(component.filteredGroupedOptions).toEqual({
        GRUPPE_A: {
          displayText: 'Gruppe A',
          options: [
            {
              name: 'OPTION_1',
              displayText: 'TestText',
            },
          ],
        },
        GRUPPE_B: {
          displayText: 'Gruppe B',
          options: [
            {
              name: 'OPTION_3',
              displayText: 'Text',
            },
          ],
        },
      });
    }));

    it('should filter complete group if no option of this group matches', fakeAsync(() => {
      component.groupedOptions = {
        GRUPPE_A: {
          displayText: 'Gruppe A',
          options: [
            {
              name: 'OPTION_1',
              displayText: 'TestText',
            },
            {
              name: 'OPTION_2',
              displayText: 'Test',
            },
          ],
        },
        GRUPPE_B: {
          displayText: 'Gruppe B',
          options: [
            {
              name: 'OPTION_3',
              displayText: 'Text',
            },
          ],
        },
      };

      component.formControl.patchValue('Test');

      tick();
      fixture.detectChanges();

      expect(component.filteredGroupedOptions).toEqual({
        GRUPPE_A: {
          displayText: 'Gruppe A',
          options: [
            {
              name: 'OPTION_1',
              displayText: 'TestText',
            },
            {
              name: 'OPTION_2',
              displayText: 'Test',
            },
          ],
        },
      });
    }));

    it('should offer all options when no input was made', fakeAsync(() => {
      const allOptions: GroupedEnumOptions = {
        GRUPPE_A: {
          displayText: 'Gruppe A',
          options: [
            {
              name: 'OPTION_1',
              displayText: 'TestText',
            },
            {
              name: 'OPTION_2',
              displayText: 'Test',
            },
          ],
        },
        GRUPPE_B: {
          displayText: 'Gruppe B',
          options: [
            {
              name: 'OPTION_3',
              displayText: 'Text',
            },
          ],
        },
      };
      component.groupedOptions = allOptions;

      component.formControl.patchValue(null);

      tick();
      fixture.detectChanges();

      expect(component.filteredGroupedOptions).toEqual(allOptions);
    }));
  });
});
