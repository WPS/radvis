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

import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Data, RouterModule } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { BarriereModule } from 'src/app/viewer/barriere/barriere.module';
import { Barriere } from 'src/app/viewer/barriere/models/barriere';
import { BarriereFormDetails } from 'src/app/viewer/barriere/models/barriere-form-details';
import { defaultBarriere, otherBarriere } from 'src/app/viewer/barriere/models/barriere-test-data-provider.spec';
import { SaveBarriereCommand } from 'src/app/viewer/barriere/models/save-barriere-command';
import { BarrierenService } from 'src/app/viewer/barriere/services/barrieren.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { BarriereEditorComponent } from './barriere-editor.component';

describe(BarriereEditorComponent.name, () => {
  let component: BarriereEditorComponent;
  let fixture: MockedComponentFixture<BarriereEditorComponent>;
  let data$: BehaviorSubject<Data>;
  let barrierenService: BarrierenService;

  beforeEach(() => {
    data$ = new BehaviorSubject<Data>({ isCreator: true });
    barrierenService = mock(BarrierenService);

    return MockBuilder(BarriereEditorComponent, BarriereModule)
      .replace(RouterModule, RouterTestingModule)
      .provide({
        provide: OlMapService,
        useValue: instance(mock(OlMapComponent)),
      })
      .provide({
        provide: ActivatedRoute,
        useValue: {
          data: data$,
          snapshot: {
            paramMap: convertToParamMap({}),
          },
        },
      })
      .provide({
        provide: BarrierenService,
        useValue: instance(barrierenService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(BarriereEditorComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('as Creator', () => {
    beforeEach(() => {
      component.formGroup.patchValue({
        ...defaultBarriere,
      });
      component.formGroup.markAsDirty();
      data$.next({ isCreator: true });
    });

    describe('fillForm', () => {
      it('should reset', () => {
        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: null,
          verantwortlicheOrganisation: null,
          barrierenForm: null,
          verbleibendeDurchfahrtsbreite: null,
          sicherung: null,
          markierung: null,
          begruendung: null,
        });
      });
    });

    describe('onSave', () => {
      it('should create correct command', () => {
        component.formGroup.patchValue({
          ...otherBarriere,
        });
        component.formGroup.markAsDirty();
        when(barrierenService.createBarriere(anything())).thenResolve(1);

        component.onSave();

        // eslint-disable-next-line no-unused-vars
        const { version, darfBenutzerBearbeiten, ...otherBarriereCommandValues } = otherBarriere;
        const createCommand: SaveBarriereCommand = {
          ...otherBarriereCommandValues,
          verantwortlicheOrganisation: otherBarriere.verantwortlicheOrganisation.id,
        };
        expect(capture(barrierenService.createBarriere).last()[0]).toEqual(createCommand);
      });
    });

    describe('onReset', () => {
      it('should reset', () => {
        component.formGroup.patchValue({
          ...otherBarriere,
        });
        component.formGroup.markAsDirty();

        component.onReset();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.value).toEqual({
          netzbezug: null,
          verantwortlicheOrganisation: null,
          barrierenForm: null,
          verbleibendeDurchfahrtsbreite: null,
          sicherung: null,
          markierung: null,
          begruendung: null,
        });
      });
    });
  });

  describe('as Editor', () => {
    // eslint-disable-next-line no-unused-vars
    const { version, darfBenutzerBearbeiten, ...defaultBarriereFormValues } = defaultBarriere;
    const {
      // eslint-disable-next-line no-unused-vars
      version: versionOther,
      // eslint-disable-next-line no-unused-vars
      darfBenutzerBearbeiten: darfBenutzerBearbeitenOther,
      ...otherBarriereFormValues
    } = otherBarriere;

    beforeEach(() => {
      component.formGroup.patchValue({
        ...otherBarriere,
      });
      component.formGroup.markAsDirty();

      data$.next({ isCreator: false, barriere: defaultBarriere });
    });

    describe('fillForm', () => {
      it('should reset', () => {
        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.getRawValue()).toEqual(defaultBarriereFormValues);
        expect(component.formGroup.controls.barriereFormDetails.disabled).toBeTrue();
      });

      it('should enable barriereFormDetails', () => {
        const barriere: Barriere = {
          ...defaultBarriere,
          barrierenForm: 'SPERRPFOSTEN',
          barriereFormDetails: BarriereFormDetails.SPERRPFOSTEN_GESICHERT,
        };
        data$.next({
          isCreator: false,
          barriere: barriere,
        });

        expect(component.formGroup.controls.barriereFormDetails.enabled).toBeTrue();
      });
    });

    describe('onBarriereFormChanged', () => {
      it('should reset barriereFormDetails', () => {
        component.formGroup.patchValue({
          barrierenForm: 'SPERRPFOSTEN',
          barriereFormDetails: BarriereFormDetails.SPERRPFOSTEN_GESICHERT,
        });

        component.formGroup.controls.barrierenForm.setValue('ANORDNUNG_VZ_220');

        expect(component.formGroup.controls.barriereFormDetails.value).toBeFalsy();
      });

      it('should enable barriereFormDetails', () => {
        component.formGroup.controls.barriereFormDetails.disable();

        component.formGroup.patchValue({
          barrierenForm: 'SPERRPFOSTEN',
        });

        expect(component.formGroup.controls.barriereFormDetails.enabled).toBeTrue();
      });

      it('should disable barriereFormDetails', () => {
        component.formGroup.controls.barriereFormDetails.enable();

        component.formGroup.patchValue({
          barrierenForm: 'ANORDNUNG_VZ_220',
        });

        expect(component.formGroup.controls.barriereFormDetails.enabled).toBeFalse();
      });

      it('should reset barriereFormDetailsOptions', () => {
        component.barriereFormDetailsOptions = [];

        component.formGroup.patchValue({
          barrierenForm: 'SPERRPFOSTEN',
        });

        expect(component.barriereFormDetailsOptions).toEqual(
          BarriereFormDetails.getOptionsForBarriereForm('SPERRPFOSTEN')
        );
      });

      it('should handle null correctly', () => {
        component.formGroup.patchValue({
          barrierenForm: 'SPERRPFOSTEN',
          barriereFormDetails: BarriereFormDetails.SPERRPFOSTEN_GESICHERT,
        });

        component.formGroup.controls.barrierenForm.reset();

        expect(component.barriereFormDetailsOptions).toEqual([]);
        expect(component.formGroup.controls.barriereFormDetails.enabled).toBeFalse();
        expect(component.formGroup.controls.barriereFormDetails.value).toBeFalsy();
      });
    });

    describe('onSave', () => {
      it('should create correct command', () => {
        component.formGroup.patchValue({
          ...otherBarriere,
        });

        component.formGroup.markAsDirty();
        when(barrierenService.updateBarriere(anything(), anything())).thenResolve({
          ...otherBarriere,
          version: 3,
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();

        const updateCommand: SaveBarriereCommand = {
          netzbezug: otherBarriere.netzbezug,
          version: otherBarriere.version,
          barrierenForm: otherBarriere.barrierenForm,
          verbleibendeDurchfahrtsbreite: otherBarriere.verbleibendeDurchfahrtsbreite,
          sicherung: otherBarriere.sicherung,
          markierung: otherBarriere.markierung,
          begruendung: otherBarriere.begruendung,
          verantwortlicheOrganisation: otherBarriere.verantwortlicheOrganisation.id,
          barriereFormDetails: otherBarriere.barriereFormDetails,
        };

        verify(barrierenService.updateBarriere(anything(), anything())).once();
        expect(capture(barrierenService.updateBarriere).last()[0]).toEqual(2345);
        expect(capture(barrierenService.updateBarriere).last()[1]).toEqual(updateCommand);
      });

      it('should create correct command with barriereFormDetails', () => {
        component.formGroup.patchValue({
          ...otherBarriere,
          barrierenForm: 'SPERRPFOSTEN',
          barriereFormDetails: BarriereFormDetails.SPERRPFOSTEN_GESICHERT,
        });

        component.formGroup.markAsDirty();
        when(barrierenService.updateBarriere(anything(), anything())).thenResolve({
          ...otherBarriere,
          version: 3,
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();

        verify(barrierenService.updateBarriere(anything(), anything())).once();
        expect(capture(barrierenService.updateBarriere).last()[1].barriereFormDetails).toEqual(
          BarriereFormDetails.SPERRPFOSTEN_GESICHERT
        );
      });

      it('should reset form', fakeAsync(() => {
        component.formGroup.markAsDirty();
        when(barrierenService.updateBarriere(anything(), anything())).thenResolve({
          ...otherBarriere,
          version: 3,
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();
        tick();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.getRawValue()).toEqual({
          ...otherBarriereFormValues,
        });
        expect(component.formGroup.controls.barriereFormDetails.disabled).toBeTrue();
      }));

      it('should use correct version', fakeAsync(() => {
        component.formGroup.markAsDirty();
        when(barrierenService.updateBarriere(anything(), anything())).thenResolve({
          ...otherBarriere,
          version: 3,
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();
        tick();
        verify(barrierenService.updateBarriere(anything(), anything())).once();
        expect(capture(barrierenService.updateBarriere).last()[1].version).toEqual(2);

        component.onSave();
        tick();
        verify(barrierenService.updateBarriere(anything(), anything())).twice();
        expect(capture(barrierenService.updateBarriere).last()[1].version).toEqual(3);
      }));
    });

    describe('onReset', () => {
      it('should reset form', () => {
        component.formGroup.patchValue({
          ...otherBarriere,
        });

        component.formGroup.markAsDirty();

        component.onReset();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.getRawValue()).toEqual(defaultBarriereFormValues);
        expect(component.formGroup.controls.barriereFormDetails.disabled).toBeTrue();
      });

      it('should reset after previous save', fakeAsync(() => {
        component.formGroup.markAsDirty();
        when(barrierenService.updateBarriere(anything(), anything())).thenResolve({
          ...otherBarriere,
          version: 3,
        });
        spyOnProperty(component, 'selectedId').and.returnValue(2345);

        component.onSave();
        tick();

        component.formGroup.patchValue({
          ...defaultBarriere,
        });

        component.formGroup.markAsDirty();

        component.onReset();

        expect(component.formGroup.dirty).toBeFalse();
        expect(component.formGroup.getRawValue()).toEqual(otherBarriereFormValues);
        expect(component.formGroup.controls.barriereFormDetails.disabled).toBeTrue();
      }));
    });
  });
});
