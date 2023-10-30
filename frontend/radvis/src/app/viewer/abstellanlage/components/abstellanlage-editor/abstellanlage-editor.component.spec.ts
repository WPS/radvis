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

import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { Subject } from 'rxjs';
import { defaultGemeinden, defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { AbstellanlageModule } from 'src/app/viewer/abstellanlage/abstellanlage.module';
import { AbstellanlageEditorComponent } from 'src/app/viewer/abstellanlage/components/abstellanlage-editor/abstellanlage-editor.component';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { defaultAbstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage-testdata-provider.spec';
import { AbstellanlagenQuellSystem } from 'src/app/viewer/abstellanlage/models/abstellanlagen-quell-system';
import { AbstellanlagenStatus } from 'src/app/viewer/abstellanlage/models/abstellanlagen-status';
import { Groessenklasse } from 'src/app/viewer/abstellanlage/models/groessenklasse';
import { Stellplatzart } from 'src/app/viewer/abstellanlage/models/stellplatzart';
import { Ueberwacht } from 'src/app/viewer/abstellanlage/models/ueberwacht';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { AbstellanlageUpdatedService } from 'src/app/viewer/abstellanlage/services/abstellanlage-updated.service';
import { AbstellanlageService } from 'src/app/viewer/abstellanlage/services/abstellanlage.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

class TestAbstellanlageUpdateService extends AbstellanlageUpdatedService {
  updateAbstellanlage(): void {}
}

describe(AbstellanlageEditorComponent.name, () => {
  let component: AbstellanlageEditorComponent;
  let fixture: MockedComponentFixture<AbstellanlageEditorComponent>;
  let data$: Subject<{ isCreator: boolean; abstellanlage?: Abstellanlage }>;
  let abstellanlageService: AbstellanlageService;
  let abstellanlageUpdatedService: AbstellanlageUpdatedService;
  let abstellanlageRoutingService: AbstellanlageRoutingService;
  let organisationenService: OrganisationenService;
  let benutzerDetailService: BenutzerDetailsService;

  beforeEach(() => {
    data$ = new Subject();
    abstellanlageService = mock(AbstellanlageService);
    abstellanlageUpdatedService = mock(TestAbstellanlageUpdateService);
    abstellanlageRoutingService = mock(AbstellanlageRoutingService);

    organisationenService = mock(OrganisationenService);
    benutzerDetailService = mock(BenutzerDetailsService);

    when(organisationenService.getOrganisationen()).thenResolve(defaultGemeinden);
    when(benutzerDetailService.aktuellerBenutzerOrganisation()).thenReturn(defaultOrganisation);

    return MockBuilder(AbstellanlageEditorComponent, AbstellanlageModule)
      .provide({
        provide: ActivatedRoute,
        useValue: {
          data: data$,
        },
      })
      .provide({
        provide: AbstellanlageRoutingService,
        useValue: instance(abstellanlageRoutingService),
      })
      .provide({
        provide: AbstellanlageService,
        useValue: instance(abstellanlageService),
      })
      .provide({
        provide: AbstellanlageUpdatedService,
        useValue: instance(abstellanlageUpdatedService),
      })
      .provide({
        provide: OrganisationenService,
        useValue: instance(organisationenService),
      })
      .provide({
        provide: BenutzerDetailsService,
        useValue: instance(benutzerDetailService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(AbstellanlageEditorComponent);
    fixture.detectChanges();
    component = fixture.point.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('asCreator', () => {
    beforeEach(() => {
      data$.next({ isCreator: true });
    });

    it('should set quellsystem to Radvis and disable formField', () => {
      expect(component.formGroup.get('quellSystem')?.value).toEqual(AbstellanlagenQuellSystem.RADVIS);
      expect(component.formGroup.get('quellSystem')?.disabled).toBeTrue();
    });

    it('should enable groessenklasse only if B+R is selected', fakeAsync(() => {
      tick();
      expect(component.formGroup.get('istBikeAndRide')?.value).toBeNull();
      expect(component.formGroup.get('groessenklasse')?.disabled).toBeTrue();
      component.formGroup.patchValue({ istBikeAndRide: true });
      tick();
      expect(component.formGroup.get('istBikeAndRide')?.value).toBeTrue();
      expect(component.formGroup.get('groessenklasse')?.disabled).toBeFalse();
    }));

    it('should reset form', fakeAsync(() => {
      data$.next({ isCreator: false, abstellanlage: defaultAbstellanlage });
      tick();
      component.formGroup.markAsDirty();
      expect(component.formGroup.value.betreiber).toBe(defaultAbstellanlage.betreiber);

      data$.next({ isCreator: true });
      tick();
      expect(component.formGroup.dirty).toBeFalse();
      expect(component.formGroup.getRawValue()).toEqual({
        geometrie: null,
        betreiber: null,
        externeId: null,
        quellSystem: AbstellanlagenQuellSystem.RADVIS,
        zustaendig: defaultOrganisation,
        anzahlStellplaetze: null,
        anzahlSchliessfaecher: null,
        anzahlLademoeglichkeiten: null,
        ueberwacht: null,
        istBikeAndRide: null,
        groessenklasse: null,
        stellplatzart: null,
        ueberdacht: null,
        gebuehrenProTag: null,
        gebuehrenProMonat: null,
        gebuehrenProJahr: null,
        beschreibung: null,
        weitereInformation: null,
        status: null,
      });
    }));

    it('should doCreate correctly', fakeAsync(() => {
      when(abstellanlageService.create(anything())).thenResolve(1);
      tick();
      expect(component.formGroup.valid).toBeFalse();

      component.formGroup.patchValue({
        geometrie: [0, 1],
        betreiber: 'Betreiber',
        externeId: 'externe_id',
        quellSystem: AbstellanlagenQuellSystem.RADVIS,
        zustaendig: defaultGemeinden[0],
        anzahlStellplaetze: 1,
        anzahlSchliessfaecher: 2,
        anzahlLademoeglichkeiten: 3,
        ueberwacht: Ueberwacht.VIDEO,
        istBikeAndRide: true,
        groessenklasse: Groessenklasse.BASISANGEBOT_XS,
        stellplatzart: Stellplatzart.ANLEHNBUEGEL,
        ueberdacht: false,
        gebuehrenProTag: 400,
        gebuehrenProMonat: 500,
        gebuehrenProJahr: 600,
        beschreibung: 'beschreibung neu',
        weitereInformation: 'weitere Info neu',
        status: AbstellanlagenStatus.AUSSER_BETRIEB,
      });
      expect(component.formGroup.valid).toBeTrue();

      component.formGroup.markAsDirty();

      component.onSave();
      verify(abstellanlageService.create(anything())).once();
      expect(capture(abstellanlageService.create).last()[0]).toEqual({
        geometrie: { coordinates: [0, 1], type: 'Point' },
        betreiber: 'Betreiber',
        externeId: 'externe_id',
        zustaendigId: defaultGemeinden[0].id,
        anzahlStellplaetze: 1,
        anzahlSchliessfaecher: 2,
        anzahlLademoeglichkeiten: 3,
        ueberwacht: Ueberwacht.VIDEO,
        istBikeAndRide: true,
        groessenklasse: Groessenklasse.BASISANGEBOT_XS,
        stellplatzart: Stellplatzart.ANLEHNBUEGEL,
        ueberdacht: false,
        gebuehrenProTag: 400,
        gebuehrenProMonat: 500,
        gebuehrenProJahr: 600,
        beschreibung: 'beschreibung neu',
        weitereInformation: 'weitere Info neu',
        status: AbstellanlagenStatus.AUSSER_BETRIEB,
      });

      tick();
      verify(abstellanlageRoutingService.toInfrastrukturEditor(1)).once();
      expect(component.canDiscard()).toBeTrue();
    }));

    it('should fill command with correct default values', fakeAsync(() => {
      when(abstellanlageService.create(anything())).thenResolve(1);
      tick();

      expect(component.formGroup.valid).toBeFalse();
      component.formGroup.patchValue({
        betreiber: 'Betreiber',
        geometrie: [0, 1],
        quellSystem: AbstellanlagenQuellSystem.RADVIS,
        anzahlStellplaetze: 1,
        anzahlSchliessfaecher: 2,
        ueberwacht: Ueberwacht.VIDEO,
        stellplatzart: Stellplatzart.ANLEHNBUEGEL,
        status: AbstellanlagenStatus.AUSSER_BETRIEB,
        zustaendig: undefined,
      });
      expect(component.formGroup.valid).toBeTrue();

      component.formGroup.markAsDirty();

      component.onSave();
      verify(abstellanlageService.create(anything())).once();
      expect(capture(abstellanlageService.create).last()[0]).toEqual({
        geometrie: { coordinates: [0, 1], type: 'Point' },
        betreiber: 'Betreiber',
        externeId: null,
        zustaendigId: null,
        anzahlStellplaetze: 1,
        anzahlSchliessfaecher: 2,
        anzahlLademoeglichkeiten: null,
        ueberwacht: Ueberwacht.VIDEO,
        istBikeAndRide: false,
        groessenklasse: null,
        stellplatzart: Stellplatzart.ANLEHNBUEGEL,
        ueberdacht: false,
        gebuehrenProTag: null,
        gebuehrenProMonat: null,
        gebuehrenProJahr: null,
        beschreibung: null,
        weitereInformation: null,
        status: AbstellanlagenStatus.AUSSER_BETRIEB,
      });

      tick();
      verify(abstellanlageRoutingService.toInfrastrukturEditor(1)).once();
      expect(component.canDiscard()).toBeTrue();
    }));
  });

  describe('asEditor with quellsystem mobiData', () => {
    beforeEach(() => {
      data$.next({
        isCreator: false,
        abstellanlage: { ...defaultAbstellanlage, quellSystem: AbstellanlagenQuellSystem.MOBIDATABW },
      });
    });

    it('should disable whole formGroup when quellsystem mobiData', () => {
      expect(component.formGroup.get('quellSystem')?.value).toEqual(AbstellanlagenQuellSystem.MOBIDATABW);
      expect(component.formGroup.disabled).toBeTrue();
      expect(component.canEdit).toBeFalse();
    });
  });

  describe('asEditor with quellsystem radvis', () => {
    beforeEach(() => {
      data$.next({ isCreator: false, abstellanlage: defaultAbstellanlage });
    });

    it('should set quellsystem correctly and disable formfield', () => {
      expect(component.formGroup.get('quellSystem')?.value).toEqual(AbstellanlagenQuellSystem.RADVIS);
      expect(component.formGroup.get('quellSystem')?.disabled).toBeTrue();
    });

    it('should enable groessenklasse only if B+R is selected', fakeAsync(() => {
      tick();
      expect(component.formGroup.get('istBikeAndRide')?.value).toBeFalse();
      expect(component.formGroup.get('groessenklasse')?.disabled).toBeTrue();
      data$.next({ isCreator: false, abstellanlage: { ...defaultAbstellanlage, istBikeAndRide: true } });
      tick();
      expect(component.formGroup.get('istBikeAndRide')?.value).toBeTrue();
      expect(component.formGroup.get('groessenklasse')?.disabled).toBeFalse();
    }));

    it('should reset form', fakeAsync(() => {
      tick();
      component.formGroup.patchValue({
        betreiber: 'Blubb',
        externeId: 'externe_id',
        quellSystem: AbstellanlagenQuellSystem.RADVIS,
        zustaendig: defaultGemeinden[1],
        anzahlStellplaetze: 1,
        anzahlSchliessfaecher: 2,
        anzahlLademoeglichkeiten: 3,
        ueberwacht: Ueberwacht.VIDEO,
        istBikeAndRide: true,
        groessenklasse: Groessenklasse.BASISANGEBOT_XS,
        stellplatzart: Stellplatzart.ANLEHNBUEGEL,
        ueberdacht: false,
        gebuehrenProTag: 400,
        gebuehrenProMonat: 500,
        gebuehrenProJahr: 600,
        beschreibung: 'beschreibung neu',
        weitereInformation: 'weitere Info neu',
        status: AbstellanlagenStatus.AUSSER_BETRIEB,
      });
      component.formGroup.markAsDirty();

      data$.next({ isCreator: false, abstellanlage: defaultAbstellanlage });
      tick();

      expect(component.formGroup.dirty).toBeFalse();
      expect(component.formGroup.getRawValue()).toEqual({
        betreiber: defaultAbstellanlage.betreiber,
        geometrie: defaultAbstellanlage.geometrie.coordinates,
        externeId: defaultAbstellanlage.externeId,
        quellSystem: defaultAbstellanlage.quellSystem,
        zustaendig: defaultAbstellanlage.zustaendig,
        anzahlStellplaetze: defaultAbstellanlage.anzahlStellplaetze,
        anzahlSchliessfaecher: defaultAbstellanlage.anzahlSchliessfaecher,
        anzahlLademoeglichkeiten: defaultAbstellanlage.anzahlLademoeglichkeiten,
        ueberwacht: defaultAbstellanlage.ueberwacht,
        istBikeAndRide: defaultAbstellanlage.istBikeAndRide,
        groessenklasse: null,
        stellplatzart: defaultAbstellanlage.stellplatzart,
        ueberdacht: defaultAbstellanlage.ueberdacht,
        gebuehrenProTag: defaultAbstellanlage.gebuehrenProTag,
        gebuehrenProMonat: defaultAbstellanlage.gebuehrenProMonat,
        gebuehrenProJahr: defaultAbstellanlage.gebuehrenProJahr,
        beschreibung: defaultAbstellanlage.beschreibung,
        weitereInformation: defaultAbstellanlage.weitereInformation,
        status: defaultAbstellanlage.status,
      });
      expect(component.currentAbstellanlage).toEqual(defaultAbstellanlage);
    }));

    it('should doSave correctly', fakeAsync(() => {
      when(abstellanlageService.save(anything(), anything())).thenResolve(defaultAbstellanlage);
      tick();

      component.formGroup.patchValue({
        geometrie: [0, 1],
        betreiber: 'Betreiber',
        externeId: 'externe_id',
        quellSystem: AbstellanlagenQuellSystem.RADVIS,
        zustaendig: defaultGemeinden[1],
        anzahlStellplaetze: 1,
        anzahlSchliessfaecher: 2,
        anzahlLademoeglichkeiten: 3,
        ueberwacht: Ueberwacht.VIDEO,
        istBikeAndRide: true,
        groessenklasse: Groessenklasse.BASISANGEBOT_XS,
        stellplatzart: Stellplatzart.ANLEHNBUEGEL,
        ueberdacht: false,
        gebuehrenProTag: 420,
        gebuehrenProMonat: 500,
        gebuehrenProJahr: 600,
        beschreibung: 'beschreibung geändert',
        weitereInformation: 'weitere Info geändert',
        status: AbstellanlagenStatus.AUSSER_BETRIEB,
      });
      tick();

      component.formGroup.markAsDirty();

      component.onSave();
      tick();
      verify(abstellanlageService.save(anything(), anything())).once();
      expect(capture(abstellanlageService.save).last()[1]).toEqual({
        geometrie: { coordinates: [0, 1], type: 'Point' },
        betreiber: 'Betreiber',
        externeId: 'externe_id',
        zustaendigId: defaultGemeinden[1].id,
        anzahlStellplaetze: 1,
        anzahlSchliessfaecher: 2,
        anzahlLademoeglichkeiten: 3,
        ueberwacht: Ueberwacht.VIDEO,
        istBikeAndRide: true,
        groessenklasse: Groessenklasse.BASISANGEBOT_XS,
        stellplatzart: Stellplatzart.ANLEHNBUEGEL,
        ueberdacht: false,
        gebuehrenProTag: 420,
        gebuehrenProMonat: 500,
        gebuehrenProJahr: 600,
        beschreibung: 'beschreibung geändert',
        weitereInformation: 'weitere Info geändert',
        status: AbstellanlagenStatus.AUSSER_BETRIEB,
        version: defaultAbstellanlage.version,
      });
      expect(capture(abstellanlageService.save).last()[0]).toEqual(defaultAbstellanlage.id);
      verify(abstellanlageUpdatedService.updateAbstellanlage()).once();
    }));
  });
});
