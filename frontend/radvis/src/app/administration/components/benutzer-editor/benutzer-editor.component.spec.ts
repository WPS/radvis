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
import { ReactiveFormsModule } from '@angular/forms';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MockComponent, MockModule, MockPipe } from 'ng-mocks';
import { MatomoTracker } from 'ngx-matomo-client';
import { Benutzer } from 'src/app/administration/models/benutzer';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { Rolle } from 'src/app/administration/models/rolle';
import { SaveBenutzerCommand } from 'src/app/administration/models/save-benutzer-command';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { BenutzerService } from 'src/app/administration/services/benutzer.service';
import { ActionButtonComponent } from 'src/app/form-elements/components/action-button/action-button.component';
import {
  AutocompleteDropdownComponent,
  AutoCompleteOption,
} from 'src/app/form-elements/components/autocomplete-dropdown/autocomplete-dropdown.component';
import { EnumDropdownControlComponent } from 'src/app/form-elements/components/enum-dropdown-control/enum-dropdown-control.component';
import { ErrorMessagePipe } from 'src/app/form-elements/components/error-message.pipe';
import { TextInputControlComponent } from 'src/app/form-elements/components/text-input-control/text-input-control.component';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { BenutzerEditorComponent } from './benutzer-editor.component';

describe(BenutzerEditorComponent.name, () => {
  let component: BenutzerEditorComponent;
  let fixture: ComponentFixture<BenutzerEditorComponent>;
  const benutzerId = 74654;
  const existingBenutzer: Benutzer = {
    email: 'ich@test.de',
    nachname: 'mein nachname',
    vorname: 'mein vorname',
    status: BenutzerStatus.AKTIV,
    ablaufdatum: null,
    version: 1,
    organisation: defaultOrganisation,
    rollen: [Rolle.RADVIS_ADMINISTRATOR, Rolle.RADWEGE_ERFASSERIN],
  };
  const testOrganisationen: Verwaltungseinheit[] = [
    defaultOrganisation,
    {
      id: 3,
      name: 'Gemeinde Organisation 1',
      organisationsArt: OrganisationsArt.GEMEINDE,
      idUebergeordneteOrganisation: null,
      aktiv: true,
    },
    {
      id: 4,
      name: 'Kreis Organisation 2',
      organisationsArt: OrganisationsArt.KREIS,
      idUebergeordneteOrganisation: null,
      aktiv: true,
    },
  ];
  const activatedRoute = {
    snapshot: {
      data: {
        benutzer: existingBenutzer,
        organisationen: testOrganisationen,
      },
      params: {
        id: benutzerId,
      },
    },
  };

  const defaultGewaehlteOrganisation: AutoCompleteOption = {
    id: defaultOrganisation.id,
    name: defaultOrganisation.name,
    displayText: Verwaltungseinheit.getDisplayName(defaultOrganisation),
  };

  let benutzerService: BenutzerService;
  let administrationRoutingService: AdministrationRoutingService;
  let matomoTracker: MatomoTracker;

  beforeEach(async () => {
    benutzerService = mock(BenutzerService);
    administrationRoutingService = mock(AdministrationRoutingService);
    matomoTracker = mock(MatomoTracker);

    await TestBed.configureTestingModule({
      declarations: [
        BenutzerEditorComponent,
        MockComponent(ActionButtonComponent),
        MockComponent(AutocompleteDropdownComponent),
        MockComponent(TextInputControlComponent),
        MockComponent(EnumDropdownControlComponent),

        MockPipe(ErrorMessagePipe),
      ],
      imports: [
        RouterTestingModule,
        MockModule(MaterialDesignModule),
        MockModule(ReactiveFormsModule),
        MockModule(MatDatepickerModule),
        MockModule(MatNativeDateModule),
      ],
      providers: [
        { provide: BenutzerService, useValue: instance(benutzerService) },
        { provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) },
        { provide: ActivatedRoute, useValue: activatedRoute },
        {
          provide: MatomoTracker,
          useValue: instance(mock(MatomoTracker)),
        },
        { provide: AdministrationRoutingService, useValue: instance(administrationRoutingService) },
        { provide: MatomoTracker, useValue: instance(matomoTracker) },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BenutzerEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should fill form', () => {
    const expectedBenutzerinForm = {
      status: BenutzerStatus.getDisplayName(BenutzerStatus.AKTIV),
      ablaufdatum: null,
      vorname: existingBenutzer.vorname,
      nachname: existingBenutzer.nachname,
      email: existingBenutzer.email,
      organisation: defaultGewaehlteOrganisation,
      rollen: ['RADVIS_ADMINISTRATOR', 'RADWEGE_ERFASSERIN'],
    };
    expect(component.formGroup.getRawValue()).toEqual(expectedBenutzerinForm);
  });

  it('should read form', fakeAsync(() => {
    when(benutzerService.save(anything())).thenReturn(Promise.resolve({ ...existingBenutzer, version: 2 } as Benutzer));
    const organisationId = 94766;
    component.formGroup.patchValue({
      nachname: 'Test',
      organisation: {
        ...defaultGewaehlteOrganisation,
        id: organisationId,
      },
    });
    component.onSave();

    verify(benutzerService.save(anything())).once();
    expect(capture(benutzerService.save).last()[0]).toEqual({
      id: benutzerId,
      ablaufdatum: null,
      vorname: existingBenutzer.vorname,
      nachname: 'Test',
      email: 'ich@test.de',
      version: 1,
      organisation: organisationId,
      rollen: ['RADVIS_ADMINISTRATOR', 'RADWEGE_ERFASSERIN'],
    } as SaveBenutzerCommand);
  }));

  it('should not save when invalid', () => {
    spyOnProperty(component.formGroup, 'valid').and.returnValue(false);
    component.onSave();
    verify(benutzerService.save(anything())).never();
    expect().nothing();
  });

  describe('validation', () => {
    it('should be invalid if fields not set', () => {
      component.formGroup.reset();
      expect(component.formGroup.get('vorname')?.valid).toBeFalse();
      expect(component.formGroup.get('nachname')?.valid).toBeFalse();
      expect(component.formGroup.get('organisation')?.valid).toBeFalse();
      expect(component.formGroup.get('rollen')?.valid).toBeFalse();
    });

    it('should be invalid when fields too long', () => {
      const stringLonger255 =
        'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata ';
      component.formGroup.patchValue({
        nachname: stringLonger255,
        vorname: stringLonger255,
      });
      expect(component.formGroup.get('vorname')?.valid).toBeFalse();
      expect(component.formGroup.get('nachname')?.valid).toBeFalse();
    });

    it('should be invalid if ablaufdatum is enabled but not set', () => {
      component.isAblaufdatumEnabled = true;
      expect(component.formGroup.get('ablaufdatum')?.valid).toBeFalse();
    });
  });

  describe('onSave', () => {
    it('should trim nachname, vorname onSave', () => {
      when(benutzerService.save(anything())).thenReturn(
        Promise.resolve({ ...existingBenutzer, version: 2 } as Benutzer)
      );

      const benutzer = activatedRoute.snapshot.data.benutzer;
      component.formGroup.reset();
      component.formGroup.patchValue({
        version: benutzer.version,
        nachname: '   test   ',
        vorname: '   test   ',
        email: benutzer.email,
        organisation: defaultGewaehlteOrganisation,
        rollen: benutzer.rollen,
      });

      component.onSave();

      fixture.detectChanges();
      const data = capture(benutzerService.save).first()[0];

      expect(data).toEqual({
        id: activatedRoute.snapshot.params.id,
        version: benutzer.version,
        ablaufdatum: null,
        nachname: 'test',
        vorname: 'test',
        email: benutzer.email,
        organisation: defaultGewaehlteOrganisation.id!,
        rollen: benutzer.rollen,
      });
    });
  });

  describe(BenutzerEditorComponent.prototype.onZurueck.name, () => {
    beforeEach(() => {
      component.onZurueck();
    });

    it('should invoke toAdministration', () => {
      verify(administrationRoutingService.toAdministration()).once();
      expect().nothing();
    });
  });

  describe('onAblehnen', () => {
    it('should disable the ablaufdatum', fakeAsync(() => {
      // Arrange
      when(benutzerService.aendereBenutzerstatus(anything(), anything(), anything())).thenReturn(
        Promise.resolve(existingBenutzer)
      );
      const spy = spyOn(component, 'disableAblaufdatum');

      // Act
      component.onAblehnen();
      tick();

      // Assert
      expect(spy).toHaveBeenCalled();
    }));
  });
});
