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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { DiscardGuard } from 'src/app/shared/services/discard-guard.service';
import { AddKommentarCommand } from 'src/app/viewer/kommentare/models/add-kommentar-command';
import { Kommentar } from 'src/app/viewer/kommentare/models/kommentar';
import { KommentarService } from 'src/app/viewer/kommentare/services/kommentar.service';

@Component({
  selector: 'rad-kommentar-liste',
  templateUrl: './kommentar-liste.component.html',
  styleUrls: ['./kommentar-liste.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KommentarListeComponent implements OnDestroy, DiscardGuard {
  public static readonly KOMMENTARLISTE_DATA_KEY = 'kommentare';

  @ViewChild('kommentarHinzufuegen')
  private kommentarHinzufuegen: DiscardGuard | undefined;

  @ViewChild('kommentarListeContainer')
  private kommentarListeContainer: ElementRef | undefined;

  public kommentarListe: Kommentar[] = [];

  subscriptions: Subscription[] = [];

  constructor(
    activatedRoute: ActivatedRoute,
    private kommentarService: KommentarService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.subscriptions.push(
      activatedRoute.data.subscribe(data => {
        this.kommentarListe = this.getSortedKommentarListe(data[KommentarListeComponent.KOMMENTARLISTE_DATA_KEY].liste);
        this.changeDetectorRef.markForCheck();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  sendKommentar(kommentar: string): void {
    if (!this.kommentarListe) {
      return;
    }
    const command: AddKommentarCommand = {
      kommentarText: kommentar,
    };
    this.kommentarService.addKommentar(command).then(updatedKommentarListe => {
      this.kommentarListe = this.getSortedKommentarListe(updatedKommentarListe);
      this.changeDetectorRef.detectChanges();
      if (this.kommentarListeContainer) {
        this.kommentarListeContainer.nativeElement.scrollTop = this.kommentarListeContainer.nativeElement.scrollHeight;
      }
    });
  }

  canDiscard(): boolean {
    return this.kommentarHinzufuegen?.canDiscard() ?? true;
  }

  private getSortedKommentarListe(kommentarListe: Kommentar[]): Kommentar[] {
    return [...kommentarListe].sort(
      (k1: Kommentar, k2: Kommentar) => new Date(k1.datum).getTime() - new Date(k2.datum).getTime()
    );
  }
}
