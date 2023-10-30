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

import { ImportAttributeAbbildungBearbeitenComponent } from 'src/app/editor/manueller-import/components/import-attribute-abbildung-bearbeiten/import-attribute-abbildung-bearbeiten.component';
import { ImportNetzklasseAbbildungBearbeitenComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abbildung-bearbeiten/import-netzklasse-abbildung-bearbeiten.component';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';

describe('ManuellerImportRoutingService', () => {
  let service: ManuellerImportRoutingService;

  beforeEach(() => {
    service = new ManuellerImportRoutingService();
  });

  describe('getRouteForStep', () => {
    it('should return korrekt Schritt 0', () => {
      expect(service.getRouteForStep(0, ImportTyp.NETZKLASSE_ZUWEISEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_DATEI_UPLOAD_ROUTE
      );
      expect(service.getRouteForStep(0, ImportTyp.ATTRIBUTE_UEBERNEHMEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_DATEI_UPLOAD_ROUTE
      );
    });

    it('should return korrekt Schritt 1', () => {
      expect(service.getRouteForStep(1, ImportTyp.NETZKLASSE_ZUWEISEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_NETZKLASSE_PARAMETER_EINGEBEN_ROUTE
      );
      expect(service.getRouteForStep(1, ImportTyp.ATTRIBUTE_UEBERNEHMEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_ATTRIBUTE_PARAMETER_EINGEBEN_ROUTE
      );
    });

    it('should return korrekt Schritt 2', () => {
      expect(service.getRouteForStep(2, ImportTyp.NETZKLASSE_ZUWEISEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_AUTOMATISCHE_ABBILDUNG_ROUTE
      );
      expect(service.getRouteForStep(2, ImportTyp.ATTRIBUTE_UEBERNEHMEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_AUTOMATISCHE_ABBILDUNG_ROUTE
      );
    });

    it('should return korrekt Schritt 3', () => {
      expect(service.getRouteForStep(3, ImportTyp.NETZKLASSE_ZUWEISEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_NETZKLASSE_KORREKTUR_ROUTE
      );
      expect(service.getRouteForStep(3, ImportTyp.ATTRIBUTE_UEBERNEHMEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_ATTRIBUTE_KORREKTUR_ROUTE
      );
    });

    it('should return korrekt Schritt 4', () => {
      expect(service.getRouteForStep(4, ImportTyp.NETZKLASSE_ZUWEISEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_NETZKLASSE_ABSCHLUSS_ROUTE
      );
      expect(service.getRouteForStep(4, ImportTyp.ATTRIBUTE_UEBERNEHMEN)).toEqual(
        ManuellerImportRoutingService.IMPORT_ATTRIBUTE_ABSCHLUSS_ROUTE
      );
    });
  });

  describe('getChildRoutes', () => {
    it('should return two', () => {
      const attributeStepRoute = {
        component: ImportAttributeAbbildungBearbeitenComponent,
        link: 'testAttribute',
      };
      const netzklassenStepRoute = {
        component: ImportNetzklasseAbbildungBearbeitenComponent,
        link: 'test',
      };
      const result = ManuellerImportRoutingService.getChildRoutes([
        {
          abweichendeAttributImportRoute: attributeStepRoute,
          bezeichnung: 'Test',
          route: netzklassenStepRoute,
        },
      ]);
      expect(result.length).toBe(2);
      expect(result[0].path).toBe(netzklassenStepRoute.link);
      expect(result[0].data?.importTyp).toBe(ImportTyp.NETZKLASSE_ZUWEISEN);
      expect(result[0].component).toBe(netzklassenStepRoute.component);
      expect(result[1].path).toBe(attributeStepRoute.link);
      expect(result[1].data?.importTyp).toBe(ImportTyp.ATTRIBUTE_UEBERNEHMEN);
      expect(result[1].component).toBe(attributeStepRoute.component);
    });

    it('should return one', () => {
      const netzklassenStepRoute = {
        component: ImportNetzklasseAbbildungBearbeitenComponent,
        link: 'test',
      };
      const result = ManuellerImportRoutingService.getChildRoutes([
        {
          bezeichnung: 'Test',
          route: netzklassenStepRoute,
        },
      ]);
      expect(result.length).toBe(1);
      expect(result[0].path).toBe(netzklassenStepRoute.link);
      expect(result[0].data?.importTyp).toBeUndefined();
      expect(result[0].component).toBe(netzklassenStepRoute.component);
    });
  });
});
