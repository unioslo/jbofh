/*
 * Copyright 2002-2016 University of Oslo, Norway
 *
 * This file is part of Cerebrum.
 *
 * Cerebrum is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Cerebrum is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cerebrum; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package no.uio.jbofh;

import java.awt.Frame;
import java.io.IOException;

/**
 * These methods are used by the gui, which is currently implemented
 * in swing.  By using this interface, it is possible to compile jbofh
 * when swing is not available.
 */

public interface JBofhFrame {

    /**
     *
     */
    java.awt.Frame AWT_frame = null;

    /**
     *
     * @param prompt
     * @param parent
     * @return
     * @throws MethodFailedException
     */
    public String getPasswordByJDialog(String prompt, Frame parent) 
        throws MethodFailedException;

    /**
     *
     * @return
     */
    public String getCmdLineText();

    /**
     *
     * @param prompt
     * @param addHist
     * @return
     * @throws IOException
     */
    public String promptArg(String prompt, boolean addHist) throws IOException;

    /**
     *
     * @param msg
     * @param crlf
     */
    public void showMessage(String msg, boolean crlf);

    /**
     *
     * @return
     */
    public boolean confirmExit();

    /**
     *
     * @param on
     */
    public void showWait(boolean on);

    /**
     *
     * @param title
     * @param msg
     */
    public void showErrorMessageDialog(String title, String msg);

}

// arch-tag: 30786514-5874-45e8-a9d7-3bb2c4c3e5cb
