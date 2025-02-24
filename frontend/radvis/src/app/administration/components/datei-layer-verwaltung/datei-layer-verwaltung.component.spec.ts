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
import { MatDialogRef } from '@angular/material/dialog';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { AdministrationModule } from 'src/app/administration/administration.module';
import { DateiLayerVerwaltungComponent } from 'src/app/administration/components/datei-layer-verwaltung/datei-layer-verwaltung.component';
import { BenutzerName } from 'src/app/shared/models/benutzer-name';
import { CreateDateiLayerCommand } from 'src/app/shared/models/create-datei-layer-command';
import { DateiLayer } from 'src/app/shared/models/datei-layer';
import { DateiLayerFormat } from 'src/app/shared/models/datei-layer-format';
import { DateiLayerService } from 'src/app/shared/services/datei-layer.service';
import { anything, capture, instance, mock, resetCalls, verify, when } from 'ts-mockito';

describe(DateiLayerVerwaltungComponent.name, () => {
  let component: DateiLayerVerwaltungComponent;
  let fixture: MockedComponentFixture<DateiLayerVerwaltungComponent>;

  let dateiLayerService: DateiLayerService;

  let mockFile: File;

  beforeEach(() => {
    dateiLayerService = mock(DateiLayerService);
    mockFile = mock(File);

    when(dateiLayerService.getAll()).thenResolve([]);
    when(dateiLayerService.getMaxFileSizeInMB()).thenResolve(10);
    when(dateiLayerService.create(anything(), anything())).thenResolve();
    when(dateiLayerService.changeStyle(anything(), anything())).thenResolve();
    when(dateiLayerService.deleteStyle(anything())).thenResolve();

    return MockBuilder(DateiLayerVerwaltungComponent, AdministrationModule)
      .provide({
        provide: DateiLayerService,
        useValue: instance(dateiLayerService),
      })
      .provide({ provide: MatDialogRef, useValue: instance(mock(MatDialogRef)) });
  });

  beforeEach(() => {
    fixture = MockRender(DateiLayerVerwaltungComponent);
    component = fixture.point.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('onCreate', () => {
    it('format form field schon ausgefüllt', () => {
      // arrange
      expect(component.formGroupDateiLayer.get('format')?.valid).toBeTrue();
      expect(component.formGroupDateiLayer.get('format')?.value).toEqual(DateiLayerFormat.SHAPE);
    });

    it('should invalid form', () => {
      // arrange
      component.formGroupDateiLayer.setValue({
        name: null,
        format: DateiLayerFormat.SHAPE,
        quellangabe: 'testQuellangabe',
        file: mockFile,
      });

      expect(component.formGroupDateiLayer.valid).toBeFalse();
    });

    it('should fill command from form', () => {
      // arrange
      component.formGroupDateiLayer.setValue({
        name: 'testname',
        format: DateiLayerFormat.SHAPE,
        quellangabe: 'testQuellangabe',
        file: mockFile,
      });

      // act
      component.onCreate();

      // assert
      verify(dateiLayerService.create(anything(), anything())).once();
      expect(capture(dateiLayerService.create).last()[0]).toEqual({
        name: 'testname',
        format: DateiLayerFormat.SHAPE,
        quellangabe: 'testQuellangabe',
      } as CreateDateiLayerCommand);
    });

    it('should refresh layers on success', fakeAsync(() => {
      const testDateiLayers: DateiLayer[] = [
        {
          id: 1,
          name: 'testDateiLayer1',
          format: DateiLayerFormat.SHAPE,
          quellangabe: 'dies ist eine Quellangabe',
          benutzer: { vorname: 'vorname', nachname: 'nachname' } as BenutzerName,
          erstelltAm: '12.12.2112',
          geoserverLayerName: 'Test',
        },
      ];
      when(dateiLayerService.getAll()).thenResolve(testDateiLayers);
      resetCalls(dateiLayerService);

      component.formGroupDateiLayer.setValue({
        name: 'testname',
        format: DateiLayerFormat.SHAPE,
        quellangabe: 'testQuellangabe',
        file: mockFile,
      });

      component.onCreate();
      tick();

      verify(dateiLayerService.getAll()).once();
      expect(component.dateiLayers).toEqual(testDateiLayers);
    }));
  });

  describe(DateiLayerVerwaltungComponent.prototype.onAddOrChangeStyle.name, () => {
    it('should make correct Api-Call', () => {
      // act
      const sldFile = instance(mock(File));
      component.onAddOrChangeStyle(1, sldFile);
      // assert
      verify(dateiLayerService.changeStyle(anything(), anything())).once();
      expect(capture(dateiLayerService.changeStyle).last()).toEqual([1, sldFile]);
    });

    it('should refresh layers on success', fakeAsync(() => {
      const testDateiLayers: DateiLayer[] = [
        {
          id: 1,
          name: 'testDateiLayer1',
          format: DateiLayerFormat.SHAPE,
          quellangabe: 'dies ist eine Quellangabe',
          benutzer: { vorname: 'vorname', nachname: 'nachname' } as BenutzerName,
          erstelltAm: '12.12.2112',
          geoserverLayerName: 'Test',
        },
      ];
      when(dateiLayerService.getAll()).thenResolve(testDateiLayers);
      resetCalls(dateiLayerService);
      const sldFile = instance(mock(File));
      component.onAddOrChangeStyle(1, sldFile);
      tick();

      verify(dateiLayerService.getAll()).once();
      expect(component.dateiLayers).toEqual(testDateiLayers);
    }));
  });

  describe(DateiLayerVerwaltungComponent.prototype.onDeleteStyle.name, () => {
    it('should make correct Api-Call', () => {
      // act
      component.onDeleteStyle(22527);
      // assert
      verify(dateiLayerService.deleteStyle(anything())).once();
      expect(capture(dateiLayerService.deleteStyle).last()[0]).toEqual(22527);
    });

    it('should refresh layers on success', fakeAsync(() => {
      const testDateiLayers: DateiLayer[] = [
        {
          id: 1,
          name: 'testDateiLayer1',
          format: DateiLayerFormat.SHAPE,
          quellangabe: 'dies ist eine Quellangabe',
          benutzer: { vorname: 'vorname', nachname: 'nachname' } as BenutzerName,
          erstelltAm: '12.12.2112',
          geoserverLayerName: 'Test',
        },
      ];
      when(dateiLayerService.getAll()).thenResolve(testDateiLayers);
      resetCalls(dateiLayerService);
      component.onDeleteStyle(22527);
      tick();

      verify(dateiLayerService.getAll()).once();
      expect(component.dateiLayers).toEqual(testDateiLayers);
    }));
  });

  describe('list dateiLayers', () => {
    it('should list vorhandene DateiLayers', fakeAsync(() => {
      const testDateiLayers: DateiLayer[] = [
        {
          id: 1,
          name: 'testDateiLayer1',
          format: DateiLayerFormat.SHAPE,
          quellangabe: 'dies ist eine Quellangabe',
          benutzer: { vorname: 'vorname', nachname: 'nachname' } as BenutzerName,
          erstelltAm: '12.12.2112',
          geoserverLayerName: 'Test',
        },
      ];
      when(dateiLayerService.getAll()).thenResolve(testDateiLayers);

      fixture = MockRender(DateiLayerVerwaltungComponent, {} as DateiLayerVerwaltungComponent, { reset: true });
      tick();

      expect(fixture.point.componentInstance.dateiLayers).toEqual(testDateiLayers);
    }));
  });
});
