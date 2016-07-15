============================
jBofh - bofh client in Java
============================

TODO: About jBofh
TODO: New version with a lot of fixes and improvements, detailed documentation later!

Building jBofh
================

You can build the jBofh jar file by running::

    ant dist

in the root directory of this project.


Requirements
------------

* Ant (version?)
* TODO: More?


CA-certificates
----------------

jBofh will include a ``cacerts.pem``-file in the jar-file, if it exists when
running ``ant dist``. If this file is not included, or if it needs to be
replaced, it can be added to the jar-archive later with the python script
`fix_jbofh_jar.py`_.


Settings
---------
jBofh will include a ``jbofh.properties``-file in the jar-file, if it exists when
running ``ant dist``. If this file is not included, or if it needs to be
replaced, it can be added to the jar-archive later with the python script
`fix_jbofh_jar.py`_.


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

TODO


Runtime requirements?
---------------------

* Java (version?)
* TODO: More?


jBofh usage
-----------

TODO
