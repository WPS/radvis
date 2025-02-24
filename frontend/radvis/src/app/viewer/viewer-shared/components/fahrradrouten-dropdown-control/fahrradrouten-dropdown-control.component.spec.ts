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

import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { skip } from 'rxjs/operators';
import { SharedModule } from 'src/app/shared/shared.module';
import { FahrradroutenDropdownControlComponent } from 'src/app/viewer/viewer-shared/components/fahrradrouten-dropdown-control/fahrradrouten-dropdown-control.component';
import { FahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view';
import { testFahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view-test-data-provider.spec';

describe(FahrradroutenDropdownControlComponent.name, () => {
  let component: FahrradroutenDropdownControlComponent;
  let fixture: MockedComponentFixture<FahrradroutenDropdownControlComponent>;

  beforeEach(() => {
    return MockBuilder(FahrradroutenDropdownControlComponent, SharedModule);
  });

  beforeEach(() => {
    fixture = MockRender(FahrradroutenDropdownControlComponent, {
      options: [],
    } as unknown as FahrradroutenDropdownControlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('filterOptions', () => {
    it('should filter options on type', () => {
      component.options = testFahrradrouteListenView;

      component.filteredOptions$.pipe(skip(1)).subscribe(filtered => {
        expect(filtered).toEqual(testFahrradrouteListenView.slice(0, 2));
      });

      component.formControl.patchValue('Tes');
    });

    it('should emit all if value null', () => {
      component.options = testFahrradrouteListenView;

      component.formControl.patchValue('Tes');

      component.filteredOptions$.subscribe(filtered => {
        expect(filtered).toEqual(testFahrradrouteListenView);
      });
      component.formControl.patchValue(null);
    });

    it('should emit all if value is Fahrradroute', () => {
      component.options = testFahrradrouteListenView;

      component.formControl.patchValue('Tes');

      component.filteredOptions$.subscribe(filtered => {
        expect(filtered).toEqual(testFahrradrouteListenView);
      });
      component.formControl.patchValue(testFahrradrouteListenView[0]);
    });
  });

  describe('options input', () => {
    it('should update filterOptions on changes', () => {
      const onChangesSpy = spyOn(component, 'onChange').and.callFake(() => {});
      const formValue: FahrradrouteListenView = testFahrradrouteListenView[0];
      component.writeValue(formValue);
      expect(onChangesSpy).not.toHaveBeenCalled();

      component.options = testFahrradrouteListenView;

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

      component.options = testFahrradrouteListenView;

      component.ngOnInit();

      component.filteredOptions$.subscribe(filteredOptions$ => {
        expect(filteredOptions$).toEqual(testFahrradrouteListenView.slice(0, 2));
        expect(onChangesSpy).not.toHaveBeenCalled();
        expect(component.formControl.value).toEqual(formValue);
      });
    });
  });
});
