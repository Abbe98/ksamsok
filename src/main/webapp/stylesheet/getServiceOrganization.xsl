<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="2.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:pres="http://kulturarvsdata.se/presentation#"
xmlns:srw="http://www.loc.gov/zing/srw/">
<xsl:output method='html' version='1.0' encoding='iso-8859-1' indent='yes'/>

<xsl:template match="/">
<html>
<head>
<style type="text/css">
body
{
	font-family: Verdana,arial;
	font-size: 11px;
	margin-top: 0;
	margin-bottom: 0;
	padding: 0;
	height: 100%;
}

h1
{
	font-family: Verdana,arial;
	font-size: 14px;
	font-weight: bold;
}

h2
{
	font-family: Verdana,arial;
	font-size: 12px;
	font-weight: bold;
}

p
{
	font-family: Verdana;
	font-size: 11px;
}

td
{
	font-family: Verdana;
	font-size: 11px;
	border-bottom: 1px solid #999999;
}

th
{
	font-family: Verdana;
	font-size: 11px;
}

li
{
	font-family: Verdana;
	font-size: 11px;
}

#red
{
	color: #990000;
	font-weight: bold;
}

#box1
{
	border-left: 1px solid #999999;
	border-right: 1px solid #999999;
	border-bottom: 1px solid #999999;
	width:800px;
	height: 100%;
	padding: 10px;
	margin-right: auto;
	margin-left: auto;
	background-color: #ededed;
	text-align: center;
}

.divider
{
	border-bottom: solid thin black;
	margin: 5px 0px 5px 0px;
}

table
{
	margin-left: auto;
	margin-right: auto;
	text-align: left;
}

</style>
</head>
<body>
<div id="box1">
	<img src="../bilder/ksmsok_logga.png" />
	<div id="red">Demo</div>
  	<h2>Kulturarvsdata</h2>
  	<xsl:for-each select="//institution">
  		<table>
  			<tr>
  				<th colspan="2"><xsl:value-of select="namnswe"/></th>
  			</tr>
  			<tr>
  				<td>F�rkortning av organisation:</td>
  				<td><xsl:value-of select="kortnamn"/></td>
  			</tr>
  			<tr>
  				<td>Organisationens namn p� svenska:</td>
  				<td><xsl:value-of select="namnswe"/></td>
  			</tr>
  			<tr>
  				<td>Organisationens namn p� engelska:</td>
  				<td><xsl:value-of select="namneng"/></td>
  			</tr>
  			<tr>
  				<td>Beskrivning av organisationen p� svenska:</td>
  				<td><xsl:value-of select="beskrivswe"/></td>
  			</tr>
  			<tr>
  				<td>Beskrivning av organisationen p� engelska:</td>
  				<td><xsl:value-of select="beskriveng"/></td>
  			</tr>
  			<tr>
  				<td>Adressf�lt 1:</td>
  				<td><xsl:value-of select="adress1"/></td>
  			</tr>
  			<tr>
  				<td>Adressf�lt 2:</td>
  				<td><xsl:value-of select="adress2"/></td>
  			</tr>
  			<tr>
  				<td>Postadress:</td>
  				<td><xsl:value-of select="postadress"/></td>
  			</tr>
  			<tr>
  				<td>Kontaktperson:</td>
  				<td><xsl:value-of select="kontaktperson"/></td>
  			</tr>
  			<tr>
  				<td>E-postadress till kontaktperson:</td>
  				<td><xsl:value-of select="epostkontaktperson"/></td>
  			</tr>
  			<tr>
  				<td>Organisationens webbplats:</td>
  				<td><xsl:value-of select="websida"/></td>
  			</tr>
  			<tr>
  				<td>K-sams�k relaterad sida:</td>
  				<td><xsl:value-of select="websidaks"/></td>
  			</tr>
  			<tr>
  				<td>URL till l�guppl�st bild:</td>
  				<td><xsl:value-of select="lowressurl"/></td>
  			</tr>
  			<tr>
  				<td>URL till tummnagel:</td>
  				<td><xsl:value-of select="thumbnailurl"/></td>
  			</tr>
  			<tr>
  				<th colspan="2">Tj�nster</th>
  			</tr>
  			<xsl:for-each select="services/service">
  				<tr>
  					<td><xsl:value-of select="namn"/></td>
  					<td><xsl:value-of select="beskrivning"/></td>
  				</tr>
  			</xsl:for-each>
  		</table>
  		<div class="divider"></div>
  	</xsl:for-each>
</div>
</body>
</html>
</xsl:template>
</xsl:stylesheet>