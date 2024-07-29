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

import { BehaviorSubject, from, Observable, Subscription } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { FilterQueryParamsService } from './filter-query-params.service';

export abstract class AbstractInfrastrukturenFilterService<T> {
  public filter$: Observable<FieldFilter[]>;

  protected filteredInfrastrukturenSubject = new BehaviorSubject<T[]>([]);
  protected alleInfrastrukturen: T[] = [];
  protected _fetching = false;
  protected fetchSubscription: Subscription | undefined;
  private fetchDataSubscription: Subscription = new Subscription();

  protected constructor(
    private infrastrukturenSelektionService: InfrastrukturenSelektionService,
    private infrastrukturArt: Infrastruktur,
    private filterQueryParamsService: FilterQueryParamsService
  ) {
    this.filter$ = filterQueryParamsService.filter$.pipe(map(filterMap => filterMap.get(infrastrukturArt) || []));
    this.filter$.subscribe(fieldFilters => this.filterInfrastruktur(fieldFilters));
  }

  public get filteredList$(): Observable<T[]> {
    return this.filteredInfrastrukturenSubject.asObservable();
  }

  public get currentFilteredList(): T[] {
    return this.filteredInfrastrukturenSubject.getValue();
  }

  public get fetching(): boolean {
    return this._fetching;
  }

  public getFilterForField(field: string): string | undefined {
    return this.getFieldFiltersFromUrl().find(fieldFilter => fieldFilter.field === field)?.value;
  }

  public refetchData(): void {
    if (this.filteredInfrastrukturenSubject.getValue().length > 0) {
      this.filteredInfrastrukturenSubject.next([]);
    }
    this.fetchDataSubscription.unsubscribe();

    this._fetching = true;
    this.fetchDataSubscription = from(this.getAll())
      .subscribe(this.onAlleInfrastrukturenChanged)
      .add(() => {
        this._fetching = false;
      });
  }

  public filterField(field: string, value: string): void {
    this.filterQueryParamsService.update([new FieldFilter(field, value)], this.infrastrukturArt);
  }

  public reset(): void {
    this.filterQueryParamsService.reset(this.infrastrukturArt);
  }

  protected onAlleInfrastrukturenChanged = (infrastrukturen: T[]): void => {
    this.alleInfrastrukturen = infrastrukturen;
    this.filterInfrastruktur(this.getFieldFiltersFromUrl());
  };

  // kann nicht im Constructor gemacht werden, da dann die DI der abgeleiteten Klassen noch nicht fertig ist...
  protected init(): void {
    this.fetchSubscription = this.infrastrukturenSelektionService.selektierteInfrastrukturen$
      .pipe(
        map(infrastrukturen => infrastrukturen.includes(this.infrastrukturArt)),
        distinctUntilChanged()
      )
      .subscribe(typeSelected => {
        if (typeSelected) {
          this.refetchData();
        } else {
          this.alleInfrastrukturen = [];
          this.filteredInfrastrukturenSubject.next(this.alleInfrastrukturen);
        }
      });
  }

  private filterFieldsByValues(infrastrukturen: T[], fieldFilter: FieldFilter[]): T[] {
    return fieldFilter.length > 0
      ? infrastrukturen.filter(infrastruktur => this.filterInfrastrukturFieldByValue(infrastruktur, fieldFilter))
      : infrastrukturen;
  }

  private filterInfrastruktur(fieldFilters: FieldFilter[]): void {
    const filteredInfrastrukturen = this.filterFieldsByValues(this.alleInfrastrukturen, fieldFilters);
    this.filteredInfrastrukturenSubject.next(filteredInfrastrukturen);
  }

  private filterInfrastrukturFieldByValue(infrastruktur: T, fieldFilters: FieldFilter[]): boolean {
    return fieldFilters.every(fieldFilter => {
      let infrastrukturValueForKey: string | string[];
      try {
        infrastrukturValueForKey = this.getInfrastrukturValueForKey(infrastruktur, fieldFilter.field);
      } catch (e) {
        // wenn dem Wert für das Feld (fieldFilter.field) keine Eigenschaft der Infrastruktur entspricht,
        // dann soll auf Grundlage dieses Filters nicht gefiltert werden
        return true;
      }
      if (Array.isArray(infrastrukturValueForKey)) {
        return infrastrukturValueForKey.some(singleValue =>
          singleValue.toLowerCase().includes(fieldFilter.value.toLowerCase())
        );
      } else {
        return infrastrukturValueForKey.toLowerCase().includes(fieldFilter.value.toLowerCase());
      }
    });
  }

  private getFieldFiltersFromUrl(): FieldFilter[] {
    const fieldFilters = this.filterQueryParamsService.filterQueryParamsSnapshot.filters.get(
      this.infrastrukturArt.pathElement
    );
    return fieldFilters ?? [];
  }

  protected abstract getAll(): Promise<T[]>;

  protected abstract getInfrastrukturValueForKey(item: T, key: string): string | string[];
}
