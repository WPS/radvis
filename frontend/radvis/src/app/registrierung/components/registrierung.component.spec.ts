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

import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { MockComponent, MockedComponentFixture, MockModule, MockRender } from 'ng-mocks';
import { Rolle } from 'src/app/administration/models/rolle';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { RegistriereBenutzerCommand } from 'src/app/registrierung/registriere-benutzer-command';
import { RegistrierungService } from 'src/app/registrierung/registrierung.service';
import { OrganisationenDropdownControlComponent } from 'src/app/shared/components/organisationen-dropdown-control/organisationen-dropdown-control.component';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { RegistrierungComponent } from './registrierung.component';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { FormElementsModule } from 'src/app/form-elements/form-elements.module';

describe('RegistrierungComponent', () => {
  let component: RegistrierungComponent;
  let fixture: MockedComponentFixture<RegistrierungComponent>;
  let registrierungService: RegistrierungService;

  beforeEach(() => {
    registrierungService = mock(RegistrierungService);
    return TestBed.configureTestingModule({
      declarations: [RegistrierungComponent, MockComponent(OrganisationenDropdownControlComponent)],
      providers: [{ provide: RegistrierungService, useValue: instance(registrierungService) }],
      imports: [
        RouterTestingModule,
        FormElementsModule,
        MockModule(MaterialDesignModule),
        MockModule(ReactiveFormsModule),
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = MockRender(RegistrierungComponent);
    component = fixture.point.componentInstance;
    // @ts-expect-error Migration von ts-ignore
    spyOn(component, 'reloadApplication').and.callFake(() => {});
    fixture.detectChanges();
  });

  it('should read form correct', fakeAsync(() => {
    const status = BenutzerStatus.getDisplayName(BenutzerStatus.WARTE_AUF_FREISCHALTUNG);
    const vorname = 'Vorname';
    const nachname = 'Nachname';
    const email = 'vorname.nachname@wps.de';
    const organisation = { ...defaultOrganisation, id: 954676 };
    const rollen = [Rolle.RADVIS_ADMINISTRATOR, Rolle.KREISKOORDINATOREN];
    component.registrierungForm.patchValue({
      status,
      vorname,
      nachname,
      email,
      organisation,
      rollen,
    });
    when(registrierungService.register(anything())).thenResolve();

    component.register();

    verify(registrierungService.register(anything())).once();
    expect(capture(registrierungService.register).last()[0]).toEqual({
      vorname,
      nachname,
      email,
      organisation: organisation.id,
      rollen,
    } as RegistriereBenutzerCommand);
  }));

  it('should not save when invalid', () => {
    spyOnProperty(component.registrierungForm, 'valid').and.returnValue(false);
    component.register();
    verify(registrierungService.register(anything())).never();
    expect().nothing();
  });

  describe('validation', () => {
    it('should be invalid if fields not set', () => {
      component.registrierungForm.reset();
      expect(component.registrierungForm.get('vorname')?.valid).toBeFalse();
      expect(component.registrierungForm.get('nachname')?.valid).toBeFalse();
      expect(component.registrierungForm.get('organisation')?.valid).toBeFalse();
      expect(component.registrierungForm.get('rollen')?.valid).toBeFalse();
    });

    it('should be invalid when fields too long', () => {
      const stringLonger255 =
        'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata ';
      component.registrierungForm.patchValue({
        nachname: stringLonger255,
        vorname: stringLonger255,
      });
      expect(component.registrierungForm.get('vorname')?.valid).toBeFalse();
      expect(component.registrierungForm.get('nachname')?.valid).toBeFalse();
    });
  });
});
