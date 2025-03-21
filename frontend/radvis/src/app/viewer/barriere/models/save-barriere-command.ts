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

import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { BarriereFormDetails } from 'src/app/viewer/barriere/models/barriere-form-details';
import { Markierung } from 'src/app/viewer/barriere/models/markierung';
import { Sicherung } from 'src/app/viewer/barriere/models/sicherung';
import { VerbleibendeDurchfahrtsbreite } from 'src/app/viewer/barriere/models/verbleibende-durchfahrtsbreite';

export interface SaveBarriereCommand {
  netzbezug: Netzbezug;
  verantwortlicheOrganisation: number;
  version?: number;
  barrierenForm: string;
  verbleibendeDurchfahrtsbreite: VerbleibendeDurchfahrtsbreite | null;
  sicherung: Sicherung | null;
  markierung: Markierung | null;
  begruendung: string | null;
  barriereFormDetails: BarriereFormDetails | null;
}
