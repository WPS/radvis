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

import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { AdministrationModule } from 'src/app/administration/administration.module';
import { MultiOrganisationenDropdownControlComponent } from 'src/app/form-elements/components/multi-organisationen-dropdown-control/multi-organisationen-dropdown-control.component';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';

describe(MultiOrganisationenDropdownControlComponent.name, () => {
  let component: MultiOrganisationenDropdownControlComponent;
  let fixture: MockedComponentFixture<MultiOrganisationenDropdownControlComponent>;

  beforeEach(() => {
    return MockBuilder(MultiOrganisationenDropdownControlComponent, AdministrationModule);
  });

  beforeEach(() => {
    fixture = MockRender(MultiOrganisationenDropdownControlComponent, {
      options: [],
    } as unknown as MultiOrganisationenDropdownControlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('filterOptions', () => {
    it('should filter options on type', () => {
      fixture.componentInstance.options = [
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 2,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 3,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ];
      fixture.detectChanges();
      component.formControl.patchValue('Tes');

      expect(component.filteredOptions).toEqual([
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 2,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ]);
    });

    it('should filter options on Input changes', () => {
      fixture.componentInstance.options = [
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 2,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 3,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ];
      fixture.detectChanges();
      // passiert durch Autocomplete
      component.formControl.patchValue(fixture.componentInstance.options[0]);

      expect(component.filteredOptions.length).toBe(3);
    });

    it('should emit all if value null', () => {
      fixture.componentInstance.options = [
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 2,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 3,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ];
      fixture.detectChanges();
      component.formControl.patchValue('Tes');

      expect(component.filteredOptions.length).toBe(2);

      component.formControl.patchValue(null);

      expect(component.filteredOptions).toEqual([
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 2,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 3,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ]);
    });

    describe('remove already selected', () => {
      it('should update on input change', () => {
        const options = [
          {
            id: 1,
            name: 'Test1',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 2,
            name: 'Test2',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 3,
            name: 'Blubb',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
        ];
        fixture.componentInstance.options = [options[0]];
        fixture.detectChanges();

        component.writeValue([options[0]]);

        expect(component.filteredOptions).toEqual([]);

        fixture.componentInstance.options = options;
        fixture.detectChanges();

        expect(component.filteredOptions).toEqual([options[1], options[2]]);
      });

      it('should update on select value', () => {
        const options = [
          {
            id: 1,
            name: 'Test1',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 2,
            name: 'Test2',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 3,
            name: 'Blubb',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
        ];
        fixture.componentInstance.options = options;
        fixture.detectChanges();

        component.writeValue([options[0]]);
        expect(component.filteredOptions).toEqual([options[1], options[2]]);

        component.onOptionSelected({ option: { value: options[1] } } as unknown as MatAutocompleteSelectedEvent);
        expect(component.filteredOptions).toEqual([options[2]]);
      });

      it('should update on remove value', () => {
        const options = [
          {
            id: 1,
            name: 'Test1',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 2,
            name: 'Test2',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 3,
            name: 'Blubb',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
        ];
        fixture.componentInstance.options = options;
        fixture.detectChanges();

        component.writeValue([options[0]]);
        expect(component.filteredOptions).toEqual([options[1], options[2]]);

        component.onOrganisationRemoved(options[0]);
        expect(component.filteredOptions).toEqual(options);
      });

      it('should update on write value', () => {
        const options = [
          {
            id: 1,
            name: 'Test1',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 2,
            name: 'Test2',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
          {
            id: 3,
            name: 'Blubb',
            organisationsArt: OrganisationsArt.KREIS,
            idUebergeordneteOrganisation: 2,
            aktiv: true,
          },
        ];
        fixture.componentInstance.options = options;
        fixture.detectChanges();

        component.writeValue([options[0]]);

        expect(component.filteredOptions).toEqual([options[1], options[2]]);
      });
    });
  });

  describe('onChange', () => {
    let onChangeSpy: jasmine.Spy;

    beforeEach(() => {
      onChangeSpy = spyOn(component, 'onChange');
    });

    it('should add Value', () => {
      component.writeValue([defaultOrganisation]);

      const org = { ...defaultOrganisation, id: 83556 };
      component.onOptionSelected({ option: { value: org } } as unknown as MatAutocompleteSelectedEvent);

      expect(onChangeSpy).toHaveBeenCalled();
      expect(onChangeSpy.calls.mostRecent().args[0]).toEqual([defaultOrganisation, org]);
    });

    it('should remove Value', () => {
      const org = { ...defaultOrganisation, id: 83556 };
      component.writeValue([defaultOrganisation, org]);

      component.onOrganisationRemoved(org);

      expect(onChangeSpy).toHaveBeenCalled();
      expect(onChangeSpy.calls.mostRecent().args[0]).toEqual([defaultOrganisation]);
    });
  });

  describe('options input', () => {
    it('should consider current input', () => {
      const onChangesSpy = spyOn(component, 'onChange').and.callFake(() => {});
      const formValue = 'Tes';
      component.formControl.patchValue(formValue);
      expect(onChangesSpy).not.toHaveBeenCalled();

      fixture.componentInstance.options = [
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 2,
          name: 'Test2',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 3,
          name: 'Blubb',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
      ];
      fixture.detectChanges();

      expect(component.filteredOptions).toEqual([
        {
          id: 1,
          name: 'Test1',
          organisationsArt: OrganisationsArt.KREIS,
          idUebergeordneteOrganisation: 2,
          aktiv: true,
        },
        {
          id: 2,
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
