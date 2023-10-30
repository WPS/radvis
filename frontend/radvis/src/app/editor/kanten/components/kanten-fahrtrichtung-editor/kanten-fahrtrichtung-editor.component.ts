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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { AbstractAttributGruppeEditor } from 'src/app/editor/kanten/components/abstract-attribut-gruppe-editor';
import { FahrtrichtungAttributGruppe } from 'src/app/editor/kanten/models/fahrtrichtung-attributgruppe';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { Richtung } from 'src/app/editor/kanten/models/richtung';
import { SaveFahrtrichtungAttributGruppeCommand } from 'src/app/editor/kanten/models/save-fahrtrichtung-attribut-gruppe-command';
import { fillFormWithMultipleValues } from 'src/app/editor/kanten/services/fill-form-with-multiple-values';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { readEqualValuesFromForm } from 'src/app/editor/kanten/services/read-equal-values-from-form';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardGuard } from 'src/app/shared/services/discard-guard.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

type GenericRichtung = {
  richtung: Richtung;
};

@Component({
  selector: 'rad-kanten-fahrtrichtung-editor',
  templateUrl: './kanten-fahrtrichtung-editor.component.html',
  styleUrls: [
    './kanten-fahrtrichtung-editor.component.scss',
    '../../../../form-elements/components/attribute-editor/attribut-editor.scss',
    '../abstract-attribut-gruppe-editor-mit-auswahl.scss',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KantenFahrtrichtungEditorComponent implements DiscardGuard, OnInit, OnDestroy {
  public NICHT_BEARBEITBAR_HINWEIS = AbstractAttributGruppeEditor.NICHT_BEARBEITBAR_HINWEIS;

  richtungOptions = Richtung.options;
  displayedAttributeformGroup: FormGroup;
  hasKanten$: Observable<boolean>;
  showRadNetzHinweis = false;
  editingAllowed = true;
  isFetching = false;
  currentSelektion: KantenSelektion[] | null = null;
  areAttributesPristine = true;

  // Hält die modifizierten Attribute für jede selektierte Kante in Schwebe
  private currentFahrtrichtungAttributGruppen: FahrtrichtungAttributGruppe[] = [];

  private subscriptions: Subscription[] = [];

  constructor(
    private netzService: NetzService,
    private changeDetectorRef: ChangeDetectorRef,
    private errorHandlingService: ErrorHandlingService,
    private notifyUserService: NotifyUserService,
    private kantenSelektionService: KantenSelektionService,
    private benutzerDetailsService: BenutzerDetailsService
  ) {
    this.kantenSelektionService.registerForDiscardGuard(this);
    this.hasKanten$ = kantenSelektionService.selektierteKanten$.pipe(map(kanten => kanten.length > 0));

    this.displayedAttributeformGroup = new FormGroup({
      richtung: new FormControl(null),
    });
  }

  ngOnInit(): void {
    this.subscriptions.push(
      this.kantenSelektionService.selektion$.subscribe(newSelektion => {
        if (!this.onlySeitenSelektionChanged(newSelektion)) {
          // Discard Guard war aktiv, daher kann jetzt sicher aus den geladenen Kanten gelesen werden ohne WiP zu überschreiben
          const noRadNetzKanteSelected = !newSelektion.some(
            kantenSelektion => kantenSelektion.kante.quelle === QuellSystem.RadNETZ
          );
          this.showRadNetzHinweis = !noRadNetzKanteSelected;
          const allKantenInZustaendigkeitsbereich = newSelektion.every(
            kantenSelektion => kantenSelektion.kante.liegtInZustaendigkeitsbereich
          );
          this.editingAllowed =
            noRadNetzKanteSelected &&
            (allKantenInZustaendigkeitsbereich || this.benutzerDetailsService.canEditGesamtesNetz());
          if (this.editingAllowed) {
            this.displayedAttributeformGroup.enable({ emitEvent: false });
          } else {
            this.displayedAttributeformGroup.disable({ emitEvent: false });
          }

          this.resetCurrentFahrtrichtungAttributGruppen(newSelektion);
          this.areAttributesPristine = true;
        }

        this.resetDisplayedAttribute(newSelektion);

        this.changeDetectorRef.markForCheck();

        this.currentSelektion = newSelektion;
      })
    );

    this.subscriptions.push(
      this.displayedAttributeformGroup.valueChanges.subscribe(() => {
        this.updateAttribute();
        this.areAttributesPristine = false;
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  canDiscard(): boolean {
    return this.areAttributesPristine;
  }

  onSave(): void {
    // verhindern, dass unnötig gespeichert wird
    if (this.areAttributesPristine) {
      return;
    }

    this.isFetching = true;

    const commands: SaveFahrtrichtungAttributGruppeCommand[] = this.currentFahrtrichtungAttributGruppen.map(gruppe => {
      const associatedKantenSelektion = this.currentSelektion?.find(
        kantenSelektion => kantenSelektion.kante.fahrtrichtungAttributGruppe.id === gruppe.id
      ) as KantenSelektion;

      return {
        gruppenId: gruppe.id,
        gruppenVersion: gruppe.version,
        fahrtrichtungLinks: gruppe.fahrtrichtungLinks,
        fahrtrichtungRechts: gruppe.fahrtrichtungRechts,
        kanteId: associatedKantenSelektion.kante.id,
      } as SaveFahrtrichtungAttributGruppeCommand;
    });

    this.netzService
      .saveFahrtrichtungAttributgruppe(commands)
      .then(savedKanten => {
        this.kantenSelektionService.updateKanten(savedKanten);
        this.onReset();
        this.notifyUserService.inform('Kanten wurden erfolgreich gespeichert.');
      })
      .finally(() => {
        this.isFetching = false;
        this.changeDetectorRef.markForCheck();
      });
  }

  onReset(): void {
    if (this.editingAllowed) {
      this.displayedAttributeformGroup.enable({ emitEvent: false });
    } else {
      this.displayedAttributeformGroup.disable({ emitEvent: false });
    }

    const selektion = this.kantenSelektionService.selektion;
    this.resetCurrentFahrtrichtungAttributGruppen(selektion);
    this.areAttributesPristine = true;
    this.resetDisplayedAttribute(selektion);
  }

  onClose(): void {
    this.kantenSelektionService.cleanUp(true);
  }

  private onlySeitenSelektionChanged(newSelektion: KantenSelektion[]): boolean {
    if (!this.currentSelektion || this.currentSelektion.length !== newSelektion.length) {
      return false;
    }

    return this.currentSelektion.every((kantenSelektion, i) => kantenSelektion.kante.id === newSelektion[i].kante.id);
  }

  private resetCurrentFahrtrichtungAttributGruppen(newSelektion: KantenSelektion[]): void {
    this.currentFahrtrichtungAttributGruppen = newSelektion.map(kantenSelektion => {
      const fahrtrichtungAttributGruppe = kantenSelektion.kante.fahrtrichtungAttributGruppe;
      // deep copy
      return JSON.parse(JSON.stringify(fahrtrichtungAttributGruppe)) as FahrtrichtungAttributGruppe;
    });
  }

  private resetDisplayedAttribute(selektion: KantenSelektion[]): void {
    if (selektion.length === 0) {
      this.displayedAttributeformGroup.reset(undefined, { emitEvent: false });
      return;
    }

    const selectedFahrtrichtungen: GenericRichtung[] = [];
    selektion.forEach((kantenSelektion, kantenIndex) => {
      if (kantenSelektion.istSeiteSelektiert(Seitenbezug.LINKS)) {
        selectedFahrtrichtungen.push({
          richtung: this.currentFahrtrichtungAttributGruppen[kantenIndex].fahrtrichtungLinks,
        });
      }
      if (kantenSelektion.istSeiteSelektiert(Seitenbezug.RECHTS)) {
        selectedFahrtrichtungen.push({
          richtung: this.currentFahrtrichtungAttributGruppen[kantenIndex].fahrtrichtungRechts,
        });
      }
    });

    fillFormWithMultipleValues(this.displayedAttributeformGroup, selectedFahrtrichtungen, false);
  }

  private updateAttribute(): void {
    this.currentSelektion?.forEach(kantenSelektion => {
      const attributGruppeToChange = this.currentFahrtrichtungAttributGruppen.find(
        gruppe => gruppe.id === kantenSelektion.kante.fahrtrichtungAttributGruppe.id
      ) as FahrtrichtungAttributGruppe;

      if (kantenSelektion.istSeiteSelektiert(Seitenbezug.LINKS)) {
        const { richtung } = readEqualValuesFromForm(this.displayedAttributeformGroup);
        if (richtung) {
          attributGruppeToChange.fahrtrichtungLinks = richtung;
        }
      }
      if (kantenSelektion.istSeiteSelektiert(Seitenbezug.RECHTS)) {
        const { richtung } = readEqualValuesFromForm(this.displayedAttributeformGroup);
        if (richtung) {
          attributGruppeToChange.fahrtrichtungRechts = richtung;
        }
      }
    });
  }
}
