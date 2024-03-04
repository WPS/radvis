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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, ViewContainerRef } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { AbstellanlageFilterService } from 'src/app/viewer/abstellanlage/services/abstellanlage-filter.service';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { AbstellanlageService } from 'src/app/viewer/abstellanlage/services/abstellanlage.service';
import { CsvImportDialogComponent } from 'src/app/viewer/viewer-shared/components/csv-import-dialog/csv-import-dialog.component';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { CsvImportService } from 'src/app/viewer/viewer-shared/services/csv-import.service';
import { ExportService } from 'src/app/viewer/viewer-shared/services/export.service';

@Component({
  selector: 'rad-abstellanlage-tabelle',
  templateUrl: './abstellanlage-tabelle.component.html',
  styleUrls: ['./abstellanlage-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: AbstractInfrastrukturenFilterService,
      useExisting: AbstellanlageFilterService,
    },
    { provide: CsvImportService, useExisting: forwardRef(() => AbstellanlageTabelleComponent) },
  ],
})
export class AbstellanlageTabelleComponent implements CsvImportService {
  public csvImportFeatureToggl = false;

  data$: Observable<Abstellanlage[]>;
  displayedColumns: string[];
  selectedID$: Observable<number | null>;
  exporting = false;
  exportFormate = [ExportFormat.CSV];
  isSmallViewport = false;

  private columnMapping: Map<string, string> = new Map([
    ['betreiber', 'Betreiber'],
    ['externeId', 'Externe ID'],
    ['quellSystem', 'Quellsystem'],
    ['zustaendig', 'Zuständig in RadVIS'],
    ['anzahlStellplaetze', 'Anzahl Stellplätze'],
    ['abstellanlagenOrt', 'Ort'],
    ['groessenklasse', 'Größenklasse'],
    ['stellplatzart', 'Stellplatzart'],
    ['status', 'Status'],
  ]);

  constructor(
    public dialog: MatDialog,
    public filterService: AbstellanlageFilterService,
    private routingService: AbstellanlageRoutingService,
    private exportService: ExportService,
    private changeDetector: ChangeDetectorRef,
    private abstellanlageService: AbstellanlageService,
    private viewContainerRef: ViewContainerRef,
    private manualRoutingService: ManualRoutingService,
    featureTogglzService: FeatureTogglzService
  ) {
    this.data$ = this.filterService.filteredList$;
    this.selectedID$ = this.routingService.selectedInfrastrukturId$;
    this.displayedColumns = Array.from(this.columnMapping.keys());
    this.csvImportFeatureToggl = featureTogglzService.isToggledOn(
      FeatureTogglzService.TOGGLZ_ABSTELLANLAGEN_CSV_IMPORT
    );
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  uploadCsv(file: File): Promise<Blob> {
    const formData = new FormData();
    formData.append('file', file);
    return this.abstellanlageService.uploadCsv(formData);
  }

  afterUpload(): void {
    this.filterService.refetchData();
  }

  openManual(): void {
    this.manualRoutingService.openManualAbstellanlageImport();
  }

  getHeader(key: string): string {
    return this.columnMapping.get(key) ?? '';
  }

  onCreate(): void {
    this.routingService.toCreator();
  }

  onSelectRecord(selectedId: number): void {
    this.routingService.toInfrastrukturEditor(selectedId);
  }

  onFilterReset(): void {
    this.filterService.reset();
  }

  public onExport(format: ExportFormat): void {
    const currentFilter = this.filterService.currentFilteredList.map(m => m.id);
    this.exporting = true;
    this.exportService.exportInfrastruktur('ABSTELLANLAGE', format, currentFilter).finally(() => {
      this.exporting = false;
      this.changeDetector.markForCheck();
    });
  }

  public onOpenCsvImportDialog(): void {
    this.dialog.open(CsvImportDialogComponent, { viewContainerRef: this.viewContainerRef });
  }
}
