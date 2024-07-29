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

import { Anpassungswunsch } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch';
import { AnpassungswunschKategorie } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-kategorie';
import { AnpassungswunschStatus } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-status';
import { SaveAnpassungswunschCommand } from 'src/app/viewer/anpassungswunsch/models/save-anpassungswunsch-command';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { AnpassungswunschFilterService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-filter.service';
import { AnpassungswunschService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch.service';
import { anything, capture, instance, mock, when } from 'ts-mockito';
import { AnpassungswunschAnlegenServiceImpl } from './anpassungswunsch-anlegen-impl.service';

describe(AnpassungswunschAnlegenServiceImpl.name, () => {
  let service: AnpassungswunschAnlegenServiceImpl;
  let routingService: AnpassungenRoutingService;
  let anpassungswunschService: AnpassungswunschService;
  let filterService: AnpassungswunschFilterService;

  beforeEach(() => {
    routingService = mock(AnpassungenRoutingService);
    anpassungswunschService = mock(AnpassungswunschService);
    filterService = mock(AnpassungswunschFilterService);

    service = new AnpassungswunschAnlegenServiceImpl(
      instance(routingService),
      instance(anpassungswunschService),
      instance(filterService)
    );
  });

  it('should create correct command', () => {
    when(anpassungswunschService.createAnpassungswunsch(anything())).thenResolve({
      id: 1,
    } as unknown as Anpassungswunsch);

    service.addAnpassungswunschFuerFehlerprotokoll([0, 10], 'Test Beschreibung', 'konsistenzregel/1');

    const expectedCommand: SaveAnpassungswunschCommand = {
      beschreibung: 'Test Beschreibung',
      geometrie: { coordinates: [0, 10], type: 'Point' },
      kategorie: AnpassungswunschKategorie.RADVIS,
      status: AnpassungswunschStatus.OFFEN,
      fehlerprotokollId: 'konsistenzregel/1',
    };
    expect(capture(anpassungswunschService.createAnpassungswunsch).last()[0]).toEqual(expectedCommand);
  });

  it('should handle too long beschreibung', () => {
    when(anpassungswunschService.createAnpassungswunsch(anything())).thenResolve({
      id: 1,
    } as unknown as Anpassungswunsch);

    service.addAnpassungswunschFuerFehlerprotokoll([0, 10], text1000Zeichen, 'konsistenzregel/1');

    const expectedCommand: SaveAnpassungswunschCommand = {
      beschreibung: text1000Zeichen.substring(0, 1000),
      geometrie: { coordinates: [0, 10], type: 'Point' },
      kategorie: AnpassungswunschKategorie.RADVIS,
      status: AnpassungswunschStatus.OFFEN,
      fehlerprotokollId: 'konsistenzregel/1',
    };
    expect(capture(anpassungswunschService.createAnpassungswunsch).last()[0]).toEqual(expectedCommand);
  });

  const text1000Zeichen =
    'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu fe';
});
