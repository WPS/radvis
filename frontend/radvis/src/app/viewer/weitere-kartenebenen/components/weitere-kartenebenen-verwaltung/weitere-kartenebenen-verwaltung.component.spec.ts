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
import { ChangeDetectorRef } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { BenutzerName } from 'src/app/shared/models/benutzer-name';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { WeitereKartenebenenVerwaltungComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-verwaltung/weitere-kartenebenen-verwaltung.component';
import { DateiLayer } from 'src/app/viewer/weitere-kartenebenen/models/datei-layer';
import { DateiLayerFormat } from 'src/app/viewer/weitere-kartenebenen/models/datei-layer-format';
import { PredefinedWeitereKartenebenen } from 'src/app/viewer/weitere-kartenebenen/models/predefined-weitere-kartenebenen';
import { SaveWeitereKartenebeneCommand } from 'src/app/viewer/weitere-kartenebenen/models/save-weitere-kartenebene-command';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { WeitereKartenebeneTyp } from 'src/app/viewer/weitere-kartenebenen/models/weitereKartenebeneTyp';
import { DateiLayerService } from 'src/app/viewer/weitere-kartenebenen/services/datei-layer.service';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { neueWeitereKartenebenenDefaultZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';

describe(WeitereKartenebenenVerwaltungComponent.name, () => {
  let component: WeitereKartenebenenVerwaltungComponent;
  let fixture: MockedComponentFixture<WeitereKartenebenenVerwaltungComponent>;

  let weitereKartenebenen$$: BehaviorSubject<WeitereKartenebene[]>;
  let weitereKartenebenenService: WeitereKartenebenenService;

  let dateiLayerService: DateiLayerService;
  let alleDateiLayers$: BehaviorSubject<DateiLayer[]>;

  beforeEach(() => {
    weitereKartenebenenService = mock(WeitereKartenebenenService);
    weitereKartenebenen$$ = new BehaviorSubject<WeitereKartenebene[]>([]);
    when(weitereKartenebenenService.weitereKartenebenen$).thenReturn(weitereKartenebenen$$.asObservable());
    when(weitereKartenebenenService.weitereKartenebenen).thenCall(() => weitereKartenebenen$$.getValue());
    when(weitereKartenebenenService.save(anything())).thenReturn(Promise.resolve());

    dateiLayerService = mock(DateiLayerService);

    alleDateiLayers$ = new BehaviorSubject<DateiLayer[]>([]);
    when(dateiLayerService.allDateiLayers$).thenReturn(alleDateiLayers$);

    return MockBuilder(WeitereKartenebenenVerwaltungComponent, ViewerModule)
      .provide({ provide: WeitereKartenebenenService, useValue: instance(weitereKartenebenenService) })
      .provide({ provide: DateiLayerService, useValue: instance(dateiLayerService) })
      .provide({ provide: FormBuilder, useValue: new FormBuilder() })
      .provide({ provide: ChangeDetectorRef, useValue: instance(mock(ChangeDetectorRef)) })
      .provide({ provide: MatDialogRef, useValue: instance(mock(MatDialogRef)) })
      .provide({ provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) });
  });

  beforeEach(() => {
    fixture = MockRender(WeitereKartenebenenVerwaltungComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('form', () => {
    it('should be valid and saved', () => {
      component['resetForm']([
        ({
          name: 'Layer valid',
          url: 'http://localhost',
          quellangabe: 'Testquelle',
        } as unknown) as WeitereKartenebene,
      ]);
      component.onAddPredefinedWeitereKartenebenen(PredefinedWeitereKartenebenen.allgemein[0]);
      expect(component.weitereKartenebenenFormArray.valid).toBeTrue();

      component['saveLayers']();
      verify(weitereKartenebenenService.save(anything())).once();
    });

    it('should be invalid and not saved', () => {
      component['resetForm']([
        ({
          name: 'Layer invalid url',
          url: 'invalid url!',
          quellangabe: null,
        } as unknown) as WeitereKartenebene,
      ]);
      expect(component.weitereKartenebenenFormArray.invalid).toBeTrue();
      component['resetForm']([
        {
          // empty name should mean not valid
          name: '',
          url: 'http://valid.com',
        } as WeitereKartenebene,
      ]);
      expect(component.weitereKartenebenenFormArray.invalid).toBeTrue();

      component['saveLayers']();
      verify(weitereKartenebenenService.save(anything())).never();
    });
  });

  describe('resetForm', () => {
    const layerList = [
      {
        id: 1,
        name: 'Test1',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        url: 'http://test1',
        quellangabe: 'Testquelle',
      },
      {
        id: 2,
        name: 'Test2',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        url: 'http://test2',
        farbe: '#666666',
        quellangabe: 'Testquelle',
      },
    ];

    beforeEach(() => {
      component.weitereKartenebenenFormArray.markAsDirty();
      weitereKartenebenen$$.next(layerList);
    });

    it('should mark as pristine', () => {
      expect(component.weitereKartenebenenFormArray.dirty).toBeFalse();
    });

    it('should set controls', () => {
      expect(component.weitereKartenebenenFormArray.value).toEqual([
        {
          id: 1,
          name: 'Test1',
          weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
          deckkraft: 1.0,
          zoomstufe: 8.7,
          zindex: 1000,
          url: 'http://test1',
          farbe: '#55dd66',
          quellangabe: 'Testquelle',
          dateiLayerId: null,
        },
        {
          id: 2,
          name: 'Test2',
          weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
          deckkraft: 1.0,
          zoomstufe: 8.7,
          zindex: 1000,
          url: 'http://test2',
          farbe: '#666666',
          quellangabe: 'Testquelle',
          dateiLayerId: null,
        },
      ]);
    });
  });

  it('should read correct save command', fakeAsync(() => {
    const layerList = [
      {
        id: 1,
        name: 'Test1',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        url: 'http://test1',
        quellangabe: 'Testquelle',
        dateiLayerId: 1,
      },
      {
        id: 2,
        name: 'Test2',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        url: 'http://test2',
        farbe: '#666666',
        quellangabe: 'Testquelle',
        dateiLayerId: 2,
      },
    ];
    weitereKartenebenen$$.next(layerList);

    component.weitereKartenebenenFormArray.at(0).patchValue({
      name: '1234Layer',
      url: 'http://test1234',
    });
    component.weitereKartenebenenFormArray.at(1).patchValue({
      name: 'Blubb',
      farbe: '#000000',
    });

    component.onSave();
    tick();

    expect(capture(weitereKartenebenenService.save).last()[0]).toEqual([
      {
        id: 1,
        name: '1234Layer',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        url: 'http://test1234',
        farbe: undefined,
        quellangabe: 'Testquelle',
        dateiLayerId: 1,
      },
      {
        id: 2,
        name: 'Blubb',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        url: 'http://test2',
        farbe: '#000000',
        quellangabe: 'Testquelle',
        dateiLayerId: 2,
      },
    ]);
  }));

  it('should add new layer', () => {
    weitereKartenebenen$$.next([]);

    component.onAddWeitereKartenebenen();

    expect(component.weitereKartenebenenFormArray.length).toBe(1);
    expect(component.weitereKartenebenenFormArray.dirty).toBeTrue();
    expect(component.weitereKartenebenenFormArray.valid).toBeFalse();
    expect(component.weitereKartenebenenFormArray.at(0).get('zindex')?.value).toEqual(
      neueWeitereKartenebenenDefaultZIndex
    );
  });

  it('should add predefined', () => {
    weitereKartenebenen$$.next([]);

    component.onAddPredefinedWeitereKartenebenen(PredefinedWeitereKartenebenen.allgemein[0]);

    expect(component.weitereKartenebenenFormArray.length).toBe(1);
    expect(component.weitereKartenebenenFormArray.dirty).toBeTrue();
    expect(component.weitereKartenebenenFormArray.valid).toBeTrue();
    expect(component.weitereKartenebenenFormArray.value).toEqual([PredefinedWeitereKartenebenen.allgemein[0]]);
  });

  it('should add new custom Layer with zindex on top of predefined', () => {
    weitereKartenebenen$$.next([
      {
        ...PredefinedWeitereKartenebenen.allgemein[0],
        id: 1,
      },
    ]);
    component.onAddWeitereKartenebenen();

    expect(component.weitereKartenebenenFormArray.length).toBe(2);
    expect(component.weitereKartenebenenFormArray.dirty).toBeTrue();
    expect(component.weitereKartenebenenFormArray.valid).toBeFalse();
    expect(component.weitereKartenebenenFormArray.at(1).get('zindex')?.value).toEqual(
      PredefinedWeitereKartenebenen.allgemein[0].zindex + 1
    );
  });

  it('should delete layer', () => {
    component.onAddWeitereKartenebenen();
    component.onAddPredefinedWeitereKartenebenen(PredefinedWeitereKartenebenen.allgemein[0]);
    component.onAddWeitereKartenebenen();

    component.onDeleteLayer(2);

    expect(component.weitereKartenebenenFormArray.value).toEqual([
      {
        id: null,
        name: '',
        url: '',
        farbe: '#55dd66',
        deckkraft: 1,
        zoomstufe: 8.7,
        zindex: 1500,
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
        quellangabe: null,
        dateiLayerId: null,
      },
      PredefinedWeitereKartenebenen.allgemein[0],
    ]);
  });

  describe('farbe Validierung', () => {
    it('should be valid, if WFS and exists', () => {
      component.onAddWeitereKartenebenen();
      component.weitereKartenebenenFormArray.at(0).patchValue({
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
        name: 'Test',
        url: 'https://test',
        farbe: '#000000',
        quellangabe: 'Testquelle',
      });

      expect(component.weitereKartenebenenFormArray.valid).toBeTrue();
    });

    it('should be invalid, if WFS and is null', () => {
      component.onAddWeitereKartenebenen();
      component.weitereKartenebenenFormArray.at(0).patchValue({
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
        name: 'Test',
        url: 'https://test',
        farbe: undefined,
        quellangabe: 'Testquelle',
      });

      expect(component.weitereKartenebenenFormArray.valid).toBeFalse();
    });

    it('should be valid, if WMS', () => {
      component.onAddWeitereKartenebenen();
      component.weitereKartenebenenFormArray.at(0).patchValue({
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
        name: 'Test',
        url: 'https://test',
        farbe: undefined,
        quellangabe: 'Testquelle',
      });

      expect(component.weitereKartenebenenFormArray.valid).toBeTrue();
    });
  });

  it('should not send color for wms layer', fakeAsync(() => {
    component.onAddWeitereKartenebenen();
    component.weitereKartenebenenFormArray.at(0).patchValue({
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      name: 'Test',
      url: 'https://test',
      quellangabe: 'Testquelle',
    });
    component.onSave();
    tick();

    const expectedCommand: SaveWeitereKartenebeneCommand = {
      name: 'Test',
      farbe: undefined,
      id: null,
      deckkraft: 1,
      zoomstufe: 8.7,
      zindex: 1500,
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      url: 'https://test',
      quellangabe: 'Testquelle',
      dateiLayerId: null,
    };
    expect(capture(weitereKartenebenenService.save).last()[0]).toEqual([expectedCommand]);
  }));

  describe('datei-layer', () => {
    it('should not load', () => {
      expect(component.dateiLayerKartenebenen).toEqual([]);
    });

    it('should create save-commands for datei-layer', fakeAsync(() => {
      const layers = [
        {
          id: 123,
          quellangabe: 'Quellekatalog',
          name: 'no-name',
          geoserverLayerName: 'foobar-layer',
          benutzer: {
            vorname: 'Vor dem Namen ist nach dem Namen',
            nachname: 'Nach dem Namen ist vor dem Namen',
          } as BenutzerName,
          erstelltAm: 'irgendwann',
          format: DateiLayerFormat.GEOJSON,
        } as DateiLayer,
        {
          id: 234,
          quellangabe: 'Quallenangabe',
          name: 'wichtiger Name',
          geoserverLayerName: 'wichtiger-foobar-layer',
          benutzer: {
            vorname: 'Johann Wolfgang',
            nachname: 'van Beethoven',
          } as BenutzerName,
          erstelltAm: 'nicht heute',
          format: DateiLayerFormat.SHAPE,
        } as DateiLayer,
      ];

      alleDateiLayers$.next(layers);

      tick();

      expect(component.dateiLayerKartenebenen).toContain({
        name: layers[0].name,
        url: window.location.origin + '/api/geoserver/saml/datei-layer/wms?LAYERS=datei-layer:foobar-layer&TILED=true',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        quellangabe: layers[0].quellangabe,
        dateiLayerId: 123,
      } as SaveWeitereKartenebeneCommand);
      expect(component.dateiLayerKartenebenen).toContain({
        name: layers[1].name,
        url:
          window.location.origin +
          '/api/geoserver/saml/datei-layer/wms?LAYERS=datei-layer:wichtiger-foobar-layer&TILED=true',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        quellangabe: layers[1].quellangabe,
        dateiLayerId: 234,
      } as SaveWeitereKartenebeneCommand);
    }));
  });
});
