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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { MatTableDataSource } from '@angular/material/table';
import {
  MassnahmenImportZuordnung,
  MassnahmenImportZuordnungStatus,
} from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung';

export interface MassnahmenAttributFehlerUeberpruefenRow {
  status: string;
  id: string;
  attribut: string;
  hinweis: string;
  first: boolean;
  rowspan: number;
}

@Component({
  selector: 'rad-import-massnahmen-attributfehler-ueberpruefen',
  templateUrl: './import-massnahmen-attributfehler-ueberpruefen.component.html',
  styleUrl: './import-massnahmen-attributfehler-ueberpruefen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportMassnahmenAttributfehlerUeberpruefenComponent implements OnInit {
  private static readonly STEP = 3;

  MassnahmenImportZuordnungStatus = MassnahmenImportZuordnungStatus;
  dataSource: MatTableDataSource<MassnahmenAttributFehlerUeberpruefenRow> = new MatTableDataSource();
  displayedColumns = ['status', 'id', 'attribut', 'hinweis'];
  loading = false;

  constructor(
    private massnahmenImportService: MassnahmenImportService,
    private massnahmenImportRoutingService: MassnahmenImportRoutingService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  onAbort(): void {
    this.massnahmenImportService.deleteImportSession().subscribe(() => {
      this.massnahmenImportRoutingService.navigateToFirst();
    });
  }

  onPrevious(): void {
    this.massnahmenImportRoutingService.navigateToPrevious(ImportMassnahmenAttributfehlerUeberpruefenComponent.STEP);
  }

  onNext(): void {
    this.massnahmenImportRoutingService.navigateToNext(ImportMassnahmenAttributfehlerUeberpruefenComponent.STEP);
  }

  private convertZuordnungen(zuordnungen: MassnahmenImportZuordnung[]): MassnahmenAttributFehlerUeberpruefenRow[] {
    return zuordnungen
      .filter(zuordnung => zuordnung.fehler.length > 0)
      .reduce((current: MassnahmenAttributFehlerUeberpruefenRow[], next) => {
        current.push(
          ...next.fehler.map((fehler, index) => {
            return {
              status: next.status,
              id: next.id,
              attribut: fehler.attributName,
              hinweis: fehler.text,
              first: index == 0,
              rowspan: next.fehler.length,
            };
          })
        );

        return current;
      }, []);
  }

  ngOnInit(): void {
    this.loading = true;
    this.changeDetectorRef.detectChanges();
    this.massnahmenImportService.getZuordnungen().subscribe({
      next: zuordnungen => {
        if (zuordnungen) {
          this.dataSource = new MatTableDataSource<MassnahmenAttributFehlerUeberpruefenRow>(
            this.convertZuordnungen(zuordnungen)
          );
        }
      },
      complete: () => {
        this.loading = false;
        this.changeDetectorRef.detectChanges();
      },
    });
  }
}
