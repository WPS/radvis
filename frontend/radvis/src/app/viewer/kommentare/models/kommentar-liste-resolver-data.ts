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

import { Kommentar } from 'src/app/viewer/kommentare/models/kommentar';

// Wir haben ein eigenes Objekt samt massnahmeId, damit das data-Observable triggert, wenn zwei mal eine leere Liste
// rein kommt (Wechsel zwischen Maßnahmen mit jeweils einer leeren Liste). Angular macht da "schlaue" vergleiche der
// Daten und wir möchten, dass IMMER das observable feuert.
export interface KommentarListeResolverData {
  massnahmeId: number;
  liste: Kommentar[];
}
