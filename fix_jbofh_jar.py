#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# Copyright 2003, 2014 University of Oslo, Norway
#
# This file is part of Cerebrum.
#
# Cerebrum is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# Cerebrum is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Cerebrum; if not, write to the Free Software Foundation,
# Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
""" Script to replace the config and CA-certs in jBofh.

This script can replace one of (or both) the following files in jBofh.jar:

    - jbofh.properties   Properties file
    - cacert.pem         CA-certificate chain(s)

Both of these files are packaged with the jBofh jar file as part of the build.
Running this script will create a file 'jbofh_new.jar', which will contain the
new files, specified as arguments.

This utility depends on:
    - unzip
    - jar

"""
import getopt
import os
import shutil
import sys
import time


def fix_file(jar_in, jar_out, cert_file, property_file):
    """ Copy ca-cert file and/or prop-file into the jar file.

    @type jar_in: str
    @param jar_in: filename of the source jar-file

    @type jar_out: str or NoneType
    @param jar_out:
        filename of the output jar-file. If None, we alter the L{jar_in} file
        in place.

    @type cert_file: str or NoneType
    @param cert_file:
        filename of the ca-cert file (or None if we're not (re)placing a
        cert-file.

    @type property_file: str or NoneType
    @param property_file:
        filename of the .properties file (or None if we're not (re)placing a
        property file.

    """
    if (not cert_file) and (not property_file):
        raise SystemExit("Nothing to do.")

    jar = 'jar'
    tmp_dir = 'tmp_%s' % time.time()

    # Create a copy of the jar input file,
    # or modify input file directly?
    if jar_out:
        shutil.copyfile(jar_in, jar_out)
    else:
        # In-place update
        jar_out = jar_in

    os.mkdir(tmp_dir)
    update_cmd = [jar, 'uf', jar_out]

    # Copy (replacement) files to tmp_dir
    # We need to recreate the same directory and file name structure as we want
    # the files to have in the jar archive.
    if property_file is not None:
        shutil.copyfile(property_file,
                        os.path.join(tmp_dir, 'jbofh.properties'))
        update_cmd.extend(['-C', tmp_dir, 'jbofh.properties'])
    if cert_file is not None:
        shutil.copyfile(cert_file, os.path.join(tmp_dir, 'cacert.pem'))
        update_cmd.extend(['-C', tmp_dir, 'cacert.pem'])

    # Update jar_out with the given files
    ret = os.spawnvp(os.P_WAIT, jar, update_cmd)
    if ret != 0:
        raise SystemExit("Error running: '%s'" % ' '.join(update_cmd))

    print "New file: %s" % jar_out
    shutil.rmtree(tmp_dir)


def usage():
    """ Return usage string. """
    return """Usage: fix_jbofh_jar.py [-c cert_file | -p property_file] jar_file

This utility is for people who want to update the JBofh.jar file
without running ant.  It can replace the cacert.pem and
jbofh.properties files.
"""


def main():
    """ Main. """
    opts, args = getopt.getopt(sys.argv[1:], 'c:p:h',
                               ['cert-file', 'property-file', 'help'])

    cert_file = property_file = None
    for opt, val in opts:
        if opt in ('-c', '--cert-file'):
            cert_file = val
        elif opt in('-p', '--property-file'):
            property_file = val
        elif opt in('-h', '--help'):
            print usage()
            raise SystemExit()
        else:
            raise SystemExit("Invalid arguments.\n" + usage())

    jar_file = sys.argv[-1]
    if not jar_file.endswith(".jar"):
        raise SystemExit("Invalid jar-file '%s'\n%s" % (jar_file, usage()))

    # Do the actual work
    fix_file(jar_file, 'jbofh_new.jar', cert_file, property_file)

if __name__ == '__main__':
    main()
