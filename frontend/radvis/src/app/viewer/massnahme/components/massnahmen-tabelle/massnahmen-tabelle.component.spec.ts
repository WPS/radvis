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

import { waitForAsync } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { ErweiterterMassnahmenFilterDialogComponent } from 'src/app/viewer/massnahme/components/erweiterter-massnahmen-filter-dialog/erweiterter-massnahmen-filter-dialog.component';
import { MassnahmenTabelleComponent } from 'src/app/viewer/massnahme/components/massnahmen-tabelle/massnahmen-tabelle.component';
import { ErweiterterMassnahmenFilter } from 'src/app/viewer/massnahme/models/erweiterter-massnahmen-filter';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { testFahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view-test-data-provider.spec';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { ExportService } from 'src/app/viewer/viewer-shared/services/export.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(MassnahmenTabelleComponent.name, () => {
  let component: MassnahmenTabelleComponent;
  let fixture: MockedComponentFixture<MassnahmenTabelleComponent>;

  let massnahmeFilterService: MassnahmeFilterService;
  let dialog: MatDialog;
  let exportService: ExportService;

  let filterSubject: Subject<FieldFilter[]>;

  beforeEach(() => {
    massnahmeFilterService = mock(MassnahmeFilterService);
    dialog = mock(MatDialog);
    exportService = mock(ExportService);

    when(massnahmeFilterService.erweiterterFilterAktiv$).thenReturn(of(false));

    filterSubject = new Subject();
    when(massnahmeFilterService.filter$).thenReturn(filterSubject.asObservable());

    return MockBuilder(MassnahmenTabelleComponent, ViewerModule)
      .provide({ provide: ExportService, useValue: instance(exportService) })
      .provide({ provide: MassnahmeFilterService, useValue: instance(massnahmeFilterService) })
      .provide({ provide: MatDialog, useValue: instance(dialog) });
  });

  beforeEach(waitForAsync(() => {
    fixture = MockRender(MassnahmenTabelleComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  }));

  describe('filteredSpalten', () => {
    it('should correspond to filteredFields', (done: DoneFn) => {
      component.filteredSpalten$.subscribe(filteredSpalten => {
        expect(filteredSpalten).toEqual(['test']);
        done();
      });

      filterSubject.next([new FieldFilter('test', 'filter')]);
    });
  });

  describe('onExport', () => {
    it('should respect spaltenauswahl', () => {
      when(massnahmeFilterService.currentFilteredList).thenReturn([]);
      when(exportService.exportInfrastruktur(anything(), anything(), anything(), anything())).thenResolve();

      component.onExport({
        felder: [
          component.spaltenDefinition[0].name,
          component.spaltenDefinition[4].name,
          component.spaltenDefinition[5].name,
        ],
        format: ExportFormat.CSV,
      });

      verify(exportService.exportInfrastruktur(anything(), anything(), anything(), anything())).once();
      expect(capture(exportService.exportInfrastruktur).last()).toEqual([
        'MASSNAHME',
        ExportFormat.CSV,
        [],
        component.spaltenDefinition
          .filter((v, index) => index !== 0 && index !== 4 && index !== 5)
          .map(def => def.displayName),
      ]);
    });
  });

  describe('onErweiterteFilterVerwalten', () => {
    it('should provide the dialog with the current filter settings and disable clicking outside', () => {
      // Arrange
      const erweiterterFilter: ErweiterterMassnahmenFilter = {
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilter: {
          fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
          fahrradroute: testFahrradrouteListenView[0],
          fahrradroutenIds: [],
        },
        organisation: null,
      };
      when(massnahmeFilterService.erweiterterFilter).thenReturn(erweiterterFilter);
      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => of(),
      } as MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>);

      // Act
      component.onErweiterteFilterVerwalten();

      // Assert
      verify(dialog.open(ErweiterterMassnahmenFilterDialogComponent, anything())).once();
      const dialogData = capture(dialog.open).last()[1];
      expect(dialogData?.disableClose).toBe(true);
      expect(dialogData?.data).toEqual(erweiterterFilter);
    });

    it('should update the filter when the dialog closes with a new filter', () => {
      // Arrange
      const neuerErweiterterFilter: ErweiterterMassnahmenFilter = {
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilter: {
          fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
          fahrradroute: null,
          fahrradroutenIds: [],
        },
        organisation: null,
      };
      const dialogRefSpy = jasmine.createSpyObj({ afterClosed: of(neuerErweiterterFilter) });
      when(dialog.open(anything(), anything())).thenReturn(dialogRefSpy);

      // Act
      component.onErweiterteFilterVerwalten();

      // Assert
      verify(massnahmeFilterService.updateErweiterterFilter(anything())).once();
      expect(capture(massnahmeFilterService.updateErweiterterFilter).last()[0]).toEqual(neuerErweiterterFilter);
    });

    it('should not update the filter when the dialog closes with false', () => {
      // Arrange
      const erweiterterFilter: ErweiterterMassnahmenFilter = {
        historischeMassnahmenAnzeigen: false,
        fahrradrouteFilter: null,
        organisation: null,
      };
      when(massnahmeFilterService.erweiterterFilter).thenReturn(erweiterterFilter);
      const dialogRefSpy = jasmine.createSpyObj({ afterClosed: of() });
      when(dialog.open(anything(), anything())).thenReturn(dialogRefSpy);

      // Act
      component.onErweiterteFilterVerwalten();

      // Assert
      verify(massnahmeFilterService.updateErweiterterFilter(anything())).never();
      expect().nothing();
    });
  });
});
