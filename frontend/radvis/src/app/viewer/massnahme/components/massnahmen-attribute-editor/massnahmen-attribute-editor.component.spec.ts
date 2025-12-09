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
import { ActivatedRoute } from '@angular/router';
import { MockBuilder } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { AutoCompleteOption } from 'src/app/form-elements/components/autocomplete-dropdown/autocomplete-dropdown.component';
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { defaultNetzbezug } from 'src/app/shared/models/netzbezug-test-data-provider.spec';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { MassnahmenAttributeEditorComponent } from 'src/app/viewer/massnahme/components/massnahmen-attribute-editor/massnahmen-attribute-editor.component';
import { MassnahmenToolComponent } from 'src/app/viewer/massnahme/components/massnahmen-tool/massnahmen-tool.component';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { Massnahme } from 'src/app/viewer/massnahme/models/massnahme';
import { defaultMassnahme } from 'src/app/viewer/massnahme/models/massnahmen-test-data-provider.spec';
import { Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';
import { Realisierungshilfe } from 'src/app/viewer/massnahme/models/realisierungshilfe';
import { SaveMassnahmeCommand } from 'src/app/viewer/massnahme/models/save-massnahme-command';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { ZurueckstellungsGrund } from 'src/app/viewer/massnahme/models/zurueckstellung-grund';
import { MassnahmeNetzbezugDisplayService } from 'src/app/viewer/massnahme/services/massnahme-netzbezug-display.service';
import { MassnahmeUpdatedService } from 'src/app/viewer/massnahme/services/massnahme-updated.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(MassnahmenAttributeEditorComponent.name, () => {
  let component: MassnahmenAttributeEditorComponent;
  let fixture: ComponentFixture<MassnahmenAttributeEditorComponent>;

  let dataSubject: Subject<{ massnahme: Massnahme }>;

  let organisationenService: OrganisationenService;
  let massnahmeService: MassnahmeService;
  let massnahmenRoutingService: MassnahmenRoutingService;
  let notifyUserService: NotifyUserService;
  let activatedRoute: ActivatedRoute;
  let massnahmeUpdatedService: MassnahmeUpdatedService;
  let massnahmeNetzbezugDisplayService: MassnahmeNetzbezugDisplayService;

  let benutzerDetailsService: BenutzerDetailsService;

  beforeEach(() => {
    dataSubject = new Subject();

    organisationenService = mock(OrganisationenService);
    massnahmeService = mock(MassnahmeService);
    massnahmenRoutingService = mock(MassnahmenRoutingService);
    notifyUserService = mock(NotifyUserService);
    activatedRoute = mock(ActivatedRoute);
    massnahmeUpdatedService = mock(MassnahmenToolComponent);
    massnahmeNetzbezugDisplayService = mock(MassnahmenToolComponent);
    benutzerDetailsService = mock(BenutzerDetailsService);

    when(activatedRoute.data).thenReturn(dataSubject.asObservable());
    when(organisationenService.getAlleOrganisationen()).thenReturn(of([defaultOrganisation]));

    when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(true);

    return MockBuilder(MassnahmenAttributeEditorComponent, ViewerModule)
      .provide({
        provide: OrganisationenService,
        useValue: instance(organisationenService),
      })
      .provide({
        provide: MassnahmeService,
        useValue: instance(massnahmeService),
      })
      .provide({
        provide: MassnahmenRoutingService,
        useValue: instance(massnahmenRoutingService),
      })
      .provide({
        provide: NotifyUserService,
        useValue: instance(notifyUserService),
      })
      .provide({
        provide: ActivatedRoute,
        useValue: instance(activatedRoute),
      })
      .provide({
        provide: MassnahmeUpdatedService,
        useValue: instance(massnahmeUpdatedService),
      })
      .provide({
        provide: BenutzerDetailsService,
        useValue: instance(benutzerDetailsService),
      })
      .provide({
        provide: MassnahmeNetzbezugDisplayService,
        useValue: instance(massnahmeNetzbezugDisplayService),
      });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MassnahmenAttributeEditorComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should set netzbezugVisible correct', () => {
    verify(massnahmeNetzbezugDisplayService.showNetzbezug(anything())).never();

    dataSubject.next({ massnahme: { ...defaultMassnahme, archiviert: true } });
    verify(massnahmeNetzbezugDisplayService.showNetzbezug(anything())).once();
    expect(capture(massnahmeNetzbezugDisplayService.showNetzbezug).last()[0]).toBeTrue();

    dataSubject.next({ massnahme: { ...defaultMassnahme, archiviert: false } });
    verify(massnahmeNetzbezugDisplayService.showNetzbezug(anything())).twice();
    expect(capture(massnahmeNetzbezugDisplayService.showNetzbezug).last()[0]).toBeFalse();
  });

  it('should enable begruendung if zurueckstellungsgrund=WEITERE_GRUENDE', () => {
    component.formGroup.controls.umsetzungsstatus.setValue(Umsetzungsstatus.ZURUECKGESTELLT);

    component.formGroup.controls.begruendungZurueckstellung.reset();
    component.formGroup.controls.begruendungZurueckstellung.disable();

    component.formGroup.controls.zurueckstellungsGrund.setValue(ZurueckstellungsGrund.WEITERE_GRUENDE);

    expect(component.formGroup.controls.begruendungZurueckstellung.enabled).toBeTrue();
    expect(component.formGroup.controls.begruendungZurueckstellung.valid).toBeFalse();
    expect(component.formGroup.value.begruendungZurueckstellung).toBeNull();
  });

  it('should disabled/reset begruendung if zurueckstellungsgrund!=WEITERE_GRUENDE', () => {
    component.formGroup.controls.umsetzungsstatus.setValue(Umsetzungsstatus.ZURUECKGESTELLT);

    component.formGroup.controls.begruendungZurueckstellung.setValue('Test');
    component.formGroup.controls.begruendungZurueckstellung.enable();

    component.formGroup.controls.zurueckstellungsGrund.setValue(ZurueckstellungsGrund.PERSONELLE_ZEITLICHE_RESSOURCEN);

    expect(component.formGroup.controls.begruendungZurueckstellung.enabled).toBeFalse();
    expect(component.formGroup.value.begruendungZurueckstellung).toBeFalsy();
  });

  describe('massnahme kategorien', () => {
    [
      Konzeptionsquelle.KOMMUNALES_KONZEPT,
      Konzeptionsquelle.KREISKONZEPT,
      Konzeptionsquelle.RADNETZ_MASSNAHME,
      Konzeptionsquelle.SONSTIGE,
    ].forEach(quelle => {
      it(`should show all options for massnahme with konzeptionsquelle=${quelle}`, () => {
        dataSubject.next({ massnahme: { ...defaultMassnahme, konzeptionsquelle: quelle } });
        expect(component.massnahmeKategorienOptions).toEqual(Massnahmenkategorien.ALL);
      });
    });

    it('should show filtered options for massnahme with konzeptionsquelle=RADNETZ_MASSNAHME_2024', () => {
      dataSubject.next({
        massnahme: { ...defaultMassnahme, konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME_2024 },
      });
      expect(component.massnahmeKategorienOptions).toEqual(Massnahmenkategorien.RADNETZ_2024_KATEGORIEN_ONLY);
    });

    describe('onKategorieChanged', () => {
      it('should update options', () => {
        component.massnahmeKategorienOptions = Massnahmenkategorien.RADNETZ_2024_KATEGORIEN_ONLY;
        component.formGroup.controls.konzeptionsquelle.setValue(Konzeptionsquelle.RADNETZ_MASSNAHME);
        expect(component.massnahmeKategorienOptions).toEqual(Massnahmenkategorien.ALL);
      });

      it('should validate kategorien', () => {
        component.formGroup.patchValue({
          konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME,
          massnahmenkategorien: [Massnahmenkategorien.kategorienNotAllowedForRadnetz2024[0]],
        });

        expect(component.formGroup.controls.massnahmenkategorien.valid).toBeTrue();

        component.formGroup.controls.konzeptionsquelle.setValue(Konzeptionsquelle.RADNETZ_MASSNAHME_2024);

        expect(component.formGroup.controls.massnahmenkategorien.valid).toBeFalse();
      });
    });
  });

  describe('with massnahme', () => {
    let massnahme: Massnahme;

    beforeEach(() => {
      massnahme = {
        ...defaultMassnahme,
        id: 2,
        umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
        zurueckstellungsGrund: ZurueckstellungsGrund.PERSONELLE_ZEITLICHE_RESSOURCEN,
      };
      dataSubject.next({ massnahme });
    });

    Umsetzungsstatus.options
      .map(opt => opt.name)
      .forEach((status: string) => {
        it('should set disabled umsetzungsstatus korrekt for status: ' + status, () => {
          when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);

          dataSubject.next({
            massnahme: {
              ...massnahme,
              canEdit: true,
              umsetzungsstatus: status as Umsetzungsstatus,
              konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME,
            } as Massnahme,
          });

          expect(component.formGroup.controls.umsetzungsstatus.disabled).toBe(
            Umsetzungsstatus.isStorniert(status as Umsetzungsstatus)
          );
          expect(component.formGroup.controls.umsetzungsstatus.value).toBe(status);
        });
      });

    it('should update after unArchivieren', fakeAsync(() => {
      const archivierteMassnahme = {
        ...massnahme,
        canEdit: false,
        archiviert: true,
      };
      when(massnahmeService.unarchivieren(anything())).thenResolve(archivierteMassnahme);

      component.onUnarchivieren();
      tick();

      verify(massnahmeService.unarchivieren(anything())).once();
      expect(capture(massnahmeService.unarchivieren).last()[0]).toEqual(massnahme.id);
      expect(component.formGroup.disabled).toBe(true);
      verify(massnahmeUpdatedService.updateMassnahme()).once();
      expect(component.currentMassnahme).toEqual(archivierteMassnahme);
    }));

    it('should fill form with massnahme correct', fakeAsync(() => {
      expect(component.currentMassnahme).toEqual(massnahme);

      const eineTestOrga: AutoCompleteOption = {
        id: 5,
        name: 'Orgablaaaa',
        displayText: 'Orgablaaaa (Gemeinde, inaktiv)',
      };

      expect(component.formGroup.getRawValue()).toEqual({
        bezeichnung: 'Bezeichnung',
        massnahmenkategorien: ['STRECKE_FUER_KFZVERKEHR_SPERREN'],
        netzbezug: {
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              kanteId: 7,
              geometrie: {
                type: 'LineString',
                coordinates: [
                  [4, 4],
                  [5, 5],
                ],
              },
            },
          ],
          knotenBezug: [],
          punktuellerKantenBezug: defaultNetzbezug.punktuellerKantenBezug,
        } as Netzbezug,
        umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
        veroeffentlicht: false,
        planungErforderlich: true,
        durchfuehrungszeitraum: 2042,
        baulastZustaendiger: eineTestOrga,
        prioritaet: 5,
        kostenannahme: 500,
        netzklassen: {
          radnetzAlltag: true,
          radnetzFreizeit: false,
          radnetzZielnetz: false,
          kreisnetzAlltag: false,
          kreisnetzFreizeit: false,
          kommunalnetzAlltag: false,
          kommunalnetzFreizeit: false,
          radschnellverbindung: false,
          radvorrangrouten: false,
        },
        zustaendiger: eineTestOrga,
        unterhaltsZustaendiger: eineTestOrga,
        letzteAenderung: '01.02.22 00:00',
        benutzerLetzteAenderung: 'Olaf Müller',
        maViSID: 'eineMaViSID',
        verbaID: 'eineVerbaID',
        lgvfgid: 'eineLgvfgid',
        massnahmeKonzeptID: 'konzeptId',
        sollStandard: SollStandard.BASISSTANDARD,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'konzeptionsQuelle',
        realisierungshilfe: {
          name: Realisierungshilfe.NR_2_2_1,
          displayText: '2.2-1 Sichtfelder an Knotenpunkten und Querungsstellen',
        },
        zurueckstellungsGrund: ZurueckstellungsGrund.PERSONELLE_ZEITLICHE_RESSOURCEN,
        begruendungStornierungsanfrage: null,
        begruendungZurueckstellung: null,
      });
    }));

    it('should read command from form correct', fakeAsync(() => {
      when(massnahmeService.saveMassnahme(anything())).thenReturn(Promise.resolve(massnahme));

      component.formGroup.markAsDirty();
      component.onSave();

      verify(massnahmeService.saveMassnahme(anything())).once();
      const command = capture(massnahmeService.saveMassnahme).last()[0];
      const expected: SaveMassnahmeCommand = {
        id: massnahme.id,
        version: massnahme.version,
        bezeichnung: massnahme.bezeichnung,
        massnahmenkategorien: massnahme.massnahmenkategorien,
        netzbezug: massnahme.netzbezug,
        umsetzungsstatus: massnahme.umsetzungsstatus,
        veroeffentlicht: massnahme.veroeffentlicht,
        planungErforderlich: massnahme.planungErforderlich,
        durchfuehrungszeitraum: massnahme.durchfuehrungszeitraum,
        baulastZustaendigerId: massnahme.baulastZustaendiger?.id ?? null,
        zurueckstellungsGrund: massnahme.zurueckstellungsGrund,
        prioritaet: massnahme.prioritaet,
        kostenannahme: massnahme.kostenannahme,
        netzklassen: massnahme.netzklassen,
        zustaendigerId: massnahme.zustaendiger?.id,
        unterhaltsZustaendigerId: massnahme.unterhaltsZustaendiger?.id ?? null,
        maViSID: massnahme.maViSID,
        verbaID: massnahme.verbaID,
        lgvfgid: massnahme.lgvfgid,
        massnahmeKonzeptID: massnahme.massnahmeKonzeptID,
        sollStandard: SollStandard.BASISSTANDARD,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'konzeptionsQuelle',
        realisierungshilfe: Realisierungshilfe.NR_2_2_1,
        begruendungStornierungsanfrage: null,
        begruendungZurueckstellung: null,
      };
      expect(command).toEqual(expected);
    }));

    it('should update umsetzungsstand for status changed to STORNIERT', fakeAsync(() => {
      const neueMassnahme: Massnahme = { ...massnahme, konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME };
      neueMassnahme.umsetzungsstatus = Umsetzungsstatus.STORNIERUNG_ANGEFRAGT;
      when(massnahmeService.saveMassnahme(anything())).thenReturn(Promise.resolve(neueMassnahme));

      component.formGroup.markAsDirty();
      component.onSave();

      tick();

      verify(massnahmenRoutingService.toUmsetzungstandEditor(anything())).once();
      expect().nothing();
    }));

    Konzeptionsquelle.values
      .filter(k => Konzeptionsquelle.isRadNetzMassnahme(k))
      .forEach(k => {
        it(`should update umsetzungsstand for status changed to UMGESETZT (${k})`, fakeAsync(() => {
          massnahme.umsetzungsstatus = Umsetzungsstatus.STORNIERT;
          massnahme.konzeptionsquelle = k;

          const neueMassnahme: Massnahme = { ...massnahme };
          neueMassnahme.umsetzungsstatus = Umsetzungsstatus.UMGESETZT;
          when(massnahmeService.saveMassnahme(anything())).thenReturn(Promise.resolve(neueMassnahme));

          component.formGroup.markAsDirty();
          component.onSave();

          tick();

          verify(massnahmenRoutingService.toUmsetzungstandEditor(anything())).once();
          expect().nothing();
        }));
      });

    it('should not update umsetzungsstand when status not changed', fakeAsync(() => {
      massnahme.umsetzungsstatus = Umsetzungsstatus.STORNIERT;

      const neueMassnahme: Massnahme = { ...massnahme };
      neueMassnahme.umsetzungsstatus = Umsetzungsstatus.STORNIERT;
      when(massnahmeService.saveMassnahme(anything())).thenReturn(Promise.resolve(neueMassnahme));

      component.formGroup.markAsDirty();
      component.onSave();

      tick();

      verify(massnahmenRoutingService.toUmsetzungstandEditor(anything())).never();
      expect().nothing();
    }));

    describe('enable/disable form', () => {
      it(`it should toggle zurueckstellungsGrund correct for Umsetzungsstatus`, fakeAsync(() => {
        expect(component.formGroup.disabled).toBeFalse();

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: false,
            umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
            zurueckstellungsGrund: ZurueckstellungsGrund.WEITERE_PLANUNGEN_IM_ZUSAMMENHANG,
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.disabled).toBeTrue();
        expect(component.formGroup.controls.zurueckstellungsGrund?.disabled).toBeTrue();
        expect(component.zurueckstellungsGrundVisible).toBeTrue();

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: true,
            umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
            zurueckstellungsGrund: ZurueckstellungsGrund.WEITERE_PLANUNGEN_IM_ZUSAMMENHANG,
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.enabled).toBeTrue();
        expect(component.formGroup.controls.zurueckstellungsGrund?.enabled).toBeTrue();
        expect(component.zurueckstellungsGrundVisible).toBeTrue();

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: true,
            umsetzungsstatus: Umsetzungsstatus.PLANUNG,
            zurueckstellungsGrund: null,
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.disabled).toBeFalse();
        expect(component.formGroup.controls.zurueckstellungsGrund?.disabled).toBeTrue();
        expect(component.formGroup.controls.zurueckstellungsGrund?.value).toBeFalsy();
        expect(component.zurueckstellungsGrundVisible).toBeFalse();
      }));

      it(`it should toggle begruendungStornierungsanfrage correct for Umsetzungsstatus`, fakeAsync(() => {
        expect(component.formGroup.disabled).toBeFalse();

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: false,
            umsetzungsstatus: Umsetzungsstatus.STORNIERUNG_ANGEFRAGT,
            begruendungStornierungsanfrage: 'Test',
            konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.disabled).toBeTrue();
        expect(component.formGroup.controls.begruendungStornierungsanfrage?.disabled).toBeTrue();
        expect(component.begruendungStornierungVisible).toBeTrue();

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: true,
            umsetzungsstatus: Umsetzungsstatus.STORNIERUNG_ANGEFRAGT,
            begruendungStornierungsanfrage: 'Test',
            konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.enabled).toBeTrue();
        expect(component.formGroup.controls.begruendungStornierungsanfrage?.enabled).toBeTrue();
        expect(component.formGroup.getRawValue().umsetzungsstatus).toBe(Umsetzungsstatus.STORNIERUNG_ANGEFRAGT);
        expect(component.begruendungStornierungVisible).toBeTrue();

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: true,
            umsetzungsstatus: Umsetzungsstatus.PLANUNG,
            begruendungStornierungsanfrage: null,
            konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.disabled).toBeFalse();
        expect(component.formGroup.controls.begruendungStornierungsanfrage?.disabled).toBeTrue();
        expect(component.begruendungStornierungVisible).toBeFalse();
      }));

      it(`it should toggle begruendungZurueckstellung correct for Zurueckstellungsgrund`, fakeAsync(() => {
        expect(component.formGroup.disabled).toBeFalse();

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: false,
            umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
            zurueckstellungsGrund: ZurueckstellungsGrund.WEITERE_GRUENDE,
            begruendungZurueckstellung: 'Test',
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.disabled).toBeTrue();
        expect(component.formGroup.controls.begruendungZurueckstellung?.disabled).toBeTrue();
        expect(component.begruendungZurueckstellungVisible).toBeTrue();
        expect(component.formGroup.controls.begruendungZurueckstellung?.value).toBe('Test');

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: true,
            umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
            zurueckstellungsGrund: ZurueckstellungsGrund.WEITERE_GRUENDE,
            begruendungZurueckstellung: 'Test',
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.enabled).toBeTrue();
        expect(component.formGroup.controls.begruendungZurueckstellung?.enabled).toBeTrue();
        expect(component.begruendungZurueckstellungVisible).toBeTrue();
        expect(component.formGroup.controls.begruendungZurueckstellung?.value).toBe('Test');

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: true,
            umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
            zurueckstellungsGrund: ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN,
            begruendungZurueckstellung: null,
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.disabled).toBeFalse();
        expect(component.formGroup.controls.begruendungZurueckstellung?.disabled).toBeTrue();
        expect(component.begruendungZurueckstellungVisible).toBeFalse();
        expect(component.formGroup.controls.begruendungZurueckstellung?.value).toBeNull();
      }));

      Konzeptionsquelle.values
        .filter(k => Konzeptionsquelle.isRadNetzMassnahme(k))
        .forEach(k => {
          it(`it should toggle with Konzeptionsquelle ${k}`, fakeAsync(() => {
            expect(component.formGroup.disabled).toBeFalse();

            dataSubject.next({
              massnahme: {
                ...massnahme,
                canEdit: false,
                konzeptionsquelle: k,
              } as Massnahme,
            });

            tick();

            expect(component.formGroup.disabled).toBeTrue();
            expect(component.isRadNETZMassnahme).toBeTrue();
            expect(component.formGroup.get('konzeptionsquelle')?.disabled).toBeTrue();

            dataSubject.next({
              massnahme: {
                ...massnahme,
                canEdit: true,
                konzeptionsquelle: k,
              } as Massnahme,
            });

            tick();

            expect(component.formGroup.disabled).toBeFalse();
            expect(component.isRadNETZMassnahme).toBeTrue();
            expect(component.formGroup.get('konzeptionsquelle')?.disabled).toBeTrue();
          }));
        });

      it('it should toggle with Konzeptionsquelle keine RadNETZ-Maßnahme', fakeAsync(() => {
        expect(component.formGroup.disabled).toBeFalse();

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: false,
            konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.disabled).toBeTrue();
        expect(component.isRadNETZMassnahme).toBeFalse();
        expect(component.formGroup.get('konzeptionsquelle')?.disabled).toBeTrue();

        dataSubject.next({
          massnahme: {
            ...massnahme,
            canEdit: true,
            konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
          } as Massnahme,
        });

        tick();

        expect(component.formGroup.disabled).toBeFalse();
        expect(component.isRadNETZMassnahme).toBeFalse();
        expect(component.formGroup.get('konzeptionsquelle')?.disabled).toBeFalse();
      }));

      it('should disable Konzeptionsquelle for RadNETZ-Maßnahmen', fakeAsync(() => {
        expect(component.isRadNETZMassnahme).toBeFalse();
        expect(component.formGroup.get('konzeptionsquelle')?.disabled).toBeFalse();

        dataSubject.next({ massnahme: { ...massnahme, konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME } });
        tick();

        expect(component.isRadNETZMassnahme).toBeTrue();
        expect(component.formGroup.get('konzeptionsquelle')?.disabled).toBeTrue();

        dataSubject.next({ massnahme });
        tick();

        expect(component.isRadNETZMassnahme).toBeFalse();
        expect(component.formGroup.get('konzeptionsquelle')?.disabled).toBeFalse();

        dataSubject.next({ massnahme: { ...massnahme, konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME_2024 } });
        tick();

        expect(component.isRadNETZMassnahme).toBeTrue();
        expect(component.formGroup.get('konzeptionsquelle')?.disabled).toBeTrue();
      }));
    });

    describe('onUmsetzungsstatusChanged', () => {
      it('should enable zurueckstellungsgrund if ZURUECKGESTELLT', () => {
        component.formGroup.controls.zurueckstellungsGrund.reset();
        component.formGroup.controls.zurueckstellungsGrund.disable();

        component.formGroup.controls.umsetzungsstatus.setValue(Umsetzungsstatus.ZURUECKGESTELLT);

        expect(component.formGroup.controls.zurueckstellungsGrund.enabled).toBeTrue();
        expect(component.formGroup.value.zurueckstellungsGrund).toBeNull();
      });

      it('should disabled/reset zurueckstellungsgrund if not ZURUECKGESTELLT', () => {
        component.formGroup.controls.zurueckstellungsGrund.setValue(ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN);
        component.formGroup.controls.zurueckstellungsGrund.enable();

        component.formGroup.controls.umsetzungsstatus.setValue(Umsetzungsstatus.PLANUNG);

        expect(component.formGroup.controls.zurueckstellungsGrund.enabled).toBeFalse();
        expect(component.formGroup.getRawValue().zurueckstellungsGrund).toBeFalsy();
      });

      it('should enable begruendung if STORNIERUNG_ANGEFRAGT', () => {
        component.formGroup.controls.begruendungStornierungsanfrage.reset();
        component.formGroup.controls.begruendungStornierungsanfrage.disable();

        component.formGroup.controls.umsetzungsstatus.setValue(Umsetzungsstatus.STORNIERUNG_ANGEFRAGT);

        expect(component.formGroup.controls.begruendungStornierungsanfrage.enabled).toBeTrue();
        expect(component.formGroup.value.begruendungStornierungsanfrage).toBeNull();
      });

      it('should disabled/reset begruendung if not STORNIERUNG_ANGEFRAGT', () => {
        component.formGroup.controls.begruendungStornierungsanfrage.setValue('Test');
        component.formGroup.controls.begruendungStornierungsanfrage.enable();

        component.formGroup.controls.umsetzungsstatus.setValue(Umsetzungsstatus.PLANUNG);

        expect(component.formGroup.controls.begruendungStornierungsanfrage.enabled).toBeFalse();
        expect(component.formGroup.getRawValue().begruendungStornierungsanfrage).toBeFalsy();
      });
    });

    it('should not save with invalid entries', fakeAsync(() => {
      when(massnahmeService.saveMassnahme(anything())).thenReturn(Promise.resolve(massnahme));

      // ungueltige bezeichnung
      component.formGroup.patchValue({
        bezeichnung: '',
      });

      component.formGroup.markAsDirty();
      component.onSave();

      verify(massnahmeService.saveMassnahme(anything())).never();

      // Massnahmen zuruecksetzten
      component.onReset();

      component.onSave();

      verify(massnahmeService.saveMassnahme(anything())).never();

      // ungueltiger durchfuehrungszeitraum
      component.formGroup.patchValue({
        umsetzungsstatus: Umsetzungsstatus.PLANUNG,
        durchfuehrungszeitraum: 20,
      });

      component.formGroup.markAsDirty();
      component.onSave();

      verify(massnahmeService.saveMassnahme(anything())).never();
      expect().nothing();
    }));
  });

  describe('erlaubte umsetzungsstatus', () => {
    const setAllUmsetzungsstatusDisabled = (disabled: boolean): void => {
      component.umsetzungsstatusOptions.map(opt => ({ ...opt, disabled }));
    };

    it('should disable Storniert-Status if radNETZ-Maßnahme and nutzer has no recht', () => {
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      setAllUmsetzungsstatusDisabled(false);
      dataSubject.next({ massnahme: { ...defaultMassnahme, konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME } });

      [Umsetzungsstatus.STORNIERT_ENGSTELLE, Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH].forEach(status => {
        expect(component.umsetzungsstatusOptions.find(opt => opt.name === status)?.disabled).toBeTrue();
      });
    });

    it('should reset nicht erlaubte umsetzungsstatus on konzeptionsquelle changed', () => {
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      dataSubject.next({
        massnahme: {
          ...defaultMassnahme,
          konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
          umsetzungsstatus: Umsetzungsstatus.STORNIERT_ENGSTELLE,
        },
      });

      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME });

      expect(component.formGroup.controls.umsetzungsstatus.value).toBeFalsy();
      expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeFalse();
      expect(component.formGroup.controls.umsetzungsstatus.valid).toBeFalse();
    });

    it('should not reset erlaubte umsetzungsstatus on konzeptionsquelle changed', () => {
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      dataSubject.next({
        massnahme: {
          ...defaultMassnahme,
          konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
          umsetzungsstatus: Umsetzungsstatus.IDEE,
        },
      });

      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME });

      expect(component.formGroup.controls.umsetzungsstatus.value).toBe(Umsetzungsstatus.IDEE);
    });

    it('should update on konzeptionsquelle changed', () => {
      setAllUmsetzungsstatusDisabled(true);
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      dataSubject.next({ massnahme: { ...defaultMassnahme, konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME } });

      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT });

      [Umsetzungsstatus.STORNIERT_ENGSTELLE, Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH].forEach(status => {
        expect(component.umsetzungsstatusOptions.find(opt => opt.name === status)?.disabled).toBeFalse();
      });
    });

    it('should enable Storniert-Status if no radNETZ-Maßnahme', () => {
      setAllUmsetzungsstatusDisabled(true);
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      dataSubject.next({ massnahme: { ...defaultMassnahme, konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT } });

      [Umsetzungsstatus.STORNIERT_ENGSTELLE, Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH].forEach(status => {
        expect(component.umsetzungsstatusOptions.find(opt => opt.name === status)?.disabled).toBeFalse();
      });
    });

    it('should enable Storniert-Status if nutzer has recht', () => {
      setAllUmsetzungsstatusDisabled(true);
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(true);
      dataSubject.next({ massnahme: { ...defaultMassnahme, konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME } });

      [Umsetzungsstatus.STORNIERT_ENGSTELLE, Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH].forEach(status => {
        expect(component.umsetzungsstatusOptions.find(opt => opt.name === status)?.disabled).toBeFalse();
      });
    });

    it('should disable/reset Stornierung angefragt if no radNETZ-Maßnahme', () => {
      setAllUmsetzungsstatusDisabled(false);
      component.formGroup.patchValue({ umsetzungsstatus: Umsetzungsstatus.STORNIERUNG_ANGEFRAGT });

      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT });

      expect(
        component.umsetzungsstatusOptions.find(opt => opt.name === Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)?.disabled
      ).toBe(true);
      expect(component.formGroup.controls.umsetzungsstatus.value).toBeFalsy();
      expect(component.formGroup.controls.umsetzungsstatus.valid).toBeFalse();
      expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeFalse();
    });

    it('should enable Stornierung angefragt if radNETZ-Maßnahme', () => {
      setAllUmsetzungsstatusDisabled(true);
      component.umsetzungsstatusOptions = [];
      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME });

      expect(
        component.umsetzungsstatusOptions.find(opt => opt.name === Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)?.disabled
      ).toBe(false);
    });
  });

  describe('disable umsetzungsstatus', () => {
    [Umsetzungsstatus.STORNIERT_ENGSTELLE, Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH].forEach(status => {
      it(`should be disabled if ${status} and radNETZ-Maßnahme and no Recht`, () => {
        when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
        dataSubject.next({
          massnahme: {
            ...defaultMassnahme,
            canEdit: true,
            konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
            umsetzungsstatus: status,
          },
        });
        expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeTrue();
      });

      it(`should be enabled if ${status} on change konzeptionsquelle with no Recht`, () => {
        when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
        dataSubject.next({
          massnahme: {
            ...defaultMassnahme,
            canEdit: true,
            konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
            umsetzungsstatus: status,
          },
        });
        // Fall ist nur theoretisch relevant, da RadNETZ-Konzeptionsquellen disabled sind
        component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT });
        expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeFalse();
      });

      it(`should be enabled if ${status} and radNETZ-Maßnahme and has Recht`, () => {
        when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(true);
        dataSubject.next({
          massnahme: {
            ...defaultMassnahme,
            canEdit: true,
            konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
            umsetzungsstatus: status,
          },
        });
        expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeFalse();
      });

      it(`should be enabled if ${status} and not radNETZ-Maßnahme and no Recht`, () => {
        when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
        dataSubject.next({
          massnahme: {
            ...defaultMassnahme,
            canEdit: true,
            konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
            umsetzungsstatus: status,
          },
        });
        expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeFalse();
      });

      it(`should be cleared if ${status} on change Konzeptionsquelle with no Recht`, () => {
        when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
        dataSubject.next({
          massnahme: {
            ...defaultMassnahme,
            canEdit: true,
            konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
            umsetzungsstatus: status,
          },
        });
        component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME });
        expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeFalse();
        expect(component.formGroup.controls.umsetzungsstatus.valid).toBeFalse();
        expect(component.formGroup.controls.umsetzungsstatus.value).toBeFalsy();
      });
    });

    it('should be disabled if storniert and has Recht and not canEdit', () => {
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(true);
      dataSubject.next({
        massnahme: { ...defaultMassnahme, canEdit: false, umsetzungsstatus: Umsetzungsstatus.STORNIERT_ENGSTELLE },
      });
      expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeTrue();
    });

    it('should be enabled if not storniert and no Recht', () => {
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      dataSubject.next({ massnahme: { ...defaultMassnahme, canEdit: true, umsetzungsstatus: Umsetzungsstatus.IDEE } });
      expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeFalse();
    });
  });
});
