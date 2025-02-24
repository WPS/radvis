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
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { ErweiterterMassnahmenFilter } from 'src/app/viewer/massnahme/models/erweiterter-massnahmen-filter';
import { FahrradrouteFilter } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter';

@Component({
  selector: 'rad-erweiterter-massnahmen-filter-dialog',
  templateUrl: './erweiterter-massnahmen-filter-dialog.component.html',
  styleUrls: ['./erweiterter-massnahmen-filter-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ErweiterterMassnahmenFilterDialogComponent {
  formGroup = new FormGroup({
    historischeMassnahmenAusblenden: new FormControl<boolean>(true, { nonNullable: true }),
    fahrradrouteFilter: new FormControl<FahrradrouteFilter | null>(null),
    organisation: new FormControl<Verwaltungseinheit | null>(null),
  });
  alleNonBundeslandOrganisationenOptions$: Promise<Verwaltungseinheit[]>;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ErweiterterMassnahmenFilter,
    private matDialogRef: MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>,
    organisationenService: OrganisationenService
  ) {
    this.formGroup.reset({ ...data, historischeMassnahmenAusblenden: !data.historischeMassnahmenAnzeigen });

    this.alleNonBundeslandOrganisationenOptions$ = organisationenService
      .getOrganisationen()
      .then(organisationen =>
        organisationen.filter(organisation => !Verwaltungseinheit.isLandesOderBundesweit(organisation))
      );
  }

  onSave(): void {
    const { historischeMassnahmenAusblenden, fahrradrouteFilter, organisation } = this.formGroup.value;

    this.matDialogRef.close({
      historischeMassnahmenAnzeigen: !historischeMassnahmenAusblenden,
      fahrradrouteFilter: fahrradrouteFilter ?? null,
      organisation,
    });
  }
}
