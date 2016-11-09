============================
jBofh - bofh client in Java
============================

jBofh is a Cerebrum end user client for the Cerebrum Bofhd daemon.

jBofh is written in Java and has been updated lately in order not to use
obsolete Java classes directly from within its source code, but some effort
still needs to be done in order to get rid of obsolete Java modules that JBofh
is indirectly dependent on (see in next section
`Building the required library JARs`_).


Building jBofh
================

You can build the jBofh jar file by running::

    ant dist

in the root directory of this project, the JAR files for the libraries that
jBofh uses should have been included, otherwise look at the following section.


Building the required library JARs
----------------------------------

jBofh is dependent on 4 externally developped modules that are not part of the
standard core JDK modules. In addition and by the time of writing jBofh uses old
versions of those modules that are no longer maintained by their respective
developpers as they have reached the End Of Life (EOL) cycle but fortunately
the code is in open source and license by the authors is granted in order to
redistribute freely (each module has its own license mantra and flavor such as
Apache, GPL ... but all grant free copyright). In all cases  that should not
stand in the way that Cerebrum and jBofh itself are fully licensed under GPL.
In fact, jBofh just uses those libraries, but we are including the code in here
as an extra convenience and in order to better understand jBofh and better
debug it in cases there would be issues with ambiguous root cause that might be
originating from the underlying modules or external APIs.

The source code is therefore copied in here in a tree structure that is similar
to the original structure in the classpath, the main purpose is not to loose the
bytecode files (\*.class) on the term and to be able to generate them again
whenever required. That is surely a temporary decision until jBofh is migrated
to support the latest versions of those modules and external APIs. But maybe by
that time the obsolete source code could be dropped from here and the latest
bytecodes would rather be developed, maintained and even probably compiled and
provided by their respective authors.


JLine module (jline.jar):

 -Used version 1.0 of JLine last commit eece34debfb8ee6898579b23b76635b3f9c0f289
  in GitHub repository:
  https://github.com/jline/jline/tree/master/src/main/java/jline
  We had to fix an annoying bug by adding line 733 to 'ConsoleReader.java'.

 -Software license type: BSD (see the headers of the code files).

 -Compile and pack jline.jar:

  -Move to lib directory under the jBofh repository.

  -Run::

   javac @jline/listfiles_jline

  -Run::

   jar cvfM jline.jar jline/{*.class,*.properties,*.html}

  -Remove the generated \*.class files under 'jline/'.


XP and PrintFormat modules (com.jar):

 -Used the following downloaded versions respectively:

  -Version 0.5 (and only one available) from http://jclark.com/xml/xp/index.html
  for XP

  -Only version existing from
   http://www.public.asu.edu/~kintigh/simulation/PrintfFormat.java
   for PrintFormat, with one trivial amendement around line 29 in order to
   reflect the package structure (commented in the file as well).

 -Software license type: 'private' (in fact for both).
  XP is from the owner of the website jclark.com and is open for free usage and
  redistribution as detailed in the terms of the author in the file that is
  included in here: 'com/jclark/jclark.com.xml.xp.copying.txt'.
  For PrintFormat it is a standard Java Open Source license from the time
  Sun Microsystems existed and owned the development and maintenance of all
  the Java core modules. The license is summarized in the header of the source
  file 'com/sun/java/text/PrintfFormat.java'.

 -Compile and pack com.jar:

  -Move to lib directory under the jBofh repository.

  -Run::

   javac @com/listfiles_com

  -Run the following one line command:
   +-----------------------------------------------------------------------+
   |jar cvfM com.jar com/{jclark/{util/,xml{/,/apps/,/output/,/parse/,/tok\|
   |/}},sun/java/text/}*.class                                             |
   +-----------------------------------------------------------------------+

  -Remove the generated \*.class files under 'com/'.


Apache modules (org.jar):

 -Used the latest stable archived versions of the Apache XMLRPC, LOG4J,
  Commons and WS:

  -XMLRPC: SVN repository at
   http://svn.apache.org/repos/asf/webservices/archive/xmlrpc/
   tag 3.1.3 which is at revision 1754844 by the time of writing.

  -LOG4J: SVN repository at https://svn.apache.org/repos/asf/logging/log4j/
   tag v1_2_17 revision by the time of writing: 1754862 (only files that were
    required for compiling JBofh were included).

  -Commons:
   -Logging: only needed files for compilation checked out from:
    http://svn.apache.org/repos/asf/commons/proper/logging/trunk/ revision
    1748024 by the time of writing.
   -CODEC: only needed files for compilation checked out from:
    http://svn.apache.org/repos/asf/commons/proper/codec/trunk/ revision
    1754939 by the time of writing.

  -WS: SVN repository at http://svn.apache.org/repos/asf/webservices/commons/
   tag util/1.0.2 revision 1684746 by the time of writing.

  -HttpClient: SVN repository at
   http://svn.apache.org/repos/asf/httpcomponents/oac.hc3x/tags/HTTPCLIENT_3_1
   revision 1755063 by the time of writing.

 -Software license type: Apache License (see the headers of the code files).

 -Compile and pack org.jar:

  -Move to lib directory under the jBofh repository.

  -Run::

   javac @org/listfiles_org

  -Run the following one line command:
   +-------------------------------------------------------------------------+
   |jar cvfM org.jar {org/apache/{commons/{codec{/,/binary/,/net/},\         |
   |httpclient{/,/auth/,/cookie/,/methods/,/params/,/protocol/,/util/},\     |
   |logging/},log4j{/,/config/,/helpers/,/or/,/spi/},ws/commons/{serialize/,\|
   |util/},xmlrpc{/,/client{/,/util/},/common/,/jaxb/,/parser/,/serializer/,\|
   |/util/}}*.class,org/apache/{ws/commons/{serialize/,util/}package.html,\  |
   |xmlrpc/client/XmlRpcClient.properties}}                                  |
   +-------------------------------------------------------------------------+

  -Remove the generated \*.class files under 'org/'.


