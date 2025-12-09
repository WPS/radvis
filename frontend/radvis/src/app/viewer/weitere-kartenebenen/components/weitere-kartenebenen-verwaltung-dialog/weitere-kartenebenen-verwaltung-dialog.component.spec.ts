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

import { ChangeDetectorRef } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { UntypedFormBuilder } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { BenutzerName } from 'src/app/shared/models/benutzer-name';
import { DateiLayer } from 'src/app/shared/models/datei-layer';
import { DateiLayerFormat } from 'src/app/shared/models/datei-layer-format';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DateiLayerService } from 'src/app/shared/services/datei-layer.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { neueWeitereKartenebenenDefaultZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { WeitereKartenebenenVerwaltungDialogComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-verwaltung-dialog/weitere-kartenebenen-verwaltung-dialog.component';
import { SaveWeitereKartenebeneCommand } from 'src/app/viewer/weitere-kartenebenen/models/save-weitere-kartenebene-command';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { WeitereKartenebeneTyp } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene-typ';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(WeitereKartenebenenVerwaltungDialogComponent.name, () => {
  let component: WeitereKartenebenenVerwaltungDialogComponent;
  let fixture: MockedComponentFixture<WeitereKartenebenenVerwaltungDialogComponent>;

  let weitereKartenebenen$$: BehaviorSubject<WeitereKartenebene[]>;
  let weitereKartenebenenService: WeitereKartenebenenService;

  let dateiLayerService: DateiLayerService;
  let benutzerDetailsService: BenutzerDetailsService;

  const predefinedLayer: SaveWeitereKartenebeneCommand = {
    name: 'Bevölkerungszahlen',
    url: 'https://www.wms.nrw.de/wms/zensusatlas?LAYERS=bevoelkerungszahl',
    weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
    farbe: undefined,
    deckkraft: 0.7,
    zoomstufe: 8.7,
    zindex: 1,
    id: null,
    quellangabe: 'https://www.wms.nrw.de/wms/zensusatlas?REQUEST=GetCapabilities',
    dateiLayerId: null,
    defaultLayer: false,
  };

  beforeEach(() => {
    weitereKartenebenenService = mock(WeitereKartenebenenService);
    weitereKartenebenen$$ = new BehaviorSubject<WeitereKartenebene[]>([]);
    when(weitereKartenebenenService.weitereKartenebenen$).thenReturn(weitereKartenebenen$$.asObservable());
    when(weitereKartenebenenService.weitereKartenebenen).thenCall(() => weitereKartenebenen$$.getValue());
    when(weitereKartenebenenService.save(anything())).thenReturn(Promise.resolve());

    dateiLayerService = mock(DateiLayerService);
    when(dateiLayerService.getAll()).thenResolve([]);

    benutzerDetailsService = mock(BenutzerDetailsService);

    return MockBuilder(WeitereKartenebenenVerwaltungDialogComponent, ViewerModule)
      .provide({ provide: WeitereKartenebenenService, useValue: instance(weitereKartenebenenService) })
      .provide({ provide: DateiLayerService, useValue: instance(dateiLayerService) })
      .provide({ provide: UntypedFormBuilder, useValue: new UntypedFormBuilder() })
      .provide({ provide: ChangeDetectorRef, useValue: instance(mock(ChangeDetectorRef)) })
      .provide({ provide: BenutzerDetailsService, useValue: instance(benutzerDetailsService) })
      .provide({ provide: MatDialogRef, useValue: instance(mock(MatDialogRef)) })
      .provide({ provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) });
  });

  beforeEach(() => {
    fixture = MockRender(WeitereKartenebenenVerwaltungDialogComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('form', () => {
    it('should be valid and saved', () => {
      component['resetForm']([
        {
          name: 'Layer valid',
          url: 'http://localhost',
          quellangabe: 'Testquelle',
        } as unknown as WeitereKartenebene,
      ]);
      component.onAddPredefinedWeitereKartenebenen(predefinedLayer);
      expect(component.weitereKartenebenenFormArray.valid).toBeTrue();

      component['saveLayers']();
      verify(weitereKartenebenenService.save(anything())).once();
    });

    it('should be invalid and not saved', () => {
      component['resetForm']([
        {
          name: 'Layer invalid url',
          url: 'invalid url!',
          quellangabe: null,
        } as unknown as WeitereKartenebene,
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
    const layerList: WeitereKartenebene[] = [
      {
        id: 1,
        name: 'Test1',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        url: 'http://test1',
        quellangabe: 'Testquelle',
        defaultLayer: false,
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
        defaultLayer: true,
      },
    ];

    it('should disable default layer if nutzer has no recht', () => {
      when(benutzerDetailsService.canLayerAlsDefaultFestlegen()).thenReturn(false);
      component.weitereKartenebenenFormArray.markAsDirty();
      weitereKartenebenen$$.next(layerList);
      expect(component.weitereKartenebenenFormArray.at(0).enabled).toBeTrue();
      expect(component.weitereKartenebenenFormArray.at(1).enabled).toBeFalse();
    });

    it('should enable default layer if nutzer has recht', () => {
      when(benutzerDetailsService.canLayerAlsDefaultFestlegen()).thenReturn(true);
      component.weitereKartenebenenFormArray.markAsDirty();
      weitereKartenebenen$$.next(layerList);
      component.weitereKartenebenenFormArray.controls.forEach(c => {
        expect(c.enabled).toBeTrue();
      });
    });

    it('should mark as pristine', () => {
      component.weitereKartenebenenFormArray.markAsDirty();
      weitereKartenebenen$$.next(layerList);
      expect(component.weitereKartenebenenFormArray.dirty).toBeFalse();
    });

    it('should set controls', () => {
      component.weitereKartenebenenFormArray.markAsDirty();
      weitereKartenebenen$$.next(layerList);
      expect(component.weitereKartenebenenFormArray.getRawValue()).toEqual([
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
          defaultLayer: false,
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
          defaultLayer: true,
        },
      ]);
    });
  });

  it('should read correct save command', fakeAsync(() => {
    const layerList: WeitereKartenebene[] = [
      {
        id: 1,
        name: 'Test1',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        url: 'http://test1',
        quellangabe: 'Testquelle',
        defaultLayer: false,
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
        defaultLayer: false,
      },
    ];
    weitereKartenebenen$$.next(layerList);

    component.weitereKartenebenenFormArray.at(0).patchValue({
      name: '1234Layer',
      url: 'http://test1234',
      defaultLayer: true,
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
        dateiLayerId: null,
        defaultLayer: true,
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
        dateiLayerId: null,
        defaultLayer: false,
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

    component.onAddPredefinedWeitereKartenebenen(predefinedLayer);

    expect(component.weitereKartenebenenFormArray.length).toBe(1);
    expect(component.weitereKartenebenenFormArray.dirty).toBeTrue();
    expect(component.weitereKartenebenenFormArray.valid).toBeTrue();
    expect(component.weitereKartenebenenFormArray.value).toEqual([predefinedLayer]);
  });

  it('should add new custom Layer with zindex on top of predefined', () => {
    weitereKartenebenen$$.next([
      {
        ...predefinedLayer,
        id: 1,
      },
    ]);
    component.onAddWeitereKartenebenen();

    expect(component.weitereKartenebenenFormArray.length).toBe(2);
    expect(component.weitereKartenebenenFormArray.dirty).toBeTrue();
    expect(component.weitereKartenebenenFormArray.valid).toBeFalse();
    expect(component.weitereKartenebenenFormArray.at(1).get('zindex')?.value).toEqual(predefinedLayer.zindex + 1);
  });

  it('should delete layer', () => {
    component.onAddWeitereKartenebenen();
    component.onAddPredefinedWeitereKartenebenen(predefinedLayer);
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
        defaultLayer: false,
      },
      predefinedLayer,
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
      defaultLayer: false,
    };
    expect(capture(weitereKartenebenenService.save).last()[0]).toEqual([expectedCommand]);
  }));

  describe('datei-layer', () => {
    it('should have valid uri for layernames with spaces', fakeAsync(() => {
      when(dateiLayerService.getAll()).thenResolve([
        {
          id: 123,
          quellangabe: 'Quellekatalog',
          name: 'no-name',
          geoserverLayerName: 'foobar-layer 1',
          benutzer: {
            vorname: 'Vor dem Namen ist nach dem Namen',
            nachname: 'Nach dem Namen ist vor dem Namen',
          } as BenutzerName,
          erstelltAm: 'irgendwann',
          format: DateiLayerFormat.GEOJSON,
        },
      ]);
      fixture = MockRender(
        WeitereKartenebenenVerwaltungDialogComponent,
        {} as WeitereKartenebenenVerwaltungDialogComponent,
        {
          reset: true,
        }
      );
      component = fixture.point.componentInstance;
      fixture.detectChanges();
      tick();
      component.onAddPredefinedWeitereKartenebenen(component.dateiLayerKartenebenen[0]);

      expect(component.weitereKartenebenenFormArray.length).toBe(1);
      expect(component.weitereKartenebenenFormArray.valid).toBeTrue();
    }));

    it('should save predefinedKatenebene', () => {
      const saveCommand = {
        name: 'ABC-Layer',
        url: window.location.origin + '/api/geoserver/saml/datei-layer/wms?LAYERS=datei-layer:foobar-layer&TILED=true',
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
        deckkraft: 1.0,
        zoomstufe: 8.7,
        zindex: 1000,
        quellangabe: 'Test',
        dateiLayerId: 123,
        defaultLayer: false,
        id: null,
      };
      component.onAddPredefinedWeitereKartenebenen({ ...saveCommand });
      component.onSave();

      verify(weitereKartenebenenService.save(anything())).once();
      expect(capture(weitereKartenebenenService.save).last()[0]).toEqual([{ ...saveCommand, farbe: undefined }]);
    });

    it('should create save-commands for datei-layer', fakeAsync(() => {
      const layers: DateiLayer[] = [
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
        },
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
        },
      ];

      when(dateiLayerService.getAll()).thenResolve(layers);
      fixture = MockRender(
        WeitereKartenebenenVerwaltungDialogComponent,
        {} as WeitereKartenebenenVerwaltungDialogComponent,
        {
          reset: true,
        }
      );
      component = fixture.point.componentInstance;
      fixture.detectChanges();
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
        defaultLayer: false,
        id: null,
      });
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
        defaultLayer: false,
        id: null,
      });
    }));
  });
});
