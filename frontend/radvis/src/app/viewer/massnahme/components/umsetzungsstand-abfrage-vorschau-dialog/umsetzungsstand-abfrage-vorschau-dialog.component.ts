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
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { UmsetzungsstandAbfrageVorschau } from 'src/app/viewer/massnahme/models/umsetzungsstand-abfrage-vorschau';

@Component({
  selector: 'rad-umsetzungsstand-abfrage-vorschau-dialog',
  templateUrl: './umsetzungsstand-abfrage-vorschau-dialog.component.html',
  styleUrl: './umsetzungsstand-abfrage-vorschau-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class UmsetzungsstandAbfrageVorschauDialogComponent {
  private static readonly ANZAHL_VISIBLE_EMPFAENGER = 8;
  visibleEmpfaenger: string[];
  weitereEmpfaenger: string[];

  constructor(@Inject(MAT_DIALOG_DATA) protected vorschau: UmsetzungsstandAbfrageVorschau) {
    const sortedBenutzer = [...vorschau.empfaenger].sort((a, b) => a.localeCompare(b));
    this.visibleEmpfaenger = sortedBenutzer.slice(
      0,
      UmsetzungsstandAbfrageVorschauDialogComponent.ANZAHL_VISIBLE_EMPFAENGER
    );
    this.weitereEmpfaenger = sortedBenutzer.slice(
      UmsetzungsstandAbfrageVorschauDialogComponent.ANZAHL_VISIBLE_EMPFAENGER
    );
  }
}
