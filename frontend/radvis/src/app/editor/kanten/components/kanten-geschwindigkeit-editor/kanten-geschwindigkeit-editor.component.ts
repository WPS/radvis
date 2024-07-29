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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Feature } from 'ol';
import { LineString } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { StyleFunction } from 'ol/style/Style';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { AbstractLinearReferenzierteAttributGruppeOhneSeitenbezugEditor } from 'src/app/editor/kanten/components/abstract-linear-referenzierte-attribut-gruppe-ohne-seitenbezug-editor';
import { GeschwindigkeitAttribute } from 'src/app/editor/kanten/models/geschwindigkeit-attribute';
import { GeschwindigkeitsAttributGruppe } from 'src/app/editor/kanten/models/geschwindigkeits-attribut-gruppe';
import { Hoechstgeschwindigkeit } from 'src/app/editor/kanten/models/hoechstgeschwindigkeit';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenOrtslage } from 'src/app/editor/kanten/models/kanten-ortslage';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { SaveGeschwindigkeitAttributGruppeCommand } from 'src/app/editor/kanten/models/save-geschwindigkeit-attribut-gruppe-command';
import { SaveGeschwindigkeitAttributeCommand } from 'src/app/editor/kanten/models/save-geschwindigkeit-attribute-command';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-kanten-geschwindigkeit-editor',
  templateUrl: './kanten-geschwindigkeit-editor.component.html',
  styleUrls: [
    './kanten-geschwindigkeit-editor.component.scss',
    '../../../../form-elements/components/attribute-editor/attribut-editor.scss',
    '../abstract-attribut-gruppe-editor-mit-auswahl.scss',
    '../lineare-referenz-tabelle.scss',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KantenGeschwindigkeitEditorComponent
  extends AbstractLinearReferenzierteAttributGruppeOhneSeitenbezugEditor<
    GeschwindigkeitAttribute,
    GeschwindigkeitsAttributGruppe
  >
  implements DiscardableComponent, OnDestroy, OnInit
{
  public ortslageOptions = KantenOrtslage.options;
  public hoechstgeschwindigkeitOptions = Hoechstgeschwindigkeit.options;

  private stationierungsrichtungSource: VectorSource;
  private stationierungsrichtungLayer: VectorLayer;

  constructor(
    private netzService: NetzService,
    changeDetectorRef: ChangeDetectorRef,
    notifyUserService: NotifyUserService,
    kantenSelektionService: KantenSelektionService,
    benutzerDetailsService: BenutzerDetailsService,
    private olMapService: OlMapService
  ) {
    super(changeDetectorRef, notifyUserService, kantenSelektionService, benutzerDetailsService);

    this.stationierungsrichtungSource = new VectorSource();
    this.stationierungsrichtungLayer = new VectorLayer({
      source: this.stationierungsrichtungSource,
      style: this.arrowStyleFn,
    });
    this.olMapService.addLayer(this.stationierungsrichtungLayer);
  }

  ngOnInit(): void {
    super.subscribeToKantenSelektion();
    this.subscriptions.push(
      this.kantenSelektionService.selektierteKanten$.subscribe(kanten => {
        this.stationierungsrichtungSource.clear();
        this.stationierungsrichtungSource.addFeatures(
          kanten.map(k => new Feature(new LineString(k.geometry.coordinates)))
        );
        this.stationierungsrichtungSource.changed();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.olMapService.removeLayer(this.stationierungsrichtungLayer);
  }

  protected createDisplayedAttributeFormGroup(): UntypedFormGroup {
    return new UntypedFormGroup({
      ortslage: new UntypedFormControl(null),
      hoechstgeschwindigkeit: new UntypedFormControl(null),
      abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung: new UntypedFormControl(null),
    });
  }

  protected saveAttributgruppe(attributgruppen: GeschwindigkeitsAttributGruppe[]): Promise<Kante[]> {
    const commands = attributgruppen.map(attributgruppe => {
      const attributeCommand = attributgruppe.geschwindigkeitAttribute.map(attribute =>
        this.convertAttributeToAttributeCommand(attribute)
      );
      const associatedKantenSelektion = this.currentSelektion?.find(
        kantenSelektion => this.getAttributGruppeFrom(kantenSelektion.kante).id === attributgruppe.id
      ) as KantenSelektion;
      return {
        gruppenID: attributgruppe.id,
        gruppenVersion: attributgruppe.version,
        geschwindigkeitAttribute: attributeCommand,
        kanteId: associatedKantenSelektion.kante.id,
      } as SaveGeschwindigkeitAttributGruppeCommand;
    });
    return this.netzService.saveGeschwindigkeitsGruppe(commands);
  }

  protected getAttributeForSelektion(selektion: KantenSelektion[]): GeschwindigkeitAttribute[] {
    const result: GeschwindigkeitAttribute[] = [];
    selektion.forEach((kantenSelektion, kantenIndex) => {
      kantenSelektion.getSelectedSegmentIndices().forEach(selectedSegmentIndex => {
        result.push(this.currentAttributgruppen[kantenIndex].geschwindigkeitAttribute[selectedSegmentIndex]);
      });
    });
    return result;
  }

  protected getAttributeFromAttributGruppe(attributGruppe: GeschwindigkeitsAttributGruppe): GeschwindigkeitAttribute[] {
    return attributGruppe.geschwindigkeitAttribute;
  }

  protected getAttributGruppeFrom(kante: Kante): GeschwindigkeitsAttributGruppe {
    return kante.geschwindigkeitAttributGruppe;
  }

  protected updateCurrentAttributgruppenWithLineareReferenzen(
    newLineareReferenzenArrays: LinearReferenzierterAbschnitt[][]
  ): void {
    newLineareReferenzenArrays.forEach((lineareReferenzen, kantenIndex) => {
      lineareReferenzen.forEach((lineareReferenz, segmentIndex) => {
        this.currentAttributgruppen[kantenIndex].geschwindigkeitAttribute[segmentIndex].linearReferenzierterAbschnitt =
          newLineareReferenzenArrays[kantenIndex][segmentIndex];
      });
    });
  }

  protected updateCurrentAttributgruppenWithAttribute(changedAttributePartial: { [id: string]: any }): void {
    this.currentSelektion?.forEach(kantenSelektion => {
      const attributgruppeToChange = this.currentAttributgruppen.find(
        gruppe => gruppe.id === kantenSelektion.kante.geschwindigkeitAttributGruppe.id
      );
      invariant(attributgruppeToChange);
      kantenSelektion.getSelectedSegmentIndices().forEach(selectedSegmentIndex => {
        attributgruppeToChange.geschwindigkeitAttribute[selectedSegmentIndex] = {
          ...attributgruppeToChange.geschwindigkeitAttribute[selectedSegmentIndex],
          ...changedAttributePartial,
        };
      });
    });
  }

  protected resetLineareReferenzenFormArrays(newSelektion: KantenSelektion[]): void {
    const values = newSelektion.map(kantenSelektion => this.extractLineareReferenzenFromKante(kantenSelektion.kante));

    this.resetFormArray(this.lineareReferenzenFormArray, values);
  }

  private extractLineareReferenzenFromKante(kante: Kante): LinearReferenzierterAbschnitt[] {
    return kante.geschwindigkeitAttributGruppe.geschwindigkeitAttribute.map(
      attribute => attribute.linearReferenzierterAbschnitt
    );
  }

  private convertAttributeToAttributeCommand(attribute: GeschwindigkeitAttribute): SaveGeschwindigkeitAttributeCommand {
    return {
      ortslage: attribute.ortslage,
      hoechstgeschwindigkeit: attribute.hoechstgeschwindigkeit,
      abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung:
        attribute.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung,
      linearReferenzierterAbschnitt: attribute.linearReferenzierterAbschnitt,
    } as SaveGeschwindigkeitAttributeCommand;
  }

  // eslint-disable-next-line no-unused-vars
  private arrowStyleFn: StyleFunction = (f, r) => {
    const coordinates = (f.getGeometry() as LineString).getCoordinates();
    return MapStyles.createArrowBegleitend(coordinates, MapStyles.FEATURE_COLOR);
  };
}
