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

import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, filter, map } from 'rxjs/operators';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';

export abstract class AbstractInfrastrukturenRoutingService {
  private selectedInfrastrukturId$$: BehaviorSubject<number | null>;

  constructor(
    protected router: Router,
    protected infrastrukturArt: Infrastruktur
  ) {
    this.selectedInfrastrukturId$$ = new BehaviorSubject(this.getIdFromRoute());
    this.router.events
      .pipe(
        filter(ev => ev instanceof NavigationEnd),
        map(() => this.getIdFromRoute()),
        distinctUntilChanged()
      )
      .subscribe(id => this.selectedInfrastrukturId$$.next(id));
  }

  public getIdFromRoute(): number | null {
    const match = new RegExp(`${this.infrastrukturArt.pathElement}/(\\d+)($|/|\\?)`).exec(this.router.url);

    if (!match || !match[1] || isNaN(Number(match[1]))) {
      return null;
    }
    return Number(match[1]);
  }

  public get selectedInfrastrukturId$(): Observable<number | null> {
    return this.selectedInfrastrukturId$$.asObservable();
  }

  public get selectedInfrastrukturId(): number | null {
    return this.selectedInfrastrukturId$$.value;
  }

  public toInfrastrukturEditor(id: number): void {
    this.router.navigate(this.getInfrastrukturenEditorRoute(id), {
      queryParamsHandling: 'merge',
    });
  }

  public getInfrastrukturenEditorRoute(id: number): string[] {
    return [VIEWER_ROUTE, this.infrastrukturArt.pathElement, id.toString()];
  }
}
