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
import { AuswertungService } from 'src/app/auswertung/services/auswertung.service';
import { IstStandard } from 'src/app/shared/models/ist-standard';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { Wahlkreis } from 'src/app/shared/models/wahlkreis';
import { WahlkreiseService } from 'src/app/shared/services/wahlkreise.service';
import { AuswertungGebietsauswahl } from 'src/app/auswertung/models/auswertung-gebietsauswahl';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';
import { GroupedEnumOptions } from 'src/app/form-elements/models/grouped-enum-options';

@Component({
  selector: 'rad-auswertung',
  templateUrl: './auswertung.component.html',
  styleUrls: ['./auswertung.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuswertungComponent {
  public ergebnis = 0;

  public fetching = false;

  public form: UntypedFormGroup;

  public netzklassenFormArray: UntypedFormArray;

  public istStandardsFormArray: UntypedFormArray;

  public gemeindeKreisBezirkOptions: Promise<Verwaltungseinheit[]> = Promise.resolve([]);
  public wahlkreisOptions: Promise<Wahlkreis[]> = Promise.resolve([]);
  public organisationsOptions: Promise<Verwaltungseinheit[]> = Promise.resolve([]);
  public readonly AuswertungGebietsauswahl = AuswertungGebietsauswahl;

  public gebietskoerperschaftOderWahlkreisControl = new UntypedFormControl(
    this.AuswertungGebietsauswahl.GEBIETSKOERPERSCHAFT
  );

  public belagartOptions: EnumOption[];
  public radverkehrsfuehrungOptions: GroupedEnumOptions;
  private keineNetzklasse = 'NICHT_KLASSIFIZIERT';

  private netzklassenArray = [
    Netzklasse.RADNETZ_ALLTAG,
    Netzklasse.RADNETZ_FREIZEIT,
    Netzklasse.RADNETZ_ZIELNETZ,
    Netzklasse.KREISNETZ_ALLTAG,
    Netzklasse.KREISNETZ_FREIZEIT,
    Netzklasse.KOMMUNALNETZ_ALLTAG,
    Netzklasse.KOMMUNALNETZ_FREIZEIT,
    Netzklasse.RADSCHNELLVERBINDUNG,
    Netzklasse.RADVORRANGROUTEN,
    this.keineNetzklasse,
  ];

  private keinStandard = 'KEIN_STANDARD';

  private istStandardArray = [
    IstStandard.BASISSTANDARD,
    this.keinStandard,
    IstStandard.STARTSTANDARD_RADNETZ,
    IstStandard.ZIELSTANDARD_RADNETZ,
    IstStandard.RADSCHNELLVERBINDUNG,
    IstStandard.RADVORRANGROUTEN,
  ];

  constructor(
    private auswertungService: AuswertungService,
    private organisationenService: OrganisationenService,
    private wahlkreiseService: WahlkreiseService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.gemeindeKreisBezirkOptions = this.organisationenService.getOrganisationen().then(orgs => {
      return orgs.filter(
        orga =>
          orga.organisationsArt === OrganisationsArt.GEMEINDE ||
          orga.organisationsArt === OrganisationsArt.KREIS ||
          orga.organisationsArt === OrganisationsArt.REGIERUNGSBEZIRK
      );
    });

    this.wahlkreisOptions = this.wahlkreiseService.getWahlkreise();

    this.organisationsOptions = this.organisationenService.getOrganisationen();

    this.belagartOptions = BelagArt.options;
    this.radverkehrsfuehrungOptions = Radverkehrsfuehrung.options;

    this.form = new UntypedFormGroup({
      gemeindeKreisBezirk: new UntypedFormControl(),
      wahlkreis: new UntypedFormControl(),
      useNetzklassen: new UntypedFormControl(),
      netzklassen: new UntypedFormArray([
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
      ]),
      useIstStandards: new UntypedFormControl(),
      istStandards: new UntypedFormArray([
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
        new UntypedFormControl(null),
      ]),
      baulast: new UntypedFormControl(),
      unterhalt: new UntypedFormControl(),
      erhalt: new UntypedFormControl(),
      belagart: new UntypedFormControl(),
      fuehrung: new UntypedFormControl(),
    });

    this.netzklassenFormArray = this.form.get('netzklassen') as UntypedFormArray;
    this.netzklassenFormArray.valueChanges.subscribe(value => {
      const anyBoxChecked = value.some((val: boolean) => val);
      this.form.get('useNetzklassen')?.patchValue(anyBoxChecked, { emitEvent: false });
    });

    this.istStandardsFormArray = this.form.get('istStandards') as UntypedFormArray;
    this.istStandardsFormArray.valueChanges.subscribe(value => {
      const anyBoxChecked = value.some((val: boolean) => val);
      this.form.get('useIstStandards')?.patchValue(anyBoxChecked, { emitEvent: false });
    });
  }

  getAuswertung(): void {
    const netzklassenParam: string[] = [];

    this.form.getRawValue().netzklassen.forEach((val: boolean, index: number) => {
      if (val) {
        netzklassenParam.push(this.netzklassenArray[index]);
      }
    });

    const istStandardParam: string[] = [];

    this.form.getRawValue().istStandards.forEach((val: boolean, index: number) => {
      if (val) {
        istStandardParam.push(this.istStandardArray[index]);
      }
    });

    this.fetching = true;
    const isWahlkreis: boolean =
      this.gebietskoerperschaftOderWahlkreisControl.value === AuswertungGebietsauswahl.WAHLKREIS;
    this.auswertungService
      .getAuswertung({
        gemeindeKreisBezirkId:
          this.form.value.gemeindeKreisBezirk && !isWahlkreis ? this.form.value.gemeindeKreisBezirk.id : '',
        wahlkreisId: this.form.value.wahlkreis && isWahlkreis ? this.form.value.wahlkreis.id : '',
        netzklassen: this.form.value.useNetzklassen
          ? netzklassenParam.filter(value => value !== this.keineNetzklasse)
          : [],
        beachteNichtKlassifizierteKanten: this.form.value.useNetzklassen
          ? netzklassenParam.includes(this.keineNetzklasse)
          : false,
        istStandards: this.form.value.useIstStandards
          ? istStandardParam.filter(value => value !== this.keinStandard)
          : [],
        beachteKantenOhneStandards: this.form.value.useIstStandards
          ? istStandardParam.includes(this.keinStandard)
          : false,
        baulastId: this.form.value.baulast ? this.form.value.baulast.id : '',
        unterhaltId: this.form.value.unterhalt ? this.form.value.unterhalt.id : '',
        erhaltId: this.form.value.erhalt ? this.form.value.erhalt.id : '',
        fuehrung: this.form.value.fuehrung ?? '',
        belagart: this.form.value.belagart ?? '',
      })
      .then(ergebnis => {
        this.ergebnis = ergebnis;
      })
      .finally(() => {
        this.fetching = false;
        this.changeDetectorRef.markForCheck();
      });
  }

  toggleFormArray(formArrayName: string, checked: boolean): void {
    const formArray = this.form.get(formArrayName) as UntypedFormArray;
    formArray.controls.forEach(element => {
      element.patchValue(checked, { emitEvent: false });
    });
  }
}
