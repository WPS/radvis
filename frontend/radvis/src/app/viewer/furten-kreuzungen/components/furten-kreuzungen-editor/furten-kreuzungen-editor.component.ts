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
import { KNOTENFORMEN, Knotenformen } from 'src/app/shared/models/knotenformen';
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { SharedKnotenFormGroup } from 'src/app/shared/models/shared-knoten-form-group';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { FurtKreuzung } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung';
import { FurtKreuzungTyp } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-typ';
import { FURTEN_KREUZUNGEN } from 'src/app/viewer/furten-kreuzungen/models/furten-kreuzungen.infrastruktur';
import { GruenAnforderung } from 'src/app/viewer/furten-kreuzungen/models/gruen-anforderung';
import { LichtsignalAnlageEigenschaften } from 'src/app/viewer/furten-kreuzungen/models/lichtsignal-anlage-eigenschaften';
import { Linksabbieger } from 'src/app/viewer/furten-kreuzungen/models/linksabbieger';
import { Rechtsabbieger } from 'src/app/viewer/furten-kreuzungen/models/rechtsabbieger';
import { SaveFurtKreuzungCommand } from 'src/app/viewer/furten-kreuzungen/models/save-furt-kreuzung-command';
import { FurtenKreuzungenFilterService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-filter.service';
import { FurtenKreuzungenRoutingService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-routing.service';
import { FurtenKreuzungenService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-furten-kreuzungen-editor',
  templateUrl: './furten-kreuzungen-editor.component.html',
  styleUrls: ['./furten-kreuzungen-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class FurtenKreuzungenEditorComponent implements DiscardableComponent {
  isFetching = false;
  formGroup = new FormGroup({
    netzbezug: new FormControl<Netzbezug | null>(null, RadvisValidators.isNotNullOrEmpty),
    verantwortlicheOrganisation: new FormControl<Verwaltungseinheit | null>(null, RadvisValidators.isNotNullOrEmpty),
    kommentar: new FormControl<string | null>(null, RadvisValidators.maxLength(2000)),
    typ: new FormControl<FurtKreuzungTyp | null>(null, RadvisValidators.isNotNullOrEmpty),
    radnetzKonform: new FormControl<boolean>(false),
    furtKreuzungMusterloesung: new FormControl<string | null>({ value: null, disabled: true }),
    shared: new SharedKnotenFormGroup(true),
    lichtsignalAnlageEigenschaften: new FormGroup({
      fahrradSignal: new FormControl<boolean>(false, { nonNullable: true }),
      gruenVorlauf: new FormControl<boolean>(false, { nonNullable: true }),
      getrenntePhasen: new FormControl<boolean>(false, { nonNullable: true }),
      rechtsabbieger: new FormControl<Rechtsabbieger | null>(null, RadvisValidators.isNotNullOrEmpty),
      linksabbieger: new FormControl<Linksabbieger | null>(null, RadvisValidators.isNotNullOrEmpty),
      vorgezogeneHalteLinie: new FormControl<boolean>(false, { nonNullable: true }),
      radAufstellflaeche: new FormControl<boolean>(false, { nonNullable: true }),
      gruenAnforderung: new FormControl<GruenAnforderung | null>(null, RadvisValidators.isNotNullOrEmpty),
      umlaufzeit: new FormControl<number | null>(null, [
        RadvisValidators.isPositiveInteger,
        RadvisValidators.max(9999),
      ]),
    }),
  });

  isCreator = false;
  FURTEN_KREUZUNGEN = FURTEN_KREUZUNGEN;
  alleOrganisationen$: Promise<Verwaltungseinheit[]>;
  currentFurtKreuzung: FurtKreuzung | undefined;
  typOptions = FurtKreuzungTyp.options;
  gruenAnforderungOptions = GruenAnforderung.options;
  linksabbiegerOptions = Linksabbieger.options;
  rechtsabbiegerOptions = Rechtsabbieger.options;
  knotenFormOptions = KNOTENFORMEN;
  musterloesungOptions$: Promise<EnumOption[]>;
  isLSAKnotenForm = false;

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

  protected get sharedKnotenFormGroup(): SharedKnotenFormGroup {
    return this.formGroup.controls.shared;
  }

  constructor(
    private olMapService: OlMapService,
    private viewerRoutingService: ViewerRoutingService,
    private activatedRoute: ActivatedRoute,
    private furtenKreuzungenService: FurtenKreuzungenService,
    private furtenKreuzungenFilterService: FurtenKreuzungenFilterService,
    private furtenKreuzungenRoutingService: FurtenKreuzungenRoutingService,
    infrastrukturenSelektionService: InfrastrukturenSelektionService,
    private changeDetector: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    organisationService: OrganisationenService
  ) {
    this.alleOrganisationen$ = organisationService.getOrganisationen();
    infrastrukturenSelektionService.selectInfrastrukturen(FURTEN_KREUZUNGEN);
    this.musterloesungOptions$ = furtenKreuzungenService.getAllMusterloesungen();

    this.formGroup.controls.radnetzKonform.valueChanges.subscribe(v => {
      if (v) {
        this.formGroup.controls.furtKreuzungMusterloesung.enable();
      } else {
        this.formGroup.controls.furtKreuzungMusterloesung.disable();
        this.formGroup.controls.furtKreuzungMusterloesung.reset();
      }
    });

    this.formGroup.controls.lichtsignalAnlageEigenschaften.disable();

    this.formGroup.controls.shared.controls.knotenForm.valueChanges.subscribe(v => {
      if (v && Knotenformen.isLSAKnotenForm(v)) {
        this.formGroup.controls.lichtsignalAnlageEigenschaften.enable();
        this.isLSAKnotenForm = true;
      } else {
        this.formGroup.controls.lichtsignalAnlageEigenschaften.disable();
        this.formGroup.controls.lichtsignalAnlageEigenschaften.reset();
        this.isLSAKnotenForm = false;
      }
    });

    activatedRoute.data.subscribe(d => {
      this.isCreator = d.isCreator;
      this.resetForm(d.furtKreuzung);
      this.currentFurtKreuzung = d.furtKreuzung;
      if (this.currentFurtKreuzung?.benutzerDarfBearbeiten === false) {
        this.formGroup.disable();
      }
      this.focusFurtKreuzungIntoView();
    });
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
    let lsa: LichtsignalAnlageEigenschaften | null = null;
    if (formValue.lichtsignalAnlageEigenschaften) {
      lsa = {
        fahrradSignal: formValue.lichtsignalAnlageEigenschaften.fahrradSignal ?? false,
        gruenVorlauf: formValue.lichtsignalAnlageEigenschaften.gruenVorlauf ?? false,
        getrenntePhasen: formValue.lichtsignalAnlageEigenschaften.getrenntePhasen ?? false,
        rechtsabbieger: formValue.lichtsignalAnlageEigenschaften.rechtsabbieger!,
        linksabbieger: formValue.lichtsignalAnlageEigenschaften.linksabbieger!,
        vorgezogeneHalteLinie: formValue.lichtsignalAnlageEigenschaften.vorgezogeneHalteLinie ?? false,
        radAufstellflaeche: formValue.lichtsignalAnlageEigenschaften.radAufstellflaeche ?? false,
        gruenAnforderung: formValue.lichtsignalAnlageEigenschaften.gruenAnforderung!,
        umlaufzeit: formValue.lichtsignalAnlageEigenschaften.umlaufzeit ?? null,
      };
    }
    const command: SaveFurtKreuzungCommand = {
      netzbezug: formValue.netzbezug!,
      verantwortlicheOrganisation: formValue.verantwortlicheOrganisation!.id,
      typ: formValue.typ!,
      knotenForm: formValue.shared!.knotenForm!,
      radnetzKonform: formValue.radnetzKonform ?? false,
      kommentar: formValue.kommentar ?? null,
      furtKreuzungMusterloesung: formValue.furtKreuzungMusterloesung ?? null,
      lichtsignalAnlageEigenschaften: lsa,
      bauwerksmangel: formValue.shared?.bauwerksmangel?.vorhanden ?? null,
      bauwerksmangelArt: formValue.shared?.bauwerksmangel?.bauwerksmangelArt ?? null,
      querungshilfeDetails: formValue.shared?.querungshilfeDetails ?? null,
    };
    if (this.isCreator) {
      promise = this.furtenKreuzungenService.createFurtKreuzung(command).then(newId => {
        this.notifyUserService.inform('Furt/Kreuzung wurde erfolgreich gespeichert.');
        this.formGroup.reset();
        this.furtenKreuzungenRoutingService.toInfrastrukturEditor(newId);
        this.furtenKreuzungenFilterService.refetchData();
      });
    } else {
      invariant(this.selectedId);
      invariant(this.currentFurtKreuzung);
      command.version = this.currentFurtKreuzung.version;
      promise = this.furtenKreuzungenService.updateFurtKreuzung(this.selectedId, command).then(updated => {
        this.notifyUserService.inform('Furt/Kreuzung wurde erfolgreich gespeichert.');
        this.currentFurtKreuzung = updated;
        this.resetForm(updated);
        this.furtenKreuzungenFilterService.refetchData();
      });
    }

    promise.finally(() => {
      this.isFetching = false;
      this.changeDetector.markForCheck();
    });
  }

  onReset(): void {
    this.resetForm(this.currentFurtKreuzung);
  }

  canDiscard: () => boolean = () => {
    return !this.formGroup.dirty;
  };

  private resetForm(furtKreuzung: FurtKreuzung | undefined): void {
    const resetValue = {
      ...furtKreuzung,
      lichtsignalAnlageEigenschaften: furtKreuzung?.lichtsignalAnlageEigenschaften ?? {
        linksabbieger: Linksabbieger.UNBEKANNT,
        rechtsabbieger: Rechtsabbieger.UNBEKANNT,
      },
      shared: {
        ...furtKreuzung,
        bauwerksmangel: {
          vorhanden: furtKreuzung?.bauwerksmangel,
          bauwerksmangelArt: furtKreuzung?.bauwerksmangelArt,
        },
      },
    };
    this.formGroup.reset(resetValue);
  }

  private focusFurtKreuzungIntoView(): void {
    const toFocus =
      this.currentFurtKreuzung?.netzbezug?.kantenBezug[0]?.geometrie.coordinates[0] ||
      this.currentFurtKreuzung?.netzbezug?.knotenBezug[0]?.geometrie.coordinates ||
      this.currentFurtKreuzung?.netzbezug?.punktuellerKantenBezug[0]?.geometrie.coordinates;

    if (toFocus) {
      this.olMapService.scrollIntoViewByCoordinate(toFocus);
    }
  }
}
