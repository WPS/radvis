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

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatTabNavPanel } from '@angular/material/tabs';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';

@Component({
  selector: 'rad-editor-menu',
  templateUrl: './editor-menu.component.html',
  styleUrls: ['./editor-menu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditorMenuComponent {
  @Input()
  public tabPanel!: MatTabNavPanel;

  AttributGruppe = AttributGruppe;

  kantenRoute: string;
  knotenRoute: string;

  kantenAktiv$ = new BehaviorSubject<boolean>(false);
  knotenAktiv$ = new BehaviorSubject<boolean>(false);
  anpassungenAktiv$ = new BehaviorSubject<boolean>(false);

  constructor(
    private routingService: EditorRoutingService,
    router: Router
  ) {
    this.kantenRoute = this.routingService.getKantenRoute();
    this.knotenRoute = this.routingService.getKnotenRoute();

    router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        map(() => router.url.split('?')[0])
      )
      .subscribe(url => {
        this.kantenAktiv$.next(url.includes(this.kantenRoute));
        this.knotenAktiv$.next(url.includes(this.knotenRoute));
      });
  }
}
