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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { ErweiterterMassnahmenFilter } from 'src/app/viewer/massnahme/models/erweiterter-massnahmen-filter';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/massnahme/models/fahrradroute-filter-kategorie';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { FahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view';
import { FahrradroutenProviderService } from 'src/app/viewer/viewer-shared/services/fahrradrouten-provider.service';

@Component({
  selector: 'rad-erweiterter-massnahmen-filter-dialog',
  templateUrl: './erweiterter-massnahmen-filter-dialog.component.html',
  styleUrls: ['./erweiterter-massnahmen-filter-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErweiterterMassnahmenFilterDialogComponent {
  formGroup = new FormGroup({
    historischeMassnahmenAusblenden: new FormControl<boolean>(true, { nonNullable: true }),
    fahrradrouteFilterKategorie: new FormControl<FahrradrouteFilterKategorie | null>(null),
    fahrradroute: new FormControl<FahrradrouteListenView | null>(null, RadvisValidators.isNotNullOrEmpty),
    organisation: new FormControl<Verwaltungseinheit | null>(null),
  });
  massnahmenTabelleFahrradrouteFilterKategorieOptions = FahrradrouteFilterKategorie.options;
  alleFahrradrouten: FahrradrouteListenView[] = [];
  alleNonBundeslandOrganisationenOptions$: Promise<Verwaltungseinheit[]>;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ErweiterterMassnahmenFilter,
    private matDialogRef: MatDialogRef<ErweiterterMassnahmenFilterDialogComponent>,
    organisationenService: OrganisationenService,
    fahrradrouteProviderService: FahrradroutenProviderService,
    changeDetector: ChangeDetectorRef
  ) {
    this.formGroup.reset({ ...data, historischeMassnahmenAusblenden: !data.historischeMassnahmenAnzeigen });

    const isFahrradrouteEnabled =
      data.fahrradrouteFilterKategorie === FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE;
    if (!isFahrradrouteEnabled) {
      this.formGroup.controls.fahrradroute.disable();
    }
    this.formGroup.controls.fahrradrouteFilterKategorie.valueChanges.subscribe(newValue =>
      this.onFahrradrouteFilterKategorieChanged(newValue)
    );

    fahrradrouteProviderService.getAll().then(fahrradrouten => {
      this.alleFahrradrouten = fahrradrouten;
      changeDetector.markForCheck();
    });

    this.alleNonBundeslandOrganisationenOptions$ = organisationenService
      .getOrganisationen()
      .then(organisationen =>
        organisationen.filter(organisation => !Verwaltungseinheit.isLandesOderBundesweit(organisation))
      );
  }

  private onFahrradrouteFilterKategorieChanged(newValue: FahrradrouteFilterKategorie | null): void {
    const isFahrradrouteEnabled = newValue === FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE;

    const fahrradrouteFormControl = this.formGroup.controls.fahrradroute;
    fahrradrouteFormControl.reset();

    if (isFahrradrouteEnabled) {
      fahrradrouteFormControl.enable();
    } else {
      fahrradrouteFormControl.disable();
    }
  }

  onSave(): void {
    const { historischeMassnahmenAusblenden, fahrradrouteFilterKategorie, fahrradroute, organisation } =
      this.formGroup.value;
    let matchingFahrradrouten: FahrradrouteListenView[] = [];

    switch (fahrradrouteFilterKategorie) {
      case FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE:
        matchingFahrradrouten = [fahrradroute!];
        break;
      case FahrradrouteFilterKategorie.ALLE_LRFW:
        matchingFahrradrouten = this.alleFahrradrouten.filter(
          f => f.fahrradrouteKategorie === FahrradrouteKategorie.LANDESRADFERNWEG
        );
        break;
      case FahrradrouteFilterKategorie.ALLE_DROUTEN:
        matchingFahrradrouten = this.alleFahrradrouten.filter(
          f => f.fahrradrouteKategorie === FahrradrouteKategorie.D_ROUTE
        );
        break;
      case FahrradrouteFilterKategorie.ALLE_FAHRRADROUTEN:
        matchingFahrradrouten = this.alleFahrradrouten;
        break;
    }

    this.matDialogRef.close({
      historischeMassnahmenAnzeigen: !historischeMassnahmenAusblenden,
      fahrradrouteFilterKategorie,
      fahrradroute,
      fahrradroutenIds: matchingFahrradrouten.map(item => item.id),
      organisation,
    } as ErweiterterMassnahmenFilter);
  }
}
