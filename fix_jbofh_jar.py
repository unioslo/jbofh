#!/usr/bin/env python
#
# -*- coding: utf-8 -*-
#
# Copyright 2003-2019 University of Oslo, Norway
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
import argparse
import logging
import os
import shutil
import subprocess
import tempfile

try:
    from shlex import quote as cmd_quote
except ImportError:
    from pipes import quote as cmd_quote


logger = logging.getLogger('patch-jbofh')

DEFAULT_LOG_FORMAT = '%(levelname)s: %(message)s'
DEFAULT_LOG_LEVEL = logging.INFO

JAR_CMD = 'jar'


class TempDir(object):

    def __init__(self, suffix='jbofh-workdir'):
        self.suffix = suffix
        self.path = None

    def copy(self, filename, destname):
        dest = os.path.join(self.path, destname)
        logger.debug('copying file=%r to %r', filename, dest)
        shutil.copyfile(filename, dest)

    def __enter__(self):
        self.path = tempfile.mkdtemp(suffix=self.suffix)
        logger.debug('created tempdir=%r', self.path)
        return self

    def __exit__(self, *args, **kwargs):
        if self.path:
            shutil.rmtree(self.path)
            logger.debug('removed tempdir=%r', self.path)


def run_cmd(cmd):
    pretty = ' '.join(cmd_quote(arg) for arg in cmd)
    logger.debug('running: %s', pretty)

    proc = subprocess.Popen(cmd,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)

    out, err = proc.communicate()
    if out:
        logger.debug('stdout: %r', out)
    if err:
        logger.debug('stderr: %r', err)

    if proc.returncode:
        logger.debug('exit: %r', proc.returncode)
        raise RuntimeError("Error running: %r (exitcode=%r)" %
                           (cmd, proc.returncode))


def patch_jar(filename, cert_file=None, property_file=None):
    """
    Copy ca-cert file and/or prop-file into a jar file.

    :type filename: str
    :param filename: the jar-file to patch

    :type cert_file: str or NoneType
    :param cert_file:
        filename of the ca-cert file (or None if we're not (re)placing a
        cert-file.

    :type property_file: str or NoneType
    :param property_file:
        filename of the .properties file (or None if we're not (re)placing a
        property file.
    """
    if (not cert_file) and (not property_file):
        raise SystemExit("Nothing to do.")

    update_cmd = [JAR_CMD, 'uf', filename]

    with TempDir() as workdir:
        # Copy (replacement) files to workdir
        # We need to recreate the same directory and file name structure as we
        # want the files to have in the jar archive.
        if property_file is not None:
            workdir.copy(property_file, 'jbofh.properties')
            update_cmd.extend(['-C', workdir.path, 'jbofh.properties'])
        if cert_file is not None:
            workdir.copy(cert_file, 'cacert.pem')
            update_cmd.extend(['-C', workdir.path, 'cacert.pem'])

        # Update dest with the given files
        run_cmd(update_cmd)


DESCRIPTION = """
Patch config-files and resources in a JBofh.jar archive.

This utility is for people who want to update the JBofh.jar file
without running ant.  It can replace the cacert.pem and
jbofh.properties files.
""".lstrip()


def filename_type(filename):
    if not os.path.exists(filename):
        raise ValueError('No file %r' % (filename, ))
    if not os.path.isfile(filename):
        raise ValueError('Path %r is not a regular file' % (filename, ))
    return filename


def main(inargs=None):
    """ Main. """
    parser = argparse.ArgumentParser(
        description=DESCRIPTION,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        '-c', '--cert-file',
        dest='cacerts',
        type=filename_type,
        help='Replace cacert.pem in the JAR-file with %(metavar)s',
        metavar='<file>',
    )
    parser.add_argument(
        '-p', '--property-file',
        dest='properties',
        type=filename_type,
        help='Replace jbofh.properties in the JAR-file with %(metavar)s',
        metavar='<file>',
    )
    out_file = parser.add_mutually_exclusive_group()
    out_file.add_argument(
        '-o', '--output',
        dest='destination',
        default='jbofh_new.jar',
        help='Write new JAR-file to %(metavar)s (default: %(default)s)',
        metavar='<file>',
    )
    out_file.add_argument(
        '--in-place',
        dest='destination',
        action='store_const',
        const=None,
        help='Update source JAR-file in-place',
    )
    parser.add_argument(
        'source',
        type=filename_type,
        help='Input JAR-file',
    )

    args = parser.parse_args(inargs)

    logger.info('Patching jar=%r with cacerts=%r, properties=%r',
                args.source, args.cacerts, args.properties)

    if args.destination and args.destination != args.source:
        logger.debug('copying %r to %r', args.source, args.destination)
        shutil.copyfile(args.source, args.destination)
        filename = args.destination
    else:
        logger.warning('Updating %r in place!', args.source)
        filename = args.source

    patch_jar(filename=filename,
              cert_file=args.cacerts,
              property_file=args.properties)

    logger.info("Archive written to %r", args.destination)


if __name__ == '__main__':
    logging.basicConfig(level=DEFAULT_LOG_LEVEL,
                        format=DEFAULT_LOG_FORMAT)
    main()
