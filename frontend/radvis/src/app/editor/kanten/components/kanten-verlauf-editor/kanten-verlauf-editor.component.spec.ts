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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { KantenFahrtrichtungEditorComponent } from 'src/app/editor/kanten/components/kanten-fahrtrichtung-editor/kanten-fahrtrichtung-editor.component';
import { KantenVerlaufEditorComponent } from 'src/app/editor/kanten/components/kanten-verlauf-editor/kanten-verlauf-editor.component';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { anotherKante, defaultKante } from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NotifyGeometryChangedService } from 'src/app/editor/kanten/services/notify-geometry-changed.service';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { anything, capture, instance, mock, when } from 'ts-mockito';

describe('KantenVerlaufEditorComponent', () => {
  let component: KantenVerlaufEditorComponent;
  let fixture: ComponentFixture<KantenVerlaufEditorComponent>;
  const netzServiceMock = mock(NetzService);
  const kanteSelektionServiceMock = mock(KantenSelektionService);
  const notifyGeometryChangedService = mock(NotifyGeometryChangedService);

  let kantenSubject$: Subject<Kante[]>;
  let kantenSelektionSubject$: Subject<KantenSelektion[]>;

  const setupSelektion = (selektion: KantenSelektion[]): void => {
    when(kanteSelektionServiceMock.selektion).thenReturn(selektion);
    when(kanteSelektionServiceMock.selektierteKanten).thenReturn(selektion.map(s => s.kante));
    kantenSubject$.next(selektion.map(s => s.kante));
    kantenSelektionSubject$.next(selektion);
  };

  beforeEach(() => {
    const einseitigeKante = { ...anotherKante, zweiseitig: false };
    kantenSubject$ = new Subject();
    kantenSelektionSubject$ = new Subject();
    when(kanteSelektionServiceMock.selektierteKanten$).thenReturn(kantenSubject$);
    when(kanteSelektionServiceMock.selektion$).thenReturn(kantenSelektionSubject$);

    setupSelektion([KantenSelektion.ofGesamteKante(defaultKante), KantenSelektion.ofGesamteKante(einseitigeKante)]);

    when(netzServiceMock.berechneVerlaufLinks(anything())).thenResolve({ type: 'LineString', coordinates: [[10, 20]] });
    when(netzServiceMock.berechneVerlaufRechts(anything())).thenResolve({
      type: 'LineString',
      coordinates: [[30, 40]],
    });
    return MockBuilder(KantenFahrtrichtungEditorComponent, EditorModule)
      .provide({
        provide: NetzService,
        useValue: instance(netzServiceMock),
      })
      .provide({ provide: KantenSelektionService, useValue: instance(kanteSelektionServiceMock) })
      .provide({ provide: NotifyGeometryChangedService, useValue: instance(notifyGeometryChangedService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(KantenVerlaufEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('resetForm', () => {
    it('should reset form correctly', () => {
      component.onReset();

      expect(component.geometryFormControls.length).toEqual(2);
      expect(component.verlaufLinksFormControls.length).toEqual(2);
      expect(component.verlaufRechtsFormControls.length).toEqual(2);
      expect(component.verlaufEinseitigFormControls.length).toEqual(2);
      expect(component.geometryFormControls[0].value).toEqual({
        coordinates: [[0, 1]],
        type: 'LineString',
      } as LineStringGeojson);
      expect(component.geometryFormControls[1].value).toEqual({
        coordinates: [[2, 3]],
        type: 'LineString',
      } as LineStringGeojson);
      expect((component.verlaufLinksFormControls[0].value as LineStringGeojson).coordinates).toEqual([[0, 1]]);
      expect(component.verlaufLinksFormControls[1].value).toBeNull();
      expect((component.verlaufRechtsFormControls[0].value as LineStringGeojson).coordinates).toEqual([[0, 1]]);
      expect(component.verlaufRechtsFormControls[1].value).toBeNull();
      expect((component.verlaufEinseitigFormControls[0].value as LineStringGeojson).coordinates).toEqual([[0, 1]]);
      expect(component.verlaufEinseitigFormControls[1].value).toBeNull();

      expect(component.formGroup.pristine).toBeTrue();
    });
  });

  describe('save', () => {
    it('should build commands correctly', () => {
      component.onReset();

      component.verlaufLinksFormControls[0].setValue({ type: 'LineString', coordinates: [[0, 1]] });
      component.verlaufRechtsFormControls[0].setValue({ type: 'LineString', coordinates: [[2, 3]] });
      component.geometryFormControls[0].setValue({ type: 'LineString', coordinates: [[4, 5]] });

      component.geometryFormControls[1].setValue({ type: 'LineString', coordinates: [[6, 7]] });
      component.verlaufEinseitigFormControls[1].setValue({ type: 'LineString', coordinates: [[8, 9]] });

      component['save']();

      const [commands] = capture(netzServiceMock.saveKanteVerlauf).last();
      expect(commands[0].id).toEqual(1);
      expect(commands[0].geometry).toEqual({
        coordinates: [[4, 5]],
        type: 'LineString',
      } as LineStringGeojson);
      expect((commands[0].verlaufLinks as LineStringGeojson).coordinates).toEqual([[0, 1]]);
      expect((commands[0].verlaufRechts as LineStringGeojson).coordinates).toEqual([[2, 3]]);
      expect(commands[0].kantenVersion).toEqual(1);

      expect(commands[1].id).toEqual(2);
      expect(commands[1].geometry).toEqual({
        coordinates: [[6, 7]],
        type: 'LineString',
      } as LineStringGeojson);
      expect(commands[1].verlaufLinks).toEqual({
        coordinates: [[8, 9]],
        type: 'LineString',
      } as LineStringGeojson);
      expect(commands[1].verlaufRechts).toEqual({
        coordinates: [[8, 9]],
        type: 'LineString',
      } as LineStringGeojson);
      expect(commands[1].kantenVersion).toEqual(1);
    });
  });

  describe('mindestensEinVerlaufFehlt', () => {
    it('should return false if no Verlauf is missing', () => {
      component.onReset();

      component.verlaufLinksFormControls[0].setValue({ type: 'LineString', coordinates: [[0, 1]] });
      component.verlaufRechtsFormControls[0].setValue({ type: 'LineString', coordinates: [[2, 3]] });
      component.geometryFormControls[0].setValue({ type: 'LineString', coordinates: [[4, 5]] });
      component.verlaufEinseitigFormControls[0].setValue(null);

      component.verlaufLinksFormControls[1].setValue(null);
      component.verlaufRechtsFormControls[1].setValue(null);
      component.geometryFormControls[1].setValue({ type: 'LineString', coordinates: [[6, 7]] });
      component.verlaufEinseitigFormControls[1].setValue({ type: 'LineString', coordinates: [[8, 9]] });

      expect(component.mindestensEinVerlaufFehlt).toBeFalse();
    });

    it('should return true if Verlauf is missing', () => {
      component.onReset();

      component.verlaufLinksFormControls[0].setValue(null);
      component.verlaufRechtsFormControls[0].setValue(null);
      component.geometryFormControls[0].setValue({ type: 'LineString', coordinates: [[4, 5]] });
      component.verlaufEinseitigFormControls[0].setValue(null);

      component.verlaufLinksFormControls[1].setValue(null);
      component.verlaufRechtsFormControls[1].setValue(null);
      component.geometryFormControls[1].setValue({ type: 'LineString', coordinates: [[6, 7]] });
      component.verlaufEinseitigFormControls[1].setValue({ type: 'LineString', coordinates: [[8, 9]] });

      expect(component.mindestensEinVerlaufFehlt).toBeTrue();
    });
  });

  describe('mindestensEinVerlaufVorhanden', () => {
    it('should return false if no Verlauf is present', () => {
      component.onReset();

      component.verlaufLinksFormControls[0].setValue(null);
      component.verlaufRechtsFormControls[0].setValue(null);
      component.geometryFormControls[0].setValue(null);
      component.verlaufEinseitigFormControls[0].setValue(null);

      component.verlaufLinksFormControls[1].setValue(null);
      component.verlaufRechtsFormControls[1].setValue(null);
      component.geometryFormControls[1].setValue(null);
      component.verlaufEinseitigFormControls[1].setValue(null);

      expect(component.mindestensEinVerlaufVorhanden).toBeFalse();
    });

    it('should return true if one Verlauf is present', () => {
      component.onReset();

      component.verlaufLinksFormControls[0].setValue(null);
      component.verlaufRechtsFormControls[0].setValue(null);
      component.geometryFormControls[0].setValue(null);
      component.verlaufEinseitigFormControls[0].setValue(null);

      component.verlaufLinksFormControls[1].setValue(null);
      component.verlaufRechtsFormControls[1].setValue(null);
      component.geometryFormControls[1].setValue(null);
      component.verlaufEinseitigFormControls[1].setValue({ type: 'LineString', coordinates: [[6, 7]] });

      expect(component.mindestensEinVerlaufVorhanden).toBeTrue();
    });
  });

  describe('verlaeufeHinzufuegen', () => {
    it('should add verlaeufe correctly', fakeAsync(() => {
      const zweiseitigeKante = { ...defaultKante, zweiseitig: true, verlaufLinks: null, verlaufRechts: null };
      const einseitigeKante = { ...anotherKante, zweiseitig: false, verlaufLinks: null, verlaufRechts: null };
      when(kanteSelektionServiceMock.selektion$).thenReturn(
        of([KantenSelektion.ofGesamteKante(zweiseitigeKante), KantenSelektion.ofGesamteKante(einseitigeKante)])
      );
      when(kanteSelektionServiceMock.selektion).thenReturn([
        KantenSelektion.ofGesamteKante(zweiseitigeKante),
        KantenSelektion.ofGesamteKante(einseitigeKante),
      ]);
      component.onReset();

      component.verlaeufeHinzufuegen();
      tick();

      expect(component.verlaufLinksFormControls.length).toEqual(2);
      expect(component.verlaufRechtsFormControls.length).toEqual(2);
      expect(component.verlaufEinseitigFormControls.length).toEqual(2);
      expect((component.verlaufLinksFormControls[0].value as LineStringGeojson).coordinates).toEqual([[10, 20]]);
      expect((component.verlaufRechtsFormControls[0].value as LineStringGeojson).coordinates).toEqual([[30, 40]]);
      expect(component.verlaufEinseitigFormControls[0].value as LineStringGeojson).toBeNull();
      expect(component.verlaufLinksFormControls[1].value).toBeNull();
      expect(component.verlaufRechtsFormControls[1].value).toBeNull();
      expect((component.verlaufEinseitigFormControls[1].value as LineStringGeojson).coordinates).toEqual([[2, 3]]);
    }));
  });

  describe('verlaeufeLoeschen', () => {
    it('should delete verlaeufe correctly', () => {
      component.onReset();

      component.verlaeufeLoeschen();

      expect(component.verlaufLinksFormControls.length).toEqual(2);
      expect(component.verlaufRechtsFormControls.length).toEqual(2);
      expect(component.verlaufEinseitigFormControls.length).toEqual(2);
      expect(component.verlaufLinksFormControls[0].value).toBeNull();
      expect(component.verlaufRechtsFormControls[0].value).toBeNull();
      expect(component.verlaufEinseitigFormControls[0].value).toBeNull();
      expect(component.verlaufLinksFormControls[1].value).toBeNull();
      expect(component.verlaufRechtsFormControls[1].value).toBeNull();
      expect(component.verlaufEinseitigFormControls[1].value).toBeNull();
    });
  });

  describe('edit RadNETZ', () => {
    it('should disable control if RadNETZ-Kante is selected', fakeAsync(() => {
      const kante: Kante = {
        ...defaultKante,
        quelle: QuellSystem.RadNETZ,
      };
      setupSelektion([KantenSelektion.ofGesamteKante(kante)]);

      tick();

      expect(component.formGroup.disabled).toBeTrue();
    }));

    it('should reanable controls when last RadNETZ-Kante is deselected', fakeAsync(() => {
      const kanteRadNETZ: Kante = {
        ...defaultKante,
        quelle: QuellSystem.RadNETZ,
      };
      const kanteDLM: Kante = {
        ...defaultKante,
        quelle: QuellSystem.DLM,
      };
      setupSelektion([kanteRadNETZ, kanteDLM].map(k => KantenSelektion.ofGesamteKante(k)));

      tick();

      expect(component.formGroup.disabled).toBeTrue();

      setupSelektion([KantenSelektion.ofGesamteKante(kanteDLM)]);

      tick();

      expect(component.formGroup.disabled).toBeFalse();
    }));
  });
});
