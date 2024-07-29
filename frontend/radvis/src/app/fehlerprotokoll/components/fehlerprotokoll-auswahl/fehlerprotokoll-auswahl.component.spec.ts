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

import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { of } from 'rxjs';
import { skip } from 'rxjs/operators';
import {
  DLM_REIMPORT_JOB_FAHRRADROUTEN_FEHLERPROTOKOLL,
  TFIS_IMPORT_FAHRRADROUTEN_FEHLERPROTOKOLL,
} from 'src/app/fehlerprotokoll/models/fehlerpotokoll-typ-test-data-provider.spec';
import { FehlerprotokollView } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-view';
import { FehlerprotokollSelectionService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll-selection.service';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { KonsistenzregelService } from 'src/app/fehlerprotokoll/services/konsistenzregel.service';
import { OrganisationenDropdownControlComponent } from 'src/app/shared/components/organisationen-dropdown-control/organisationen-dropdown-control.component';
import { PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { Konsistenzregel } from 'src/app/shared/models/konsistenzregel';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { MenuEventService } from 'src/app/shared/services/menu-event.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { anything, capture, deepEqual, instance, mock, verify, when } from 'ts-mockito';
import { FehlerprotokollAuswahlComponent } from './fehlerprotokoll-auswahl.component';

describe(FehlerprotokollAuswahlComponent.name, () => {
  let fixture: MockedComponentFixture<FehlerprotokollAuswahlComponent>;
  let component: FehlerprotokollAuswahlComponent;
  let selectionService: FehlerprotokollSelectionService;
  let fehlerprotokollService: FehlerprotokollService;
  let konsistenzregelService: KonsistenzregelService;
  let menuEventService: MenuEventService;
  let featureTogglzService: FeatureTogglzService;
  const radNETZVerletzungsTyp = 'RADNETZ_LUECKE';

  beforeEach(async () => {
    selectionService = new FehlerprotokollSelectionService();
    fehlerprotokollService = mock(FehlerprotokollService);
    when(fehlerprotokollService.getFehlerFromManuellerImport(anything(), anything(), anything())).thenReturn(of([]));
    when(fehlerprotokollService.getFehlerprotokolle(anything())).thenReturn(of([]));

    konsistenzregelService = mock(KonsistenzregelService);
    when(konsistenzregelService.getAllKonsistenzRegel()).thenReturn(
      of([
        {
          verletzungsTyp: radNETZVerletzungsTyp,
          regelGruppe: 'Datenprüfung',
          titel: 'RadNETZ-Lücke',
        } as Konsistenzregel,
      ])
    );
    when(konsistenzregelService.getAlleVerletzungenForTypen(anything())).thenReturn(of([]));

    menuEventService = mock(MenuEventService);
    when(menuEventService.menuClosed$).thenReturn(of());

    featureTogglzService = mock(FeatureTogglzService);
    when(featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_KONSISTENZREGELN)).thenReturn(true);

    return MockBuilder(FehlerprotokollAuswahlComponent, SharedModule)
      .keep(OrganisationenDropdownControlComponent)
      .provide({
        provide: FehlerprotokollSelectionService,
        useValue: selectionService,
      })
      .provide({
        provide: FehlerprotokollService,
        useValue: instance(fehlerprotokollService),
      })
      .provide({
        provide: KonsistenzregelService,
        useValue: instance(konsistenzregelService),
      })
      .provide({
        provide: FeatureTogglzService,
        useValue: instance(featureTogglzService),
      })
      .provide({
        provide: MenuEventService,
        useValue: instance(menuEventService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(FehlerprotokollAuswahlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('updateFehlerprotokollLoader', () => {
    it('should load and combine all three', (done: DoneFn) => {
      const fehlerprotokolle2: FehlerprotokollView[] = [{ ...defaultFehlerprotokoll, id: 7654 }];
      when(
        fehlerprotokollService.getFehlerFromManuellerImport(anything(), anything(), anything(), anything())
      ).thenReturn(of(fehlerprotokolle2));
      const fehlerprotokolle1: FehlerprotokollView[] = [{ ...defaultFehlerprotokoll, id: 8756 }];
      when(fehlerprotokollService.getFehlerprotokolle(anything(), anything())).thenReturn(of(fehlerprotokolle1));
      const fehlerprotokolle3: FehlerprotokollView[] = [{ ...defaultFehlerprotokoll, id: 1234 }];
      when(
        konsistenzregelService.getAlleVerletzungenForTypen(deepEqual([radNETZVerletzungsTyp]), anything())
      ).thenReturn(of(fehlerprotokolle3));

      selectionService.selectedOrganisation = defaultOrganisation;
      selectionService.netzklassenImportSelected = true;
      selectionService.selectKonsistenzregel({ verletzungsTyp: radNETZVerletzungsTyp, regelGruppe: '', titel: '' });
      selectionService.attributeImportSelected = false;
      selectionService.selectFehlerprotokoll(TFIS_IMPORT_FAHRRADROUTEN_FEHLERPROTOKOLL);

      selectionService.fehlerprotokollLoader$.pipe(skip(1)).subscribe(loader => {
        loader([0, 0, 0, 0]).subscribe(fP => {
          expect(fP).toEqual([...fehlerprotokolle1, ...fehlerprotokolle2, ...fehlerprotokolle3]);
          done();
        });
      });

      component.onFehlerprotokollClicked(DLM_REIMPORT_JOB_FAHRRADROUTEN_FEHLERPROTOKOLL);

      verify(fehlerprotokollService.getFehlerprotokolle(anything(), anything())).once();
      expect(capture(fehlerprotokollService.getFehlerprotokolle).last()[0]).toEqual([
        TFIS_IMPORT_FAHRRADROUTEN_FEHLERPROTOKOLL,
        DLM_REIMPORT_JOB_FAHRRADROUTEN_FEHLERPROTOKOLL,
      ]);
      verify(
        fehlerprotokollService.getFehlerFromManuellerImport(anything(), anything(), anything(), anything())
      ).once();
      expect(capture(fehlerprotokollService.getFehlerFromManuellerImport).last().slice(0, 3)).toEqual([
        defaultOrganisation,
        true,
        false,
      ]);
      verify(konsistenzregelService.getAlleVerletzungenForTypen(anything(), anything())).once();
      expect(capture(konsistenzregelService.getAlleVerletzungenForTypen).last()[0]).toEqual([radNETZVerletzungsTyp]);
    });

    it('should not load from Manueller Import, if no Organisation', () => {
      selectionService.selectedOrganisation = null;
      selectionService.netzklassenImportSelected = true;

      component.onFehlerprotokollClicked(DLM_REIMPORT_JOB_FAHRRADROUTEN_FEHLERPROTOKOLL);

      triggerLoader();
      verify(fehlerprotokollService.getFehlerprotokolle(anything(), anything())).once();
      expect(capture(fehlerprotokollService.getFehlerprotokolle).last()[0]).toEqual([
        DLM_REIMPORT_JOB_FAHRRADROUTEN_FEHLERPROTOKOLL,
      ]);
      verify(
        fehlerprotokollService.getFehlerFromManuellerImport(anything(), anything(), anything(), anything())
      ).never();
    });
  });

  describe('organisationControl', () => {
    it('should be invalid if no organisation', () => {
      component.organisationControl.setValue(null);
      expect(component.organisationControl.valid).toBeFalse();

      component.organisationControl.setValue(defaultOrganisation);
      expect(component.organisationControl.valid).toBeTrue();
    });

    it('should trigger reload', () => {
      const organisation = { ...defaultOrganisation, id: 876354 };
      component.organisationControl.setValue(organisation);

      triggerLoader();

      verify(
        fehlerprotokollService.getFehlerFromManuellerImport(anything(), anything(), anything(), anything())
      ).once();
      expect(capture(fehlerprotokollService.getFehlerFromManuellerImport).last()[0]).toEqual(organisation);
      expect(selectionService.selectedOrganisation).toEqual(organisation);
    });
  });

  describe('selected KonsistenzregelVerletzungen', () => {
    it('should toggle selection', () => {
      const regel = { regelGruppe: '', titel: '', verletzungsTyp: 'Verletzung' };
      component.onRegelClicked(regel);

      expect(selectionService.selectedKonsistenzregelVerletzungen).toEqual([regel.verletzungsTyp]);

      component.onRegelClicked(regel);

      expect(selectionService.selectedKonsistenzregelVerletzungen).toEqual([]);
    });

    it('should trigger reload on change', () => {
      selectionService.selectedOrganisation = defaultOrganisation;
      selectionService.attributeImportSelected = false;
      selectionService.netzklassenImportSelected = false;
      const fehlerprotokolle3: FehlerprotokollView[] = [{ ...defaultFehlerprotokoll, id: 1234 }];
      when(konsistenzregelService.getAlleVerletzungenForTypen(deepEqual([radNETZVerletzungsTyp]))).thenReturn(
        of(fehlerprotokolle3)
      );

      component.onRegelClicked({ verletzungsTyp: radNETZVerletzungsTyp } as Konsistenzregel);

      triggerLoader();

      expect(selectionService.selectedKonsistenzregelVerletzungen).toContain(radNETZVerletzungsTyp);

      verify(konsistenzregelService.getAlleVerletzungenForTypen(deepEqual([radNETZVerletzungsTyp]), anything())).once();
    });
  });

  describe('selected Manueller Import typ', () => {
    it('should trigger reload on change', () => {
      selectionService.selectedOrganisation = defaultOrganisation;
      selectionService.attributeImportSelected = false;
      selectionService.netzklassenImportSelected = false;

      component.netzklassenImportControl.setValue(true);

      triggerLoader();

      verify(
        fehlerprotokollService.getFehlerFromManuellerImport(anything(), anything(), anything(), anything())
      ).once();
      expect(capture(fehlerprotokollService.getFehlerFromManuellerImport).last().slice(0, 3)).toEqual([
        defaultOrganisation,
        true,
        false,
      ]);
      expect(selectionService.netzklassenImportSelected).toBeTrue();
      expect(selectionService.attributeImportSelected).toBeFalse();

      component.attributeImportControl.setValue(true);

      triggerLoader();

      verify(
        fehlerprotokollService.getFehlerFromManuellerImport(anything(), anything(), anything(), anything())
      ).twice();
      expect(capture(fehlerprotokollService.getFehlerFromManuellerImport).last().slice(0, 3)).toEqual([
        defaultOrganisation,
        true,
        true,
      ]);
      expect(selectionService.netzklassenImportSelected).toBeTrue();
      expect(selectionService.attributeImportSelected).toBeTrue();
    });

    it('should disable/enable when organisation valid/invalid', () => {
      component.organisationControl.setValue(null);

      triggerLoader();

      verify(
        fehlerprotokollService.getFehlerFromManuellerImport(anything(), anything(), anything(), anything())
      ).never();

      expect(component.netzklassenImportControl.disabled).toBeTrue();
      expect(component.attributeImportControl.disabled).toBeTrue();

      component.organisationControl.setValue(defaultOrganisation);

      triggerLoader();

      verify(
        fehlerprotokollService.getFehlerFromManuellerImport(anything(), anything(), anything(), anything())
      ).once();

      expect(component.netzklassenImportControl.disabled).toBeFalse();
      expect(component.attributeImportControl.disabled).toBeFalse();
    });
  });

  const triggerLoader = (): void => {
    selectionService.fehlerprotokollLoader$.value([0, 0, 0, 0]);
  };
});

const defaultFehlerprotokoll: FehlerprotokollView = {
  beschreibung: 'Beschreibung eines Fehlerprotokolls',
  datum: new Date().toISOString(),
  entityLink: '',
  fehlerprotokollKlasse: 'Blubb',
  iconPosition: { coordinates: [[0, 0]], type: 'Point' },
  id: 2,
  originalGeometry: { coordinates: [0, 0], type: 'Point' } as PointGeojson,
  titel: 'Titel eines Fehlerprotokolls',
};
