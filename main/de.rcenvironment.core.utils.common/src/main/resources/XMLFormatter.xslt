<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- Style sheet for formatting DOM XML output -->
	<xsl:param name="indent" select="'&#x9;'" />
	<xsl:param name="newline" select="'&#xA;'" />

	<xsl:template match="node()">
		<xsl:param name="indent-sum" />
		<!-- Indent begin tag. -->
		<xsl:value-of select="$newline" />
		<xsl:value-of select="$indent-sum" />
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<!-- This if allows for self-closing tags. -->
			<xsl:if test="count(node())">
				<xsl:apply-templates>
					<xsl:with-param name="indent-sum" select="concat($indent, $indent-sum)" />
				</xsl:apply-templates>
				<xsl:if test="count(node()) > count(text())">
					<!-- Indent end tag. -->
					<xsl:value-of select="$newline" />
					<xsl:value-of select="$indent-sum" />
				</xsl:if>
			</xsl:if>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="text()[normalize-space(.)=''] | comment()[normalize-space(.)='']" />
	<xsl:template match="text()">
		<xsl:value-of select="normalize-space(.)" />
	</xsl:template>
</xsl:stylesheet>

