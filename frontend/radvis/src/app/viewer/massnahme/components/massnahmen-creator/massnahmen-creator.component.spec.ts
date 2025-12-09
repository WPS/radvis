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
import { MockBuilder } from 'ng-mocks';
import { of } from 'rxjs';
import { AutoCompleteOption } from 'src/app/form-elements/components/autocomplete-dropdown/autocomplete-dropdown.component';
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { defaultNetzbezug } from 'src/app/shared/models/netzbezug-test-data-provider.spec';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { MassnahmenCreatorComponent } from 'src/app/viewer/massnahme/components/massnahmen-creator/massnahmen-creator.component';
import { CreateMassnahmeCommand } from 'src/app/viewer/massnahme/models/create-massnahme-command';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { ZurueckstellungsGrund } from 'src/app/viewer/massnahme/models/zurueckstellung-grund';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anyString, anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(MassnahmenCreatorComponent.name, () => {
  let component: MassnahmenCreatorComponent;
  let fixture: ComponentFixture<MassnahmenCreatorComponent>;

  let viewerRoutingService: ViewerRoutingService;
  let organisationenService: OrganisationenService;
  let massnahmeService: MassnahmeService;
  let changeDetectorRef: ChangeDetectorRef;
  let notifyUserService: NotifyUserService;
  let errorHandlingService: ErrorHandlingService;
  let benutzerDetailsService: BenutzerDetailsService;

  beforeEach(() => {
    viewerRoutingService = mock(ViewerRoutingService);
    organisationenService = mock(OrganisationenService);
    massnahmeService = mock(MassnahmeService);
    changeDetectorRef = mock(ChangeDetectorRef);
    notifyUserService = mock(NotifyUserService);
    errorHandlingService = mock(ErrorHandlingService);
    benutzerDetailsService = mock(BenutzerDetailsService);

    when(organisationenService.getAlleOrganisationen()).thenReturn(of([defaultOrganisation]));

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
        provide: BenutzerDetailsService,
        useValue: instance(benutzerDetailsService),
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

      component.formGroup.controls.zurueckstellungsGrund.setValue(
        ZurueckstellungsGrund.PERSONELLE_ZEITLICHE_RESSOURCEN
      );

      expect(component.formGroup.controls.begruendungZurueckstellung.enabled).toBeFalse();
      expect(component.formGroup.value.begruendungZurueckstellung).toBeFalsy();
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

  describe('onKategorieChanged', () => {
    it('should update options', () => {
      component.massnahmenkategorieOptions = Massnahmenkategorien.RADNETZ_2024_KATEGORIEN_ONLY;
      component.formGroup.controls.konzeptionsquelle.setValue(Konzeptionsquelle.RADNETZ_MASSNAHME);
      expect(component.massnahmenkategorieOptions).toEqual(Massnahmenkategorien.ALL);
    });

    [
      Konzeptionsquelle.KOMMUNALES_KONZEPT,
      Konzeptionsquelle.KREISKONZEPT,
      Konzeptionsquelle.RADNETZ_MASSNAHME,
      Konzeptionsquelle.SONSTIGE,
    ].forEach(quelle => {
      it(`should show all options for konzeptionsquelle=${quelle}`, () => {
        component.massnahmenkategorieOptions = [];
        component.formGroup.controls.konzeptionsquelle.setValue(quelle);
        expect(component.massnahmenkategorieOptions).toEqual(Massnahmenkategorien.ALL);
      });
    });

    it('should show filtered options for konzeptionsquelle=RADNETZ_MASSNAHME_2024', () => {
      component.massnahmenkategorieOptions = [];
      component.formGroup.controls.konzeptionsquelle.setValue(Konzeptionsquelle.RADNETZ_MASSNAHME_2024);
      expect(component.massnahmenkategorieOptions).toEqual(Massnahmenkategorien.RADNETZ_2024_KATEGORIEN_ONLY);
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

  describe('save', () => {
    it('should toggle validation message correctly', () => {
      expect(component.formGroup.errors).toBeNull();

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

      const baulastZustaendiger: AutoCompleteOption = {
        id: 5,
        name: 'Orgablaaaa',
        displayText: 'Orgablaaaa (Gemeinde)',
      };

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

      const baulastZustaendiger: AutoCompleteOption = {
        id: 5,
        name: 'Orgablaaaa',
        displayText: 'Orgablaaaa (Gemeinde)',
      };

      const zustaendiger: AutoCompleteOption = {
        id: 24,
        name: 'Zuständige Organisation',
        displayText: 'Zuständige Organisation (Regierungsbezirk)',
      };

      const values = {
        bezeichnung: 'bezeichnung',
        massnahmenkategorien: ['UMWIDMUNG_GEMEINSAMER_RADGEHWEG'],
        netzbezug,
        umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
        veroeffentlicht: false,
        planungErforderlich: false,
        durchfuehrungszeitraum: 2020,
        baulastZustaendiger,
        zustaendiger,
        sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'WAMBO',
        zurueckstellungsGrund: ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN,
      };

      when(massnahmeService.createMassnahme(anything())).thenResolve(1);

      component.formGroup.patchValue(values);
      component.formGroup.markAsDirty();
      component.onSave();

      verify(massnahmeService.createMassnahme(anything())).once();
      const command = capture(massnahmeService.createMassnahme).last()[0];
      const expected: CreateMassnahmeCommand = {
        bezeichnung: 'bezeichnung',
        massnahmenkategorien: ['UMWIDMUNG_GEMEINSAMER_RADGEHWEG'],
        netzbezug,
        umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
        veroeffentlicht: false,
        planungErforderlich: false,
        durchfuehrungszeitraum: { geplanterUmsetzungsstartJahr: 2020 },
        baulastZustaendigerId: 5,
        zustaendigerId: 24,
        sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'WAMBO',
        zurueckstellungsGrund: ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN,
        begruendungStornierungsanfrage: null,
        begruendungZurueckstellung: null,
      };
      expect(command).toEqual(expected);
    }));
  });

  describe(MassnahmenCreatorComponent.prototype.onReset.name, () => {
    it('should reset correctly', () => {
      const zustaendiger: Verwaltungseinheit = {
        id: 24,
        name: 'Zuständige Organisation',
        organisationsArt: OrganisationsArt.REGIERUNGSBEZIRK,
        idUebergeordneteOrganisation: null,
        aktiv: true,
      };
      when(benutzerDetailsService.aktuellerBenutzerOrganisation()).thenReturn(zustaendiger);
      const zustaendigerOption: AutoCompleteOption = {
        id: zustaendiger.id,
        name: zustaendiger.name,
        displayText: zustaendiger.name + ' (Regierungsbezirk)',
      };

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

      const baulastZustaendiger: AutoCompleteOption = {
        id: 5,
        name: 'Orgablaaaa',
        displayText: 'Orgablaaaa (Gemeinde)',
      };

      const andererZustaendiger: AutoCompleteOption = {
        id: 25,
        name: 'Andere Organisation',
        displayText: 'Andere Organisation (Kreis)',
      };

      const values = {
        bezeichnung: 'bezeichnung',
        massnahmenkategorien: ['UMWIDMUNG_GEMEINSAMER_RADGEHWEG'],
        netzbezug,
        umsetzungsstatus: Umsetzungsstatus.ZURUECKGESTELLT,
        veroeffentlicht: true,
        planungErforderlich: false,
        durchfuehrungszeitraum: -5,
        baulastZustaendiger,
        zustaendiger: andererZustaendiger,
        sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
        handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
        konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
        sonstigeKonzeptionsquelle: 'WAMBO',
        zurueckstellungsGrund: ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN,
      };
      component.formGroup.patchValue(values);
      component.formGroup.controls.zurueckstellungsGrund.enable();

      component.onReset();

      expect(component.formGroup.value).toEqual({
        umsetzungsstatus: Umsetzungsstatus.IDEE,
        massnahmenkategorien: [],
        veroeffentlicht: false,
        planungErforderlich: false,
        bezeichnung: null,
        netzbezug: null,
        durchfuehrungszeitraum: null,
        baulastZustaendiger: null,
        zustaendiger: zustaendigerOption,
        sollStandard: null,
        handlungsverantwortlicher: null,
        konzeptionsquelle: null,
        sonstigeKonzeptionsquelle: null,
      });
    });
  });

  describe('erlaubte umsetzungsstatus', () => {
    it('should disable Storniert-Status if radNETZ-Maßnahme and nutzer has no recht', () => {
      component.umsetzungsstatusOptions = Umsetzungsstatus.options;
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME });

      [
        Umsetzungsstatus.STORNIERT_ENGSTELLE,
        Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH,
        Umsetzungsstatus.STORNIERT,
      ].forEach(status => {
        expect(component.umsetzungsstatusOptions.map(opt => opt.name).includes(status)).toBe(false);
      });
    });

    it('should disable/reset Stornierung angefragt if no radNETZ-Maßnahme', () => {
      component.umsetzungsstatusOptions = Umsetzungsstatus.options;
      component.formGroup.patchValue({ umsetzungsstatus: Umsetzungsstatus.STORNIERUNG_ANGEFRAGT });

      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT });

      expect(
        component.umsetzungsstatusOptions.map(opt => opt.name).includes(Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)
      ).toBe(false);
      expect(component.formGroup.controls.umsetzungsstatus.value).toBeFalsy();
      expect(component.formGroup.controls.umsetzungsstatus.valid).toBeFalse();
      expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeFalse();
    });

    it('should enable Stornierung angefragt if radNETZ-Maßnahme', () => {
      component.umsetzungsstatusOptions = [];
      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME });

      expect(
        component.umsetzungsstatusOptions.map(opt => opt.name).includes(Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)
      ).toBe(true);
    });

    it('should reset nicht erlaubte umsetzungsstatus on konzeptionsquelle changed', () => {
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      component.formGroup.patchValue({
        konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
        umsetzungsstatus: Umsetzungsstatus.STORNIERT_ENGSTELLE,
      });

      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME });

      expect(component.formGroup.controls.umsetzungsstatus.value).toBeFalsy();
      expect(component.formGroup.controls.umsetzungsstatus.valid).toBeFalse();
      expect(component.formGroup.controls.umsetzungsstatus.disabled).toBeFalse();
    });

    it('should not reset erlaubte umsetzungsstatus on konzeptionsquelle changed', () => {
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      component.formGroup.patchValue({
        konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
        umsetzungsstatus: Umsetzungsstatus.IDEE,
      });

      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME });

      expect(component.formGroup.controls.umsetzungsstatus.value).toBe(Umsetzungsstatus.IDEE);
    });

    it('should enable Storniert-Status if no radNETZ-Maßnahme nutzer has no recht', () => {
      component.umsetzungsstatusOptions = [];
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(false);
      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT });

      [Umsetzungsstatus.STORNIERT_ENGSTELLE, Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH].forEach(status => {
        expect(component.umsetzungsstatusOptions.map(opt => opt.name).includes(status)).toBe(true);
      });
    });

    it('should enable Storniert-Status initially', () => {
      [
        Umsetzungsstatus.STORNIERT_ENGSTELLE,
        Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH,
        Umsetzungsstatus.STORNIERUNG_ANGEFRAGT,
      ].forEach(status => {
        expect(component.umsetzungsstatusOptions.map(opt => opt.name).includes(status)).toBe(true);
      });
    });

    it('should enable Storniert-Status if radNETZ-Maßnahme nutzer has recht', () => {
      component.umsetzungsstatusOptions = [];
      when(benutzerDetailsService.canMassnahmenStornieren()).thenReturn(true);
      component.formGroup.patchValue({ konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME });

      [Umsetzungsstatus.STORNIERT_ENGSTELLE, Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH].forEach(status => {
        expect(component.umsetzungsstatusOptions.map(opt => opt.name).includes(status)).toBe(true);
      });
    });
  });
});
