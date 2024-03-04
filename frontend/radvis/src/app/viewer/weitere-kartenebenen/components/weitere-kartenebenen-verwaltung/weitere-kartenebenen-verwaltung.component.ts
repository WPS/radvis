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

import { HttpClient } from '@angular/common/http';
import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { AbstractControl, UntypedFormArray, UntypedFormControl, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import {
  defaultDateiLayerZIndex,
  neueWeitereKartenebenenDefaultZIndex,
} from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { PredefinedWeitereKartenebenen } from 'src/app/viewer/weitere-kartenebenen/models/predefined-weitere-kartenebenen';
import { SaveWeitereKartenebeneCommand } from 'src/app/viewer/weitere-kartenebenen/models/save-weitere-kartenebene-command';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { WeitereKartenebeneTyp } from 'src/app/viewer/weitere-kartenebenen/models/weitereKartenebeneTyp';
import { DateiLayerService } from 'src/app/viewer/weitere-kartenebenen/services/datei-layer.service';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';

@Component({
  selector: 'rad-weitere-kartenebenen-verwaltung',
  templateUrl: './weitere-kartenebenen-verwaltung.component.html',
  styleUrls: ['./weitere-kartenebenen-verwaltung.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeitereKartenebenenVerwaltungComponent implements OnDestroy, AfterViewInit {
  public readonly QUELLANGABE_MAX_LENGTH = 1000;

  public showLayerAusDateiFeatureToggl: boolean;

  public PredefinedWeitereKartenebenen = PredefinedWeitereKartenebenen;
  public layerTypOptions: EnumOption[] = WeitereKartenebeneTyp.options;

  public disableAnimation = true;
  public saving = false;
  public weitereKartenebenenFormArray: UntypedFormArray;
  public parsingQuellangabeForLayerIndex: number | null = null;
  public dateiLayerKartenebenen: SaveWeitereKartenebeneCommand[] = [];

  private subscriptions: Subscription[] = [];

  constructor(
    private weitereKartenebenenService: WeitereKartenebenenService,
    private dateiLayerService: DateiLayerService,
    private changeDetectorRef: ChangeDetectorRef,
    private matDialogRef: MatDialogRef<WeitereKartenebenenVerwaltungComponent>,
    private notifyUserService: NotifyUserService,
    private httpClient: HttpClient,
    private featureTogglzService: FeatureTogglzService,
    private manualRoutingService: ManualRoutingService
  ) {
    this.showLayerAusDateiFeatureToggl = this.featureTogglzService.isToggledOn(
      FeatureTogglzService.TOGGLZ_DATEILAYER_HOCHLADEN_ANZEIGEN
    );

    this.weitereKartenebenenFormArray = new UntypedFormArray([]);
    this.subscriptions.push(
      this.weitereKartenebenenService.weitereKartenebenen$.subscribe(layers => {
        this.resetForm(layers);
        this.changeDetectorRef.markForCheck();
        this.weitereKartenebenenFormArray.markAsPristine();
      }),
      this.dateiLayerService.allDateiLayers$.subscribe(layers => {
        this.dateiLayerKartenebenen = layers.map(layer => {
          return {
            name: layer.name,
            quellangabe: layer.quellangabe,
            url:
              window.location.origin +
              '/api/geoserver/saml/datei-layer/wms?LAYERS=datei-layer:' +
              layer.geoserverLayerName +
              '&TILED=true',
            weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
            deckkraft: 1.0,
            zoomstufe: 8.7,
            zindex: defaultDateiLayerZIndex,
            dateiLayerId: layer.id,
          } as SaveWeitereKartenebeneCommand;
        });
      })
    );
  }

  public get formArrayAsFormGroupArray(): UntypedFormGroup[] {
    return this.weitereKartenebenenFormArray.controls as UntypedFormGroup[];
  }

  ngAfterViewInit(): void {
    // workaround for https://github.com/angular/components/issues/14759
    setTimeout(() => (this.disableAnimation = false));
    this.dateiLayerService.refreshDateiLayers();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  public isWFS(weitereKartenebeneTyp: WeitereKartenebeneTyp | null): boolean {
    return weitereKartenebeneTyp === WeitereKartenebeneTyp.WFS;
  }

  public onSave(): void {
    this.saveLayers();
  }

  public onDeleteLayer(atIndex: number): void {
    this.weitereKartenebenenFormArray.removeAt(atIndex);
    this.weitereKartenebenenFormArray.markAsDirty();
  }

  public onAddWeitereKartenebenen(): void {
    const newControl = this.createLayerFormGroup();
    if (this.weitereKartenebenenService.weitereKartenebenen?.length > 0) {
      const maxZindex = this.weitereKartenebenenService.weitereKartenebenen.reduce(
        (max, weitereKartenebene) => Math.max(max, weitereKartenebene.zindex),
        0
      );
      newControl.patchValue({ zindex: maxZindex + 1 });
    }
    this.weitereKartenebenenFormArray.push(newControl);
    this.weitereKartenebenenFormArray.markAsDirty();
  }

  public onAddPredefinedWeitereKartenebenen(predefined: SaveWeitereKartenebeneCommand): void {
    const fg = this.createLayerFormGroup();
    fg.patchValue(predefined);
    this.weitereKartenebenenFormArray.push(fg);
    this.weitereKartenebenenFormArray.markAsDirty();
  }

  public onGenerateQuellangabe(layerFormGroup: UntypedFormGroup, index: number): void {
    const url = new URL(layerFormGroup.value.url);

    url.searchParams.set('request', 'GetCapabilities');
    url.searchParams.set('version', '1.1.0');
    url.searchParams.set('format', 'text/xml');
    url.searchParams.set('service', layerFormGroup.value.weitereKartenebeneTyp);

    this.parsingQuellangabeForLayerIndex = index;
    this.httpClient
      .get(url.toString(), { responseType: 'text' })
      .toPromise()
      .then(result => {
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(result, 'text/xml');
        const accessConstraints =
          xmlDoc.getElementsByTagName('AccessConstraints')[0]?.textContent ??
          xmlDoc.getElementsByTagName('ows:AccessConstraints')[0]?.textContent;
        if (!accessConstraints) {
          this.notifyUserService.inform(
            'Die Lizenzbedingungen konnte nicht automatisch ermittelt werden. Bitte ggf. manuell hinzufügen.'
          );
        }

        const quellangabe = `${url.host}${url.pathname}\n\n${accessConstraints}`;
        layerFormGroup.patchValue({ quellangabe });
      })
      .catch(() => {
        this.notifyUserService.inform(
          'Die Lizenzbedingungen konnte nicht automatisch ermittelt werden. Bitte ggf. manuell hinzufügen.'
        );

        const quellangabe = `${url.host}${url.pathname}`;
        layerFormGroup.patchValue({ quellangabe });
      })
      .finally(() => {
        this.parsingQuellangabeForLayerIndex = null;
        this.changeDetectorRef.markForCheck();
      });
  }

  public openManualAnzeigeordnungLayerViewer(): void {
    this.manualRoutingService.openAnzeigeordnungViewer();
  }

  private saveLayers(): void {
    if (this.weitereKartenebenenFormArray.invalid) {
      this.notifyUserService.warn('Die Eingabe ist nicht korrekt und konnte nicht gespeichert werden.');
      return;
    }
    this.saving = true;
    this.weitereKartenebenenService
      .save(this.readForm())
      .then(() => {
        this.matDialogRef.close();
      })
      .catch(() => {
        this.saving = false;
        this.changeDetectorRef.markForCheck();
      });
  }

  private readForm(): SaveWeitereKartenebeneCommand[] {
    return this.weitereKartenebenenFormArray.value.map((v: any) => {
      return {
        ...v,
        farbe: v.weitereKartenebeneTyp === WeitereKartenebeneTyp.WMS ? undefined : v.farbe,
      } as SaveWeitereKartenebeneCommand;
    });
  }

  private resetForm(layers: WeitereKartenebene[]): void {
    this.weitereKartenebenenFormArray.clear();
    layers.forEach(l => {
      const fg = this.createLayerFormGroup();
      fg.patchValue(l);
      this.weitereKartenebenenFormArray.push(fg);
    });
  }

  private createLayerFormGroup(): UntypedFormGroup {
    return new UntypedFormGroup(
      {
        id: new UntypedFormControl(null),
        name: new UntypedFormControl('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
        url: new UntypedFormControl('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.url]),
        farbe: new UntypedFormControl('#55dd66'),
        deckkraft: new UntypedFormControl(1.0),
        zoomstufe: new UntypedFormControl(8.7),
        zindex: new UntypedFormControl(neueWeitereKartenebenenDefaultZIndex, [
          RadvisValidators.isNotNullOrEmpty,
          RadvisValidators.isPositiveInteger,
        ]),
        weitereKartenebeneTyp: new UntypedFormControl(WeitereKartenebeneTyp.WFS),
        quellangabe: new UntypedFormControl(null, [
          RadvisValidators.isNotNullOrEmpty,
          RadvisValidators.maxLength(this.QUELLANGABE_MAX_LENGTH),
        ]),
        dateiLayerId: new UntypedFormControl(null),
      },
      { validators: this.layerFarbeValidator }
    );
  }

  private layerFarbeValidator: ValidatorFn = (c: AbstractControl) => {
    if (c.value.weitereKartenebeneTyp === WeitereKartenebeneTyp.WFS && !c.value.farbe) {
      return { farbeFehlt: 'Es muss eine Farbe angegeben werden.' };
    }
    return null;
  };
}
