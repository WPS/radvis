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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AbstractControl, FormControl, FormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MAT_SLIDE_TOGGLE_DEFAULT_OPTIONS, MatSlideToggleDefaultOptions } from '@angular/material/slide-toggle';
import { Subscription } from 'rxjs';
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
import { StrassenquerschnittRASt06 } from 'src/app/editor/kanten/models/strassenquerschnittrast06';
import { Umfeld } from 'src/app/editor/kanten/models/umfeld';
import { WegeNiveau } from 'src/app/editor/kanten/models/wege-niveau';
import { fillFormWithMultipleValues } from 'src/app/editor/kanten/services/fill-form-with-multiple-values';
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
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzklassenConverterService } from 'src/app/shared/services/netzklassen-converter.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';

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
})
export class KantenAttributeEditorComponent extends AbstractAttributGruppeEditor implements OnInit {
  public wegeniveauOptions = WegeNiveau.options;
  public beleuchtungsOptions = Beleuchtung.options;
  public umfeldOptions = Umfeld.options;
  public strassenquerschnittRASt06Options = StrassenquerschnittRASt06.options;
  public alleOrganisationenOptions: Promise<Verwaltungseinheit[]> = Promise.resolve([]);
  public statusOptions = Status.options;

  public disableRadNetzIstStandards = false;

  public gemeindeOptions: Promise<Verwaltungseinheit[]> = Promise.resolve([]);

  public readonly kommentarMaxLength = 2000;

  formGroup: FormGroup;
  netzklassenSubscription: Subscription | undefined;
  changeSeitenbezug = false;
  seitenbezogenUndetermined = false;
  seitenbezogen = false;
  showRadNetzHinweis = false;

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
    private benutzerDetailsService: BenutzerDetailsService
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

    this.subscribeToGemeindeChanges();
  }

  ngOnInit(): void {
    this.kanteSelektionService.selektion$.subscribe(selektion => {
      const allKantenInZustaendigkeitsbereich = selektion.every(
        kantenSelektion => kantenSelektion.kante.liegtInZustaendigkeitsbereich
      );

      if (selektion.length > 0) {
        Promise.resolve(selektion.some(kantenSelektion => kantenSelektion.kante.quelle === QuellSystem.RadNETZ)).then(
          radNetzSelected => {
            return this.organisationenService
              .liegenAlleInQualitaetsgesichertenLandkreisen(selektion.map(kantenSelektion => kantenSelektion.kante.id))
              .then(canEditRadnetz => {
                const disableAll =
                  (!allKantenInZustaendigkeitsbereich && !this.benutzerDetailsService.canEditGesamtesNetz()) ||
                  (radNetzSelected && !canEditRadnetz);
                const disableRadNetzKlasse = !canEditRadnetz;
                this.showRadNetzHinweis = !canEditRadnetz;
                this.resetAndDisable(selektion, disableAll, !disableRadNetzKlasse);
                this.changeDetectorRef.markForCheck();
              });
          }
        );
      } else {
        this.resetAndDisable(selektion, false, true);
      }
    });
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
    return (this.formGroup.get('gemeinde') as FormControl).valueChanges.subscribe(value => {
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

  private getIstStandardsFromGroup(): FormGroup {
    return this.formGroup.get('istStandards') as FormGroup;
  }

  private getNetzklassenFromGroup(): FormGroup {
    return this.formGroup.get('netzklassen') as FormGroup;
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

  private createForm(): FormGroup {
    return new FormGroup({
      wegeNiveau: new FormControl(null),
      beleuchtung: new FormControl(null),
      umfeld: new FormControl(null),
      strassenquerschnittRASt06: new FormControl(null),
      laengeBerechnet: new FormControl({ value: 0, disabled: true }),
      laengeManuellErfasst: new FormControl(null),
      gemeinde: new FormControl(null),
      landkreis: new FormControl({ value: 0, disabled: true }),
      dtvFussverkehr: new FormControl(null, [RadvisValidators.isPositiveInteger, RadvisValidators.max(1_000_000)]),
      dtvRadverkehr: new FormControl(null, [RadvisValidators.isPositiveInteger, RadvisValidators.max(1_000_000)]),
      dtvPkw: new FormControl(null, [RadvisValidators.isPositiveInteger, RadvisValidators.max(1_000_000)]),
      sv: new FormControl(null, [RadvisValidators.isPositiveInteger, RadvisValidators.max(1_000_000)]),
      kommentar: new FormControl(null, RadvisValidators.maxLength(this.kommentarMaxLength)),
      strassenName: new FormControl({ value: 0, disabled: true }),
      strassenNummer: new FormControl({ value: 0, disabled: true }),
      status: new FormControl(null),

      netzklassen: NetzklassenConverterService.createFormGroup(),

      istStandards: new FormGroup({
        radnetzStartstandard: new FormControl(null),
        radnetzZielstandard: new FormControl(null),
        radschnellverbindung: new FormControl(null),
        basisstandard: new FormControl(null),
        radvorrangrouten: new FormControl(null),
      }),
    });
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
