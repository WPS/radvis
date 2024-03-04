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
import { Servicestation } from 'src/app/viewer/servicestation/models/servicestation';
import { ServicestationFilterService } from 'src/app/viewer/servicestation/services/servicestation-filter.service';
import { ServicestationRoutingService } from 'src/app/viewer/servicestation/services/servicestation-routing.service';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import { CsvImportDialogComponent } from 'src/app/viewer/viewer-shared/components/csv-import-dialog/csv-import-dialog.component';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { CsvImportService } from 'src/app/viewer/viewer-shared/services/csv-import.service';
import { ExportService } from 'src/app/viewer/viewer-shared/services/export.service';

@Component({
  selector: 'rad-servicestation-tabelle',
  templateUrl: './servicestation-tabelle.component.html',
  styleUrls: ['./servicestation-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: AbstractInfrastrukturenFilterService,
      useExisting: ServicestationFilterService,
    },
    { provide: CsvImportService, useExisting: forwardRef(() => ServicestationTabelleComponent) },
  ],
})
export class ServicestationTabelleComponent implements CsvImportService {
  public csvImportFeatureToggl = false;
  data$: Observable<Servicestation[]>;
  displayedColumns: string[];
  selectedID$: Observable<number | null>;
  exporting = false;
  exportFormate = [ExportFormat.CSV];
  isSmallViewport = false;

  private columnMapping: Map<string, string> = new Map([
    ['name', 'Name'],
    ['quellSystem', 'Quellsystem'],
    ['gebuehren', 'Gebühren'],
    ['betreiber', 'Betreiber'],
    ['marke', 'Marke'],
    ['luftpumpe', 'Luftpumpe'],
    ['kettenwerkzeug', 'Kettenwerkzeug'],
    ['werkzeug', 'Werkzeug'],
    ['fahrradhalterung', 'Fahrradhalterung'],
    ['organisation', 'Zuständig in RadVIS'],
    ['typ', 'Servicestation-Typ'],
    ['status', 'Status'],
  ]);

  constructor(
    public filterService: ServicestationFilterService,
    private servicestationService: ServicestationService,
    private routingService: ServicestationRoutingService,
    private exportService: ExportService,
    private changeDetector: ChangeDetectorRef,
    featureTogglzService: FeatureTogglzService,
    private viewContainerRef: ViewContainerRef,
    public dialog: MatDialog,
    private manualRoutingService: ManualRoutingService
  ) {
    this.data$ = this.filterService.filteredList$;
    this.selectedID$ = this.routingService.selectedInfrastrukturId$;
    this.displayedColumns = Array.from(this.columnMapping.keys());
    this.csvImportFeatureToggl = featureTogglzService.isToggledOn(
      FeatureTogglzService.TOGGLZ_SERVICESTATIONEN_CSV_IMPORT
    );
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  uploadCsv(file: File): Promise<Blob> {
    return this.servicestationService.uploadCsv(file);
  }

  afterUpload(): void {
    this.filterService.refetchData();
  }

  openManual(): void {
    this.manualRoutingService.openManualServicestationenImport();
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
    this.exportService.exportInfrastruktur('SERVICESTATION', format, currentFilter).finally(() => {
      this.exporting = false;
      this.changeDetector.markForCheck();
    });
  }

  public onOpenCsvImportDialog(): void {
    this.dialog.open(CsvImportDialogComponent, { viewContainerRef: this.viewContainerRef });
  }
}
