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
import { fakeAsync } from '@angular/core/testing';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { FahrradroutenCreatorComponent } from 'src/app/viewer/fahrradroute/components/fahrradrouten-creator/fahrradrouten-creator.component';
import { CreateFahrradrouteCommand } from 'src/app/viewer/fahrradroute/models/create-fahrradroute-command';
import { defaultFahrradrouteNetzbezug } from 'src/app/viewer/fahrradroute/models/fahrradroute-netzbezug-test-data-provider.spec';
import { FahrradrouteProfilService } from 'src/app/viewer/fahrradroute/services/fahrradroute-profil.service';
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, deepEqual, instance, mock, verify, when } from 'ts-mockito';

describe(FahrradroutenCreatorComponent.name, () => {
  let component: FahrradroutenCreatorComponent;
  let fixture: MockedComponentFixture<FahrradroutenCreatorComponent>;

  let viewerRoutingService: ViewerRoutingService;
  let organisationenService: OrganisationenService;
  let fahrradrouteService: FahrradrouteService;
  let changeDetectorRef: ChangeDetectorRef;
  let notifyUserService: NotifyUserService;
  let errorHandlingService: ErrorHandlingService;
  let fahrradrouteProfilService: FahrradrouteProfilService;

  beforeEach(() => {
    viewerRoutingService = mock(ViewerRoutingService);
    organisationenService = mock(OrganisationenService);
    fahrradrouteService = mock(FahrradrouteService);
    changeDetectorRef = mock(ChangeDetectorRef);
    notifyUserService = mock(NotifyUserService);
    errorHandlingService = mock(ErrorHandlingService);
    fahrradrouteProfilService = mock(FahrradrouteProfilService);

    return MockBuilder(FahrradroutenCreatorComponent, ViewerModule)
      .provide({
        provide: ViewerRoutingService,
        useValue: instance(viewerRoutingService),
      })
      .provide({
        provide: OrganisationenService,
        useValue: instance(organisationenService),
      })
      .provide({
        provide: FahrradrouteService,
        useValue: instance(fahrradrouteService),
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
      })
      .provide({
        provide: FahrradrouteProfilService,
        useValue: instance(fahrradrouteProfilService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(FahrradroutenCreatorComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('update fahrradrouteprofil', () => {
    it('should update Profil and trigger showProfile if geometry changes', fakeAsync(() => {
      const values = {
        netzbezug: defaultFahrradrouteNetzbezug,
      };

      component.formGroup.patchValue(values);
      component.formGroup.markAsDirty();

      verify(
        fahrradrouteProfilService.updateCurrentRouteProfil(
          deepEqual({
            geometrie: defaultFahrradrouteNetzbezug.geometrie,
            name: '',
            profilEigenschaften: defaultFahrradrouteNetzbezug.profilEigenschaften,
          })
        )
      ).once();

      expect().nothing();
    }));

    it('should update Profile if name changes and geometry exists', fakeAsync(() => {
      const values = {
        netzbezug: defaultFahrradrouteNetzbezug,
      };

      component.formGroup.patchValue(values, { emitEvent: false });
      component.formGroup.patchValue({ name: 'neuer Name' });
      component.formGroup.markAsDirty();

      verify(
        fahrradrouteProfilService.updateCurrentRouteProfil(
          deepEqual({
            geometrie: defaultFahrradrouteNetzbezug.geometrie,
            name: 'neuer Name',
            profilEigenschaften: defaultFahrradrouteNetzbezug.profilEigenschaften,
          })
        )
      ).once();
      verify(fahrradrouteProfilService.showCurrentRouteProfile()).never();
      expect().nothing();
    }));

    it('should update Profile with only name if name changes and no geometry exists', fakeAsync(() => {
      component.formGroup.patchValue({ name: 'neuer Name' });
      component.formGroup.markAsDirty();

      verify(
        fahrradrouteProfilService.updateCurrentRouteProfil(
          deepEqual({
            geometrie: undefined,
            name: 'neuer Name',
            profilEigenschaften: undefined,
          })
        )
      ).once();
      verify(fahrradrouteProfilService.showCurrentRouteProfile()).never();
      expect().nothing();
    }));
    describe(FahrradroutenCreatorComponent.prototype.ngOnDestroy.name, () => {
      it('should trigger closing of profile', fakeAsync(() => {
        component.ngOnDestroy();

        verify(fahrradrouteProfilService.hideCurrentRouteProfile()).once();
        expect().nothing();
      }));
    });
  });

  describe(FahrradroutenCreatorComponent.prototype.onSave.name, () => {
    beforeEach(() => {
      when(fahrradrouteService.createFahrradroute(anything())).thenResolve(1);
    });

    it('should build command from form correct', fakeAsync(() => {
      const values = {
        name: 'name',
        kategorie: FahrradrouteKategorie.RADSCHNELLWEG,
        beschreibung: 'beschreibung',
        netzbezug: {
          kantenIDs: [1],
          stuetzpunkte: [
            [0, 0],
            [0, 10],
          ],
          geometrie: {
            coordinates: [
              [0, 0],
              [0, 10],
            ],
            type: 'LineString',
          },
          profilEigenschaften: [],
          customProfileId: 123,
        },
      };

      component.formGroup.patchValue(values);
      component.formGroup.markAsDirty();
      component.onSave();

      verify(fahrradrouteService.createFahrradroute(anything())).once();
      const command = capture(fahrradrouteService.createFahrradroute).last()[0];
      expect(command).toEqual({
        name: 'name',
        kategorie: FahrradrouteKategorie.RADSCHNELLWEG,
        beschreibung: 'beschreibung',
        kantenIDs: [1],
        stuetzpunkte: {
          coordinates: [
            [0, 0],
            [0, 10],
          ],
          type: 'LineString',
        },
        routenVerlauf: {
          coordinates: [
            [0, 0],
            [0, 10],
          ],
          type: 'LineString',
        },
        profilEigenschaften: [],
        customProfileId: 123,
      } as CreateFahrradrouteCommand);
    }));
  });

  describe(FahrradroutenCreatorComponent.prototype.onReset.name, () => {
    beforeEach(() => {
      const values = {
        name: 'name',
        kategorie: FahrradrouteKategorie.RADSCHNELLWEG,
        beschreibung: 'beschreibung',
        netzbezug: {
          kantenIDs: [1],
          stuetzpunkte: [
            [0, 0],
            [0, 10],
          ],
          geometrie: {
            coordinates: [
              [0, 0],
              [0, 10],
            ],
            type: 'LineString',
          },
        },
      };
      component.formGroup.patchValue(values);

      component.onReset();
    });

    it('should reset correctly', () => {
      expect(component.formGroup.value).toEqual({
        name: null,
        beschreibung: null,
        kategorie: null,
        netzbezug: null,
      });
    });
  });
});
