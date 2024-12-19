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

/* eslint-disable @typescript-eslint/dot-notation */
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
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { ErweiterterMassnahmenFilterDialogComponent } from 'src/app/viewer/massnahme/components/erweiterter-massnahmen-filter-dialog/erweiterter-massnahmen-filter-dialog.component';
import { ErweiterterMassnahmenFilter } from 'src/app/viewer/massnahme/models/erweiterter-massnahmen-filter';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/massnahme/models/fahrradroute-filter-kategorie';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { testFahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view-test-data-provider.spec';
import { FahrradroutenProviderService } from 'src/app/viewer/viewer-shared/services/fahrradrouten-provider.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(ErweiterterMassnahmenFilterDialogComponent.name + ' - no initial fahrradroute filter', () => {
  let component: ErweiterterMassnahmenFilterDialogComponent;
  let fixture: MockedComponentFixture<ErweiterterMassnahmenFilterDialogComponent>;

  let organisationService: OrganisationenService;
  let fahrradrouteService: FahrradrouteService;
  let matDialogRef: MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>;

  const initialFilterData: ErweiterterMassnahmenFilter = {
    historischeMassnahmenAnzeigen: true,
    fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
    fahrradroute: null,
    organisation: defaultGemeinden[0],
    fahrradroutenIds: [2],
  };

  beforeEach(() => {
    organisationService = mock(OrganisationenService);
    fahrradrouteService = mock(FahrradrouteService);
    matDialogRef = mock(MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>);

    when(organisationService.getOrganisationen()).thenResolve([defaultBundeslandOrganisation, ...defaultGemeinden]);
    when(fahrradrouteService.getAll()).thenResolve(testFahrradrouteListenView);

    return MockBuilder(ErweiterterMassnahmenFilterDialogComponent, ViewerModule)
      .provide({ provide: OrganisationenService, useValue: instance(organisationService) })
      .provide({ provide: FahrradroutenProviderService, useValue: instance(fahrradrouteService) })
      .provide({ provide: MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>, useValue: instance(matDialogRef) })
      .provide({ provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) })
      .provide({ provide: MAT_DIALOG_DATA, useValue: initialFilterData });
  });

  beforeEach(waitForAsync(() => {
    fixture = MockRender(ErweiterterMassnahmenFilterDialogComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  }));

  it('should disable fahrradroute control', () => {
    expect(component.formGroup.controls.fahrradroute.enabled).toBe(false);
  });
});

