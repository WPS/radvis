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

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { Subject } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { KantenGeschwindigkeitEditorComponent } from 'src/app/editor/kanten/components/kanten-geschwindigkeit-editor/kanten-geschwindigkeit-editor.component';
import { Hoechstgeschwindigkeit } from 'src/app/editor/kanten/models/hoechstgeschwindigkeit';
import { Kante } from 'src/app/editor/kanten/models/kante';
import {
  anotherKante,
  defaultGeschwindigkeitAttribute,
  defaultKante,
} from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenOrtslage } from 'src/app/editor/kanten/models/kanten-ortslage';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { SaveGeschwindigkeitAttributGruppeCommand } from 'src/app/editor/kanten/models/save-geschwindigkeit-attribut-gruppe-command';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe('KantenGeschwindigkeitEditorComponent', () => {
  let component: KantenGeschwindigkeitEditorComponent;
  let fixture: ComponentFixture<KantenGeschwindigkeitEditorComponent>;
  let netzService: NetzService;
  let kantenSelektionService: KantenSelektionService;
  let kantenSubject$: Subject<Kante[]>;
  let kantenSelektionSubject$: Subject<KantenSelektion[]>;
  let notifyUserService: NotifyUserService;

  beforeEach(() => {
    netzService = mock(NetzService);
    kantenSelektionService = mock(KantenSelektionService);
    kantenSubject$ = new Subject();
    kantenSelektionSubject$ = new Subject();
    notifyUserService = mock(NotifyUserService);
    when(kantenSelektionService.selektierteKanten$).thenReturn(kantenSubject$);
    when(kantenSelektionService.selektion$).thenReturn(kantenSelektionSubject$);
    return MockBuilder(KantenGeschwindigkeitEditorComponent, EditorModule)
      .provide({
        provide: NetzService,
        useValue: instance(netzService),
      })
      .provide({ provide: OlMapService, useValue: instance(mock(OlMapComponent)) })
      .provide({ provide: NotifyUserService, useValue: instance(notifyUserService) })
      .provide({ provide: KantenSelektionService, useValue: instance(kantenSelektionService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(KantenGeschwindigkeitEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should handle empty selektion gracefully', fakeAsync(() => {
    kantenSelektionSubject$.next([]);
    tick();

    expect(component.displayedAttributeformGroup.value).toEqual({
      ortslage: null,
      hoechstgeschwindigkeit: null,
      abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung: null,
    });
  }));

  describe('fillForm', () => {
    it('should set undetermined correct, segment', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante(
          {
            ...defaultKante,
            geschwindigkeitAttributGruppe: {
              id: 1,
              version: 1,
              geschwindigkeitAttribute: [
                {
                  ...defaultGeschwindigkeitAttribute,
                  hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_20_KMH,
                },
                {
                  ...defaultGeschwindigkeitAttribute,
                  hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_90_KMH,
                },
              ],
            },
          },
          2,
          2
        ),
      ]);
      tick();

      const { ortslage, hoechstgeschwindigkeit, abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung } =
        component.displayedAttributeformGroup.value;

      expect(ortslage).toEqual(KantenOrtslage.INNERORTS);
      expect(hoechstgeschwindigkeit).toBeInstanceOf(UndeterminedValue);
      expect(abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung).toEqual(Hoechstgeschwindigkeit.UNBEKANNT);
    }));

    it('should set undetermined correct, multiple kanten', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          geschwindigkeitAttributGruppe: {
            id: 1,
            version: 1,
            geschwindigkeitAttribute: [
              {
                ...defaultGeschwindigkeitAttribute,
                hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_20_KMH,
              },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          geschwindigkeitAttributGruppe: {
            id: 1,
            version: 1,
            geschwindigkeitAttribute: [
              {
                ...defaultGeschwindigkeitAttribute,
                hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_90_KMH,
              },
            ],
          },
        }),
      ]);
      tick();

      const { ortslage, hoechstgeschwindigkeit, abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung } =
        component.displayedAttributeformGroup.value;

      expect(ortslage).toEqual(KantenOrtslage.INNERORTS);
      expect(hoechstgeschwindigkeit).toBeInstanceOf(UndeterminedValue);
      expect(abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung).toEqual(Hoechstgeschwindigkeit.UNBEKANNT);
    }));

    it('should set segmentierung correct, zweiseitig', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          geschwindigkeitAttributGruppe: {
            id: 1,
            version: 1,
            geschwindigkeitAttribute: [
              { ...defaultGeschwindigkeitAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultGeschwindigkeitAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          geschwindigkeitAttributGruppe: {
            id: 1,
            version: 1,
            geschwindigkeitAttribute: [
              { ...defaultGeschwindigkeitAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultGeschwindigkeitAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
            ],
          },
        }),
      ]);
      tick();

      const lineareReferenzen: LinearReferenzierterAbschnitt[][] = component.lineareReferenzenFormArray.value;

      expect(lineareReferenzen).toEqual([
        [
          { von: 0, bis: 0.5 },
          { von: 0.5, bis: 1 },
        ],
        [
          { von: 0, bis: 0.9 },
          { von: 0.9, bis: 1 },
        ],
      ]);
    }));
  });

  describe('onSave', () => {
    beforeEach(() => {
      when(netzService.saveGeschwindigkeitsGruppe(anything())).thenResolve();
    });

    it('should read undefined values correct, multiple kanten', fakeAsync(() => {
      const selektion = [
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          geschwindigkeitAttributGruppe: {
            id: 1,
            version: 1,
            geschwindigkeitAttribute: [
              {
                ...defaultGeschwindigkeitAttribute,
                ortslage: KantenOrtslage.INNERORTS,
                hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_30_KMH,
              },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...anotherKante,
          geschwindigkeitAttributGruppe: {
            id: 2,
            version: 3,
            geschwindigkeitAttribute: [
              {
                ...defaultGeschwindigkeitAttribute,
                ortslage: KantenOrtslage.AUSSERORTS,
                hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_90_KMH,
              },
            ],
          },
        }),
      ];
      setupSelektion(selektion);

      tick();
      component.displayedAttributeformGroup.patchValue({
        ortslage: KantenOrtslage.INNERORTS,
      });
      component.onSave();

      verify(netzService.saveGeschwindigkeitsGruppe(anything())).once();
      expect(capture(netzService.saveGeschwindigkeitsGruppe).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          geschwindigkeitAttribute: [
            {
              ...defaultGeschwindigkeitAttribute,
              ortslage: KantenOrtslage.INNERORTS,
              hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_30_KMH,
            },
          ],
        } as SaveGeschwindigkeitAttributGruppeCommand,
        {
          kanteId: 2,
          gruppenID: 2,
          gruppenVersion: 3,
          geschwindigkeitAttribute: [
            {
              ...defaultGeschwindigkeitAttribute,
              ortslage: KantenOrtslage.INNERORTS,
              hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_90_KMH,
            },
          ],
        } as SaveGeschwindigkeitAttributGruppeCommand,
      ]);
    }));

    it('should read undefined values correct, segment', fakeAsync(() => {
      const selektion = [
        KantenSelektion.ofGesamteKante(
          {
            ...defaultKante,
            id: 1,
            geschwindigkeitAttributGruppe: {
              id: 1,
              version: 1,
              geschwindigkeitAttribute: [
                {
                  ...defaultGeschwindigkeitAttribute,
                  ortslage: KantenOrtslage.INNERORTS,
                  hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_30_KMH,
                  linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
                },
                {
                  ...defaultGeschwindigkeitAttribute,
                  ortslage: KantenOrtslage.AUSSERORTS,
                  hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_90_KMH,
                  linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
                },
              ],
            },
          },
          2,
          2
        ),
      ];
      setupSelektion(selektion);

      tick();
      component.displayedAttributeformGroup.patchValue({
        ortslage: KantenOrtslage.INNERORTS,
      });
      component.onSave();

      verify(netzService.saveGeschwindigkeitsGruppe(anything())).once();
      expect(capture(netzService.saveGeschwindigkeitsGruppe).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          geschwindigkeitAttribute: [
            {
              ...defaultGeschwindigkeitAttribute,
              ortslage: KantenOrtslage.INNERORTS,
              hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_30_KMH,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultGeschwindigkeitAttribute,
              ortslage: KantenOrtslage.INNERORTS,
              hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_90_KMH,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            },
          ],
        } as SaveGeschwindigkeitAttributGruppeCommand,
      ]);
    }));

    it('should read lineare referenzen correctly', fakeAsync(() => {
      setupSelektion([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          geschwindigkeitAttributGruppe: {
            id: 1,
            version: 1,
            geschwindigkeitAttribute: [
              { ...defaultGeschwindigkeitAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultGeschwindigkeitAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          geschwindigkeitAttributGruppe: {
            id: 1,
            version: 1,
            geschwindigkeitAttribute: [
              { ...defaultGeschwindigkeitAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultGeschwindigkeitAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
            ],
          },
        }),
      ]);
      tick();

      component.lineareReferenzenFormArray.controls[0].setValue([
        { von: 0, bis: 0.76 },
        { von: 0.76, bis: 1 },
      ]);
      component.lineareReferenzenFormArray.markAsDirty();

      component.onSave();
      tick();

      verify(netzService.saveGeschwindigkeitsGruppe(anything())).once();
      expect(
        capture(netzService.saveGeschwindigkeitsGruppe)
          .last()[0]
          .map(command => command.geschwindigkeitAttribute.map(attr => attr.linearReferenzierterAbschnitt))
      ).toEqual([
        [
          { von: 0, bis: 0.76 },
          { von: 0.76, bis: 1 },
        ],
        [
          { von: 0, bis: 0.9 },
          { von: 0.9, bis: 1 },
        ],
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
      expect(component.lineareReferenzenFormArray.disabled).toBeTrue();
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
      expect(component.lineareReferenzenFormArray.disabled).toBeTrue();

      setupSelektion([KantenSelektion.ofGesamteKante(kanteDLM)]);

      tick();

      expect(component.displayedAttributeformGroup.disabled).toBeFalse();
      expect(component.lineareReferenzenFormArray.disabled).toBeFalse();
    }));
  });

  const setupSelektion = (selektion: KantenSelektion[]): void => {
    when(kantenSelektionService.selektion).thenReturn(selektion);
    when(kantenSelektionService.selektierteKanten).thenReturn(selektion.map(s => s.kante));
    kantenSubject$.next(selektion.map(s => s.kante));
    kantenSelektionSubject$.next(selektion);
  };
});
