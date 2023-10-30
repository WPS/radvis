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

import { VerbleibendeDurchfahrtsbreite } from 'src/app/viewer/barriere/models/verbleibende-durchfahrtsbreite';
import { Sicherung } from 'src/app/viewer/barriere/models/sicherung';
import { Markierung } from 'src/app/viewer/barriere/models/markierung';
import { Barriere } from 'src/app/viewer/barriere/models/barriere';
import { defaultNetzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug-test-data-provider.spec';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { BARRIEREN_FORM } from 'src/app/viewer/barriere/models/barrieren-form';

export const defaultBarriere: Barriere = {
  netzbezug: defaultNetzbezug,
  verantwortlicheOrganisation: defaultOrganisation,
  version: 2,
  barrierenForm: BARRIEREN_FORM.ANDERE_BARRIEREN.options[0].name,
  verbleibendeDurchfahrtsbreite: VerbleibendeDurchfahrtsbreite.GROESSER_250CM,
  sicherung: Sicherung.BAULICHE_SICHERUNG,
  markierung: Markierung.ROTWEISS_RETROREFLEKTIERENDE_MARKIERUNG,
  begruendung: 'Begruendungstext',
  darfBenutzerBearbeiten: null,
};

export const otherBarriere: Barriere = {
  netzbezug: defaultNetzbezug,
  verantwortlicheOrganisation: { ...defaultOrganisation, id: 3456 },
  version: 2,
  barrierenForm: BARRIEREN_FORM.ANDERE_BARRIEREN.options[1].name,
  verbleibendeDurchfahrtsbreite: VerbleibendeDurchfahrtsbreite.KEINE_DURCHFAHRT_MOEGLICH,
  sicherung: Sicherung.BODENMARKIERUNG,
  markierung: Markierung.UNMARKIERTE_ABSPERRANLAGE,
  begruendung: 'Ein anderer Begruendungstext',
  darfBenutzerBearbeiten: null,
};
