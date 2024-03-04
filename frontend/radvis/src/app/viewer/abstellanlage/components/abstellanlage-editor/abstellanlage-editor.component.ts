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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostBinding, OnDestroy, Optional } from '@angular/core';
import { AbstractControl, FormControl, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { ABSTELLANLAGEN } from 'src/app/viewer/abstellanlage/models/abstellanlage.infrastruktur';
import { AbstellanlagenQuellSystem } from 'src/app/viewer/abstellanlage/models/abstellanlagen-quell-system';
import { AbstellanlagenStatus } from 'src/app/viewer/abstellanlage/models/abstellanlagen-status';
import { AbstellanlagenOrt } from 'src/app/viewer/abstellanlage/models/abstellanlagen-ort';
import { Groessenklasse } from 'src/app/viewer/abstellanlage/models/groessenklasse';
import { SaveAbstellanlageCommand } from 'src/app/viewer/abstellanlage/models/save-abstellanlage-command';
import { Stellplatzart } from 'src/app/viewer/abstellanlage/models/stellplatzart';
import { Ueberwacht } from 'src/app/viewer/abstellanlage/models/ueberwacht';
import { AbstellanlageFilterService } from 'src/app/viewer/abstellanlage/services/abstellanlage-filter.service';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { AbstellanlageUpdatedService } from 'src/app/viewer/abstellanlage/services/abstellanlage-updated.service';
import { AbstellanlageService } from 'src/app/viewer/abstellanlage/services/abstellanlage.service';
import { SimpleEditorCreatorComponent } from 'src/app/viewer/viewer-shared/components/simple-editor-creator.component';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-abstellanlage-editor',
  templateUrl: './abstellanlage-editor.component.html',
  styleUrls: ['./abstellanlage-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AbstellanlageEditorComponent extends SimpleEditorCreatorComponent<Abstellanlage> implements OnDestroy {
  public MOBIDATA_DATENSATZ_URL =
    'https://www.mobidata-bw.de/dataset/gebundelte-fahrradabstellanlagen-baden-wurttemberg';

  // Setzt je nachdem ob es Creator oder Editor ist die entsprechende CSS Klasse an :host,
  // welches in .scss dann verwendet werden kann.
  @HostBinding('class') get hostClasses(): string {
    return this.isCreator ? 'is-creator' : 'is-editor';
  }

  entityName = 'Abstellanlage';

  iconName = ABSTELLANLAGEN.iconFileName;
  currentAbstellanlage: Abstellanlage | null = null;

  abstellanlagenQuellSystemOptions = AbstellanlagenQuellSystem.options;
  ueberwachtOptions = Ueberwacht.options;
  abstellanlagenOrtOptions = AbstellanlagenOrt.options;
  groessenklasseOptions = Groessenklasse.options;
  stellplatzartOptions = Stellplatzart.options;
  abstellanlagenStatusOptions = AbstellanlagenStatus.options;

  alleOrganisationen$: Promise<Verwaltungseinheit[]>;
  organisationAktuellerBenutzer: Verwaltungseinheit | undefined;

  subscriptions: Subscription[] = [];

  constructor(
    activatedRoute: ActivatedRoute,
    changeDetector: ChangeDetectorRef,
    private viewerRoutingService: ViewerRoutingService,
    private abstellanlageService: AbstellanlageService,
    @Optional() private abstellanlageUpdatedService: AbstellanlageUpdatedService,
    notifyUserService: NotifyUserService,
    private abstellanlageRoutingService: AbstellanlageRoutingService,
    filterService: AbstellanlageFilterService,
    organisationenService: OrganisationenService,
    benutzerDetailsService: BenutzerDetailsService
  ) {
    super(
      new UntypedFormGroup({
        geometrie: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
        betreiber: new UntypedFormControl(null, [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
        externeId: new UntypedFormControl(null, RadvisValidators.maxLength(255)),
        quellSystem: new UntypedFormControl({ value: null, disabled: true }),
        zustaendig: new UntypedFormControl(null),
        anzahlStellplaetze: new UntypedFormControl(null, [
          RadvisValidators.isNotNullOrEmpty,
          RadvisValidators.isPositiveInteger,
          RadvisValidators.isSmallerThanIntegerMaxValue,
        ]),
        anzahlSchliessfaecher: new UntypedFormControl(null, [
          RadvisValidators.isNotNullOrEmpty,
          RadvisValidators.isPositiveInteger,
          RadvisValidators.isSmallerThanIntegerMaxValue,
        ]),
        anzahlLademoeglichkeiten: new UntypedFormControl(null, [
          RadvisValidators.isPositiveInteger,
          RadvisValidators.isSmallerThanIntegerMaxValue,
        ]),
        ueberwacht: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
        abstellanlagenOrt: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
        groessenklasse: new UntypedFormControl({ value: null, disabled: true }),
        stellplatzart: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
        ueberdacht: new UntypedFormControl(null),
        gebuehrenProTag: new FormControl<number | null>(null, [
          RadvisValidators.maxDecimalPlaces(2),
          RadvisValidators.max(20000000),
        ]),
        gebuehrenProMonat: new FormControl<number | null>(null, [
          RadvisValidators.maxDecimalPlaces(2),
          RadvisValidators.max(20000000),
        ]),
        gebuehrenProJahr: new FormControl<number | null>(null, [
          RadvisValidators.maxDecimalPlaces(2),
          RadvisValidators.max(20000000),
        ]),
        beschreibung: new UntypedFormControl(null, [RadvisValidators.maxLength(2000)]),
        weitereInformation: new UntypedFormControl(null, [RadvisValidators.maxLength(2000)]),
        status: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      }),
      notifyUserService,
      changeDetector,
      filterService
    );
    this.subscriptions.push(
      (this.formGroup.get('abstellanlagenOrt') as AbstractControl)?.valueChanges.subscribe(abstellanlagenOrt => {
        this.setEnabledStateOfGroessenklasse(abstellanlagenOrt === AbstellanlagenOrt.BIKE_AND_RIDE);
      })
    );

    this.alleOrganisationen$ = organisationenService.getOrganisationen();
    this.organisationAktuellerBenutzer = benutzerDetailsService.aktuellerBenutzerOrganisation();

    activatedRoute.data.subscribe(d => {
      this.isCreator = d.isCreator;
      this.currentAbstellanlage = d.abstellanlage ?? null;
      this.resetForm(this.currentAbstellanlage);
      changeDetector.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  onSave(): void {
    super.save();
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  onReset(): void {
    this.resetForm(this.currentAbstellanlage);
  }

  protected doSave(formGroup: UntypedFormGroup): Promise<void> {
    const currentId = this.currentAbstellanlage?.id;
    invariant(currentId);
    return this.abstellanlageService
      .save(currentId, {
        ...this.readForm(formGroup),
        version: this.currentAbstellanlage?.version,
      })
      .then(savedAbstellanlage => {
        this.currentAbstellanlage = savedAbstellanlage;
        this.resetForm(this.currentAbstellanlage);
        this.abstellanlageUpdatedService?.updateAbstellanlage();
      });
  }

  protected doCreate(formGroup: UntypedFormGroup): Promise<void> {
    return this.abstellanlageService.create(this.readForm(formGroup)).then(newId => {
      this.formGroup.markAsPristine();
      this.abstellanlageRoutingService.toInfrastrukturEditor(newId);
    });
  }

  public get canEdit(): boolean {
    return this.isCreator || (!this.isQuellsystemMobiData && !!this.currentAbstellanlage?.darfBenutzerBearbeiten);
  }

  public get isQuellsystemMobiData(): boolean {
    return this.currentAbstellanlage?.quellSystem === AbstellanlagenQuellSystem.MOBIDATABW;
  }

  private setEnabledStateOfGroessenklasse(istBikeAndRide: boolean): void {
    const groessenklasseControl = this.formGroup.get('groessenklasse');
    if (istBikeAndRide) {
      groessenklasseControl?.enable();
    } else {
      groessenklasseControl?.disable();
    }
  }

  private readForm(formGroup: UntypedFormGroup): SaveAbstellanlageCommand {
    const coordinate = formGroup.value.geometrie;
    return {
      betreiber: formGroup.value.betreiber,
      geometrie: {
        coordinates: coordinate,
        type: 'Point',
      },
      externeId: formGroup.value.externeId ?? null,
      zustaendigId: formGroup.value.zustaendig?.id ?? null,
      anzahlStellplaetze: formGroup.value.anzahlStellplaetze ?? null,
      anzahlSchliessfaecher: formGroup.value.anzahlSchliessfaecher ?? null,
      anzahlLademoeglichkeiten: formGroup.value.anzahlLademoeglichkeiten ?? null,
      ueberwacht: formGroup.value.ueberwacht,
      abstellanlagenOrt: formGroup.value.abstellanlagenOrt,
      groessenklasse: formGroup.value.groessenklasse ?? null,
      stellplatzart: formGroup.value.stellplatzart,
      ueberdacht: formGroup.value.ueberdacht ?? false,
      gebuehrenProTag: this.convertNumberToEuroCent(formGroup.value.gebuehrenProTag),
      gebuehrenProMonat: this.convertNumberToEuroCent(formGroup.value.gebuehrenProMonat),
      gebuehrenProJahr: this.convertNumberToEuroCent(formGroup.value.gebuehrenProJahr),
      beschreibung: formGroup.value.beschreibung ?? null,
      weitereInformation: formGroup.value.weitereInformation ?? null,
      status: formGroup.value.status,
    };
  }

  private resetForm(abstellanlage: Abstellanlage | null): void {
    if (abstellanlage) {
      this.formGroup.reset({
        ...abstellanlage,
        geometrie: abstellanlage?.geometrie.coordinates,
        gebuehrenProTag: this.convertEuroCentToNumber(abstellanlage.gebuehrenProTag ?? null),
        gebuehrenProMonat: this.convertEuroCentToNumber(abstellanlage.gebuehrenProMonat ?? null),
        gebuehrenProJahr: this.convertEuroCentToNumber(abstellanlage.gebuehrenProJahr ?? null),
      });
    } else {
      this.formGroup.reset({
        zustaendig: this.organisationAktuellerBenutzer,
        quellSystem: AbstellanlagenQuellSystem.RADVIS,
      });
    }
    if (this.canEdit) {
      this.formGroup.enable();
      this.setEnabledStateOfGroessenklasse(
        this.formGroup.get('abstellanlagenOrt')?.value === AbstellanlagenOrt.BIKE_AND_RIDE
      );
    } else {
      this.formGroup.disable();
    }
    this.formGroup.get('quellSystem')?.disable();
  }

  private convertNumberToEuroCent(num: number | null): number | null {
    if (num === null) return null;
    return +num.toFixed(2).replace('.', '');
  }

  private convertEuroCentToNumber(num: number | null): number | null {
    if (num === null) return null;
    return num > 0 ? num / 100 : 0;
  }
}
