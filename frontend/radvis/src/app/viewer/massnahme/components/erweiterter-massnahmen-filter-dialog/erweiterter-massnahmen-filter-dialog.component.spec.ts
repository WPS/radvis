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

import { waitForAsync } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MockBuilder, MockedComponentFixture, MockRender, ngMocks } from 'ng-mocks';
import { OrganisationenDropdownControlComponent } from 'src/app/shared/components/organisationen-dropdown-control/organisationen-dropdown-control.component';
import {
  defaultBundeslandOrganisation,
  defaultGemeinden,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { ErweiterterMassnahmenFilterDialogComponent } from 'src/app/viewer/massnahme/components/erweiterter-massnahmen-filter-dialog/erweiterter-massnahmen-filter-dialog.component';
import { MassnahmeModule } from 'src/app/viewer/massnahme/massnahme.module';
import { ErweiterterMassnahmenFilter } from 'src/app/viewer/massnahme/models/erweiterter-massnahmen-filter';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { testFahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view-test-data-provider.spec';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(ErweiterterMassnahmenFilterDialogComponent.name, () => {
  let component: ErweiterterMassnahmenFilterDialogComponent;
  let fixture: MockedComponentFixture<ErweiterterMassnahmenFilterDialogComponent>;

  let organisationService: OrganisationenService;
  let matDialogRef: MatDialogRef<ErweiterterMassnahmenFilterDialogComponent, ErweiterterMassnahmenFilter>;

  const initialFilterData: ErweiterterMassnahmenFilter = {
    historischeMassnahmenAnzeigen: true,
    fahrradrouteFilter: {
      fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
      fahrradroute: testFahrradrouteListenView[1],
      fahrradroutenIds: [2],
    },
    organisation: defaultGemeinden[0],
  };

  beforeEach(() => {
    organisationService = mock(OrganisationenService);
    matDialogRef = mock(MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>);

    when(organisationService.getOrganisationen()).thenResolve([defaultBundeslandOrganisation, ...defaultGemeinden]);

    return MockBuilder(ErweiterterMassnahmenFilterDialogComponent, MassnahmeModule)
      .provide({ provide: OrganisationenService, useValue: instance(organisationService) })
      .provide({
        provide: MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>,
        useValue: instance(matDialogRef),
      })
      .provide({ provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) })
      .provide({ provide: MAT_DIALOG_DATA, useValue: initialFilterData });
  });

  beforeEach(waitForAsync(() => {
    fixture = MockRender(ErweiterterMassnahmenFilterDialogComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  }));

  it('should prefill form controls with current filter values', () => {
    expect(component.formGroup.value.historischeMassnahmenAusblenden).toEqual(
      !initialFilterData.historischeMassnahmenAnzeigen
    );
    expect(component.formGroup.value.fahrradrouteFilter).toEqual(initialFilterData.fahrradrouteFilter);
    expect(component.formGroup.value.organisation).toEqual(initialFilterData.organisation);
  });

  it('should exclude bundesland from organisation filter', () => {
    const organisationenControl: OrganisationenDropdownControlComponent = ngMocks.find(
      OrganisationenDropdownControlComponent
    ).componentInstance;
    expect(organisationenControl.options).not.toContain(defaultBundeslandOrganisation);
  });

  describe('onSave', () => {
    beforeEach(() => {
      component.formGroup.reset();
    });

    it('should return historischeMassnahmenAnzeigen', () => {
      component.formGroup.patchValue({
        historischeMassnahmenAusblenden: false,
      });

      component.onSave();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        historischeMassnahmenAnzeigen: true,
        fahrradrouteFilter: null,
        organisation: null,
      });
    });

    it('should return fahrradrouteFilter', () => {
      const expectedValue = {
        fahrradroute: null,
        fahrradroutenIds: [1],
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
      };
      component.formGroup.patchValue({
        fahrradrouteFilter: expectedValue,
      });

      component.onSave();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilter: expectedValue,
        organisation: null,
      });
    });

    it('should return verwaltungseinheit', () => {
      component.formGroup.patchValue({
        organisation: defaultGemeinden[0],
      });

      component.onSave();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilter: null,
        organisation: defaultGemeinden[0],
      });
    });
  });
});
