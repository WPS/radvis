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

/* eslint-disable @typescript-eslint/dot-notation */

import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import Point from 'ol/geom/Point';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import {
  RadVisFeatureAttributes,
  toRadVisFeatureAttributesFromMap,
} from 'src/app/shared/models/rad-vis-feature-attributes';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { BarriereRoutingService } from 'src/app/viewer/barriere/services/barriere-routing.service';
import { RadvisKnotenLayerComponent } from 'src/app/viewer/components/radvis-netz-layer/radvis-knoten-layer/radvis-knoten-layer.component';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FahrradrouteRoutingService } from 'src/app/viewer/fahrradroute/services/fahrradroute-routing.service';
import { FurtenKreuzungenRoutingService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-routing.service';
import { LeihstationRoutingService } from 'src/app/viewer/leihstation/services/leihstation-routing.service';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { NetzdetailRoutingService } from 'src/app/viewer/netz-details/services/netzdetail-routing.service';
import { LayerRegistryService } from 'src/app/viewer/services/layer-registry.service';
import { ServicestationRoutingService } from 'src/app/viewer/servicestation/services/servicestation-routing.service';
import { RadvisSignaturLayerComponent } from 'src/app/viewer/signatur/components/radvis-signatur-layer/radvis-signatur-layer.component';
import { RADVIS_NETZ_LAYER_PREFIX } from 'src/app/viewer/viewer-shared/models/radvis-netz-layer-prefix';
import { WegweisendeBeschilderungRoutingService } from 'src/app/viewer/wegweisende-beschilderung/services/wegweisende-beschilderung-routing.service';
import { anything, capture, instance, mock, verify } from 'ts-mockito';
import { FahrradzaehlstelleRoutingService } from 'src/app/viewer/fahrradzaehlstelle/services/fahrradzaehlstelle-routing.service';
import { WeitereKartenebenenRoutingService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen-routing.service';

describe(LayerRegistryService.name, () => {
  let layerRegistryService: LayerRegistryService;

  let massnahmenRoutingService: MassnahmenRoutingService;
  let fahrradrouteRoutingService: FahrradrouteRoutingService;
  let netzdetailRoutingService: NetzdetailRoutingService;
  let anpassungswunschRoutingService: AnpassungenRoutingService;
  let barriereRoutingService: BarriereRoutingService;
  let furtenKreuzungenRoutingService: FurtenKreuzungenRoutingService;
  let wegweisendeBeschilderungRoutingService: WegweisendeBeschilderungRoutingService;
  let domSanitizerMock: DomSanitizer;
  let iconRegistry: MatIconRegistry;

  beforeEach(() => {
    massnahmenRoutingService = mock(MassnahmenRoutingService);
    fahrradrouteRoutingService = mock(FahrradrouteRoutingService);
    netzdetailRoutingService = mock(NetzdetailRoutingService);
    anpassungswunschRoutingService = mock(AnpassungenRoutingService);
    barriereRoutingService = mock(BarriereRoutingService);
    furtenKreuzungenRoutingService = mock(FurtenKreuzungenRoutingService);
    wegweisendeBeschilderungRoutingService = mock(WegweisendeBeschilderungRoutingService);

    domSanitizerMock = {
      bypassSecurityTrustResourceUrl: (val: string) => val,
    } as unknown as DomSanitizer;
    iconRegistry = mock(MatIconRegistry);

    layerRegistryService = new LayerRegistryService(
      instance(massnahmenRoutingService),
      instance(fahrradrouteRoutingService),
      instance(netzdetailRoutingService),
      instance(anpassungswunschRoutingService),
      instance(barriereRoutingService),
      instance(wegweisendeBeschilderungRoutingService),
      instance(furtenKreuzungenRoutingService),
      instance(mock(AbstellanlageRoutingService)),
      instance(mock(ServicestationRoutingService)),
      instance(mock(LeihstationRoutingService)),
      instance(mock(FahrradzaehlstelleRoutingService)),
      instance(mock(WeitereKartenebenenRoutingService)),
      instance(mock(WeitereKartenebenenService)),
      domSanitizerMock,
      instance(iconRegistry)
    );
  });

  describe('getUniqueKey', () => {
    it('should generate the correct unique key for Massnahme', () => {
      // arrange
      const radVisFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap(),
        MASSNAHMEN.name,
        new Point([1, 1])
      );

      // act
      const uniqueKey = layerRegistryService.getUniqueKey(radVisFeature);

      // assert
      expect(uniqueKey).toEqual(`${MASSNAHMEN.name}_13`);
    });

    it('should generate the correct unique key for Fahrradrouten', () => {
      // arrange
      const radVisFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap(),
        FAHRRADROUTE.name,
        new Point([1, 1])
      );

      // act
      const uniqueKey = layerRegistryService.getUniqueKey(radVisFeature);

      // assert
      expect(uniqueKey).toEqual(`${FAHRRADROUTE.name}_13`);
    });

    it('should generate the correct unique key for Fahrradrouten: id=null -> FeatureProperty', () => {
      // arrange
      const radVisFeature = new RadVisFeature(
        null,
        toRadVisFeatureAttributesFromMap([[FeatureProperties.FAHRRADROUTE_ID_PROPERTY_NAME, 13]]),
        FAHRRADROUTE.name,
        new Point([1, 1])
      );

      // act
      const uniqueKey = layerRegistryService.getUniqueKey(radVisFeature);

      // assert
      expect(uniqueKey).toEqual(`${FAHRRADROUTE.name}_13`);
    });

    it('should generate the correct unique key for Knoten', () => {
      // arrange
      const radVisFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap(),
        RadvisKnotenLayerComponent.LAYER_NAME,
        new Point([1, 1])
      );

      // act
      const uniqueKey = layerRegistryService.getUniqueKey(radVisFeature);

      // assert
      expect(uniqueKey).toEqual(`${RadvisKnotenLayerComponent.LAYER_NAME}_13`);
    });

    describe('should generate the correct unique key for Kanten', () => {
      it('Einseitige Kante ohne kanteIdProperty', () => {
        // arrange
        const radVisFeature = getRadVisFeatureForKanteWithAttributes(13);

        // act
        const uniqueKey = layerRegistryService.getUniqueKey(radVisFeature);

        // assert
        expect(uniqueKey).toEqual(`${RADVIS_NETZ_LAYER_PREFIX}13`);
      });

      it('Einseitige Kante', () => {
        // arrange
        const radVisFeature = getRadVisFeatureForKanteWithAttributes(13, '13');

        // act
        const uniqueKey = layerRegistryService.getUniqueKey(radVisFeature);

        // assert
        expect(uniqueKey).toEqual(`${RADVIS_NETZ_LAYER_PREFIX}13`);
      });

      it('Zweiseitige Kante', () => {
        // arrange
        const radVisFeature = getRadVisFeatureForKanteWithAttributes(13, '13', 'LINKS');

        // act
        const uniqueKey = layerRegistryService.getUniqueKey(radVisFeature);

        // assert
        expect(uniqueKey).toEqual(`${RADVIS_NETZ_LAYER_PREFIX}13LINKS`);
      });
    });
  });

  describe('toEditor', () => {
    it('should navigate to Massnahmen Editor', () => {
      // arrange
      const radVisFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap(),
        MASSNAHMEN.name,
        new Point([1, 1])
      );

      // act
      layerRegistryService.toEditor(radVisFeature, [2, 2]);

      //assert
      verify(massnahmenRoutingService.toInfrastrukturEditor(anything())).once();
      expect(capture(massnahmenRoutingService.toInfrastrukturEditor).last()[0]).toEqual(13);
    });

    it('should navigate to Fahrradrouten Detail View', () => {
      // arrange
      const radVisFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap(),
        FAHRRADROUTE.name,
        new Point([1, 1])
      );

      // act
      layerRegistryService.toEditor(radVisFeature, [2, 2]);

      //assert
      verify(fahrradrouteRoutingService.toInfrastrukturEditor(anything())).once();
      expect(capture(fahrradrouteRoutingService.toInfrastrukturEditor).last()[0]).toEqual(13);
    });

    it('should navigate to Knoten Detail View', () => {
      // arrange
      const radVisFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap(),
        RadvisKnotenLayerComponent.LAYER_NAME,
        new Point([1, 1])
      );

      // act
      layerRegistryService.toEditor(radVisFeature, [2, 2]);

      //assert
      verify(netzdetailRoutingService.toKnotenDetails(anything())).once();
      expect(capture(netzdetailRoutingService.toKnotenDetails).last()[0]).toEqual(13);
    });

    describe('should navigate to Kanten Detail View', () => {
      it('Einseite Kante', () => {
        // arrange
        const radVisFeature = getRadVisFeatureForKanteWithAttributes(13, '13');

        // act
        layerRegistryService.toEditor(radVisFeature, [2, 2]);

        //assert
        verify(netzdetailRoutingService.toKanteDetails(anything(), anything(), anything())).once();
        expect(capture(netzdetailRoutingService.toKanteDetails).last()[0]).toEqual(13);
        expect(capture(netzdetailRoutingService.toKanteDetails).last()[1]).toEqual([2, 2]);
        expect(capture(netzdetailRoutingService.toKanteDetails).last()[2]).toEqual(undefined);
      });

      it('Zweiseitige Kante', () => {
        // arrange
        const radVisFeature = getRadVisFeatureForKanteWithAttributes(null, '13', 'LINKS');

        // act
        layerRegistryService.toEditor(radVisFeature, [2, 2]);

        //assert
        verify(netzdetailRoutingService.toKanteDetails(anything(), anything(), anything())).once();
        expect(capture(netzdetailRoutingService.toKanteDetails).last()[0]).toEqual(13);
        expect(capture(netzdetailRoutingService.toKanteDetails).last()[1]).toEqual([2, 2]);
        expect(capture(netzdetailRoutingService.toKanteDetails).last()[2]).toEqual(KantenSeite.LINKS);
      });
    });
  });

  describe('getName', () => {
    it('should return correct Name for Knoten, Kante and Massnahme, Fahrradroute without any special bezeichnung', () => {
      const testParamsLayerIds = [
        MASSNAHMEN.name,
        FAHRRADROUTE.name,
        RADVIS_NETZ_LAYER_PREFIX,
        RadvisKnotenLayerComponent.LAYER_NAME,
      ];

      const expectedResults = [MASSNAHMEN.name, FAHRRADROUTE.name, 'Kante', 'Knoten'];

      testParamsLayerIds.forEach((layerId, index) => {
        // arrange
        const radVisFeature = new RadVisFeature(13, toRadVisFeatureAttributesFromMap(), layerId, new Point([1, 1]));

        // act
        const name = layerRegistryService.getName(radVisFeature);

        // assert
        expect(name).toEqual(expectedResults[index]);
      });
    });

    it('massnahme und fahrradroute mit bezeichnungs Attribut', () => {
      const testParamsFeatures = [
        new RadVisFeature(
          13,
          toRadVisFeatureAttributesFromMap([['bezeichnung', 'massnahmeMitDerBezeichnung123']]),
          MASSNAHMEN.name,
          new Point([1, 1])
        ),
        new RadVisFeature(
          13,
          toRadVisFeatureAttributesFromMap([['bezeichnung', 'fahrradrouteMitDerBezeichnung123']]),
          FAHRRADROUTE.name,
          new Point([1, 1])
        ),
      ];

      const expectedResults = ['massnahmeMitDerBezeichnung123', 'fahrradrouteMitDerBezeichnung123'];

      testParamsFeatures.forEach((radVisFeature, index) => {
        // act
        const name = layerRegistryService.getName(radVisFeature);

        // assert
        expect(name).toEqual(expectedResults[index]);
      });
    });
  });

  describe('Layer exists in registry', () => {
    it('should have Massnahmen', () => {
      const massnahmeFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap([['bezeichnung', 'massnahmeMitDerBezeichnung123']]),
        MASSNAHMEN.name,
        new Point([1, 1])
      );

      const eintrag = layerRegistryService['findLayerEintrag'](massnahmeFeature);

      expect(eintrag).toBeTruthy();
    });

    it('should have Massnahmen', () => {
      const fahrradrouteFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap([['bezeichnung', 'fahrradrouteMitDerBezeichnung123']]),
        FAHRRADROUTE.name,
        new Point([1, 1])
      );

      const eintrag = layerRegistryService['findLayerEintrag'](fahrradrouteFeature);

      expect(eintrag).toBeTruthy();
    });

    it('should have RadVIS Netz', () => {
      expect(layerRegistryService['findLayerEintrag'](getRadVisFeatureForKanteWithAttributes(13))).toBeTruthy();
      expect(layerRegistryService['findLayerEintrag'](getRadVisFeatureForKanteWithAttributes(13, '13'))).toBeTruthy();
      expect(
        layerRegistryService['findLayerEintrag'](getRadVisFeatureForKanteWithAttributes(13, '13', 'LINKS'))
      ).toBeTruthy();
    });

    it('should have RadVIS Knoten', () => {
      const knotenFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap(),
        RadvisKnotenLayerComponent.LAYER_NAME,
        new Point([1, 1])
      );
      expect(layerRegistryService['findLayerEintrag'](knotenFeature)).toBeTruthy();
    });

    it('should have Signaturen', () => {
      const signaturFeature = new RadVisFeature(
        13,
        toRadVisFeatureAttributesFromMap([['bezeichnung', 'signaturFeatureMitDerBezeichnung123']]),
        RadvisSignaturLayerComponent.ID_PREFIX,
        new Point([1, 1])
      );

      expect(layerRegistryService['findLayerEintrag'](signaturFeature)).toBeTruthy();
    });

    it('should not have highlighted Massnahmen', () => {
      // aktuell bedeutet ein highlighted massnahme ist die ohne ID
      const massnahmeHighlightedFeature = new RadVisFeature(
        null,
        toRadVisFeatureAttributesFromMap([['bezeichnung', 'massnahmeOhneId']]),
        MASSNAHMEN.name,
        new Point([1, 1])
      );

      const eintrag = layerRegistryService['findLayerEintrag'](massnahmeHighlightedFeature);

      expect(eintrag).toBeFalsy();
    });
  });
});

const getRadVisFeatureForKanteWithAttributes = (
  id: number | null,
  kanteId: string | undefined = undefined,
  kantenSeite: string | undefined = undefined
): RadVisFeature => {
  const attributes: RadVisFeatureAttributes = toRadVisFeatureAttributesFromMap();

  if (kanteId) {
    attributes.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, kanteId);
  }

  if (kantenSeite) {
    attributes.set(FeatureProperties.SEITE_PROPERTY_NAME, kantenSeite);
  }

  return new RadVisFeature(id, attributes, RADVIS_NETZ_LAYER_PREFIX, new Point([1, 1]));
};
