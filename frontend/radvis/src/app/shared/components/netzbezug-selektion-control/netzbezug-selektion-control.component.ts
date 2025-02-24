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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  forwardRef,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';
import { asString } from 'ol/color';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { BedienhinweisService } from 'src/app/shared/services/bedienhinweis.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

enum SelektionsModus {
  STANDARD,
  PUNKT,
  SCHERE,
}

@Component({
  selector: 'rad-netzbezug-selektion-control',
  templateUrl: './netzbezug-selektion-control.component.html',
  styleUrls: ['./netzbezug-selektion-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => NetzbezugSelektionControlComponent), multi: true },
    {
      provide: NG_VALIDATORS,
      useExisting: NetzbezugSelektionControlComponent,
      multi: true,
    },
  ],
  standalone: false,
})
export class NetzbezugSelektionControlComponent
  extends AbstractFormControl<Netzbezug>
  implements Validator, OnDestroy, OnInit
{
  @Input()
  selectedId?: number;

  @Input()
  layerId!: string;

  @Input()
  netzbezugHighlightToggle = true;

  @Input()
  canZuruecksetzen = false;

  public validInputColor = asString(MapStyles.VALID_INPUT_COLOR);
  public selectionMode = false;
  public zweiseitigeNetzanzeige = false;

  public disabled = false;
  public netzbezug: Netzbezug | null = null;
  private originalNetzbezug: Netzbezug | null = null;

  private bearbeitungsModus: SelektionsModus = SelektionsModus.STANDARD;

  private readonly STANDARD_BEDIEN_HINWEIS =
    'Kanten(-Segment) mit Klick auswählen; mit Strg+Klick abwählen. Knoten mit Klick aus-/abwählen.';
  private readonly SCHERE_BEDIEN_HINWEIS = 'Kante zum Teilen anklicken (nur Auswahl)';
  private readonly POINT_BEDIEN_HINWEIS = 'Kante anklicken, um Punkt hinzuzufügen; Punkt mit Strg+Klick entfernen';

  public get pointSelectionMode(): boolean {
    return this.bearbeitungsModus === SelektionsModus.PUNKT;
  }

  public get schereSelectionMode(): boolean {
    return this.bearbeitungsModus === SelektionsModus.SCHERE;
  }

  constructor(
    private changeDetectorRef: ChangeDetectorRef,
    private olMapService: OlMapService,
    private bedienhinweisService: BedienhinweisService
  ) {
    super();
  }

  @HostListener('document:keydown.escape')
  public onEscape(): void {
    if (this.schereSelectionMode || this.pointSelectionMode) {
      this.selectStandardBearbeitungsModus();
    }
  }

  ngOnInit(): void {
    invariant(this.layerId);
  }

  ngOnDestroy(): void {
    this.olMapService.resetCursor();
    this.bedienhinweisService.hideBedienhinweis();
  }

  get netzbezugSelected(): boolean {
    if (this.netzbezug === null) {
      return false;
    }
    return (
      this.netzbezug.kantenBezug.length > 0 ||
      this.netzbezug.knotenBezug.length > 0 ||
      this.netzbezug.punktuellerKantenBezug.length > 0
    );
  }

  validate(): ValidationErrors | null {
    return !this.netzbezugSelected ? { noSelection: 'Kein Netzbezug ausgewählt' } : null;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.changeDetectorRef.markForCheck();
  }

  writeValue(netzbezug: Netzbezug | null): void {
    this.originalNetzbezug = netzbezug;
    this.netzbezug = netzbezug;
    this.selektionBeenden();
    if (!this.netzbezugSelected) {
      this.onSelektionStarten();
    }
    this.changeDetectorRef.markForCheck();
  }

  onSelektionBeenden(): void {
    this.selektionBeenden();
  }

  onZuruecksetzen(): void {
    this.netzbezug = this.originalNetzbezug;
    this.onChange(this.originalNetzbezug);
  }

  onSelektionStarten(): void {
    this.selectionMode = true;
    this.selectStandardBearbeitungsModus();
  }

  onNetzbezugChange(newNetzbezug: Netzbezug): void {
    this.netzbezug = newNetzbezug;
    this.onChange(this.netzbezug);
  }

  onTogglePunktModus(): void {
    if (this.pointSelectionMode) {
      this.selectStandardBearbeitungsModus();
    } else {
      this.bearbeitungsModus = SelektionsModus.PUNKT;
      this.olMapService.setCursor('point-selection-cursor');
      this.bedienhinweisService.showBedienhinweis(this.POINT_BEDIEN_HINWEIS);
    }
  }

  onToggleSchereModus(): void {
    if (this.schereSelectionMode) {
      this.selectStandardBearbeitungsModus();
    } else {
      this.bearbeitungsModus = SelektionsModus.SCHERE;
      this.olMapService.setCursor('schere-cursor');
      this.bedienhinweisService.showBedienhinweis(this.SCHERE_BEDIEN_HINWEIS);
    }
  }

  onToggleZweiseitigeNetzanzeige(): void {
    this.zweiseitigeNetzanzeige = !this.zweiseitigeNetzanzeige;
  }

  private selectStandardBearbeitungsModus(): void {
    this.bearbeitungsModus = SelektionsModus.STANDARD;
    this.olMapService.resetCursor();
    this.bedienhinweisService.showBedienhinweis(this.STANDARD_BEDIEN_HINWEIS);
  }

  private selektionBeenden(): void {
    this.selectionMode = false;
    this.zweiseitigeNetzanzeige = false;
    this.bedienhinweisService.hideBedienhinweis();
    this.olMapService.resetCursor();
  }
}
