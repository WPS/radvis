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
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { AdministrationModule } from 'src/app/administration/administration.module';
import { OrganisationEditorComponent } from 'src/app/administration/components/organisation-editor/organisation-editor.component';
import { CreateOrganisationCommand } from 'src/app/administration/models/create-organisation-command';
import { SaveOrganisationCommand } from 'src/app/administration/models/save-organisation-command';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { OrganisationenVerwaltungService } from 'src/app/administration/services/organisationen-verwaltung.service';
import { Organisation } from 'src/app/shared/models/organisation-edit-view';
import {
  defaultEditOrganisation,
  defaultOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(OrganisationEditorComponent.name, () => {
  let component: OrganisationEditorComponent;
  let fixture: MockedComponentFixture<OrganisationEditorComponent>;
  let data$: BehaviorSubject<{ isCreator: boolean; organisation?: Organisation }>;

  let organisationenService: OrganisationenService;
  let organisationVerwaltenService: OrganisationenVerwaltungService;
  let administrationRoutingService: AdministrationRoutingService;

  beforeEach(() => {
    organisationenService = mock(OrganisationenService);
    administrationRoutingService = mock(AdministrationRoutingService);
    organisationVerwaltenService = mock(OrganisationenVerwaltungService);

    data$ = new BehaviorSubject<{ isCreator: boolean; organisation?: Organisation }>({
      isCreator: true,
      organisation: undefined,
    });
    const activatedRoute = {
      data: data$,
    };

    return MockBuilder(OrganisationEditorComponent, AdministrationModule)
      .provide({ provide: ActivatedRoute, useValue: activatedRoute })
      .provide({ provide: OrganisationenService, useValue: instance(organisationenService) })
      .provide({ provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) })
      .provide({ provide: ChangeDetectorRef, useValue: instance(mock(ChangeDetectorRef)) })
      .provide({ provide: OrganisationenVerwaltungService, useValue: instance(organisationVerwaltenService) })
      .provide({ provide: AdministrationRoutingService, useValue: instance(administrationRoutingService) });
  });

  beforeEach(() => {
    fixture = MockRender(OrganisationEditorComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('asEditor', () => {
    const organisationToEdit: Organisation = {
      id: 2,
      name: 'Die Organisation, die wir editieren wollen',
      uebergeordneteOrganisation: defaultOrganisation,
      organisationsArt: OrganisationsArt.TOURISMUSVERBAND,
      aktuellerBenutzerDarfBearbeiten: true,
      zustaendigFuerBereichOf: [{ ...defaultOrganisation, id: 2345 }],
      aktiv: true,
      version: 0,
    };

    let currentIdSpy: jasmine.Spy;

    beforeEach(() => {
      currentIdSpy = spyOnProperty(component, 'currentId');
      currentIdSpy.and.returnValue(organisationToEdit.id);
      data$.next({ organisation: organisationToEdit, isCreator: false });
    });

    it('should fill form', () => {
      expect(component.form.getRawValue()).toEqual({
        name: organisationToEdit.name,
        zustaendigFuerBereichOf: organisationToEdit.zustaendigFuerBereichOf,
        organisationsArt: organisationToEdit.organisationsArt,
        uebergeordneteOrganisation: Verwaltungseinheit.getDisplayName(organisationToEdit.uebergeordneteOrganisation),
      });
    });

    it('should disable when kein Bearbeitungsrecht', () => {
      // disabling wird über `.value` geprüft: disabled felder sind nicht drin
      data$.next({
        isCreator: false,
        organisation: { ...defaultEditOrganisation, aktuellerBenutzerDarfBearbeiten: false },
      });

      expect(component.form.value).toEqual({
        zustaendigFuerBereichOf: defaultEditOrganisation.zustaendigFuerBereichOf,
      });

      data$.next({
        isCreator: false,
        organisation: { ...defaultEditOrganisation, aktuellerBenutzerDarfBearbeiten: true },
      });

      expect(component.form.value).toEqual({
        name: organisationToEdit.name,
        zustaendigFuerBereichOf: organisationToEdit.zustaendigFuerBereichOf,
        organisationsArt: organisationToEdit.organisationsArt,
      });
    });

    it('should clear form when change to creator', () => {
      data$.next({ isCreator: true });

      expect(component.form.getRawValue()).toEqual({
        name: null,
        zustaendigFuerBereichOf: [],
        organisationsArt: null,
        uebergeordneteOrganisation: '',
      });

      //übergeordnete Organisation ist disabled
      expect(component.form.value).toEqual({
        name: null,
        zustaendigFuerBereichOf: [],
        organisationsArt: null,
      });
    });

    describe('onSave', () => {
      it('should work when bearbeiten nicht erlaubt', () => {
        when(organisationVerwaltenService.save(anything())).thenResolve(organisationToEdit);
        const nichtBearbeitbareOrg = { ...defaultEditOrganisation, id: 346677, aktuellerBenutzerDarfBearbeiten: false };
        data$.next({
          isCreator: false,
          organisation: nichtBearbeitbareOrg,
        });
        currentIdSpy.and.returnValue(nichtBearbeitbareOrg.id);

        component.form.markAsDirty();

        component.onSave();

        verify(organisationVerwaltenService.save(anything())).once();
        const command: SaveOrganisationCommand = {
          id: nichtBearbeitbareOrg.id,
          organisationsArt: nichtBearbeitbareOrg.organisationsArt,
          name: nichtBearbeitbareOrg.name,
          zustaendigFuerBereichOf: nichtBearbeitbareOrg.zustaendigFuerBereichOf.map(o => o.id),
          version: nichtBearbeitbareOrg.version,
        };
        expect(capture(organisationVerwaltenService.save).last()[0]).toEqual(command);
      });
      it('should read form', fakeAsync(() => {
        when(organisationVerwaltenService.save(anything())).thenResolve(organisationToEdit);
        const id1 = 64;
        const id2 = 45657;
        component.form.patchValue({
          name: 'Umbenannt!',
          zustaendigFuerBereichOf: [
            { ...defaultOrganisation, id: id1 },
            { ...defaultOrganisation, id: id2 },
          ],
          organisationsArt: OrganisationsArt.EXTERNER_DIENSTLEISTER,
        });
        component.form.markAsDirty();
        component.onSave();
        tick();

        verify(organisationVerwaltenService.save(anything())).once();
        const command: SaveOrganisationCommand = {
          id: organisationToEdit.id,
          organisationsArt: OrganisationsArt.EXTERNER_DIENSTLEISTER,
          name: 'Umbenannt!',
          zustaendigFuerBereichOf: [id1, id2],
          version: organisationToEdit.version,
        };
        expect(capture(organisationVerwaltenService.save).last()[0]).toEqual(command);
        expect(component.form.dirty).toBeFalse();
        expect(component.form.getRawValue()).toEqual({
          name: organisationToEdit.name,
          zustaendigFuerBereichOf: organisationToEdit.zustaendigFuerBereichOf,
          organisationsArt: organisationToEdit.organisationsArt,
          uebergeordneteOrganisation: Verwaltungseinheit.getDisplayName(organisationToEdit.uebergeordneteOrganisation),
        });
      }));

      it('should not save when pristine', () => {
        component.form.markAsPristine();
        component.onSave();
        verify(organisationVerwaltenService.save(anything())).never();
        expect().nothing();
      });

      it('should not save when invalid', () => {
        component.form.markAsDirty();
        spyOnProperty(component.form, 'valid').and.returnValue(false);
        component.onSave();
        verify(organisationVerwaltenService.save(anything())).never();
        expect().nothing();
      });

      it('should trim name onSave', () => {
        when(organisationVerwaltenService.save(anything())).thenResolve({ ...organisationToEdit, version: 2 });

        component.form.patchValue({
          name: '   test   ',
          organisationsArt: organisationToEdit.organisationsArt,
        });
        component.form.markAsDirty();

        component.onSave();

        fixture.detectChanges();
        expect(capture(organisationVerwaltenService.save).first()[0].name).toEqual('test');
      });

      it('should have correct version on subsequent save', fakeAsync(() => {
        when(organisationVerwaltenService.save(anything())).thenResolve({
          ...organisationToEdit,
          version: organisationToEdit.version + 1,
        });
        component.form.markAsDirty();

        component.onSave();
        tick();

        component.form.markAsDirty();
        component.onSave();

        verify(organisationVerwaltenService.save(anything())).times(2);
        expect(capture(organisationVerwaltenService.save).last()[0].version).toBe(organisationToEdit.version + 1);
      }));
    });
  });

  it('should disable uebergeordnete Organisation', () => {
    expect(component.form.get('uebergeordneteOrganisation')?.disabled).toEqual(true);
  });

  describe('validation', () => {
    it('should be invalid if fields not set', () => {
      component.form.reset();
      expect(component.form.get('name')?.valid).toBeFalse();
      expect(component.form.get('organisationsArt')?.valid).toBeFalse();
    });

    it('should be invalid if uebergeordneteOrganisation not set', () => {
      component.form.reset();
      expect(component.form.get('uebergeordneteOrganisation')?.valid).toBeFalse();
    });

    it('should be invalid when name too long', () => {
      component.form.patchValue({
        name: 'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata ',
      });
      expect(component.form.get('name')?.valid).toBeFalse();
    });
  });

  describe('asCreator', () => {
    beforeEach(() => {
      data$.next({ isCreator: true });
    });

    describe('onCreate', () => {
      it('should be enabled', () => {
        expect(component.form.value).toEqual({
          name: null,
          zustaendigFuerBereichOf: [],
          organisationsArt: null,
        });
      });

      it('should work without Zuständigkeiten-Input', () => {
        when(organisationVerwaltenService.create(anything())).thenResolve(defaultEditOrganisation);
        component.form.patchValue({
          name: 'Umbenannt!',
          organisationsArt: OrganisationsArt.EXTERNER_DIENSTLEISTER,
        });
        component.form.markAsDirty();
        component.onSave();

        verify(organisationVerwaltenService.create(anything())).once();
        const command: CreateOrganisationCommand = {
          organisationsArt: OrganisationsArt.EXTERNER_DIENSTLEISTER,
          name: 'Umbenannt!',
          zustaendigFuerBereichOf: [],
        };
        expect(capture(organisationVerwaltenService.create).last()[0]).toEqual(command);
      });

      it('should read form', fakeAsync(() => {
        when(organisationVerwaltenService.create(anything())).thenResolve(defaultEditOrganisation);
        const id1 = 64;
        const id2 = 45657;
        component.form.patchValue({
          name: 'Umbenannt!',
          zustaendigFuerBereichOf: [
            { ...defaultOrganisation, id: id1 },
            { ...defaultOrganisation, id: id2 },
          ],
          organisationsArt: OrganisationsArt.EXTERNER_DIENSTLEISTER,
        });
        component.form.markAsDirty();
        component.onSave();
        tick();

        verify(organisationVerwaltenService.create(anything())).once();
        const command: CreateOrganisationCommand = {
          organisationsArt: OrganisationsArt.EXTERNER_DIENSTLEISTER,
          name: 'Umbenannt!',
          zustaendigFuerBereichOf: [id1, id2],
        };
        expect(capture(organisationVerwaltenService.create).last()[0]).toEqual(command);
        expect(component.form.dirty).toBeFalse();
        verify(administrationRoutingService.toOrganisationEditor(anything())).once();
        expect(capture(administrationRoutingService.toOrganisationEditor).last()[0]).toBe(defaultEditOrganisation.id);
      }));
    });
  });

  describe(OrganisationEditorComponent.prototype.onZurueck.name, () => {
    beforeEach(() => {
      component.onZurueck();
    });

    it('should invoke toOrganisationListe', () => {
      verify(administrationRoutingService.toOrganisationListe()).once();
      expect().nothing();
    });
  });

  describe('on de-/activate', () => {
    beforeEach(() => {
      spyOnProperty(component, 'currentId').and.returnValue(defaultOrganisation.id);
      data$.next({ organisation: defaultEditOrganisation, isCreator: false });
    });

    it('should invoke toggle aendereOrganisationAktiv', fakeAsync(() => {
      // arrange
      component.organisationAktiv = true;

      when(organisationVerwaltenService.aendereOrganisationAktiv(anything(), false)).thenResolve({
        ...defaultEditOrganisation,
        aktiv: false,
      });
      when(organisationVerwaltenService.aendereOrganisationAktiv(anything(), true)).thenResolve({
        ...defaultEditOrganisation,
        aktiv: true,
      });

      // act
      component.onToggleAktiv();
      tick();
      component.onToggleAktiv();
      tick();
      component.onToggleAktiv();
      tick();

      // assert
      verify(organisationVerwaltenService.aendereOrganisationAktiv(defaultOrganisation.id, false)).twice();
      verify(organisationVerwaltenService.aendereOrganisationAktiv(defaultOrganisation.id, true)).once();
      expect().nothing();
    }));
  });
});
