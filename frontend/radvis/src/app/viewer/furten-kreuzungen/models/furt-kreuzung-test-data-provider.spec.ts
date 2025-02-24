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
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { KNOTENFORMEN } from 'src/app/shared/models/knotenformen';
import { defaultNetzbezug } from 'src/app/shared/models/netzbezug-test-data-provider.spec';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { FurtKreuzung } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung';
import { FurtKreuzungTyp } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-typ';
import { GruenAnforderung } from 'src/app/viewer/furten-kreuzungen/models/gruen-anforderung';
import { Linksabbieger } from 'src/app/viewer/furten-kreuzungen/models/linksabbieger';
import { Rechtsabbieger } from 'src/app/viewer/furten-kreuzungen/models/rechtsabbieger';
export const defaultMusterloesungOption: EnumOption = { name: 'TEST', displayText: 'Testmusterlösung' };

export const defaultFurtKreuzung: FurtKreuzung = {
  netzbezug: defaultNetzbezug,
  verantwortlicheOrganisation: defaultOrganisation,
  version: 2,
  kommentar: 'test Kommentar',
  knotenForm: KNOTENFORMEN.KNOTEN_MIT_LSA.options[0].name,
  radnetzKonform: true,
  typ: FurtKreuzungTyp.FURT,
  furtKreuzungMusterloesung: defaultMusterloesungOption.name,
  benutzerDarfBearbeiten: true,
  lichtsignalAnlageEigenschaften: {
    linksabbieger: Linksabbieger.EIGENES_SIGNALISIEREN,
    rechtsabbieger: Rechtsabbieger.RECHTSABBIEGER,
    gruenAnforderung: GruenAnforderung.AUTOMATISCH,
    radAufstellflaeche: false,
    getrenntePhasen: true,
    fahrradSignal: true,
    gruenVorlauf: false,
    vorgezogeneHalteLinie: true,
    umlaufzeit: null,
  },
  bauwerksmangel: null,
  bauwerksmangelArt: null,
  querungshilfeDetails: null,
};
