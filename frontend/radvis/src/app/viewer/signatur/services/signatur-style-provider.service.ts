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
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturLegende } from 'src/app/shared/models/signatur-legende';
import { SignaturStyleInformation } from 'src/app/viewer/signatur/models/signatur-style-information';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';

declare let require: any;

@Injectable({
  providedIn: 'root',
})
export class SignaturStyleProviderService {
  public static SLDReader = require('@nieuwlandgeo/sldreader');

  constructor(private signaturService: SignaturService) {}

  private static getStyleFromSldText(sldText: string): any {
    const sldObject = SignaturStyleProviderService.SLDReader.Reader(sldText);
    const sldLayer = SignaturStyleProviderService.SLDReader.getLayer(sldObject);
    const styleNames: string[] = SignaturStyleProviderService.SLDReader.getStyleNames(sldLayer);

    return SignaturStyleProviderService.SLDReader.getStyle(sldLayer, styleNames[0]);
  }

  public getStyleInformation(selectedSignatur: Signatur): Promise<SignaturStyleInformation> {
    return this.signaturService.getStylingForSignatur(selectedSignatur).then(sldText => {
      const style = SignaturStyleProviderService.getStyleFromSldText(sldText);
      const featureTypeStyle = style.featuretypestyles[0];
      const styleFunction = SignaturStyleProviderService.SLDReader.createOlStyleFunction(featureTypeStyle);
      const attributnamen = this.getAttributnamenFromFeatureTypeStyle(featureTypeStyle);

      return { attributnamen, styleFunction } as SignaturStyleInformation;
    });
  }

  public getLegendeForSignatur(signatur: Signatur): Promise<SignaturLegende> {
    return this.signaturService.getStylingForSignatur(signatur).then(sldText => {
      const style = SignaturStyleProviderService.getStyleFromSldText(sldText);
      const featureTypeStyle = style.featuretypestyles[0];

      const entries = featureTypeStyle.rules.map((rule: any) => {
        if (!rule.name) {
          return null;
        }
        if (rule.linesymbolizer?.stroke?.styling.stroke) {
          return {
            name: rule.name,
            color: rule.linesymbolizer.stroke.styling.stroke,
            dash: rule.linesymbolizer.stroke.styling.strokeDasharray,
          };
        } else if (rule.linesymbolizer?.length) {
          const lastIndex = rule.linesymbolizer.length - 1;
          return {
            name: rule.name,
            color: rule.linesymbolizer[lastIndex].stroke.styling.stroke,
            dash: rule.linesymbolizer[lastIndex].stroke.styling.strokeDasharray,
          };
        } else if (rule.pointsymbolizer) {
          // wir ignorieren zunächst Punktgeometrien in den SLD-files
          return null;
        } else {
          return null;
        }
      });

      return { name: style.name, entries, typ: signatur.typ } as SignaturLegende;
    });
  }

  private getAttributnamenFromFeatureTypeStyle(featureTypeStyle: any): string[] {
    const distinkteAttributnamen = new Set();
    const rules: any[] = featureTypeStyle.rules;
    rules.forEach(rule => {
      if (!rule.filter) {
        return;
      }
      if (rule.filter.predicates) {
        const predicates: any[] = rule.filter.predicates;
        predicates.forEach(predicate => {
          distinkteAttributnamen.add(predicate.propertyname);
        });
      } else {
        distinkteAttributnamen.add(rule.filter.propertyname);
      }
    });
    return Array.from(distinkteAttributnamen) as string[];
  }
}
