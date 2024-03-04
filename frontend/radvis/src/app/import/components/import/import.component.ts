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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute, ChildActivationEnd, NavigationEnd, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { ImportStep } from 'src/app/import/models/import-step';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { LadeZustandService } from 'src/app/shared/services/lade-zustand.service';

@Component({
  selector: 'rad-import',
  templateUrl: './import.component.html',
  styleUrl: './import.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportComponent implements OnDestroy {
  @ViewChild('toolContainer', { read: ElementRef })
  toolContainer: ElementRef | undefined;

  ladend$: Observable<boolean>;

  public steps: Map<number, ImportStep> = new Map();

  public activeStepIndex = 0;
  public fehlerprotokolleEnabled: boolean;
  public fehlerprotokollZIndex = EditorLayerZindexConfig.FEHLERPROTOKOLL_LAYER;

  private subscriptions: Subscription[] = [];

  constructor(
    router: Router,
    changeDetector: ChangeDetectorRef,
    activatedRoute: ActivatedRoute,
    featureTogglzService: FeatureTogglzService,
    ladeZustandService: LadeZustandService
  ) {
    this.fehlerprotokolleEnabled = featureTogglzService.fehlerprotokoll;
    this.ladend$ = ladeZustandService.isLoading$;

    this.subscriptions.push(
      router.events
        .pipe(
          filter(e => e instanceof ChildActivationEnd && e.snapshot.component === activatedRoute.component),
          map(ev => (ev as ChildActivationEnd).snapshot.firstChild?.data)
        )
        .subscribe(childData => (this.steps = childData?.steps))
    );

    this.subscriptions.push(
      router.events
        .pipe(
          filter(ev => ev instanceof NavigationEnd),
          map(() => [...this.steps.entries()].find(([, step]) => router.url.includes(step.path))?.[0] || -1)
        )
        .subscribe(activeStepIndex => {
          this.activeStepIndex = activeStepIndex;
          changeDetector.markForCheck();
        })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  @HostListener('document:keydown.control.alt.shift.d')
  onShortcut(): void {
    this.toolContainer?.nativeElement.querySelector('button, input, mat-select')?.focus();
  }
}
