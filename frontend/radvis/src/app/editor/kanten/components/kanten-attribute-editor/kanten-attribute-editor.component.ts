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
import { AbstractControl, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MAT_SLIDE_TOGGLE_DEFAULT_OPTIONS, MatSlideToggleDefaultOptions } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarRef, TextOnlySnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { delay, filter } from 'rxjs/operators';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { AbstractAttributGruppeEditor } from 'src/app/editor/kanten/components/abstract-attribut-gruppe-editor';
import { Beleuchtung } from 'src/app/editor/kanten/models/beleuchtung';
import { ChangeSeitenbezugCommand } from 'src/app/editor/kanten/models/change-seitenbezug-command';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenAttributGruppe } from 'src/app/editor/kanten/models/kanten-attribut-gruppe';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { SaveKantenAttributGruppeCommand } from 'src/app/editor/kanten/models/save-kanten-attribut-gruppe-command';
import { Status } from 'src/app/editor/kanten/models/status';
import { StrassenkategorieRIN } from 'src/app/editor/kanten/models/strassenkategorie-rin';
import { StrassenquerschnittRASt06 } from 'src/app/editor/kanten/models/strassenquerschnittrast06';
import { Umfeld } from 'src/app/editor/kanten/models/umfeld';
import { WegeNiveau } from 'src/app/editor/kanten/models/wege-niveau';
import {
  fillFormWithMultipleValues,
  hasMultipleValues,
} from 'src/app/editor/kanten/services/fill-form-with-multiple-values';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NotifyGeometryChangedService } from 'src/app/editor/kanten/services/notify-geometry-changed.service';
import { readEqualValuesFromForm } from 'src/app/editor/kanten/services/read-equal-values-from-form';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { IstStandard } from 'src/app/shared/models/ist-standard';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { NetzklasseFlatUndertermined } from 'src/app/shared/models/netzklasse-flat';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzklassenConverterService } from 'src/app/shared/services/netzklassen-converter.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import invariant from 'tiny-invariant';

interface IstStandardFlat {
  radnetzStartstandard: boolean | UndeterminedValue;
  radnetzZielstandard: boolean | UndeterminedValue;
  radschnellverbindung: boolean | UndeterminedValue;
  basisstandard: boolean | UndeterminedValue;
  radvorrangrouten: boolean | UndeterminedValue;
}

@Component({
  selector: 'rad-kanten-attribute-editor',
  templateUrl: './kanten-attribute-editor.component.html',
  styleUrls: [
    './kanten-attribute-editor.component.scss',
    '../../../../form-elements/components/attribute-editor/attribut-editor.scss',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: MAT_SLIDE_TOGGLE_DEFAULT_OPTIONS,
      // Toggles ändern damit erst ihren State, sobald [checked] an ihnen gesetzt wird und nicht sofort bei Click
      useValue: { disableToggleValue: true } as MatSlideToggleDefaultOptions,
    },
  ],
  standalone: false,
})
export class KantenAttributeEditorComponent extends AbstractAttributGruppeEditor implements OnInit, OnDestroy {
  private static readonly ZWISCHENABLAGE_KEY = 'kanteAttributeZwischenablage';

  public wegeniveauOptions = WegeNiveau.options;
  public beleuchtungsOptions = Beleuchtung.options;
  public umfeldOptions = Umfeld.options;
  public strassenkategorieRINOptions = StrassenkategorieRIN.options;
  public strassenquerschnittRASt06Options = StrassenquerschnittRASt06.options;
  public alleOrganisationenOptions: Promise<Verwaltungseinheit[]> = Promise.resolve([]);
  public statusOptions = Status.options;
  public gemeindeOptions: Promise<Verwaltungseinheit[]> = Promise.resolve([]);

  public readonly kommentarMaxLength = 2000;

  formGroup: UntypedFormGroup;
  netzklassenSubscription: Subscription | undefined;
  changeSeitenbezug = false;
  seitenbezogenUndetermined = false;
  seitenbezogen = false;
  showRadNetzHinweis = false;
  copyDisabled = false;
  hasClipboard = false;

