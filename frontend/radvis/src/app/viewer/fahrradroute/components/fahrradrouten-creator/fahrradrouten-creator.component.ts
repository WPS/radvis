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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Subscription } from 'rxjs';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { FahrradrouteAttributeEditorComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-attribute-editor/fahrradroute-attribute-editor.component';
import { CreateFahrradrouteCommand } from 'src/app/viewer/fahrradroute/models/create-fahrradroute-command';
import { FahrradrouteFilterService } from 'src/app/viewer/fahrradroute/services/fahrradroute-filter.service';
import { FahrradrouteProfilService } from 'src/app/viewer/fahrradroute/services/fahrradroute-profil.service';
import { FahrradrouteRoutingService } from 'src/app/viewer/fahrradroute/services/fahrradroute-routing.service';
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';

@Component({
  selector: 'rad-fahrradrouten-creator',
  templateUrl: './fahrradrouten-creator.component.html',
  styleUrls: ['./fahrradrouten-creator.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FahrradroutenCreatorComponent implements OnDestroy, DiscardableComponent {
  public formGroup: UntypedFormGroup;
  public readonly MAX_LENGTH_BESCHREIBUNG = FahrradrouteAttributeEditorComponent.MAX_LENGTH_BESCHREIBUNG;
  public readonly MAX_LENGTH_TEXT = FahrradrouteAttributeEditorComponent.MAX_LENGTH_TEXT;
  public isFetching = false;
  isRouting = false;

  public kategorieOptions = FahrradrouteKategorie.options;

  private saved = false;

  private subscriptions: Subscription[] = [];

  constructor(
    private fahrradrouteRoutingService: FahrradrouteRoutingService,
    private viewerRoutingService: ViewerRoutingService,
    private fahrradrouteService: FahrradrouteService,
    private changeDetectorRef: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    private fahrradrouteFilterService: FahrradrouteFilterService,
    private fahrradrouteProfilService: FahrradrouteProfilService
  ) {
    this.formGroup = new UntypedFormGroup({
      netzbezug: new UntypedFormControl(null, [RadvisValidators.isNotNullOrEmpty]),
      name: new UntypedFormControl(null, [
        RadvisValidators.isNotNullOrEmpty,
        RadvisValidators.maxLength(this.MAX_LENGTH_TEXT),
      ]),
      beschreibung: new UntypedFormControl(null, [
        RadvisValidators.isNotNullOrEmpty,
        RadvisValidators.maxLength(this.MAX_LENGTH_BESCHREIBUNG),
      ]),
      kategorie: new UntypedFormControl(null, [RadvisValidators.isNotNullOrEmpty]),
    });

    this.subscriptions.push(
      (this.formGroup.get('netzbezug') as AbstractControl).valueChanges.subscribe(netzbezug => {
        if (netzbezug && netzbezug.geometrie) {
          const name = this.formGroup.get('name')?.value ?? '';
          this.fahrradrouteProfilService.updateCurrentRouteProfil({
            name,
            geometrie: netzbezug.geometrie,
            profilEigenschaften: netzbezug.profilEigenschaften,
          });
        }
        changeDetectorRef.markForCheck();
      })
    );

    this.subscriptions.push(
      (this.formGroup.get('name') as AbstractControl).valueChanges.subscribe(name => {
        const netzbezug = this.formGroup.get('netzbezug')?.value;
        this.fahrradrouteProfilService.updateCurrentRouteProfil({
          name: name ?? '',
          geometrie: netzbezug?.geometrie,
          profilEigenschaften: netzbezug?.profilEigenschaften,
        });
        changeDetectorRef.markForCheck();
      })
    );
  }

  onOeffneHoehenprofil(): void {
    this.fahrradrouteProfilService.showCurrentRouteProfile();
  }

  canDiscard(): boolean {
    return this.saved || this.formGroup.pristine;
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.fahrradrouteProfilService.hideCurrentRouteProfile();
  }

  onReset(): void {
    this.formGroup.reset();
  }

  onSave(): void {
    if (this.formGroup.pristine) {
      return;
    }

    if (this.formGroup.invalid) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    this.isFetching = true;

    const command: CreateFahrradrouteCommand = {
      name: this.formGroup.get('name')?.value,
      beschreibung: this.formGroup.get('beschreibung')?.value,
      kategorie: this.formGroup.get('kategorie')?.value,
      stuetzpunkte: {
        coordinates: this.formGroup.get('netzbezug')?.value.stuetzpunkte,
        type: 'LineString',
      } as LineStringGeojson,
      kantenIDs: this.formGroup.get('netzbezug')?.value.kantenIDs,
      routenVerlauf: this.formGroup.get('netzbezug')?.value.geometrie,
      profilEigenschaften: this.formGroup.get('netzbezug')?.value.profilEigenschaften,
      customProfileId: this.formGroup.get('netzbezug')?.value.customProfileId,
    };

    this.fahrradrouteService
      .createFahrradroute(command)
      .then(id => {
        this.saved = true;
        this.notifyUserService.inform('Fahrradroute wurde erfolgreich gespeichert.');
        this.fahrradrouteRoutingService.toInfrastrukturEditor(id);
      })
      .finally(() => {
        this.isFetching = false;
        this.fahrradrouteFilterService.refetchData();
        this.changeDetectorRef.markForCheck();
      });
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }
}
