<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:ng="http://docbook.org/docbook-ng" xmlns:db="http://docbook.org/ns/docbook"
	version="1.1">
	
	<!-- enable profiling -->
	<xsl:import href="urn:docbkx:stylesheet/profile-eclipse.xsl" />
	
	<xsl:param name="create.plugin.xml">0</xsl:param>
	
	<xsl:param name="html.stylesheet">eclipse.css</xsl:param>
	<xsl:param name="chunk.first.sections" select="1"/>
	<xsl:param name="chunk.section.depth" select="1"/>

	<xsl:param name="eclipse.autolabel" select="1"></xsl:param>
		
	<!-- enable auto numbering on sections -->
	<xsl:param name="section.autolabel">1</xsl:param>
	<xsl:param name="section.autolabel.max.depth">3</xsl:param>
	
	<!-- show sections including depth 3 in table of content -->
	<xsl:param name="toc.section.depth">3</xsl:param>
	
	<xsl:param name="img.src.path">../</xsl:param>
	
	<xsl:param name="generate.index" select="0"/>

	<xsl:param name="chunk.separate.lots" select="1"></xsl:param>
	<xsl:param name="chunk.tocs.and.lots.has.title" select="1"></xsl:param>

</xsl:stylesheet>