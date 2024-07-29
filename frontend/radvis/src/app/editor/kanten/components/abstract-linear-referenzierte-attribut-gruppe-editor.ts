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
import { UntypedFormArray, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { AbstractAttributGruppeEditor } from 'src/app/editor/kanten/components/abstract-attribut-gruppe-editor';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { LinearReferenzierteAttribute } from 'src/app/editor/kanten/models/linear-referenzierte-attribute';
import { fillFormWithMultipleValues } from 'src/app/editor/kanten/services/fill-form-with-multiple-values';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { readEqualValuesFromForm } from 'src/app/editor/kanten/services/read-equal-values-from-form';
import { SelectElementEvent } from 'src/app/shared/components/lineare-referenzierung-layer/lineare-referenzierung-layer.component';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { VersionierteEntitaet } from 'src/app/shared/models/versionierte-entitaet';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

export abstract class AbstractLinearReferenzierteAttributGruppeEditor<
  A extends LinearReferenzierteAttribute,
  AG extends VersionierteEntitaet,
> implements DiscardableComponent
{
  public NICHT_BEARBEITBAR_HINWEIS = AbstractAttributGruppeEditor.NICHT_BEARBEITBAR_HINWEIS;

  public showRadNetzHinweis = false;
  public editingAllowed = true;

  public isFetching = false;
  public currentSelektion: KantenSelektion[] | null = null;
  public hasKanten$: Observable<boolean>;

  // Hält nur die gerade im Editor angezeigten Attribute
  public displayedAttributeformGroup: UntypedFormGroup;
  // Hält die linearen Referenzen. Verbunden mit den LineareReferenzControls
  public lineareReferenzenFormArray: UntypedFormArray = new UntypedFormArray([]);

  protected subscriptions: Subscription[] = [];

  // Hält die modifizierten Attribute für jede selektierte Kante in Schwebe
  protected currentAttributgruppen: AG[] = [];

  protected areAttributesPristine = true;

  constructor(
    protected changeDetectorRef: ChangeDetectorRef,
    protected notifyUserService: NotifyUserService,
    protected kantenSelektionService: KantenSelektionService,
    protected benutzerDetailsService: BenutzerDetailsService
  ) {
    this.kantenSelektionService.registerForDiscardGuard(this);
    this.hasKanten$ = kantenSelektionService.selektierteKanten$.pipe(map(kanten => kanten.length > 0));
    this.displayedAttributeformGroup = this.createDisplayedAttributeFormGroup();
    this.subscriptions.push(
      this.displayedAttributeformGroup.valueChanges.subscribe(() => {
        this.updateCurrentAttributgruppenWithAttribute(readEqualValuesFromForm(this.displayedAttributeformGroup));
        this.areAttributesPristine = false;
      })
    );
    this.subscriptions.push(
      this.lineareReferenzenFormArray.valueChanges.subscribe((value: LinearReferenzierterAbschnitt[][]) =>
        this.updateCurrentAttributgruppenWithLineareReferenzen(value)
      )
    );
  }

  // Ueberschrieben im KantenFuehrungsformEditorComponent -> Beim Umbenennen aufpassen!
  public get pristine(): boolean {
    return this.areAttributesPristine && this.lineareReferenzenFormArray.pristine;
  }

  public getLineareReferenzenFormControlAt(index: number): UntypedFormControl {
    return this.lineareReferenzenFormArray.at(index) as UntypedFormControl;
  }

  public onSelectLinearesSegment(event: SelectElementEvent, kanteId: number, kantenSeite?: KantenSeite): void {
    this.kantenSelektionService.select(kanteId, event.additiv, kantenSeite, event.index);
  }

  public onDeselectLinearesSegment(index: number, kanteId: number, kantenSeite?: KantenSeite): void {
    this.kantenSelektionService.deselect(kanteId, kantenSeite, index);
  }

  onSave(): void {
    // verhindern, dass unnötig gespeichert wird, wenn es keine Änderungen gibt, z.B. bei Double-Clicks
    if (this.pristine) {
      return;
    }
    this.isFetching = true;
    this.saveAttributgruppe(this.currentAttributgruppen)
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
    this.kantenSelektionService.resetSelectionToConsistentState();
    if (this.editingAllowed) {
      this.enableControls();
    }
    this.resetLineareReferenzenFormArrays(this.kantenSelektionService.selektion);
    this.resetCurrentAttributgruppen(this.kantenSelektionService.selektion);
    this.areAttributesPristine = true;
    if (!this.editingAllowed) {
      this.disableControls();
    }
    this.resetDisplayedAttribute(this.kantenSelektionService.selektion);
    this.changeDetectorRef.markForCheck();
  }

  onClose(): void {
    this.kantenSelektionService.cleanUp(true);
  }

  canDiscard(): boolean {
    return this.pristine;
  }

  protected subscribeToKantenSelektion(): void {
    this.subscriptions.push(
      this.kantenSelektionService.selektion$.subscribe(newSelektion => {
        if (newSelektion) {
          if (!this.hasOnlySegmentSelectionChanged(newSelektion)) {
            // Discard Guard war aktiv, daher kann jetzt sicher aus den geladenen Kanten gelesen werden ohne WiP zu überschreiben
            const noRadNetzKanteSelected = !newSelektion.some(
              kantenSelektion => kantenSelektion.kante.quelle === QuellSystem.RadNETZ
            );
            this.showRadNetzHinweis = !noRadNetzKanteSelected;
            // TODO: Hier die Überprüfung im Backend nachziehen
            // const allKantenInZustaendigkeitsbereich = newSelektion.every(
            //   kantenSelektion => kantenSelektion.kante.liegtInZustaendigkeitsbereich
            // );
            const allKantenInZustaendigkeitsbereich = true;
            this.editingAllowed =
              noRadNetzKanteSelected &&
              (allKantenInZustaendigkeitsbereich || this.benutzerDetailsService.canEditGesamtesNetz());
            if (this.editingAllowed) {
              this.enableControls();
            }
            this.resetLineareReferenzenFormArrays(newSelektion);
            this.resetCurrentAttributgruppen(newSelektion);
            this.areAttributesPristine = true;
            if (!this.editingAllowed) {
              this.disableControls();
            }
          }
          this.resetDisplayedAttribute(newSelektion);
          this.changeDetectorRef.markForCheck();
          this.currentSelektion = newSelektion;
        }
      })
    );
  }

  protected resetFormArray<T>(array: UntypedFormArray, values: T[]): void {
    array.clear();
    values.forEach(v => {
      array.push(new UntypedFormControl(v), {
        emitEvent: false,
      });
    });
    array.markAsPristine();
  }

  // Ueberschrieben von KanteZustaendigkeitEditor, beim Umbenennen beachten!
  protected resetDisplayedAttribute(selektion: KantenSelektion[]): void {
    if (selektion.length === 0) {
      this.displayedAttributeformGroup.reset(undefined, { emitEvent: false });
      return;
    }
    fillFormWithMultipleValues(this.displayedAttributeformGroup, this.getAttributeForSelektion(selektion), false);
  }

  // Ueberschrieben im KantenFuehrungsformEditorComponent -> Beim Umbenennen aufpassen!
  protected disableControls(): void {
    this.displayedAttributeformGroup.disable({ emitEvent: false });
    this.lineareReferenzenFormArray.disable({ emitEvent: false });
  }

  // Ueberschrieben im KantenFuehrungsformEditorComponent -> Beim Umbenennen aufpassen!
  protected enableControls(): void {
    this.displayedAttributeformGroup.enable({ emitEvent: false });
    this.lineareReferenzenFormArray.enable({ emitEvent: false });
  }

  private resetCurrentAttributgruppen(newSelektion: KantenSelektion[]): void {
    this.currentAttributgruppen = [];
    newSelektion.forEach(kantenSelektion => {
      const attributGruppe = this.getAttributGruppeFrom(kantenSelektion.kante);
      const deepCopy = JSON.parse(JSON.stringify(attributGruppe)) as AG;
      this.currentAttributgruppen.push(deepCopy);
    });
  }

  private hasOnlySegmentSelectionChanged(newSelektion: KantenSelektion[]): boolean {
    if (this.currentSelektion) {
      if (this.currentSelektion.length !== newSelektion.length) {
        return false;
      }
      return this.currentSelektion.every(
        (kantenSelektion: KantenSelektion, index: number) => kantenSelektion.kante.id === newSelektion[index].kante.id
      );
    }
    return false;
  }

  protected abstract updateCurrentAttributgruppenWithLineareReferenzen(
    newLineareReferenzenArrays: LinearReferenzierterAbschnitt[][]
  ): void;

  protected abstract updateCurrentAttributgruppenWithAttribute(changedAttributePartial: { [id: string]: any }): void;

  protected abstract resetLineareReferenzenFormArrays(newSelektion: KantenSelektion[]): void;

  protected abstract saveAttributgruppe(attributgruppen: AG[]): Promise<Kante[]>;

  protected abstract createDisplayedAttributeFormGroup(): UntypedFormGroup;

  protected abstract getAttributGruppeFrom(kante: Kante): AG;

  protected abstract getAttributeForSelektion(selektion: KantenSelektion[]): A[];
}
