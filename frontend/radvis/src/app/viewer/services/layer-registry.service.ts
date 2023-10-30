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

import { Injectable } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { Coordinate } from 'ol/coordinate';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { ABSTELLANLAGEN } from 'src/app/viewer/abstellanlage/models/abstellanlage.infrastruktur';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { BARRIEREN } from 'src/app/viewer/barriere/models/barriere.infrastruktur';
import { BarriereRoutingService } from 'src/app/viewer/barriere/services/barriere-routing.service';
import { RadvisKnotenLayerComponent } from 'src/app/viewer/components/radvis-netz-layer/radvis-knoten-layer/radvis-knoten-layer.component';
import { WeitereKartenebenenRoutingService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen-routing.service';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FahrradrouteRoutingService } from 'src/app/viewer/fahrradroute/services/fahrradroute-routing.service';
import { FURTEN_KREUZUNGEN } from 'src/app/viewer/furten-kreuzungen/models/furten-kreuzungen.infrastruktur';
import { FurtenKreuzungenRoutingService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-routing.service';
import { LEIHSTATIONEN } from 'src/app/viewer/leihstation/models/leihstation.infrastruktur';
import { LeihstationRoutingService } from 'src/app/viewer/leihstation/services/leihstation-routing.service';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { KantenHighlightLayerComponent } from 'src/app/viewer/netz-details/components/kanten-highlight-layer/kanten-highlight-layer.component';
import { KnotenHighlightLayerComponent } from 'src/app/viewer/netz-details/components/knoten-highlight-layer/knoten-highlight-layer.component';
import { NetzdetailRoutingService } from 'src/app/viewer/netz-details/services/netzdetail-routing.service';
import { SERVICESTATIONEN } from 'src/app/viewer/servicestation/models/servicestation.infrastruktur';
import { ServicestationRoutingService } from 'src/app/viewer/servicestation/services/servicestation-routing.service';
import { RadvisSignaturLayerComponent } from 'src/app/viewer/signatur/components/radvis-signatur-layer/radvis-signatur-layer.component';
import { AbstractInfrastrukturLayerComponent } from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { RADVIS_NETZ_LAYER_PREFIX } from 'src/app/viewer/viewer-shared/models/radvis-netz-layer-prefix';
import { AbstractInfrastrukturenRoutingService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-routing.service';
import { WEGWEISENDE_BESCHILDERUNG } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung.infrastruktur';
import { WegweisendeBeschilderungRoutingService } from 'src/app/viewer/wegweisende-beschilderung/services/wegweisende-beschilderung-routing.service';
import invariant from 'tiny-invariant';
import { FAHRRADZAEHLSTELLE } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle.infrastruktur';
import { FahrradzaehlstelleRoutingService } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle-routing.service';

interface LayerEintrag {
  anzeigeNameFn: (f: RadVisFeature) => string;
  routeTo: (f: RadVisFeature, location: Coordinate) => void;
  iconFileName: string;
  identityFn: (f: RadVisFeature) => string;
}

@Injectable({ providedIn: 'root' })
export class LayerRegistryService {
  private registry: Map<string, LayerEintrag>;

  constructor(
    massnahmenRoutingService: MassnahmenRoutingService,
    fahrradrouteRoutingService: FahrradrouteRoutingService,
    netzdetailRoutingService: NetzdetailRoutingService,
    anpassungswunschRoutingService: AnpassungenRoutingService,
    barriereRoutingService: BarriereRoutingService,
    wegweisendeBeschilderungRoutingService: WegweisendeBeschilderungRoutingService,
    furtenKreuzungenRoutingService: FurtenKreuzungenRoutingService,
    abstellanlagenRoutingService: AbstellanlageRoutingService,
    servicestationRoutingService: ServicestationRoutingService,
    leihstationRoutingService: LeihstationRoutingService,
    fahrradzaehlstelleRoutingService: FahrradzaehlstelleRoutingService,
    weitereKartenebenenRoutingService: WeitereKartenebenenRoutingService,
    weitereKartenebenenService: WeitereKartenebenenService,
    sanitizer: DomSanitizer,
    iconRegistry: MatIconRegistry
  ) {
    iconRegistry.addSvgIcon('icon-kante', sanitizer.bypassSecurityTrustResourceUrl('./assets/icon-kante.svg'));
    iconRegistry.addSvgIcon('icon-knoten', sanitizer.bypassSecurityTrustResourceUrl('./assets/icon-knoten.svg'));
    iconRegistry.addSvgIcon(
      'icon-externer-layer',
      sanitizer.bypassSecurityTrustResourceUrl('./assets/icon-externer-layer.svg')
    );

    const massnahmenLayerEintrag: LayerEintrag = this.defaultInfrastrukturEintrag(MASSNAHMEN, massnahmenRoutingService);

    const fahrradrouteLayerEintrag: LayerEintrag = this.defaultInfrastrukturEintrag(
      FAHRRADROUTE,
      fahrradrouteRoutingService,
      f => (f.id ? f.id : f.attribute.find(a => a.key === FeatureProperties.FAHRRADROUTE_ID_PROPERTY_NAME)?.value)
    );

    const anpassungswunschLayerEintrag = this.defaultInfrastrukturEintrag(
      ANPASSUNGSWUNSCH,
      anpassungswunschRoutingService,
      undefined,
      (): string => 'Anpassungswunsch'
    );

    const barriereLayerEintrag: LayerEintrag = this.defaultInfrastrukturEintrag(BARRIEREN, barriereRoutingService);

    const furtKreuzungLayerEintrag: LayerEintrag = this.defaultInfrastrukturEintrag(
      FURTEN_KREUZUNGEN,
      furtenKreuzungenRoutingService
    );

    const wegweisendeBeschilderungLayerEintrag: LayerEintrag = this.defaultInfrastrukturEintrag(
      WEGWEISENDE_BESCHILDERUNG,
      wegweisendeBeschilderungRoutingService
    );

    const abstellanlageLayerEintrag: LayerEintrag = this.defaultInfrastrukturEintrag(
      ABSTELLANLAGEN,
      abstellanlagenRoutingService
    );

    const servicestationLayerEintrag: LayerEintrag = this.defaultInfrastrukturEintrag(
      SERVICESTATIONEN,
      servicestationRoutingService
    );

    const leihstationLayerEintrag: LayerEintrag = this.defaultInfrastrukturEintrag(
      LEIHSTATIONEN,
      leihstationRoutingService
    );

    const fahrradzaehlstellenLayerEintrag: LayerEintrag = this.defaultInfrastrukturEintrag(
      FAHRRADZAEHLSTELLE,
      fahrradzaehlstelleRoutingService
    );

    const knotenLayerEintrag: LayerEintrag = {
      iconFileName: 'icon-knoten',
      anzeigeNameFn: (): string => 'Knoten',
      identityFn: (f: RadVisFeature): string => {
        invariant(f.id, 'Der Knoten muss eine ID haben.');
        return `${RadvisKnotenLayerComponent.LAYER_NAME}_${f.id}`;
      },
      routeTo: (f: RadVisFeature): void => {
        const idFromFeature = f.id;
        invariant(idFromFeature);

        netzdetailRoutingService.toKnotenDetails(idFromFeature);
      },
    };

    const kanteLayerEintrag: LayerEintrag = {
      iconFileName: 'icon-kante',
      anzeigeNameFn: (): string => 'Kante',
      identityFn: (f: RadVisFeature): string => {
        const selectedKanteId =
          Number(f.attribute.find(attribut => attribut.key === FeatureProperties.KANTE_ID_PROPERTY_NAME)?.value) ||
          f.id;
        invariant(selectedKanteId, 'Die selektierte Kante besitzt keine ID');

        const selectedSeitenbezug =
          f.attribute.find(attribut => attribut.key === FeatureProperties.SEITE_PROPERTY_NAME)?.value || '';
        return `${RADVIS_NETZ_LAYER_PREFIX}${selectedKanteId}${selectedSeitenbezug}`;
      },
      routeTo: (f: RadVisFeature, location?: Coordinate): void => {
        invariant(location, 'Die Klickposition muss mit übergeben werden.');

        const selectedKanteId =
          Number(f.attribute.find(attribut => attribut.key === FeatureProperties.KANTE_ID_PROPERTY_NAME)?.value) ||
          f.id;
        invariant(selectedKanteId, 'Die selektierte Kante besitzt keine ID');

        const selectedSeitenbezug = f.attribute.find(attribut => attribut.key === FeatureProperties.SEITE_PROPERTY_NAME)
          ?.value;
        return netzdetailRoutingService.toKanteDetails(selectedKanteId, location, selectedSeitenbezug);
      },
    };

    const weitereKartenebenenEintrag: LayerEintrag = {
      anzeigeNameFn: f => {
        const layerId = f.attribute.find(attr => attr.key === WeitereKartenebene.LAYER_ID_KEY)?.value;
        let layerName;
        if (layerId) {
          layerName = weitereKartenebenenService.weitereKartenebenen.find(e => e.id === layerId)?.name;
        }
        return layerName ?? 'Weitere Kartenebenen';
      },
      iconFileName: 'icon-externer-layer',
      identityFn: f => {
        return (
          f.attribute.find(attr => attr.key === WeitereKartenebene.LAYER_ID_KEY)?.value +
          '/' +
          (f.id ||
            f.attribute.find(attr => attr.key === WeitereKartenebene.EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME)?.value)
        );
      },
      routeTo: f => {
        weitereKartenebenenRoutingService.routeToFeature(f);
      },
    };

    this.registry = new Map<string, LayerEintrag>([
      // 5 Konkrete Layer
      [MASSNAHMEN.name, massnahmenLayerEintrag],
      [FAHRRADROUTE.name, fahrradrouteLayerEintrag],
      [ANPASSUNGSWUNSCH.name, anpassungswunschLayerEintrag],
      [BARRIEREN.name, barriereLayerEintrag],
      [WEGWEISENDE_BESCHILDERUNG.name, wegweisendeBeschilderungLayerEintrag],
      [FURTEN_KREUZUNGEN.name, furtKreuzungLayerEintrag],
      [ABSTELLANLAGEN.name, abstellanlageLayerEintrag],
      [SERVICESTATIONEN.name, servicestationLayerEintrag],
      [LEIHSTATIONEN.name, leihstationLayerEintrag],
      [FAHRRADZAEHLSTELLE.name, fahrradzaehlstellenLayerEintrag],
      [KantenHighlightLayerComponent.LAYER_ID, kanteLayerEintrag],
      [RadvisKnotenLayerComponent.LAYER_NAME, knotenLayerEintrag],
      [KnotenHighlightLayerComponent.LAYER_ID, knotenLayerEintrag],
      [WeitereKartenebene.LAYER_NAME, weitereKartenebenenEintrag],
      [
        // Kante Layer für jegliche Netzklassen und Signaturen
        RADVIS_NETZ_LAYER_PREFIX,
        kanteLayerEintrag,
      ],
    ]);
  }

  public getUniqueKey(f: RadVisFeature): string | null {
    return this.findLayerEintrag(f)?.identityFn(f) || null;
  }

  toEditor(f: RadVisFeature, location: Coordinate): void {
    this.findLayerEintrag(f)?.routeTo(f, location);
  }

  getName(f: RadVisFeature): string {
    return this.findLayerEintrag(f)?.anzeigeNameFn(f) || `Kein Name in LayerRegistry für ${f.layer}`;
  }

  getIcon(f: RadVisFeature): string {
    return this.findLayerEintrag(f)?.iconFileName || 'terminal';
  }

  private findLayerEintrag(f: RadVisFeature): LayerEintrag | null {
    // Für Features mit weder id noch KanteId oder fahrradrouteId noch externeWmsFeatureId in den Attributen können keine Routen erstellt werden
    if (
      !f.id &&
      !f.attribute.find(
        a =>
          a.key === WeitereKartenebene.EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME ||
          a.key === FeatureProperties.KANTE_ID_PROPERTY_NAME ||
          a.key === FeatureProperties.FAHRRADROUTE_ID_PROPERTY_NAME
      )?.value
    ) {
      return null;
    }

    const konkreterLayerEintrag: LayerEintrag | undefined = this.registry.get(f.layer);
    if (konkreterLayerEintrag) {
      return konkreterLayerEintrag;
    }

    const netzklassenOderSignaturLayerEintrag: false | LayerEintrag | undefined =
      (f.layer.startsWith(RADVIS_NETZ_LAYER_PREFIX) || f.layer.startsWith(RadvisSignaturLayerComponent.ID_PREFIX)) &&
      this.registry.get(RADVIS_NETZ_LAYER_PREFIX);

    if (netzklassenOderSignaturLayerEintrag) {
      return netzklassenOderSignaturLayerEintrag;
    }

    return null;
  }

  private defaultInfrastrukturEintrag(
    infrastruktur: Infrastruktur,
    routingService: AbstractInfrastrukturenRoutingService,
    idFromFeature: (f: RadVisFeature) => number = (f): number => Number(f.id),
    anzeigeNameFn?: (f: RadVisFeature) => string
  ): LayerEintrag {
    return {
      iconFileName: 'infrastrukturen-icon-' + infrastruktur.name,
      anzeigeNameFn: anzeigeNameFn
        ? anzeigeNameFn
        : (f: RadVisFeature): string => {
            const bezeichnungAttribut = f.attribute.find(
              a => a.key === AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME
            );
            return bezeichnungAttribut ? bezeichnungAttribut.value : infrastruktur.displayName;
          },
      identityFn: (f: RadVisFeature): string => {
        const id = idFromFeature(f);
        invariant(id, `Die Infrastruktur ${infrastruktur.name} muss eine ID haben.`);
        return `${infrastruktur.name}_${id}`;
      },
      routeTo: (f: RadVisFeature): void => {
        const id = idFromFeature(f);
        invariant(id);
        routingService.toInfrastrukturEditor(id);
      },
    };
  }
}
