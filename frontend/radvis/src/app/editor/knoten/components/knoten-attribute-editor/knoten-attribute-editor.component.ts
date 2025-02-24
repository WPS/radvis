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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { Knoten } from 'src/app/editor/knoten/models/knoten';
import { SaveKnotenCommand } from 'src/app/editor/knoten/models/save-knoten-command';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { SharedKnotenFormGroup } from 'src/app/shared/models/shared-knoten-form-group';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'rad-knoten-attribute-editor',
  templateUrl: './knoten-attribute-editor.component.html',
  styleUrls: ['./knoten-attribute-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KnotenAttributeEditorComponent implements DiscardableComponent, OnInit, OnDestroy {
  public gemeindeOptions: Promise<Verwaltungseinheit[]> = Promise.resolve([]);

  public isFetching = false;

  public editingAllowed = true;

  public kommentarMaxLength = 2000;
  public zustandMaxLength = 2000;

  private subscriptions: Subscription[] = [];

  private currentKnoten: Knoten;

  protected get sharedKnotenFormGroup(): SharedKnotenFormGroup {
    return this.knotenFormGroup.controls.shared;
  }

  knotenFormGroup = new FormGroup({
    ortslage: new FormControl({ value: '', disabled: true }),
    gemeinde: new FormControl<Verwaltungseinheit | null>(null),
    landkreis: new FormControl({ value: '', disabled: true }),
    kommentar: new FormControl('', RadvisValidators.maxLength(this.kommentarMaxLength)),
    zustandsbeschreibung: new FormControl('', RadvisValidators.maxLength(this.zustandMaxLength)),
    shared: new SharedKnotenFormGroup(),
  });

  constructor(
    private route: ActivatedRoute,
    private notifyUserService: NotifyUserService,
    private netzService: NetzService,
    private organisationenService: OrganisationenService,
    private errorHandlingService: ErrorHandlingService,
    private changeDetectorRef: ChangeDetectorRef,
    private editorRoutingService: EditorRoutingService,
    private benutzerDetailsService: BenutzerDetailsService
  ) {
    this.currentKnoten = this.route.snapshot.data.knoten;

    this.knotenFormGroup.controls.gemeinde.valueChanges.subscribe(value => {
      const gemeinde = value!;
      if (gemeinde && gemeinde.idUebergeordneteOrganisation) {
        this.organisationenService
          .getOrganisation(gemeinde.idUebergeordneteOrganisation)
          .then(uebergoerdneteOrganisation => {
            this.knotenFormGroup.controls.landkreis.setValue(uebergoerdneteOrganisation.name);
            this.changeDetectorRef.markForCheck();
          })
          .catch(error => this.errorHandlingService.handleError(error, 'Landkreis konnte nicht geladen werden.'));
      } else {
        this.knotenFormGroup.controls.landkreis.setValue(null);
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  @HostListener('window:beforeunload')
  canDiscard(): boolean {
    if (environment.production) {
      return this.knotenFormGroup.pristine;
    }
    return true;
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(() => {
      this.currentKnoten = this.route.snapshot.data.knoten;
      const isNotRadNetzKnoten = this.currentKnoten.quelle !== QuellSystem.RadNETZ;
      const liegtInZustaendigkeitsbereich = this.currentKnoten.liegtInZustaendigkeitsbereich;
      this.editingAllowed =
        isNotRadNetzKnoten && (liegtInZustaendigkeitsbereich || this.benutzerDetailsService.canEditGesamtesNetz());
      if (this.editingAllowed) {
        this.enableForm();
      }
      this.resetForm(this.currentKnoten);
      if (!this.editingAllowed) {
        this.knotenFormGroup.disable({ emitEvent: false });
      }
    });
    this.gemeindeOptions = this.organisationenService.getGemeinden();
  }

  private resetForm(knoten: Knoten): void {
    this.knotenFormGroup.reset({
      ...knoten,
      landkreis: knoten?.landkreis?.name || null,
    });
    this.sharedKnotenFormGroup.reset({
      ...knoten,
      bauwerksmangel: {
        vorhanden: knoten.bauwerksmangel,
        bauwerksmangelArt: knoten.bauwerksmangelArt,
      },
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  onReset(): void {
    this.resetForm(this.currentKnoten);
  }

  onClose(): void {
    this.editorRoutingService.toKnotenEditor();
  }

  onSave(): void {
    if (this.knotenFormGroup.pristine) {
      return;
    }

    if (this.knotenFormGroup.invalid) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    const command: SaveKnotenCommand = {
      id: this.currentKnoten.id,
      gemeinde: this.knotenFormGroup.value.gemeinde?.id || null,
      kommentar: this.knotenFormGroup.controls.kommentar.value || null,
      zustandsbeschreibung: this.knotenFormGroup.controls.zustandsbeschreibung.value || null,
      knotenForm: this.knotenFormGroup.controls.shared.controls.knotenForm.value || null,
      knotenVersion: this.currentKnoten.knotenVersion,
      querungshilfeDetails: this.knotenFormGroup.value.shared?.querungshilfeDetails ?? null,
      bauwerksmangel: this.knotenFormGroup.value.shared?.bauwerksmangel?.vorhanden ?? null,
      bauwerksmangelArt: this.knotenFormGroup.value.shared?.bauwerksmangel?.bauwerksmangelArt ?? null,
    };

    this.isFetching = true;

    this.netzService
      .saveKnoten(command)
      .then(savedKnoten => {
        this.currentKnoten = savedKnoten;
        this.resetForm(savedKnoten);
        this.notifyUserService.inform('Knoten wurde erfolgreich gespeichert.');
      })
      .finally(() => {
        this.isFetching = false;
        this.changeDetectorRef.markForCheck();
      });
  }

  private enableForm(): void {
    this.knotenFormGroup.enable({ emitEvent: false });
    this.knotenFormGroup.controls.landkreis.disable({ emitEvent: false });
    this.knotenFormGroup.controls.ortslage.disable({ emitEvent: false });
  }
}
