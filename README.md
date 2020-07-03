jBofh
=====

jBofh is a Cerebrum administration tool for end-users.  It communicates
with Cerebrum’s bofhd daemon via an XML RPC protocol.

jBofh was recently updated not to use obsolete Java classes directly
from within its source code, but some remaining work still needs
to be done in order to get rid of obsolete Java modules that jBofh
is indirectly dependent on (see _Building the required library
JARs_).


Building
--------

To build jBofh, first retrieve the necessary dependencies, assuming RHEL:

	# dnf install ant java-latest-openjdk

jBofh requires JDK 13 or newer, but at the time of writing the default
RHEL JDK is 1.8.  You may optionally change the default runtime- and
development environments to the latest that we just installed this way:

	# alternatives --set java java-latest-openjdk.x86_64
	# alternatives --set javac java-latest-openjdk.x86_64

If you are on a different architecture than x86_64, you may list and
configure the default alternative interactively this way:

	# alternatives --config java
	# alternatives --config javac

jBofh uses ant(1) as its build system, which does not respect the chosen
default javac alternative.  To build jBofh with ant using the latest JDK:

	% JAVA_HOME=/lib/jvm/java ant dist

If you did not update the default Java environment to the latest above,
you may have to point `JAVA_HOME` to the specific directory where the
latest bin/javac executable is on your system.

You may choose to make the value of `JAVA_HOME` persistent by exporting
the environment variable in your shell login script, or by modifying
the system-wide /etc/java/java.conf.

The standard build should produce a JAR archive (an “über JAR”)
that includes the necessary runtime dependencies that jBofh requires.
Should these not have been included, you may consult the following
section.


### Building the required library JARs

jBofh is dependent on four externally developed modules that are not
part of the standard core JDK modules.  In addition and by the time of
writing jBofh uses old versions of some of those modules that are no
longer maintained by their respective developpers as they have reached
the End-of-Life (EOL) cycle but fortunately enough the code is in open
source and, license by the authors, is granted in order to redistribute
freely (each module has its own license mantra and flavor such as Apache,
GPL … but all of them grant free copyright).  In all cases that should
not prevent Cerebrum and even jBofh itself from being fully licensed
under GPL.

In fact, jBofh just uses those libraries, but we are including the code
in here for the obsoleted libs as an extra convenience and in order
to better understand jBofh and better debug it in cases there would
be issues with ambiguous root cause that might be originating from the
underlying modules or external APIs.

The source code is therefore copied in here in a tree structure that is
similar to the original structure in the classpath, the main purpose is
not to loose the bytecode files (.class files) on the term and to be able
to generate them again whenever required.  That is surely a temporary
decision until jBofh is migrated to support the latest versions of those
modules and external APIs.  But maybe by that time the obsolete source
code could be dropped from here and the latest bytecodes would rather be
developed, maintained and even probably compiled and provided by their
respective authors.

#### JLine module (jline.jar)

- Used version 1.0 of JLine last commit eece34debfb8ee6898579b23b76635b3f9c0f289
  in GitHub repository:
  https://github.com/jline/jline/tree/master/src/main/java/jline

  We had to fix an annoying bug by adding line 733 to ConsoleReader.java.

- Software license type: BSD (see the headers of the code files).

- Compile and pack jline.jar:

  - Move to lib directory under the jBofh repository.

  - Run:

    	javac @jline/listfiles_jline

  - Run:

    	jar cvfM jline.jar jline/{*.class,*.properties,*.html}

  - Remove the generated .class files under jline/ by probably running:

    	find jline/ -name '*.class' -exec rm {} \;

#### XP, PrintFormat and SmartScroller modules (com.jar)

- Used the following downloaded versions respectively:

  - Version 0.5 (and only one available) from
    http://jclark.com/xml/xp/index.html for XP

  - Only version existing from
    http://www.public.asu.edu/~kintigh/simulation/PrintfFormat.java
    for `PrintFormat`, with one trivial amendement around line 29 in order to
    reflect the package structure (commented in the file as well).

  - Only version existing from:
    http://www.camick.com/java/source/SmartScroller.java via:
    https://tips4java.wordpress.com/2013/03/03/smart-scrolling/

