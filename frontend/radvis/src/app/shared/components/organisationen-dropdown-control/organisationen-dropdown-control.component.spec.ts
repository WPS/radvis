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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { skip } from 'rxjs/operators';
import { EditorModule } from 'src/app/editor/editor.module';
import { OrganisationenDropdownControlComponent } from 'src/app/shared/components/organisationen-dropdown-control/organisationen-dropdown-control.component';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';

describe('OrganisationenDropdownControlComponent', () => {
  let component: OrganisationenDropdownControlComponent;
  let fixture: ComponentFixture<OrganisationenDropdownControlComponent>;

  beforeEach(() => {
    return MockBuilder(OrganisationenDropdownControlComponent, EditorModule);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OrganisationenDropdownControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('filterOptions', () => {
    it('should filter options on type', (done: DoneFn) => {
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

      component.ngOnChanges();

      // der erste Wert wird übersprungen, da ngOnChanges mit startWith arbeitet
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
        done();
      });

      component.formControl.patchValue('Tes');
    });

    it('should emit all if value null', (done: DoneFn) => {
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
      component.ngOnChanges();

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
        done();
      });
      component.formControl.patchValue(null);
    });

    it('should emit all if value is Organisation', (done: DoneFn) => {
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
      component.ngOnChanges();

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

      component.ngOnChanges();

      component.filteredOptions$.subscribe(filteredOptions$ => {
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

      component.ngOnChanges();

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
        done();
      });
    });
  });
});
