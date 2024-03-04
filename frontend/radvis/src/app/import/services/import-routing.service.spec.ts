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
import { ImportRoutingService } from 'src/app/import/services/import-routing.service';

describe(ImportRoutingService.name, () => {
  // eslint-disable-next-line no-unused-vars
  let service: ImportRoutingService;

  beforeEach(() => {
    service = new ImportRoutingService();
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  // TODO migrate Test?
  // describe(ImportRoutingService.prototype.getRouteForImportTyp.name, () => {
  //   const attributeComponentTestRoute = 'attribute-component-test-route';
  //   const netzklassenComponentTestRoute = 'netzklassen-component-test-route';
  //   const massnahmenComponentTestRoute = 'massnahmen-component-test-route';
  //   beforeEach(() => {
  //     when(attributeRoutingService.getRouteForStatus(ImportSessionStatus.SESSION_CREATED, undefined)).thenReturn(
  //       attributeComponentTestRoute
  //     );
  //     when(netzklassenRoutingService.getRouteForStatus(ImportSessionStatus.SESSION_CREATED, undefined)).thenReturn(
  //       netzklassenComponentTestRoute
  //     );
  //     when(massnahmenImportRoutingService.getRouteForStatus(ImportSessionStatus.SESSION_CREATED, undefined)).thenReturn(
  //       massnahmenComponentTestRoute
  //     );
  //   });
  //
  //   [
  //     {
  //       serviceName: AttributeRoutingService.name,
  //       typ: ImportTyp.ATTRIBUTE_UEBERNEHMEN,
  //       route: ImportRoutes.ATTRIBUTE_IMPORT_ROUTE,
  //       componentRoute: attributeComponentTestRoute,
  //     },
  //     {
  //       serviceName: NetzklassenRoutingService.name,
  //       typ: ImportTyp.NETZKLASSE_ZUWEISEN,
  //       route: ImportRoutes.NETZKLASSEN_IMPORT_ROUTE,
  //       componentRoute: netzklassenComponentTestRoute,
  //     },
  //     {
  //       serviceName: MassnahmenImportRoutingService.name,
  //       typ: ImportTyp.MASSNAHMEN_IMPORTIEREN,
  //       route: ImportRoutes.MASSNAHMEN_IMPORT_ROUTE,
  //       componentRoute: massnahmenComponentTestRoute,
  //     },
  //   ].forEach(({ serviceName, typ, route, componentRoute }) => {
  //     it(`should retrieve route from ${serviceName} on importType ${typ}`, () => {
  //       expect(service.getRouteForImportTyp(typ, ImportSessionStatus.SESSION_CREATED)).toEqual([
  //         ImportRoutes.IMPORT_ROUTE,
  //         route,
  //         componentRoute,
  //       ]);
  //     });
  //   });
  // });
});
