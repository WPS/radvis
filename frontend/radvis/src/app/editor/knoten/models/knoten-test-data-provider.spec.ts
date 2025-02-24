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

import { KantenOrtslage } from 'src/app/editor/kanten/models/kanten-ortslage';
import { Knoten } from 'src/app/editor/knoten/models/knoten';
import {
  defaultOrganisation,
  defaultUebergeordneteOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { QuellSystem } from 'src/app/shared/models/quell-system';

export const defaultKnoten: Knoten = {
  id: 1,
  geometry: {
    coordinates: [0, 1],
    type: 'Point',
  },
  ortslage: KantenOrtslage.AUSSERORTS,
  gemeinde: defaultOrganisation,
  landkreis: defaultUebergeordneteOrganisation,
  kommentar: 'kommentar',
  zustandsbeschreibung: 'default Zustandsbeschreibung',
  knotenForm: 'RECHTS_VOR_LINKS_REGELUNG',
  knotenVersion: 1,
  quelle: QuellSystem.DLM,
  liegtInZustaendigkeitsbereich: true,
  querungshilfeDetails: null,
  bauwerksmangel: null,
  bauwerksmangelArt: null,
};
