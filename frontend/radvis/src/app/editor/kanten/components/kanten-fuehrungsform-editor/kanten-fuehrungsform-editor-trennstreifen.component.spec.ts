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
import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { of } from 'rxjs';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { KantenFuehrungsformEditorComponent } from 'src/app/editor/kanten/components/kanten-fuehrungsform-editor/kanten-fuehrungsform-editor.component';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { Kante } from 'src/app/editor/kanten/models/kante';
import {
  defaultFuehrungsformAttribute,
  defaultKante,
} from 'src/app/editor/kanten/models/kante-test-data-provider.spec';
import { Richtung } from 'src/app/editor/kanten/models/richtung';
import { SaveFuehrungsformAttributGruppeCommand } from 'src/app/editor/kanten/models/save-fuehrungsform-attribut-gruppe-command';
import { TrennstreifenForm } from 'src/app/editor/kanten/models/trennstreifen-form';
import { TrennstreifenTrennungZu } from 'src/app/editor/kanten/models/trennstreifen-trennung-zu';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NetzBearbeitungModusService } from 'src/app/editor/kanten/services/netz-bearbeitung-modus.service';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';
import { TrennstreifenSeite } from 'src/app/shared/models/trennstreifen-seite';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardGuardService } from 'src/app/shared/services/discard-guard.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(KantenFuehrungsformEditorComponent.name + ' - Trennstreifen', () => {
  let component: KantenFuehrungsformEditorComponent;
  let fixture: MockedComponentFixture<KantenFuehrungsformEditorComponent>;
  let netzService: NetzService;
  let kantenSelektionService: KantenSelektionService;
  let benutzerDetails: BenutzerDetailsService;
  let olMapService: OlMapService;
  let discardGuardService: DiscardGuardService;

  beforeEach(() => {
    const netzbearbeitungsModusService = mock(NetzBearbeitungModusService);
    when(netzbearbeitungsModusService.getAktiveKantenGruppe()).thenReturn(of(AttributGruppe.FUEHRUNGSFORM));

    netzService = mock(NetzService);
    benutzerDetails = mock(BenutzerDetailsService);
    olMapService = mock(OlMapComponent);

    discardGuardService = mock(DiscardGuardService);
    when(discardGuardService.canDeactivate(anything())).thenReturn(of(true));

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
        provide: DiscardGuardService,
        useValue: instance(discardGuardService),
      })
      .provide({
        provide: NetzBearbeitungModusService,
        useValue: instance(netzbearbeitungsModusService),
      })
      .keep(KantenSelektionService);
  });

  beforeEach(() => {
    fixture = MockRender(KantenFuehrungsformEditorComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
    kantenSelektionService = component['kantenSelektionService'];
  });

  it('should start empty', () => {
    expect(component.trennstreifenEinseitig).toBeUndefined();
    expect(component.trennstreifenRichtungRechts).toBeUndefined();
    expect(component.trennstreifenRichtungLinks).toBeUndefined();
    expect(component.trennstreifenSeiteSelected).toBeUndefined();
    expect(component.trennstreifenBearbeiteteSeiten).toHaveSize(0);
  });

  describe('einseitige Kante selektiert', () => {
    let kante: Kante;

    beforeEach(fakeAsync(() => {
      when(netzService.saveKanteFuehrungsform(anything())).thenResolve();

      kante = {
        ...defaultKante,
        zweiseitig: false,
        fahrtrichtungAttributGruppe: {
          id: 1,
          version: 1,
          fahrtrichtungLinks: Richtung.IN_RICHTUNG,
          fahrtrichtungRechts: Richtung.IN_RICHTUNG,
        },
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
            },
          ],
        },
      };
      setupKanten([kante]);
      kantenSelektionService.select(kante.id, false);
      tick();
    }));

    it('should set relevant fields', () => {
      expect(component.trennstreifenEinseitig).toBeTrue();
      expect(component.trennstreifenRichtungRechts).toBeUndefined();
      expect(component.trennstreifenRichtungLinks).toEqual(Richtung.IN_RICHTUNG);
      expect(component.trennstreifenSeiteSelected).toBeUndefined();
      expect(component.trennstreifenBearbeiteteSeiten).toHaveSize(0);
    });

    describe('onTrennstreifenSeiteSelectionChanged', () => {
      it('should not reset trennungZu Options (RAD-7383)', () => {
        component.trennstreifenTrennungZuOptions = [];
        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);
        expect(component.trennstreifenTrennungZuOptions).toEqual([]);
      });

      it('Should give TrennstreifenSeite Options links zur Auswahl', () => {
        expect(component.trennstreifenSeiteOptions).toEqual(TrennstreifenSeite.optionsLinks);
      });

      it('should fill Trennstreifen Details form: Select A', () => {
        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);
        assertTrennstreifenFormGroup(
          TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
          TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
          12,
          TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
          TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
          23
        );
      });

      it('should fill Trennstreifen Details form: Select B', () => {
        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.B);
        assertTrennstreifenFormGroup(
          TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
          TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
          12,
          TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
          TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
          23
        );
      });

      it('should create correct save command', () => {
        component.lineareReferenzenLinksFormArray.markAsDirty();
        component.lineareReferenzenRechtsFormArray.markAsDirty();
        component.onSave();

        verify(netzService.saveKanteFuehrungsform(anything())).once();
        expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
          {
            kanteId: 1,
            gruppenID: 1,
            gruppenVersion: 1,
            fuehrungsformAttributeLinks: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
            fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          } as SaveFuehrungsformAttributGruppeCommand,
        ]);
      });
    });

    describe('trennstreifen form value changed', () => {
      describe('without selected trennstreifen', () => {
        beforeEach(() => {
          component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(
            TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
          );
        });

        it('should use previous values, not form values', () => {
          spyOnProperty(component.trennstreifenFormGroupLinks, 'valid').and.returnValue(true);
          component.lineareReferenzenLinksFormArray.markAsDirty();
          component.lineareReferenzenRechtsFormArray.markAsDirty();
          component.onSave();

          verify(netzService.saveKanteFuehrungsform(anything())).once();
          expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
            {
              gruppenID: 1,
              gruppenVersion: 1,
              kanteId: 1,
              fuehrungsformAttributeLinks: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
              fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
            } as SaveFuehrungsformAttributGruppeCommand,
          ]);
          expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(0);
        });
      });

      describe('applies simple changes from form to attribute', () => {
        beforeEach(() => {
          component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
            Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND
          );

          component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);

          component.trennstreifenFormGroupLinks.markAsDirty();
          component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(
            TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
          );
          component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.setValue(
            TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN
          );
          component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.setValue(123);
        });

        it('should update set of changed trennstreifen seiten', () => {
          expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(1);
          expect(component.trennstreifenBearbeiteteSeiten).toContain(TrennstreifenSeite.A);
        });

        it('should keep enable state of controls', () => {
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeTrue();
        });

        it('should create save command with new values', () => {
          component.lineareReferenzenLinksFormArray.markAsDirty();
          component.lineareReferenzenRechtsFormArray.markAsDirty();
          component.onSave();

          verify(netzService.saveKanteFuehrungsform(anything())).once();
          expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
            {
              kanteId: 1,
              gruppenID: 1,
              gruppenVersion: 1,
              fuehrungsformAttributeLinks: [
                {
                  ...kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                  trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
                  trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
                  radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
                  trennstreifenBreiteLinks: 123,
                },
              ],
              fuehrungsformAttributeRechts: [
                {
                  ...kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                  trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
                  trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
                  radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
                  trennstreifenBreiteLinks: 123,
                },
              ],
            } as SaveFuehrungsformAttributGruppeCommand,
          ]);
        });
      });

      [TrennstreifenForm.UNBEKANNT, TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN].forEach(
        trennstreifenForm => {
          describe('trennstreifenForm=' + trennstreifenForm + ' clears and disables other controls', () => {
            beforeEach(() => {
              component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);

              component.trennstreifenFormGroupLinks.markAsDirty();
              component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(trennstreifenForm);
            });

            it('should update set of changed trennstreifen seiten', () => {
              expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(1);
              expect(component.trennstreifenBearbeiteteSeiten).toContain(TrennstreifenSeite.A);
            });

            it('should set enable state of controls', () => {
              expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
              expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeFalse();
              expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeFalse();
            });

            it('should create save command with new values', () => {
              component.lineareReferenzenLinksFormArray.markAsDirty();
              component.lineareReferenzenRechtsFormArray.markAsDirty();
              component.onSave();

              verify(netzService.saveKanteFuehrungsform(anything())).once();
              expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
                {
                  kanteId: 1,
                  gruppenID: 1,
                  gruppenVersion: 1,
                  fuehrungsformAttributeLinks: [
                    {
                      ...kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                      trennstreifenFormLinks: trennstreifenForm,
                      trennstreifenTrennungZuLinks: null,
                      trennstreifenBreiteLinks: null,
                    },
                  ],
                  fuehrungsformAttributeRechts: [
                    {
                      ...kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                      trennstreifenFormLinks: trennstreifenForm,
                      trennstreifenTrennungZuLinks: null,
                      trennstreifenBreiteLinks: null,
                    },
                  ],
                } as SaveFuehrungsformAttributGruppeCommand,
              ]);
            });

            describe('re-select trennstreifenForm re-activates form controls', () => {
              beforeEach(() => {
                component.trennstreifenFormGroupLinks.markAsDirty();
                component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(
                  TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
                );
              });

              it('should update set of changed trennstreifen seiten', () => {
                expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(1);
                expect(component.trennstreifenBearbeiteteSeiten).toContain(TrennstreifenSeite.A);
              });

              it('should re-enable controls', () => {
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeTrue();
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeTrue();
              });

              it('should have null-values in re-enabled controls', () => {
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.value).toEqual(
                  TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
                );
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.value).toBeNull();
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.value).toBeNull();
              });
            });
          });
        }
      );
    });
  });

  describe('zweiseitige Kante selektiert', () => {
    let kante: Kante;

    beforeEach(fakeAsync(() => {
      when(netzService.saveKanteFuehrungsform(anything())).thenResolve();

      kante = {
        ...defaultKante,
        zweiseitig: true,
        fahrtrichtungAttributGruppe: {
          id: 1,
          version: 1,
          fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
          fahrtrichtungRechts: Richtung.IN_RICHTUNG,
        },
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenBreiteLinks: 34,
              trennstreifenBreiteRechts: 45,
            },
          ],
        },
      };
      setupKanten([kante]);
      kantenSelektionService.select(kante.id, false);
      tick();
    }));

    it('should set relevant fields', () => {
      expect(component.trennstreifenEinseitig).toBeFalse();
      expect(component.trennstreifenRichtungRechts).toEqual(Richtung.IN_RICHTUNG);
      expect(component.trennstreifenRichtungLinks).toEqual(Richtung.GEGEN_RICHTUNG);
      expect(component.trennstreifenSeiteSelected).toBeUndefined();
      expect(component.trennstreifenBearbeiteteSeiten).toHaveSize(0);
    });

    describe('onTrennstreifenSeiteSelectionChanged', () => {
      it('Should give TrennstreifenSeite Options links und rechts zur Auswahl', () => {
        expect(component.trennstreifenSeiteOptions).toEqual([
          ...TrennstreifenSeite.optionsLinks,
          ...TrennstreifenSeite.optionsRechts,
        ]);
      });

      it('should fill Trennstreifen Details form: FührungsformAttrLinks', () => {
        const formLinks = TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN;
        const trennungLinks = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN;
        const breiteLinks = 12;
        const formRechts = TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART;
        const trennungRechts = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN;
        const breiteRechts = 23;

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.B);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);
      });

      it('should fill Trennstreifen Details form: FührungsformAttrRechts', () => {
        const formLinks = TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM;
        const trennungLinks = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR;
        const breiteLinks = 34;
        const formRechts = TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN;
        const trennungRechts = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN;
        const breiteRechts = 45;

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.C);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.D);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);
      });

      it('should create correct save command', () => {
        component.lineareReferenzenLinksFormArray.markAsDirty();
        component.lineareReferenzenRechtsFormArray.markAsDirty();
        component.onSave();

        verify(netzService.saveKanteFuehrungsform(anything())).once();
        expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
          {
            kanteId: 1,
            gruppenID: 1,
            gruppenVersion: 1,
            fuehrungsformAttributeLinks: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
            fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          } as SaveFuehrungsformAttributGruppeCommand,
        ]);
      });
    });
  });

  describe('zweiseitige linear referenzierte Kante teilweise selektiert', () => {
    let kante: Kante;

    beforeEach(fakeAsync(() => {
      when(netzService.saveKanteFuehrungsform(anything())).thenResolve();

      kante = {
        ...defaultKante,
        zweiseitig: true,
        fahrtrichtungAttributGruppe: {
          id: 1,
          version: 1,
          fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
          fahrtrichtungRechts: Richtung.IN_RICHTUNG,
        },
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
              linearReferenzierterAbschnitt: {
                von: 0,
                bis: 0.4,
              },
            },
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 15,
              trennstreifenBreiteRechts: 23,
              linearReferenzierterAbschnitt: {
                von: 0.4,
                bis: 1.0,
              },
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenBreiteLinks: 34,
              trennstreifenBreiteRechts: 45,
              linearReferenzierterAbschnitt: {
                von: 0,
                bis: 0.2,
              },
            },
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenBreiteLinks: 34,
              trennstreifenBreiteRechts: 9,
              linearReferenzierterAbschnitt: {
                von: 0.2,
                bis: 1.0,
              },
            },
          ],
        },
      };
      setupKanten([kante]);
      kantenSelektionService.select(kante.id, true);
      tick();
      kantenSelektionService.select(kante.id, false, KantenSeite.LINKS, 0);
      kantenSelektionService.select(kante.id, true, KantenSeite.RECHTS, 1);
      tick();
    }));

    it('should set relevant fields', () => {
      expect(component.trennstreifenEinseitig).toBeFalse();
      expect(component.trennstreifenRichtungRechts).toEqual(Richtung.IN_RICHTUNG);
      expect(component.trennstreifenRichtungLinks).toEqual(Richtung.GEGEN_RICHTUNG);
      expect(component.trennstreifenSeiteSelected).toBeUndefined();
      expect(component.trennstreifenBearbeiteteSeiten).toHaveSize(0);
    });

    describe('onTrennstreifenSeiteSelectionChanged', () => {
      it('Should give TrennstreifenSeite Options links und rechts zur Auswahl', () => {
        expect(component.trennstreifenSeiteOptions).toEqual([
          ...TrennstreifenSeite.optionsLinks,
          ...TrennstreifenSeite.optionsRechts,
        ]);
      });

      it('should fill Trennstreifen Details form: FührungsformAttrLinks', () => {
        const formLinks = TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN;
        const trennungLinks = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN;
        const breiteLinks = 12;
        const formRechts = TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART;
        const trennungRechts = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN;
        const breiteRechts = 23;

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);
        expect(kantenSelektionService.isSelektiert(kante.id, KantenSeite.RECHTS)).toBeFalse();
        expect(kantenSelektionService.isSelektiert(kante.id, KantenSeite.LINKS)).toBeTrue();
        expect(kantenSelektionService.selektion.length).toEqual(1);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0]);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([]);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.B);
        expect(kantenSelektionService.isSelektiert(kante.id, KantenSeite.RECHTS)).toBeFalse();
        expect(kantenSelektionService.isSelektiert(kante.id, KantenSeite.LINKS)).toBeTrue();
        expect(kantenSelektionService.selektion.length).toEqual(1);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0]);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([]);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);
      });

      it('should fill Trennstreifen Details form: FührungsformAttrRechts', () => {
        const formLinks = TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM;
        const trennungLinks = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR;
        const breiteLinks = 34;
        const formRechts = TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART;
        const trennungRechts = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR;
        const breiteRechts = 9;

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.C);
        expect(kantenSelektionService.isSelektiert(kante.id, KantenSeite.RECHTS)).toBeTrue();
        expect(kantenSelektionService.isSelektiert(kante.id, KantenSeite.LINKS)).toBeFalse();
        expect(kantenSelektionService.selektion.length).toEqual(1);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([]);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([1]);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.D);
        expect(kantenSelektionService.isSelektiert(kante.id, KantenSeite.RECHTS)).toBeTrue();
        expect(kantenSelektionService.isSelektiert(kante.id, KantenSeite.LINKS)).toBeFalse();
        expect(kantenSelektionService.selektion.length).toEqual(1);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([]);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([1]);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);
      });

      it('should create correct save command', () => {
        component.lineareReferenzenLinksFormArray.markAsDirty();
        component.lineareReferenzenRechtsFormArray.markAsDirty();
        component.onSave();

        verify(netzService.saveKanteFuehrungsform(anything())).once();
        expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
          {
            kanteId: 1,
            gruppenID: 1,
            gruppenVersion: 1,
            fuehrungsformAttributeLinks: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
            fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          } as SaveFuehrungsformAttributGruppeCommand,
        ]);
      });
    });

    describe('trennstreifen form value changed', () => {
      describe('without selected trennstreifen', () => {
        beforeEach(() => {
          component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(
            TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
          );
        });

        it('should use previous values, not form values', () => {
          spyOnProperty(component.trennstreifenFormGroupLinks, 'valid').and.returnValue(true);
          component.lineareReferenzenLinksFormArray.markAsDirty();
          component.lineareReferenzenRechtsFormArray.markAsDirty();
          component.onSave();

          verify(netzService.saveKanteFuehrungsform(anything())).once();
          expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
            {
              gruppenID: 1,
              gruppenVersion: 1,
              kanteId: 1,
              fuehrungsformAttributeLinks: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
              fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
            } as SaveFuehrungsformAttributGruppeCommand,
          ]);
          expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(0);
        });
      });

      describe('applies simple changes from form to attribute', () => {
        beforeEach(() => {
          component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);
          component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
            Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND
          );

          component.trennstreifenFormGroupLinks.markAsDirty();
          component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(
            TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
          );
          component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.setValue(
            TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN
          );
          component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.setValue(123);
        });

        it('should update set of changed trennstreifen seiten', () => {
          expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(1);
          expect(component.trennstreifenBearbeiteteSeiten).toContain(TrennstreifenSeite.A);
        });

        it('should keep enable state of controls', () => {
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeTrue();
        });

        it('should create save command with new values', () => {
          component.lineareReferenzenLinksFormArray.markAsDirty();
          component.lineareReferenzenRechtsFormArray.markAsDirty();
          component.onSave();

          verify(netzService.saveKanteFuehrungsform(anything())).once();
          expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
            {
              kanteId: 1,
              gruppenID: 1,
              gruppenVersion: 1,
              fuehrungsformAttributeLinks: [
                {
                  ...kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                  radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
                  trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
                  trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
                  trennstreifenBreiteLinks: 123,
                },
                kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[1],
              ],
              fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
            } as SaveFuehrungsformAttributGruppeCommand,
          ]);
        });
      });

      [TrennstreifenForm.UNBEKANNT, TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN].forEach(
        trennstreifenForm => {
          describe('trennstreifenForm=' + trennstreifenForm + ' clears and disables other controls', () => {
            beforeEach(() => {
              component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);

              component.trennstreifenFormGroupLinks.markAsDirty();
              component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(trennstreifenForm);
            });

            it('should update set of changed trennstreifen seiten', () => {
              expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(1);
              expect(component.trennstreifenBearbeiteteSeiten).toContain(TrennstreifenSeite.A);
            });

            it('should set enable state of controls', () => {
              expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
              expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeFalse();
              expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeFalse();
            });

            it('should create save command with new values', () => {
              component.lineareReferenzenLinksFormArray.markAsDirty();
              component.lineareReferenzenRechtsFormArray.markAsDirty();
              component.onSave();

              verify(netzService.saveKanteFuehrungsform(anything())).once();
              expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
                {
                  kanteId: 1,
                  gruppenID: 1,
                  gruppenVersion: 1,
                  fuehrungsformAttributeLinks: [
                    {
                      ...kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                      trennstreifenFormLinks: trennstreifenForm,
                      trennstreifenTrennungZuLinks: null,
                      trennstreifenBreiteLinks: null,
                    },
                    kante.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[1],
                  ],
                  fuehrungsformAttributeRechts: kante.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
                } as SaveFuehrungsformAttributGruppeCommand,
              ]);
            });

            describe('re-select trennstreifenForm re-activates form controls', () => {
              beforeEach(() => {
                component.trennstreifenFormGroupLinks.markAsDirty();
                component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(
                  TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
                );
              });

              it('should update set of changed trennstreifen seiten', () => {
                expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(1);
                expect(component.trennstreifenBearbeiteteSeiten).toContain(TrennstreifenSeite.A);
              });

              it('should re-enable controls', () => {
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeTrue();
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeTrue();
              });

              it('should have null-values in re-enabled controls', () => {
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.value).toEqual(
                  TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
                );
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.value).toBeNull();
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.value).toBeNull();
              });
            });
          });
        }
      );
    });
  });

  describe('zweiseitige linear referenzierte Kante + einseitige selektiert', () => {
    let kanteLineareReferenzen: Kante;
    let kanteEinseitig: Kante;

    beforeEach(fakeAsync(() => {
      when(netzService.saveKanteFuehrungsform(anything())).thenResolve();

      kanteLineareReferenzen = {
        ...defaultKante,
        zweiseitig: true,
        fahrtrichtungAttributGruppe: {
          id: 1,
          version: 1,
          fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
          fahrtrichtungRechts: Richtung.IN_RICHTUNG,
        },
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
              linearReferenzierterAbschnitt: {
                von: 0,
                bis: 0.4,
              },
            },
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 15,
              trennstreifenBreiteRechts: 23,
              linearReferenzierterAbschnitt: {
                von: 0.4,
                bis: 1.0,
              },
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenBreiteLinks: 34,
              trennstreifenBreiteRechts: 45,
              linearReferenzierterAbschnitt: {
                von: 0,
                bis: 0.2,
              },
            },
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenBreiteLinks: 34,
              trennstreifenBreiteRechts: 9,
              linearReferenzierterAbschnitt: {
                von: 0.2,
                bis: 1.0,
              },
            },
          ],
        },
      };
      kanteEinseitig = {
        ...defaultKante,
        id: 2,
        zweiseitig: false,
        fahrtrichtungAttributGruppe: {
          id: 2,
          version: 1,
          fahrtrichtungLinks: Richtung.BEIDE_RICHTUNGEN,
          fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
        },
        fuehrungsformAttributGruppe: {
          id: 2,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
            },
          ],
        },
      };
      setupKanten([kanteLineareReferenzen, kanteEinseitig]);
      kantenSelektionService.select(kanteEinseitig.id, false);
      tick();
      kantenSelektionService.select(kanteLineareReferenzen.id, true, KantenSeite.LINKS);
      tick();
      kantenSelektionService.deselect(kanteLineareReferenzen.id, KantenSeite.LINKS, 0);
      tick();
    }));

    it('should set relevant fields', () => {
      expect(component.trennstreifenEinseitig).toBeFalse();
      expect(component.trennstreifenRichtungRechts).toEqual(Richtung.IN_RICHTUNG);
      expect(component.trennstreifenRichtungLinks).toBeUndefined();
      expect(component.trennstreifenSeiteSelected).toBeUndefined();
      expect(component.trennstreifenBearbeiteteSeiten).toHaveSize(0);
    });

    describe('onTrennstreifenSeiteSelectionChanged', () => {
      it('Should give TrennstreifenSeite Options links und rechts zur Auswahl', () => {
        expect(component.trennstreifenSeiteOptions).toEqual([
          ...TrennstreifenSeite.optionsLinks,
          ...TrennstreifenSeite.optionsRechts,
        ]);
      });

      it('should fill Trennstreifen Details form: FührungsformAttrLinks', () => {
        const formLinks = TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN;
        const trennungLinks = new UndeterminedValue();
        const breiteLinks = new UndeterminedValue();
        const formRechts = new UndeterminedValue();
        const trennungRechts = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN;
        const breiteRechts = 23;

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);
        expect(kantenSelektionService.isSelektiert(kanteLineareReferenzen.id, KantenSeite.RECHTS)).toBeFalse();
        expect(kantenSelektionService.isSelektiert(kanteLineareReferenzen.id, KantenSeite.LINKS)).toBeTrue();
        expect(kantenSelektionService.isSelektiert(kanteEinseitig.id, KantenSeite.RECHTS)).toBeTrue();
        expect(kantenSelektionService.isSelektiert(kanteEinseitig.id, KantenSeite.LINKS)).toBeTrue();
        expect(kantenSelektionService.selektion.length).toEqual(2);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0]);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0]);
        expect(kantenSelektionService.selektion[1].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([1]);
        expect(kantenSelektionService.selektion[1].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([]);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.B);
        expect(kantenSelektionService.isSelektiert(kanteLineareReferenzen.id, KantenSeite.RECHTS)).toBeFalse();
        expect(kantenSelektionService.isSelektiert(kanteLineareReferenzen.id, KantenSeite.LINKS)).toBeTrue();
        expect(kantenSelektionService.isSelektiert(kanteEinseitig.id, KantenSeite.RECHTS)).toBeTrue();
        expect(kantenSelektionService.isSelektiert(kanteEinseitig.id, KantenSeite.LINKS)).toBeTrue();
        expect(kantenSelektionService.selektion.length).toEqual(2);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([0]);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0]);
        expect(kantenSelektionService.selektion[1].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([1]);
        expect(kantenSelektionService.selektion[1].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([]);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);
      });

      it('should select entire kantenseite after switch from A to C', fakeAsync(() => {
        const formLinks = TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM;
        const trennungLinks = TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR;
        const breiteLinks = 34;
        const formRechts = new UndeterminedValue();
        const trennungRechts = new UndeterminedValue();
        const breiteRechts = new UndeterminedValue();

        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);
        tick();
        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.C);
        tick();

        expect(kantenSelektionService.isSelektiert(kanteLineareReferenzen.id, KantenSeite.RECHTS)).toBeTrue();
        expect(kantenSelektionService.isSelektiert(kanteLineareReferenzen.id, KantenSeite.LINKS)).toBeFalse();
        expect(kantenSelektionService.isSelektiert(kanteEinseitig.id, KantenSeite.RECHTS)).toBeFalse();
        expect(kantenSelektionService.isSelektiert(kanteEinseitig.id, KantenSeite.LINKS)).toBeFalse();
        expect(kantenSelektionService.selektion.length).toEqual(1);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.LINKS)).toEqual([]);
        expect(kantenSelektionService.selektion[0].getSelectedSegmentIndices(KantenSeite.RECHTS)).toEqual([0, 1]);
        assertTrennstreifenFormGroup(formLinks, trennungLinks, breiteLinks, formRechts, trennungRechts, breiteRechts);
      }));

      it('should create correct save command', () => {
        component.lineareReferenzenLinksFormArray.markAsDirty();
        component.lineareReferenzenRechtsFormArray.markAsDirty();
        component.onSave();

        verify(netzService.saveKanteFuehrungsform(anything())).once();
        expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
          {
            kanteId: 2,
            gruppenID: 2,
            gruppenVersion: 1,
            fuehrungsformAttributeLinks: kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
            fuehrungsformAttributeRechts: kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          } as SaveFuehrungsformAttributGruppeCommand,
          {
            kanteId: 1,
            gruppenID: 1,
            gruppenVersion: 1,
            fuehrungsformAttributeLinks: kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
            fuehrungsformAttributeRechts:
              kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          } as SaveFuehrungsformAttributGruppeCommand,
        ]);
      });

      it('should create correct save command after switch from A to C', fakeAsync(() => {
        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);
        tick();
        component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.C);
        tick();

        component.lineareReferenzenLinksFormArray.markAsDirty();
        component.lineareReferenzenRechtsFormArray.markAsDirty();
        component.onSave();

        verify(netzService.saveKanteFuehrungsform(anything())).once();
        expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
          {
            kanteId: 1,
            gruppenID: 1,
            gruppenVersion: 1,
            fuehrungsformAttributeLinks: kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
            fuehrungsformAttributeRechts:
              kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
          } as SaveFuehrungsformAttributGruppeCommand,
        ]);
      }));
    });

    describe('trennstreifen form value changed', () => {
      describe('without selected trennstreifen', () => {
        beforeEach(() => {
          component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(
            TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
          );
        });

        it('should use previous values, not form values', () => {
          spyOnProperty(component.trennstreifenFormGroupLinks, 'valid').and.returnValue(true);
          component.lineareReferenzenLinksFormArray.markAsDirty();
          component.lineareReferenzenRechtsFormArray.markAsDirty();
          component.onSave();

          verify(netzService.saveKanteFuehrungsform(anything())).once();
          expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
            {
              gruppenID: 2,
              gruppenVersion: 1,
              kanteId: 2,
              fuehrungsformAttributeLinks: kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
              fuehrungsformAttributeRechts: kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
            } as SaveFuehrungsformAttributGruppeCommand,
            {
              gruppenID: 1,
              gruppenVersion: 1,
              kanteId: 1,
              fuehrungsformAttributeLinks:
                kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks,
              fuehrungsformAttributeRechts:
                kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
            } as SaveFuehrungsformAttributGruppeCommand,
          ]);
          expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(0);
        });
      });

      describe('applies simple changes from form to attribute', () => {
        beforeEach(() => {
          component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
            Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND
          );
          component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);

          component.trennstreifenFormGroupLinks.markAsDirty();
          component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(
            TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
          );
          component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.setValue(
            TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN
          );
          component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.setValue(123);
        });

        it('should update set of changed trennstreifen seiten', () => {
          expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(1);
          expect(component.trennstreifenBearbeiteteSeiten).toContain(TrennstreifenSeite.A);
        });

        it('should keep enable state of controls', () => {
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeTrue();
        });

        it('should create save command with new values', () => {
          component.lineareReferenzenLinksFormArray.markAsDirty();
          component.lineareReferenzenRechtsFormArray.markAsDirty();
          component.onSave();

          verify(netzService.saveKanteFuehrungsform(anything())).once();
          expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
            {
              kanteId: 2,
              gruppenID: 2,
              gruppenVersion: 1,
              fuehrungsformAttributeLinks: [
                {
                  ...kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                  radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
                  trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
                  trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
                  trennstreifenBreiteLinks: 123,
                },
              ],
              fuehrungsformAttributeRechts: [
                {
                  ...kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts[0],
                  trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
                  radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
                  trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
                  trennstreifenBreiteLinks: 123,
                },
              ],
            } as SaveFuehrungsformAttributGruppeCommand,
            {
              kanteId: 1,
              gruppenID: 1,
              gruppenVersion: 1,
              fuehrungsformAttributeLinks: [
                kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                {
                  ...kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[1],
                  trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
                  radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
                  trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
                  trennstreifenBreiteLinks: 123,
                },
              ],
              fuehrungsformAttributeRechts:
                kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
            } as SaveFuehrungsformAttributGruppeCommand,
          ]);
        });
      });

      [TrennstreifenForm.UNBEKANNT, TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN].forEach(
        trennstreifenForm => {
          describe('trennstreifenForm=' + trennstreifenForm + ' clears and disables other controls', () => {
            beforeEach(fakeAsync(() => {
              component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);
              tick();

              component.trennstreifenFormGroupLinks.markAsDirty();
              component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(trennstreifenForm);
            }));

            it('should update set of changed trennstreifen seiten', () => {
              expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(1);
              expect(component.trennstreifenBearbeiteteSeiten).toContain(TrennstreifenSeite.A);
            });

            it('should set enable state of controls', () => {
              expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
              expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeFalse();
              expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeFalse();
            });

            it('should create save command with new values', () => {
              component.lineareReferenzenLinksFormArray.markAsDirty();
              component.lineareReferenzenRechtsFormArray.markAsDirty();
              component.onSave();

              verify(netzService.saveKanteFuehrungsform(anything())).once();
              expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
                {
                  kanteId: 2,
                  gruppenID: 2,
                  gruppenVersion: 1,
                  fuehrungsformAttributeLinks: [
                    {
                      ...kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                      trennstreifenFormLinks: trennstreifenForm,
                      trennstreifenTrennungZuLinks: null,
                      trennstreifenBreiteLinks: null,
                    },
                  ],
                  fuehrungsformAttributeRechts: [
                    {
                      ...kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts[0],
                      trennstreifenFormLinks: trennstreifenForm,
                      trennstreifenTrennungZuLinks: null,
                      trennstreifenBreiteLinks: null,
                    },
                  ],
                } as SaveFuehrungsformAttributGruppeCommand,
                {
                  kanteId: 1,
                  gruppenID: 1,
                  gruppenVersion: 1,
                  fuehrungsformAttributeLinks: [
                    kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                    {
                      ...kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[1],
                      trennstreifenFormLinks: trennstreifenForm,
                      trennstreifenTrennungZuLinks: null,
                      trennstreifenBreiteLinks: null,
                    },
                  ],
                  fuehrungsformAttributeRechts:
                    kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
                } as SaveFuehrungsformAttributGruppeCommand,
              ]);
            });

            describe('re-select trennstreifenForm re-activates form controls', () => {
              beforeEach(() => {
                component.trennstreifenFormGroupLinks.markAsDirty();
                component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.setValue(
                  TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
                );
              });

              it('should update set of changed trennstreifen seiten', () => {
                expect(component.trennstreifenBearbeiteteSeiten.size).toEqual(1);
                expect(component.trennstreifenBearbeiteteSeiten).toContain(TrennstreifenSeite.A);
              });

              it('should re-enable controls', () => {
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeTrue();
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeTrue();
              });

              it('should have null-values in re-enabled controls', () => {
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.value).toEqual(
                  TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN
                );
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.value).toBeNull();
                expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.value).toBeNull();
              });
            });
          });
        }
      );
    });
  });

  describe('radverkehrsfuehrung changed - multi-select relevante Radverkehrsfuehrungen', () => {
    let kanteLineareReferenzen: Kante;
    let kanteEinseitig: Kante;

    beforeEach(fakeAsync(() => {
      when(netzService.saveKanteFuehrungsform(anything())).thenResolve();

      component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);

      kanteLineareReferenzen = {
        ...defaultKante,
        zweiseitig: true,
        fahrtrichtungAttributGruppe: {
          id: 1,
          version: 1,
          fahrtrichtungLinks: Richtung.GEGEN_RICHTUNG,
          fahrtrichtungRechts: Richtung.IN_RICHTUNG,
        },
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
              linearReferenzierterAbschnitt: {
                von: 0,
                bis: 0.4,
              },
            },
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 15,
              trennstreifenBreiteRechts: 23,
              linearReferenzierterAbschnitt: {
                von: 0.4,
                bis: 1.0,
              },
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN,
              trennstreifenBreiteLinks: 34,
              trennstreifenBreiteRechts: 45,
              linearReferenzierterAbschnitt: {
                von: 0,
                bis: 0.2,
              },
            },
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenBreiteLinks: 34,
              trennstreifenBreiteRechts: 9,
              linearReferenzierterAbschnitt: {
                von: 0.2,
                bis: 1.0,
              },
            },
          ],
        },
      };
      kanteEinseitig = {
        ...defaultKante,
        id: 2,
        zweiseitig: false,
        fahrtrichtungAttributGruppe: {
          id: 2,
          version: 1,
          fahrtrichtungLinks: Richtung.BEIDE_RICHTUNGEN,
          fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
        },
        fuehrungsformAttributGruppe: {
          id: 2,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
            },
          ],
        },
      };
      setupKanten([kanteLineareReferenzen, kanteEinseitig]);
      kantenSelektionService.select(kanteEinseitig.id, false);
      tick();
      kantenSelektionService.select(kanteLineareReferenzen.id, true, KantenSeite.LINKS);
      tick();
      kantenSelektionService.deselect(kanteLineareReferenzen.id, KantenSeite.LINKS, 0);
      tick();
    }));

    [
      Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER,
    ].forEach(radverkehrsfuehrung => {
      describe('radverkehrsfuehrung=' + radverkehrsfuehrung + ' should show all trennungZu-values', () => {
        beforeEach(() => {
          component.displayedAttributeformGroup.markAsDirty();
          component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(radverkehrsfuehrung);
        });

        it('should enable trennstreifen form group', () => {
          expect(component.isTrennstreifenFormVisible()).toBeTrue();

          expect(component.trennstreifenFormGroupLinks.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenFormLinks.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupLinks.controls.trennstreifenBreiteLinks.enabled).toBeTrue();

          expect(component.trennstreifenFormGroupRechts.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupRechts.controls.trennstreifenFormRechts.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupRechts.controls.trennstreifenTrennungZuRechts.enabled).toBeTrue();
          expect(component.trennstreifenFormGroupRechts.controls.trennstreifenBreiteRechts.enabled).toBeTrue();
        });

        it('should show all form- and trennungZu-options', () => {
          expect(component.trennstreifenFormOptions).toEqual(TrennstreifenForm.options);
          expect(component.trennstreifenTrennungZuOptions.every(opt => !opt.disabled)).toBeTrue();
        });

        it('should not alter trennungZu form control', () => {
          expect(component.trennstreifenFormGroupLinks.get('trennstreifenTrennungZuLinks')?.value).toEqual(
            TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR
          );
        });
      });
    });

    describe('isTrennstreifenFormVisible', () => {
      beforeEach(() => {
        component.displayedAttributeformGroup.markAsDirty();
      });

      it('should be true if enabled for Radverkehrsfuehrung', () => {
        component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
          Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND
        );

        expect(component.isTrennstreifenFormVisible()).toBeTrue();
      });

      it('should be false if disabled for Radverkehrsfuehrung', () => {
        component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(Radverkehrsfuehrung.BEGEGNUNBSZONE);

        expect(component.isTrennstreifenFormVisible()).toBeFalse();
      });
    });

    [
      Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG,
      Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_SELBSTSTAENDIG,
      Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG,
      Radverkehrsfuehrung.GEHWEG_RAD_FREI_SELBSTSTAENDIG,
      Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG,
      Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG,
      Radverkehrsfuehrung.BETRIEBSWEG_FORST,
      Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT,
      Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG,
      Radverkehrsfuehrung.PIKTOGRAMMKETTE_BEIDSEITIG,
      Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN,
      Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN,
      Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE,
      Radverkehrsfuehrung.FUEHRUNG_IN_T20_ZONE,
      Radverkehrsfuehrung.FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH,
      Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI,
      Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI,
      Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI,
      Radverkehrsfuehrung.BEGEGNUNBSZONE,
      Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30,
      Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30,
      Radverkehrsfuehrung.EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30,
      Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG,
      Radverkehrsfuehrung.UNBEKANNT,
    ].forEach(radverkehrsfuehrung => {
      describe(
        'radverkehrsfuehrung=' + radverkehrsfuehrung + ' should reset and disable trennstreifen form-controls',
        () => {
          beforeEach(() => {
            component.displayedAttributeformGroup.markAsDirty();
            component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(radverkehrsfuehrung);
          });

          it('should hide trennstreifen form', () => {
            expect(component.isTrennstreifenFormVisible()).toBeFalse();
          });

          it('should create save command with cleared values', () => {
            component.lineareReferenzenLinksFormArray.markAsDirty();
            component.lineareReferenzenRechtsFormArray.markAsDirty();
            component.onSave();

            verify(netzService.saveKanteFuehrungsform(anything())).once();
            expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
              {
                kanteId: 2,
                gruppenID: 2,
                gruppenVersion: 1,
                fuehrungsformAttributeLinks: [
                  {
                    ...kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                    radverkehrsfuehrung,
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
                    ...kanteEinseitig.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts[0],
                    radverkehrsfuehrung,
                    trennstreifenFormLinks: null,
                    trennstreifenTrennungZuLinks: null,
                    trennstreifenBreiteLinks: null,
                    trennstreifenFormRechts: null,
                    trennstreifenTrennungZuRechts: null,
                    trennstreifenBreiteRechts: null,
                  },
                ],
              } as SaveFuehrungsformAttributGruppeCommand,
              {
                kanteId: 1,
                gruppenID: 1,
                gruppenVersion: 1,
                fuehrungsformAttributeLinks: [
                  kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                  {
                    ...kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[1],
                    radverkehrsfuehrung,
                    trennstreifenFormLinks: null,
                    trennstreifenTrennungZuLinks: null,
                    trennstreifenBreiteLinks: null,
                    trennstreifenFormRechts: null,
                    trennstreifenTrennungZuRechts: null,
                    trennstreifenBreiteRechts: null,
                  },
                ],
                fuehrungsformAttributeRechts:
                  kanteLineareReferenzen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts,
              } as SaveFuehrungsformAttributGruppeCommand,
            ]);
          });
        }
      );
    });
  });

  describe('radverkehrsfuehrung changed - multi-select relevante & nicht-relevante Radverkehrsfuehrungen', () => {
    let kanteKeinTrennstreifen: Kante;
    let kanteMitTrennstreifen: Kante;

    beforeEach(fakeAsync(() => {
      when(netzService.saveKanteFuehrungsform(anything())).thenResolve();

      component.onTrennstreifenSeiteSelectionChanged(TrennstreifenSeite.A);

      kanteKeinTrennstreifen = {
        ...defaultKante,
        zweiseitig: false,
        fahrtrichtungAttributGruppe: {
          id: 1,
          version: 1,
          fahrtrichtungLinks: Richtung.BEIDE_RICHTUNGEN,
          fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
        },
        fuehrungsformAttributGruppe: {
          id: 1,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG,
              trennstreifenFormLinks: null,
              trennstreifenFormRechts: null,
              trennstreifenTrennungZuLinks: null,
              trennstreifenTrennungZuRechts: null,
              trennstreifenBreiteLinks: null,
              trennstreifenBreiteRechts: null,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG,
              trennstreifenFormLinks: null,
              trennstreifenFormRechts: null,
              trennstreifenTrennungZuLinks: null,
              trennstreifenTrennungZuRechts: null,
              trennstreifenBreiteLinks: null,
              trennstreifenBreiteRechts: null,
            },
          ],
        },
      };
      kanteMitTrennstreifen = {
        ...defaultKante,
        id: 2,
        zweiseitig: false,
        fahrtrichtungAttributGruppe: {
          id: 2,
          version: 1,
          fahrtrichtungLinks: Richtung.BEIDE_RICHTUNGEN,
          fahrtrichtungRechts: Richtung.BEIDE_RICHTUNGEN,
        },
        fuehrungsformAttributGruppe: {
          id: 2,
          version: 1,
          fuehrungsformAttributeLinks: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
            },
          ],
          fuehrungsformAttributeRechts: [
            {
              ...defaultFuehrungsformAttribute,
              radverkehrsfuehrung: Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
              trennstreifenFormLinks: TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN,
              trennstreifenFormRechts: TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART,
              trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
              trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN,
              trennstreifenBreiteLinks: 12,
              trennstreifenBreiteRechts: 23,
            },
          ],
        },
      };
      setupKanten([kanteKeinTrennstreifen, kanteMitTrennstreifen]);

      // Reihenfolge wichtig: Form nach erster Selektion nicht sichtbar und bleibt nach zweiter auch nicht sichtbar.
      kantenSelektionService.select(kanteKeinTrennstreifen.id, false);
      tick();
      kantenSelektionService.select(kanteMitTrennstreifen.id, true);
      tick();
    }));

    it('should hide trennstreifen form', () => {
      expect(component.isTrennstreifenFormVisible()).toBeFalse();
    });

    [
      Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER,
      Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.SCHUTZSTREIFEN,
      Radverkehrsfuehrung.RADFAHRSTREIFEN,
      Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR,
      Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,
      Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG,
    ].forEach(radverkehrsfuehrung => {
      describe(
        'radverkehrsfuehrung=' + radverkehrsfuehrung + ' should enable trennstreifen form-controls and save them',
        () => {
          beforeEach(() => {
            component.displayedAttributeformGroup.markAsDirty();
            component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(radverkehrsfuehrung);
          });

          it('should show trennstreifen form', () => {
            expect(component.isTrennstreifenFormVisible()).toBeTrue();
          });

          it('should create save command with only radverkehrsfuehrung changed', () => {
            component.lineareReferenzenLinksFormArray.markAsDirty();
            component.lineareReferenzenRechtsFormArray.markAsDirty();
            component.onSave();

            verify(netzService.saveKanteFuehrungsform(anything())).once();
            expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
              {
                kanteId: 1,
                gruppenID: 1,
                gruppenVersion: 1,
                fuehrungsformAttributeLinks: [
                  {
                    ...kanteKeinTrennstreifen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                    radverkehrsfuehrung,
                  },
                ],
                fuehrungsformAttributeRechts: [
                  {
                    ...kanteKeinTrennstreifen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts[0],
                    radverkehrsfuehrung,
                  },
                ],
              } as SaveFuehrungsformAttributGruppeCommand,
              {
                kanteId: 2,
                gruppenID: 2,
                gruppenVersion: 1,
                fuehrungsformAttributeLinks: [
                  {
                    ...kanteMitTrennstreifen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                    radverkehrsfuehrung,
                  },
                ],
                fuehrungsformAttributeRechts: [
                  {
                    ...kanteMitTrennstreifen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts[0],
                    radverkehrsfuehrung,
                  },
                ],
              } as SaveFuehrungsformAttributGruppeCommand,
            ]);
          });
        }
      );
    });

    [
      Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG,
      Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_SELBSTSTAENDIG,
      Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG,
      Radverkehrsfuehrung.GEHWEG_RAD_FREI_SELBSTSTAENDIG,
      Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG,
      Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG,
      Radverkehrsfuehrung.BETRIEBSWEG_FORST,
      Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT,
      Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG,
      Radverkehrsfuehrung.PIKTOGRAMMKETTE_BEIDSEITIG,
      Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN,
      Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN,
      Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE,
      Radverkehrsfuehrung.FUEHRUNG_IN_T20_ZONE,
      Radverkehrsfuehrung.FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH,
      Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI,
      Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI,
      Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI,
      Radverkehrsfuehrung.BEGEGNUNBSZONE,
      Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30,
      Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30,
      Radverkehrsfuehrung.EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30,
      Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG,
      Radverkehrsfuehrung.UNBEKANNT,
    ].forEach(radverkehrsfuehrung => {
      describe(
        'radverkehrsfuehrung=' + radverkehrsfuehrung + ' should reset and disable trennstreifen form-controls',
        () => {
          beforeEach(() => {
            component.displayedAttributeformGroup.markAsDirty();
            component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(radverkehrsfuehrung);
          });

          it('should hide trennstreifen form', () => {
            expect(component.isTrennstreifenFormVisible()).toBeFalse();
          });

          it('should create save command with cleared values', () => {
            component.lineareReferenzenLinksFormArray.markAsDirty();
            component.lineareReferenzenRechtsFormArray.markAsDirty();
            component.onSave();

            verify(netzService.saveKanteFuehrungsform(anything())).once();
            expect(capture(netzService.saveKanteFuehrungsform).last()[0]).toEqual([
              {
                kanteId: 1,
                gruppenID: 1,
                gruppenVersion: 1,
                fuehrungsformAttributeLinks: [
                  {
                    ...kanteKeinTrennstreifen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                    radverkehrsfuehrung,
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
                    ...kanteKeinTrennstreifen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts[0],
                    radverkehrsfuehrung,
                    trennstreifenFormLinks: null,
                    trennstreifenTrennungZuLinks: null,
                    trennstreifenBreiteLinks: null,
                    trennstreifenFormRechts: null,
                    trennstreifenTrennungZuRechts: null,
                    trennstreifenBreiteRechts: null,
                  },
                ],
              } as SaveFuehrungsformAttributGruppeCommand,
              {
                kanteId: 2,
                gruppenID: 2,
                gruppenVersion: 1,
                fuehrungsformAttributeLinks: [
                  {
                    ...kanteMitTrennstreifen.fuehrungsformAttributGruppe.fuehrungsformAttributeLinks[0],
                    radverkehrsfuehrung,
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
                    ...kanteMitTrennstreifen.fuehrungsformAttributGruppe.fuehrungsformAttributeRechts[0],
                    radverkehrsfuehrung,
                    trennstreifenFormLinks: null,
                    trennstreifenTrennungZuLinks: null,
                    trennstreifenBreiteLinks: null,
                    trennstreifenFormRechts: null,
                    trennstreifenTrennungZuRechts: null,
                    trennstreifenBreiteRechts: null,
                  },
                ],
              } as SaveFuehrungsformAttributGruppeCommand,
            ]);
          });
        }
      );
    });
  });

  describe('validate trennungZu', () => {
    it('should be valid if null', () => {
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
        Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND
      );
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.setValue(null);
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.updateValueAndValidity();

      expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.valid).toBe(true);
    });

    it('should be valid if radverkehrsfuehrung allowed', () => {
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
        Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND
      );
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.setValue(
        TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN
      );
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.updateValueAndValidity();

      expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.valid).toBe(true);
    });

    it('should be valid if UndeterminedValue', () => {
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
        Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND
      );
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.setValue(new UndeterminedValue());
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.updateValueAndValidity();

      expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.valid).toBe(true);
    });

    it('should be valid if radverkehrsfuehrung is UndeterminedValue', () => {
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(new UndeterminedValue());
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.setValue(
        TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN
      );
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.updateValueAndValidity();

      expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.valid).toBe(true);
    });

    it('should be invalid if radverkehrsfuehrung not allowed', () => {
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
        Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG
      );
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.setValue(
        TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR
      );
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.updateValueAndValidity();

      expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.valid).toBe(false);
    });

    it('should be validated if Radverkehrsfuehrung change', () => {
      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
        Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND
      );
      component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.setValue(
        TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR
      );
      expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.valid).toBe(true);

      component.displayedAttributeformGroup.controls.radverkehrsfuehrung.setValue(
        Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG
      );

      expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.valid).toBe(false);
      expect(component.trennstreifenFormGroupLinks.valid).toBe(false);
      expect(component.trennstreifenFormGroupLinks.controls.trennstreifenTrennungZuLinks.value).toBe(
        TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR
      );
    });
  });

  describe('onSave', () => {
    beforeEach(() => {
      when(netzService.saveKanteFuehrungsform(anything())).thenResolve([defaultKante]);
    });

    it('should save if all Values are null and pristine for backwards compatibility', () => {
      component.trennstreifenFormGroupLinks.reset();
      component.trennstreifenFormGroupRechts.reset();
      component.displayedAttributeformGroup.updateValueAndValidity();

      component.onSave();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect().nothing();
    });

    it('should not save if trennungZu Value invalid and pristine (RAD-7382)', () => {
      component.trennstreifenFormGroupLinks.reset({
        trennstreifenTrennungZuLinks: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
      });
      component.trennstreifenFormGroupRechts.reset({
        trennstreifenTrennungZuRechts: TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR,
      });
      component.displayedAttributeformGroup.patchValue({
        radverkehrsfuehrung: Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
      });

      component.onSave();

      verify(netzService.saveKanteFuehrungsform(anything())).never();
      expect().nothing();
    });

    it('should save if all Values are null and dirty', () => {
      component.trennstreifenFormGroupLinks.reset();
      component.trennstreifenFormGroupRechts.reset();
      component.trennstreifenFormGroupRechts.updateValueAndValidity();
      component.displayedAttributeformGroup.updateValueAndValidity();

      component.onSave();

      verify(netzService.saveKanteFuehrungsform(anything())).once();
      expect().nothing();
    });
  });

  const setupKanten = (kanten: Kante[]): void => {
    kanten.forEach(kante => {
      when(netzService.getKanteForEdit(kante.id)).thenResolve(kante);
    });
  };

  const assertTrennstreifenFormGroup = (
    formLinks: TrennstreifenForm | UndeterminedValue,
    trennungLinks: TrennstreifenTrennungZu | UndeterminedValue,
    breiteLinks: number | UndeterminedValue,
    formRechts: TrennstreifenForm | UndeterminedValue,
    trennungRechts: TrennstreifenTrennungZu | UndeterminedValue,
    breiteRechts: number | UndeterminedValue
  ): void => {
    expect(component.trennstreifenFormGroupLinks.get('trennstreifenFormLinks')?.value).toEqual(formLinks);
    expect(component.trennstreifenFormGroupLinks.get('trennstreifenTrennungZuLinks')?.value).toEqual(trennungLinks);
    expect(component.trennstreifenFormGroupLinks.get('trennstreifenBreiteLinks')?.value).toEqual(breiteLinks);

    expect(component.trennstreifenFormGroupRechts.get('trennstreifenFormRechts')?.value).toEqual(formRechts);
    expect(component.trennstreifenFormGroupRechts.get('trennstreifenTrennungZuRechts')?.value).toEqual(trennungRechts);
    expect(component.trennstreifenFormGroupRechts.get('trennstreifenBreiteRechts')?.value).toEqual(breiteRechts);
  };
});
