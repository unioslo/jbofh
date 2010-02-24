/*
 * Copyright 2002-2010 University of Oslo, Norway
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

/*
 * JBofh.java
 *
 * Created on November 1, 2002, 12:20 PM
 */

package no.uio.jbofh;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.List;

import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;
import jline.Completor;

import com.sun.java.text.PrintfFormat;

/**
 * Tab-completion utility for use with readline.  Also supports translation 
 * of short-form of unique commands to full-form.
 *
 * @author  runefro
 */

class BofhdCompleter implements Completor {
    JBofh jbofh;
    Vector possible;
    Iterator iter;
    Category logger;
    Hashtable complete;
    private boolean enabled;

    BofhdCompleter(JBofh jbofh, Category logger) {
        this.jbofh = jbofh;
        this.logger = logger;
        this.enabled = false;
        complete = new Hashtable();
        //buildCompletionHash();
    }
    
    public void setEnabled(boolean state) {
        this.enabled = state;
    }

    /**
     * {   'access': {   'disk': 'access_disk', ... } }
     */
    public void addCompletion(Vector cmd_parts, String target) 
        throws BofhdException{
        Hashtable parent = complete;
        for(Enumeration e = cmd_parts.elements(); e.hasMoreElements(); ) {
            String protoCmd = (String) e.nextElement();
            Object tmp = parent.get(protoCmd);
            if(tmp == null) {
                if(e.hasMoreElements()) {
                    parent.put(protoCmd, tmp = new Hashtable());
                    parent = (Hashtable) tmp;
                } else {
                    parent.put(protoCmd, target);
                }
            } else {
                if(tmp instanceof Hashtable) {
                    if(! e.hasMoreElements()) {
                        throw new BofhdException(
                            "Existing map target for"+cmd_parts);
                    }
                    parent = (Hashtable) tmp;
                } else {
                    if(e.hasMoreElements()) {
                        throw new BofhdException(
                            "Existing map target is not a "+
                            "Hashtable for "+cmd_parts);
                    } else {
                        parent.put(protoCmd, target);
                    }
                }
            }
        }
    }

    /**
     * Analyze the command that user has entered by comparing with the
     * list of legal commands, and translating to the command-parts
     * full name.  When the command is not unique, the value of expat
     * determines the action.  If < 0, or not equal to the current
     * argument number, an AnalyzeCommandException is thrown.
     * Otherwise a list of legal values is returned.
     *
     * If expat < 0 and all checked parameters defined in the list of
     * legal commands were ok, the expanded parameters are returned,
     * followed by the corresponding protocol_command.  Otherwise an
     * exception is thrown.
     *
     * @param cmd the arguments to analyze
     * @param expat the level at which expansion should occour, or < 0 to translate
     * @throws AnalyzeCommandException
     * @return list of completions, or translated commands
     */    

    public Vector analyzeCommand(Vector cmd, int expat) throws AnalyzeCommandException { 
        Hashtable parent = complete;
        Vector cmdStack = new Vector();
        int lvl = 0;

        while(expat < 0 || lvl <= expat) {
            String this_cmd = null;
            if (lvl < cmd.size()) this_cmd = (String) cmd.get(lvl);
            Vector thisLevel = new Vector();

            for(Enumeration enumCompleter = parent.keys(); 
                enumCompleter.hasMoreElements(); ) {
                String this_key = (String) enumCompleter.nextElement();
                if(this_cmd == null || this_key.startsWith(this_cmd)) {
                    thisLevel.add(this_key);
                    if(this_key.equals(this_cmd) && expat < 1) {
                        // expanding, and one command matched exactly
                        thisLevel.clear();
                        thisLevel.add(this_key);
                        break;
                    }
                }
            }
            if (lvl == expat)
                return thisLevel;
            if(thisLevel.size() != 1 || 
                (expat < 0 && cmdStack.size() >= cmd.size())) {
                if(expat < 0){
                    if (thisLevel.size() == 0)
                        throw new AnalyzeCommandException("Unknown command");
                    throw new AnalyzeCommandException(
                        "Incomplete command, possible subcommands: "+thisLevel);
                }
                throw new AnalyzeCommandException(cmd+" -> "+thisLevel+","+lvl);
            }
            String cmdPart = (String) thisLevel.get(0);
            cmdStack.add(cmdPart);
            Object tmp = parent.get(cmdPart);
            if(!(tmp instanceof Hashtable)) {
                if(expat < 0){
                    cmdStack.add(tmp);
                    return cmdStack;
                }
                return new Vector();  // No completions
            }
            parent = (Hashtable) tmp;
            lvl++;
        }
        logger.error("oops: analyzeCommand "+parent+", "+lvl+", "+expat);
        throw new RuntimeException("Internal error");  // Not reached
    }

    public int complete(String str, int cursor, List clist) {
        String cmdLineText;
        cmdLineText = str;
        if(! this.enabled) 
            return 0;
        Vector args;
        try {
            args = jbofh.cLine.splitCommand(cmdLineText);
        } catch (ParseException pe) {
            iter = null;
            return 0;
        }
        int len = args.size();
        if(! cmdLineText.endsWith(" ")) len--;
        if(len < 0) len = 0;
        if(len >= 2) {
            iter = null;
            return 0;
        }
        try {
            possible = analyzeCommand(args, len);
            iter = possible.iterator();
        } catch (AnalyzeCommandException e) {
            logger.debug("Caught: ", e);
            iter = null;
        }
        while (iter != null && iter.hasNext()) 
            clist.add((String)iter.next() + ' ');
        return str.lastIndexOf(" ", cursor) + 1;
    }
}

// arch-tag: d0af466d-fc21-44cf-9677-ce75a0a9e1ef
