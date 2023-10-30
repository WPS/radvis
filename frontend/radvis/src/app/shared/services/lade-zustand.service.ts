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

import { Injectable } from '@angular/core';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class LadeZustandService {
  public isLoading$: Observable<boolean>;
  private loading$ = new Subject<boolean>();

  constructor(router: Router) {
    this.isLoading$ = this.loading$.asObservable().pipe(debounceTime(1), distinctUntilChanged());
    router.events.subscribe(routerEvent => {
      if (routerEvent instanceof NavigationStart) {
        this.loading$.next(true);
      } else if (
        routerEvent instanceof NavigationEnd ||
        routerEvent instanceof NavigationCancel ||
        routerEvent instanceof NavigationError
      ) {
        this.loading$.next(false);
      }
    });
  }

  public startLoading(): void {
    this.loading$.next(true);
  }

  public finishedLoading(): void {
    this.loading$.next(false);
  }
}
