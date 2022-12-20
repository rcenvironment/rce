<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:db="http://docbook.org/ns/docbook"
	version="1.1">
	
	<!-- enable profiling -->
	<xsl:import href="urn:docbkx:stylesheet/profile-docbook.xsl" />
	
	<!-- add linebreak; usage <?linebreak?> -->
	<xsl:template match="processing-instruction('linebreak')">
	  <fo:block/>
	</xsl:template>
	<!-- Line break for AsciiDoc --> 
	<xsl:template match="processing-instruction('asciidoc-br')"> 
	  <fo:block/> 
	</xsl:template>
	
	<!-- Verbatim text formatting (programlistings) -->
	<xsl:attribute-set name="monospace.verbatim.properties">
		<xsl:attribute name="font-size">
			<xsl:text>6.5pt</xsl:text>
		</xsl:attribute>
	</xsl:attribute-set>
	<xsl:attribute-set name="verbatim.properties">
		<xsl:attribute name="space-before.minimum">1em</xsl:attribute>
		<xsl:attribute name="space-before.optimum">1em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">1em</xsl:attribute>
		<xsl:attribute name="border-color">#444444</xsl:attribute>
		<xsl:attribute name="border-style">solid</xsl:attribute>
		<xsl:attribute name="border-width">0.1pt</xsl:attribute>
		<xsl:attribute name="padding-top">0.2em</xsl:attribute>
		<xsl:attribute name="padding-left">0.2em</xsl:attribute>
		<xsl:attribute name="padding-right">0.2em</xsl:attribute>
		<xsl:attribute name="padding-bottom">0.2em</xsl:attribute>
		<xsl:attribute name="margin-left">0.2em</xsl:attribute>
		<xsl:attribute name="margin-right">0.2em</xsl:attribute>
	</xsl:attribute-set>
	<!-- Shade (background) programlistings -->
	<xsl:param name="shade.verbatim">1</xsl:param>
	<xsl:attribute-set name="shade.verbatim.style">
		<xsl:attribute name="background-color">#F0F0F0</xsl:attribute>
	</xsl:attribute-set>
	
	<!-- remove 2nd title page -->
	<xsl:template name="book.titlepage.verso"/>
	<xsl:template name="book.titlepage.before.verso"/>
	
	<!-- enable auto numbering on sections -->
	<xsl:param name="section.autolabel">1</xsl:param>
	<xsl:param name="section.autolabel.max.depth">3</xsl:param>
	
	<!-- show sections including depth 3 in table of content -->
	<xsl:param name="toc.section.depth">3</xsl:param>
	
	<!-- add some space before section headers -->
	<xsl:attribute-set name="section.level1.properties">
		<xsl:attribute name="space-before.minimum">3.8em</xsl:attribute>
	  	<xsl:attribute name="space-before.optimum">3.8em</xsl:attribute>
	  	<xsl:attribute name="space-before.maximum">3.8em</xsl:attribute>
	</xsl:attribute-set>
	<xsl:attribute-set name="section.level2.properties">
		<xsl:attribute name="space-before.minimum">2.2em</xsl:attribute>
	  	<xsl:attribute name="space-before.optimum">2.2em</xsl:attribute>
	  	<xsl:attribute name="space-before.maximum">2.2em</xsl:attribute>
	</xsl:attribute-set>
	<xsl:attribute-set name="section.level3.properties">
		<xsl:attribute name="space-before.minimum">1.8em</xsl:attribute>
	  	<xsl:attribute name="space-before.optimum">1.8em</xsl:attribute>
	  	<xsl:attribute name="space-before.maximum">1.8em</xsl:attribute>
	</xsl:attribute-set>
	
	<!-- beautify notes -->
	<xsl:param name="admon.graphics">0</xsl:param>
	<xsl:attribute-set name="admonition.title.properties">
		<xsl:attribute name="font-size">8pt</xsl:attribute>
		<xsl:attribute name="font-weight">bold</xsl:attribute>
		<xsl:attribute name="hyphenate">false</xsl:attribute>
		<xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
	</xsl:attribute-set>
		<xsl:attribute-set name="admonition.properties">
		<xsl:attribute name="font-size">8pt</xsl:attribute>
		<xsl:attribute name="font-weight">normal</xsl:attribute>
		<xsl:attribute name="hyphenate">false</xsl:attribute>
		<xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
	</xsl:attribute-set>
	<xsl:template name="nongraphical.admonition">
  		<xsl:variable name="id">
    		<xsl:call-template name="object.id"/>
  		</xsl:variable>
		<fo:block space-before.minimum="0.8em"
		          space-before.optimum="1em"
		          space-before.maximum="1.2em"
		          start-indent="0.75in"
		          end-indent="0.25in"
		          border-top="0.5pt solid black"
		          border-bottom="0.5pt solid black"
		          margin-left="3.0em"
		          margin-right="0.0em"
		          padding-left="0.5em"
		          padding-right="0.5em"
		          padding-top="4pt"
		          padding-bottom="4pt"
		          id="{$id}">
			<xsl:if test="$admon.textlabel != 0 or title">
		    	<fo:block keep-with-next='always'
		                  xsl:use-attribute-sets="admonition.title.properties">
		        	<xsl:apply-templates select="." mode="object.title.markup"/>
		      </fo:block>
		      </xsl:if>
		      <fo:block xsl:use-attribute-sets="admonition.properties">
			      <xsl:apply-templates/>
			  </fo:block>
		</fo:block>
	</xsl:template>
	
	<!-- set text align of titles to left -->
	<xsl:attribute-set name="formal.title.properties">
	  <xsl:attribute name="text-align">left</xsl:attribute>
	</xsl:attribute-set>
	
	<!-- wrap backspace character -->
	<xsl:attribute-set name="monospace.verbatim.properties">
    	<xsl:attribute name="wrap-option">wrap</xsl:attribute>
    	<xsl:attribute name="hyphenation-character">\</xsl:attribute>
	</xsl:attribute-set>

</xsl:stylesheet>