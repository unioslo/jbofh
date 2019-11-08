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

Files
-----
This script can replace one or more of the following files in a jBofh.jar
archive:

jbofh.properties
    JBofh configuration file

cacert.pem
    Custom CA-certificate chain(s) for use with the JBofh setting
    ``InternalTrustManager.enable=true``

log4j.properties
    Debug log configuration (loaded with the ``-d`` flag in JBofh)

log4j_normal.properties
    Default log configuration

All these files are packaged with the jBofh jar file as part of the build.
Running this script will create a file 'jbofh_new.jar', which will contain the
new files, specified as arguments.

Dependencies
------------
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

# Files that can be replaced - and the arguments to do so.
KNOWN_FILES = (
    ('cacert.pem', 'cacerts', ('-c', '--cert-file')),
    ('jbofh.properties', 'settings', ('-p', '--property-file')),
    ('log4j.properties', 'logconf_debug', ('--log-config-debug',)),
    ('log4j_normal.properties', 'logconf_normal', ('--log-config-normal',)),
)


class TempDir(object):
    """ A simple tempfile.mkdtemp context. """

    def __init__(self, suffix='jbofh-workdir'):
        self.suffix = suffix
        self.path = None

    def copy(self, filename, destname):
        """ Copy ``filename`` into the tempdir as ``destname``. """
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
    """
    Run a command using subprocess.Popen

    :type cmd: list
    """
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


def patch_jar(filename, filemap):
    """
    Copy ca-cert file and/or prop-file into a jar file.

    :type filename: str
    :param filename: the jar-file to patch

    :type filemap: dict
    :param filemap:
        A mapping of files to patch into the jar file. E.g.:

        {'jbofh.properties': '/path/to/file.properties',
         'cacert.pem': '/path/to/certs.pem'}
    """
    if not filemap:
        raise ValueError("No files given - nothing to do!")

    update_cmd = [JAR_CMD, 'uf', filename]

    with TempDir() as workdir:
        # Copy (replacement) files to workdir
        # We need to recreate the same directory and file name structure as we
        # want the files to have in the jar archive.
        for name in filemap:
            workdir.copy(filemap[name], name)
            update_cmd.extend(['-C', workdir.path, name])

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
        raise ValueError('File %r is not a regular file' % (filename, ))
    return filename


def main(inargs=None):
    """ Main. """
    parser = argparse.ArgumentParser(
        description=DESCRIPTION,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )

    # Patchable files
    files_args = parser.add_argument_group(
        'files',
        'Files to patch into archive.')
    for filename, dest, args in KNOWN_FILES:
        kwargs = {
            'dest': dest,
            'type': filename_type,
            'help': ('Replace {} in the JAR-file '
                     'with %(metavar)s').format(filename),
            'metavar': '<file>',
        }
        files_args.add_argument(*args, **kwargs)

    # In/out
    io_args = parser.add_argument_group(
        'archives',
        'Source and destination archives')
    out_file = io_args.add_mutually_exclusive_group()
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
    io_args.add_argument(
        'source',
        type=filename_type,
        help='Input JAR-file',
    )

    args = parser.parse_args(inargs)

    # Select jar-file
    if args.destination and args.destination != args.source:
        logger.info('Creating new archive %r from %r',
                    args.destination, args.source)
        shutil.copyfile(args.source, args.destination)
        filename = args.destination
    else:
        logger.warning('Updating archive %r in place!', args.source)
        filename = args.source

    # Select files to patch
    filemap = {}
    for name, attr, _ in KNOWN_FILES:
        value = getattr(args, attr)
        if not value:
            continue
        filemap[name] = value
        logger.info('Patching archive with %s=%r', name, value)

    patch_jar(filename, filemap)
    logger.info("Archive %r updated", filename)


if __name__ == '__main__':
    logging.basicConfig(level=DEFAULT_LOG_LEVEL,
                        format=DEFAULT_LOG_FORMAT)
    main()
