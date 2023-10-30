<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
                       xmlns="http://www.opengis.net/sld" 
                       xmlns:ogc="http://www.opengis.net/ogc" 
                       xmlns:xlink="http://www.w3.org/1999/xlink" 
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>Attribute-based line</Name>
    <UserStyle>
      <Title>Straßenklassen</Title>
      <Abstract>Straßenklassen entsprechend denen aus BEMaS</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <Name>Autobahnen</Name>
          <Title>Autobahnen</Title>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
			  <ogc:PropertyName>strassenklasse</ogc:PropertyName>
			  <ogc:Literal>A</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#ff0000</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Name>Bundestraßen</Name>
          <Title>Bundestraßen</Title>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
			  <ogc:PropertyName>strassenklasse</ogc:PropertyName>
			  <ogc:Literal>B</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#006bff</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Name>Landes- und Staatsstraßen</Name>
          <Title>Landes- und Staatsstraßen</Title>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
			  <ogc:PropertyName>strassenklasse</ogc:PropertyName>
			  <ogc:Literal>L</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#00aa00</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Name>Kreisstraßen</Name>
          <Title>Kreisstraßen</Title>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
			  <ogc:PropertyName>strassenklasse</ogc:PropertyName>
			  <ogc:Literal>K</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#9c4a39</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Name>Gemeindestraßen</Name>
          <Title>Gemeindestraßen</Title>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
			  <ogc:PropertyName>strassenklasse</ogc:PropertyName>
			  <ogc:Literal>G</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#ffff00</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <VendorOption name="ruleEvaluation">first</VendorOption>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