- Software license type: 'private' (in fact for the first two and somehow
  implicitly permissive and probably private for the last one).  XP is
  from the owner of the website jclark.com and is open for free usage
  and redistribution as detailed in the terms of the author in the file
  that is included in com/jclark/jclark.com.xml.xp.copying.txt.

  For `PrintFormat` it is a standard Java Open Source license from the
  time Sun Microsystems existed and owned the development and maintenance
  of all the Java core modules.  The license is summarized in the header
  of the source file com/sun/java/text/PrintfFormat.java.

  For `SmartScroller` this has no explicit license but like
  most of the code that is open on the tips4java.wordpress.com
  the code there is mainly for educational purposes and
  only meant to teach people to code in Java, the author
  had even explicitly said it about his own contributions:
  https://tips4java.wordpress.com/2008/11/06/wrap-layout/#comment-1284
  giving the implicit authorization to use and modify the code however
  one wishes!

- Compile and pack com.jar:

  - Move to lib directory under the jBofh repository.

  - Run:

    	javac @com/listfiles_com

  - Run the following one line command:

    	jar cvfM com.jar com/{jclark/{util/,xml{/,/apps/,/output/,/parse/,/tok\/}},sun/java/text/,camick/}*.class

  - Remove the generated .class files under com/ by probably running:

    	find com/ -name '*.class' -exec rm {} \;

#### Apache modules (org.jar)

- Used the latest stable archived versions of the Apache XMLRPC, LOG4J,
  Commons and WS:

  - XMLRPC: SVN repository at
    http://svn.apache.org/repos/asf/webservices/archive/xmlrpc/
    tag 3.1.3 which is at revision 1754844 by the time of writing.

  - LOG4J: SVN repository at https://svn.apache.org/repos/asf/logging/log4j/
    tag v1_2_17 revision by the time of writing: 1754862 (only files that were
    required for compiling jBofh were included).

  - Commons:

    - Logging: only needed files for compilation checked out from:
      http://svn.apache.org/repos/asf/commons/proper/logging/trunk/ revision
      1748024 by the time of writing.
    - CODEC: only needed files for compilation checked out from:
      http://svn.apache.org/repos/asf/commons/proper/codec/trunk/ revision
      1754939 by the time of writing.

  - WS: SVN repository at http://svn.apache.org/repos/asf/webservices/commons/
    tag util/1.0.2 revision 1684746 by the time of writing.

  - HttpClient: SVN repository at
    http://svn.apache.org/repos/asf/httpcomponents/oac.hc3x/tags/HTTPCLIENT_3_1
    revision 1755063 by the time of writing.

- Software license type: Apache License (see the headers of the code files).

- Compile and pack org.jar:

  - Move to lib directory under the jBofh repository.

  - For a JDK version prior to 11 Run:

    	javac @org/listfiles_org

  - For JDK versions 11 and up, unfortunately the compilation process
    is a bit more complicated and that is mostly due to licensing issues
    that Oracle is imposing to parts of the Java code that is no
    longer considered as part of the Standard Edition but rather of
    the Enterprise Edition.  For jBofh this was mainly relevant to
    the log4j module with a tiny dependency on an external library
    that uses an Oracle EE functinality.  That can be substituted
    by the open source implementation of the JAXB found here
    https://github.com/javaee/jaxb-v2 with those libraries appended to
    the `CLASSPATH` of the compiler the previous command would work fine.

  - Run the following one line command:

    	jar cvfM org.jar {org/apache/{commons/{codec{/,/binary/,/net/},httpclient{/,/auth/,/cookie/,/methods/,/params/,/protocol/,/util/},logging/},log4j{/,/config/,/helpers/,/or/,/spi/},ws/commons/{serialize/,util/},xmlrpc{/,/client{/,/util/},/common/,/jaxb/,/parser/,/serializer/,/util/}}*.class,org/apache/{ws/commons/{serialize/,util/}package.html,xmlrpc/client/XmlRpcClient.properties}}

  - Remove the generated .class files under org/ by probably running:

    	find org/ -name '*.class' -exec rm {} \;


