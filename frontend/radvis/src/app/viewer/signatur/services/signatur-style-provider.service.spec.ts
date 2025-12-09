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

import { Feature } from 'ol';
import { LineString } from 'ol/geom';
import { Style } from 'ol/style';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { SignaturStyleProviderService } from 'src/app/viewer/signatur/services/signatur-style-provider.service';
import {
  belagArtStyleXml,
  withPredicateStyleXml,
} from 'src/app/viewer/signatur/services/signatur-style-test-data-provider.spec';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';
import { instance, mock, when } from 'ts-mockito';

describe('SignaturStyleProvider', () => {
  let signaturStyleProvider: SignaturStyleProviderService;
  let signaturService: SignaturService;
  let signaturBelegArt: Signatur;
  let signaturMitPraedikat: Signatur;

  beforeEach(() => {
    signaturBelegArt = { name: 'BelegArt', typ: SignaturTyp.NETZ };
    signaturMitPraedikat = { name: 'MitPraedikat', typ: SignaturTyp.MASSNAHME };

    signaturService = mock(SignaturService);
    when(signaturService.getStylingForSignatur(signaturBelegArt)).thenReturn(Promise.resolve(belagArtStyleXml));
    when(signaturService.getStylingForSignatur(signaturMitPraedikat)).thenReturn(
      Promise.resolve(withPredicateStyleXml)
    );
    signaturStyleProvider = new SignaturStyleProviderService(instance(signaturService));
  });

  describe('url building', () => {
    it('should return correct url with simple belagArt xml', done => {
      signaturStyleProvider.getStyleInformation(signaturBelegArt).then(styleInformation => {
        expect(styleInformation.attributnamen).toEqual(['BelagArt']);
        done();
      });
    });

    it('should return correct url with enhanced MitPraedikat xml', done => {
      signaturStyleProvider.getStyleInformation(signaturMitPraedikat).then(styleInformation => {
        expect(styleInformation.attributnamen).toEqual(['Ortslage', 'Richtung', 'Wegeniveau']);
        done();
      });
    });
  });

  describe('styleFunctions', () => {
    it('should return correct styleFunction with simple belagArt xml', done => {
      signaturStyleProvider.getStyleInformation(signaturBelegArt).then(styleInformation => {
        const styleFunction = styleInformation.styleFunction;
        expect(styleFunction).toBeDefined();

        const feature = new Feature(new LineString([0, 1, 2, 3]));
        feature.setProperties({ BelagArt: 'Unbekannt' });
        const styleArray = styleFunction(feature, 0) as Style[];
        expect(styleArray[0].getStroke()?.getColor()).toEqual('#000000');
        done();
      });
    });

    it('should return correct styleFunction with enhanced MitPraedikat xml', done => {
      signaturStyleProvider.getStyleInformation(signaturMitPraedikat).then(styleInformation => {
        const styleFunction = styleInformation.styleFunction;
        expect(styleFunction).toBeDefined();

        const feature = new Feature(new LineString([0, 1, 2, 3]));
        feature.setProperties({ Ortslage: 'Außerorts', Richtung: 'In Stationierungsrichtung' });
        const styleArray = styleFunction(feature, 0) as Style[];
        expect(styleArray[0].getStroke()?.getColor()).toEqual('#FF0000');

        const feature2 = new Feature(new LineString([0, 1, 2, 3]));
        feature2.setProperties({ Ortslage: 'Innerorts', Richtung: 'Gegen Stationierungsrichtung' });
        const styleArray2 = styleFunction(feature2, 0) as Style[];
        expect(styleArray2[0].getStroke()?.getColor()).toEqual('#00FF00');

        const feature3 = new Feature(new LineString([0, 1, 2, 3]));
        // Diese Kombination ist in der SLD nicht definiert
        feature3.setProperties({ Ortslage: 'Innerorts', Richtung: 'In Stationierungsrichtung' });
        const styleArray3 = styleFunction(feature3, 0) as Style[];
        expect(styleArray3).toHaveSize(0);

        done();
      });
    });
  });

  describe('legende', () => {
    it('should return correct legende with simple belagArt xml', done => {
      signaturStyleProvider.getLegendeForSignatur(signaturBelegArt).then(legende => {
        expect(legende).toBeDefined();

        expect(legende.entries).toHaveSize(1);
        expect(legende.entries).toContain({ name: 'Unbekannt', color: '#000000', dash: undefined });
        done();
      });
    });

    it('should return correct legende with enhanced MitPraedikat xml', done => {
      signaturStyleProvider.getLegendeForSignatur(signaturMitPraedikat).then(legende => {
        expect(legende).toBeDefined();

        expect(legende.entries).toHaveSize(3);
        expect(legende.entries).toEqual([
          {
            name: 'Ortslage außerorts und in Stationierungsrichtung',
            color: '#FF0000',
            dash: undefined,
          },
          {
            name: 'Ortslage innerorts und gegen Stationierungsrichtung',
            color: '#00FF00',
            dash: undefined,
          },
          {
            name: 'Ortslage außerorts und gegen Stationierungsrichtung',
            color: '#00FF00',
            dash: undefined,
          },
        ]);
        done();
      });
    });
  });
});
