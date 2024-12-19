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

import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MockBuilder, MockRender, ngMocks } from 'ng-mocks';
import { MassnahmeModule } from 'src/app/viewer/massnahme/massnahme.module';
import { UmsetzungsstandAbfrageVorschau } from 'src/app/viewer/massnahme/models/umsetzungsstand-abfrage-vorschau';
import { UmsetzungsstandAbfrageVorschauDialogComponent } from './umsetzungsstand-abfrage-vorschau-dialog.component';

describe(UmsetzungsstandAbfrageVorschauDialogComponent.name, () => {
  let vorschau: UmsetzungsstandAbfrageVorschau;

  beforeEach(() => {
    vorschau = { emailVorschau: '', empfaenger: [] };
    return MockBuilder(UmsetzungsstandAbfrageVorschauDialogComponent, MassnahmeModule).provide({
      provide: MAT_DIALOG_DATA,
      useValue: vorschau,
    });
  });

  it('should have no weitere empfaenger if = ANZAHL_VISIBLE', () => {
    for (let i = 0; i < UmsetzungsstandAbfrageVorschauDialogComponent['ANZAHL_VISIBLE_EMPFAENGER']; i++) {
      vorschau.empfaenger.push('test');
    }

    const fixture = MockRender(UmsetzungsstandAbfrageVorschauDialogComponent);
    expect(fixture.point.componentInstance.weitereEmpfaenger.length).toBe(0);
    expect(ngMocks.findAll('.weitere-anzeigen').length).toBe(0);
  });

  it('should shorten visible empfänger if anzahl < ANZAHL_VISIBLE', () => {
    for (let i = 1; i < UmsetzungsstandAbfrageVorschauDialogComponent['ANZAHL_VISIBLE_EMPFAENGER']; i++) {
      vorschau.empfaenger.push('test');
    }

    const fixture = MockRender(UmsetzungsstandAbfrageVorschauDialogComponent);
    expect(fixture.point.componentInstance.visibleEmpfaenger.length).toBe(
      UmsetzungsstandAbfrageVorschauDialogComponent['ANZAHL_VISIBLE_EMPFAENGER'] - 1
    );
  });

  it('should have weitere Empfaenger if > ANZAHL_VISIBLE', () => {
    const anzahlWeitere = 3;

    for (
      let i = 0;
      i < UmsetzungsstandAbfrageVorschauDialogComponent['ANZAHL_VISIBLE_EMPFAENGER'] + anzahlWeitere;
      i++
    ) {
      vorschau.empfaenger.push('test');
    }

    const fixture = MockRender(UmsetzungsstandAbfrageVorschauDialogComponent);
    expect(fixture.point.componentInstance.weitereEmpfaenger.length).toBe(anzahlWeitere);
    expect(ngMocks.find('.weitere-anzeigen')).toBeTruthy();
  });

  it('should sort Empfaenger alphabetically', () => {
    const anzahlWeitere = 1;

    const sortedEmpfaenger: string[] = [];
    for (
      let i = 0;
      i < UmsetzungsstandAbfrageVorschauDialogComponent['ANZAHL_VISIBLE_EMPFAENGER'] + anzahlWeitere;
      i++
    ) {
      sortedEmpfaenger.push('test' + i);
    }

    vorschau.empfaenger = [...sortedEmpfaenger];
    vorschau.empfaenger.reverse();

    expect(vorschau.empfaenger).not.toEqual(sortedEmpfaenger);

    const fixture = MockRender(UmsetzungsstandAbfrageVorschauDialogComponent);
    expect(fixture.point.componentInstance.visibleEmpfaenger).toEqual(
      sortedEmpfaenger.slice(0, UmsetzungsstandAbfrageVorschauDialogComponent['ANZAHL_VISIBLE_EMPFAENGER'])
    );
    expect(fixture.point.componentInstance.weitereEmpfaenger).toEqual(
      sortedEmpfaenger.slice(UmsetzungsstandAbfrageVorschauDialogComponent['ANZAHL_VISIBLE_EMPFAENGER'])
    );
  });
});
