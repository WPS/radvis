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

import { MassnahmenImportZuordnungenService } from 'src/app/import/massnahmen/services/massnahmen-import-zuordnungen.service';
import { MassnahmenImportZuordnungUeberpruefung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung';
import { getDefaultZuordnung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung-test-data-provider.spec';
import invariant from 'tiny-invariant';

describe(MassnahmenImportZuordnungenService.name, () => {
  let service: MassnahmenImportZuordnungenService;

  beforeEach(() => {
    service = new MassnahmenImportZuordnungenService();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should receive all zuordnungen on update', () => {
    // Arrange
    const zuordnungen = [
      {
        ...getDefaultZuordnung(),
        id: 1,
      },
      {
        ...getDefaultZuordnung(),
        id: 2,
      },
      {
        ...getDefaultZuordnung(),
        id: 3,
      },
    ];
    let receivedZuordnungen: MassnahmenImportZuordnungUeberpruefung[] = [];
    service.zuordnungen$.subscribe(z => (receivedZuordnungen = z));

    // Act
    service.updateZuordnungen(zuordnungen);

    // Assert
    expect(receivedZuordnungen).not.toBeUndefined();
    expect(receivedZuordnungen).toEqual(zuordnungen);
  });

  it('should receive ID of zuordnung on selection', () => {
    // Arrange
    const zuordnung = {
      ...getDefaultZuordnung(),
      id: 123,
    };
    let receivedZuordnungsID: number | undefined = undefined;
    service.selektierteZuordnungsId$.subscribe(id => (receivedZuordnungsID = id));

    // Act
    service.selektiereZuordnung(zuordnung.id);

    // Assert
    expect(receivedZuordnungsID).not.toBeUndefined();
    expect(receivedZuordnungsID!).toEqual(zuordnung.id);
  });

  it('should receive undefined on deselect', () => {
    // Arrange
    let receivedZuordnungsID: number | undefined = 0;
    service.selektierteZuordnungsId$.subscribe(id => (receivedZuordnungsID = id));

    // Act
    service.deselektiereZuordnung();

    // Assert
    expect(receivedZuordnungsID).toBeUndefined();
  });

  it('should provide correct values for a selected Zuordnung', () => {
    // Arrange
    const selektierteZuordnungId = 1234;
    const zuordnung = {
      ...getDefaultZuordnung(),
      id: selektierteZuordnungId,
    };
    invariant(!!zuordnung.netzbezug);
    invariant(!!zuordnung.originalGeometrie);

    service.updateZuordnungen([zuordnung]);
    service.selektiereZuordnung(selektierteZuordnungId);

    //Act
    const selektierterZuordnungsNetzbezug = service.selektierterZuordnungsNetzbezug;
    const selektierteZuordnungsOriginalGeometrie = service.selektierteZuordnungsOriginalGeometrie;

    //Assert
    expect(selektierterZuordnungsNetzbezug).toBe(zuordnung.netzbezug);
    expect(selektierteZuordnungsOriginalGeometrie).toBe(zuordnung.originalGeometrie);
  });
});
