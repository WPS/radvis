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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { ErweiterterMassnahmenFilterDialogComponent } from 'src/app/viewer/massnahme/components/erweiterter-massnahmen-filter-dialog/erweiterter-massnahmen-filter-dialog.component';
import { ErweiterterMassnahmenFilter } from 'src/app/viewer/massnahme/models/erweiterter-massnahmen-filter';
import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { ExportEvent } from 'src/app/viewer/viewer-shared/components/export-button/export-button.component';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { ExportService } from 'src/app/viewer/viewer-shared/services/export.service';

@Component({
  selector: 'rad-massnahmen-tabelle',
  templateUrl: './massnahmen-tabelle.component.html',
  styleUrls: ['./massnahmen-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: AbstractInfrastrukturenFilterService, useExisting: forwardRef(() => MassnahmeFilterService) }],
})
export class MassnahmenTabelleComponent {
  selectedMassnahmeID$: Observable<number | null>;
  data$: Observable<MassnahmeListenView[]>;

  exportFormate = [ExportFormat.GEOJSON, ExportFormat.SHP, ExportFormat.CSV];

  readonly spaltenDefinition: SpaltenDefinition[] = [
    { name: 'massnahmeKonzeptId', displayName: 'Maßnahme-ID', width: 'medium' },
    { name: 'bezeichnung', displayName: 'Bezeichnung', width: 'huge' },
    { name: 'massnahmenkategorien', displayName: 'Kategorien', expandable: true, width: 'huge' },
    { name: 'durchfuehrungszeitraum', displayName: 'Durchführungszeitraum' },
    { name: 'umsetzungsstatus', displayName: 'Umsetzungsstatus' },
    { name: 'umsetzungsstandStatus', displayName: 'Umsetzungsstand-Status' },
    { name: 'veroeffentlicht', displayName: 'Veröffentlicht' },
    { name: 'planungErforderlich', displayName: 'Planung erforderlich' },
    { name: 'prioritaet', displayName: 'Priorität' },
    { name: 'netzklassen', displayName: 'Netzklassen', expandable: true, width: 'medium' },
    { name: 'baulastZustaendiger', displayName: 'Baulastträger', width: 'large' },
    { name: 'zustaendiger', displayName: 'Zuständige/r', width: 'large' },
    { name: 'unterhaltsZustaendiger', displayName: 'Unterhaltszuständige/r', width: 'large' },
    { name: 'letzteAenderung', displayName: 'Letzte Änderung' },
    {
      name: 'benutzerLetzteAenderung',
      displayName: 'Benutzer/in der letzten Änderung',
      expandable: false,
      width: 'large',
    },
    { name: 'sollStandard', displayName: 'Soll-Standard' },
    { name: 'handlungsverantwortlicher', displayName: 'Wer soll tätig werden?' },
    { name: 'konzeptionsquelle', displayName: 'Quelle', width: 'large' },
    { name: 'archiviert', displayName: 'Archiviert' },
  ];

  public getDisplayValue = MassnahmeListenView.getDisplayValueForKey;
  public isBenutzerBerechtigtMassnahmenZuErstellen: boolean;
  public isBenutzerBerechtigtUmsetzungsstandsabfragenZuStarten: boolean;
  public isBenutzerBerechtigtUmsetzungsstandsabfragenAuszuwerten: boolean;
  public isBenutzerBerechtigtZuArchivieren: boolean;
  public massnahmenCreatorRoute: string;

  public exporting = false;

  isSmallViewport = false;
  erweiterterFilterActive$: Observable<boolean>;
  filteredSpalten$: Observable<string[]>;
  archiving: boolean = false;

  constructor(
    public massnahmeFilterService: MassnahmeFilterService,
    private massnahmenRoutingService: MassnahmenRoutingService,
    private exportService: ExportService,
    private changeDetector: ChangeDetectorRef,
    private dialog: MatDialog,
    private massnahmeService: MassnahmeService,
    benutzerDetailsService: BenutzerDetailsService
  ) {
    this.isBenutzerBerechtigtMassnahmenZuErstellen = benutzerDetailsService.canCreateMassnahmen();
    this.isBenutzerBerechtigtUmsetzungsstandsabfragenZuStarten =
      benutzerDetailsService.canStartUmsetzungsstandsabfragen();
    this.isBenutzerBerechtigtUmsetzungsstandsabfragenAuszuwerten =
      benutzerDetailsService.canEvaluateUmsetzungsstandsabfragen();
    this.isBenutzerBerechtigtZuArchivieren = benutzerDetailsService.canMassnahmenArchivieren();
    this.massnahmenCreatorRoute = this.massnahmenRoutingService.getCreatorRoute();
    this.selectedMassnahmeID$ = this.massnahmenRoutingService.selectedInfrastrukturId$;
    this.data$ = this.massnahmeFilterService.filteredList$;
    this.erweiterterFilterActive$ = this.massnahmeFilterService.erweiterterFilterAktiv$;
    this.filteredSpalten$ = this.massnahmeFilterService.filter$.pipe(
      map(filteredFields => filteredFields.map(f => f.field))
    );
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  public onSelectRecord(id: number): void {
    this.massnahmenRoutingService.toInfrastrukturEditor(id);
  }

  onFilterReset(): void {
    this.massnahmeFilterService.reset();
  }

  onArchivieren(): void {
    if (this.archiving) {
      return;
    }

    this.archiving = true;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question: `Es werden ${this.massnahmeFilterService.currentFilteredList.length} Maßnahmen archiviert.`,
        labelYes: 'Archivieren',
        labelNo: 'Abbrechen',
        inverseButtonColorCoding: true,
      } as QuestionYesNo,
    });

    dialogRef
      .afterClosed()
      .toPromise()
      .then(result => {
        if (result) {
          return this.massnahmeService
            .archivieren(this.massnahmeFilterService.currentFilteredList.map(m => m.id))
            .then(() => {
              this.massnahmeFilterService.refetchData();
            });
        }

        return Promise.resolve();
      })
      .finally(() => {
        this.archiving = false;
        this.changeDetector.markForCheck();
      });
  }

  public sortingDataAccessor = (item: MassnahmeListenView, header: string): any => {
    const displayValue = MassnahmeListenView.getSortingValueForKey(item, header);
    return Array.isArray(displayValue) ? displayValue[0] : displayValue;
  };

  public onExport(exportEvent: ExportEvent): void {
    const currentFilter = this.massnahmeFilterService.currentFilteredList.map(m => m.id);
    this.exporting = true;
    const fieldNamesToExclude = this.spaltenDefinition
      .filter(def => !exportEvent.felder.includes(def.name))
      .map(def => def.displayName);
    this.exportService
      .exportInfrastruktur('MASSNAHME', exportEvent.format, currentFilter, fieldNamesToExclude)
      .finally(() => {
        this.exporting = false;
        this.changeDetector.markForCheck();
      });
  }

  onErweiterteFilterVerwalten(): void {
    const dialogRef = this.dialog.open(ErweiterterMassnahmenFilterDialogComponent, {
      data: this.massnahmeFilterService.erweiterterFilter,
      width: '800px',
      disableClose: true,
      autoFocus: 'dialog',
    });

    dialogRef.afterClosed().subscribe((neuerErweiterterFilter: ErweiterterMassnahmenFilter) => {
      if (neuerErweiterterFilter) {
        this.massnahmeFilterService.updateErweiterterFilter(neuerErweiterterFilter);
      }
    });
  }
}
