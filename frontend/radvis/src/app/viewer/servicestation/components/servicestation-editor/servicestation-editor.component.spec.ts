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
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { ServicestationEditorComponent } from 'src/app/viewer/servicestation/components/servicestation-editor/servicestation-editor.component';
import { Servicestation } from 'src/app/viewer/servicestation/models/servicestation';
import { defaultServicestation } from 'src/app/viewer/servicestation/models/servicestation-testdata-provider.spec';
import { ServicestationRoutingService } from 'src/app/viewer/servicestation/services/servicestation-routing.service';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import { ServicestationModule } from 'src/app/viewer/servicestation/servicestation.module';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { defaultGemeinden, defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { ServicestationTyp } from 'src/app/viewer/servicestation/models/servicestation-typ';
import { ServicestationStatus } from 'src/app/viewer/servicestation/models/servicestation-status';
import { ServicestationUpdatedService } from 'src/app/viewer/servicestation/services/servicestation-updated.service';
import { AbstellanlagenQuellSystem } from 'src/app/viewer/abstellanlage/models/abstellanlagen-quell-system';
import { ServicestationQuellSystem } from 'src/app/viewer/servicestation/models/servicestation-quell-system';

class TestServicestationUpdateService extends ServicestationUpdatedService {
  updateServicestation(): void {}
}

describe(ServicestationEditorComponent.name, () => {
  let component: ServicestationEditorComponent;
  let fixture: MockedComponentFixture<ServicestationEditorComponent>;
  let data$: Subject<{ isCreator: boolean; servicestation?: Servicestation }>;
  let servicestationService: ServicestationService;
  let servicestationUpdateService: ServicestationUpdatedService;
  let servicestationRoutingService: ServicestationRoutingService;
  let organisationenService: OrganisationenService;
  let benutzerDetailService: BenutzerDetailsService;

  beforeEach(() => {
    data$ = new Subject();
    servicestationService = mock(ServicestationService);
    servicestationUpdateService = mock(TestServicestationUpdateService);
    servicestationRoutingService = mock(ServicestationRoutingService);
    organisationenService = mock(OrganisationenService);
    benutzerDetailService = mock(BenutzerDetailsService);

    when(organisationenService.getOrganisationen()).thenResolve(defaultGemeinden);
    when(benutzerDetailService.aktuellerBenutzerOrganisation()).thenReturn(defaultOrganisation);

    return MockBuilder(ServicestationEditorComponent, ServicestationModule)
      .provide({
        provide: ActivatedRoute,
        useValue: {
          data: data$,
        },
      })
      .provide({
        provide: OlMapService,
        useValue: instance(mock(OlMapComponent)),
      })
      .provide({
        provide: InfrastrukturenSelektionService,
        useValue: instance(mock(InfrastrukturenSelektionService)),
      })
      .provide({
        provide: ServicestationRoutingService,
        useValue: instance(servicestationRoutingService),
      })
      .provide({
        provide: ServicestationService,
        useValue: instance(servicestationService),
      })
      .provide({
        provide: ServicestationUpdatedService,
        useValue: instance(servicestationUpdateService),
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
    fixture = MockRender(ServicestationEditorComponent);
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

    it('should disable quellSystem control', () => {
      expect(component.formGroup.get('quellSystem')?.disabled).toBeTrue();
    });

    it('should reset form', fakeAsync(() => {
      data$.next({
        isCreator: false,
        servicestation: { ...defaultServicestation, quellSystem: ServicestationQuellSystem.MOBIDATABW },
      });
      tick();
      component.formGroup.markAsDirty();
      expect(component.formGroup.value.name).toBe(defaultServicestation.name);
      expect(component.formGroup.get('quellSystem')?.value).toEqual(AbstellanlagenQuellSystem.MOBIDATABW);

      data$.next({ isCreator: true });
      tick();
      expect(component.formGroup.dirty).toBeFalse();
      expect(component.formGroup.value).toEqual({
        geometrie: null,
        name: null,
        gebuehren: null,
        oeffnungszeiten: null,
        betreiber: null,
        marke: null,
        luftpumpe: null,
        kettenwerkzeug: null,
        werkzeug: null,
        fahrradhalterung: null,
        beschreibung: null,
        organisation: defaultOrganisation,
        typ: null,
        status: null,
      });
      expect(component.formGroup.get('quellSystem')?.value).toEqual(AbstellanlagenQuellSystem.RADVIS);
    }));

    it('should doCreate correctly', fakeAsync(() => {
      when(servicestationService.create(anything())).thenResolve(1);
      tick();
      expect(component.formGroup.valid).toBeFalse();

      component.formGroup.patchValue({
        geometrie: [0, 1],
        name: 'Hans',
        gebuehren: true,
        oeffnungszeiten: 'MoDiMi',
        betreiber: 'betreiber',
        marke: 'marke',
        luftpumpe: false,
        kettenwerkzeug: null,
        werkzeug: true,
        fahrradhalterung: null,
        beschreibung: 'beschreibungstext',
        organisation: defaultGemeinden[0],
        typ: ServicestationTyp.RADSERVICE_PUNKT_GROSS,
        status: ServicestationStatus.AKTIV,
      });
      expect(component.formGroup.valid).toBeTrue();

      component.formGroup.markAsDirty();

      component.onSave();
      verify(servicestationService.create(anything())).once();
      expect(capture(servicestationService.create).last()[0]).toEqual({
        geometrie: { coordinates: [0, 1], type: 'Point' },
        name: 'Hans',
        gebuehren: true,
        oeffnungszeiten: 'MoDiMi',
        betreiber: 'betreiber',
        marke: 'marke',
        luftpumpe: false,
        kettenwerkzeug: false,
        werkzeug: true,
        fahrradhalterung: false,
        beschreibung: 'beschreibungstext',
        organisationId: defaultGemeinden[0].id,
        typ: ServicestationTyp.RADSERVICE_PUNKT_GROSS,
        status: ServicestationStatus.AKTIV,
      });

      tick();
      verify(servicestationRoutingService.toInfrastrukturEditor(1)).once();
      expect(component.canDiscard()).toBeTrue();
    }));
  });

  describe('asEditor', () => {
    beforeEach(() => {
      data$.next({ isCreator: false, servicestation: defaultServicestation });
    });

    it('should disable quellSystem control', () => {
      expect(component.formGroup.get('quellSystem')?.disabled).toBeTrue();
    });

    it('should reset form', fakeAsync(() => {
      tick();
      component.formGroup.patchValue({
        betreiber: 'Blubb',
      });
      component.formGroup.markAsDirty();

      data$.next({
        isCreator: false,
        servicestation: { ...defaultServicestation, quellSystem: ServicestationQuellSystem.MOBIDATABW },
      });
      tick();

      expect(component.formGroup.dirty).toBeFalse();
      expect(component.formGroup.value).toEqual({
        name: defaultServicestation.name,
        geometrie: defaultServicestation.geometrie.coordinates,
        gebuehren: defaultServicestation.gebuehren,
        oeffnungszeiten: defaultServicestation.oeffnungszeiten,
        betreiber: defaultServicestation.betreiber,
        marke: defaultServicestation.marke,
        luftpumpe: defaultServicestation.luftpumpe,
        kettenwerkzeug: defaultServicestation.kettenwerkzeug,
        werkzeug: defaultServicestation.werkzeug,
        fahrradhalterung: defaultServicestation.fahrradhalterung,
        beschreibung: defaultServicestation.beschreibung,
        organisation: defaultServicestation.organisation,
        typ: defaultServicestation.typ,
        status: defaultServicestation.status,
      });
      expect(component.formGroup.get('quellSystem')?.value).toEqual(AbstellanlagenQuellSystem.MOBIDATABW);
      expect(component.currentServicestation).toEqual({
        ...defaultServicestation,
        quellSystem: ServicestationQuellSystem.MOBIDATABW,
      });
    }));

    it('should doSave correctly', fakeAsync(() => {
      when(servicestationService.save(anything(), anything())).thenResolve(defaultServicestation);
      tick();

      component.formGroup.patchValue({
        geometrie: [0, 1],
        name: 'Hans',
        gebuehren: false,
        oeffnungszeiten: 'nie',
        betreiber: 'niemand',
        marke: 'hype',
        luftpumpe: false,
        kettenwerkzeug: true,
        werkzeug: null,
        fahrradhalterung: null,
        beschreibung: 'beschreibender Text',
        organisation: defaultGemeinden[1],
        typ: ServicestationTyp.RADSERVICE_PUNKT_KLEIN,
        status: ServicestationStatus.AUSSER_BETRIEB,
      });

      component.formGroup.markAsDirty();

      component.onSave();
      tick();
      verify(servicestationService.save(anything(), anything())).once();
      expect(capture(servicestationService.save).last()[1]).toEqual({
        geometrie: { coordinates: [0, 1], type: 'Point' },
        name: 'Hans',
        gebuehren: false,
        oeffnungszeiten: 'nie',
        betreiber: 'niemand',
        marke: 'hype',
        luftpumpe: false,
        kettenwerkzeug: true,
        werkzeug: false,
        fahrradhalterung: false,
        beschreibung: 'beschreibender Text',
        organisationId: defaultGemeinden[1].id,
        typ: ServicestationTyp.RADSERVICE_PUNKT_KLEIN,
        status: ServicestationStatus.AUSSER_BETRIEB,
        version: defaultServicestation.version,
      });
      expect(capture(servicestationService.save).last()[0]).toEqual(defaultServicestation.id);
      verify(servicestationUpdateService.updateServicestation()).once();
    }));
  });
});
