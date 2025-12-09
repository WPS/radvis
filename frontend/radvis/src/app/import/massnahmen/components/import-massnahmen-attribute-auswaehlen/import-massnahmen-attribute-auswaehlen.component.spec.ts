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
import { of, Subject } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { MassnahmenImportAttribute } from 'src/app/import/massnahmen/models/massnahmen-import-attribute';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { ImportMassnahmenAttributeAuswaehlenComponent } from './import-massnahmen-attribute-auswaehlen.component';

describe(ImportMassnahmenAttributeAuswaehlenComponent.name, () => {
  let component: ImportMassnahmenAttributeAuswaehlenComponent;
  let fixture: MockedComponentFixture<ImportMassnahmenAttributeAuswaehlenComponent>;

  let massnahmenImportService: MassnahmenImportService;
  let massnahmenImportRoutingService: MassnahmenImportRoutingService;
  let manualRoutingService: ManualRoutingService;

  let sessionSubject: Subject<MassnahmenImportSessionView | null>;

  beforeEach(() => {
    massnahmenImportService = mock(MassnahmenImportService);
    massnahmenImportRoutingService = mock(MassnahmenImportRoutingService);
    manualRoutingService = mock(ManualRoutingService);

    return MockBuilder(ImportMassnahmenAttributeAuswaehlenComponent, ImportModule)
      .provide({ provide: MassnahmenImportService, useValue: instance(massnahmenImportService) })
      .provide({ provide: MassnahmenImportRoutingService, useValue: instance(massnahmenImportRoutingService) })
      .provide({ provide: ManualRoutingService, useValue: instance(manualRoutingService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(mock(ErrorHandlingService)) });
  });

  beforeEach(() => {
    sessionSubject = new Subject();
    when(massnahmenImportService.getImportSession()).thenReturn(sessionSubject.asObservable());
    when(massnahmenImportService.attributeAuswaehlen(anything())).thenReturn(of(undefined));

    fixture = MockRender(ImportMassnahmenAttributeAuswaehlenComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('Schritt noch nicht abgeschlossen', () => {
    let session: MassnahmenImportSessionView;
    beforeEach(done => {
      sessionSubject.subscribe(() => done());
      session = {
        log: [],
        schritt: 2,
        executing: false,
        gebietskoerperschaften: [1, 2],
        konzeptionsquelle: Konzeptionsquelle.KREISKONZEPT,
        sollStandard: SollStandard.BASISSTANDARD,
        attribute: [],
      };
      sessionSubject.next(session);
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have empty form', () => {
      expect(component.formGroup.value.pflichtAttribute.every((val: boolean) => !val)).toBeTrue();
      expect(component.formGroup.value.optionaleAttribute.every((val: boolean) => !val)).toBeTrue();
      expect(component.anyAttributSelected).toBeFalse();
      expect(component.schrittAbgeschlossen).toBeFalse();
    });

    it('should mark anyAttributSelected when a box has been checked', () => {
      component.formControlMap.get(MassnahmenImportAttribute.BEZEICHNUNG)?.setValue(true);
      component.formControlMap.get(MassnahmenImportAttribute.PRIORITAET)?.setValue(true);
      expect(component.anyAttributSelected).toBeTrue();
    });

    it('should build command correctly', () => {
      const attributesToCheck = [
        MassnahmenImportAttribute.UMSETZUNGSSTATUS,
        MassnahmenImportAttribute.BEZEICHNUNG,
        MassnahmenImportAttribute.PRIORITAET,
        MassnahmenImportAttribute.UNTERHALTSZUSTAENDIGER,
      ];
      attributesToCheck.forEach(attribut => component.formControlMap.get(attribut)?.setValue(true));

      component.onStart();

      expect(component.formGroup.disabled).toBeTrue();
      expect(capture(massnahmenImportService.attributeAuswaehlen).last()[0].attribute).toEqual(attributesToCheck);
      verify(massnahmenImportRoutingService.navigateToNext(anything())).never();
    });

    it('should uncheck all boxes on uncheck all', () => {
      const attributesToCheck = [
        MassnahmenImportAttribute.UMSETZUNGSSTATUS,
        MassnahmenImportAttribute.BEZEICHNUNG,
        MassnahmenImportAttribute.PRIORITAET,
        MassnahmenImportAttribute.UNTERHALTSZUSTAENDIGER,
      ];
      attributesToCheck.forEach(attribut => component.formControlMap.get(attribut)?.setValue(true));
      expect(component.anyAttributSelected).toBeTrue();

      component.onAlleMassnahmenAbwaehlen();

      expect(component.formGroup.value.pflichtAttribute.every((val: boolean) => !val)).toBeTrue();
      expect(component.formGroup.value.optionaleAttribute.every((val: boolean) => !val)).toBeTrue();
      expect(component.anyAttributSelected).toBeFalse();
    });

    it('should check all boxes on check all', () => {
      expect(component.anyAttributSelected).toBeFalse();

      component.onAlleMassnahmenAuswaehlen();

      expect(component.formGroup.value.pflichtAttribute.every((val: boolean) => val)).toBeTrue();
      expect(component.formGroup.value.optionaleAttribute.every((val: boolean) => val)).toBeTrue();
      expect(component.anyAttributSelected).toBeTrue();
    });

    it('should route to first step on abort', () => {
      when(massnahmenImportService.deleteImportSession()).thenReturn(of(undefined));

      component.onAbort();

      verify(massnahmenImportRoutingService.navigateToFirst()).once();
      expect().nothing();
    });

    it('should route to previous step on previous', () => {
      component.onPrevious();

      verify(massnahmenImportRoutingService.navigateToPrevious(2)).once();
      expect().nothing();
    });

    it('should route to manual on openManual', () => {
      component.openHandbuch();

      verify(manualRoutingService.openManualPflichtattribute()).once();
      expect().nothing();
    });
  });

  describe('Schritt bereits abgeschlossen', () => {
    let session: MassnahmenImportSessionView;
    beforeEach(done => {
      sessionSubject.subscribe(() => done());
      session = {
        log: [],
        schritt: 3,
        executing: false,
        gebietskoerperschaften: [1, 2],
        konzeptionsquelle: Konzeptionsquelle.KREISKONZEPT,
        sollStandard: SollStandard.BASISSTANDARD,
        attribute: [
          MassnahmenImportAttribute.ZUSTAENDIGER,
          MassnahmenImportAttribute.BEZEICHNUNG,
          MassnahmenImportAttribute.PRIORITAET,
          MassnahmenImportAttribute.REALISIERUNGSHILFE,
        ],
      };
      sessionSubject.next(session);
    });

    it('should have filled form', () => {
      expect(component.formGroup.value.pflichtAttribute.filter((val: boolean) => val).length).toBe(2);
      expect(component.formGroup.value.pflichtAttribute).toEqual([
        false,
        true,
        false,
        true,
        false,
        false,
        false,
        false,
      ]);
      expect(component.formGroup.value.optionaleAttribute.filter((val: boolean) => val).length).toBe(2);
      expect(component.formGroup.value.optionaleAttribute).toEqual([
        true,
        false,
        false,
        false,
        false,
        false,
        true,
        false,
        false,
        false,
        false,
        false,
        false,
      ]);
      expect(component.schrittAbgeschlossen).toBeTrue();
    });

    it('should not call service on next', () => {
      component.onNext();

      verify(massnahmenImportService.attributeAuswaehlen(anything())).never();
      verify(massnahmenImportRoutingService.navigateToNext(2)).once();
      expect().nothing();
    });
  });
});
