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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { Barriere } from 'src/app/viewer/barriere/models/barriere';
import { BarriereFormDetails } from 'src/app/viewer/barriere/models/barriere-form-details';
import { BARRIEREN } from 'src/app/viewer/barriere/models/barriere.infrastruktur';
import { BARRIEREN_FORM } from 'src/app/viewer/barriere/models/barrieren-form';
import { Markierung } from 'src/app/viewer/barriere/models/markierung';
import { SaveBarriereCommand } from 'src/app/viewer/barriere/models/save-barriere-command';
import { Sicherung } from 'src/app/viewer/barriere/models/sicherung';
import { VerbleibendeDurchfahrtsbreite } from 'src/app/viewer/barriere/models/verbleibende-durchfahrtsbreite';
import { BarriereFilterService } from 'src/app/viewer/barriere/services/barriere-filter.service';
import { BarriereRoutingService } from 'src/app/viewer/barriere/services/barriere-routing.service';
import { BarrierenService } from 'src/app/viewer/barriere/services/barrieren.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-barriere-editor',
  templateUrl: './barriere-editor.component.html',
  styleUrls: ['./barriere-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class BarriereEditorComponent implements DiscardableComponent {
  isFetching = false;
  formGroup = new FormGroup({
    netzbezug: new FormControl<Netzbezug | null>(null, RadvisValidators.isNotNullOrEmpty),
    verantwortlicheOrganisation: new FormControl<Verwaltungseinheit | null>(null, RadvisValidators.isNotNullOrEmpty),
    barrierenForm: new FormControl<string>('', RadvisValidators.isNotNullOrEmpty),
    verbleibendeDurchfahrtsbreite: new FormControl<VerbleibendeDurchfahrtsbreite | null>(null),
    sicherung: new FormControl<Sicherung | null>(null),
    markierung: new FormControl<Markierung | null>(null),
    begruendung: new FormControl<string | null>(null, RadvisValidators.maxLength(2000)),
    barriereFormDetails: new FormControl<BarriereFormDetails | null>(null),
  });
  isCreator = false;
  BARRIEREN = BARRIEREN;
  alleOrganisationen$: Promise<Verwaltungseinheit[]>;
  barrierenFormOptions = BARRIEREN_FORM;
  verbleibendeDurchfahrtsbreiteOptions = VerbleibendeDurchfahrtsbreite.options;
  sicherungOptions = Sicherung.options;
  markierungOptions = Markierung.options;

  currentBarriere: Barriere | undefined;
  barriereFormDetailsOptions: EnumOption[] = [];

  get isDirty(): boolean {
    return this.formGroup.dirty;
  }

  get selectedId(): number | undefined {
    const idFromRoute = this.activatedRoute.snapshot.paramMap.get('id');
    if (idFromRoute) {
      return +idFromRoute;
    }
    return undefined;
  }

  constructor(
    private olMapService: OlMapService,
    private viewerRoutingService: ViewerRoutingService,
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    private activatedRoute: ActivatedRoute,
    private barriereService: BarrierenService,
    private barriereFilterService: BarriereFilterService,
    private barriereRoutingService: BarriereRoutingService,
    private changeDetector: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    organisationService: OrganisationenService
  ) {
    this.formGroup.controls.barrierenForm.valueChanges.subscribe(value => {
      this.formGroup.controls.barriereFormDetails.reset();
      if (value) {
        this.barriereFormDetailsOptions = BarriereFormDetails.getOptionsForBarriereForm(value);
        if (BarriereFormDetails.isEnabledForBarriereForm(value)) {
          this.formGroup.controls.barriereFormDetails.enable();
        } else {
          this.formGroup.controls.barriereFormDetails.disable();
        }
      } else {
        this.barriereFormDetailsOptions = [];
        this.formGroup.controls.barriereFormDetails.disable();
      }
    });

    activatedRoute.data.subscribe(d => {
      this.isCreator = d.isCreator;
      this.resetForm(d.barriere);
      this.currentBarriere = d.barriere;
      if (this.currentBarriere?.darfBenutzerBearbeiten === false) {
        this.formGroup.disable();
      }
      this.focusBarriereIntoView();
    });
    this.alleOrganisationen$ = organisationService.getOrganisationen();
    infrastrukturenSelektionService.selectInfrastrukturen(BARRIEREN);
  }

  @HostListener('keydown.escape')
  public onEscape(): void {
    this.onClose();
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  onSave(): void {
    if (!this.formGroup.valid) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    const formValue = this.formGroup.value;
    this.isFetching = true;
    let promise: Promise<void>;
    const command: SaveBarriereCommand = {
      netzbezug: formValue.netzbezug!,
      verantwortlicheOrganisation: formValue.verantwortlicheOrganisation!.id,
      barrierenForm: formValue.barrierenForm!,
      verbleibendeDurchfahrtsbreite: formValue.verbleibendeDurchfahrtsbreite ?? null,
      sicherung: formValue.sicherung ?? null,
      markierung: formValue.markierung ?? null,
      begruendung: formValue.begruendung ?? null,
      barriereFormDetails: formValue.barriereFormDetails ?? null,
    };
    if (this.isCreator) {
      promise = this.barriereService.createBarriere(command).then(newId => {
        this.notifyUserService.inform('Barriere wurde erfolgreich gespeichert.');
        this.formGroup.reset();
        this.barriereRoutingService.toInfrastrukturEditor(newId);
        this.barriereFilterService.refetchData();
      });
    } else {
      invariant(this.selectedId);
      invariant(this.currentBarriere);
      promise = this.barriereService
        .updateBarriere(this.selectedId, {
          ...command,
          version: this.currentBarriere.version,
        })
        .then(updated => {
          this.notifyUserService.inform('Barriere wurde erfolgreich gespeichert.');
          this.currentBarriere = updated;
          this.resetForm(updated);
          this.barriereFilterService.refetchData();
        });
    }

    promise.finally(() => {
      this.isFetching = false;
      this.changeDetector.markForCheck();
    });
  }

  onReset(): void {
    this.resetForm(this.currentBarriere);
  }

  canDiscard: () => boolean = () => {
    return !this.formGroup.dirty;
  };

  private resetForm(barriere: Barriere | undefined): void {
    this.formGroup.reset(barriere);
  }

  private focusBarriereIntoView(): void {
    const toFocus =
      this.currentBarriere?.netzbezug?.kantenBezug[0]?.geometrie.coordinates[0] ||
      this.currentBarriere?.netzbezug?.knotenBezug[0]?.geometrie.coordinates ||
      this.currentBarriere?.netzbezug?.punktuellerKantenBezug[0]?.geometrie.coordinates;

    if (toFocus) {
      this.olMapService.scrollIntoViewByCoordinate(toFocus);
    }
  }
}
