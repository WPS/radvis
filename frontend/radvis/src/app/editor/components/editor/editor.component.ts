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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NotifyGeometryChangedService } from 'src/app/editor/kanten/services/notify-geometry-changed.service';
import { LadeZustandService } from 'src/app/shared/services/lade-zustand.service';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'rad-editor',
  templateUrl: './editor.component.html',
  styleUrls: ['./editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NotifyGeometryChangedService],
  standalone: false,
})
export class EditorComponent {
  ladend$: Observable<boolean>;

  constructor(
    public activatedRoute: ActivatedRoute,
    private changeDetectorRef: ChangeDetectorRef,
    private kantenSelektionService: KantenSelektionService,
    ladeZustandService: LadeZustandService
  ) {
    this.kantenSelektionService.selektion$.subscribe(() => {
      this.changeDetectorRef.markForCheck();
    });
    this.ladend$ = ladeZustandService.isLoading$;
  }

  @HostListener('window:beforeunload')
  public onBeforeUnload(): boolean {
    if (environment.production) {
      return this.kantenSelektionService.canDiscard();
    }
    return true;
  }
}
