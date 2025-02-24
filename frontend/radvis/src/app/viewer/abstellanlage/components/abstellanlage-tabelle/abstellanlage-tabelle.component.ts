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
import { map } from 'rxjs/operators';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { AbstellanlageFilterService } from 'src/app/viewer/abstellanlage/services/abstellanlage-filter.service';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { AbstellanlageService } from 'src/app/viewer/abstellanlage/services/abstellanlage.service';
import { CsvImportDialogComponent } from 'src/app/viewer/viewer-shared/components/csv-import-dialog/csv-import-dialog.component';
import { ExportEvent } from 'src/app/viewer/viewer-shared/components/export-button/export-button.component';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
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
  standalone: false,
})
export class AbstellanlageTabelleComponent implements CsvImportService {
  public csvImportFeatureToggl = false;

  data$: Observable<Abstellanlage[]>;
  selectedID$: Observable<number | null>;
  exporting = false;
  exportFormate = [ExportFormat.CSV];
  isSmallViewport = false;

  spaltenDefinition: SpaltenDefinition[] = [
    { name: 'betreiber', displayName: 'Betreiber' },
    { name: 'externeId', displayName: 'Externe ID', width: 'large' },
    { name: 'quellSystem', displayName: 'Quellsystem' },
    { name: 'zustaendig', displayName: 'Zuständig in RadVIS' },
    { name: 'anzahlStellplaetze', displayName: 'Anzahl Stellplätze' },
    { name: 'abstellanlagenOrt', displayName: 'Ort' },
    { name: 'groessenklasse', displayName: 'Größenklasse' },
    { name: 'stellplatzart', displayName: 'Stellplatzart' },
    { name: 'status', displayName: 'Status' },
  ];
  filteredSpalten$: Observable<string[]>;

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
    this.csvImportFeatureToggl = featureTogglzService.isToggledOn(
      FeatureTogglzService.TOGGLZ_ABSTELLANLAGEN_CSV_IMPORT
    );
    this.filteredSpalten$ = this.filterService.filter$.pipe(map(filteredFields => filteredFields.map(f => f.field)));
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

  onCreate(): void {
    this.routingService.toCreator();
  }

  onSelectRecord(selectedId: number): void {
    this.routingService.toInfrastrukturEditor(selectedId);
  }

  onFilterReset(): void {
    this.filterService.reset();
  }

  public onExport(exportEvent: ExportEvent): void {
    const currentFilter = this.filterService.currentFilteredList.map(m => m.id);
    this.exporting = true;
    const fieldNamesToExclude = this.spaltenDefinition
      .filter(def => !exportEvent.felder.includes(def.name))
      .map(def => def.displayName);
    this.exportService
      .exportInfrastruktur('ABSTELLANLAGE', exportEvent.format, currentFilter, fieldNamesToExclude)
      .finally(() => {
        this.exporting = false;
        this.changeDetector.markForCheck();
      });
  }

  public onOpenCsvImportDialog(): void {
    this.dialog.open(CsvImportDialogComponent, { viewContainerRef: this.viewContainerRef });
  }
}
