<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" 
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
    xmlns="http://www.opengis.net/sld" 
    xmlns:ogc="http://www.opengis.net/ogc" 
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>Attribute-based line</Name>
    <UserStyle>
      <Name>Unser Style</Name>
      <FeatureTypeStyle>
        <Rule>
          <Name>Ortslage außerorts und in Stationierungsrichtung</Name>
          <ogc:Filter>
			<ogc:And>
				<ogc:PropertyIsEqualTo>
				  <ogc:PropertyName>Ortslage</ogc:PropertyName>
				  <ogc:Literal>Außerorts</ogc:Literal>
				</ogc:PropertyIsEqualTo>
				<ogc:PropertyIsEqualTo>
				  <ogc:PropertyName>Richtung</ogc:PropertyName>
				  <ogc:Literal>In Stationierungsrichtung</ogc:Literal>
				</ogc:PropertyIsEqualTo>
			</ogc:And>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#FF0000</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Name>Ortslage innerorts und gegen Stationierungsrichtung</Name>
          <ogc:Filter>
			<ogc:And>
				<ogc:PropertyIsEqualTo>
				  <ogc:PropertyName>Ortslage</ogc:PropertyName>
				  <ogc:Literal>Innerorts</ogc:Literal>
				</ogc:PropertyIsEqualTo>
				<ogc:PropertyIsEqualTo>
				  <ogc:PropertyName>Richtung</ogc:PropertyName>
				  <ogc:Literal>Gegen Stationierungsrichtung</ogc:Literal>
				</ogc:PropertyIsEqualTo>
			</ogc:And>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#00FF00</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
		<Rule>
          <Name>Ortslage außerorts und gegen Stationierungsrichtung</Name>
          <ogc:Filter>
			<ogc:And>
				<ogc:PropertyIsEqualTo>
				  <ogc:PropertyName>Ortslage</ogc:PropertyName>
				  <ogc:Literal>Außerorts</ogc:Literal>
				</ogc:PropertyIsEqualTo>
				<ogc:PropertyIsEqualTo>
				  <ogc:PropertyName>Richtung</ogc:PropertyName>
				  <ogc:Literal>Gegen Stationierungsrichtung</ogc:Literal>
				</ogc:PropertyIsEqualTo>
			</ogc:And>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#00FF00</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
