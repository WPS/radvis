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

import { BreakpointObserver } from '@angular/cdk/layout';
import { fakeAsync, tick } from '@angular/core/testing';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { KantenFuehrungsformEditorComponent } from 'src/app/editor/kanten/components/kanten-fuehrungsform-editor/kanten-fuehrungsform-editor.component';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { Beschilderung } from 'src/app/editor/kanten/models/beschilderung';
import { Bordstein } from 'src/app/editor/kanten/models/bordstein';
import { FuehrungsformAttribute } from 'src/app/editor/kanten/models/fuehrungsform-attribute';
import { Kante } from 'src/app/editor/kanten/models/kante';
import {
  defaultFuehrungsformAttribute,
  defaultKante,
} from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { SaveFuehrungsformAttributGruppeCommand } from 'src/app/editor/kanten/models/save-fuehrungsform-attribut-gruppe-command';
import { TrennstreifenTrennungZu } from 'src/app/editor/kanten/models/trennstreifen-trennung-zu';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NetzBearbeitungModusService } from 'src/app/editor/kanten/services/netz-bearbeitung-modus.service';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardGuardService } from 'src/app/shared/services/discard-guard.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { LadeZustandService } from 'src/app/shared/services/lade-zustand.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { anything, capture, instance, mock, resetCalls, verify, when } from 'ts-mockito';

