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
import { fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { NEVER, of } from 'rxjs';
import { AnpassungswunschModule } from 'src/app/viewer/anpassungswunsch/anpassungswunsch.module';
import { ErweiterterAnpassungswunschFilterDialogComponent } from 'src/app/viewer/anpassungswunsch/components/erweiterter-anpassungswunsch-filter-dialog/erweiterter-anpassungswunsch-filter-dialog.component';
import { ErweiterterAnpassungswunschFilter } from 'src/app/viewer/anpassungswunsch/models/erweiterter-anpassungswunsch-filter';
import { AnpassungswunschFilterService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-filter.service';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { testFahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view-test-data-provider.spec';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { AnpassungswunschTabelleComponent } from './anpassungswunsch-tabelle.component';

describe(AnpassungswunschTabelleComponent.name, () => {
  let component: AnpassungswunschTabelleComponent;
  let fixture: MockedComponentFixture<AnpassungswunschTabelleComponent>;

  let filterService: AnpassungswunschFilterService;
  let dialog: MatDialog;

  beforeEach(() => {
    filterService = mock(AnpassungswunschFilterService);
    dialog = mock(MatDialog);

    when(filterService.erweiterterFilterActive$).thenReturn(of(false));
    when(filterService.filter$).thenReturn(NEVER);

    return MockBuilder(AnpassungswunschTabelleComponent, AnpassungswunschModule)
      .provide({ provide: AnpassungswunschFilterService, useValue: instance(filterService) })
      .provide({ provide: MatDialog, useValue: instance(dialog) });
  });

  beforeEach(waitForAsync(() => {
    fixture = MockRender(AnpassungswunschTabelleComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  }));

  describe('onErweiterteFilterVerwalten', () => {
    it('should provide the dialog with the current filter settings and disable clicking outside', () => {
      // Arrange
      const erweiterterFilter: ErweiterterAnpassungswunschFilter = {
        abgeschlosseneAusblenden: false,
        fahrradrouteFilter: {
          fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
          fahrradroute: testFahrradrouteListenView[0],
          fahrradroutenIds: [],
        },
      };
      when(filterService.erweiterterFilter).thenReturn(erweiterterFilter);
      when(dialog.open(anything(), anything())).thenReturn({
        afterClosed: () => of(),
      } as MatDialogRef<ErweiterterAnpassungswunschFilterDialogComponent>);

      // Act
      component.onOpenErweiterterFilter();

      // Assert
      verify(dialog.open(ErweiterterAnpassungswunschFilterDialogComponent, anything())).once();
      const dialogData = capture(dialog.open).last()[1];
      expect(dialogData?.disableClose).toBe(true);
      expect(dialogData?.data).toEqual(erweiterterFilter);
    });

    it('should update the filter when the dialog closes with a new filter', fakeAsync(() => {
      // Arrange
      const neuerErweiterterFilter: ErweiterterAnpassungswunschFilter = {
        abgeschlosseneAusblenden: false,
        fahrradrouteFilter: {
          fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
          fahrradroute: null,
          fahrradroutenIds: [],
        },
      };
      const dialogRefSpy = jasmine.createSpyObj({ afterClosed: of(neuerErweiterterFilter) });
      when(dialog.open(anything(), anything())).thenReturn(dialogRefSpy);

      // Act
      component.onOpenErweiterterFilter();
      tick();

      // Assert
      verify(filterService.updateErweiterterFilter(anything())).once();
      expect(capture(filterService.updateErweiterterFilter).last()[0]).toEqual(neuerErweiterterFilter);
    }));

    it('should not update the filter when the dialog closes with false', fakeAsync(() => {
      // Arrange
      const erweiterterFilter: ErweiterterAnpassungswunschFilter = {
        abgeschlosseneAusblenden: false,
        fahrradrouteFilter: null,
      };
      when(filterService.erweiterterFilter).thenReturn(erweiterterFilter);
      const dialogRefSpy = jasmine.createSpyObj({ afterClosed: of() });
      when(dialog.open(anything(), anything())).thenReturn(dialogRefSpy);

      // Act
      component.onOpenErweiterterFilter();
      tick();

      // Assert
      verify(filterService.updateErweiterterFilter(anything())).never();
      expect().nothing();
    }));
  });
});
