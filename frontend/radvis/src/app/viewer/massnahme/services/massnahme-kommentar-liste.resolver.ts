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

import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { KommentarListeResolverData } from 'src/app/viewer/kommentare/models/kommentar-liste-resolver-data';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import invariant from 'tiny-invariant';

export const massnahmeKommentarListeResolver: ResolveFn<KommentarListeResolverData> = (route, state) => {
  const massnahmeService: MassnahmeService = inject(MassnahmeService);
  const id = route.parent?.paramMap.get('id');
  invariant(id, 'Massnahme-ID muss als Parameter id an der Route gesetzt sein.');
  return massnahmeService.getKommentarListe(+id).then(kommentarListe => {
    // Wir bauen hier ein eigenes Objekt samt massnahmeId, damit das data-Observable triggert, wenn zwei mal eine
    // leere Liste rein kommt (Wechsel zwischen Maßnahmen mit jeweils einer leeren Liste). Angular macht da
    // "schlaue" vergleiche der Daten und wir möchten, dass IMMER das observable feuert.
    return { massnahmeId: +id, liste: kommentarListe };
  });
};
