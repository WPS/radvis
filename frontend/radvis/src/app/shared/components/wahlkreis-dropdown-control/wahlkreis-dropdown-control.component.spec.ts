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
import { skip, take } from 'rxjs/operators';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { WahlkreisDropdownControlComponent } from 'src/app/shared/components/wahlkreis-dropdown-control/wahlkreis-dropdown-control.component';
import { SharedModule } from 'src/app/shared/shared.module';

describe(WahlkreisDropdownControlComponent.name, () => {
  let component: WahlkreisDropdownControlComponent;
  let fixture: MockedComponentFixture<WahlkreisDropdownControlComponent, any>;

  beforeEach(() => {
    return MockBuilder(WahlkreisDropdownControlComponent, SharedModule);
  });

  beforeEach(() => {
    fixture = MockRender(WahlkreisDropdownControlComponent, { options: [] });
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('filterOptions', () => {
    it('should filter options on type', (done: DoneFn) => {
      component.options = [
        {
          id: 1,
          name: 'Test1',
          nummer: 1,
        },
        {
          id: 1,
          name: 'Test2',
          nummer: 2,
        },
        {
          id: 1,
          name: 'Blubb',
          nummer: 3,
        },
      ];

      component.ngOnChanges();

      // der erste Wert wird übersprungen, da ngOnChanges mit startWith arbeitet
      component.filteredOptions$.pipe(skip(1)).subscribe(filtered => {
        expect(filtered).toEqual([
          {
            id: 1,
            name: 'Test1',
            nummer: 1,
          },
          {
            id: 1,
            name: 'Test2',
            nummer: 2,
          },
        ]);
        done();
      });

      component.formControl.patchValue('Tes');
    });

    it('should emit all if value null', (done: DoneFn) => {
      component.options = [
        {
          id: 1,
          name: 'Test1',
          nummer: 1,
        },
        {
          id: 1,
          name: 'Test2',
          nummer: 2,
        },
        {
          id: 1,
          name: 'Blubb',
          nummer: 3,
        },
      ];
      component.ngOnChanges();

      component.formControl.patchValue('Tes');

      component.filteredOptions$.pipe(skip(1)).subscribe(filtered => {
        expect(filtered).toEqual([
          {
            id: 1,
            name: 'Test1',
            nummer: 1,
          },
          {
            id: 1,
            name: 'Test2',
            nummer: 2,
          },
          {
            id: 1,
            name: 'Blubb',
            nummer: 3,
          },
        ]);
        done();
      });
      component.formControl.patchValue(null);
    });

    it('should emit all if value is Organisation', (done: DoneFn) => {
      component.options = [
        {
          id: 1,
          name: 'Test1',
          nummer: 1,
        },
        {
          id: 1,
          name: 'Test2',
          nummer: 2,
        },
        {
          id: 1,
          name: 'Blubb',
          nummer: 3,
        },
      ];
      component.ngOnChanges();

      component.formControl.patchValue('Tes');

      component.filteredOptions$.pipe(skip(1)).subscribe(filtered => {
        expect(filtered).toEqual([
          {
            id: 1,
            name: 'Test1',
            nummer: 1,
          },
          {
            id: 1,
            name: 'Test2',
            nummer: 2,
          },
          {
            id: 1,
            name: 'Blubb',
            nummer: 3,
          },
        ]);
        done();
      });
      component.formControl.patchValue({
        id: 1,
        name: 'Test2',
        organisationsArt: OrganisationsArt.KREIS,
        idUebergeordneteOrganisation: 2,
      });
    });
  });

  describe('options input', () => {
    it('should update filterOptions on changes', (done: DoneFn) => {
      const onChangesSpy = spyOn(component, 'onChange').and.callFake(() => {});
      const formValue = {
        id: 1,
        name: 'Test1',
        organisationsArt: OrganisationsArt.KREIS,
        idUebergeordneteOrganisation: 2,
      };
      component.writeValue(formValue);
      expect(onChangesSpy).not.toHaveBeenCalled();

      component.options = [
        {
          id: 1,
          name: 'Test1',
          nummer: 1,
        },
        {
          id: 1,
          name: 'Test2',
          nummer: 2,
        },
        {
          id: 1,
          name: 'Blubb',
          nummer: 3,
        },
      ];

      component.ngOnChanges();

      component.filteredOptions$.pipe(take(1)).subscribe(filteredOptions$ => {
        expect(filteredOptions$).toEqual(component.options);
        expect(onChangesSpy).not.toHaveBeenCalled();
        expect(component.formControl.value).toEqual(formValue);
        done();
      });
    });

    it('should consider current input', (done: DoneFn) => {
      const onChangesSpy = spyOn(component, 'onChange').and.callFake(() => {});
      const formValue = 'Tes';
      component.formControl.patchValue(formValue);
      expect(onChangesSpy).not.toHaveBeenCalled();

      component.options = [
        {
          id: 1,
          name: 'Test1',
          nummer: 1,
        },
        {
          id: 1,
          name: 'Test2',
          nummer: 2,
        },
        {
          id: 1,
          name: 'Blubb',
          nummer: 3,
        },
      ];

      component.ngOnChanges();

      component.filteredOptions$.pipe(take(1)).subscribe(filteredOptions$ => {
        expect(filteredOptions$).toEqual([
          {
            id: 1,
            name: 'Test1',
            nummer: 1,
          },
          {
            id: 1,
            name: 'Test2',
            nummer: 2,
          },
        ]);
        expect(onChangesSpy).not.toHaveBeenCalled();
        expect(component.formControl.value).toEqual(formValue);
        done();
      });
    });
  });
});
