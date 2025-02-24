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
  EventEmitter,
  forwardRef,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR, UntypedFormControl, ValidationErrors, Validator } from '@angular/forms';
import { asString } from 'ol/color';
import { Coordinate } from 'ol/coordinate';
import Feature, { FeatureLike } from 'ol/Feature';
import { Geometry, LineString, Point } from 'ol/geom';
import GeometryType from 'ol/geom/GeometryType';
import { Draw, Modify } from 'ol/interaction';
import { DrawEvent } from 'ol/interaction/Draw';
import { ModifyEvent } from 'ol/interaction/Modify';
import { Layer } from 'ol/layer';
import VectorLayer from 'ol/layer/Vector';
import { Source } from 'ol/source';
import VectorSource from 'ol/source/Vector';
import { Circle, Stroke, Style } from 'ol/style';
import Fill from 'ol/style/Fill';
import Text from 'ol/style/Text';
import { Subscription } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { LineStringOperations } from 'src/app/shared/models/line-string-operations';
import { infrastrukturHighlightLayerZIndex } from 'src/app/shared/models/shared-layer-zindex-config';
import { BedienhinweisService } from 'src/app/shared/services/bedienhinweis.service';
import { NetzbezugAuswahlModusService } from 'src/app/shared/services/netzbezug-auswahl-modus.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { CustomRoutingProfile, DEFAULT_PROFILE_ID } from 'src/app/viewer/fahrradroute/models/custom-routing-profile';
import { FahrradrouteNetzbezug } from 'src/app/viewer/fahrradroute/models/fahrradroute.netzbezug';
import { ProfilEigenschaften } from 'src/app/viewer/fahrradroute/models/profil-eigenschaften';
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { RoutingProfileService } from 'src/app/viewer/fahrradroute/services/routing-profile.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-fahrradroute-netzbezug-control',
  templateUrl: './fahrradroute-netzbezug-control.component.html',
  styleUrls: ['./fahrradroute-netzbezug-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FahrradrouteNetzbezugControlComponent), multi: true },
    {
      provide: NG_VALIDATORS,
      useExisting: FahrradrouteNetzbezugControlComponent,
      multi: true,
    },
  ],
  standalone: false,
})
export class FahrradrouteNetzbezugControlComponent
  extends AbstractFormControl<FahrradrouteNetzbezug>
  implements Validator, OnDestroy, OnInit
{
  @Output()
  loading = new EventEmitter<boolean>();

  readonly DEFAULT_PROFILE_ID = DEFAULT_PROFILE_ID;

  validInputColor = asString(MapStyles.VALID_INPUT_COLOR);

  public loadingRoute = false;
  public createMode = true;

  public customProfileSelectionControl = new UntypedFormControl(DEFAULT_PROFILE_ID);
  public mitFahrtrichtungControl = new UntypedFormControl(true);

  customRoutingProfile: CustomRoutingProfile[] = [];

  public readonly BEDIENHINWEIS_STUETZPUNKTE =
    'Wegpunkt hinzufügen durch Klick auf Verlauf.\n' +
    'Wegpunkt anfassen zum Verschieben.\n' +
    'Wegpunkt löschen durch Strg+Klick.\n';

  private readonly INDEX_PROPERTY_NAME = 'index';
  private readonly TEXT_PROPERTY_NAME = 'text';
  private readonly START_END_PROPERTY_NAME = 'istStartEnd';

  private readonly BEDIENHINWEIS_START = 'Startpunkt wählen durch Klick auf Karte.';
  private readonly BEDIENHINWEIS_END = 'Endpunkt wählen durch Klick auf Karte.';

  private stuetzpunkte: Coordinate[] = [];
  private kanteIDs: number[] = [];
  private coordinates: Coordinate[] = [];
  private profilEigenschaften: ProfilEigenschaften[] = [];
  private stuetzpunktVectorSource: VectorSource = new VectorSource();
  private stuetzpunktVectorLayer: VectorLayer;
  private drawStartEndInteraction: Draw;
  private modifyStuetzpunktInteraction: Modify;

  private verlaufVectorSource: VectorSource = new VectorSource();
  private verlaufVectorLayer: VectorLayer;

  private subscriptions: Subscription[] = [];

  constructor(
    private olMapService: OlMapService,
    private changeDetectorRef: ChangeDetectorRef,
    private netzbezugAuswahlModusService: NetzbezugAuswahlModusService,
    private fahrradrouteService: FahrradrouteService,
    private bedienhinweisService: BedienhinweisService,
    private notifyUserService: NotifyUserService,
    private routingProfileService: RoutingProfileService
  ) {
    super();

    this.netzbezugAuswahlModusService.startNetzbezugAuswahl(false);

    this.stuetzpunktVectorLayer = new VectorLayer({
      source: this.stuetzpunktVectorSource,
      style: this.stuetzpunktStyleFunction,
      zIndex: infrastrukturHighlightLayerZIndex,
    });
    this.olMapService.addLayer(this.stuetzpunktVectorLayer);

    this.drawStartEndInteraction = new Draw({
      source: this.stuetzpunktVectorSource,
      type: GeometryType.POINT,
    });
    this.olMapService.addInteraction(this.drawStartEndInteraction);
    this.drawStartEndInteraction.on('drawend', this.onStartEndErstellt);

    this.modifyStuetzpunktInteraction = new Modify({
      source: this.stuetzpunktVectorSource,
    });
    this.olMapService.addInteraction(this.modifyStuetzpunktInteraction);
    this.modifyStuetzpunktInteraction.on('modifyend', this.onStuetzpunktVerschoben);

    this.verlaufVectorLayer = new VectorLayer({
      source: this.verlaufVectorSource,
      style: MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_SELECT_COLOR),
      zIndex: infrastrukturHighlightLayerZIndex,
    });
    this.olMapService.addLayer(this.verlaufVectorLayer);

    this.subscriptions.push(
      this.olMapService.click$().subscribe(event => {
        const stuetzpunktAtPixel = this.olMapService.getFeaturesAtPixel(event.pixel, this.stuetzpunktLayerFilter, 25);
        const verlaufAtPixel = this.olMapService.getFeaturesAtPixel(event.pixel, this.verlaufLayerFilter);
        if (stuetzpunktAtPixel && stuetzpunktAtPixel.length > 0) {
          // Stuetzpunkte ausser Start und Ziel loeschen
          const deleteModus =
            (event.originalEvent as PointerEvent).ctrlKey || (event.originalEvent as PointerEvent).metaKey;
          const indexOfFeature = stuetzpunktAtPixel[0].get(this.INDEX_PROPERTY_NAME);
          if (deleteModus && indexOfFeature > 0 && indexOfFeature < this.stuetzpunkte.length - 1) {
            this.deleteStuetzpunkt(stuetzpunktAtPixel[0] as Feature);
            this.updateVerlauf();
          }
        } else if (verlaufAtPixel && verlaufAtPixel.length > 0) {
          // bei Klick auf Verlauf Stützpunkt einfügen
          const verlaufLineString = this.verlaufVectorSource.getFeatures()[0].getGeometry() as LineString;
          const neuerStuetzpunkt = verlaufLineString.getClosestPoint(event.coordinate);
          this.insertStuetzpunkt(neuerStuetzpunkt, verlaufLineString);
          this.onChange({
            stuetzpunkte: this.stuetzpunkte,
            kantenIDs: this.kanteIDs,
            geometrie: { coordinates: this.coordinates, type: 'LineString' },
            profilEigenschaften: this.profilEigenschaften,
            customProfileId: this.customProfileSelectionControl.value,
          });
        }
      })
    );

    this.bedienhinweisService.showBedienhinweis(this.BEDIENHINWEIS_START);

    this.subscriptions.push(
      this.mitFahrtrichtungControl.valueChanges.subscribe(() => {
        if (this.stuetzpunkte.length >= 2) {
          this.updateVerlauf();
        }
      }),
      this.customProfileSelectionControl.valueChanges.pipe(distinctUntilChanged()).subscribe(value => {
        if (this.stuetzpunkte.length >= 2) {
          this.updateVerlauf();
        }
      })
    );
  }

  writeValue(fahrradrouteNetzbezug: FahrradrouteNetzbezug | null): void {
    if (!fahrradrouteNetzbezug) {
      this.stuetzpunkte = [];
      this.kanteIDs = [];
      this.coordinates = [];

      this.bedienhinweisService.showBedienhinweis(this.BEDIENHINWEIS_START);
      this.drawStartEndInteraction.setActive(true);
      this.olMapService.setCursor('point-selection-cursor');

      this.customProfileSelectionControl.setValue(DEFAULT_PROFILE_ID, { emitEvent: false });

      this.createMode = true;
    } else {
      this.coordinates = fahrradrouteNetzbezug.geometrie?.coordinates.slice() ?? [];
      this.kanteIDs = fahrradrouteNetzbezug.kantenIDs?.slice() ?? [];
      this.stuetzpunkte = fahrradrouteNetzbezug.stuetzpunkte?.slice() ?? [];

      this.bedienhinweisService.hideBedienhinweis();
      this.drawStartEndInteraction.setActive(false);
      this.olMapService.resetCursor();

      this.customProfileSelectionControl.setValue(fahrradrouteNetzbezug.customProfileId, { emitEvent: false });

      this.createMode = false;
    }
    this.drawVerlauf();
    this.drawStuetzpunkte();
    this.changeDetectorRef.markForCheck();
  }

  // eslint-disable-next-line no-unused-vars
  setDisabledState(isDisabled: boolean): void {}

  validate(): ValidationErrors | null {
    const errors: ValidationErrors = {};
    if (this.stuetzpunkte.length < 2) {
      errors.startUndEnd = 'Start- und Endpunkt muss gesetzt sein';
    } else if (this.kanteIDs.length === 0) {
      errors.noKanteIds = 'Keine Kanten zugeordnet';
    } else if (this.coordinates.length < 2) {
      errors.noLineString = 'Keine Routen gefunden';
    }

    if (Object.keys(errors).length === 0) {
      return null;
    }

    return errors;
  }

  get startPunkt(): Coordinate | null {
    return this.stuetzpunkte[0] ?? null;
  }

  get endPunkt(): Coordinate | null {
    if (this.stuetzpunkte.length < 2) {
      return null;
    } else {
      return this.stuetzpunkte[this.stuetzpunkte.length - 1] ?? null;
    }
  }

  ngOnDestroy(): void {
    this.bedienhinweisService.hideBedienhinweis();
    this.olMapService.resetCursor();
    this.olMapService.removeInteraction(this.drawStartEndInteraction);
    this.olMapService.removeInteraction(this.modifyStuetzpunktInteraction);
    this.olMapService.removeLayer(this.stuetzpunktVectorLayer);
    this.olMapService.removeLayer(this.verlaufVectorLayer);
    this.netzbezugAuswahlModusService.stopNetzbezugAuswahl();
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  ngOnInit(): void {
    this.subscriptions.push(
      this.routingProfileService.profiles$.subscribe(profiles => {
        this.customRoutingProfile = profiles;
        const profileId = this.customProfileSelectionControl.value;
        if (!profiles.map(p => p.id).includes(profileId) && profileId !== this.DEFAULT_PROFILE_ID) {
          this.customProfileSelectionControl.patchValue(this.DEFAULT_PROFILE_ID);
        }
        this.changeDetectorRef.detectChanges();
      })
    );
  }

  private onStartEndErstellt = (evt: DrawEvent): void => {
    const clickedCoordinates = (evt.feature.getGeometry() as Point).getCoordinates();
    this.stuetzpunkte.push(clickedCoordinates);
    evt.feature.set(this.TEXT_PROPERTY_NAME, this.stuetzpunkte.length === 1 ? 'A' : 'B');
    evt.feature.set(this.START_END_PROPERTY_NAME, true);
    evt.feature.set(this.INDEX_PROPERTY_NAME, this.stuetzpunkte.length - 1);
    if (this.stuetzpunkte.length === 1) {
      this.bedienhinweisService.showBedienhinweis(this.BEDIENHINWEIS_END);
    } else if (this.stuetzpunkte.length >= 2) {
      this.bedienhinweisService.showBedienhinweis(this.BEDIENHINWEIS_STUETZPUNKTE);
      this.olMapService.resetCursor();
      this.drawStartEndInteraction.setActive(false);
      this.updateVerlauf();
    }
    this.changeDetectorRef.markForCheck();
  };

  private onStuetzpunktVerschoben = (evt: ModifyEvent): void => {
    const bewegterStuetzpunkt = evt.features.getArray()[0];
    if (bewegterStuetzpunkt) {
      const indexOfBewegterStuetzpunkt = bewegterStuetzpunkt.get(this.INDEX_PROPERTY_NAME);
      this.stuetzpunkte[indexOfBewegterStuetzpunkt] = (bewegterStuetzpunkt.getGeometry() as Point).getCoordinates();
      this.updateVerlauf();
    }
  };

  private updateVerlauf(): void {
    invariant(this.stuetzpunkte.length >= 2, 'Es müssen mindestens zwei Stützpunkte vorhanden sein um zu routen.');
    this.loadingRoute = true;
    this.loading.next(true);
    this.fahrradrouteService
      .routeFahrradroutenVerlauf(
        this.stuetzpunkte,
        this.customProfileSelectionControl.value,
        this.mitFahrtrichtungControl.value
      )
      .then(routingResult => {
        if (routingResult) {
          this.kanteIDs = routingResult.kantenIDs;
          this.coordinates = routingResult.routenGeometrie.coordinates;
          this.profilEigenschaften = routingResult.profilEigenschaften;
          this.drawVerlauf();
        } else {
          this.notifyUserService.warn('Es konnte keine Route berechnet werden');
        }
        this.onChange({
          stuetzpunkte: this.stuetzpunkte,
          kantenIDs: this.kanteIDs,
          geometrie: { coordinates: this.coordinates, type: 'LineString' },
          profilEigenschaften: this.profilEigenschaften,
          customProfileId: this.customProfileSelectionControl.value,
        });
      })
      .catch(() => {
        this.notifyUserService.warn('Es konnte keine Route berechnet werden');
      })
      .finally(() => {
        this.loadingRoute = false;
        this.loading.next(false);
        this.changeDetectorRef.markForCheck();
      });
  }

  private insertStuetzpunkt(neuerStuetzpunkt: Coordinate, verlaufLineString: LineString): void {
    // Wir entfernen die z-Coordinate (falls vorhanden), da die Stuetzpunkte über
    // spatial4j deserialisiert werden und es hier einen Bug (in spatial4j) gibt, wenn
    // z-Coordinaten vorhanden sind. Außerdem ist die z-Coordinate hier irrelevant.
    // Für die 3D-Geometrie einer Route wird ein eigener Deserialierungsmechanismus verwendet.
    // Siehe onStuetzpunktVerschoben.
    const neuerStuetzpunkt2D = neuerStuetzpunkt.slice(0, 2);
    if (this.stuetzpunkte.length === 2) {
      invariant(this.startPunkt);
      invariant(this.endPunkt);
      this.stuetzpunkte = [this.startPunkt, neuerStuetzpunkt2D, this.endPunkt];
    } else {
      // den neuen Stuetzpunkt in das stuetzpunkt array einsortieren
      const neuerStuetzpunktAsLineareReferenz = LineStringOperations.getFractionOfPointOnLineString(
        neuerStuetzpunkt2D,
        verlaufLineString
      );

      // wir fangen ab dem ersten Element an, was kein Startpunkt ist und ignorieren den Endpunkt
      let stuetzpunktEingefuegt = false;
      for (let i = 1; i < this.stuetzpunkte.length; i++) {
        const ithStuetzpunktAsLineareReferenz = LineStringOperations.getFractionOfPointOnLineString(
          this.stuetzpunkte[i],
          verlaufLineString
        );
        if (neuerStuetzpunktAsLineareReferenz < ithStuetzpunktAsLineareReferenz) {
          // neuen Stuetzpunkt an der Stelle i einfuegen
          this.stuetzpunkte.splice(i, 0, neuerStuetzpunkt2D);
          stuetzpunktEingefuegt = true;
          break;
        }
      }
      if (!stuetzpunktEingefuegt) {
        this.stuetzpunkte.splice(this.stuetzpunkte.length - 2, 0, neuerStuetzpunkt2D);
      }
    }
    this.drawStuetzpunkte();
  }

  private deleteStuetzpunkt(feature: Feature): void {
    const indexOfFeature = feature.get(this.INDEX_PROPERTY_NAME);
    invariant(
      indexOfFeature > 0 && indexOfFeature < this.stuetzpunkte.length - 1,
      'Start- und Endpunkt können nicht entfernt werden.'
    );
    this.stuetzpunkte.splice(indexOfFeature, 1);
    this.drawStuetzpunkte();
  }

  private drawVerlauf(): void {
    this.verlaufVectorSource.clear();
    if (this.coordinates.length > 0) {
      this.verlaufVectorSource.addFeature(new Feature<Geometry>(new LineString(this.coordinates)));
    }
  }

  private drawStuetzpunkte(): void {
    this.stuetzpunktVectorSource.clear();
    this.stuetzpunktVectorSource.addFeatures(
      this.stuetzpunkte.map((coordinate, index) => {
        const feature = new Feature(new Point(coordinate));
        feature.set(this.INDEX_PROPERTY_NAME, index);
        if (index === 0) {
          feature.set(this.START_END_PROPERTY_NAME, true);
          feature.set(this.TEXT_PROPERTY_NAME, 'A');
        } else if (index === this.stuetzpunkte.length - 1) {
          feature.set(this.START_END_PROPERTY_NAME, true);
          feature.set(this.TEXT_PROPERTY_NAME, 'B');
        } else {
          feature.set(this.START_END_PROPERTY_NAME, false);
        }
        return feature;
      })
    );
  }

  private stuetzpunktStyleFunction: Style | Style[] | ((p0: FeatureLike, p1: number) => Style | Style[]) = (
    feature: FeatureLike
  ): Style => {
    if (feature.get(this.START_END_PROPERTY_NAME) === true) {
      return new Style({
        image: new Circle({
          radius: 8,
          fill: new Stroke({
            color: MapStyles.FEATURE_SELECT_COLOR,
            width: MapStyles.LINE_WIDTH_MEDIUM,
          }),
        }),
        text: new Text({
          text: feature.get(this.TEXT_PROPERTY_NAME),
          font: 'bold 20px Roboto',
          fill: new Fill({
            color: MapStyles.FEATURE_SELECT_COLOR,
          }),
          offsetY: -15,
        }),
      });
    } else {
      return new Style({
        image: new Circle({
          radius: 8,
          stroke: new Stroke({
            color: MapStyles.FEATURE_SELECT_COLOR,
            width: MapStyles.LINE_WIDTH_MEDIUM,
          }),
        }),
      });
    }
  };

  private stuetzpunktLayerFilter = (layer: Layer<Source>): boolean => {
    return layer === this.stuetzpunktVectorLayer;
  };

  private verlaufLayerFilter = (layer: Layer<Source>): boolean => {
    return layer === this.verlaufVectorLayer;
  };
}