describe(ErweiterterMassnahmenFilterDialogComponent.name, () => {
  let component: ErweiterterMassnahmenFilterDialogComponent;
  let fixture: MockedComponentFixture<ErweiterterMassnahmenFilterDialogComponent>;

  let organisationService: OrganisationenService;
  let fahrradrouteService: FahrradrouteService;
  let matDialogRef: MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>;

  const initialFilterData: ErweiterterMassnahmenFilter = {
    historischeMassnahmenAnzeigen: true,
    fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
    fahrradroute: testFahrradrouteListenView[1],
    organisation: defaultGemeinden[0],
    fahrradroutenIds: [2],
  };

  beforeEach(() => {
    organisationService = mock(OrganisationenService);
    fahrradrouteService = mock(FahrradrouteService);
    matDialogRef = mock(MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>);

    when(organisationService.getOrganisationen()).thenResolve([defaultBundeslandOrganisation, ...defaultGemeinden]);
    when(fahrradrouteService.getAll()).thenResolve(testFahrradrouteListenView);

    return MockBuilder(ErweiterterMassnahmenFilterDialogComponent, ViewerModule)
      .provide({ provide: OrganisationenService, useValue: instance(organisationService) })
      .provide({ provide: FahrradroutenProviderService, useValue: instance(fahrradrouteService) })
      .provide({ provide: MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>, useValue: instance(matDialogRef) })
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
    expect(component.formGroup.value.fahrradrouteFilterKategorie).toEqual(
      initialFilterData.fahrradrouteFilterKategorie
    );
    expect(component.formGroup.value.fahrradroute).toEqual(initialFilterData.fahrradroute);
    expect(component.formGroup.value.organisation).toEqual(initialFilterData.organisation);
    expect(component.formGroup.controls.fahrradroute.enabled).toBe(true);
  });

  it('should exclude bundesland from organisation filter', () => {
    const organisationenControl: OrganisationenDropdownControlComponent = ngMocks.find(
      OrganisationenDropdownControlComponent
    ).componentInstance;
    expect(organisationenControl.options).not.toContain(defaultBundeslandOrganisation);
  });

  describe('onFahrradrouteFilterKategorieChanged', () => {
    Object.values(FahrradrouteFilterKategorie).forEach(filterKategorie => {
      it(`should always reset the fahrradroute FormControl: ${filterKategorie}`, () => {
        component.formGroup.controls.fahrradroute.reset(testFahrradrouteListenView[0]);
        component.formGroup.controls.fahrradrouteFilterKategorie.setValue(
          filterKategorie as FahrradrouteFilterKategorie
        );
        expect(component.formGroup.controls.fahrradroute.value).toBeFalsy();
      });

      it(`should update isFahrradrouteEnabled and the fahrradrouteFormControl appropriately: ${filterKategorie}`, () => {
        component.formGroup.controls.fahrradrouteFilterKategorie.setValue(
          filterKategorie as FahrradrouteFilterKategorie
        );

        const fahrradrouteControl = component.formGroup.controls.fahrradroute;
        const fahrradrouteEnabled = filterKategorie === FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE;
        expect(fahrradrouteControl.enabled).toBe(fahrradrouteEnabled);
      });
    });
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
        fahrradrouteFilterKategorie: null,
        fahrradroute: undefined,
        fahrradroutenIds: [],
        organisation: null,
      });
    });

    it('should return IDs from D-Routen', () => {
      component.formGroup.patchValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
      });
      const expectedIds = testFahrradrouteListenView
        .filter(item => item.fahrradrouteKategorie === FahrradrouteKategorie.D_ROUTE)
        .map(item => item.id);
      expect(expectedIds.length > 0).toBe(true);

      component.onSave();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
        fahrradroute: undefined,
        fahrradroutenIds: expectedIds,
        organisation: null,
      });
    });

    it('should return IDs from Landesradfernwege', () => {
      component.formGroup.patchValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
      });
      const expectedIds = testFahrradrouteListenView
        .filter(item => item.fahrradrouteKategorie === FahrradrouteKategorie.LANDESRADFERNWEG)
        .map(item => item.id);
      expect(expectedIds.length > 0).toBe(true);

      component.onSave();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
        fahrradroute: undefined,
        fahrradroutenIds: expectedIds,
        organisation: null,
      });
    });

    it('should return IDs from alle fahrradrouten', () => {
      component.formGroup.patchValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_FAHRRADROUTEN,
      });

      component.onSave();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_FAHRRADROUTEN,
        fahrradroute: undefined,
        fahrradroutenIds: testFahrradrouteListenView.map(f => f.id),
        organisation: null,
      });
    });

    it('should return no fahrradroute', () => {
      component.onSave();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilterKategorie: null,
        fahrradroute: undefined,
        fahrradroutenIds: [],
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
        fahrradrouteFilterKategorie: null,
        fahrradroute: undefined,
        fahrradroutenIds: [],
        organisation: defaultGemeinden[0],
      });
    });

    it('should return einzelne fahrradroute', () => {
      component.formGroup.patchValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
        fahrradroute: testFahrradrouteListenView[0],
      });

      component.onSave();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
        fahrradroute: testFahrradrouteListenView[0],
        fahrradroutenIds: [testFahrradrouteListenView[0].id],
        organisation: null,
      });
    });
  });
});
