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
import { Leihstation } from 'src/app/viewer/leihstation/models/leihstation';
import { LeihstationFilterService } from 'src/app/viewer/leihstation/services/leihstation-filter.service';
import { LeihstationRoutingService } from 'src/app/viewer/leihstation/services/leihstation-routing.service';
import { LeihstationService } from 'src/app/viewer/leihstation/services/leihstation.service';
import { CsvImportDialogComponent } from 'src/app/viewer/viewer-shared/components/csv-import-dialog/csv-import-dialog.component';
import { ExportEvent } from 'src/app/viewer/viewer-shared/components/export-button/export-button.component';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { CsvImportService } from 'src/app/viewer/viewer-shared/services/csv-import.service';
import { ExportService } from 'src/app/viewer/viewer-shared/services/export.service';

@Component({
  selector: 'rad-leihstation-tabelle',
  templateUrl: './leihstation-tabelle.component.html',
  styleUrls: ['./leihstation-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: AbstractInfrastrukturenFilterService,
      useExisting: LeihstationFilterService,
    },
    { provide: CsvImportService, useExisting: forwardRef(() => LeihstationTabelleComponent) },
  ],
})
export class LeihstationTabelleComponent implements CsvImportService {
  public csvImportFeatureToggl = false;
  data$: Observable<Leihstation[]>;
  selectedID$: Observable<number | null>;
  exporting = false;
  exportFormate = [ExportFormat.CSV];
  isSmallViewport = false;

  spaltenDefinition: SpaltenDefinition[] = [
    { name: 'betreiber', displayName: 'Betreiber' },
    { name: 'status', displayName: 'Status' },
    { name: 'anzahlFahrraeder', displayName: 'Anzahl Fahrräder' },
    { name: 'anzahlPedelecs', displayName: 'Anzahl Pedelecs' },
    {
      name: 'anzahlAbstellmoeglichkeiten',
      displayName: 'Anzahl Abstellmöglichkeiten',
      width: 'auto',
      expandable: false,
    },
    { name: 'freiesAbstellen', displayName: 'Freies Abstellen möglich' },
    { name: 'quellSystem', displayName: 'Quellsystem' },
  ];
  filteredSpalten$: Observable<string[]>;

  constructor(
    public filterService: LeihstationFilterService,
    public dialog: MatDialog,
    private routingService: LeihstationRoutingService,
    private exportService: ExportService,
    private changeDetector: ChangeDetectorRef,
    private viewContainerRef: ViewContainerRef,
    private leihstationService: LeihstationService,
    private manualRoutingService: ManualRoutingService,
    featureTogglzService: FeatureTogglzService
  ) {
    this.data$ = this.filterService.filteredList$;
    this.selectedID$ = this.routingService.selectedInfrastrukturId$;
    this.filteredSpalten$ = this.filterService.filter$.pipe(map(filteredFields => filteredFields.map(f => f.field)));
    this.csvImportFeatureToggl = featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_LEIHSTATIONEN_CSV_IMPORT);
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  uploadCsv(file: File): Promise<Blob> {
    return this.leihstationService.uploadCsv(file);
  }

  afterUpload(): void {
    this.filterService.refetchData();
  }

  openManual(): void {
    this.manualRoutingService.openManualLeihstationenImport();
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
    const fieldNamesToExclude = this.spaltenDefinition
      .filter(def => !exportEvent.felder.includes(def.name))
      .map(def => def.displayName);
    this.exporting = true;
    this.exportService
      .exportInfrastruktur('LEIHSTATION', exportEvent.format, currentFilter, fieldNamesToExclude)
      .finally(() => {
        this.exporting = false;
        this.changeDetector.markForCheck();
      });
  }

  public onOpenCsvImportDialog(): void {
    this.dialog.open(CsvImportDialogComponent, { viewContainerRef: this.viewContainerRef });
  }
}
