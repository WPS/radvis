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

import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import {
  AfterContentInit,
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ContentChildren,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnInit,
  Output,
  QueryList,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { FormArray, FormControl, FormGroup } from '@angular/forms';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTable, MatTableDataSource } from '@angular/material/table';
import { InfrastrukturTabelleSpalteComponent } from 'src/app/viewer/viewer-shared/components/infrastruktur-tabelle-spalte/infrastruktur-tabelle-spalte.component';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
import { TabellenSpaltenAuswahlService } from 'src/app/viewer/viewer-shared/services/tabellen-spalten-auswahl.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-infrastruktur-tabelle-layout',
  templateUrl: './infrastruktur-tabelle-layout.component.html',
  styleUrls: ['./infrastruktur-tabelle-layout.component.scss'],
  providers: [{ provide: TabellenSpaltenAuswahlService, useExisting: InfrastrukturTabelleLayoutComponent }],
  changeDetection: ChangeDetectionStrategy.Default,
  standalone: false,
})
export class InfrastrukturTabelleLayoutComponent
  implements AfterContentInit, AfterViewInit, OnChanges, OnInit, TabellenSpaltenAuswahlService
{
  @Input()
  selectedId: number | null = null;
  @Input()
  fetching = false;
  @Input()
  data: any[] = [];
  @Input()
  sort: MatSort | undefined;
  @Input()
  sortingDataAccessor: undefined | ((data: any, sortHeaderId: string) => string | number);
  @Input()
  titleColumnIndex = 0;
  @Input()
  titleColumnPrefix = '';
  @Input()
  spaltenDefinition: SpaltenDefinition[] = [];
  @Input()
  filteredSpalten: string[] = [];

  @ViewChild(MatTable, { static: true }) table: MatTable<any> | undefined;
  @ContentChildren(InfrastrukturTabelleSpalteComponent) tabellenSpalten:
    | QueryList<InfrastrukturTabelleSpalteComponent>
    | undefined;
  @ViewChild(MatPaginator) paginator: MatPaginator | null = null;

  @ViewChild('filterBereich', { read: ElementRef })
  filterBereich: ElementRef | undefined;
  @ViewChild('tabellenInhalt', { read: ElementRef })
  tabellenInhalt: ElementRef | undefined;

  /**
   * Gibt nur die Id zurueck. Wenn die Infrastruktur gebraucht wird, dann stattdessen selectItem verwenden.
   */
  @Output()
  selectRow = new EventEmitter<number>();

  /**
   * Gibt die komplette Infrastruktur zurueck
   */
  @Output()
  selectItem = new EventEmitter<any>();

  @Output()
  changeBreakpointState = new EventEmitter<boolean>();
  isSmallViewport = false;

  public dataSource: MatTableDataSource<any> = new MatTableDataSource();

  displayedColumnsWithEdit: string[] = [];
  spaltenAuswahl = new FormArray<
    FormGroup<{ name: FormControl<string>; selected: FormControl<boolean>; displayName: FormControl<string> }>
  >([]);

  public get hasFilterOnAusgeblendetenSpalten(): boolean {
    return this.filteredSpalten.some(f =>
      this.spaltenAuswahl.value.filter(auswahl => !auswahl.selected).some(auswahl => auswahl.name === f)
    );
  }

  constructor(private responsive: BreakpointObserver) {
    this.spaltenAuswahl.valueChanges.subscribe(() => this.onSpaltenauswahlChanged());
  }

  public getCurrentAuswahl(): string[] {
    return this.spaltenAuswahl.value.filter(v => v.selected).map(v => v.name!);
  }

  @HostListener('document:keydown.control.alt.shift.f')
  onShortcutFilterbereich(): void {
    this.filterBereich?.nativeElement.querySelector('button, [role="button"]').focus();
  }

  @HostListener('document:keydown.control.alt.shift.i')
  onShortcutTabellenInhalt(): void {
    this.tabellenInhalt?.nativeElement.querySelector('button')?.focus();
  }

  ngOnInit(): void {
    this.responsive.observe([Breakpoints.HandsetLandscape, Breakpoints.TabletLandscape]).subscribe(result => {
      this.isSmallViewport = result.matches;
      this.changeBreakpointState.next(this.isSmallViewport);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(
      !changes.initialFilterValue || changes.initialFilterValue.firstChange,
      'initialFilterValue darf sich nicht ändern!'
    );
    invariant(this.data);
    invariant(this.spaltenDefinition);
    invariant(this.spaltenDefinition.length > 0, 'Infrastruktur-Tabelle muss mindestens eine Spalte haben');

    if (changes.data && this.dataSource.paginator) {
      this.dataSource.data = this.data;
    }

    if (changes.sort && this.sort) {
      this.dataSource.sort = this.sort;
    }

    if (changes.sortingDataAccessor && this.sortingDataAccessor) {
      this.dataSource.sortingDataAccessor = this.sortingDataAccessor;
    }

    if (changes.spaltenDefinition) {
      this.spaltenAuswahl.clear();
      this.spaltenDefinition.forEach(spaltenDef => {
        this.spaltenAuswahl.push(
          new FormGroup({
            name: new FormControl(spaltenDef.name, { nonNullable: true }),
            selected: new FormControl(spaltenDef.defaultVisible ?? true, { nonNullable: true }),
            displayName: new FormControl(spaltenDef.displayName, { nonNullable: true }),
          })
        );
      });
    }
  }

  ngAfterContentInit(): void {
    invariant(this.tabellenSpalten);
    this.tabellenSpalten.forEach(tabellenSpalte => {
      invariant(this.table);
      invariant(tabellenSpalte.matColumnDef);
      this.table.addColumnDef(tabellenSpalte.matColumnDef);
    });
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    if (this.data) {
      this.dataSource.data = this.data;
    }
  }

  public onSelectRecord(row: any): void {
    this.selectRow.next(row.id);
    this.selectItem.next(row);
  }

  public onSpaltenauswahlChanged(): void {
    const selectedColumns = this.spaltenAuswahl.value.filter(auswahl => auswahl.selected).map(auswahl => auswahl.name!);
    this.displayedColumnsWithEdit = [...selectedColumns, 'bearbeiten'];
  }
}
