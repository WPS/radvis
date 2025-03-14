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

import { Absenkung } from 'src/app/editor/kanten/models/absenkung';
import { Benutzungspflicht } from 'src/app/editor/kanten/models/benutzungspflicht';
import { Beschilderung } from 'src/app/editor/kanten/models/beschilderung';
import { Bordstein } from 'src/app/editor/kanten/models/bordstein';
import { KfzParkenForm } from 'src/app/editor/kanten/models/kfz-parken-form';
import { KfzParkenTyp } from 'src/app/editor/kanten/models/kfz-parken-typ';
import { LinearReferenzierteAttribute } from 'src/app/editor/kanten/models/linear-referenzierte-attribute';
import { Oberflaechenbeschaffenheit } from 'src/app/editor/kanten/models/oberflaechenbeschaffenheit';
import { Schadenart } from 'src/app/editor/kanten/models/schadenart';
import { TrennstreifenForm } from 'src/app/editor/kanten/models/trennstreifen-form';
import { TrennstreifenTrennungZu } from 'src/app/editor/kanten/models/trennstreifen-trennung-zu';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';

export interface FuehrungsformAttribute extends LinearReferenzierteAttribute {
  belagArt: BelagArt;
  oberflaechenbeschaffenheit: Oberflaechenbeschaffenheit;
  bordstein: Bordstein;
  radverkehrsfuehrung: Radverkehrsfuehrung;
  benutzungspflicht: Benutzungspflicht;
  parkenForm: KfzParkenForm;
  parkenTyp: KfzParkenTyp;
  breite: number | null;
  beschilderung: Beschilderung;
  schaeden: Schadenart[];
  absenkung: Absenkung;
  trennstreifenBreiteRechts: number | null;
  trennstreifenBreiteLinks: number | null;
  trennstreifenTrennungZuRechts: TrennstreifenTrennungZu | null;
  trennstreifenTrennungZuLinks: TrennstreifenTrennungZu | null;
  trennstreifenFormRechts: TrennstreifenForm | null;
  trennstreifenFormLinks: TrennstreifenForm | null;
}
