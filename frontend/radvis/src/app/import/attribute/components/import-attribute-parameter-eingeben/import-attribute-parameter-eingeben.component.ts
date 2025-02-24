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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { UntypedFormArray, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { AttributeParameter } from 'src/app/import/attribute/models/attribute-parameter';
import { StartAttributeImportSessionCommand } from 'src/app/import/attribute/models/start-attribute-import-session-command';
import { ValidateAttributeImportCommand } from 'src/app/import/attribute/models/validate-attribute-import-command';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { CreateSessionStateService } from 'src/app/import/services/create-session.state.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-attribute-parameter-eingeben',
  templateUrl: './import-attribute-parameter-eingeben.component.html',
  styleUrls: ['./import-attribute-parameter-eingeben.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportAttributeParameterEingebenComponent {
  private static readonly STEP = 2;

  sessionExists = false;
  uploading = false;
  loading = false;
  invalidAttributesPresent = false;

  formArray = new UntypedFormArray([]);
  attribute: string[] | null = null;

  ungueltigeWerte: Map<string, string[]> = new Map();
  attributHinweisTexte: string[] = [];

  get attributeGroups(): UntypedFormGroup[] {
    return this.formArray.controls as UntypedFormGroup[];
  }

  get isSelektionValid(): boolean {
    return !this.attributHinweisTexte || this.attributHinweisTexte.length === 0;
  }

  constructor(
    private attributeImportService: AttributeImportService,
    private attributeRoutingService: AttributeRoutingService,
    private createSessionStateService: CreateSessionStateService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.loading = true;
    this.attributeImportService.getImportSession().subscribe(session => {
      this.sessionExists = !!session;
      if (session) {
        this.attribute = session.attribute;
        this.formArray.disable();
        this.loading = false;
        this.changeDetectorRef.detectChanges();
      } else {
        const dateiInfo = this.createSessionStateService.dateiUploadInfo;
        const attributeImportFormat = this.createSessionStateService.attributeImportFormat;
        invariant(dateiInfo);
        invariant(attributeImportFormat);
        this.attributeImportService
          .getImportierbareAttribute(
            {
              attributeImportFormat,
            } as ValidateAttributeImportCommand,
            dateiInfo.file
          )
          .then(attribute => {
            this.formArray = new UntypedFormArray(
              attribute
                .sort((a, b) => {
                  if (a.attributDisplayName > b.attributDisplayName) {
                    return 1;
                  } else if (a.attributDisplayName < b.attributDisplayName) {
                    return -1;
                  }
                  return 0;
                })
                .map(attr => {
                  const selected =
                    (this.createSessionStateService.parameterInfo == null ||
                      (this.createSessionStateService.parameterInfo as AttributeParameter).attributListe.includes(
                        attr.attributName
                      )) &&
                    attr.valid;
                  const attributGroup = new UntypedFormGroup({
                    attributName: new UntypedFormControl(attr.attributName),
                    attributDisplayName: new UntypedFormControl(attr.attributDisplayName),
                    radvisName: new UntypedFormControl(attr.radvisName),
                    selected: new UntypedFormControl(selected),
                  });
                  if (!attr.valid) {
                    attributGroup.disable();
                    this.invalidAttributesPresent = true;
                    this.ungueltigeWerte.set(attr.attributName, attr.ungueltigeWerte);
                  }
                  return attributGroup;
                })
            );

            this.formArray.valueChanges.subscribe(attributeFormGroupValue => {
              const newAttributAuswahl = this.extractAttributNamesFromFormValue(attributeFormGroupValue);
              this.createSessionStateService.updateParameterInfo(AttributeParameter.of(newAttributAuswahl));
              this.validateAttributSelektion();
            });

            this.createSessionStateService.updateParameterInfo(
              // Achtung: this.formArray.value liefert - wenn alle enthaltenen Controls disabled sind ein Array mit
              // allen Werte (https://github.com/angular/angular/issues/15982#issuecomment-327335108). Da diese jedoch
              // nicht selektiert sind, werden sie von extractAttributNamesFromFormValue herausgefiltert
              AttributeParameter.of(this.extractAttributNamesFromFormValue(this.formArray.value))
            );
          })
          .finally(() => {
            this.loading = false;
            this.changeDetectorRef.detectChanges();
          });
      }
    });
  }

  onAbort(): void {
    this.attributeImportService.deleteImportSession().subscribe(() => {
      this.attributeRoutingService.navigateToFirst();
    });
    this.createSessionStateService.reset();
  }

  onStart(): void {
    if (!this.isSelektionValid) {
      return;
    }

    this.formArray.disable();
    if (this.sessionExists) {
      this.navigateToNextStep();
      return;
    }

    this.uploading = true;

    if (
      this.createSessionStateService.dateiUploadInfo !== null &&
      this.createSessionStateService.parameterInfo !== null
    ) {
      this.attributeImportService
        .createSessionAndStartAttributeImport(
          {
            organisation: this.createSessionStateService.dateiUploadInfo?.organisation,
            attribute: (this.createSessionStateService.parameterInfo as AttributeParameter).attributListe,
            attributeImportFormat: this.createSessionStateService.attributeImportFormat,
          } as StartAttributeImportSessionCommand,
          this.createSessionStateService.dateiUploadInfo.file
        )
        .then(() => {
          this.createSessionStateService.reset();
          this.navigateToNextStep();
        })
        .finally(() => {
          this.uploading = false;
          this.changeDetectorRef.markForCheck();
        });
    } else {
      this.uploading = false;
      this.formArray.enable();
      throw new Error(
        'Es sind noch nicht alle Informationen zum Erstellen einer Session vorhanden. Bitte überprüfen Sie die Eingaben von Schritt 1 und Schritt 2.'
      );
    }
  }

  onAlleAttributeAbwaehlen(): void {
    this.attributeGroups.forEach(value => {
      value.patchValue({ selected: false });
    });
  }

  onAlleAttributeAuswaehlen(): void {
    this.attributeGroups.forEach(value => {
      if (value.enabled) {
        value.patchValue({ selected: true });
      }
    });
  }

  onNext(): void {
    this.navigateToNextStep();
  }

  onPrevious(): void {
    this.attributeRoutingService.navigateToPrevious(ImportAttributeParameterEingebenComponent.STEP);
  }

  private navigateToNextStep(): void {
    this.attributeRoutingService.navigateToNext(ImportAttributeParameterEingebenComponent.STEP);
  }

  private extractAttributNamesFromFormValue(attributeFormGroupValue: any): string[] {
    return attributeFormGroupValue
      .filter((attrValue: any) => attrValue.selected)
      .map((attrValue: any) => attrValue.attributName);
  }

  /**
   * Da Trennstreifen und Radverkehrsführung eng zusammen hängen, müssen diese immer zusammen übernommen werden.
   */
  private validateAttributSelektion(): void {
    if (!this.createSessionStateService.parameterInfo) {
      this.attributHinweisTexte = [];
      return;
    }

    const attribute = (this.createSessionStateService.parameterInfo as AttributeParameter).attributListe;
    const trennstreifenAttribute = ['sts_f_l', 'sts_t_l', 'sts_b_l', 'sts_f_r', 'sts_t_r', 'sts_b_r'];

    const trennstreifenUebernehmen = attribute.find(attribut =>
      trennstreifenAttribute.includes(attribut.toLowerCase())
    );
    const fuehrungsformUebernehmen = attribute.find(attribut => attribut.toLowerCase() === 'radverkehr');
    const alleTrennstreifenAttributeVorhanden = !trennstreifenAttribute.every(attribut => attribute.includes(attribut));

    this.attributHinweisTexte = [];

    if (trennstreifenUebernehmen && alleTrennstreifenAttributeVorhanden) {
      this.attributHinweisTexte.push('Es können nur alle Trennstreifen-Attribute auf einmal übernommen werden.');
    }

    if (fuehrungsformUebernehmen && !trennstreifenUebernehmen) {
      this.attributHinweisTexte.push(
        'Bei zu übernehmender Radverkehrsführung müssen auch die Trennstreifen-Attribute übernommen werden.'
      );
    }

    if (!fuehrungsformUebernehmen && trennstreifenUebernehmen) {
      this.attributHinweisTexte.push(
        'Bei zu übernehmenden Trennstreifen-Attributen muss auch die Radverkehrsführung übernommen werden.'
      );
    }
  }
}