Requirements
------------

* Ant (latest version) whether installed on your OS or embeded in an IDE like
  NetBeans.
* Latest OpenJDK or Oracle© JDK


CA-certificates
----------------

jBofh might include a valid ``cacert.pem``-file in the jar-file, if it existed
when running ``ant dist``. If this file was not included then, or if it needs to
be replaced, it can be added to the jar-archive later with the python script
`fix_jbofh_jar.py`_. It is also possible to override the included cacert.pem
with a file from outside the .jar package at runtime, by just adding the file
with its system path as a value to the --ca argument on the command line
(e.g. java -jar JBofh.jar --ca /tmp/new_cacert.pem).


Settings
---------
jBofh might include a valid ``jbofh.properties``-file in the jar-file, if it
existed when running ``ant dist``. If this file was not included then, or if it
needs to be replaced, it can be added to the jar-archive later with the python
script `fix_jbofh_jar.py`_. Furthermore it is possible to pass extra property
parameters to the commad line when running JBofh.jar with --set argument at
runtime.


fix_jbofh_jar.py
----------------

Usage::

    python fix_jbofh_jar.py [-c|--cert-file PEM] [-p|--property PROP] /path/to/jBofh.jar

        The PEM file should be a complete CA-chain that validates our
        bofhd-server(s), in PEM format.

        The PROP file is a settings file. See jbofh.property in the root
        directory of this project.

This script will add or replace the settings file and/or the CA certificate file
in the jBofh jar archive. It will create a new jar archive ``jbofh_new.jar`` in
the working directory.


Installing jBofh
=================

jBofh uses only Java bytecodes to run, which in principle makes it possible to
run everywhere, or where ever a Java Virtual Machine is installed (look at the
following section), thus installing jBofh is very trivial in most cases and
could be summarized by just copying the JAR file and running the command as
pertailed under `jBofh usage`_


Runtime requirements
---------------------

* jBofh requires the latest version of Java runtime available, at the time of
  writing the stable Java version available is '8' and we have included bytecode
  files for the libraries that were compiled with the OpenJDK version
  '1.8.0_101', which means that if you try compiling and running JBofh with an
  older version of OpenJDK, by just using the provided library bytecodes that
  are packed in the JAR files under 'lib/' then you might face trouble and
  should rather compile all the bytecodes and pack them yourself according to
  the procedure in here:
  `Building the required library JARs`_. The same applies if you would be using
  Oracle© JDK, on the other hand Oracle© has a full packaging solution that
  would automate compiling and packing up the bytecodes along with the whole JVM
  (java virtual machine), a procedure that should be similar accross different
  operating systems from different vendors and thus would make this code and its
  compiled bytecodes run seemlessly, provided that you have the proper license
  from all parties, including the operating systems' vendors:
  https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/packager.html

  PS: Even though jBofh is supposed to run seemlessly on all operating systems
  that run with a supported JVM, it is not tested and not known to be working
  perfectly with its console interface across all platforms apart from the UNIX/
  Linux variants, on the other hand the GUI interface which lies in a higher
  abstraction layer within the Java stack (Swing, AWT etc...) is most likely to
  run seemlessly and without trouble across all operating systems.

* It is always recommended to have the latest JVM or Java Runtime Environment
  (JRE) to run JBofh whether it is the Open Source variant or the proprietary
  one, and that is mainly for security reasons.


jBofh usage
-----------

The basic and default usage of jBofh starts with running::

    java -jar <Path to the compiled JBofh main module>JBofh.jar

    Providing the --help option would guide you further through different usage
    possibilities.

    JBofh is supposed to have the properly signed certificate to communicate
    with the default server defined in the properties' file or explicitly
    mentioned on the command line otherwise it would fail once it is run.


Change Log
==========

Changes and improvements with version 0.9.9
-------------------------------------------

- All obsolete code was upgraded in the core jBofh Java classes, references to
  Vector and Hashtable Java classes have been replaced, other standard coding
  issues were detected and corrected with the help of the NetBeans IDE.
- All API libraries (e.g. XMLRPC, JLine) were upgraded to the latest stable.
  released versions before EOL see `Building the required library JARs`_
- After upgrade of the underlying XMLRPC API and touching the code a bit, a
  serious bug/vulnerability that would have allowed sending the password over an
  unsecure though encrypted wire during handshake has been fixed.
- Possibility to pass muliple --set arguments on the command line separated by
  commas like that::

   bofh --gui --set gui.font.size.outputwindow=9,gui.font.name.outputwindow=Sans

- JBofh trims blanks at the end of the command now before sending them over to
  the XMLRPC daemon.
- Reverse search in the JBofh console (not the GUI)works well now, in addition
  to all the previously defect keymaps that were fixed due to a newer and more
  stable version of JLine.
- A line break has been added at the end of each command as requested by some
  Bofh users.
- The GUI interface had some face liftings as well, we hereby name the most
  important and relevant ones:

    - The keyboard is focused automatically on the text fields when the focus is
      set on the open Java GUI (focus is set as well by default when the GUI is
      started).

    - Spaces have been forced between the results of the commands for an
      enhanced readability experience.
