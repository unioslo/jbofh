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

/*
 * CommandLine.java
 *
 * Created on November 4, 2002, 11:06 AM
 */

package no.uio.jbofh;

import java.io.IOException;
import java.text.ParseException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import jline.ConsoleReader;

import org.apache.log4j.Logger;


/**
 *
 * @author  runefro, hamar
 */
public class CommandLine {
    Logger logger;
    JBofh jbofh;
    Timer timer;
    IdleTerminatorTask terminatorTask;
    ConsoleReader consolereader;

    /**
     * <code>IdleTerminatorTask</code> is used to terminate the
     * program when no command is entered for a time.  A warning is
     * issued after warnDelay seconds.  Unless stopWaiting() is called
     * terminateDelay seconds after that, the program will exit.
     * Setting warnDelay = 0 disables this feature.
     *
     */
    class IdleTerminatorTask extends TimerTask {
        long period;            // frequency with which we are called (in ms)
        int warnDelay, terminateDelay;  // seconds to wait
        long waited = -1;
        boolean has_warned = false;

        IdleTerminatorTask(long period, int warnDelay, int terminateDelay) {
            this.period = period;
            this.warnDelay = warnDelay;
            this.terminateDelay = terminateDelay;
        }
        
        public void startWaiting() {
            waited = 0;
            has_warned = false;
        }
        @Override
        public void run() {
            if (waited == -1) return;
            if (warnDelay == 0) return;
            waited += period;
            if (waited > (warnDelay + terminateDelay) * 1000) {
                jbofh.showMessage("Terminating program due to inactivity", true);
                jbofh.bye();
            } else if (waited > (warnDelay) * 1000) {
                if (! has_warned)
                    jbofh.showMessage("Session about to timeout, press enter to cancel", true);
                has_warned = true;
            }
        }
        public void stopWaiting() {
            waited = -1;
        }
    }

    /** Creates a new instance of CommandLine
     * @param logger
     * @param jbofh
     * @param warnDelay
     * @param terminateDelay */
    public CommandLine(Logger logger, JBofh jbofh, int warnDelay, int terminateDelay) {
        this.jbofh = jbofh;
        terminatorTask = new IdleTerminatorTask(60*1000, warnDelay, terminateDelay);
        timer = new Timer(false);
        timer.schedule(terminatorTask, 1000, 60*1000);

        if(! (jbofh != null && jbofh.guiEnabled)) {
            try {
                this.consolereader = new ConsoleReader();
            }
            catch (IOException e) {
                System.err.println("Could not open jLine library: " + e);
                System.exit(1);
            }
            this.logger = logger;
            /*Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        //Readline.cleanup();
                    }
                });*/
        }
    }

    /**
     * Split string into tokens, using whitespace as delimiter.
     * Matching '/" pairs can be used to include whitespace in the
     * tokens.  Sub-groups marked by matching parenthesis are returned
     * as sub-ArrayLists. Sub-sub groups are not allowed.
     *
     * @param str
     * @return an ArrayList of parsed tokens.
     */
    @SuppressWarnings("unchecked")
    ArrayList splitCommand(String str) throws ParseException {
        /* This could probably be done easier by using String.parse(), but that would require JDK1.4 */
        String trim = str.trim();
        char chars[] = (trim+" ").toCharArray();
        ArrayList ret = new ArrayList();
        ArrayList subCmd = null, curApp = ret;
        int i = 0, pstart = 0;
        Character quote = null;
        while(i < chars.length) {
            if(quote != null) {
                if(chars[i] == quote) {
                    if(i >= pstart) { // We allow empty strings within quotes
                        curApp.add(new String(trim.substring(pstart, i)));
                    }
                    pstart = i+1;
                    quote = null;
                }
            } else {
                if(chars[i] == '\'' || chars[i] == '"') {
                    pstart = i+1;
                    quote = chars[i];
                } else if(chars[i] == ' ' || chars[i] == '\t' || chars[i] == '('
                                                           || chars[i] == ')') {
                    if(i > pstart) {
                            curApp.add(new String(trim.substring(pstart, i)));
                        }
                    pstart = i+1;
                    if(chars[i] == ')') {
                        if(subCmd == null) 
                            throw new ParseException(") with no (", i);
                        ret.add(curApp);
                        curApp = ret;
                        subCmd = null;
                    } else if(chars[i] == '(') {
                        if(subCmd != null) 
                            throw new ParseException("nested paranthesis detected", i);
                        subCmd = new ArrayList();
                        curApp = subCmd;
                    }
                }
            }
            i++;
        }
        if(quote != null)
            throw new ParseException("Missing end-quote", i);
        if(subCmd != null)
            throw new ParseException("Missing end )", i);
        return ret;
    }

    String promptArg(String prompt, boolean addHist) throws IOException {
        if(jbofh.guiEnabled) {
            return jbofh.mainFrame.promptArg(prompt, addHist);
        }
        while (true) {
            // A readline thingy where methods were non-static would have helped a lot.
            terminatorTask.startWaiting();
            consolereader.setUseHistory(addHist);
            String ret = consolereader.readLine(prompt);
            terminatorTask.stopWaiting();
            return ret;
        }
    }
    
    ArrayList getSplittedCommand() throws IOException, ParseException {
        String line = promptArg((String)jbofh.props.get("console_prompt"), true);
        if (line == null)
            return null;
        return splitCommand(line);
    }

    /**
     *
     * @param c
     */
    public void setCompleter(jline.Completor c) {
        if (!jbofh.guiEnabled)  {
            consolereader.addCompletor(c);
        }
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        org.apache.log4j.PropertyConfigurator.configure("log4j.properties");
        String tests[] = {
            "dette er en test",
            "en 'noe annerledes' test",
            "en (parantes test 'med quote' test) hest",
            "mer(test hei)du morn ",
            "en liten (test av) dette) her",
            "mer (enn du(skulle tro))",
            "test empty \"\" quote"
        };
        CommandLine cLine;
        cLine = new CommandLine(Logger.getLogger(CommandLine.class),
                                                                    null, 0, 0);
        for (String test : tests) {
            System.out.println("split: --------" + test + "-----------");
            try {
                ArrayList v = cLine.splitCommand(test);
                for(int i = 0; i < v.size(); i++)
                    System.out.println(i+": '"+v.get(i)+"'");
            }catch (ParseException ex) {
                System.out.println("got: "+ex);
            }
        }       
    }
}

// arch-tag: 22043997-ab83-4c15-ad46-f00498d03860
