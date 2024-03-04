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

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { OrganisationenVerwaltungService } from 'src/app/administration/services/organisationen-verwaltung.service';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';

@Component({
  selector: 'rad-organisation-list',
  templateUrl: './organisation-list.component.html',
  styleUrls: ['./organisation-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganisationListComponent implements AfterViewInit, OnDestroy {
  @ViewChild(MatPaginator) paginator: MatPaginator | null = null;
  @ViewChild(MatSort) sort: MatSort | null = null;

  pageSize$: Observable<number>;
  page$: Observable<number>;

  public isBenutzerBerechtigtOrganisationZuErstellen: boolean;

  organisationDataSource: MatTableDataSource<Verwaltungseinheit> = new MatTableDataSource<Verwaltungseinheit>();
  isFetching = false;
  headerColumns = ['name', 'organisationsArt', 'status'];
  formControl: UntypedFormControl;
  OrganisationsArt = OrganisationsArt;

  private subscriptions: Subscription[] = [];

  private readonly defaultPageSize = 10;

  constructor(
    public activatedRoute: ActivatedRoute,
    private router: Router,
    private administrationRoutingService: AdministrationRoutingService,
    benutzerDetailsService: BenutzerDetailsService,
    changeDetector: ChangeDetectorRef,
    organisationVerwaltungService: OrganisationenVerwaltungService
  ) {
    this.isBenutzerBerechtigtOrganisationZuErstellen = benutzerDetailsService.canCreateOrganisationen();
    //TODO stattdessen Resolver verwenden
    this.isFetching = true;
    organisationVerwaltungService
      .getAlleFuerBearbeitung()
      .then(organisationsListe => (this.organisationDataSource.data = organisationsListe))
      .finally(() => {
        this.isFetching = false;
        changeDetector.markForCheck();
      });
    this.formControl = new UntypedFormControl('');
    this.formControl.valueChanges.pipe(debounceTime(100)).subscribe(() => this.updateQueryParams());
    this.subscriptions.push(
      this.activatedRoute.queryParamMap.subscribe((route: ParamMap) => {
        const suche = route.get(AdministrationRoutingService.ORGANISATION_SUCHE_QUERY_PARAM) || '';
        this.organisationDataSource.filter = suche;
        this.formControl.setValue(suche, { emitEvent: false });
      })
    );
    this.pageSize$ = this.activatedRoute.queryParamMap.pipe(
      map(params => {
        const param = params.get(AdministrationRoutingService.ORGANISATION_SUCHE_PAGE_SIZE_QUERY_PARAM);
        if (param) {
          return +param;
        }
        return this.defaultPageSize;
      }),
      distinctUntilChanged()
    );
    this.page$ = this.activatedRoute.queryParamMap.pipe(
      map(params => {
        const param = params.get(AdministrationRoutingService.ORGANISATION_SUCHE_PAGE_INDEX_QUERY_PARAM);
        if (param) {
          return +param;
        }
        return 0;
      }),
      distinctUntilChanged()
    );
  }

  ngAfterViewInit(): void {
    this.organisationDataSource.paginator = this.paginator;
    this.organisationDataSource.sort = this.sort;
    // eslint-disable-next-line @typescript-eslint/explicit-function-return-type
    this.organisationDataSource.sortingDataAccessor = (value, key) =>
      Verwaltungseinheit.getSortingValueForKey(value, key);
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

  onEdit(id: number): void {
    this.administrationRoutingService.toOrganisationEditor(id);
  }

  onCreate(): void {
    this.administrationRoutingService.toOrganisationCreator();
  }

  private updateQueryParams(): void {
    const urlTree = this.router.parseUrl(this.router.url);
    urlTree.queryParams[AdministrationRoutingService.ORGANISATION_SUCHE_QUERY_PARAM] = this.formControl.value;
    urlTree.queryParams[AdministrationRoutingService.ORGANISATION_SUCHE_PAGE_INDEX_QUERY_PARAM] =
      this.paginator?.pageIndex || 0;
    urlTree.queryParams[AdministrationRoutingService.ORGANISATION_SUCHE_PAGE_SIZE_QUERY_PARAM] =
      this.paginator?.pageSize || this.defaultPageSize;
    this.router.navigateByUrl(urlTree, { replaceUrl: true });
  }
}
