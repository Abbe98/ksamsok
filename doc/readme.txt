Enkelt harvest och content-repository

Utvecklat med java 1.6 och tomcat 6.0.

** Installation

* konfigurera datak�lla f�r applikationen jdbc/harvestdb

Ex i context.xml f�r en hsqldb i fil-mode:
    <Resource name="jdbc/harvestdb" auth="Container" type="javax.sql.DataSource"
    		factory="org.apache.tomcat.dbcp.dbcp.BasicDataSourceFactory"
		maxActive="10" maxIdle="5" maxWait="-1"
		username="sa" password="" driverClassName="org.hsqldb.jdbcDriver"
		url="jdbc:hsqldb:file:d:/temp/harvestdb"
		testOnBorrow="true" testOnReturn="false" testWhileIdle="true"
		timeBetweenEvictionRunsMillis="300000"
		minEvictableIdleTimeMillis="-1" numTestsPerEvictionRun="10"
		defaultAutoCommit="false" />

Jdbc-driver f�r r�tt databastyp m�ste in i tomcat/lib. I och med inf�randet av indexering av
spatialdata f�ljer hsqldb med i lib (den anv�nds av internt av geotools), men den b�r/m�ste
flyttas till tomcat/lib *om* det �r den databastypen som ocks� anv�nds f�r lagring av inneh�ll
f�r att undvika klassladdarproblem.

* skapa tabeller enligt sql i sql/repo.sql f�r datak�llan

* peka ev ut var lucene ska l�gga sitt index med javaflaggan -Dsamsok-lucene-index-dir=[s�kv�g till katalog]
 om ej pekas ut kommer indexet att l�ggas i /var/lucene-index/ksamsok.

* peka ev ut var filer som sk�rdas ska l�ggas innan de behandlas -Dsamsok-harvest-spool-dir=[s�kv�g till katalog]
 en sk�rd h�mtas f�rst till en tempor�rfil och flyttas sen till spool-katalogen s� att den kan
 �teranv�ndas om jobbet g�r fel vid senare steg, tex lagring i databas
 om ej pekas ut kommer javas default-tempdir att anv�ndas

* ange om inte datak�llan st�djer spatialt data med -Dsamsok.spatial=false
 default �r sant och en klass f�r att hantera spatialdata kommer att f�rs�ka h�rledas
 fram utfr�n klassen p� uppkopplingen - fn st�ds bara oracle

* ange ev egen klass f�r att hantera spatialt data med -Dsamsok.spatial.class=xx.yy.Z
 klassen m�ste implementera interfacet se.raa.ksamsok.spatial.GMLDBWriter och ha en publik
 default-konstruktor
 fr�mst f�r debug eller tredjeparts utv, har inget defaultv�rde och b�r normalt ej s�ttas

* k�r "ant war" och kopiera war-fil till tomcat/webapps

** Anv�ndning

Ett grundl�ggande (fult och ej stylat) gr�nssnitt finns f�r att hantera tj�nster, uppdatera
lucene-index och s�kning.

Tj�nster kan l�ggas upp i gr�nssnittet f�r sk�rd med http (OAI-PMH-[SAMSOK]) eller via en
fill�sning. En fil m�ste ha samma syntax som en h�mtning mha OAIPMHHarvestJob.getRecords()
vilket �r den metod som ocks� anv�nds f�r att g�ra en sk�rd via OAI-PMH.
Ex
	fos = new FileOutputStream(new File("d:/temp/kthdiva.xml"));
	OAIPMHHarvestJob.getRecords("http://www.diva-portal.org/oai/kth/OAI", null, null, "oai_dc", null, fos);

Tj�nster som sk�rdas med OAI-DC-schemat g�rs om till mycket enkel ksams�ks-xml.

Tj�nsterna sk�rdas enligt den periodicitet som anges i cron-str�ngen. Om kolumnen jobbstatus
ej visar "OK" �r det troligt att cron-str�ngen ej �r korrekt.
St�d finns ocks� f�r att k�ra en tj�nst interaktivt, indexera om en tj�nst (inneb�r att uppdatera
lucene-indexet f�r den tj�nsten - f�r�ndrar ej det sk�rdade datat) eller att indexera om alla tj�nster.
Indexoptimering kan ocks� schemal�ggas.

