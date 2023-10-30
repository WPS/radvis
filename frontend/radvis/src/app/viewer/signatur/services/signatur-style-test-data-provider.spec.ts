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

export const belagArtStyleXml =
  '<?xml version="1.0" encoding="ISO-8859-1"?>\n' +
  '<StyledLayerDescriptor version="1.0.0" \n' +
  '    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" \n' +
  '    xmlns="http://www.opengis.net/sld" \n' +
  '    xmlns:ogc="http://www.opengis.net/ogc" \n' +
  '    xmlns:xlink="http://www.w3.org/1999/xlink" \n' +
  '    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n' +
  '  <NamedLayer>\n' +
  '    <Name>Attribute-based line</Name>\n' +
  '    <UserStyle>\n' +
  '      <Name>Unser Style</Name>\n' +
  '      <FeatureTypeStyle>\n' +
  '        <Rule>\n' +
  '          <Name>Unbekannt</Name>\n' +
  '\t\t  <Title></Title>\n' +
  '          <ogc:Filter>\n' +
  '\t\t\t<ogc:PropertyIsEqualTo>\n' +
  '\t\t\t  <ogc:PropertyName>BelagArt</ogc:PropertyName>\n' +
  '\t\t\t  <ogc:Literal>Unbekannt</ogc:Literal>\n' +
  '\t\t\t</ogc:PropertyIsEqualTo>\n' +
  '          </ogc:Filter>\n' +
  '          <LineSymbolizer>\n' +
  '            <Stroke>\n' +
  '              <CssParameter name="stroke">#000000</CssParameter>\n' +
  '            </Stroke>\n' +
  '          </LineSymbolizer>\n' +
  '        </Rule>\n' +
  '      </FeatureTypeStyle>\n' +
  '    </UserStyle>\n' +
  '  </NamedLayer>\n' +
  '</StyledLayerDescriptor>\n';

export const withPredicateStyleXml =
  '<?xml version="1.0" encoding="ISO-8859-1"?>\n' +
  '<StyledLayerDescriptor version="1.0.0" \n' +
  '    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" \n' +
  '    xmlns="http://www.opengis.net/sld" \n' +
  '    xmlns:ogc="http://www.opengis.net/ogc" \n' +
  '    xmlns:xlink="http://www.w3.org/1999/xlink" \n' +
  '    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\n' +
  '  <NamedLayer>\n' +
  '    <Name>Attribute-based line</Name>\n' +
  '    <UserStyle>\n' +
  '      <Name>Unser Style</Name>\n' +
  '      <FeatureTypeStyle>\n' +
  '        <Rule>\n' +
  '          <Name>Ortslage außerorts und in Stationierungsrichtung</Name>\n' +
  '          <ogc:Filter>\n' +
  '\t\t\t<ogc:And>\n' +
  '\t\t\t\t<ogc:PropertyIsEqualTo>\n' +
  '\t\t\t\t  <ogc:PropertyName>Ortslage</ogc:PropertyName>\n' +
  '\t\t\t\t  <ogc:Literal>Außerorts</ogc:Literal>\n' +
  '\t\t\t\t</ogc:PropertyIsEqualTo>\n' +
  '\t\t\t\t<ogc:PropertyIsEqualTo>\n' +
  '\t\t\t\t  <ogc:PropertyName>Richtung</ogc:PropertyName>\n' +
  '\t\t\t\t  <ogc:Literal>In Stationierungsrichtung</ogc:Literal>\n' +
  '\t\t\t\t</ogc:PropertyIsEqualTo>\n' +
  '\t\t\t</ogc:And>\n' +
  '          </ogc:Filter>\n' +
  '          <LineSymbolizer>\n' +
  '            <Stroke>\n' +
  '              <CssParameter name="stroke">#FF0000</CssParameter>\n' +
  '            </Stroke>\n' +
  '          </LineSymbolizer>\n' +
  '        </Rule>\n' +
  '        <Rule>\n' +
  '          <Name>Ortslage innerorts und gegen Stationierungsrichtung</Name>\n' +
  '          <ogc:Filter>\n' +
  '\t\t\t<ogc:And>\n' +
  '\t\t\t\t<ogc:PropertyIsEqualTo>\n' +
  '\t\t\t\t  <ogc:PropertyName>Ortslage</ogc:PropertyName>\n' +
  '\t\t\t\t  <ogc:Literal>Innerorts</ogc:Literal>\n' +
  '\t\t\t\t</ogc:PropertyIsEqualTo>\n' +
  '\t\t\t\t<ogc:PropertyIsEqualTo>\n' +
  '\t\t\t\t  <ogc:PropertyName>Richtung</ogc:PropertyName>\n' +
  '\t\t\t\t  <ogc:Literal>Gegen Stationierungsrichtung</ogc:Literal>\n' +
  '\t\t\t\t</ogc:PropertyIsEqualTo>\n' +
  '\t\t\t</ogc:And>\n' +
  '          </ogc:Filter>\n' +
  '          <LineSymbolizer>\n' +
  '            <Stroke>\n' +
  '              <CssParameter name="stroke">#00FF00</CssParameter>\n' +
  '            </Stroke>\n' +
  '          </LineSymbolizer>\n' +
  '        </Rule>\n' +
  '\t\t<Rule>\n' +
  '          <Name>Ortslage außerorts und gegen Stationierungsrichtung</Name>\n' +
  '          <ogc:Filter>\n' +
  '\t\t\t<ogc:PropertyIsEqualTo>\n' +
  '\t\t\t\t<ogc:PropertyName>Wegeniveau</ogc:PropertyName>\n' +
  '\t\t\t\t<ogc:Literal>Gehweg</ogc:Literal>\n' +
  '\t\t\t</ogc:PropertyIsEqualTo>\n' +
  '          </ogc:Filter>\n' +
  '          <LineSymbolizer>\n' +
  '            <Stroke>\n' +
  '              <CssParameter name="stroke">#00FF00</CssParameter>\n' +
  '            </Stroke>\n' +
  '          </LineSymbolizer>\n' +
  '        </Rule>\n' +
  '      </FeatureTypeStyle>\n' +
  '    </UserStyle>\n' +
  '  </NamedLayer>\n' +
  '</StyledLayerDescriptor>\n';
