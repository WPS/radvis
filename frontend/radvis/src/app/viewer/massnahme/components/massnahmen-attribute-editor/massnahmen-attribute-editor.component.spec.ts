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
import { ActivatedRoute, Data } from '@angular/router';
import { MockBuilder } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { AutoCompleteOption } from 'src/app/form-elements/components/autocomplete-dropdown/autocomplete-dropdown.component';
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { defaultNetzbezug } from 'src/app/shared/models/netzbezug-test-data-provider.spec';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { MassnahmenAttributeEditorComponent } from 'src/app/viewer/massnahme/components/massnahmen-attribute-editor/massnahmen-attribute-editor.component';
import { MassnahmenToolComponent } from 'src/app/viewer/massnahme/components/massnahmen-tool/massnahmen-tool.component';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { Massnahme } from 'src/app/viewer/massnahme/models/massnahme';
import { defaultMassnahme } from 'src/app/viewer/massnahme/models/massnahmen-test-data-provider.spec';
import { Realisierungshilfe } from 'src/app/viewer/massnahme/models/realisierungshilfe';
import { SaveMassnahmeCommand } from 'src/app/viewer/massnahme/models/save-massnahme-command';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { MassnahmeNetzbezugDisplayService } from 'src/app/viewer/massnahme/services/massnahme-netzbezug-display.service';
import { MassnahmeUpdatedService } from 'src/app/viewer/massnahme/services/massnahme-updated.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(MassnahmenAttributeEditorComponent.name, () => {
  let component: MassnahmenAttributeEditorComponent;
  let fixture: ComponentFixture<MassnahmenAttributeEditorComponent>;

  let dataSubject: Subject<Data>;

  let organisationenService: OrganisationenService;
  let massnahmeService: MassnahmeService;
  let massnahmenRoutingService: MassnahmenRoutingService;
  let notifyUserService: NotifyUserService;
  let activatedRoute: ActivatedRoute;
  let massnahmeUpdatedService: MassnahmeUpdatedService;
  let massnahmeNetzbezugDisplayService: MassnahmeNetzbezugDisplayService;

  beforeEach(() => {
    dataSubject = new Subject();

    organisationenService = mock(OrganisationenService);
    massnahmeService = mock(MassnahmeService);
    massnahmenRoutingService = mock(MassnahmenRoutingService);
    notifyUserService = mock(NotifyUserService);
    activatedRoute = mock(ActivatedRoute);
    massnahmeUpdatedService = mock(MassnahmenToolComponent);
    massnahmeNetzbezugDisplayService = mock(MassnahmenToolComponent);

    when(activatedRoute.data).thenReturn(dataSubject.asObservable());
    when(organisationenService.getAlleOrganisationen()).thenReturn(of([defaultOrganisation]));

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

  describe('with massnahme', () => {
    let massnahme: Massnahme;

    beforeEach(() => {
      massnahme = { ...defaultMassnahme, id: 2 };
      dataSubject.next({ massnahme });
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
        umsetzungsstatus: Umsetzungsstatus.IDEE,
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
      });
    }));

    it('should read command from form correct', fakeAsync(() => {
      when(massnahmeService.saveMassnahme(anything())).thenReturn(Promise.resolve(massnahme));

      component.formGroup.markAsDirty();
      component.onSave();

      verify(massnahmeService.saveMassnahme(anything())).once();
      const command = capture(massnahmeService.saveMassnahme).last()[0];
      expect(command).toEqual({
        id: massnahme.id,
        version: massnahme.version,
        bezeichnung: massnahme.bezeichnung,
        massnahmenkategorien: massnahme.massnahmenkategorien,
        netzbezug: massnahme.netzbezug,
        umsetzungsstatus: massnahme.umsetzungsstatus,
        veroeffentlicht: massnahme.veroeffentlicht,
        planungErforderlich: massnahme.planungErforderlich,
        durchfuehrungszeitraum: massnahme.durchfuehrungszeitraum,
        baulastZustaendigerId: massnahme.baulastZustaendiger?.id,

        prioritaet: massnahme.prioritaet,
        kostenannahme: massnahme.kostenannahme,
        netzklassen: massnahme.netzklassen,
        zustaendigerId: massnahme.zustaendiger?.id,
        unterhaltsZustaendigerId: massnahme.unterhaltsZustaendiger?.id,
        maViSID: massnahme.maViSID,
        verbaID: massnahme.verbaID,
        lgvfgid: massnahme.lgvfgid,
        massnahmeKonzeptID: massnahme.massnahmeKonzeptID,
        sollStandard: SollStandard.BASISSTANDARD,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'konzeptionsQuelle',
        realisierungshilfe: Realisierungshilfe.NR_2_2_1,
      } as SaveMassnahmeCommand);
    }));

    it('should update umsetzungsstand for status changed to STORNIERT', fakeAsync(() => {
      const neueMassnahme: Massnahme = { ...massnahme, konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME };
      neueMassnahme.umsetzungsstatus = Umsetzungsstatus.STORNIERT;
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
        it(`should update umsetzungsstand for status changed from STORNIERT to UMGESETZT (${k})`, fakeAsync(() => {
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
});
