<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:ng="http://docbook.org/docbook-ng" xmlns:db="http://docbook.org/ns/docbook"
	version="1.1">
	
	<xsl:import href="urn:docbkx:stylesheet/docbook.xsl" />
	
	<xsl:param name="draft.mode">no</xsl:param>

	<xsl:param name="chapter.autolabel" select="0"></xsl:param>
	<xsl:param name="html.ext">.xhtml</xsl:param>
	<xsl:param name="ulink.target">_blanc</xsl:param>
		
	<xsl:param name="section.autolabel">0</xsl:param>
	
	<xsl:param name="admon.style">margin-left: 0.5in; margin-right: 0.5in;</xsl:param>

</xsl:stylesheet>