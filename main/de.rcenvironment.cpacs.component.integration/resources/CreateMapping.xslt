<?xml version="1.0" encoding="UTF-8"?>
<xslt:stylesheet version="1.0" xmlns:xslt="http://www.w3.org/1999/XSL/Transform" xmlns:xsl="http://dummy_namespace" xmlns:map="http://www.dlr.de/sistec/tiva/tool/mapping">

    <xslt:namespace-alias stylesheet-prefix="xsl" result-prefix="xslt" />

    <xslt:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />

    <xslt:template match="/">
        <xsl:stylesheet version="1.0">
            <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />
            <xsl:param name="sourceFilename"></xsl:param>
            <xsl:param name="sourceFile" select="document($sourceFilename)" />
            <xsl:param name="targetFilename"></xsl:param>
            <xsl:param name="targetFile" select="document($targetFilename)" />

            <xsl:template match="/">
                <xslt:copy-of select="//map:mappings" />
            </xsl:template>
        </xsl:stylesheet>
    </xslt:template>

</xslt:stylesheet>
