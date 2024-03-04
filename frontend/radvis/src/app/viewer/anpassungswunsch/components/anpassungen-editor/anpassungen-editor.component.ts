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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener, OnDestroy } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Params } from '@angular/router';
import { Coordinate } from 'ol/coordinate';
import { Subscription } from 'rxjs';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { Anpassungswunsch } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch';
import { AnpassungswunschKategorie } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-kategorie';
import { AnpassungswunschStatus } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-status';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { SaveAnpassungswunschCommand } from 'src/app/viewer/anpassungswunsch/models/save-anpassungswunsch-command';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { AnpassungswunschFilterService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-filter.service';
import { AnpassungswunschService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-anpassungen-editor',
  templateUrl: './anpassungen-editor.component.html',
  styleUrls: ['./anpassungen-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnpassungenEditorComponent implements OnDestroy, DiscardableComponent {
  isFetching = false;
  AnpassungswunschStatus = AnpassungswunschStatus;
  AnpassungswunschKategorie = AnpassungswunschKategorie;
  BESCHREIBUNG_MAX_LENGTH = Anpassungswunsch.BESCHREIBUNG_MAX_LENGTH;
  iconName = ANPASSUNGSWUNSCH.iconFileName;

  public formGroup = new UntypedFormGroup({
    geometrie: new UntypedFormControl(null),
    beschreibung: new UntypedFormControl(null, [
      RadvisValidators.isNotNullOrEmpty,
      RadvisValidators.maxLength(Anpassungswunsch.BESCHREIBUNG_MAX_LENGTH),
    ]),
    status: new UntypedFormControl(null, [RadvisValidators.isNotNullOrEmpty]),
    erstellung: new UntypedFormControl(null),
    aenderung: new UntypedFormControl(null),
    benutzerLetzteAenderung: new UntypedFormControl(null),
    verantwortlicheOrganisation: new UntypedFormControl(null),
    kategorie: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
  });

  currentAnpassungswunsch: Anpassungswunsch | undefined;

  alleOrganisationen$: Promise<Verwaltungseinheit[]>;

  isCreator = false;
  canSeeRadvisEditor = false;

  private subscriptions: Subscription[] = [];

  private get currentId(): number | null {
    return this.activatedRoute.snapshot.data.anpassungswunsch?.id || null;
  }

  public get selectedCoordinate(): Coordinate | null {
    return this.formGroup.value.geometrie ?? null;
  }

  public get isDirty(): boolean {
    return this.formGroup.dirty;
  }

  public get editorTitel(): string {
    return `Anpassungswunsch ${this.isCreator ? 'erstellen' : 'bearbeiten'}`;
  }

  private forceClose = false;

  constructor(
    olMapService: OlMapService,
    private activatedRoute: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    private editorRoutingService: EditorRoutingService,
    private anpassungswunschService: AnpassungswunschService,
    private anpassungenRoutingService: AnpassungenRoutingService,
    private notifyUserService: NotifyUserService,
    private changeDetector: ChangeDetectorRef,
    private anpassungswunschFilterService: AnpassungswunschFilterService,
    organisationenService: OrganisationenService,
    nutzerDetailsService: BenutzerDetailsService
  ) {
    this.canSeeRadvisEditor = nutzerDetailsService.canEdit();

    this.subscriptions.push(
      activatedRoute.data.subscribe(data => {
        this.isCreator = data.isCreator;
        this.currentAnpassungswunsch = data.anpassungswunsch;
        this.resetForm(this.currentAnpassungswunsch);
        if (this.currentAnpassungswunsch?.canEdit === false) {
          this.formGroup.disable();
        }
        this.changeDetector.markForCheck();
        const coordinates = this.currentAnpassungswunsch?.geometrie?.coordinates;
        if (coordinates) {
          olMapService.scrollIntoViewByCoordinate(coordinates);
        }
      })
    );

    this.alleOrganisationen$ = organisationenService.getOrganisationen();
  }

  @HostListener('keydown.escape')
  public onEscape(): void {
    this.onClose();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  onSave(): void {
    if (!this.formGroup.valid) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    let savePromise: Promise<void>;

    const formValue = this.formGroup.value;
    const command: SaveAnpassungswunschCommand = {
      beschreibung: formValue.beschreibung,
      geometrie: { coordinates: formValue.geometrie, type: 'Point' },
      status: formValue.status,
      kategorie: formValue.kategorie,
      verantwortlicheOrganisation: formValue.verantwortlicheOrganisation?.id,
    };

    this.isFetching = true;

    if (this.isCreator) {
      savePromise = this.anpassungswunschService.createAnpassungswunsch(command).then(newAnp => {
        this.resetForm();
        this.anpassungenRoutingService.toInfrastrukturEditor(newAnp.id);
      });
    } else {
      invariant(this.currentId);
      savePromise = this.anpassungswunschService.updateAnpassungswunsch(this.currentId, command).then(updated => {
        this.resetForm(updated);
        this.currentAnpassungswunsch = updated;
      });
    }

    savePromise.finally(() => {
      this.isFetching = false;
      this.anpassungswunschFilterService.refetchData();
      this.changeDetector.markForCheck();
    });
  }

  onReset(): void {
    this.resetForm(this.currentAnpassungswunsch);
  }

  onClose(): void {
    this.forceClose = true;
    this.viewerRoutingService.toViewer();
  }

  canDiscard = (): boolean => {
    return this.forceClose || !this.isDirty;
  };

  get viewParamForCenterAtCoordinate(): Params {
    return this.editorRoutingService.getViewParamForCenterAtCoordinate(this.selectedCoordinate);
  }

  get linkToEditor(): string {
    return this.editorRoutingService.getEditorRoute();
  }

  private resetForm(anpassungswunsch?: Anpassungswunsch): void {
    if (anpassungswunsch) {
      this.formGroup.reset({ ...anpassungswunsch, geometrie: anpassungswunsch.geometrie.coordinates });
    } else {
      this.formGroup.reset({ status: AnpassungswunschStatus.OFFEN });
    }
  }
}