  private subscriptions: Subscription[] = [];
  private copyPasteHinweis: MatSnackBarRef<TextOnlySnackBar> | undefined;
  copiedKanteId: number | null = null;

  constructor(
    netzService: NetzService,
    private organisationenService: OrganisationenService,
    changeDetectorRef: ChangeDetectorRef,
    editorRoutingService: EditorRoutingService,
    errorHandlingService: ErrorHandlingService,
    notifyUserService: NotifyUserService,
    kanteSelektionService: KantenSelektionService,
    private notifyGeometryChangedService: NotifyGeometryChangedService,
    private dialog: MatDialog,
    private benutzerDetailsService: BenutzerDetailsService,
    private snackbar: MatSnackBar
  ) {
    super(
      netzService,
      errorHandlingService,
      notifyUserService,
      changeDetectorRef,
      editorRoutingService,
      kanteSelektionService
    );

    this.formGroup = this.createForm();

    this.alleOrganisationenOptions = this.organisationenService.getOrganisationen();
    this.gemeindeOptions = this.organisationenService.getGemeinden();

    this.hasClipboard = Boolean(localStorage.getItem(KantenAttributeEditorComponent.ZWISCHENABLAGE_KEY));
    if (this.hasClipboard) {
      this.copiedKanteId = JSON.parse(localStorage.getItem(KantenAttributeEditorComponent.ZWISCHENABLAGE_KEY)!).id;
      this.showCopyPasteHinweis();
    }

    this.subscribeToGemeindeChanges();
  }

  get canDelete(): boolean {
    return (
      this.kanteSelektionService.selektierteKanten.length === 1 &&
      this.kanteSelektionService.selektierteKanten[0].loeschenErlaubt
    );
  }

