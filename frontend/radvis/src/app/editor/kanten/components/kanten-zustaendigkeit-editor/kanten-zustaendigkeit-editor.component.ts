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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import {
  AbstractLinearReferenzierteAttributGruppeOhneSeitenbezugEditor
} from 'src/app/editor/kanten/components/abstract-linear-referenzierte-attribut-gruppe-ohne-seitenbezug-editor';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import {
  SaveZustaendigkeitAttributGruppeCommand
} from 'src/app/editor/kanten/models/save-zustaendigkeit-attribut-gruppe-command';
import { SaveZustaendigkeitAttributeCommand } from 'src/app/editor/kanten/models/save-zustaendigkeit-attribute-command';
import { ZustaendigkeitAttributGruppe } from 'src/app/editor/kanten/models/zustaendigkeit-attribut-gruppe';
import { ZustaendigkeitAttribute } from 'src/app/editor/kanten/models/zustaendigkeit-attribute';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { UndeterminedValue } from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-kanten-zustaendigkeit-editor',
  templateUrl: './kanten-zustaendigkeit-editor.component.html',
  styleUrls: [
    './kanten-zustaendigkeit-editor.component.scss',
    '../../../../form-elements/components/attribute-editor/attribut-editor.scss',
    '../abstract-attribut-gruppe-editor-mit-auswahl.scss',
    '../lineare-referenz-tabelle.scss',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KantenZustaendigkeitEditorComponent
  extends AbstractLinearReferenzierteAttributGruppeOhneSeitenbezugEditor<ZustaendigkeitAttribute,
    ZustaendigkeitAttributGruppe>
  implements DiscardableComponent, OnDestroy, OnInit {
  alleOrganisationenOptions: Promise<Verwaltungseinheit[]>;

  private readonly vereinbarungsKennungMaxLength = 255;

  constructor(
    private netzService: NetzService,
    changeDetectorRef: ChangeDetectorRef,
    notifyUserService: NotifyUserService,
    kantenSelektionService: KantenSelektionService,
    organisationenService: OrganisationenService,
    benutzerDetailsService: BenutzerDetailsService,
  ) {
    super(changeDetectorRef, notifyUserService, kantenSelektionService, benutzerDetailsService);
    this.alleOrganisationenOptions = organisationenService.getOrganisationen();
  }

  ngOnInit(): void {
    super.subscribeToKantenSelektion();
    this.displayedAttributeformGroup.get('vereinbarungsKennung')?.valueChanges.subscribe((value: string) => {
      if (value.length > this.vereinbarungsKennungMaxLength) {
        this.displayedAttributeformGroup
        .get('vereinbarungsKennung')
        ?.setValue(value.substr(0, this.vereinbarungsKennungMaxLength));
        this.displayedAttributeformGroup.get('vereinbarungsKennung')?.updateValueAndValidity({ emitEvent: false });
        this.notifyUserService.inform('Vereinbarungskennung wurde auf maximal erlaubte Länge gekürzt.');
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  protected createDisplayedAttributeFormGroup(): UntypedFormGroup {
    return new UntypedFormGroup({
      baulastTraeger: new UntypedFormControl(null),
      unterhaltsZustaendiger: new UntypedFormControl(null),
      erhaltsZustaendiger: new UntypedFormControl(null),
      vereinbarungsKennung: new UntypedFormControl(null, {
        validators: [RadvisValidators.maxLength(this.vereinbarungsKennungMaxLength)],
      }),
    });
  }

  protected saveAttributgruppe(attributgruppen: ZustaendigkeitAttributGruppe[]): Promise<Kante[]> {
    const commands = attributgruppen.map(attributgruppe => {
      const attributeCommand = attributgruppe.zustaendigkeitAttribute.map(attribute =>
        this.convertAttributeToAttributeCommand(attribute),
      );
      const associatedKantenSelektion = this.currentSelektion?.find(
        kantenSelektion => this.getAttributGruppeFrom(kantenSelektion.kante).id === attributgruppe.id,
      ) as KantenSelektion;
      return {
        gruppenID: attributgruppe.id,
        gruppenVersion: attributgruppe.version,
        zustaendigkeitAttribute: attributeCommand,
        kanteId: associatedKantenSelektion.kante.id,
      } as SaveZustaendigkeitAttributGruppeCommand;
    });
    return this.netzService.saveZustaendigkeitsGruppe(commands);
  }

  protected getAttributeForSelektion(selektion: KantenSelektion[]): ZustaendigkeitAttribute[] {
    const result: ZustaendigkeitAttribute[] = [];
    selektion.forEach((kantenSelektion, kantenIndex) => {
      kantenSelektion.getSelectedSegmentIndices().forEach(selectedSegmentIndex => {
        result.push(this.currentAttributgruppen[kantenIndex].zustaendigkeitAttribute[selectedSegmentIndex]);
      });
    });
    return result;
  }

  // eslint-disable-next-line prettier/prettier
  protected override resetDisplayedAttribute(selektion: KantenSelektion[]): void {
    super.resetDisplayedAttribute(selektion);
    if (selektion.length > 0) {
      const selectedZustaendigkeitAttributeArray = this.getAttributeForSelektion(selektion);
      this.displayedAttributeformGroup.patchValue(
        {
          baulastTraeger: this.determineValueOrUndeterminedForOrganisation(
            'baulastTraeger',
            selectedZustaendigkeitAttributeArray,
          ),
          unterhaltsZustaendiger: this.determineValueOrUndeterminedForOrganisation(
            'unterhaltsZustaendiger',
            selectedZustaendigkeitAttributeArray,
          ),
          erhaltsZustaendiger: this.determineValueOrUndeterminedForOrganisation(
            'erhaltsZustaendiger',
            selectedZustaendigkeitAttributeArray,
          ),
        },
        { emitEvent: false },
      );
    }
  }

  protected getAttributeFromAttributGruppe(attributGruppe: ZustaendigkeitAttributGruppe): ZustaendigkeitAttribute[] {
    return attributGruppe.zustaendigkeitAttribute;
  }

  protected getAttributGruppeFrom(kante: Kante): ZustaendigkeitAttributGruppe {
    return kante.zustaendigkeitAttributGruppe;
  }

  protected updateCurrentAttributgruppenWithLineareReferenzen(newLineareReferenzenArrays: LinearReferenzierterAbschnitt[][]): void {
    newLineareReferenzenArrays.forEach((lineareReferenzen, kantenIndex) => {
      lineareReferenzen.forEach((lineareReferenz, segmentIndex) => {
        this.currentAttributgruppen[kantenIndex].zustaendigkeitAttribute[segmentIndex].linearReferenzierterAbschnitt =
          newLineareReferenzenArrays[kantenIndex][segmentIndex];
      });
    });
  }

  protected updateCurrentAttributgruppenWithAttribute(changedAttributePartial: { [id: string]: any }): void {
    this.currentSelektion?.forEach(kantenSelektion => {
      const attributgruppeToChange = this.currentAttributgruppen.find(
        gruppe => gruppe.id === kantenSelektion.kante.zustaendigkeitAttributGruppe.id,
      );
      invariant(attributgruppeToChange);
      kantenSelektion.getSelectedSegmentIndices().forEach(selectedSegmentIndex => {
        attributgruppeToChange.zustaendigkeitAttribute[selectedSegmentIndex] = {
          ...attributgruppeToChange.zustaendigkeitAttribute[selectedSegmentIndex],
          ...changedAttributePartial,
        };
      });
    });
  }

  protected resetLineareReferenzenFormArrays(newSelektion: KantenSelektion[]): void {
    const values = newSelektion.map(kantenSelektion => this.extractLineareReferenzenFromKante(kantenSelektion.kante));

    this.resetFormArray(this.lineareReferenzenFormArray, values);
  }

  private extractLineareReferenzenFromKante(kante: Kante): LinearReferenzierterAbschnitt[] {
    return kante.zustaendigkeitAttributGruppe.zustaendigkeitAttribute.map(attribute => attribute.linearReferenzierterAbschnitt);
  }

  private convertAttributeToAttributeCommand(attribute: ZustaendigkeitAttribute): SaveZustaendigkeitAttributeCommand {
    return {
      baulastTraeger: attribute.baulastTraeger?.id,
      unterhaltsZustaendiger: attribute.unterhaltsZustaendiger?.id,
      erhaltsZustaendiger: attribute.erhaltsZustaendiger?.id,
      vereinbarungsKennung: attribute.vereinbarungsKennung,
      linearReferenzierterAbschnitt: attribute.linearReferenzierterAbschnitt,
    } as SaveZustaendigkeitAttributeCommand;
  }

  private determineValueOrUndeterminedForOrganisation(
    key: keyof ZustaendigkeitAttribute,
    zustaendigkeitAttributeArray: ZustaendigkeitAttribute[],
  ): any {
    return zustaendigkeitAttributeArray.every(
      attribute => (attribute[key] as Verwaltungseinheit)?.id === (zustaendigkeitAttributeArray[0][key] as Verwaltungseinheit)?.id,
    )
      ? zustaendigkeitAttributeArray[0][key]
      : new UndeterminedValue();
  }
}
