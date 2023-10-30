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

import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, ActivatedRouteSnapshot, Router } from '@angular/router';
import { MockBuilder, MockRender } from 'ng-mocks';
import { ManuellerImportModule } from 'src/app/editor/manueller-import/manueller-import.module';
import { AttributeImportFormat } from 'src/app/editor/manueller-import/models/attribute-import-format';
import { AttributeParameter } from 'src/app/editor/manueller-import/models/attribute-parameter';
import { DateiUploadInfo } from 'src/app/editor/manueller-import/models/datei-upload-info';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { StartAttributeImportSessionCommand } from 'src/app/editor/manueller-import/models/start-attribute-import-session-command';
import { CreateSessionStateService } from 'src/app/editor/manueller-import/services/create-session.state.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { ImportAttributeParameterEingebenComponent } from './import-attribute-parameter-eingeben.component';

describe('ImportAttributeParameterEingebenComponent', () => {
  let component: ImportAttributeParameterEingebenComponent;
  let fixture: ComponentFixture<ImportAttributeParameterEingebenComponent>;
  let createSessionStateService: CreateSessionStateService;
  let manuellerImportService: ManuellerImportService;

  beforeEach(() => {
    createSessionStateService = mock(CreateSessionStateService);
    manuellerImportService = mock(ManuellerImportService);
    const activatedRoute = mock(ActivatedRoute);
    when(activatedRoute.snapshot).thenReturn(({
      data: {
        step: 2,
      },
    } as unknown) as ActivatedRouteSnapshot);
    when(manuellerImportService.existsImportSession()).thenResolve(false);
    when(createSessionStateService.dateiUploadInfo).thenReturn(
      DateiUploadInfo.of(ImportTyp.ATTRIBUTE_UEBERNEHMEN, instance(mock(File)), 1)
    );
    when(createSessionStateService.attributeImportFormat).thenReturn(AttributeImportFormat.LUBW);
    when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([]);
    return MockBuilder(ImportAttributeParameterEingebenComponent, ManuellerImportModule)
      .provide({
        provide: ActivatedRoute,
        useValue: instance(activatedRoute),
      })
      .provide({
        provide: Router,
        useValue: instance(mock(Router)),
      })
      .provide({
        provide: CreateSessionStateService,
        useValue: instance(createSessionStateService),
      })
      .provide({
        provide: ManuellerImportService,
        useValue: instance(manuellerImportService),
      });
  });

  beforeEach(() => {
    recreateComponent();
  });

  const recreateComponent = (): void => {
    fixture = MockRender(ImportAttributeParameterEingebenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  describe('attribute form', () => {
    beforeEach(() => {
      when(createSessionStateService.parameterInfo).thenReturn(null);
    });

    it('should disable and deselect invalid attribute', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'ValidAttribut',
          attributDisplayName: 'DisplayNameValidAttribut',
          radvisName: 'Test1',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'InvalidAttribut',
          attributDisplayName: 'DisplayNameInValidAttribut',
          radvisName: 'Test2',
          valid: false,
          ungueltigeWerte: ['InvalidAttribut'],
        },
      ]);
      recreateComponent();
      tick();

      expect(component.formArray.length).toBe(2);
      // formArray[1] == ValidAttribut, da alphabetisch sortiert wird
      expect(component.formArray.at(1).disabled).toBeFalse();
      expect(component.formArray.at(0).disabled).toBeTrue();
      expect(component.formArray.at(1).value.selected).toBeTrue();
      expect(component.formArray.at(0).value.selected).toBeFalse();
      expect(component.formArray.value.length).toBe(1);
    }));

    it('should select all on initialize', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'ValidAttribut',
          attributDisplayName: 'DisplayNameValidAttribut',
          radvisName: 'Test1',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'InvalidAttribut',
          attributDisplayName: 'DisplayNameInValidAttribut',
          radvisName: 'Test2',
          valid: true,
          ungueltigeWerte: [],
        },
      ]);
      recreateComponent();
      tick();

      expect(component.formArray.value.map((v: any) => v.selected)).toEqual([true, true]);
    }));

    it('should update state on initialize', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'ValidAttribut',
          radvisName: 'Test1',
          attributDisplayName: 'DisplayNameValidAttribut',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'InvalidAttribut',
          radvisName: 'Test2',
          attributDisplayName: 'DisplayNameInValidAttribut',
          valid: true,
          ungueltigeWerte: [],
        },
      ]);
      recreateComponent();
      tick();

      verify(createSessionStateService.updateParameterInfo(anything())).once();
      // formArray ist alphabetisch sortiert, daher auch die Parameter-Liste hier
      expect(capture(createSessionStateService.updateParameterInfo).last()[0]).toEqual(
        AttributeParameter.of(['InvalidAttribut', 'ValidAttribut'])
      );
    }));

    describe('Alle an- und Abwählen', () => {
      it('should disable all on button click', fakeAsync(() => {
        when(createSessionStateService.parameterInfo).thenReturn(AttributeParameter.of([]));
        when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
          {
            attributName: 'ValidAttribut1',
            attributDisplayName: 'DisplayNameValidAttribut',
            radvisName: 'Test1',
            valid: true,
            ungueltigeWerte: [],
          },
          {
            attributName: 'ValidAttribut2',
            attributDisplayName: 'DisplayNameInValidAttribut',
            radvisName: 'Test2',
            valid: true,
            ungueltigeWerte: [],
          },
        ]);
        recreateComponent();
        tick();

        component.onAlleAttributeAbwaehlen();
        tick();

        expect(component.formArray.value.map((v: any) => v.selected)).toEqual([false, false]);
      }));

      it('should enable all on button click, but not invalids', fakeAsync(() => {
        when(createSessionStateService.parameterInfo).thenReturn(AttributeParameter.of([]));
        when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
          {
            attributName: 'ValidAttribut1',
            attributDisplayName: 'DisplayNameValidAttribut1',
            radvisName: 'Test1',
            valid: true,
            ungueltigeWerte: [],
          },
          {
            attributName: 'ValidAttribut2',
            attributDisplayName: 'DisplayNameValidAttribut2',
            radvisName: 'Test2',
            valid: true,
            ungueltigeWerte: [],
          },
          {
            attributName: 'InvalidAttribut',
            attributDisplayName: 'DisplayNameInValidAttribut',
            radvisName: 'Test3',
            valid: false,
            ungueltigeWerte: ['InvalidAttribut'],
          },
        ]);
        recreateComponent();
        tick();

        component.onAlleAttributeAuswaehlen();
        tick();

        expect(component.formArray.value.map((v: any) => v.selected)).toEqual([true, true]);
        // formArray[0] == InvalidAttribut, da alphabetisch sortiert wird
        expect(component.formArray.at(0).disabled).toBeTrue();
        expect(component.formArray.at(0).value.selected).toBeFalse();
      }));
    });
  });

  describe('invalidAttributesPresent', () => {
    it('should be true', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'ValidAttribut',
          attributDisplayName: 'DisplayNameValidAttribut',
          radvisName: 'Test1',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'InvalidAttribut',
          attributDisplayName: 'DisplayNameInValidAttribut',
          radvisName: 'Test2',
          valid: false,
          ungueltigeWerte: ['InvalidAttribut'],
        },
      ]);
      recreateComponent();
      tick();

      expect(component.invalidAttributesPresent).toBeTrue();
    }));

    it('should be false if no attribute', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([]);
      recreateComponent();
      tick();

      expect(component.invalidAttributesPresent).toBeFalse();
    }));

    it('should be false if all valid', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'ValidAttribut',
          attributDisplayName: 'ValidAttributDisplayName',
          radvisName: 'Test1',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'InvalidAttribut',
          attributDisplayName: 'InvalidAttributDisplayName',
          radvisName: 'Test2',
          valid: true,
          ungueltigeWerte: [],
        },
      ]);
      recreateComponent();
      tick();

      expect(component.invalidAttributesPresent).toBeFalse();
    }));
  });

  describe('creationState', () => {
    const attribute = ['Attribut1', 'Attribut2'];
    beforeEach(() => {
      when(createSessionStateService.parameterInfo).thenReturn(AttributeParameter.of(attribute));
    });

    it('should fill attribute from creation service', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'Attribut1',
          attributDisplayName: 'Attribut1DisplayName',
          radvisName: 'Test1',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'Attribut2',
          attributDisplayName: 'Attribut2DisplayName',
          radvisName: 'Test2',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'Attribut3',
          attributDisplayName: 'Attribut3DisplayName',
          radvisName: 'Test3',
          valid: true,
          ungueltigeWerte: [],
        },
      ]);
      recreateComponent();
      tick();

      expect(component.formArray.length).toBe(3);
      expect(component.formArray.value.filter((v: any) => v.selected).map((v: any) => v.attributName)).toEqual(
        attribute
      );
    }));

    it('should filter attribute from creation service and update state', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'Attribut2',
          attributDisplayName: 'Attribut2DisplayName',
          radvisName: 'Test2',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'Attribut3',
          attributDisplayName: 'Attribut3DisplayName',
          radvisName: 'Test3',
          valid: true,
          ungueltigeWerte: [],
        },
      ]);
      recreateComponent();
      tick();

      expect(component.formArray.length).toBe(2);
      expect(component.formArray.value.filter((v: any) => v.selected).map((v: any) => v.attributName)).toEqual([
        'Attribut2',
      ]);
      verify(createSessionStateService.updateParameterInfo(anything())).once();
      expect(capture(createSessionStateService.updateParameterInfo).last()[0]).toEqual(
        AttributeParameter.of(['Attribut2'])
      );
    }));

    it('should filter invalid attribute from creation service and update state', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'Attribut1',
          attributDisplayName: 'Attribut1DisplayName',
          radvisName: 'Test2',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'Attribut2',
          attributDisplayName: 'Attribut2DisplayName',
          radvisName: 'Test3',
          valid: false,
          ungueltigeWerte: ['Attribut2'],
        },
      ]);
      recreateComponent();
      tick();

      expect(component.formArray.length).toBe(2);
      expect(component.formArray.value.filter((v: any) => v.selected).map((v: any) => v.attributName)).toEqual([
        'Attribut1',
      ]);
      verify(createSessionStateService.updateParameterInfo(anything())).once();
      expect(capture(createSessionStateService.updateParameterInfo).last()[0]).toEqual(
        AttributeParameter.of(['Attribut1'])
      );
    }));

    it('should update state from form', fakeAsync(() => {
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'Attribut1',
          attributDisplayName: 'Attribut1DisplayName',
          radvisName: 'Test2',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'Attribut2',
          attributDisplayName: 'Attribut2DisplayName',
          radvisName: 'Test3',
          valid: true,
          ungueltigeWerte: [],
        },
      ]);
      recreateComponent();
      tick();
      verify(createSessionStateService.updateParameterInfo(anything())).once();

      component.formArray.at(1).get('selected')?.setValue(false);
      tick();

      verify(createSessionStateService.updateParameterInfo(anything())).twice();
      expect(capture(createSessionStateService.updateParameterInfo).last()[0]).toEqual(
        AttributeParameter.of(['Attribut1'])
      );
    }));
  });

  describe('startAttributImport', () => {
    const organisationsId = 348678;

    beforeEach(fakeAsync(() => {
      when(createSessionStateService.dateiUploadInfo).thenReturn(
        DateiUploadInfo.of(ImportTyp.ATTRIBUTE_UEBERNEHMEN, instance(mock(File)), organisationsId)
      );
      when(createSessionStateService.parameterInfo).thenReturn(AttributeParameter.of(['Attribut2']));
      when(manuellerImportService.getImportierbareAttribute(anything(), anything())).thenResolve([
        {
          attributName: 'Attribut1',
          attributDisplayName: 'Attribut1DisplayName',
          radvisName: 'Test2',
          valid: true,
          ungueltigeWerte: [],
        },
        {
          attributName: 'Attribut2',
          attributDisplayName: 'Attribut2DisplayName',
          radvisName: 'Test3',
          valid: true,
          ungueltigeWerte: [],
        },
      ]);
      when(manuellerImportService.createSessionAndStartAttributeImport(anything(), anything())).thenResolve();
      recreateComponent();
      tick();
    }));

    it('should disable form', fakeAsync(() => {
      expect(component.formArray.disabled).toBeFalse();
      component.onStart();
      expect(component.formArray.disabled).toBeTrue();
    }));

    it('should reenable form if errors occured', fakeAsync(() => {
      when(createSessionStateService.parameterInfo).thenReturn(null);

      expect(component.formArray.disabled).toBeFalse();
      expect(() => {
        component.onStart();
      }).toThrow();

      expect(component.formArray.disabled).toBeFalse();
      tick();
    }));

    it('should read command correct', fakeAsync(() => {
      component.onStart();
      tick();

      verify(manuellerImportService.createSessionAndStartAttributeImport(anything(), anything())).once();
      expect(capture(manuellerImportService.createSessionAndStartAttributeImport).last()[0]).toEqual({
        attribute: ['Attribut2'],
        organisation: organisationsId,
        attributeImportFormat: AttributeImportFormat.LUBW,
      } as StartAttributeImportSessionCommand);
    }));
  });
});
