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

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { AnpassungswunschModule } from 'src/app/viewer/anpassungswunsch/anpassungswunsch.module';
import { ErweiterterAnpassungswunschFilter } from 'src/app/viewer/anpassungswunsch/models/erweiterter-anpassungswunsch-filter';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { testFahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view-test-data-provider.spec';
import { anything, capture, instance, mock, verify } from 'ts-mockito';
import { ErweiterterAnpassungswunschFilterDialogComponent } from './erweiterter-anpassungswunsch-filter-dialog.component';

describe('ErweiterterAnpassungswunschFilterDialogComponent', () => {
  let component: ErweiterterAnpassungswunschFilterDialogComponent;
  let fixture: MockedComponentFixture<ErweiterterAnpassungswunschFilterDialogComponent>;

  let defaultFilter: ErweiterterAnpassungswunschFilter;
  let matDialogRef: MatDialogRef<ErweiterterAnpassungswunschFilterDialogComponent, ErweiterterAnpassungswunschFilter>;

  beforeEach(() => {
    defaultFilter = {
      abgeschlosseneAusblenden: true,
      fahrradrouteFilter: {
        fahrradroute: testFahrradrouteListenView[0],
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE,
        fahrradroutenIds: [testFahrradrouteListenView[0].id],
      },
    };
    matDialogRef = mock(MatDialogRef<ErweiterterAnpassungswunschFilterDialogComponent>);
    return MockBuilder(ErweiterterAnpassungswunschFilterDialogComponent, AnpassungswunschModule)
      .provide({
        provide: MatDialogRef<ErweiterterAnpassungswunschFilterDialogComponent>,
        useValue: instance(matDialogRef),
      })
      .provide({
        provide: MAT_DIALOG_DATA,
        useValue: defaultFilter,
      });
  });

  beforeEach(() => {
    fixture = MockRender(ErweiterterAnpassungswunschFilterDialogComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should prefill form controls with current filter values', () => {
    expect(component.formGroup.value.abgeschlosseneAusblenden).toEqual(defaultFilter.abgeschlosseneAusblenden);
    expect(component.formGroup.value.fahrradrouteFilter).toEqual(defaultFilter.fahrradrouteFilter);
  });

  describe('onSave', () => {
    beforeEach(() => {
      component.formGroup.reset();
    });

    it('should return historischeMassnahmenAnzeigen', () => {
      component.formGroup.patchValue({
        abgeschlosseneAusblenden: false,
      });

      component.onFilter();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        abgeschlosseneAusblenden: false,
        fahrradrouteFilter: null,
      });
    });

    it('should return fahrradrouteFilter', () => {
      const expectedValue = {
        fahrradroute: null,
        fahrradroutenIds: [1],
        fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_LRFW,
      };
      component.formGroup.patchValue({
        fahrradrouteFilter: expectedValue,
      });

      component.onFilter();

      verify(matDialogRef.close(anything())).once();
      expect(capture(matDialogRef.close).last()[0]).toEqual({
        abgeschlosseneAusblenden: true,
        fahrradrouteFilter: expectedValue,
      });
    });
  });
});
