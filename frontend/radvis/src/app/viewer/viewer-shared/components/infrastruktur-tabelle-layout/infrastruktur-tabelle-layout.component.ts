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
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatColumnDef, MatTable, MatTableDataSource } from '@angular/material/table';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-infrastruktur-tabelle-layout',
  templateUrl: './infrastruktur-tabelle-layout.component.html',
  styleUrls: ['./infrastruktur-tabelle-layout.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default, //wichtig, damit änderungen an der datasource gerendert werden
})
export class InfrastrukturTabelleLayoutComponent implements AfterContentInit, AfterViewInit, OnChanges, OnInit {
  @Input()
  displayedColumns!: string[];
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

  @ViewChild(MatTable, { static: true }) table: MatTable<any> | undefined;
  @ContentChildren(MatColumnDef) columnDefs: QueryList<MatColumnDef> | undefined;
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

  get displayedColumnsWithEdit(): string[] {
    return [...this.displayedColumns, 'bearbeiten'];
  }

  constructor(private responsive: BreakpointObserver) {}

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
    invariant(this.displayedColumns);
    invariant(this.displayedColumns.length > 0, 'Infrastruktur-Tabelle muss mindestens eine Spalte haben');

    if (changes.data && this.dataSource.paginator) {
      this.dataSource.data = this.data;
    }

    if (changes.sort && this.sort) {
      this.dataSource.sort = this.sort;
    }

    if (changes.sortingDataAccessor && this.sortingDataAccessor) {
      this.dataSource.sortingDataAccessor = this.sortingDataAccessor;
    }
  }

  ngAfterContentInit(): void {
    invariant(this.columnDefs);
    this.columnDefs.forEach(columnDef => {
      invariant(this.table);
      this.table.addColumnDef(columnDef);
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
}
