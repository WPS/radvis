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

import { SaveWeitereKartenebeneCommand } from 'src/app/viewer/weitere-kartenebenen/models/save-weitere-kartenebene-command';
import { WeitereKartenebeneTyp } from 'src/app/viewer/weitere-kartenebenen/models/weitereKartenebeneTyp';
import { predefinedWeitereKartenebenenBaseZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';

export class PredefinedWeitereKartenebenen {
  public static allgemein: SaveWeitereKartenebeneCommand[] = [
    {
      name: 'Bevölkerungszahlen',
      url: 'https://www.wms.nrw.de/wms/zensusatlas?LAYERS=bevoelkerungszahl',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      farbe: undefined,
      deckkraft: 0.7,
      zoomstufe: 8.7,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 1,
      id: null,
      quellangabe: 'https://www.wms.nrw.de/wms/zensusatlas?REQUEST=GetCapabilities',
      dateiLayerId: null,
    },
    {
      name: 'Haltestellen des ÖPNVs',
      url: 'https://www.radroutenplaner-bw.de/api/geoserver/wfs?typeNames=rrpbw-poi:haltestellen',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
      farbe: '#ba2f2f',
      deckkraft: 1.0,
      zoomstufe: 13.5,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 2,
      id: null,
      quellangabe:
        'https://www.nvbw.de/open-data/haltestellen\n\nDieses Werk ist lizensiert unter der Datenlizenz Deutschland - Namensnennung - Version 2.0 (www.govdata.de/dl-de/by-2-0)',
      dateiLayerId: null,
    },
  ];
  public static ttSib: SaveWeitereKartenebeneCommand[] = [
    {
      name: 'TT-SIB Mittelstreifen mit Straßenklassen',
      url: window.location.origin + '/api/geoserver/saml/radvis/wms?layers=radvis%3Att_sib_mittelstreifen',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      farbe: undefined,
      deckkraft: 1.0,
      zoomstufe: 8.7,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 3,
      id: null,
      quellangabe: window.location.origin + '/api/geoserver/saml/radvis/wms?REQUEST=GetCapabilities',
      dateiLayerId: null,
    },
    {
      name: 'TT-SIB Fahrradwege',
      url: window.location.origin + '/api/geoserver/saml/radvis/wms?layers=radvis%3Att_sib_fahrradwege',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      farbe: undefined,
      deckkraft: 1.0,
      zoomstufe: 8.7,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 4,
      id: null,
      quellangabe: window.location.origin + '/api/geoserver/saml/radvis/wms?REQUEST=GetCapabilities',
      dateiLayerId: null,
    },
  ];
  public static unfallzahlen: SaveWeitereKartenebeneCommand[] = [
    {
      name: 'Unfallhäufigkeit pro Jahr mit Fahrradbeteiligung (naher Zoom)',
      url: 'https://www.wms.nrw.de/wms/unfallatlas?LAYERS=Beteiligung_Fahrrad_250',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      farbe: undefined,
      deckkraft: 1.0,
      zoomstufe: 8.7,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 5,
      id: null,
      quellangabe:
        'www.wms.nrw.de/wms/unfallatlas\n\nDieses Werk ist lizensiert unter der Datenlizenz Deutschland - Namensnennung - Version 2.0 (www.govdata.de/dl-de/by-2-0)',
      dateiLayerId: null,
    },
    {
      name: 'Unfallhäufigkeit pro Jahr mit Fahrradbeteiligung (ferner Zoom)',
      url: 'https://www.wms.nrw.de/wms/unfallatlas?LAYERS=Beteiligung_Fahrrad_5000',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      farbe: undefined,
      deckkraft: 1.0,
      zoomstufe: 8.7,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 6,
      id: null,
      quellangabe:
        'www.wms.nrw.de/wms/unfallatlas\n\nDieses Werk ist lizensiert unter der Datenlizenz Deutschland - Namensnennung - Version 2.0 (www.govdata.de/dl-de/by-2-0)',
      dateiLayerId: null,
    },
  ];
  public static verwaltungsgrenzen: SaveWeitereKartenebeneCommand[] = [
    {
      name: 'Verwaltungsgrenzen Regierungsbezirke',
      url:
        'https://owsproxy.lgl-bw.de/owsproxy/ows/WFS_LGL-BW_ATKIS_BasisDLM_VerwGr?SERVICE=WFS&version=1.1.0&request=GetFeature&typename=nora:v_at_regierungsbezirk',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
      farbe: '#03574b',
      deckkraft: 1.0,
      zoomstufe: 7,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 7,
      id: null,
      quellangabe:
        'owsproxy.lgl-bw.de/owsproxy/ows/WFS_LGL-BW_ATKIS_BasisDLM_VerwGr\n\nLizenz: Datenlizenz Deutschland - Namensnennung - Version 2.0 (http://www.govdata.de/dl-de/by-2-0)\nDatengrundlage: LGL, www.lgl-bw.de.',
      dateiLayerId: null,
    },
    {
      name: 'Verwaltungsgrenzen Stadt- und Landkreise',
      url:
        'https://owsproxy.lgl-bw.de/owsproxy/ows/WFS_LGL-BW_ATKIS_BasisDLM_VerwGr?SERVICE=WFS&version=1.1.0&request=GetFeature&typename=nora:v_at_kreis',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
      farbe: '#038a78',
      deckkraft: 0.8,
      zoomstufe: 9.5,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 8,
      id: null,
      quellangabe:
        'owsproxy.lgl-bw.de/owsproxy/ows/WFS_LGL-BW_ATKIS_BasisDLM_VerwGr\n\nLizenz: Datenlizenz Deutschland - Namensnennung - Version 2.0 (http://www.govdata.de/dl-de/by-2-0)\nDatengrundlage: LGL, www.lgl-bw.de.',
      dateiLayerId: null,
    },
    {
      name: 'Verwaltungsgrenzen Gemeinden',
      url:
        'https://owsproxy.lgl-bw.de/owsproxy/ows/WFS_LGL-BW_ATKIS_BasisDLM_VerwGr?SERVICE=WFS&version=1.1.0&request=GetFeature&typename=nora:v_at_gemeinde',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
      farbe: '#02c9af',
      deckkraft: 0.7,
      zoomstufe: 12,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 9,
      id: null,
      quellangabe:
        'owsproxy.lgl-bw.de/owsproxy/ows/WFS_LGL-BW_ATKIS_BasisDLM_VerwGr\n\nLizenz: Datenlizenz Deutschland - Namensnennung - Version 2.0 (http://www.govdata.de/dl-de/by-2-0)\nDatengrundlage: LGL, www.lgl-bw.de.',
      dateiLayerId: null,
    },
  ];
  public static orthofotos: SaveWeitereKartenebeneCommand[] = [
    {
      name: 'Orthofotos 10cm',
      url:
        'https://owsproxy.lgl-bw.de/owsproxy/ows/WMS_LGL-BW_ATKIS_DOP_10_Bildflugkacheln_Aktualitaet?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&BBOX=47.39999999999999858%2C7.200000000000000178%2C50%2C10.66153982078683704&CRS=EPSG%3A4326&WIDTH=1966&HEIGHT=1476&LAYERS=verm%3Av_dop_10_bildflugkacheln&STYLES=&FORMAT=image%2Fpng&DPI=72&MAP_RESOLUTION=72&FORMAT_OPTIONS=dpi%3A72&TRANSPARENT=TRUE',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      farbe: undefined,
      deckkraft: 1.0,
      zoomstufe: 8.7,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 10,
      id: null,
      quellangabe:
        'owsproxy.lgl-bw.de/owsproxy/ows/WMS_LGL-BW_ATKIS_DOP_10_Bildflugkacheln_Aktualitaet\n\nLizenz: Datenlizenz Deutschland - Namensnennung - Version 2.0 (http://www.govdata.de/dl-de/by-2-0) Datengrundlage: LGL, www.lgl-bw.de.',
      dateiLayerId: null,
    },
    {
      name: 'Orthofotos 20cm',
      url:
        'https://owsproxy.lgl-bw.de/owsproxy/ows/WMS_LGL-BW_ATKIS_DOP_20_Bildflugkacheln_Aktualitaet?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&BBOX=47.39999999999999858%2C7.200000000000000178%2C50%2C10.69999999999999929&CRS=EPSG%3A4326&WIDTH=1658&HEIGHT=1232&LAYERS=verm%3Av_dop_20_bildflugkacheln&STYLES=&FORMAT=image%2Fpng&DPI=72&MAP_RESOLUTION=72&FORMAT_OPTIONS=dpi%3A72&TRANSPARENT=TRUE',
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      farbe: undefined,
      deckkraft: 1.0,
      zoomstufe: 8.7,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 11,
      id: null,
      quellangabe:
        'owsproxy.lgl-bw.de/owsproxy/ows/WMS_LGL-BW_ATKIS_DOP_20_Bildflugkacheln_Aktualitaet\n\nLizenz: Datenlizenz Deutschland - Namensnennung - Version 2.0 (http://www.govdata.de/dl-de/by-2-0) Datengrundlage: LGL, www.lgl-bw.de.',
      dateiLayerId: null,
    },
  ];
  public static bisWfs: SaveWeitereKartenebeneCommand[] = [
    PredefinedWeitereKartenebenen.createBisWFSLayer('ArbeitsstelleLinie', 'Arbeitsstelle (Linie)'),
    PredefinedWeitereKartenebenen.createBisWFSLayer('ArbeitsstellePunkt', 'Arbeitsstelle (Punkt) '),
    PredefinedWeitereKartenebenen.createBisWFSLayer('ArbeitsstellenPhaseFrLinie', 'Arbeitsstellen-Phase FR (Linie)'),
    PredefinedWeitereKartenebenen.createBisWFSLayer('ArbeitsstellenPhaseFrPunkt', 'Arbeitsstellen-Phase FR (Punkt)'),
    PredefinedWeitereKartenebenen.createBisWFSLayer('ArbeitsstellenPhaseLinie', 'Arbeitsstellen-Phase (Linie)'),
    PredefinedWeitereKartenebenen.createBisWFSLayer('ArbeitsstellenPhasePunkt', 'Arbeitsstellen-Phase (Punkt)'),
    PredefinedWeitereKartenebenen.createBisWFSLayer('ArbeitsstellenUmleitung', 'Arbeitsstellen-Umleitung'),
  ];
  public static bisWms: SaveWeitereKartenebeneCommand[] = [
    PredefinedWeitereKartenebenen.createBisWMSLayer('Arbeitsstelle', 'Arbeitsstelle'),
    PredefinedWeitereKartenebenen.createBisWMSLayer(
      'ArbeitsstellenPhaseFrStrecke',
      'Arbeitsstellen-Phase FR (Strecke)'
    ),
    PredefinedWeitereKartenebenen.createBisWMSLayer('ArbeitsstellenPhaseFrSymbol', 'Arbeitsstellen-Phase FR (Symbol)'),
    PredefinedWeitereKartenebenen.createBisWMSLayer('ArbeitsstellenPhaseStrecke', 'Arbeitsstellen-Phase (Strecke)'),
    PredefinedWeitereKartenebenen.createBisWMSLayer('ArbeitsstellenPhaseSymbol', 'Arbeitsstellen-Phase (Symbol)'),
    PredefinedWeitereKartenebenen.createBisWMSLayer('ArbeitsstellenUmleitung', 'Arbeitsstellen-Umleitung'),
  ];

  private static createBisWFSLayer(typeName: string, displayText: string): SaveWeitereKartenebeneCommand {
    return {
      name: displayText,
      url: `https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2externwfs/wfs?typeNames=bis2externwfs:${typeName}`,
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
      farbe: '#1c5b9a',
      deckkraft: 1.0,
      zoomstufe: 13.5,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 12,
      id: null,
      quellangabe:
        'https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2externwfs/wfs?REQUEST=GetCapabilities',
      dateiLayerId: null,
    };
  }

  private static createBisWMSLayer(layer: string, displayText: string): SaveWeitereKartenebeneCommand {
    return {
      name: displayText,
      url: `https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2extern/wms?LAYERS=${layer}`,
      weitereKartenebeneTyp: WeitereKartenebeneTyp.WMS,
      farbe: undefined,
      deckkraft: 1.0,
      zoomstufe: 13.5,
      zindex: predefinedWeitereKartenebenenBaseZIndex + 13,
      id: null,
      quellangabe: 'https://bis2.strassen.baden-wuerttemberg.de/api/geoserver/bis2extern/wms?REQUEST=GetCapabilities',
      dateiLayerId: null,
    };
  }
}
