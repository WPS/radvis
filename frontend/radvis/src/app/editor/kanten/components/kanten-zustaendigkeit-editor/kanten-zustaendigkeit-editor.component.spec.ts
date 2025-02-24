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
import { Kante } from 'src/app/editor/kanten/models/kante';
import {
  anotherKante,
  defaultKante,
  defaultZustaendigkeitAttribute,
} from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { SaveZustaendigkeitAttributGruppeCommand } from 'src/app/editor/kanten/models/save-zustaendigkeit-attribut-gruppe-command';
import { ZustaendigkeitAttribute } from 'src/app/editor/kanten/models/zustaendigkeit-attribute';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import {
  defaultOrganisation,
  defaultUebergeordneteOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { KantenZustaendigkeitEditorComponent } from './kanten-zustaendigkeit-editor.component';

describe('KantenZustaendigkeitEditorComponent', () => {
  let component: KantenZustaendigkeitEditorComponent;
  let fixture: ComponentFixture<KantenZustaendigkeitEditorComponent>;
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
    return MockBuilder(KantenZustaendigkeitEditorComponent, EditorModule)
      .provide({
        provide: NetzService,
        useValue: instance(netzService),
      })
      .provide({ provide: NotifyUserService, useValue: instance(notifyUserService) })
      .provide({ provide: KantenSelektionService, useValue: instance(kantenSelektionService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(KantenZustaendigkeitEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should handle empty selektion gracefully', fakeAsync(() => {
    kantenSelektionSubject$.next([]);
    tick();

    expect(component.displayedAttributeformGroup.value).toEqual({
      baulastTraeger: null,
      unterhaltsZustaendiger: null,
      erhaltsZustaendiger: null,
      vereinbarungsKennung: null,
    });
  }));

  describe('fillForm', () => {
    it('should set undetermined correct, segment', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante(
          {
            ...defaultKante,
            zustaendigkeitAttributGruppe: {
              id: 1,
              version: 1,
              zustaendigkeitAttribute: [
                {
                  ...defaultZustaendigkeitAttribute,
                  erhaltsZustaendiger: defaultOrganisation,
                  vereinbarungsKennung: 'a',
                },
                {
                  ...defaultZustaendigkeitAttribute,
                  erhaltsZustaendiger: defaultUebergeordneteOrganisation,
                  vereinbarungsKennung: 'b',
                },
              ],
            },
          },
          2,
          2
        ),
      ]);
      tick();

      const { erhaltsZustaendiger, vereinbarungsKennung, ...equalValues } = component.displayedAttributeformGroup.value;

      expect(erhaltsZustaendiger).toBeInstanceOf(UndeterminedValue);
      expect(vereinbarungsKennung).toBeInstanceOf(UndeterminedValue);
      expect(equalValues).toEqual({
        baulastTraeger: defaultZustaendigkeitAttribute.baulastTraeger,
        unterhaltsZustaendiger: defaultZustaendigkeitAttribute.unterhaltsZustaendiger,
      });
    }));

    it('should set undetermined correct, multiple kanten', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zustaendigkeitAttributGruppe: {
            id: 1,
            version: 1,
            zustaendigkeitAttribute: [
              {
                ...defaultZustaendigkeitAttribute,
                erhaltsZustaendiger: defaultOrganisation,
                vereinbarungsKennung: 'a',
              },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zustaendigkeitAttributGruppe: {
            id: 1,
            version: 1,
            zustaendigkeitAttribute: [
              {
                ...defaultZustaendigkeitAttribute,
                erhaltsZustaendiger: defaultUebergeordneteOrganisation,
                vereinbarungsKennung: 'b',
              },
            ],
          },
        }),
      ]);
      tick();

      const { erhaltsZustaendiger, vereinbarungsKennung, ...equalValues } = component.displayedAttributeformGroup.value;

      expect(erhaltsZustaendiger).toBeInstanceOf(UndeterminedValue);
      expect(vereinbarungsKennung).toBeInstanceOf(UndeterminedValue);
      expect(equalValues).toEqual({
        baulastTraeger: defaultZustaendigkeitAttribute.baulastTraeger,
        unterhaltsZustaendiger: defaultZustaendigkeitAttribute.unterhaltsZustaendiger,
      });
    }));

    it('should set segmentierung correct, zweiseitig', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zustaendigkeitAttributGruppe: {
            id: 1,
            version: 1,
            zustaendigkeitAttribute: [
              { ...defaultZustaendigkeitAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultZustaendigkeitAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zustaendigkeitAttributGruppe: {
            id: 1,
            version: 1,
            zustaendigkeitAttribute: [
              { ...defaultZustaendigkeitAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultZustaendigkeitAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
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
      when(netzService.saveZustaendigkeitsGruppe(anything())).thenResolve();
    });

    it('should read undefined values correct, multiple kanten', fakeAsync(() => {
      const selektion = [
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zustaendigkeitAttributGruppe: {
            id: 1,
            version: 1,
            zustaendigkeitAttribute: [
              {
                ...defaultZustaendigkeitAttribute,
                erhaltsZustaendiger: defaultOrganisation,
                unterhaltsZustaendiger: defaultOrganisation,
                vereinbarungsKennung: 'a',
              },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...anotherKante,
          zustaendigkeitAttributGruppe: {
            id: 2,
            version: 3,
            zustaendigkeitAttribute: [
              {
                erhaltsZustaendiger: defaultUebergeordneteOrganisation,
                unterhaltsZustaendiger: defaultUebergeordneteOrganisation,
                baulastTraeger: defaultUebergeordneteOrganisation,
                vereinbarungsKennung: 'b',
                linearReferenzierterAbschnitt: defaultZustaendigkeitAttribute.linearReferenzierterAbschnitt,
              },
            ],
          },
        }),
      ];
      setupSelektion(selektion);

      tick();
      component.displayedAttributeformGroup.patchValue({
        unterhaltsZustaendiger: defaultOrganisation,
      });
      component.onSave();

      verify(netzService.saveZustaendigkeitsGruppe(anything())).once();
      expect(capture(netzService.saveZustaendigkeitsGruppe).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          zustaendigkeitAttribute: [
            {
              erhaltsZustaendiger: defaultOrganisation.id,
              unterhaltsZustaendiger: defaultOrganisation.id,
              baulastTraeger: defaultOrganisation.id,
              vereinbarungsKennung: 'a',
              linearReferenzierterAbschnitt: defaultZustaendigkeitAttribute.linearReferenzierterAbschnitt,
            },
          ],
        } as SaveZustaendigkeitAttributGruppeCommand,
        {
          kanteId: 2,
          gruppenID: 2,
          gruppenVersion: 3,
          zustaendigkeitAttribute: [
            {
              erhaltsZustaendiger: defaultUebergeordneteOrganisation.id,
              unterhaltsZustaendiger: defaultOrganisation.id,
              baulastTraeger: defaultUebergeordneteOrganisation.id,
              vereinbarungsKennung: 'b',
              linearReferenzierterAbschnitt: defaultZustaendigkeitAttribute.linearReferenzierterAbschnitt,
            },
          ],
        } as SaveZustaendigkeitAttributGruppeCommand,
      ]);
    }));

    it('should read undefined values correct, segment', fakeAsync(() => {
      const selektion = [
        KantenSelektion.ofGesamteKante(
          {
            ...defaultKante,
            id: 1,
            zustaendigkeitAttributGruppe: {
              id: 1,
              version: 1,
              zustaendigkeitAttribute: [
                {
                  erhaltsZustaendiger: defaultOrganisation,
                  unterhaltsZustaendiger: defaultOrganisation,
                  baulastTraeger: defaultOrganisation,
                  vereinbarungsKennung: 'a',
                  linearReferenzierterAbschnitt: defaultZustaendigkeitAttribute.linearReferenzierterAbschnitt,
                },
                {
                  erhaltsZustaendiger: defaultUebergeordneteOrganisation,
                  unterhaltsZustaendiger: defaultUebergeordneteOrganisation,
                  baulastTraeger: defaultUebergeordneteOrganisation,
                  vereinbarungsKennung: 'b',
                  linearReferenzierterAbschnitt: defaultZustaendigkeitAttribute.linearReferenzierterAbschnitt,
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
        unterhaltsZustaendiger: defaultOrganisation,
      });
      component.onSave();

      verify(netzService.saveZustaendigkeitsGruppe(anything())).once();
      expect(capture(netzService.saveZustaendigkeitsGruppe).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          zustaendigkeitAttribute: [
            {
              erhaltsZustaendiger: defaultOrganisation.id,
              unterhaltsZustaendiger: defaultOrganisation.id,
              baulastTraeger: defaultOrganisation.id,
              vereinbarungsKennung: 'a',
              linearReferenzierterAbschnitt: defaultZustaendigkeitAttribute.linearReferenzierterAbschnitt,
            },
            {
              erhaltsZustaendiger: defaultUebergeordneteOrganisation.id,
              unterhaltsZustaendiger: defaultOrganisation.id,
              baulastTraeger: defaultUebergeordneteOrganisation.id,
              vereinbarungsKennung: 'b',
              linearReferenzierterAbschnitt: defaultZustaendigkeitAttribute.linearReferenzierterAbschnitt,
            },
          ],
        } as SaveZustaendigkeitAttributGruppeCommand,
      ]);
    }));

    it('should read lineare referenzen correctly', fakeAsync(() => {
      setupSelektion([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zustaendigkeitAttributGruppe: {
            id: 1,
            version: 1,
            zustaendigkeitAttribute: [
              { ...defaultZustaendigkeitAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultZustaendigkeitAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zustaendigkeitAttributGruppe: {
            id: 1,
            version: 1,
            zustaendigkeitAttribute: [
              { ...defaultZustaendigkeitAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultZustaendigkeitAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
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

      verify(netzService.saveZustaendigkeitsGruppe(anything())).once();
      expect(
        capture(netzService.saveZustaendigkeitsGruppe)
          .last()[0]
          .map(command => command.zustaendigkeitAttribute.map(attr => attr.linearReferenzierterAbschnitt))
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

    it('should reenable controls when last RadNETZ-Kante is deselected', fakeAsync(() => {
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

  describe('Validierung', () => {
    it('should autocorrect attributes', fakeAsync(() => {
      component.displayedAttributeformGroup
        .get('vereinbarungsKennung')
        ?.setValue(
          'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimatax'
        );
      tick();
      verify(notifyUserService.inform(anything())).once();
      expect(component.displayedAttributeformGroup.value.vereinbarungsKennung).toEqual(
        'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata'
      );
    }));
  });

  describe('Selektion', () => {
    it('should have lineare referenz in correct order', fakeAsync(() => {
      const zustaendigkeitAttribute: ZustaendigkeitAttribute[] = [
        {
          ...defaultZustaendigkeitAttribute,
          linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
        },
        {
          ...defaultZustaendigkeitAttribute,
          linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 },
        },
        {
          ...defaultZustaendigkeitAttribute,
          linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
        },
      ];
      const kante: Kante = {
        ...defaultKante,
        zustaendigkeitAttributGruppe: {
          id: 1,
          version: 1,
          zustaendigkeitAttribute: [
            // falscher Reihenfolge ist absicht
            zustaendigkeitAttribute[1],
            zustaendigkeitAttribute[0],
            zustaendigkeitAttribute[2],
          ],
        },
      };
      setupSelektion([KantenSelektion.ofGesamteKante(kante)]);

      tick();

      const [selektion]: KantenSelektion[] = component.currentSelektion || [];

      const [vonBis1, vonBis2, vonBis3]: LinearReferenzierterAbschnitt[] =
        selektion.kante.zustaendigkeitAttributGruppe.zustaendigkeitAttribute.map(a => a.linearReferenzierterAbschnitt);

      expect(vonBis1).toEqual({ von: 0, bis: 0.3 } as LinearReferenzierterAbschnitt);
      expect(vonBis2).toEqual({ von: 0.3, bis: 0.7 } as LinearReferenzierterAbschnitt);
      expect(vonBis3).toEqual({ von: 0.7, bis: 1 } as LinearReferenzierterAbschnitt);
    }));
  });

  const setupSelektion = (selektion: KantenSelektion[]): void => {
    when(kantenSelektionService.selektion).thenReturn(selektion);
    when(kantenSelektionService.selektierteKanten).thenReturn(selektion.map(s => s.kante));
    kantenSubject$.next(selektion.map(s => s.kante));
    kantenSelektionSubject$.next(selektion);
  };
});

// {
//   "von": 0.3,
//   "bis": 0.7
// }
