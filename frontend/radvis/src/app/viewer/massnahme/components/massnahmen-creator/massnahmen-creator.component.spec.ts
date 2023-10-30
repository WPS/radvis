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
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MockBuilder } from 'ng-mocks';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { MassnahmenCreatorComponent } from 'src/app/viewer/massnahme/components/massnahmen-creator/massnahmen-creator.component';
import { CreateMassnahmeCommand } from 'src/app/viewer/massnahme/models/create-massnahme-command';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { Netzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug';
import { defaultNetzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug-test-data-provider.spec';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anyString, anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe('MassnahmenCreatorComponent', () => {
  let component: MassnahmenCreatorComponent;
  let fixture: ComponentFixture<MassnahmenCreatorComponent>;

  let viewerRoutingService: ViewerRoutingService;
  let organisationenService: OrganisationenService;
  let massnahmeService: MassnahmeService;
  let changeDetectorRef: ChangeDetectorRef;
  let notifyUserService: NotifyUserService;
  let errorHandlingService: ErrorHandlingService;

  beforeEach(() => {
    viewerRoutingService = mock(ViewerRoutingService);
    organisationenService = mock(OrganisationenService);
    massnahmeService = mock(MassnahmeService);
    changeDetectorRef = mock(ChangeDetectorRef);
    notifyUserService = mock(NotifyUserService);
    errorHandlingService = mock(ErrorHandlingService);

    return MockBuilder(MassnahmenCreatorComponent, ViewerModule)
      .provide({
        provide: ViewerRoutingService,
        useValue: instance(viewerRoutingService),
      })
      .provide({
        provide: OrganisationenService,
        useValue: instance(organisationenService),
      })
      .provide({
        provide: MassnahmeService,
        useValue: instance(massnahmeService),
      })
      .provide({
        provide: ChangeDetectorRef,
        useValue: instance(changeDetectorRef),
      })
      .provide({
        provide: ErrorHandlingService,
        useValue: instance(errorHandlingService),
      })
      .provide({
        provide: NotifyUserService,
        useValue: instance(notifyUserService),
      });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MassnahmenCreatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('save', () => {
    it('should toggle validation message correctly', () => {
      expect(getValidationMessage(fixture)).toBeUndefined();

      const netzbezug = {
        kantenBezug: [
          {
            ...defaultNetzbezug.kantenBezug[0],
            kanteId: 70,
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
        punktuellerKantenBezug: [],
      } as Netzbezug;
      const baulastZustaendiger = {
        id: 5,
        name: 'Orgablaaaa',
        organisationsArt: OrganisationsArt.GEMEINDE,
        idUebergeordneteOrganisation: null,
      } as Verwaltungseinheit;

      const values = {
        bezeichnung: 'bezeichnung',
        massnahmenkategorien: ['UMWIDMUNG_GEMEINSAMER_RADGEHWEG'],
        netzbezug,
        umsetzungsstatus: Umsetzungsstatus.IDEE,
        veroeffentlicht: false,
        planungErforderlich: false,
        durchfuehrungszeitraum: -5,
        baulastZustaendiger,
        sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'WAMBO',
      };

      when(massnahmeService.createMassnahme(anything())).thenResolve(1);

      component.onSave();
      verify(notifyUserService.warn(anyString())).never();

      component.formGroup.patchValue(values);
      component.formGroup.markAsDirty();

      component.onSave();
      verify(notifyUserService.warn(anyString())).once();
    });

    it('should build command from form correct', fakeAsync(() => {
      const netzbezug = {
        kantenBezug: [
          {
            ...defaultNetzbezug.kantenBezug[0],
            kanteId: 70,
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
        punktuellerKantenBezug: [],
      } as Netzbezug;
      const baulastZustaendiger = {
        id: 5,
        name: 'Orgablaaaa',
        organisationsArt: OrganisationsArt.GEMEINDE,
        idUebergeordneteOrganisation: null,
      } as Verwaltungseinheit;

      const values = {
        bezeichnung: 'bezeichnung',
        massnahmenkategorien: ['UMWIDMUNG_GEMEINSAMER_RADGEHWEG'],
        netzbezug,
        umsetzungsstatus: Umsetzungsstatus.IDEE,
        veroeffentlicht: false,
        planungErforderlich: false,
        durchfuehrungszeitraum: 2020,
        baulastZustaendiger,
        sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'WAMBO',
      };

      when(massnahmeService.createMassnahme(anything())).thenResolve(1);

      component.formGroup.patchValue(values);
      component.formGroup.markAsDirty();
      component.onSave();

      verify(massnahmeService.createMassnahme(anything())).once();
      const command = capture(massnahmeService.createMassnahme).last()[0];
      expect(command).toEqual({
        bezeichnung: 'bezeichnung',
        massnahmenkategorien: ['UMWIDMUNG_GEMEINSAMER_RADGEHWEG'],
        netzbezug,
        umsetzungsstatus: Umsetzungsstatus.IDEE,
        veroeffentlicht: false,
        planungErforderlich: false,
        durchfuehrungszeitraum: { geplanterUmsetzungsstartJahr: 2020 },
        baulastZustaendigerId: 5,
        sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'WAMBO',
      } as CreateMassnahmeCommand);
    }));
  });

  describe(MassnahmenCreatorComponent.prototype.onReset.name, () => {
    beforeEach(() => {
      const netzbezug = {
        kantenBezug: [
          {
            ...defaultNetzbezug.kantenBezug[0],
            kanteId: 70,
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
        punktuellerKantenBezug: [],
      } as Netzbezug;
      const baulastZustaendiger = {
        id: 5,
        name: 'Orgablaaaa',
        organisationsArt: OrganisationsArt.GEMEINDE,
        idUebergeordneteOrganisation: null,
      } as Verwaltungseinheit;
      const values = {
        bezeichnung: 'bezeichnung',
        massnahmenkategorien: ['UMWIDMUNG_GEMEINSAMER_RADGEHWEG'],
        netzbezug,
        umsetzungsstatus: Umsetzungsstatus.PLANUNG,
        veroeffentlicht: true,
        planungErforderlich: false,
        durchfuehrungszeitraum: -5,
        baulastZustaendiger,
        sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'WAMBO',
      };
      component.formGroup.patchValue(values);

      component.onReset();
    });

    it('should reset correctly', () => {
      expect(component.formGroup.value).toEqual({
        umsetzungsstatus: Umsetzungsstatus.IDEE,
        massnahmenkategorien: [],
        veroeffentlicht: false,
        planungErforderlich: false,
        bezeichnung: null,
        netzbezug: null,
        durchfuehrungszeitraum: null,
        baulastZustaendiger: null,
        sollStandard: null,
        handlungsverantwortlicher: null,
        konzeptionsquelle: null,
        sonstigeKonzeptionsquelle: null,
      });
    });
  });
});

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function getValidationMessage(fixture: ComponentFixture<MassnahmenCreatorComponent>): string | undefined {
  return fixture.debugElement.queryAll(By.css('.fehlertext'))[0]?.nativeElement.textContent;
}
