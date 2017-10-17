<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cpacs_schema.xsd">
 <xsl:output method="xml" media-type="text/xml" />
 <xsl:template match="/">
  <toolInput>
   <data>
    <var1>
     <xsl:value-of select="/cpacs/vehicles/aircraft/model/reference/area" />
     <!--layer=Default-->
    </var1>
   </data>
  </toolInput>
 </xsl:template>
</xsl:stylesheet>
<!--xsl-easyControl - (C) 2003-2007 SoftProject GmbH-->
<!--Source: "H:\Eigene Dateien\DLR\Projekte\TIVAII\CPACS_own\cpacs.xml"|Type:"xml"-->
<!--Destination: "C:\Dokumente und Einstellungen\kund_ma\Desktop\toolInput.xml"|Type:"xml"-->
<!--Document type: Input Driven-->