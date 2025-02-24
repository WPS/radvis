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
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { FahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view';
import { testFahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view-test-data-provider.spec';
import { FahrradroutenProviderService } from 'src/app/viewer/viewer-shared/services/fahrradrouten-provider.service';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { instance, mock, when } from 'ts-mockito';
import { FahrradrouteFilterAuswahlControlComponent } from './fahrradroute-filter-auswahl-control.component';

describe(FahrradrouteFilterAuswahlControlComponent.name, () => {
  let component: FahrradrouteFilterAuswahlControlComponent;
  let fixture: MockedComponentFixture<FahrradrouteFilterAuswahlControlComponent>;
  let fahrradrouteService: FahrradrouteService;

  beforeEach(() => {
    fahrradrouteService = mock(FahrradrouteService);
    when(fahrradrouteService.getAll()).thenResolve(testFahrradrouteListenView);
    return MockBuilder(FahrradrouteFilterAuswahlControlComponent, ViewerSharedModule).provide({
      provide: FahrradroutenProviderService,
      useValue: instance(fahrradrouteService),
    });
  });

  beforeEach(() => {
    fixture = MockRender(FahrradrouteFilterAuswahlControlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('writeValue', () => {
    let valueChangeSpy: jasmine.Spy;
    beforeEach(() => {
      valueChangeSpy = spyOn(component, 'onChange');
    });

    it('should reset if null', () => {
      component.formGroup.patchValue(
        { fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW },
        { emitEvent: false }
      );
      component.writeValue(null);

      expect(component.formGroup.getRawValue()).toEqual({ fahrradroute: null, fahrradrouteFilterKategorie: null });
      expect(valueChangeSpy).not.toHaveBeenCalled();
      expect(component.formGroup.controls.fahrradroute.enabled).toBeFalse();
    });

    it('should set value to form', () => {
      component.writeValue({
        fahrradroute: null,
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_FAHRRADROUTEN,
        fahrradroutenIds: [],
      });

      expect(component.formGroup.getRawValue()).toEqual({
        fahrradroute: null,
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_FAHRRADROUTEN,
      });
      expect(valueChangeSpy).not.toHaveBeenCalled();
      expect(component.formGroup.controls.fahrradroute.enabled).toBeFalse();
    });

    it('should set einzelne Fahrradroute correct', () => {
      component.writeValue({
        fahrradroute: { id: 1 } as FahrradrouteListenView,
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
        fahrradroutenIds: [1],
      });

      expect(component.formGroup.value).toEqual({
        fahrradroute: { id: 1 } as FahrradrouteListenView,
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
      });
      expect(valueChangeSpy).not.toHaveBeenCalled();
      expect(component.formGroup.controls.fahrradroute.enabled).toBeTrue();
    });
  });

  describe('onFahrradrouteFilterKategorieChanged', () => {
    FahrradrouteFilterKategorie.options.forEach(option => {
      it(`should always reset the fahrradroute FormControl: ${option.name}`, () => {
        component.formGroup.controls.fahrradroute.reset(testFahrradrouteListenView[0]);
        component.formGroup.controls.fahrradrouteFilterKategorie.setValue(option.name as FahrradrouteFilterKategorie);
        expect(component.formGroup.controls.fahrradroute.value).toBeFalsy();
      });

      it(`should update isFahrradrouteEnabled and the fahrradrouteFormControl appropriately: ${option.name}`, () => {
        component.formGroup.controls.fahrradrouteFilterKategorie.setValue(option.name as FahrradrouteFilterKategorie);

        const fahrradrouteControl = component.formGroup.controls.fahrradroute;
        const fahrradrouteEnabled = option.name === FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE;
        expect(fahrradrouteControl.enabled).toBe(fahrradrouteEnabled);
      });
    });
  });

  describe('setDisabledState', () => {
    let valueChangeSpy: jasmine.Spy;
    beforeEach(() => {
      valueChangeSpy = spyOn(component, 'onChange');
    });

    it('should disable', () => {
      component.setDisabledState(true);
      expect(component.formGroup.disabled).toBeTrue();
      expect(valueChangeSpy).not.toHaveBeenCalled();
    });

    it('should enable all if einzelne Fahrradroute', () => {
      component.writeValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
        fahrradroute: testFahrradrouteListenView[0],
        fahrradroutenIds: [],
      });
      component.setDisabledState(false);
      expect(component.formGroup.disabled).toBeFalse();
      expect(component.formGroup.controls.fahrradroute.disabled).toBeFalse();
      expect(valueChangeSpy).not.toHaveBeenCalled();
    });

    it('should enable all except fahrradroute if not einzelne Fahrradroute', () => {
      component.writeValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
        fahrradroute: null,
        fahrradroutenIds: [],
      });
      component.setDisabledState(false);
      expect(component.formGroup.disabled).toBeFalse();
      expect(component.formGroup.controls.fahrradroute.disabled).toBeTrue();
      expect(valueChangeSpy).not.toHaveBeenCalled();
    });
  });

  describe('onChange', () => {
    let valueChangeSpy: jasmine.Spy;
    beforeEach(() => {
      valueChangeSpy = spyOn(component, 'onChange');
      component.alleFahrradrouten = [
        {
          ...testFahrradrouteListenView[0],
          id: 1,
          fahrradrouteKategorie: FahrradrouteKategorie.LANDESRADFERNWEG,
        },
        {
          ...testFahrradrouteListenView[0],
          id: 2,
          fahrradrouteKategorie: FahrradrouteKategorie.D_ROUTE,
        },
      ];
    });

    it('should return IDs from D-Routen', () => {
      component.formGroup.patchValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
      });

      expect(valueChangeSpy).toHaveBeenCalled();
      expect(valueChangeSpy.calls.mostRecent().args[0]).toEqual({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
        fahrradroute: null,
        fahrradroutenIds: [2],
      });
    });

    it('should return IDs from Landesradfernwege', () => {
      component.formGroup.patchValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
      });

      expect(valueChangeSpy).toHaveBeenCalled();
      expect(valueChangeSpy.calls.mostRecent().args[0]).toEqual({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
        fahrradroute: null,
        fahrradroutenIds: [1],
      });
    });

    it('should return IDs from alle fahrradrouten', () => {
      component.formGroup.patchValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_FAHRRADROUTEN,
      });

      expect(valueChangeSpy).toHaveBeenCalled();
      expect(valueChangeSpy.calls.mostRecent().args[0]).toEqual({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_FAHRRADROUTEN,
        fahrradroute: null,
        fahrradroutenIds: [1, 2],
      });
    });

    it('should return no fahrradroute', () => {
      component.formGroup.patchValue({
        fahrradrouteFilterKategorie: null,
      });

      expect(valueChangeSpy).toHaveBeenCalled();
      expect(valueChangeSpy.calls.mostRecent().args[0]).toEqual({
        fahrradrouteFilterKategorie: null,
        fahrradroute: null,
        fahrradroutenIds: [],
      });
    });

    it('should return einzelne fahrradroute', () => {
      component.formGroup.patchValue({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
        fahrradroute: component.alleFahrradrouten[1],
      });

      expect(valueChangeSpy).toHaveBeenCalled();
      expect(valueChangeSpy.calls.mostRecent().args[0]).toEqual({
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
        fahrradroute: component.alleFahrradrouten[1],
        fahrradroutenIds: [2],
      });
    });
  });
});
