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
import { FormArray, FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AttributeImportSessionView } from 'src/app/editor/manueller-import/models/attribute-import-session-view';
import { AttributeParameter } from 'src/app/editor/manueller-import/models/attribute-parameter';
import { ImportSessionView } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { StartAttributeImportSessionCommand } from 'src/app/editor/manueller-import/models/start-attribute-import-session-command';
import { ValidateAttributeImportCommand } from 'src/app/editor/manueller-import/models/validate-attribute-import-command';
import { CreateSessionStateService } from 'src/app/editor/manueller-import/services/create-session.state.service';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-attribute-parameter-eingeben',
  templateUrl: './import-attribute-parameter-eingeben.component.html',
  styleUrls: ['./import-attribute-parameter-eingeben.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportAttributeParameterEingebenComponent {
  previousLink: string;
  nextLink: string;

  sessionExists = false;
  uploading = false;
  loading = false;
  invalidAttributesPresent = false;

  formArray = new FormArray([]);
  attribute: string[] | null = null;

  ungueltigeWerte: Map<string, string[]> = new Map();
  attributHinweisTexte: string[] = [];

  get attributeGroups(): FormGroup[] {
    return this.formArray.controls as FormGroup[];
  }

  get isSelektionValid(): boolean {
    return !this.attributHinweisTexte || this.attributHinweisTexte.length === 0;
  }

  constructor(
    private manuellerImportRoutingService: ManuellerImportRoutingService,
    private route: ActivatedRoute,
    private router: Router,
    private manuellerImportService: ManuellerImportService,
    private createSessionStateService: CreateSessionStateService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.previousLink =
      '../' +
      manuellerImportRoutingService.getRouteForStep(route.snapshot.data.step - 1, ImportTyp.ATTRIBUTE_UEBERNEHMEN);

    this.nextLink =
      '../' +
      manuellerImportRoutingService.getRouteForStep(route.snapshot.data.step + 1, ImportTyp.ATTRIBUTE_UEBERNEHMEN);

    this.loading = true;
    this.manuellerImportService.existsImportSession().then(exists => {
      this.sessionExists = exists;
      if (exists) {
        this.manuellerImportService
          .getImportSession()
          .toPromise()
          .then((session: ImportSessionView) => {
            this.attribute = (session as AttributeImportSessionView).attribute;
            this.formArray.disable();
            this.loading = false;
            this.changeDetectorRef.detectChanges();
          });
      } else {
        const dateiInfo = this.createSessionStateService.dateiUploadInfo;
        const attributeImportFormat = this.createSessionStateService.attributeImportFormat;
        invariant(dateiInfo);
        invariant(attributeImportFormat);
        this.manuellerImportService
          .getImportierbareAttribute(
            {
              attributeImportFormat,
            } as ValidateAttributeImportCommand,
            dateiInfo.file
          )
          .then(attribute => {
            this.formArray = new FormArray(
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
                  const attributGroup = new FormGroup({
                    attributName: new FormControl(attr.attributName),
                    attributDisplayName: new FormControl(attr.attributDisplayName),
                    radvisName: new FormControl(attr.radvisName),
                    selected: new FormControl(selected),
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
    this.manuellerImportService.deleteImportSession().then(() => {
      this.router.navigate(['../' + this.manuellerImportRoutingService.getStartStepRoute()], {
        relativeTo: this.route,
        queryParamsHandling: 'merge',
      });
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
      this.manuellerImportService
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

  private navigateToNextStep(): void {
    this.router.navigate([this.nextLink], {
      relativeTo: this.route,
      queryParamsHandling: 'merge',
    });
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
