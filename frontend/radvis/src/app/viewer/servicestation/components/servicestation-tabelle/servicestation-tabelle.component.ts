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
import { ServicestationListView } from 'src/app/viewer/servicestation/models/servicestation-list-view';
import { ServicestationFilterService } from 'src/app/viewer/servicestation/services/servicestation-filter.service';
import { ServicestationRoutingService } from 'src/app/viewer/servicestation/services/servicestation-routing.service';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import { CsvImportDialogComponent } from 'src/app/viewer/viewer-shared/components/csv-import-dialog/csv-import-dialog.component';
import { ExportEvent } from 'src/app/viewer/viewer-shared/components/export-button/export-button.component';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
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
  standalone: false,
})
export class ServicestationTabelleComponent implements CsvImportService {
  public csvImportFeatureToggl = false;
  data$: Observable<ServicestationListView[]>;
  selectedID$: Observable<number | null>;
  exporting = false;
  exportFormate = [ExportFormat.CSV];
  isSmallViewport = false;

  spaltenDefinition: SpaltenDefinition[] = [
    { name: 'name', displayName: 'Name', width: 'large' },
    { name: 'quellSystem', displayName: 'Quellsystem' },
    { name: 'gebuehren', displayName: 'Gebühren' },
    { name: 'betreiber', displayName: 'Betreiber' },
    { name: 'radkultur', displayName: 'RadKULTUR' },
    { name: 'marke', displayName: 'Marke' },
    { name: 'luftpumpe', displayName: 'Luftpumpe' },
    { name: 'kettenwerkzeug', displayName: 'Kettenwerkzeug' },
    { name: 'werkzeug', displayName: 'Werkzeug' },
    { name: 'fahrradhalterung', displayName: 'Fahrradhalterung' },
    { name: 'organisation', displayName: 'Zuständig in RadVIS', width: 'large' },
    { name: 'typ', displayName: 'Servicestation-Typ', width: 'large' },
    { name: 'status', displayName: 'Status' },
  ];
  filteredSpalten$: Observable<string[]>;

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
    this.filteredSpalten$ = this.filterService.filter$.pipe(map(filteredFields => filteredFields.map(f => f.field)));
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
      .exportInfrastruktur('SERVICESTATION', exportEvent.format, currentFilter, fieldNamesToExclude)
      .finally(() => {
        this.exporting = false;
        this.changeDetector.markForCheck();
      });
  }

  public onOpenCsvImportDialog(): void {
    this.dialog.open(CsvImportDialogComponent, { viewContainerRef: this.viewContainerRef });
  }
}
