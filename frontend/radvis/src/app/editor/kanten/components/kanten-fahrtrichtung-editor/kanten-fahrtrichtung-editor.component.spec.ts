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
import { Subject } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { FahrtrichtungAttributGruppe } from 'src/app/editor/kanten/models/fahrtrichtung-attributgruppe';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { defaultKante } from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { Richtung } from 'src/app/editor/kanten/models/richtung';
import { SaveFahrtrichtungAttributGruppeCommand } from 'src/app/editor/kanten/models/save-fahrtrichtung-attribut-gruppe-command';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { KantenFahrtrichtungEditorComponent } from './kanten-fahrtrichtung-editor.component';

describe('KantenFahrtrichtungEditorComponent', () => {
  let component: KantenFahrtrichtungEditorComponent;
  let fixture: ComponentFixture<KantenFahrtrichtungEditorComponent>;
  let netzService: NetzService;
  let kantenSelektionService: KantenSelektionService;
  let kantenSubject$: Subject<Kante[]>;
  let kantenSelektionSubject$: Subject<KantenSelektion[]>;

  const setupSelektion = (selektion: KantenSelektion[]): void => {
    when(kantenSelektionService.selektion).thenReturn(selektion);
    when(kantenSelektionService.selektierteKanten).thenReturn(selektion.map(s => s.kante));
    kantenSubject$.next(selektion.map(s => s.kante));
    kantenSelektionSubject$.next(selektion);
  };

  beforeEach(() => {
    netzService = mock(NetzService);
    kantenSelektionService = mock(KantenSelektionService);
    kantenSubject$ = new Subject();
    kantenSelektionSubject$ = new Subject();
    when(kantenSelektionService.selektierteKanten$).thenReturn(kantenSubject$);
    when(kantenSelektionService.selektion$).thenReturn(kantenSelektionSubject$);
    return MockBuilder(KantenFahrtrichtungEditorComponent, EditorModule)
      .provide({
        provide: NetzService,
        useValue: instance(netzService),
      })
      .provide({ provide: KantenSelektionService, useValue: instance(kantenSelektionService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(KantenFahrtrichtungEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('fillForm', () => {
    it('should set richtung correct, seite', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fahrtrichtungAttributGruppe: {
            id: 1,
            version: 1,
            fahrtrichtungLinks: Richtung.IN_RICHTUNG,
            fahrtrichtungRechts: Richtung.IN_RICHTUNG,
          } as FahrtrichtungAttributGruppe,
        } as Kante),
      ]);

      tick();

      expect(component.displayedAttributeformGroup.value.richtung).toBe(Richtung.IN_RICHTUNG);
    }));

    it('should set undetermined correct, seite', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fahrtrichtungAttributGruppe: {
            id: 1,
            version: 1,
            fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
            fahrtrichtungRechts: Richtung.IN_RICHTUNG,
          } as FahrtrichtungAttributGruppe,
        } as Kante),
      ]);

      tick();

      expect(component.displayedAttributeformGroup.value.richtung).toBeInstanceOf(UndeterminedValue);
    }));

    it('should set richtung correct, multiple kanten', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fahrtrichtungAttributGruppe: {
              id: 1,
              version: 1,
              fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
              fahrtrichtungRechts: Richtung.GEGEN_RICHTUNG,
            } as FahrtrichtungAttributGruppe,
          } as Kante,
          Seitenbezug.LINKS
        ),
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fahrtrichtungAttributGruppe: {
              id: 1,
              version: 1,
              fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
              fahrtrichtungRechts: Richtung.GEGEN_RICHTUNG,
            } as FahrtrichtungAttributGruppe,
          } as Kante,
          Seitenbezug.RECHTS
        ),
      ]);
      tick();

      expect(component.displayedAttributeformGroup.value.richtung).toBe(Richtung.GEGEN_RICHTUNG);
    }));

    it('should set undetermined correct, multiple kanten', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fahrtrichtungAttributGruppe: {
              id: 1,
              version: 1,
              fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
              fahrtrichtungRechts: Richtung.GEGEN_RICHTUNG,
            } as FahrtrichtungAttributGruppe,
          } as Kante,
          Seitenbezug.LINKS
        ),
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fahrtrichtungAttributGruppe: {
              id: 1,
              version: 1,
              fahrtrichtungLinks: Richtung.IN_RICHTUNG,
              fahrtrichtungRechts: Richtung.IN_RICHTUNG,
            } as FahrtrichtungAttributGruppe,
          } as Kante,
          Seitenbezug.RECHTS
        ),
      ]);
      tick();

      expect(component.displayedAttributeformGroup.value.richtung).toBeInstanceOf(UndeterminedValue);
    }));
  });

  describe('onReset', () => {
    it('should reset selected Seiten', fakeAsync(() => {
      const selektion = [
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fahrtrichtungAttributGruppe: {
              id: 1,
              version: 1,
              fahrtrichtungLinks: Richtung.IN_RICHTUNG,
              fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
            } as FahrtrichtungAttributGruppe,
          } as Kante,
          Seitenbezug.LINKS
        ),
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fahrtrichtungAttributGruppe: {
              id: 2,
              version: 3,
              fahrtrichtungLinks: Richtung.BEIDE_RICHTUNGEN,
              fahrtrichtungRechts: Richtung.GEGEN_RICHTUNG,
            } as FahrtrichtungAttributGruppe,
          } as Kante,
          Seitenbezug.LINKS
        ),
      ];
      setupSelektion(selektion);

      tick();
      component.displayedAttributeformGroup.patchValue({
        richtung: Richtung.GEGEN_RICHTUNG,
      });
      expect(component.areAttributesPristine).toBeFalse();
      component.onReset();
      expect(component.areAttributesPristine).toBeTrue();

      const [gruppe1, gruppe2]: FahrtrichtungAttributGruppe[] = component['currentFahrtrichtungAttributGruppen'];
      expect(gruppe1.fahrtrichtungLinks).toBe(Richtung.IN_RICHTUNG);
      expect(gruppe1.fahrtrichtungRechts).toBe(Richtung.BEIDE_RICHTUNGEN);
      expect(gruppe2.fahrtrichtungLinks).toBe(Richtung.BEIDE_RICHTUNGEN);
      expect(gruppe2.fahrtrichtungRechts).toBe(Richtung.GEGEN_RICHTUNG);
    }));
  });

  describe('onSave', () => {
    beforeEach(() => {
      when(netzService.saveFahrtrichtungAttributgruppe(anything())).thenResolve();
    });

    it('should read values correct, multiple kanten', fakeAsync(() => {
      const selektion = [
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fahrtrichtungAttributGruppe: {
              id: 1,
              version: 1,
              fahrtrichtungLinks: Richtung.IN_RICHTUNG,
              fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
            } as FahrtrichtungAttributGruppe,
          } as Kante,
          Seitenbezug.LINKS
        ),
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fahrtrichtungAttributGruppe: {
              id: 2,
              version: 3,
              fahrtrichtungLinks: Richtung.BEIDE_RICHTUNGEN,
              fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
            } as FahrtrichtungAttributGruppe,
          } as Kante,
          Seitenbezug.LINKS
        ),
      ];
      setupSelektion(selektion);

      tick();
      component.displayedAttributeformGroup.patchValue({
        richtung: Richtung.GEGEN_RICHTUNG,
      });
      component.onSave();

      verify(netzService.saveFahrtrichtungAttributgruppe(anything())).once();
      expect(capture(netzService.saveFahrtrichtungAttributgruppe).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenId: 1,
          gruppenVersion: 1,
          fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
          fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
        } as SaveFahrtrichtungAttributGruppeCommand,
        {
          kanteId: 1,
          gruppenId: 2,
          gruppenVersion: 3,
          fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
          fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
        } as SaveFahrtrichtungAttributGruppeCommand,
      ]);
    }));

    it('should read undetermined correct, multiple Seiten', fakeAsync(() => {
      // zuerst die linke seite selektieren
      setupSelektion([
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fahrtrichtungAttributGruppe: {
              id: 1,
              version: 1,
              fahrtrichtungLinks: Richtung.IN_RICHTUNG,
              fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
            } as FahrtrichtungAttributGruppe,
          } as Kante,
          Seitenbezug.LINKS
        ),
      ]);

      tick();

      // dann den Wert anpassen
      component.displayedAttributeformGroup.patchValue({
        richtung: Richtung.GEGEN_RICHTUNG,
      });

      // nun die Rechte Seite zusätzlich selektieren, um somit den Undetermined Zustand zu erhalten
      setupSelektion([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fahrtrichtungAttributGruppe: {
            id: 1,
            version: 1,
            fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
            fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
          } as FahrtrichtungAttributGruppe,
        } as Kante),
      ]);

      tick();

      component.onSave();

      verify(netzService.saveFahrtrichtungAttributgruppe(anything())).once();
      expect(capture(netzService.saveFahrtrichtungAttributgruppe).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenId: 1,
          gruppenVersion: 1,
          fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
          fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
        } as SaveFahrtrichtungAttributGruppeCommand,
      ]);
    }));
  });

  describe('edit RadNETZ', () => {
    it('should disable control if RadNETZ-Kante is selected', fakeAsync(() => {
      const kante: Kante = {
        ...defaultKante,
        quelle: QuellSystem.RadNETZ,
      };
      setupSelektion([KantenSelektion.ofGesamteKante(kante)]);

      tick();

      expect(component.displayedAttributeformGroup.disabled).toBeTrue();
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

      expect(component.displayedAttributeformGroup.disabled).toBeTrue();

      setupSelektion([KantenSelektion.ofGesamteKante(kanteDLM)]);

      tick();

      expect(component.displayedAttributeformGroup.disabled).toBeFalse();
    }));
  });
});
