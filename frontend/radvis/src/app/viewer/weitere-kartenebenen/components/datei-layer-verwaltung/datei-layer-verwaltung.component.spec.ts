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

import { DateiLayerVerwaltungComponent } from 'src/app/viewer/weitere-kartenebenen/components/datei-layer-verwaltung/datei-layer-verwaltung.component';
import { ComponentFixture } from '@angular/core/testing';
import { DateiLayerService } from 'src/app/viewer/weitere-kartenebenen/services/datei-layer.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { MockBuilder, MockRender } from 'ng-mocks';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { MatDialogRef } from '@angular/material/dialog';
import { DateiLayerFormat } from 'src/app/viewer/weitere-kartenebenen/models/datei-layer-format';
import { CreateDateiLayerCommand } from 'src/app/viewer/weitere-kartenebenen/models/create-datei-layer-command';
import { DateiLayer } from 'src/app/viewer/weitere-kartenebenen/models/datei-layer';
import { BenutzerName } from 'src/app/shared/models/benutzer-name';
import { BehaviorSubject } from 'rxjs';

describe(DateiLayerVerwaltungComponent.name, () => {
  let component: DateiLayerVerwaltungComponent;
  let fixture: ComponentFixture<DateiLayerVerwaltungComponent>;

  let dateiLayerService: DateiLayerService;

  let mockFile: File;

  const alleDateiLayers$: BehaviorSubject<DateiLayer[]> = new BehaviorSubject<DateiLayer[]>([]);

  beforeEach(() => {
    dateiLayerService = mock(DateiLayerService);
    mockFile = mock(File);

    when(dateiLayerService.allDateiLayers$).thenReturn(alleDateiLayers$);
    when(dateiLayerService.getMaxFileSizeInMB()).thenResolve(10);
    when(dateiLayerService.create(anything(), anything())).thenResolve();
    when(dateiLayerService.changeStyle(anything(), anything())).thenResolve();
    when(dateiLayerService.deleteStyle(anything())).thenResolve();

    return MockBuilder(DateiLayerVerwaltungComponent, ViewerModule)
      .provide({
        provide: DateiLayerService,
        useValue: instance(dateiLayerService),
      })
      .provide({ provide: MatDialogRef, useValue: instance(mock(MatDialogRef)) });
  });

  beforeEach(() => {
    fixture = MockRender(DateiLayerVerwaltungComponent);
    component = fixture.componentInstance;
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
  });

  describe(DateiLayerVerwaltungComponent.prototype.onDeleteStyle.name, () => {
    it('should make correct Api-Call', () => {
      // act
      component.onDeleteStyle(22527);
      // assert
      verify(dateiLayerService.deleteStyle(anything())).once();
      expect(capture(dateiLayerService.deleteStyle).last()[0]).toEqual(22527);
    });
  });

  describe('list dateiLayers', () => {
    it('should list vorhandene DateiLayers', (done: DoneFn) => {
      const testDateiLayers = [
        {
          id: 1,
          name: 'testDateiLayer1',
          format: DateiLayerFormat.SHAPE,
          quellangabe: 'dies ist eine Quellangabe',
          benutzer: { vorname: 'vorname', nachname: 'nachname' } as BenutzerName,
          erstelltAm: '12.12.2112',
        } as DateiLayer,
      ];
      alleDateiLayers$.next(testDateiLayers);

      component.dateiLayers$.subscribe(value => {
        expect(value).toEqual(testDateiLayers);
        done();
      });
    });
  });
});