Ett mycket simpelt s�kgr�nssnitt finns vilket s�ker i f�ltet "text" (fritext) och visar
tr�ffarna som xml.

Admin-delen av centralnoden skyddas och anv�ndare m�ste ha rollen "ksamsok" f�r att f�
anv�nda den vilket m�ste s�ttas upp i tomcatkonf p� vanligt s�tt.

* Modifierad jrdf-jar
Nedanst�ende �r (ful-)patchar som beh�vs f�r att teckenkodning ska fungera ok med
parseType="Literal" f�r presentations-xml:en samt f�r att fixa ett namespace-problem
d�r namespaces som anv�ndes i en literal fortsatte att skrivas ut felaktigt.
Teckenkodningsproblemet kan ev ha berott p� en felaktig locale-inst�llning. Denna
patch f�ruts�tter att alla dokument �r i utf-8 vilket de �r i ksams�k, men
det �r inte tillr�ckligt f�r att submitta en generell patch till jrdf-projektet.
Namespace-problemet l�ses annars bara med en rad se "newNamespaceMappings.clear();"
nedan.

Index: src/java/org/jrdf/parser/rdfxml/RdfXmlParser.java
===================================================================
--- src/java/org/jrdf/parser/rdfxml/RdfXmlParser.java	(revision 2880)
+++ src/java/org/jrdf/parser/rdfxml/RdfXmlParser.java	(working copy)
@@ -407,6 +407,7 @@
         try {
             //saxFilter.clear();
             saxFilter.setDocumentURI(inputSource.getSystemId());
+            saxFilter.setDocumentEncoding("UTF-8"); //inputSource.getEncoding());
 
             SAXParserFactory factory = SAXParserFactory.newInstance();
             factory.setFeature("http://xml.org/sax/features/namespaces", true);
Index: src/java/org/jrdf/parser/rdfxml/SAXFilter.java
===================================================================
--- src/java/org/jrdf/parser/rdfxml/SAXFilter.java	(revision 2880)
+++ src/java/org/jrdf/parser/rdfxml/SAXFilter.java	(working copy)
@@ -106,6 +106,11 @@
     private URI documentURI;
 
     /**
+     * The document's encoding.
+     */
+    private String documentEncoding;
+
+    /**
      * Flag indicating whether the parser parses stand-alone RDF
      * documents. In stand-alone documents, the rdf:RDF element is
      * optional if it contains just one element.
@@ -193,6 +198,7 @@
         elInfoStack.clear();
         charBuf.setLength(0);
         documentURI = null;
+        documentEncoding = null;
         deferredElement = null;
 
         newNamespaceMappings.clear();
@@ -211,6 +217,20 @@
         this.documentURI = createBaseURI(documentURI);
     }
 
+    public void setDocumentEncoding(String encoding) {
+        this.documentEncoding = encoding;
+        try {
+            escapedWriter = new OutputStreamWriter(escapedStream, encoding);
+            th.setResult(new StreamResult(escapedWriter));
+        } catch (Exception e) {
+            e.printStackTrace();
+        }
+    }
+
+    public String getDocumentEncoding() {
+        return documentEncoding;
+    }
+
     public void setParseStandAloneDocuments(boolean standAloneDocs) {
         parseStandAloneDocuments = standAloneDocs;
     }
@@ -273,6 +293,8 @@
         if (parseLiteralMode) {
             appendStartTag(qName, attributes);
             xmlLiteralStackHeight++;
+            // clear now that they have been used once
+            newNamespaceMappings.clear();
         } else {
             ElementInfo parent = peekStack();
             ElementInfo elInfo = new ElementInfo(parent, qName, namespaceURI, localName);
@@ -615,7 +637,11 @@
         try {
             th.characters(c, start, length);
             escapedWriter.flush();
-            sb.append(escapedStream.toString());
+            if (documentEncoding != null) {
+                sb.append(escapedStream.toString(documentEncoding));
+            } else {
+                sb.append(escapedStream.toString());
+            }
         } catch (IOException e) {
             throw new SAXException("Error occurred escaping attribute text ", e);
         }
@@ -749,4 +775,5 @@
             this.baseURI = baseURI.resolve(createBaseURI(uriString));
         }
     }
+
 }
