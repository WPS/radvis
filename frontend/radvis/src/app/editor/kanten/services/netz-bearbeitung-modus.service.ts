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
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, filter, map } from 'rxjs/operators';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';

@Injectable({
  providedIn: 'root',
})
export class NetzBearbeitungModusService {
  route$: BehaviorSubject<string>;

  constructor(
    router: Router,
    private routingService: EditorRoutingService
  ) {
    this.route$ = new BehaviorSubject(router.url.toString());
    router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        map(() => router.url.toString()),
        distinctUntilChanged()
      )
      .subscribe(url => {
        this.route$.next(url);
      });
  }

  public isKantenSubEditorAktiv(url: string): boolean {
    return (
      url.includes(EditorRoutingService.EDITOR_KANTEN_ROUTE) &&
      !url.includes(this.routingService.getKantenCreatorRoute())
    );
  }

  isKantenCreatorAktiv(): Observable<boolean> {
    return this.route$.pipe(
      map(url => {
        return url.includes(this.routingService.getKantenCreatorRoute());
      })
    );
  }

  getAktiveKantenGruppe(): Observable<AttributGruppe | null> {
    return this.route$.pipe(
      map(url => {
        const activeAttributgruppe = AttributGruppe.values.find(value => url.includes(value));
        return activeAttributgruppe || null;
      }),
      distinctUntilChanged()
    );
  }
}
