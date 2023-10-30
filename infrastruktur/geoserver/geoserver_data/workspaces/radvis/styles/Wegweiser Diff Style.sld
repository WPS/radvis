<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
                       xmlns="http://www.opengis.net/sld" 
                       xmlns:ogc="http://www.opengis.net/ogc" 
                       xmlns:xlink="http://www.w3.org/1999/xlink" 
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>Attribute-based point</Name>
    <UserStyle>
      <Title>Wegweisende Beschilderungen Diff</Title>
      <Abstract>Wegweisende Beschilderungen, Änderungen aus VP-Info</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <Name>Neu</Name>
          <Title>Neu</Title>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
			  <ogc:PropertyName>revtype</ogc:PropertyName>
			  <ogc:Literal>0</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#00FF00</CssParameter>
                </Fill>
              </Mark>
            <Size>6</Size>
          </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>Aktualisiert</Name>
          <Title>Aktualisiert</Title>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
			  <ogc:PropertyName>revtype</ogc:PropertyName>
			  <ogc:Literal>1</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#0000FF</CssParameter>
                </Fill>
              </Mark>
            <Size>6</Size>
          </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>Entfernt</Name>
          <Title>Entfernt</Title>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
			  <ogc:PropertyName>revtype</ogc:PropertyName>
			  <ogc:Literal>2</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#FF0000</CssParameter>
                </Fill>
              </Mark>
            <Size>6</Size>
          </Graphic>
          </PointSymbolizer>
        </Rule>
        <VendorOption name="ruleEvaluation">first</VendorOption>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>