describe(KantenFuehrungsformEditorComponent.name, () => {
  let component: KantenFuehrungsformEditorComponent;
  let fixture: MockedComponentFixture<KantenFuehrungsformEditorComponent>;
  let netzService: NetzService;
  let kantenSelektionService: KantenSelektionService;
  let discardGuardService: DiscardGuardService;
  let kantenSubject$: Subject<Kante[]>;
  let kantenSelektionSubject$: Subject<KantenSelektion[]>;
  let benutzerDetails: BenutzerDetailsService;
  let olMapService: OlMapService;

  beforeEach(() => {
    netzService = mock(NetzService);
    benutzerDetails = mock(BenutzerDetailsService);
    olMapService = mock(OlMapComponent);

    discardGuardService = mock(DiscardGuardService);
    when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

    kantenSelektionService = mock(KantenSelektionService);
    kantenSubject$ = new Subject();
    kantenSelektionSubject$ = new Subject();
    when(kantenSelektionService.selektierteKanten$).thenReturn(kantenSubject$);
    when(kantenSelektionService.selektion$).thenReturn(kantenSelektionSubject$);

    return MockBuilder(KantenFuehrungsformEditorComponent, EditorModule)
      .provide({
        provide: NetzService,
        useValue: instance(netzService),
      })
      .provide({
        provide: BenutzerDetailsService,
        useValue: instance(benutzerDetails),
      })
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: KantenSelektionService,
        useValue: instance(kantenSelektionService),
      })
      .provide({
        provide: DiscardGuardService,
        useValue: instance(discardGuardService),
      })
      .keep(BreakpointObserver);
  });

  beforeEach(() => {
    fixture = MockRender(KantenFuehrungsformEditorComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('fillForm', () => {
    it('should set undetermined correct, seite', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.ASPHALT }],
            fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
          },
        }),
      ]);
      tick();

      const { belagArt, ...equalValues } = component.displayedAttributeformGroup.value;

      expect(belagArt).toBeInstanceOf(UndeterminedValue);
      expect(equalValues).toEqual({
        oberflaechenbeschaffenheit: defaultFuehrungsformAttribute.oberflaechenbeschaffenheit,
        bordstein: defaultFuehrungsformAttribute.bordstein,
        radverkehrsfuehrung: defaultFuehrungsformAttribute.radverkehrsfuehrung,
        benutzungspflicht: defaultFuehrungsformAttribute.benutzungspflicht,
        breite: defaultFuehrungsformAttribute.breite,
        parkenTyp: defaultFuehrungsformAttribute.parkenTyp,
        parkenForm: defaultFuehrungsformAttribute.parkenForm,
        beschilderung: defaultFuehrungsformAttribute.beschilderung,
        schaeden: defaultFuehrungsformAttribute.schaeden,
        absenkung: defaultFuehrungsformAttribute.absenkung,
      });
    }));

    it('should set undetermined correct, segment', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeLinks: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.ASPHALT },
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          KantenSeite.LINKS,
          2
        ),
      ]);
      tick();

      const { belagArt, ...equalValues } = component.displayedAttributeformGroup.value;

      expect(belagArt).toBeInstanceOf(UndeterminedValue);
      expect(equalValues).toEqual({
        oberflaechenbeschaffenheit: defaultFuehrungsformAttribute.oberflaechenbeschaffenheit,
        bordstein: defaultFuehrungsformAttribute.bordstein,
        radverkehrsfuehrung: defaultFuehrungsformAttribute.radverkehrsfuehrung,
        benutzungspflicht: defaultFuehrungsformAttribute.benutzungspflicht,
        breite: defaultFuehrungsformAttribute.breite,
        parkenTyp: defaultFuehrungsformAttribute.parkenTyp,
        parkenForm: defaultFuehrungsformAttribute.parkenForm,
        beschilderung: defaultFuehrungsformAttribute.beschilderung,
        schaeden: defaultFuehrungsformAttribute.schaeden,
        absenkung: defaultFuehrungsformAttribute.absenkung,
      });
    }));

    it('should set undetermined correct, multiple kanten', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeLinks: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.ASPHALT }],
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          KantenSeite.LINKS
        ),
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
              fuehrungsformAttributeLinks: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          KantenSeite.RECHTS
        ),
      ]);
      tick();

      const { belagArt, ...equalValues } = component.displayedAttributeformGroup.value;

      expect(belagArt).toBeInstanceOf(UndeterminedValue);
      expect(equalValues).toEqual({
        oberflaechenbeschaffenheit: defaultFuehrungsformAttribute.oberflaechenbeschaffenheit,
        bordstein: defaultFuehrungsformAttribute.bordstein,
        radverkehrsfuehrung: defaultFuehrungsformAttribute.radverkehrsfuehrung,
        benutzungspflicht: defaultFuehrungsformAttribute.benutzungspflicht,
        breite: defaultFuehrungsformAttribute.breite,
        parkenTyp: defaultFuehrungsformAttribute.parkenTyp,
        parkenForm: defaultFuehrungsformAttribute.parkenForm,
        beschilderung: defaultFuehrungsformAttribute.beschilderung,
        schaeden: defaultFuehrungsformAttribute.schaeden,
        absenkung: defaultFuehrungsformAttribute.absenkung,
      });
    }));

    it('should set trennstreifenValuesCorrect after delete segment rechts (RAD-7134)', fakeAsync(() => {
      const selektion = KantenSelektion.ofGesamteKante(
        {
          ...defaultKante,
          zweiseitig: true,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 1 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 1 } },
            ],
          },
        },
        2,
        2
      );
      when(kantenSelektionService.adjustSelectionForSegmentDeletion(anything(), anything(), anything())).thenCall(
        (id, index, seite) => {
          const newSelektion = selektion.deleteSegment(index, seite);
          kantenSelektionSubject$.next([newSelektion]);
        }
      );
      kantenSelektionSubject$.next([selektion]);
      tick();

      component.onDeleteAtIndex(0, 1, KantenSeite.RECHTS);
      tick();

      expect(component['currentAttributgruppen'][0].fuehrungsformAttributeRechts.length).toBe(1);
    }));

    it('should set segmentierung correct, zweiseitig', fakeAsync(() => {
      kantenSelektionSubject$.next([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.7 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              {
                ...defaultFuehrungsformAttribute,
                linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
            ],
          },
        }),
      ]);
      tick();

      const lrLinks: LinearReferenzierterAbschnitt[][] = component.lineareReferenzenLinksFormArray.value;
      const lrEinseitig: LinearReferenzierterAbschnitt[][] = component.lineareReferenzenFormArray.value;
      const lrRechts: LinearReferenzierterAbschnitt[][] = component.lineareReferenzenRechtsFormArray.value;

      expect(lrLinks).toEqual([
        [
          { von: 0, bis: 0.5 },
          { von: 0.5, bis: 1 },
        ],
        [{ von: 0, bis: 1 }],
      ]);

      expect(lrEinseitig).toEqual([
        [
          { von: 0, bis: 0.5 },
          { von: 0.5, bis: 1 },
        ],
        [{ von: 0, bis: 1 }],
      ]);

      expect(lrRechts).toEqual([
        [
          { von: 0, bis: 0.7 },
          { von: 0.7, bis: 1 },
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
      when(netzService.saveKanteFuehrungsform(anything())).thenResolve();
    });

    it('should read undefined values correct, multiple kanten', () => {
      const selektion = [
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            id: 1,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeLinks: [
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.ASPHALT,
                  bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
                },
              ],
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          KantenSeite.LINKS
        ),
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            id: 2,
            fuehrungsformAttributGruppe: {
              id: 2,
              version: 3,
              fuehrungsformAttributeLinks: [
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.NATURSTEINPFLASTER,
                  bordstein: Bordstein.KEINE_ABSENKUNG,
                },
              ],
              fuehrungsformAttributeRechts: [
                { ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER },
              ],
            },
          },
          KantenSeite.LINKS
        ),
      ];
      setupSelektion(selektion);

      component.displayedAttributeformGroup.patchValue({
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
      });
      component.onSave();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.ASPHALT,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
            },
          ],
          fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
        } as SaveFuehrungsformAttributGruppeCommand,
        {
          kanteId: 2,
          gruppenID: 2,
          gruppenVersion: 3,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.NATURSTEINPFLASTER,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
            },
          ],
          fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    });

    it('should read undefined values correct, seite', () => {
      const selektion = [
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          id: 1,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              {
                ...defaultFuehrungsformAttribute,
                belagArt: BelagArt.ASPHALT,
                bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
              },
            ],
            fuehrungsformAttributeRechts: [
              {
                ...defaultFuehrungsformAttribute,
                belagArt: BelagArt.NATURSTEINPFLASTER,
                bordstein: Bordstein.KEINE_ABSENKUNG,
              },
            ],
          },
        }),
      ];
      setupSelektion(selektion);

      component.displayedAttributeformGroup.patchValue({
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
      });
      component.onSave();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.ASPHALT,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.NATURSTEINPFLASTER,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
            },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    });

    it('should read undefined values correct, segment', () => {
      const selektion = [
        KantenSelektion.ofSeite(
          {
            ...defaultKante,
            id: 1,
            fuehrungsformAttributGruppe: {
              id: 1,
              version: 1,
              fuehrungsformAttributeLinks: [
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.ASPHALT,
                  bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
                },
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.NATURSTEINPFLASTER,
                  bordstein: Bordstein.KEINE_ABSENKUNG,
                },
              ],
              fuehrungsformAttributeRechts: [
                {
                  ...defaultFuehrungsformAttribute,
                  belagArt: BelagArt.NATURSTEINPFLASTER,
                },
              ],
            },
          },
          KantenSeite.LINKS,
          2
        ),
      ];
      setupSelektion(selektion);

      component.displayedAttributeformGroup.patchValue({
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
      });
      component.onSave();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: 1,
          gruppenID: 1,
          gruppenVersion: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.ASPHALT,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
              trennstreifenFormLinks: null,
              trennstreifenTrennungZuLinks: null,
              trennstreifenBreiteLinks: null,
              trennstreifenFormRechts: null,
              trennstreifenTrennungZuRechts: null,
              trennstreifenBreiteRechts: null,
            },
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.NATURSTEINPFLASTER,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
              trennstreifenFormLinks: null,
              trennstreifenTrennungZuLinks: null,
              trennstreifenBreiteLinks: null,
              trennstreifenFormRechts: null,
              trennstreifenTrennungZuRechts: null,
              trennstreifenBreiteRechts: null,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              belagArt: BelagArt.NATURSTEINPFLASTER,
            },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    });

    it('should read lineare referenzen, einseitig', fakeAsync(() => {
      setupSelektion([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.7 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              {
                ...defaultFuehrungsformAttribute,
                linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
            ],
          },
        }),
      ]);

      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.76 },
        { von: 0.76, bis: 1 },
      ]);

      component.lineareReferenzenRechtsFormArray.controls[1].setValue([
        { von: 0, bis: 0.8 },
        { von: 0.8, bis: 1 },
      ]);

      component.lineareReferenzenLinksFormArray.markAsDirty();
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      component.onSave();
      tick();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(
        capture(netzService.saveKanteFuehrungsform)
          .last()[0]
          .map(command => command.fuehrungsformAttributeLinks.map(attr => attr.linearReferenzierterAbschnitt))
      ).toEqual([
        [
          { von: 0, bis: 0.76 },
          { von: 0.76, bis: 1 },
        ],
        [{ von: 0, bis: 1 }],
      ]);
      expect(
        capture(netzService.saveKanteFuehrungsform)
          .last()[0]
          .map(command => command.fuehrungsformAttributeRechts.map(attr => attr.linearReferenzierterAbschnitt))
      ).toEqual([
        [
          { von: 0, bis: 0.7 },
          { von: 0.7, bis: 1 },
        ],
        [
          { von: 0, bis: 0.8 },
          { von: 0.8, bis: 1 },
        ],
      ]);
    }));

    it('should read lineare referenzen, zweiseitig', fakeAsync(() => {
      setupSelektion([
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zweiseitig: false,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.7 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
            ],
          },
        }),
        KantenSelektion.ofGesamteKante({
          ...defaultKante,
          zweiseitig: true,
          fuehrungsformAttributGruppe: {
            id: 1,
            version: 1,
            fuehrungsformAttributeLinks: [
              {
                ...defaultFuehrungsformAttribute,
                linearReferenzierterAbschnitt: { von: 0, bis: 1 },
              },
            ],
            fuehrungsformAttributeRechts: [
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.9 } },
              { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.9, bis: 1 } },
            ],
          },
        }),
      ]);

      component.lineareReferenzenFormArray.controls[0].setValue([
        { von: 0, bis: 0.76 },
        { von: 0.76, bis: 1 },
      ]);

      component.lineareReferenzenRechtsFormArray.controls[1].setValue([
        { von: 0, bis: 0.8 },
        { von: 0.8, bis: 1 },
      ]);

      component.lineareReferenzenFormArray.markAsDirty();
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      component.onSave();
      tick();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(
        capture(netzService.saveKanteFuehrungsform)
          .last()[0]
          .map(command => command.fuehrungsformAttributeLinks.map(attr => attr.linearReferenzierterAbschnitt))
      ).toEqual([
        [
          { von: 0, bis: 0.76 },
          { von: 0.76, bis: 1 },
        ],
        [{ von: 0, bis: 1 }],
      ]);
      expect(
        capture(netzService.saveKanteFuehrungsform)
          .last()[0]
          .map(command => command.fuehrungsformAttributeRechts.map(attr => attr.linearReferenzierterAbschnitt))
      ).toEqual([
        [
          { von: 0, bis: 0.76 },
          { von: 0.76, bis: 1 },
        ],
        [
          { von: 0, bis: 0.8 },
          { von: 0.8, bis: 1 },
        ],
      ]);
    }));
  });

  describe('edit RadNETZ', () => {
    it('should disable control if RadNETZ-Kante is selected', () => {
      const kante: Kante = {
        ...defaultKante,
        quelle: QuellSystem.RadNETZ,
      };
      setupSelektion([KantenSelektion.ofGesamteKante(kante)]);

      expect(component.displayedAttributeformGroup.disabled).toBeTrue();
      expect(component.lineareReferenzenFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenLinksFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenRechtsFormArray.disabled).toBeTrue();
    });

    it('should reanable controls when last RadNETZ-Kante is deselected', () => {
      const kanteRadNETZ: Kante = {
        ...defaultKante,
        quelle: QuellSystem.RadNETZ,
      };
      const kanteDLM: Kante = {
        ...defaultKante,
        quelle: QuellSystem.DLM,
      };
      setupSelektion([kanteRadNETZ, kanteDLM].map(k => KantenSelektion.ofGesamteKante(k)));

      expect(component.displayedAttributeformGroup.disabled).toBeTrue();
      expect(component.lineareReferenzenFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenLinksFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenRechtsFormArray.disabled).toBeTrue();

      setupSelektion([KantenSelektion.ofGesamteKante(kanteDLM)]);

      expect(component.displayedAttributeformGroup.disabled).toBeFalse();
      expect(component.lineareReferenzenFormArray.disabled).toBeFalse();
      expect(component.lineareReferenzenLinksFormArray.disabled).toBeFalse();
      expect(component.lineareReferenzenRechtsFormArray.disabled).toBeFalse();
    });
  });

  describe('onRadverkehsfuehrungChanged', () => {
    describe('should filter enums initially', () => {
      it('should filter beschilderung', () => {
        const kante: Kante = {
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            ...defaultKante.fuehrungsformAttributGruppe,
            fuehrungsformAttributeLinks: [
              { ...defaultFuehrungsformAttribute, radverkehrsfuehrung: Radverkehrsfuehrung.BEGEGNUNBSZONE },
            ],
          },
        };
        setupSelektion([KantenSelektion.ofSeite(kante, KantenSeite.LINKS)]);

        expect(component.beschilderungOptions.filter(opt => opt.disabled).length).toBe(4);
      });

      it('should filter trennung zu', () => {
        const kante: Kante = {
          ...defaultKante,
          fuehrungsformAttributGruppe: {
            ...defaultKante.fuehrungsformAttributGruppe,
            fuehrungsformAttributeLinks: [
              {
                ...defaultFuehrungsformAttribute,
                radverkehrsfuehrung: Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG,
              },
            ],
          },
        };
        setupSelektion([KantenSelektion.ofSeite(kante, KantenSeite.LINKS)]);

        expect(component.trennstreifenTrennungZuOptions.filter(opt => !opt.disabled).length).toBe(1);
      });
    });

    it('should reset trennungZuOptions to all if null', () => {
      component.trennstreifenTrennungZuOptions = [];
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(null);
      expect(component.trennstreifenTrennungZuOptions).toEqual(TrennstreifenTrennungZu.options);
    });

    it('should reset trennungZuOptions to all if undetermined', () => {
      component.trennstreifenTrennungZuOptions = [];
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(new UndeterminedValue());
      expect(component.trennstreifenTrennungZuOptions).toEqual(TrennstreifenTrennungZu.options);
    });

    it('should reset trennungZuOptions to allowedOptions if value', () => {
      component.trennstreifenTrennungZuOptions = [];
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
        Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG
      );
      expect(component.trennstreifenTrennungZuOptions.filter(opt => !opt.disabled).map(opt => opt.name)).toEqual([
        TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
      ]);
    });

    it('should reset beschilderungOptions to all if null', () => {
      component.beschilderungOptions = [];
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(null);
      expect(component.beschilderungOptions).toEqual(Beschilderung.options);
    });

    it('should reset beschilderungOptions to all if undetermined', () => {
      component.beschilderungOptions = [];
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(new UndeterminedValue());
      expect(component.beschilderungOptions).toEqual(Beschilderung.options);
    });

    it('should reset beschilderungOptions to allowedOptions if value', () => {
      component.beschilderungOptions = [];
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(Radverkehrsfuehrung.BEGEGNUNBSZONE);
      expect(component.beschilderungOptions.filter(opt => opt.disabled).length).toBe(4);
    });
  });

  describe('beschilderung validierung', () => {
    it('should be valid if radverkehrsfuehrung null', () => {
      component.displayedAttributeformGroup.patchValue({
        radverkehrsfuehrung: null,
        beschilderung: Beschilderung.UNBEKANNT,
      });

      expect(component.displayedAttributeformGroup.controls.beschilderung.valid).toBeTrue();
    });

    it('should be valid if beschilderung null', () => {
      component.displayedAttributeformGroup.patchValue({
        radverkehrsfuehrung: Radverkehrsfuehrung.BEGEGNUNBSZONE,
        beschilderung: null,
      });

      expect(component.displayedAttributeformGroup.controls.beschilderung.valid).toBeTrue();
    });

    it('should be valid if beschilderung undetermined', () => {
      component.displayedAttributeformGroup.patchValue({
        radverkehrsfuehrung: new UndeterminedValue(),
        beschilderung: Beschilderung.UNBEKANNT,
      });

      expect(component.displayedAttributeformGroup.controls.beschilderung.valid).toBeTrue();
    });

    it('should be valid if beschilderung undetermined', () => {
      component.displayedAttributeformGroup.patchValue({
        radverkehrsfuehrung: Radverkehrsfuehrung.BEGEGNUNBSZONE,
        beschilderung: new UndeterminedValue(),
      });

      expect(component.displayedAttributeformGroup.controls.beschilderung.valid).toBeTrue();
    });

    it('should be valid if beschilderung passt zu radverkehrsfuehrung', () => {
      component.displayedAttributeformGroup.patchValue({
        radverkehrsfuehrung: Radverkehrsfuehrung.BEGEGNUNBSZONE,
        beschilderung: Beschilderung.GEHWEG_MIT_VZ_239,
      });

      expect(component.displayedAttributeformGroup.controls.beschilderung.valid).toBeTrue();

      component.displayedAttributeformGroup.patchValue({
        radverkehrsfuehrung: Radverkehrsfuehrung.BETRIEBSWEG_FORST,
        beschilderung: Beschilderung.ZUSATZZEICHEN_NICHT_VORHANDEN,
      });

      expect(component.displayedAttributeformGroup.controls.beschilderung.valid).toBeTrue();
    });

    it('should be invalid if beschilderung passt nicht zu radverkehrsfuehrung', () => {
      component.displayedAttributeformGroup.patchValue({
        radverkehrsfuehrung: Radverkehrsfuehrung.BEGEGNUNBSZONE,
        beschilderung: Beschilderung.ZUSATZZEICHEN_NICHT_VORHANDEN,
      });

      expect(component.displayedAttributeformGroup.controls.beschilderung.valid).toBeFalse();
    });

    it('should update validity on radverkehrsfuehrung changed', () => {
      component.displayedAttributeformGroup.patchValue({
        radverkehrsfuehrung: Radverkehrsfuehrung.BEGEGNUNBSZONE,
        beschilderung: Beschilderung.ZUSATZZEICHEN_NICHT_VORHANDEN,
      });

      expect(component.displayedAttributeformGroup.controls.beschilderung.valid).toBeFalse();

      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
        Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG
      );
      expect(component.displayedAttributeformGroup.controls.beschilderung.valid).toBeTrue();
    });
  });

  const setupSelektion = (selektion: KantenSelektion[]): void => {
    when(kantenSelektionService.selektion).thenReturn(selektion);
    when(kantenSelektionService.selektierteKanten).thenReturn(selektion.map(s => s.kante));
    kantenSubject$.next(selektion.map(s => s.kante));
    kantenSelektionSubject$.next(selektion);
  };
});

