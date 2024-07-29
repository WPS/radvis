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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { MAT_CHECKBOX_DEFAULT_OPTIONS, MatCheckboxDefaultOptions } from '@angular/material/checkbox';
import { Subscription } from 'rxjs';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import {
  KantenHoverEvent,
  KantenSelektionHoverService,
} from 'src/app/editor/kanten/services/kanten-selektion-hover.service';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';

@Component({
  selector: 'rad-kanten-selektion-tabelle',
  templateUrl: './kanten-selektion-tabelle.component.html',
  styleUrls: ['./kanten-selektion-tabelle.component.scss', '../lineare-referenz-tabelle.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: MAT_CHECKBOX_DEFAULT_OPTIONS, useValue: { clickAction: 'noop' } as MatCheckboxDefaultOptions },
  ],
})
export class KantenSelektionTabelleComponent implements OnDestroy, OnInit {
  @Input()
  selektion: KantenSelektion[] = [];

  public LINKS = KantenSeite.LINKS;
  public RECHTS = KantenSeite.RECHTS;
  hoveredKante: { kanteId: number; kantenSeite?: KantenSeite } | null = null;
  private subscriptions: Subscription[] = [];

  constructor(
    private kantenSelektionService: KantenSelektionService,
    private kantenSelektionHoverService: KantenSelektionHoverService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  isHovered(kanteId: number, kantenSeite?: KantenSeite): boolean {
    return this.hoveredKante?.kanteId === kanteId && this.hoveredKante?.kantenSeite === kantenSeite;
  }

  ngOnInit(): void {
    this.subscriptions.push(this.kantenSelektionHoverService.hoverKante$.subscribe(event => this.onHover(event)));
    this.subscriptions.push(this.kantenSelektionHoverService.unhoverKante$.subscribe(() => this.onUnhover()));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  onTabelleHover(hovered: boolean, kanteId: number, kantenSeite?: KantenSeite): void {
    if (hovered) {
      this.kantenSelektionHoverService.notifyHover({ kanteId, kantenSeite: kantenSeite });
    } else {
      this.kantenSelektionHoverService.notifyUnhover();
    }
  }

  onSelect(kanteId: number, kantenSeite?: KantenSeite): void {
    const clickedKantenSelektion = this.selektion.find(
      kantenSelektion => kantenSelektion.kante.id === kanteId
    ) as KantenSelektion;
    if (kantenSeite) {
      if (clickedKantenSelektion.istSeiteSelektiert(kantenSeite)) {
        this.kantenSelektionService.deselect(kanteId, kantenSeite);
      } else {
        this.kantenSelektionService.select(kanteId, true, kantenSeite);
      }
    } else {
      this.kantenSelektionService.deselect(kanteId);
    }
  }

  private onHover(event: KantenHoverEvent): void {
    // Nicht unnötig viel change detection betreiben
    if (this.hoveredKante?.kanteId !== event.kanteId || this.hoveredKante.kantenSeite !== event.kantenSeite) {
      this.hoveredKante = { kanteId: event.kanteId, kantenSeite: event.kantenSeite };
      this.changeDetectorRef.detectChanges();
    }
  }

  private onUnhover(): void {
    // Nicht unnötig viel change detection betreiben
    if (this.hoveredKante !== null) {
      this.hoveredKante = null;
      this.changeDetectorRef.detectChanges();
    }
  }
}
