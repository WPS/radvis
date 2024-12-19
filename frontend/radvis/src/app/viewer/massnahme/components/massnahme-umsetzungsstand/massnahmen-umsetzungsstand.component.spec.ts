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
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, ActivatedRouteSnapshot, Data } from '@angular/router';
import { MockBuilder } from 'ng-mocks';
import { MatomoTracker } from 'ngx-matomo-client';
import { Subject } from 'rxjs';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { MassnahmenUmsetzungsstandComponent } from 'src/app/viewer/massnahme/components/massnahme-umsetzungsstand/massnahmen-umsetzungsstand.component';
import { GrundFuerAbweichungZumMassnahmenblatt } from 'src/app/viewer/massnahme/models/grund-fuer-abweichung-zum-massnahmenblatt';
import { GrundFuerNichtUmsetzungDerMassnahme } from 'src/app/viewer/massnahme/models/grund-fuer-nicht-umsetzung-der-massnahme';
import { defaultUmsetzungsstand } from 'src/app/viewer/massnahme/models/massnahmen-test-data-provider.spec';
import { PruefungQualitaetsstandardsErfolgt } from 'src/app/viewer/massnahme/models/pruefung-qualitaetsstandards-erfolgt';
import { Umsetzungsstand } from 'src/app/viewer/massnahme/models/umsetzungsstand';
import { UmsetzungsstandStatus } from 'src/app/viewer/massnahme/models/umsetzungsstand-status';
import { MassnahmeNetzbezugDisplayService } from 'src/app/viewer/massnahme/services/massnahme-netzbezug-display.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(MassnahmenUmsetzungsstandComponent.name, () => {
  let massnahmenUmsetzungsstandComponent: MassnahmenUmsetzungsstandComponent;
  let fixture: ComponentFixture<MassnahmenUmsetzungsstandComponent>;

  let dataSubject: Subject<Data>;

  let massnahmeService: MassnahmeService;
  let notifyUserService: NotifyUserService;
  let activatedRoute: ActivatedRoute;
  let dialog: MatDialog;
  let massnahmeNetzbezugDisplayService: MassnahmeNetzbezugDisplayService;
  let matomoTracker: MatomoTracker;

  beforeEach(() => {
    dataSubject = new Subject();

    massnahmeService = mock(MassnahmeService);
    notifyUserService = mock(NotifyUserService);
    activatedRoute = mock(ActivatedRoute);
    dialog = mock(MatDialog);
    massnahmeNetzbezugDisplayService = {
      showNetzbezug: (): void => {},
    };
    matomoTracker = mock(MatomoTracker);

    when(activatedRoute.data).thenReturn(dataSubject.asObservable());
    when(activatedRoute.snapshot).thenReturn({ url: [''] } as unknown as ActivatedRouteSnapshot);

    return MockBuilder(MassnahmenUmsetzungsstandComponent, ViewerModule)
      .provide({
        provide: MassnahmeService,
        useValue: instance(massnahmeService),
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
        provide: MatDialog,
        useValue: instance(dialog),
      })
      .provide({
        provide: MatomoTracker,
        useValue: instance(mock(MatomoTracker)),
      })
      .provide({
        provide: MassnahmeNetzbezugDisplayService,
        useValue: massnahmeNetzbezugDisplayService,
      })
      .provide({
        provide: MatomoTracker,
        useValue: instance(matomoTracker),
      });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MassnahmenUmsetzungsstandComponent);
    massnahmenUmsetzungsstandComponent = fixture.componentInstance;
    dataSubject.next({ umsetzungsstand: defaultUmsetzungsstand });
  });

  it('should create', () => {
    expect(massnahmenUmsetzungsstandComponent).toBeTruthy();
  });

  describe('form', () => {
    it('should fill form with umsetzungsstand correct', fakeAsync(() => {
      // eslint-disable-next-line @typescript-eslint/dot-notation
      expect(massnahmenUmsetzungsstandComponent['umsetzungsstand']).toEqual(defaultUmsetzungsstand);

      expect(massnahmenUmsetzungsstandComponent.formGroup.getRawValue()).toEqual({
        umsetzungGemaessMassnahmenblatt: false,
        grundFuerAbweichungZumMassnahmenblatt:
          GrundFuerAbweichungZumMassnahmenblatt.UMSETZUNG_ALTERNATIVER_MASSNAHME_ERFORDERLICH,
        pruefungQualitaetsstandardsErfolgt: PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH,
        beschreibungAbweichenderMassnahme: 'Freitext Beschreibung abweichender Massnahme',
        kostenDerMassnahme: 10000,
        grundFuerNichtUmsetzungDerMassnahme: GrundFuerNichtUmsetzungDerMassnahme.LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH,
        anmerkung: 'Freitext Anmerkung',
      });
    }));

    it('should read command from form correct', fakeAsync(() => {
      when(massnahmeService.saveUmsetzungsstand(anything())).thenReturn(Promise.resolve(defaultUmsetzungsstand));

      massnahmenUmsetzungsstandComponent.formGroup.markAsDirty();
      massnahmenUmsetzungsstandComponent.onSave();

      verify(massnahmeService.saveUmsetzungsstand(anything())).once();
      const command = capture(massnahmeService.saveUmsetzungsstand).last()[0];

      expect(command).toEqual({
        id: defaultUmsetzungsstand.id,
        version: defaultUmsetzungsstand.version,
        umsetzungGemaessMassnahmenblatt: defaultUmsetzungsstand.umsetzungGemaessMassnahmenblatt,
        grundFuerAbweichungZumMassnahmenblatt: defaultUmsetzungsstand.grundFuerAbweichungZumMassnahmenblatt,
        pruefungQualitaetsstandardsErfolgt: defaultUmsetzungsstand.pruefungQualitaetsstandardsErfolgt,
        beschreibungAbweichenderMassnahme: defaultUmsetzungsstand.beschreibungAbweichenderMassnahme,
        kostenDerMassnahme: defaultUmsetzungsstand.kostenDerMassnahme,
        grundFuerNichtUmsetzungDerMassnahme: defaultUmsetzungsstand.grundFuerNichtUmsetzungDerMassnahme,
        anmerkung: defaultUmsetzungsstand.anmerkung,
      });
    }));

    it('should not save with invalid entries', fakeAsync(() => {
      when(massnahmeService.saveUmsetzungsstand(anything())).thenReturn(Promise.resolve(defaultUmsetzungsstand));

      // Pflichtfeld
      massnahmenUmsetzungsstandComponent.formGroup.patchValue({
        pruefungQualitaetsstandardsErfolgt: null,
      });

      massnahmenUmsetzungsstandComponent.formGroup.markAsDirty();
      massnahmenUmsetzungsstandComponent.onSave();

      verify(massnahmeService.saveMassnahme(anything())).never();

      // Massnahmen zuruecksetzen
      massnahmenUmsetzungsstandComponent.onReset();

      massnahmenUmsetzungsstandComponent.onSave();

      verify(massnahmeService.saveMassnahme(anything())).never();

      // muss Ganzzahl sein
      massnahmenUmsetzungsstandComponent.formGroup.patchValue({
        kostenDerMassnahme: 12.34,
      });

      massnahmenUmsetzungsstandComponent.formGroup.markAsDirty();
      massnahmenUmsetzungsstandComponent.onSave();

      verify(massnahmeService.saveMassnahme(anything())).never();

      // Beschreibung zu lang
      massnahmenUmsetzungsstandComponent.formGroup.patchValue({
        beschreibungAbweichenderMassnahme: new Array(30001).join('a'),
      });

      massnahmenUmsetzungsstandComponent.formGroup.markAsDirty();
      massnahmenUmsetzungsstandComponent.onSave();

      verify(massnahmeService.saveMassnahme(anything())).never();

      // Anmerkung zu lang
      massnahmenUmsetzungsstandComponent.formGroup.patchValue({
        anmerkung: new Array(30001).join('a'),
      });

      massnahmenUmsetzungsstandComponent.formGroup.markAsDirty();
      massnahmenUmsetzungsstandComponent.onSave();

      verify(massnahmeService.saveMassnahme(anything())).never();
      expect().nothing();
    }));

    it('should disable Form if Umsetzungsstand is not editable', fakeAsync(() => {
      expect(massnahmenUmsetzungsstandComponent.formGroup.disabled).toBeFalse();

      const nonEditableUmsetzungsstand: Umsetzungsstand = { ...defaultUmsetzungsstand };
      nonEditableUmsetzungsstand.massnahmeUmsetzungsstatus = Umsetzungsstatus.UMGESETZT;
      nonEditableUmsetzungsstand.umsetzungsstandStatus = UmsetzungsstandStatus.AKTUALISIERT;
      dataSubject.next({ umsetzungsstand: nonEditableUmsetzungsstand });
      tick();

      expect(massnahmenUmsetzungsstandComponent.formGroup.disabled).toBeTrue();

      dataSubject.next({ umsetzungsstand: defaultUmsetzungsstand });
      tick();

      expect(massnahmenUmsetzungsstandComponent.formGroup.disabled).toBeFalse();
    }));

    it('should disable Form if Umsetzungsstand is not editable', fakeAsync(() => {
      expect(massnahmenUmsetzungsstandComponent.formGroup.disabled).toBeFalse();

      dataSubject.next({ umsetzungsstand: { ...defaultUmsetzungsstand, canEdit: false } as Umsetzungsstand });
      tick();

      expect(massnahmenUmsetzungsstandComponent.formGroup.disabled).toBeTrue();

      dataSubject.next({ umsetzungsstand: { ...defaultUmsetzungsstand, canEdit: true } as Umsetzungsstand });
      tick();

      expect(massnahmenUmsetzungsstandComponent.formGroup.disabled).toBeFalse();
    }));
  });

  describe('hinweis-dialog', () => {
    it('should show dialog if Massnahme is umgesetzt/storniert and aktualisierung angefordert', fakeAsync(() => {
      const umsetzungsstand: Umsetzungsstand = { ...defaultUmsetzungsstand };
      umsetzungsstand.massnahmeUmsetzungsstatus = Umsetzungsstatus.UMGESETZT;
      umsetzungsstand.umsetzungsstandStatus = UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT;
      dataSubject.next({ umsetzungsstand });
      tick();

      verify(dialog.open(anything(), anything())).once();
      expect().nothing();
    }));

    it('should not show dialog if Massnahme is not umgesetzt/storniert', fakeAsync(() => {
      const umsetzungsstand: Umsetzungsstand = { ...defaultUmsetzungsstand };
      umsetzungsstand.massnahmeUmsetzungsstatus = Umsetzungsstatus.IDEE;
      umsetzungsstand.umsetzungsstandStatus = UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT;
      dataSubject.next({ umsetzungsstand });
      tick();

      verify(dialog.open(anything(), anything())).never();
      expect().nothing();
    }));
  });
});