  ngOnInit(): void {
    this.subscriptions.push(
      this.kanteSelektionService.selektion$.subscribe(selektion => {
        const allKantenInZustaendigkeitsbereich = selektion.every(
          kantenSelektion => kantenSelektion.kante.liegtInZustaendigkeitsbereich
        );

        if (selektion.length > 0) {
          const disableAll = !allKantenInZustaendigkeitsbereich && !this.benutzerDetailsService.canEditGesamtesNetz();
          const canEditRadnetzNetzklassen = this.benutzerDetailsService.canRadNetzVerlegen();

          this.resetAndDisable(selektion, disableAll, canEditRadnetzNetzklassen);
          this.changeDetectorRef.markForCheck();
        } else {
          this.resetAndDisable(selektion, false, true);
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.copyPasteHinweis?.dismiss();
  }

  onSeitenbezugChange(): void {
    const changeSeitenbezug = (): void => {
      this.changeSeitenbezug = true;
      this.seitenbezogen = !this.seitenbezogen;
      const commands: ChangeSeitenbezugCommand[] = this.kanteSelektionService.selektierteKanten.map(k => {
        return {
          id: k.id,
          version: k.kantenVersion,
          zweiseitig: this.seitenbezogen,
        };
      });
      this.netzService
        .updateSeitenbezug(commands)
        .then(kanten => {
          this.kanteSelektionService.updateKanten(kanten);
          this.notifyUserService.inform('Seitenbezug erfolgreich geändert');
          this.notifyGeometryChangedService.notify();
        })
        .catch(() => {
          this.resetForm(this.kanteSelektionService.selektion);
        })
        .finally(() => {
          this.changeSeitenbezug = false;
          this.changeDetectorRef.detectChanges();
        });
    };
    if (this.seitenbezogen) {
      const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
        data: {
          question:
            'Wollen Sie den Radweg wirklich auf eine Seite reduzieren? Alle Änderungen an der rechten Seite gehen damit verloren.',
          labelYes: 'Ja',
          labelNo: 'Nein',
        } as QuestionYesNo,
      });
      dialogRef.afterClosed().subscribe(yes => {
        if (yes) {
          changeSeitenbezug();
        }
      });
    } else {
      changeSeitenbezug();
    }
  }

  onCopy(): void {
    this.copiedKanteId = this.kanteSelektionService.selektierteKanten[0].id;
    localStorage.setItem(
      KantenAttributeEditorComponent.ZWISCHENABLAGE_KEY,
      JSON.stringify({ id: this.copiedKanteId, ...this.formGroup.value })
    );
    if (!this.hasClipboard) {
      this.showCopyPasteHinweis();
    }
    this.hasClipboard = true;
  }

  onPaste(): void {
    const values = localStorage.getItem(KantenAttributeEditorComponent.ZWISCHENABLAGE_KEY);
    if (!values) {
      this.notifyUserService.warn('Die Zwischenablage enthält keine Werte.');
      return;
    }
    this.formGroup.patchValue(JSON.parse(values));
    this.formGroup.markAsDirty();
  }

  onDelete(): void {
    invariant(this.canDelete);

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question:
          'Wollen Sie die Kante wirklich löschen? Durch das Löschen der Kante kann es zur Anpassung des Netzbezugs von anderen Objekten (z.B. Maßnahmen) kommen.',
        labelYes: 'Löschen',
        labelNo: 'Abbrechen',
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        this.netzService
          .deleteKante(this.currentKante.id)
          .then(() => {
            this.notifyUserService.inform('RadVIS-Kante erfolgreich gelöscht.');
            this.notifyGeometryChangedService.notify();
            this.onClose();
          })
          .catch(() => {
            this.resetForm(this.kanteSelektionService.selektion);
          })
          .finally(() => {
            this.changeDetectorRef.detectChanges();
          });
      }
    });
  }

  protected getForm(): AbstractControl {
    return this.formGroup;
  }

  protected resetForm(selektion: KantenSelektion[]): void {
    if (this.netzklassenSubscription) {
      this.netzklassenSubscription.unsubscribe();
    }

    if (selektion.length > 0) {
      fillFormWithMultipleValues(
        this.formGroup,
        selektion.map(kantenSelektion => {
          return {
            ...kantenSelektion.kante.kantenAttributGruppe,
            laengeBerechnet: kantenSelektion.kante.laengeBerechnet?.toFixed(2).replace('.', ',') || null,
          };
        })
      );

      fillFormWithMultipleValues(
        this.getNetzklassenFromGroup(),
        selektion.map(kantenSelektion =>
          NetzklassenConverterService.convertToFlat<NetzklasseFlatUndertermined>(
            kantenSelektion.kante.kantenAttributGruppe.netzklassen
          )
        )
      );

      fillFormWithMultipleValues(
        this.getIstStandardsFromGroup(),
        selektion.map(kantenSelektion =>
          this.getIstStandardsFlat(kantenSelektion.kante.kantenAttributGruppe.istStandards)
        )
      );

      this.formGroup.patchValue({
        gemeinde: selektion
          .map(kantenSelektion => kantenSelektion.kante.kantenAttributGruppe.gemeinde?.id)
          .every(id => id === selektion[0].kante.kantenAttributGruppe.gemeinde?.id)
          ? selektion[0].kante.kantenAttributGruppe.gemeinde
          : new UndeterminedValue(),
      });

      this.updateIstStandardsEnabled(selektion.map(s => s.kante.kantenAttributGruppe.netzklassen));

      this.seitenbezogenUndetermined = !selektion
        .map(s => s.kante.zweiseitig)
        .every(v => v === selektion[0].kante.zweiseitig);
      if (!this.seitenbezogenUndetermined) {
        this.seitenbezogen = selektion[0].kante.zweiseitig;
      } else {
        // damit bei Toggle des Buttons true selektiert wird
        this.seitenbezogen = false;
      }
    } else {
      this.formGroup.reset();
      this.seitenbezogenUndetermined = false;
    }

    this.copyDisabled = hasMultipleValues(this.formGroup) || this.seitenbezogenUndetermined;

    this.netzklassenSubscription = this.subscribeToNetzklassenChanges();
  }

  protected save(): Promise<Kante[]> {
    const equalValues = readEqualValuesFromForm(this.formGroup);

    const allgemeinCommands = this.kanteSelektionService.selektierteKanten.map(kante => {
      const kantenAttributGruppe: KantenAttributGruppe = kante.kantenAttributGruppe;
      const newNetzklassen = NetzklassenConverterService.convertFlatUndeterminedToNetzklassen(
        this.formGroup.getRawValue().netzklassen,
        kante.kantenAttributGruppe.netzklassen
      );
      let newIstStandards = this.updateIstStandards(
        this.formGroup.getRawValue().istStandards,
        kante.kantenAttributGruppe.istStandards
      );

      newIstStandards = this.validateIstStandards(newNetzklassen, newIstStandards);

      return {
        kanteId: kante.id,
        gruppenId: kantenAttributGruppe.id,
        gruppenVersion: kantenAttributGruppe.version,
        wegeNiveau: kantenAttributGruppe.wegeNiveau,
        beleuchtung: kantenAttributGruppe.beleuchtung,
        umfeld: kantenAttributGruppe.umfeld,
        strassenkategorieRIN: kantenAttributGruppe.strassenkategorieRIN,
        strassenquerschnittRASt06: kantenAttributGruppe.strassenquerschnittRASt06,
        laengeManuellErfasst: kantenAttributGruppe.laengeManuellErfasst,
        dtvFussverkehr: kantenAttributGruppe.dtvFussverkehr,
        dtvRadverkehr: kantenAttributGruppe.dtvRadverkehr,
        dtvPkw: kantenAttributGruppe.dtvPkw,
        sv: kantenAttributGruppe.sv,
        kommentar: kantenAttributGruppe.kommentar,
        status: kantenAttributGruppe.status,
        ...equalValues,
        netzklassen: newNetzklassen,
        istStandards: newIstStandards,
        gemeinde: equalValues.gemeinde ? equalValues.gemeinde.id : kantenAttributGruppe.gemeinde?.id,
      } as SaveKantenAttributGruppeCommand;
    });
    return this.netzService.saveKanteAllgemein(allgemeinCommands);
  }

  private enableForm(allowRadNetzKlasse: boolean): void {
    this.formGroup.enable({ emitEvent: false });
    this.formGroup.get('landkreis')?.disable({ emitEvent: false });
    this.formGroup.get('laengeBerechnet')?.disable({ emitEvent: false });
    this.formGroup.get('strassenName')?.disable({ emitEvent: false });
    this.formGroup.get('strassenNummer')?.disable({ emitEvent: false });
    // Disabling der RadNETZKlassen bitte wieder entfernen, wenn RadNETZ-Klassen an Grundnetzkanten gesetzt werden können!
    if (!allowRadNetzKlasse) {
      this.getNetzklassenFromGroup().get('radnetzAlltag')?.disable({ emitEvent: false });
      this.getNetzklassenFromGroup().get('radnetzFreizeit')?.disable({ emitEvent: false });
      this.getNetzklassenFromGroup().get('radnetzZielnetz')?.disable({ emitEvent: false });
    }
    if (!this.benutzerDetailsService.canKreisnetzVerlegen()) {
      this.getNetzklassenFromGroup().get('kreisnetzAlltag')?.disable({ emitEvent: false });
      this.getNetzklassenFromGroup().get('kreisnetzFreizeit')?.disable({ emitEvent: false });
    }
  }

  private updateIstStandardsEnabled(netzklassen: Netzklasse[][]): void {
    if (this.isRadnetzEnabled(netzklassen)) {
      this.getIstStandardsFromGroup()?.get('radnetzStartstandard')?.enable();
      this.getIstStandardsFromGroup()?.get('radnetzZielstandard')?.enable();
    } else {
      this.getIstStandardsFromGroup()?.get('radnetzStartstandard')?.disable();
      this.getIstStandardsFromGroup()?.get('radnetzZielstandard')?.disable();
    }
  }

  private validateIstStandards(newNetzklassen: Netzklasse[], newIstStandards: IstStandard[]): IstStandard[] {
    if (
      !(
        newNetzklassen.includes(Netzklasse.RADNETZ_ALLTAG) ||
        newNetzklassen.includes(Netzklasse.RADNETZ_ZIELNETZ) ||
        newNetzklassen.includes(Netzklasse.RADNETZ_FREIZEIT)
      )
    ) {
      newIstStandards = newIstStandards.filter(
        standard => standard !== IstStandard.ZIELSTANDARD_RADNETZ && standard !== IstStandard.STARTSTANDARD_RADNETZ
      );
    }
    return newIstStandards;
  }

  private subscribeToGemeindeChanges(): Subscription {
    return (this.formGroup.get('gemeinde') as UntypedFormControl).valueChanges.subscribe(value => {
      const organisation = value as Verwaltungseinheit;
      if (organisation && organisation.idUebergeordneteOrganisation) {
        this.organisationenService
          .getOrganisation(organisation.idUebergeordneteOrganisation)
          .then(uebergoerdneteOrganisation => {
            this.formGroup.get('landkreis')?.setValue(uebergoerdneteOrganisation.name);
            this.changeDetectorRef.markForCheck();
          })
          .catch(error => this.errorHandlingService.handleError(error, 'Landkreis konnte nicht geladen werden.'));
      } else {
        this.formGroup.get('landkreis')?.setValue(null);
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  private isRadnetzEnabled(netzklassen: Netzklasse[][]): boolean {
    return netzklassen
      .map(
        standards =>
          standards.includes(Netzklasse.RADNETZ_ALLTAG) ||
          standards.includes(Netzklasse.RADNETZ_FREIZEIT) ||
          standards.includes(Netzklasse.RADNETZ_ZIELNETZ)
      )
      .every(v => v);
  }

  private subscribeToNetzklassenChanges(): Subscription | undefined {
    return this.getNetzklassenFromGroup()?.valueChanges.subscribe((value: NetzklasseFlatUndertermined) => {
      const currentNetzklassen = this.kanteSelektionService.selektion.map(s =>
        NetzklassenConverterService.convertFlatUndeterminedToNetzklassen(
          value,
          s.kante.kantenAttributGruppe.netzklassen
        )
      );
      const currentIstStandards = this.kanteSelektionService.selektierteKanten.map(k => {
        const netzklassen = NetzklassenConverterService.convertFlatUndeterminedToNetzklassen(
          value,
          k.kantenAttributGruppe.netzklassen
        );
        const istStandards = this.updateIstStandards(
          this.getIstStandardsFromGroup().getRawValue(),
          k.kantenAttributGruppe.istStandards
        );
        return this.validateIstStandards(netzklassen, istStandards);
      });
      fillFormWithMultipleValues(
        this.getIstStandardsFromGroup(),
        currentIstStandards.map(standards => this.getIstStandardsFlat(standards))
      );

      this.updateIstStandardsEnabled(currentNetzklassen);
    });
  }

  private getIstStandardsFromGroup(): UntypedFormGroup {
    return this.formGroup.get('istStandards') as UntypedFormGroup;
  }

  private getNetzklassenFromGroup(): UntypedFormGroup {
    return this.formGroup.get('netzklassen') as UntypedFormGroup;
  }

  private getIstStandardsFlat(istStandards: IstStandard[]): IstStandardFlat {
    return {
      radnetzStartstandard: istStandards.includes(IstStandard.STARTSTANDARD_RADNETZ),
      radnetzZielstandard: istStandards.includes(IstStandard.ZIELSTANDARD_RADNETZ),
      radschnellverbindung: istStandards.includes(IstStandard.RADSCHNELLVERBINDUNG),
      basisstandard: istStandards.includes(IstStandard.BASISSTANDARD),
      radvorrangrouten: istStandards.includes(IstStandard.RADVORRANGROUTEN),
    };
  }

  private updateIstStandards(flat: IstStandardFlat, oldValues: IstStandard[]): IstStandard[] {
    const result: IstStandard[] = [];
    if (
      (flat.radnetzStartstandard instanceof UndeterminedValue &&
        oldValues.includes(IstStandard.STARTSTANDARD_RADNETZ)) ||
      flat.radnetzStartstandard === true
    ) {
      result.push(IstStandard.STARTSTANDARD_RADNETZ);
    }
    if (
      (flat.radnetzZielstandard instanceof UndeterminedValue && oldValues.includes(IstStandard.ZIELSTANDARD_RADNETZ)) ||
      flat.radnetzZielstandard === true
    ) {
      result.push(IstStandard.ZIELSTANDARD_RADNETZ);
    }
    if (
      (flat.basisstandard instanceof UndeterminedValue && oldValues.includes(IstStandard.BASISSTANDARD)) ||
      flat.basisstandard === true
    ) {
      result.push(IstStandard.BASISSTANDARD);
    }
    if (
      (flat.radschnellverbindung instanceof UndeterminedValue &&
        oldValues.includes(IstStandard.RADSCHNELLVERBINDUNG)) ||
      flat.radschnellverbindung === true
    ) {
      result.push(IstStandard.RADSCHNELLVERBINDUNG);
    }
    if (
      (flat.radvorrangrouten instanceof UndeterminedValue && oldValues.includes(IstStandard.RADVORRANGROUTEN)) ||
      flat.radvorrangrouten === true
    ) {
      result.push(IstStandard.RADVORRANGROUTEN);
    }

    return result;
  }

  private createForm(): UntypedFormGroup {
    return new UntypedFormGroup({
      wegeNiveau: new UntypedFormControl(null),
      beleuchtung: new UntypedFormControl(null),
      umfeld: new UntypedFormControl(null),
      strassenkategorieRIN: new UntypedFormControl(null),
      strassenquerschnittRASt06: new UntypedFormControl(null),
      laengeBerechnet: new UntypedFormControl({ value: 0, disabled: true }),
      laengeManuellErfasst: new UntypedFormControl(null),
      gemeinde: new UntypedFormControl(null),
      landkreis: new UntypedFormControl({ value: 0, disabled: true }),
      dtvFussverkehr: new UntypedFormControl(null, [
        RadvisValidators.isPositiveInteger,
        RadvisValidators.max(1_000_000),
      ]),
      dtvRadverkehr: new UntypedFormControl(null, [
        RadvisValidators.isPositiveInteger,
        RadvisValidators.max(1_000_000),
      ]),
      dtvPkw: new UntypedFormControl(null, [RadvisValidators.isPositiveInteger, RadvisValidators.max(1_000_000)]),
      sv: new UntypedFormControl(null, [RadvisValidators.isPositiveInteger, RadvisValidators.max(1_000_000)]),
      kommentar: new UntypedFormControl(null, RadvisValidators.maxLength(this.kommentarMaxLength)),
      strassenName: new UntypedFormControl({ value: 0, disabled: true }),
      strassenNummer: new UntypedFormControl({ value: 0, disabled: true }),
      status: new UntypedFormControl(null),

      netzklassen: NetzklassenConverterService.createFormGroup(),

      istStandards: new UntypedFormGroup({
        radnetzStartstandard: new UntypedFormControl(null),
        radnetzZielstandard: new UntypedFormControl(null),
        radschnellverbindung: new UntypedFormControl(null),
        basisstandard: new UntypedFormControl(null),
        radvorrangrouten: new UntypedFormControl(null),
      }),
    });
  }

  private showCopyPasteHinweis(): void {
    this.copyPasteHinweis = this.snackbar.open(
      'Kantenattribute wurden in die Zwischenablage kopiert. Wählen Sie andere Kanten aus, um die Attribute einzufügen. Mit Klick auf Beenden leeren Sie die Zwischenablage.',
      'Beenden'
    );
    this.subscriptions.push(
      this.copyPasteHinweis.onAction().subscribe(() => {
        localStorage.removeItem(KantenAttributeEditorComponent.ZWISCHENABLAGE_KEY);
        this.hasClipboard = false;
        this.copiedKanteId = null;
        this.changeDetectorRef.markForCheck();
      })
    );
    this.subscriptions.push(
      this.copyPasteHinweis
        .afterDismissed()
        .pipe(
          filter(v => !v.dismissedByAction),
          delay(NotifyUserService.DURATION)
        )
        .subscribe(() => {
          if (this.hasClipboard) {
            this.showCopyPasteHinweis();
          }
        })
    );
  }

  private resetAndDisable(selektion: KantenSelektion[], disable: boolean, allowRadNetzKlasse: boolean): void {
    if (!disable) {
      this.enableForm(allowRadNetzKlasse);
    }
    // das ist hier auseinander gezogen, da beim reset einige Felder disabled werden
    this.resetForm(selektion);
    if (disable) {
      this.formGroup.disable({ emitEvent: false });
    }
  }
}
