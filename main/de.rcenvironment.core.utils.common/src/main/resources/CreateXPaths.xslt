<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:map="http://www.dlr.de/sistec/tiva/tool/mapping">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:param name="sourceXPath"></xsl:param>
    <xsl:param name="targetXPath"></xsl:param>
 
	<xsl:template match="/">
    <map:mappings>
    
        <!-- Loop over all nodes with no childs (all leafs) -->
        <xsl:for-each select="//*[count(child::*) = 0]">
		<map:mapping mode="delete">
			<map:source>
			
			<xsl:value-of select="$sourceXPath"/>
			<xsl:for-each select="(ancestor-or-self::*)">
			<xsl:choose>
				<xsl:when test="count(ancestor::*) = 0"/>
				<xsl:otherwise>/<xsl:value-of select="concat(name(.), '[', count(preceding-sibling::*[name(current()) = name(.)]) + 1, ']')"/>
					<xsl:for-each select="@*">[@<xsl:value-of select="name()"/>="<xsl:value-of select="."/>"]</xsl:for-each>
				</xsl:otherwise>
			</xsl:choose>
			</xsl:for-each>
			
			</map:source>

			<map:target>
			
			<xsl:value-of select="$targetXPath"/>
			<xsl:for-each select="(ancestor-or-self::*)">
			<xsl:choose>
				<xsl:when test="count(ancestor::*) = 0"/>
				<xsl:otherwise>/<xsl:value-of select="concat(name(.), '[', count(preceding-sibling::*[name(current()) = name(.)]) + 1, ']')"/>
					<xsl:for-each select="@*">[@<xsl:value-of select="name()"/>="<xsl:value-of select="."/>"]</xsl:for-each>
				</xsl:otherwise>
			</xsl:choose>
			</xsl:for-each>
			
			</map:target>

        </map:mapping>

		</xsl:for-each>

    </map:mappings>
   	</xsl:template>
</xsl:stylesheet>
