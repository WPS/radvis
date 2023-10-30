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
import { LeihstationEditorComponent } from 'src/app/viewer/leihstation/components/leihstation-editor/leihstation-editor.component';
import { LeihstationModule } from 'src/app/viewer/leihstation/leihstation.module';
import { Leihstation } from 'src/app/viewer/leihstation/models/leihstation';
import { LeihstationQuellSystem } from 'src/app/viewer/leihstation/models/leihstation-quell-system';
import { LeihstationStatus } from 'src/app/viewer/leihstation/models/leihstation-status';
import { defaultLeihstation } from 'src/app/viewer/leihstation/models/leihstation-testdata-provider.spec';
import { LeihstationRoutingService } from 'src/app/viewer/leihstation/services/leihstation-routing.service';
import { LeihstationService } from 'src/app/viewer/leihstation/services/leihstation.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(LeihstationEditorComponent.name, () => {
  let component: LeihstationEditorComponent;
  let fixture: MockedComponentFixture<LeihstationEditorComponent>;
  let data$: Subject<{ isCreator: boolean; leihstation?: Leihstation }>;
  let leihstationService: LeihstationService;
  let leihstationRoutingService: LeihstationRoutingService;

  beforeEach(() => {
    data$ = new Subject();
    leihstationService = mock(LeihstationService);
    leihstationRoutingService = mock(LeihstationRoutingService);
    return MockBuilder(LeihstationEditorComponent, LeihstationModule)
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
        provide: LeihstationRoutingService,
        useValue: instance(leihstationRoutingService),
      })
      .provide({
        provide: LeihstationService,
        useValue: instance(leihstationService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(LeihstationEditorComponent);
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
      expect(component.formGroup.get('quellSystem')?.value).toEqual(LeihstationQuellSystem.RADVIS);
      expect(component.formGroup.get('quellSystem')?.disabled).toBeTrue();
    });

    it('should reset form', fakeAsync(() => {
      data$.next({ isCreator: false, leihstation: defaultLeihstation });
      tick();
      component.formGroup.markAsDirty();
      expect(component.formGroup.value.betreiber).toBe(defaultLeihstation.betreiber);

      data$.next({ isCreator: true });
      tick();
      expect(component.formGroup.dirty).toBeFalse();
      expect(component.formGroup.getRawValue()).toEqual({
        geometrie: null,
        betreiber: null,
        freiesAbstellen: null,
        anzahlPedelecs: null,
        anzahlFahrraeder: null,
        anzahlAbstellmoeglichkeiten: null,
        buchungsUrl: null,
        status: null,
        quellSystem: LeihstationQuellSystem.RADVIS,
      });
    }));

    it('should doCreate correctly', fakeAsync(() => {
      when(leihstationService.create(anything())).thenResolve(1);
      tick();
      expect(component.formGroup.valid).toBeFalse();

      component.formGroup.patchValue({
        geometrie: [0, 1],
        betreiber: 'Hans',
        freiesAbstellen: false,
        anzahlPedelecs: 1,
        anzahlFahrraeder: 2,
        anzahlAbstellmoeglichkeiten: 3,
        buchungsUrl: 'https://someurl.com',
        status: LeihstationStatus.GEPLANT,
        quellSystem: LeihstationQuellSystem.RADVIS,
      });
      expect(component.formGroup.valid).toBeTrue();

      component.formGroup.markAsDirty();

      component.onSave();
      verify(leihstationService.create(anything())).once();
      expect(capture(leihstationService.create).last()[0]).toEqual({
        geometrie: { coordinates: [0, 1], type: 'Point' },
        freiesAbstellen: false,
        anzahlPedelecs: 1,
        anzahlFahrraeder: 2,
        anzahlAbstellmoeglichkeiten: 3,
        buchungsUrl: 'https://someurl.com',
        status: LeihstationStatus.GEPLANT,
        betreiber: 'Hans',
      });

      tick();

      verify(leihstationRoutingService.toInfrastrukturEditor(1)).once();
      expect(component.canDiscard()).toBeTrue();
    }));
  });

  describe('asEditor with quellsystem mobiData', () => {
    beforeEach(() => {
      data$.next({
        isCreator: false,
        leihstation: { ...defaultLeihstation, quellSystem: LeihstationQuellSystem.MOBIDATABW },
      });
    });

    it('should disable whole formGroup when quellsystem mobiData', () => {
      expect(component.formGroup.get('quellSystem')?.value).toEqual(LeihstationQuellSystem.MOBIDATABW);
      expect(component.formGroup.disabled).toBeTrue();
      expect(component.canEdit).toBeFalse();
    });
  });

  describe('asEditor with quellsystem radvis', () => {
    beforeEach(() => {
      data$.next({ isCreator: false, leihstation: defaultLeihstation });
    });

    it('should set quellsystem correctly and disable formfield', () => {
      expect(component.formGroup.get('quellSystem')?.value).toEqual(LeihstationQuellSystem.RADVIS);
      expect(component.formGroup.get('quellSystem')?.disabled).toBeTrue();
    });

    it('should reset form', fakeAsync(() => {
      tick();
      component.formGroup.patchValue({
        betreiber: 'Blubb',
        quellSystem: LeihstationQuellSystem.RADVIS,
      });
      component.formGroup.markAsDirty();

      data$.next({ isCreator: false, leihstation: defaultLeihstation });
      tick();

      expect(component.formGroup.dirty).toBeFalse();
      expect(component.formGroup.getRawValue()).toEqual({
        betreiber: defaultLeihstation.betreiber,
        geometrie: defaultLeihstation.geometrie.coordinates,
        anzahlFahrraeder: defaultLeihstation.anzahlFahrraeder,
        anzahlPedelecs: defaultLeihstation.anzahlPedelecs,
        anzahlAbstellmoeglichkeiten: defaultLeihstation.anzahlAbstellmoeglichkeiten,
        freiesAbstellen: defaultLeihstation.freiesAbstellen,
        buchungsUrl: defaultLeihstation.buchungsUrl,
        status: defaultLeihstation.status,
        quellSystem: defaultLeihstation.quellSystem,
      });
      expect(component.currentLeihstation).toEqual(defaultLeihstation);
    }));

    it('should doSave correctly', fakeAsync(() => {
      when(leihstationService.save(anything(), anything())).thenResolve(defaultLeihstation);
      tick();

      component.formGroup.patchValue({
        geometrie: [0, 1],
        betreiber: 'Hans',
        freiesAbstellen: false,
        anzahlPedelecs: 1,
        anzahlFahrraeder: 2,
        anzahlAbstellmoeglichkeiten: 3,
        buchungsUrl: 'https://someurl.com',
        status: LeihstationStatus.GEPLANT,
        quellSystem: LeihstationQuellSystem.RADVIS,
      });
      tick();

      component.formGroup.markAsDirty();

      component.onSave();
      tick();
      verify(leihstationService.save(anything(), anything())).once();
      expect(capture(leihstationService.save).last()[1]).toEqual({
        geometrie: { coordinates: [0, 1], type: 'Point' },
        betreiber: 'Hans',
        freiesAbstellen: false,
        anzahlPedelecs: 1,
        anzahlFahrraeder: 2,
        anzahlAbstellmoeglichkeiten: 3,
        buchungsUrl: 'https://someurl.com',
        status: LeihstationStatus.GEPLANT,
        version: defaultLeihstation.version,
      });
      expect(capture(leihstationService.save).last()[0]).toEqual(defaultLeihstation.id);
    }));
  });
});