describe(KantenFuehrungsformEditorComponent.name + ' - embedded', () => {
  let component: KantenFuehrungsformEditorComponent;
  let fixture: MockedComponentFixture<KantenFuehrungsformEditorComponent>;
  let kantenSelektionService: KantenSelektionService;
  let netzService: NetzService;
  let benutzerDetailsService: BenutzerDetailsService;
  let olMapService: OlMapService;
  let discardGuardService: DiscardGuardService;

  beforeEach(() => {
    const netzbearbeitungsModusService = mock(NetzBearbeitungModusService);

    benutzerDetailsService = mock(BenutzerDetailsService);
    when(netzbearbeitungsModusService.getAktiveKantenGruppe()).thenReturn(of(AttributGruppe.FUEHRUNGSFORM));

    netzService = mock(NetzService);
    when(netzService.saveKanteFuehrungsform(anything())).thenResolve([]);

    olMapService = mock(OlMapComponent);

    discardGuardService = mock(DiscardGuardService);
    when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

    return MockBuilder(KantenFuehrungsformEditorComponent, EditorModule)
      .keep(KantenSelektionService)
      .provide({ provide: NetzService, useValue: instance(netzService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(mock(ErrorHandlingService)) })
      .provide({ provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) })
      .provide({ provide: LadeZustandService, useValue: instance(mock(LadeZustandService)) })
      .provide({ provide: NetzBearbeitungModusService, useValue: instance(netzbearbeitungsModusService) })
      .provide({ provide: BenutzerDetailsService, useValue: instance(benutzerDetailsService) })
      .provide({ provide: OlMapService, useValue: instance(olMapService) })
      .provide({ provide: DiscardGuardService, useValue: instance(discardGuardService) })
      .keep(BreakpointObserver);
  });

  beforeEach(() => {
    fixture = MockRender(KantenFuehrungsformEditorComponent);
    fixture.detectChanges();
    component = fixture.point.componentInstance;
    kantenSelektionService = component['kantenSelektionService'];
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });

  it('should not fill lineare referenzen if only segment selection changed', fakeAsync(() => {
    const kante = {
      ...defaultKante,
      fuehrungsformAttributGruppe: {
        id: 1,
        version: 1,
        fuehrungsformAttributeLinks: [
          { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
          { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
        ] as FuehrungsformAttribute[],
        fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
      },
    };
    setupKanten([kante]);

    kantenSelektionService.select(kante.id, true, KantenSeite.LINKS, undefined);
    tick();

    const clearRechtsSpy = spyOn(component.lineareReferenzenRechtsFormArray, 'clear');
    const clearLinksSpy = spyOn(component.lineareReferenzenLinksFormArray, 'clear');
    const clearEinseitigSpy = spyOn(component.lineareReferenzenFormArray, 'clear');

    kantenSelektionService.deselect(kante.id, KantenSeite.LINKS, 0);
    tick();

    expect(clearEinseitigSpy).not.toHaveBeenCalled();
    expect(clearLinksSpy).not.toHaveBeenCalled();
    expect(clearRechtsSpy).not.toHaveBeenCalled();
  }));

  describe('onSelectLinearesSegment', () => {
    let kante: Kante;
    beforeEach(() => {
      kante = {
        ...defaultKante,
        zweiseitig: true,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [defaultFuehrungsformAttribute],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);
    });

    it('should select segment without discard guard', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, KantenSeite.LINKS);
      tick();
      resetCalls(discardGuardService);

      component.onSelectLinearesSegment({ additiv: true, index: 0 }, kante.id, KantenSeite.RECHTS);
      tick();

      verify(discardGuardService.canDeactivate(anything())).never();
      expect(kantenSelektionService.isSelektiert(kante.id, KantenSeite.RECHTS)).toBeTrue();
    }));

    it('should select both segments when no seitenbezug', fakeAsync(() => {
      kante = {
        ...defaultKante,
        zweiseitig: false,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
          fuehrungsformAttributeRechts: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
        },
      };
      setupKanten([kante]);

      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();

      component.onDeselectLinearesSegment(0, kante.id);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([1]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([1]);

      component.onSelectLinearesSegment({ additiv: true, index: 0 }, kante.id);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS).sort()).toEqual([0, 1]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([0, 1]);
    }));
  });

  describe('onDeselectLinearesSegment', () => {
    let kante: Kante;
    beforeEach(() => {
      kante = {
        ...defaultKante,
        zweiseitig: true,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [defaultFuehrungsformAttribute],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);
    });

    it('should remove kante from selection if it is last selected element', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, KantenSeite.LINKS);
      tick();
      resetCalls(discardGuardService);

      component.onDeselectLinearesSegment(0, kante.id, KantenSeite.LINKS);
      tick();

      verify(discardGuardService.canDeactivate(anything())).once();
      expect(kantenSelektionService.isSelektiert(kante.id)).toBeFalse();
    }));

    it('should remove kante from selection if einseitig', fakeAsync(() => {
      kante.zweiseitig = false;

      kantenSelektionService.select(kante.id, true);
      tick();
      resetCalls(discardGuardService);

      component.onDeselectLinearesSegment(0, kante.id);
      tick();

      verify(discardGuardService.canDeactivate(anything())).once();
      expect(kantenSelektionService.isSelektiert(kante.id)).toBeFalse();
    }));

    it('should not remove kante from selection if other seite also selected', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true);
      tick();
      resetCalls(discardGuardService);

      component.onDeselectLinearesSegment(0, kante.id, KantenSeite.LINKS);
      tick();

      verify(discardGuardService.canDeactivate(anything())).never();
      expect(kantenSelektionService.isSelektiert(kante.id)).toBeTrue();
    }));
  });

  describe('onReset', () => {
    it('should reset selected Segment', fakeAsync(() => {
      const kante = {
        ...defaultKante,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ] as FuehrungsformAttribute[],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);

      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0, 1]);

      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.76 },
        { von: 0.76, bis: 1 },
      ]);

      component.onReset();
      tick();

      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0, 1]);
      expect(component.lineareReferenzenLinksFormArray.value).toEqual([
        [
          { von: 0, bis: 0.5 },
          { von: 0.5, bis: 1 },
        ],
      ]);
    }));

    it('should not work on reference from selektionService', fakeAsync(() => {
      const kante1 = {
        ...defaultKante,
        id: 1,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
              belagArt: BelagArt.ASPHALT,
            },
          ],
          fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
        },
      };
      const kante2 = {
        ...defaultKante,
        id: 2,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
              belagArt: BelagArt.NATURSTEINPFLASTER,
            },
          ],
          fuehrungsformAttributeRechts: [{ ...defaultFuehrungsformAttribute, belagArt: BelagArt.NATURSTEINPFLASTER }],
        },
      };

      setupKanten([kante1, kante2]);
      kantenSelektionService.select(kante1.id, true, KantenSeite.LINKS, undefined);
      tick();
      kantenSelektionService.select(kante2.id, true, KantenSeite.LINKS, undefined);
      tick();

      component.displayedAttributeformGroup.patchValue({
        belagArt: BelagArt.BETON,
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
      });

      component.onReset();
      tick();

      expect(component.displayedAttributeformGroup.value.belagArt).toBeInstanceOf(UndeterminedValue);
      expect(component.displayedAttributeformGroup.value.bordstein).toEqual(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER);
    }));

    it('it should disable controls if editing is not allowed', fakeAsync(() => {
      component.editingAllowed = false;
      setupKanten([defaultKante]);
      tick();

      component.onReset();
      tick();

      expect(component.displayedAttributeformGroup.disabled).toBeTrue();
      expect(component.lineareReferenzenFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenLinksFormArray.disabled).toBeTrue();
      expect(component.lineareReferenzenRechtsFormArray.disabled).toBeTrue();
    }));
  });

  describe('onInsertSegment', () => {
    let kante: Kante;
    beforeEach(() => {
      kante = {
        ...defaultKante,
        zweiseitig: true,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);
    });

    it('should insert and select new Segment with old values', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      component.onInsertAtIndex(0, 1, KantenSeite.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 0.75 },
        { von: 0.75, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices().sort()).toEqual([0, 1, 2]);

      component.onSave();
      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: kante.id,
          gruppenID: kante.fuehrungsformAttributGruppe.id,
          gruppenVersion: kante.fuehrungsformAttributGruppe.id,
          fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.75, bis: 1 } },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));

    it('should change Selection to first element after reset', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      component.onInsertAtIndex(0, 1, KantenSeite.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 0.75 },
        { von: 0.75, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices().sort()).toEqual([0, 1, 2]);

      component.onReset();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices().sort()).toEqual([0, 1]);
    }));

    it('should work einseitig', fakeAsync(() => {
      kante = {
        ...defaultKante,
        zweiseitig: false,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
          fuehrungsformAttributeRechts: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
          ],
        },
      };
      setupKanten([kante]);

      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      component.onInsertAtIndex(0, 1);
      component.lineareReferenzenFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 0.75 },
        { von: 0.75, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([
        0, 1, 2,
      ]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS).sort()).toEqual([
        0, 1, 2,
      ]);

      component.onSave();
      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: kante.id,
          gruppenID: kante.fuehrungsformAttributGruppe.id,
          gruppenVersion: kante.fuehrungsformAttributGruppe.id,
          fuehrungsformAttributeRechts: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.75, bis: 1 } },
          ],
          fuehrungsformAttributeLinks: [
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 } },
            { ...defaultFuehrungsformAttribute, linearReferenzierterAbschnitt: { von: 0.75, bis: 1 } },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));
  });

  describe('onDeleteSegment', () => {
    let kante: Kante;
    beforeEach(() => {
      kante = {
        ...defaultKante,
        zweiseitig: true,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KEINE_ABSENKUNG,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KOMPLETT_ABGESENKT,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 },
            },
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KEINE_ABSENKUNG,
              linearReferenzierterAbschnitt: { von: 0.75, bis: 1 },
            },
          ],
          fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
        },
      };
      setupKanten([kante]);
    });

    it('should delete segment and update undetermined state', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, KantenSeite.LINKS, undefined);
      tick();
      expect(component.displayedAttributeformGroup.value.bordstein).toBeInstanceOf(UndeterminedValue);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([
        0, 1, 2,
      ]);

      component.onDeleteAtIndex(0, 1, KantenSeite.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();

      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([0, 1]);
      expect(component.displayedAttributeformGroup.value.bordstein).toEqual(Bordstein.KEINE_ABSENKUNG);

      component.onSave();
      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: kante.id,
          gruppenID: kante.fuehrungsformAttributGruppe.id,
          gruppenVersion: kante.fuehrungsformAttributGruppe.id,
          fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KEINE_ABSENKUNG,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              bordstein: Bordstein.KEINE_ABSENKUNG,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));

    it('should change selection to first element if the only selected element is deleted', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, KantenSeite.LINKS, undefined);
      tick();
      kantenSelektionService.select(kante.id, false, KantenSeite.LINKS, 1);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([1]);

      component.onDeleteAtIndex(0, 1, KantenSeite.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();
      tick();

      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([0]);
    }));

    it('should change Selection to first element after reset', fakeAsync(() => {
      kantenSelektionService.select(kante.id, true, KantenSeite.LINKS, undefined);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([
        0, 1, 2,
      ]);

      component.onDeleteAtIndex(0, 1, KantenSeite.LINKS);
      component.lineareReferenzenLinksFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.lineareReferenzenRechtsFormArray.markAsDirty();
      tick();

      component.onReset();
      tick();

      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([0, 1]);
    }));

    it('should work einseitig', fakeAsync(() => {
      kante = {
        ...defaultKante,
        zweiseitig: false,
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.75, bis: 1 },
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 0.75 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.75, bis: 1 },
            },
          ],
        },
      };
      setupKanten([kante]);

      kantenSelektionService.select(kante.id, true, undefined, undefined);
      tick();
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([
        0, 1, 2,
      ]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS).sort()).toEqual([
        0, 1, 2,
      ]);

      component.onDeleteAtIndex(0, 1);
      component.lineareReferenzenFormArray.controls[0].setValue([
        { von: 0, bis: 0.5 },
        { von: 0.5, bis: 1 },
      ]);
      component.lineareReferenzenFormArray.markAsDirty();
      tick();

      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS).sort()).toEqual([0, 1]);
      expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS).sort()).toEqual([0, 1]);

      component.onSave();
      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
        {
          kanteId: kante.id,
          gruppenID: kante.fuehrungsformAttributGruppe.id,
          gruppenVersion: kante.fuehrungsformAttributGruppe.id,
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            },
          ],
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0, bis: 0.5 },
            },
            {
              ...defaultFuehrungsformAttribute,
              linearReferenzierterAbschnitt: { von: 0.5, bis: 1 },
            },
          ],
        } as SaveFuehrungsformAttributGruppeCommand,
      ]);
    }));
  });

  const setupKanten = (kanten: Kante[]): void => {
    kanten.forEach(kante => {
      when(netzService.getKanteForEdit(kante.id)).thenResolve(kante);
    });
  };
});
