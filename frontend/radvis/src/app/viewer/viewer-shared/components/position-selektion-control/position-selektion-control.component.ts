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
  Input,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';
import { Feature, MapBrowserEvent } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Subscription } from 'rxjs';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { BedienhinweisService } from 'src/app/shared/services/bedienhinweis.service';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { NetzbezugAuswahlModusService } from 'src/app/shared/services/netzbezug-auswahl-modus.service';
import { puntuelleKantenBezuegeVectorLayerZIndex } from 'src/app/shared/models/shared-layer-zindex-config';

@Component({
  selector: 'rad-position-selektion-control',
  templateUrl: './position-selektion-control.component.html',
  styleUrls: ['./position-selektion-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PositionSelektionControlComponent), multi: true },
    {
      provide: NG_VALIDATORS,
      useExisting: PositionSelektionControlComponent,
      multi: true,
    },
  ],
})
export class PositionSelektionControlComponent
  extends AbstractFormControl<Coordinate>
  implements Validator, OnInit, OnDestroy
{
  public static readonly BEDIENHINWEIS = 'Klicken Sie auf die Karte, um einen Ort zuzuweisen.';

  @Input()
  withInfrastrukturIcon: string | undefined;

  public get position(): Coordinate | null {
    if (this.selectCoordinateSource.getFeatures().length === 1) {
      return (this.selectCoordinateSource.getFeatures()[0].getGeometry() as Point).getCoordinates();
    }
    return null;
  }

  public set position(value: Coordinate | null) {
    this.selectCoordinateSource.clear();
    if (value) {
      this.selectCoordinateSource.addFeature(new Feature(new Point(value)));
    }
  }

  public disabled = false;
  public selectionMode = false;

  private subscriptions: Subscription[] = [];
  private readonly selectCoordinateSource = new VectorSource();
  private readonly selectAtCoordinateLayer: VectorLayer;

  constructor(
    private changeDetectorRef: ChangeDetectorRef,
    private netzbezugAuswahlModusService: NetzbezugAuswahlModusService,
    private olMapService: OlMapService,
    private bedienhinweisService: BedienhinweisService
  ) {
    super();

    this.selectAtCoordinateLayer = new VectorLayer({
      source: this.selectCoordinateSource,
      style: MapStyles.defaultPointStyleLarge(MapStyles.FEATURE_SELECT_COLOR),
      zIndex: puntuelleKantenBezuegeVectorLayerZIndex,
    });
    this.olMapService.addLayer(this.selectAtCoordinateLayer);

    this.subscriptions.push(
      this.olMapService.click$().subscribe(clickEvent => {
        if (this.selectionMode) {
          this.onMapClick(clickEvent);
          this.changeDetectorRef.markForCheck();
        }
      })
    );
  }

  get positionSelected(): boolean {
    return this.position !== null;
  }

  validate(): ValidationErrors | null {
    return !this.positionSelected ? { noSelection: 'Kein Ort ausgewählt' } : null;
  }

  public writeValue(position: Coordinate | null): void {
    this.position = position;
    this.onSelektionBeenden();
    if (!this.positionSelected) {
      this.onSelektionStarten();
    }
    this.changeDetectorRef.markForCheck();
  }

  public setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.changeDetectorRef.markForCheck();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.olMapService.removeLayer(this.selectAtCoordinateLayer);
    this.onSelektionBeenden();
  }

  ngOnInit(): void {
    if (this.withInfrastrukturIcon) {
      this.selectAtCoordinateLayer.setStyle([
        MapStyles.defaultPointStyleLarge(MapStyles.FEATURE_SELECT_COLOR),
        ...MapStyles.getInfrastrukturIconStyle(this.withInfrastrukturIcon, true),
      ]);
    }
  }

  onSelektionStarten(): void {
    this.selectionMode = true;
    this.bedienhinweisService.showBedienhinweis(PositionSelektionControlComponent.BEDIENHINWEIS);
    this.netzbezugAuswahlModusService.startNetzbezugAuswahl(false);
    this.olMapService.setCursor('point-selection-cursor');
  }

  onSelektionBeenden(): void {
    this.selectionMode = false;
    this.bedienhinweisService.hideBedienhinweis();
    this.netzbezugAuswahlModusService.stopNetzbezugAuswahl();
    this.olMapService.resetCursor();
  }

  private onMapClick(clickEvent: MapBrowserEvent<UIEvent>): void {
    this.position = clickEvent.coordinate;
    this.onChange(this.position);
  }
}
