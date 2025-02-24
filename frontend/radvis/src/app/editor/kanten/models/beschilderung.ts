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
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';

export enum Beschilderung {
  UNBEKANNT = 'UNBEKANNT',
  GEHWEG_OHNE_VZ_239 = 'GEHWEG_OHNE_VZ_239',
  GEHWEG_MIT_VZ_239 = 'GEHWEG_MIT_VZ_239',
  VZ_254_ANGEORDNET = 'VZ_254_ANGEORDNET',
  VZ_1012_32_ANGEORDNET = 'VZ_1012_32_ANGEORDNET',
  KEIN_WEG_VORHANDEN = 'KEIN_WEG_VORHANDEN',
  PRIVATWEG = 'PRIVATWEG',
  ABGESPERRT = 'ABGESPERRT',
  ZUSATZZEICHEN_VORHANDEN = 'ZUSATZZEICHEN_VORHANDEN',
  ZUSATZZEICHEN_NICHT_VORHANDEN = 'ZUSATZZEICHEN_NICHT_VORHANDEN',
  HAUPTZEICHEN_KFZ_VERBOTEN = 'HAUPTZEICHEN_KFZ_VERBOTEN',
  NICHT_VORHANDEN = 'NICHT_VORHANDEN',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Beschilderung {
  export const options: EnumOption[] = Object.keys(Beschilderung).map((k: string): EnumOption => {
    switch (k) {
      case Beschilderung.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
      case Beschilderung.GEHWEG_OHNE_VZ_239:
        return { name: k, displayText: 'StVO-Beschilderung: Gehweg ohne VZ 239' };
      case Beschilderung.GEHWEG_MIT_VZ_239:
        return { name: k, displayText: 'StVO-Beschilderung: Gehweg mit VZ 239' };
      case Beschilderung.VZ_254_ANGEORDNET:
        return { name: k, displayText: 'StVO-Beschilderung: VZ 254 (Radfahren verboten) angeordnet' };
      case Beschilderung.VZ_1012_32_ANGEORDNET:
        return { name: k, displayText: 'StVO-Beschilderung: VZ 1012-32 (Radfahrer absteigen) angeordnet' };
      case Beschilderung.KEIN_WEG_VORHANDEN:
        return { name: k, displayText: 'Kein Weg vorhanden (physische Netzlücke)' };
      case Beschilderung.PRIVATWEG:
        return { name: k, displayText: 'Privatweg' };
      case Beschilderung.ABGESPERRT:
        return { name: k, displayText: 'Abgesperrter Weg' };
      case Beschilderung.ZUSATZZEICHEN_VORHANDEN:
        return { name: k, displayText: 'Zusatzzeichen vorhanden' };
      case Beschilderung.ZUSATZZEICHEN_NICHT_VORHANDEN:
        return { name: k, displayText: 'Zusatzzeichen nicht vorhanden' };
      case Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN:
        return { name: k, displayText: 'Hauptzeichen nur Kraftfahrzeuge verboten (kein Zusatzzeichen nötig)' };
      case Beschilderung.NICHT_VORHANDEN:
        return { name: k, displayText: 'keinerlei StVO-Beschilderung vorhanden' };
    }
    throw new Error('Beschreibung für enum Beschilderung fehlt: ' + k);
  });

  export const isValidFuerRadverkehrsfuehrung = (
    beschilderung: Beschilderung | string,
    radverkehrsfuehrung: Radverkehrsfuehrung
  ): boolean => {
    switch (beschilderung) {
      case Beschilderung.ZUSATZZEICHEN_VORHANDEN:
      case Beschilderung.ZUSATZZEICHEN_NICHT_VORHANDEN:
      case Beschilderung.HAUPTZEICHEN_KFZ_VERBOTEN:
      case Beschilderung.NICHT_VORHANDEN:
        return (
          radverkehrsfuehrung === Radverkehrsfuehrung.BETRIEBSWEG_FORST ||
          radverkehrsfuehrung === Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG ||
          radverkehrsfuehrung === Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT ||
          radverkehrsfuehrung === Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG ||
          radverkehrsfuehrung === Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND
        );
      default:
        return true;
    }
  };
}