### CA-certificates

jBofh might include a valid cacert.pem in the JAR archive, if it existed
when running `ant dist`.  If this file was not included then, or if it
needs to be replaced, it can be added to the JAR archive later with
the Python script fix_jbofh_jar.py.  It is also possible to override
the included cacert.pem with a file from outside the .jar package at
runtime, by just adding the file with its system path as a value to the
`--ca` argument on the command line (e.g. `java -jar JBofh.jar --ca
/tmp/new_cacert.pem`).


### Settings

jBofh might include a valid jbofh.properties file in the JAR archive, if
it existed when running `ant dist`.  If this file was not included then,
or if it needs to be replaced, it can be added to the JAR archive later
with the Python script fix_jbofh_jar.py.  Furthermore it is possible to
pass extra property parameters to the command line when running JBofh.jar
with the `--set` argument at runtime.


### fix_jbofh_jar.py

Usage:

	python fix_jbofh_jar.py [-c|--cert-file PEM] [-p|--property PROP] /path/to/jBofh.jar

The PEM file should be a complete CA-chain that validates our
bofhd-server(s), in PEM format.

The PROP file is a settings file.  See jbofh.property in the root
directory of this project.

This script will add or replace the settings file and/or the CA
certificate file in the jBofh JAR archive.  It will create a new JAR
archive `jbofh_new.jar` in the working directory.


Installing jBofh
----------------

jBofh uses only Java bytecodes to run, which in principle makes it possible to
run everywhere, or where ever a Java Virtual Machine is installed (look at the
following section), thus installing jBofh is very trivial in most cases and
could be summarized by just copying the JAR file and running the command as
pertailed under _jBofh usage_.


### Runtime requirements

* jBofh requires the latest version of Java runtime available, at the time of
  writing the stable Java version available is '13' and we have included
  bytecode files for the libraries that were compiled with the OpenJDK
  version '13.0.0.33', which means that if you try compiling and
  running jBofh with an older version of OpenJDK, by just using the
  provided library bytecodes that are packed in the JAR files under
  lib/ then you might face trouble and should rather compile all the
  bytecodes and pack them yourself according to the procedure mentioned
  under _Building the required library JARs_.  The same applies if
  you would be using Oracle© JDK, on the other hand Oracle© has a
  full packaging solution that would automate compiling and packing
  up the bytecodes along with the whole JVM (Java Virtual Machine),
  a procedure that should be similar accross different operating
  systems from different vendors and thus would make this code and its
  compiled bytecodes run seemlessly, provided that you have the proper
  license from all parties, including the operating systems' vendors:
  https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/packager.html
  It is also quite possible that jBofh could use external libraries
  dynamically and not at runtime but would then require a different
  compilation procedure, and that would de facto restrain the binaries
  to only run natively on the OS where they where compiled.

  PS: Even though jBofh is supposed to run seamlessly on all operating systems
  that run with a supported JVM, it is not tested and not known to be
  working perfectly with its console interface across all platforms apart
  from the Unix/Linux variants, on the other hand the GUI interface
  which lies in a higher abstraction layer within the Java stack (Swing,
  AWT etc...) is most likely to run seemlessly and without trouble across
  all operating systems.

* It is always recommended to have the latest JVM or Java Runtime
  Environment (JRE) to run jBofh whether it is the Open Source variant
  or the proprietary one, and that is mainly for security reasons.


jBofh usage
-----------

The basic and default usage of jBofh starts with running:

	java -jar <Path to the compiled JBofh main module>JBofh.jar

Providing the `--help` option would guide you further through different
usage possibilities.

jBofh is supposed to have a properly signed certificate to communicate
with the default server defined in the properties' file or explicitly
mentioned on the command line otherwise it would fail once it is run
especially if the property `InternalTrustManager.enable` is not set
to false and the target server doesn't have a valid certificate signed
by a publicly recognized root CA (and whose signatures are in general
included in the updated releases of OpenJDK and Oracle Java).
