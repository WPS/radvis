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
import { OrganisationenDropdownControlComponent } from 'src/app/shared/components/organisationen-dropdown-control/organisationen-dropdown-control.component';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { SharedModule } from 'src/app/shared/shared.module';

describe(OrganisationenDropdownControlComponent.name, () => {
  let component: OrganisationenDropdownControlComponent;
  let fixture: MockedComponentFixture<OrganisationenDropdownControlComponent>;

  beforeEach(() => {
    return MockBuilder(OrganisationenDropdownControlComponent, SharedModule);
  });

  beforeEach(() => {
    fixture = MockRender(OrganisationenDropdownControlComponent, {
      options: [],
    } as unknown as OrganisationenDropdownControlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('filterOptions', () => {
    it('should filter options on type', () => {
      component.options = [
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ];

      component.filteredOptions$.pipe(skip(1)).subscribe(filtered => {
        expect(filtered).toEqual([
          {
            id: 1,
            name: 'Test1',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 1,
            name: 'Test2',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
        ]);
      });

      component.formControl.patchValue('Tes');
    });

    it('should emit all if value null', () => {
      component.options = [
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ];

      component.formControl.patchValue('Tes');

      component.filteredOptions$.subscribe(filtered => {
        expect(filtered).toEqual([
          {
            id: 1,
            name: 'Test1',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 1,
            name: 'Test2',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 1,
            name: 'Blubb',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
        ]);
      });
      component.formControl.patchValue(null);
    });

    it('should emit all if value is Organisation', () => {
      component.options = [
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ];

      component.formControl.patchValue('Tes');

      component.filteredOptions$.subscribe(filtered => {
        expect(filtered).toEqual([
          {
            id: 1,
            name: 'Test1',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 1,
            name: 'Test2',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 1,
            name: 'Blubb',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
        ]);
      });
      component.formControl.patchValue({
        id: 1,
        name: 'Test2',
        organisationsArt: OrganisationsArt.KREIS,
        idUebergeordneteOrganisation: 2,
        aktiv: true,
      });
    });
  });

  describe('options input', () => {
    it('should update filterOptions on changes', () => {
      const onChangesSpy = spyOn(component, 'onChange').and.callFake(() => {});
      const formValue: Verwaltungseinheit = {
        id: 1,
        name: 'Test1',
        organisationsArt: OrganisationsArt.KREIS,
        idUebergeordneteOrganisation: 2,
        aktiv: true,
      };
      component.writeValue(formValue);
      expect(onChangesSpy).not.toHaveBeenCalled();

      component.options = [
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
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
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 1,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ];

      component.ngOnInit();

      component.filteredOptions$.subscribe(filteredOptions$ => {
        expect(filteredOptions$).toEqual([
          {
            id: 1,
            name: 'Test1',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 1,
            name: 'Test2',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
        ]);
        expect(onChangesSpy).not.toHaveBeenCalled();
        expect(component.formControl.value).toEqual(formValue);
      });
    });
  });
});
