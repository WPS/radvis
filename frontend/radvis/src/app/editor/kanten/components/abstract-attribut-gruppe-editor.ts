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

import { ChangeDetectorRef } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { DiscardGuard } from 'src/app/shared/services/discard-guard.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

export abstract class AbstractAttributGruppeEditor implements DiscardGuard {
  public static readonly NICHT_BEARBEITBAR_HINWEIS =
    'In diesem Bereich ist das RadNETZ noch nicht zur Bearbeitung freigegeben';
  public NICHT_BEARBEITBAR_HINWEIS = AbstractAttributGruppeEditor.NICHT_BEARBEITBAR_HINWEIS;
  public isFetching = false;
  public hasKanten$: Observable<boolean>;
  public multipleKantenSelected$: Observable<boolean>;

  protected constructor(
    protected netzService: NetzService,
    protected errorHandlingService: ErrorHandlingService,
    protected notifyUserService: NotifyUserService,
    protected changeDetectorRef: ChangeDetectorRef,
    protected editorRoutingService: EditorRoutingService,
    protected kanteSelektionService: KantenSelektionService
  ) {
    this.kanteSelektionService.registerForDiscardGuard(this);
    this.hasKanten$ = this.kanteSelektionService.selektierteKanten$.pipe(
      map(kanten => kanten.length > 0),
      distinctUntilChanged()
    );
    this.multipleKantenSelected$ = this.kanteSelektionService.selektierteKanten$.pipe(map(kanten => kanten.length > 1));
  }

  get currentKante(): Kante {
    return this.getCurrentKante(this.kanteSelektionService.selektierteKanten);
  }

  getCurrentKante(kanten: Kante[]): Kante {
    return kanten[kanten.length - 1];
  }

  onSave(): void {
    // verhindern, dass unnötig gespeichert wird, wenn es keine Änderungen gibt, z.B. bei Double-Clicks
    if (this.getForm().pristine) {
      return;
    }

    // Hinweis, wenn Daten falsch eingegeben wurden
    if (this.getForm().invalid) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    this.isFetching = true;

    this.save()
      .then(savedKanten => {
        this.kanteSelektionService.updateKanten(savedKanten);
        this.resetForm(this.kanteSelektionService.selektion);
        this.notifyUserService.inform('Kanten wurden erfolgreich gespeichert.');
      })
      .finally(() => {
        this.isFetching = false;
        this.changeDetectorRef.markForCheck();
        this.onAfterSave();
      });
  }

  onReset(): void {
    this.resetForm(this.kanteSelektionService.selektion);
  }

  onClose(): void {
    this.kanteSelektionService.cleanUp(true);
  }

  canDiscard(): boolean {
    return this.getForm().pristine;
  }

  protected onAfterSave(): void {
    // Kann in implementierenden Editoren überschrieben werden, um nach dem Speichervorgang etwas zu tun
  }

  protected abstract resetForm(selektion: KantenSelektion[]): void;

  protected abstract save(): Promise<Kante[]>;

  protected abstract getForm(): AbstractControl;
}
