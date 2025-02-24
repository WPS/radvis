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
import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ErweiterterAnpassungswunschFilter } from 'src/app/viewer/anpassungswunsch/models/erweiterter-anpassungswunsch-filter';
import { FahrradrouteFilter } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter';

@Component({
  selector: 'rad-erweiterter-anpassungswunsch-filter-dialog',
  templateUrl: './erweiterter-anpassungswunsch-filter-dialog.component.html',
  styleUrl: './erweiterter-anpassungswunsch-filter-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ErweiterterAnpassungswunschFilterDialogComponent {
  formGroup = new FormGroup({
    abgeschlosseneAusblenden: new FormControl<boolean>(true, { nonNullable: true }),
    fahrradrouteFilter: new FormControl<FahrradrouteFilter | null>(null),
  });

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ErweiterterAnpassungswunschFilter,
    private matDialogRef: MatDialogRef<
      ErweiterterAnpassungswunschFilterDialogComponent,
      ErweiterterAnpassungswunschFilter
    >
  ) {
    this.formGroup.reset({ ...data });
  }

  onFilter(): void {
    const { fahrradrouteFilter, abgeschlosseneAusblenden } = this.formGroup.value;

    this.matDialogRef.close({
      abgeschlosseneAusblenden: abgeschlosseneAusblenden ?? false,
      fahrradrouteFilter: fahrradrouteFilter ?? null,
    });
  }
}
