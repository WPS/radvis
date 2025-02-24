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

import { ChangeDetectionStrategy, Component, forwardRef } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { ErweiterterAnpassungswunschFilterDialogComponent } from 'src/app/viewer/anpassungswunsch/components/erweiterter-anpassungswunsch-filter-dialog/erweiterter-anpassungswunsch-filter-dialog.component';
import { AnpassungswunschListenView } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-listen-view';
import { ErweiterterAnpassungswunschFilter } from 'src/app/viewer/anpassungswunsch/models/erweiterter-anpassungswunsch-filter';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { AnpassungswunschFilterService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-filter.service';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';

@Component({
  selector: 'rad-anpassungswunsch-tabelle',
  templateUrl: './anpassungswunsch-tabelle.component.html',
  styleUrls: ['./anpassungswunsch-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: AbstractInfrastrukturenFilterService, useExisting: forwardRef(() => AnpassungswunschFilterService) },
  ],
  standalone: false,
})
export class AnpassungswunschTabelleComponent {
  selectedAnpassungswunschID$: Observable<number | null>;

  spaltenDefinition: SpaltenDefinition[] = [
    { name: 'beschreibung', displayName: 'Beschreibung' },
    { name: 'status', displayName: 'Status' },
    { name: 'kategorie', displayName: 'Zu ändern in' },
    { name: 'verantwortlicheOrganisation', displayName: 'Verantwortliche Organisation' },
    { name: 'erstelltAm', displayName: 'Erstellt am' },
    { name: 'zuletztGeaendertAm', displayName: 'Letzte Änderung' },
  ];

  data$: Observable<AnpassungswunschListenView[]>;

  public anpassungswunschCreatorRoute: string;

  public isBenutzerBerechtigtAnpassungswunschZuErstellen: boolean;

  isSmallViewport = false;
  filteredSpalten$: Observable<string[]>;
  erweiterterFilterActive$: Observable<boolean>;

  constructor(
    public anpassungswunschFilterService: AnpassungswunschFilterService,
    private anpassungenRoutingService: AnpassungenRoutingService,
    benutzerDetailsService: BenutzerDetailsService,
    private matDialog: MatDialog
  ) {
    this.data$ = this.anpassungswunschFilterService.filteredList$;

    this.selectedAnpassungswunschID$ = this.anpassungenRoutingService.selectedInfrastrukturId$;

    this.isBenutzerBerechtigtAnpassungswunschZuErstellen = benutzerDetailsService.canCreateAnpassungswunsch();

    this.anpassungswunschCreatorRoute = anpassungenRoutingService.getCreatorRoute();

    this.filteredSpalten$ = this.anpassungswunschFilterService.filter$.pipe(
      map(filteredFields => filteredFields.map(f => f.field))
    );

    this.erweiterterFilterActive$ = anpassungswunschFilterService.erweiterterFilterActive$;
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  public onSelectRecord(id: number): void {
    this.anpassungenRoutingService.toInfrastrukturEditor(id);
  }

  onFilterReset(): void {
    this.anpassungswunschFilterService.reset();
  }

  onOpenErweiterterFilter(): void {
    const dialogRef = this.matDialog.open<
      ErweiterterAnpassungswunschFilterDialogComponent,
      ErweiterterAnpassungswunschFilter,
      ErweiterterAnpassungswunschFilter
    >(ErweiterterAnpassungswunschFilterDialogComponent, {
      data: this.anpassungswunschFilterService.erweiterterFilter,
      width: '800px',
      disableClose: true,
      autoFocus: 'dialog',
    });

    dialogRef
      .afterClosed()
      .toPromise()
      .then(filter => {
        if (filter) {
          this.anpassungswunschFilterService.updateErweiterterFilter(filter);
        }
      });
  }

  getElementValue: (item: AnpassungswunschListenView, key: string) => string | string[] = (
    item: AnpassungswunschListenView,
    key: string
  ) => {
    const value = this.anpassungswunschFilterService.getInfrastrukturValueForKey(item, key);
    if (key === 'beschreibung' && value.length > 50) {
      return value.slice(0, 47) + '...';
    }
    return value;
  };
}
