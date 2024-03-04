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

import { AfterViewInit, ChangeDetectionStrategy, Component, OnDestroy, ViewChild } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { BenutzerListView } from 'src/app/administration/models/benutzer-list-view';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';

@Component({
  selector: 'rad-benutzer-list',
  templateUrl: './benutzer-list.component.html',
  styleUrls: ['./benutzer-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BenutzerListComponent implements AfterViewInit, OnDestroy {
  @ViewChild(MatPaginator) paginator: MatPaginator | null = null;
  @ViewChild(MatSort) sort: MatSort | null = null;

  pageSize$: Observable<number>;
  page$: Observable<number>;

  benutzerDataSource: MatTableDataSource<BenutzerListView>;
  // muss den keys am BenutzerListView entsprechen, damit sorting funktioniert
  headerColumns = ['nachname', 'vorname', 'status', 'organisation', 'email'];
  formControl: UntypedFormControl;
  BenutzerStatus = BenutzerStatus;

  private subscriptions: Subscription[] = [];

  private readonly defaultPageSize = 10;

  private data: BenutzerListView[] = [];
  private filteredData: BenutzerListView[] = [];

  showInaktiveBenutzer = false;
  showAbgelehnteBenutzer = false;

  constructor(
    public activatedRoute: ActivatedRoute,
    private router: Router,
    private administrationRoutingService: AdministrationRoutingService
  ) {
    this.data = activatedRoute.snapshot.data.benutzer;
    this.data.sort((b, a) => a.status.localeCompare(b.status));
    this.benutzerDataSource = new MatTableDataSource<BenutzerListView>([]);
    this.applyFilter();
    this.formControl = new UntypedFormControl('');
    this.formControl.valueChanges.pipe(debounceTime(100)).subscribe(() => this.updateQueryParams());
    this.subscriptions.push(
      this.activatedRoute.queryParamMap.subscribe((route: ParamMap) => {
        const suche = route.get(AdministrationRoutingService.BENUTZER_SUCHE_QUERY_PARAM) || '';
        this.benutzerDataSource.filter = suche;
        this.formControl.setValue(suche, { emitEvent: false });
      })
    );
    this.pageSize$ = this.activatedRoute.queryParamMap.pipe(
      map(params => {
        const param = params.get(AdministrationRoutingService.BENUTZER_SUCHE_PAGE_SIZE_QUERY_PARAM);
        if (param) {
          return +param;
        }
        return this.defaultPageSize;
      }),
      distinctUntilChanged()
    );
    this.page$ = this.activatedRoute.queryParamMap.pipe(
      map(params => {
        const param = params.get(AdministrationRoutingService.BENUTZER_SUCHE_PAGE_INDEX_QUERY_PARAM);
        if (param) {
          return +param;
        }
        return 0;
      }),
      distinctUntilChanged()
    );
  }

  ngAfterViewInit(): void {
    this.benutzerDataSource.paginator = this.paginator;
    this.benutzerDataSource.sort = this.sort;
    this.applyFilter;
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  onClearSearch(): void {
    this.formControl.setValue('');
  }

  onPage(): void {
    this.updateQueryParams();
  }

  onEdit(benutzerId: number): void {
    this.administrationRoutingService.toBenutzerEditor(benutzerId);
  }

  private updateQueryParams(): void {
    const urlTree = this.router.parseUrl(this.router.url);
    urlTree.queryParams[AdministrationRoutingService.BENUTZER_SUCHE_QUERY_PARAM] = this.formControl.value;
    urlTree.queryParams[AdministrationRoutingService.BENUTZER_SUCHE_PAGE_INDEX_QUERY_PARAM] =
      this.paginator?.pageIndex || 0;
    urlTree.queryParams[AdministrationRoutingService.BENUTZER_SUCHE_PAGE_SIZE_QUERY_PARAM] =
      this.paginator?.pageSize || this.defaultPageSize;
    this.router.navigateByUrl(urlTree, { replaceUrl: true });
  }

  onToggleInaktiveBenutzerAktiv(): void {
    this.showInaktiveBenutzer = !this.showInaktiveBenutzer;
    this.applyFilter();
  }

  onToggleAbgelehnteBenutzerAktiv(): void {
    this.showAbgelehnteBenutzer = !this.showAbgelehnteBenutzer;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.filteredData = this.data.filter(
      data =>
        (this.showInaktiveBenutzer || data.status !== BenutzerStatus.INAKTIV) &&
        (this.showAbgelehnteBenutzer || data.status !== BenutzerStatus.ABGELEHNT)
    );
    this.benutzerDataSource.data = this.filteredData;
  }
}
