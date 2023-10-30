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
import { Coordinate } from 'ol/coordinate';
import { AnpassungswunschAnlegenService } from 'src/app/shared/services/anpassungswunsch-anlegen.service';
import { Anpassungswunsch } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch';
import { AnpassungswunschKategorie } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-kategorie';
import { AnpassungswunschStatus } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-status';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { AnpassungswunschFilterService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-filter.service';
import { AnpassungswunschService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch.service';
import { SaveAnpassungswunschCommand } from 'src/app/viewer/anpassungswunsch/models/save-anpassungswunsch-command';

@Injectable()
export class AnpassungswunschAnlegenServiceImpl implements AnpassungswunschAnlegenService {
  constructor(
    private routingService: AnpassungenRoutingService,
    private anpassungswunschService: AnpassungswunschService,
    private filterService: AnpassungswunschFilterService
  ) {}

  public addAnpassungswunschFuerFehlerprotokoll(
    coordinates: Coordinate,
    beschreibung: string,
    fehlerprotokollId: string
  ): Promise<void> {
    const saveAnpassungswunschCommand: SaveAnpassungswunschCommand = {
      beschreibung: beschreibung.substring(0, Anpassungswunsch.BESCHREIBUNG_MAX_LENGTH),
      geometrie: { coordinates, type: 'Point' },
      kategorie: AnpassungswunschKategorie.RADVIS,
      status: AnpassungswunschStatus.OFFEN,
      fehlerprotokollId,
    };

    return this.anpassungswunschService
      .createAnpassungswunsch(saveAnpassungswunschCommand)
      .then(anp => anp.id)
      .then(newId => {
        this.filterService.refetchData();
        this.routingService.toInfrastrukturEditor(newId);
      });
  }
}
