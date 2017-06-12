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
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.sun.java.text.PrintfFormat;

/**
 * Main class for the JBofh program.
 *
 * @author  runefro
 */
public final class JBofh {
    Properties props;
    CommandLine cLine;
    BofhdConnection bc;
    static Logger logger = Logger.getLogger(JBofh.class);
    BofhdCompleter bcompleter;
    HashMap knownFormats;
    String version = "unknown";
    boolean guiEnabled, hideRepeatedHeaders;
    JBofhFrame mainFrame;
    String uname;
    private FileWriter script_file;

    /** Creates a new instance of JBofh
     * @param gui
     * @param log4jPropertyFile
     * @param bofhd_url
     * @param propsOverride
     * @param cafile
     * @throws no.uio.jbofh.BofhdException
     */
    @SuppressWarnings("unchecked")
    public JBofh(boolean gui, String log4jPropertyFile, String bofhd_url,
        HashMap propsOverride, String cafile) throws BofhdException {
        guiEnabled = gui;
        loadPropertyFiles(log4jPropertyFile);
        for (Iterator e = (propsOverride.keySet().iterator()) ;
                e.hasNext() ;) {
            String k = (String) e.next();
            String v = (String) propsOverride.get(k);
            props.put(k, v);
        }
        if(gui){
            // Load class with reflection to prevent javac from trying
            // to compile JBofhFrameImpl which requires swing.
            try {
                Class c = Class.forName("no.uio.jbofh.JBofhFrameImpl");
                Object[] args = new Object[] { this };
                Class[] cargs = new Class[] { this.getClass() };
                java.lang.reflect.Constructor constr = c.getConstructor(cargs);
                mainFrame = (JBofhFrame) constr.newInstance(args);
            } catch (ClassNotFoundException | NoSuchMethodException |
                        IllegalAccessException | InstantiationException |
                        java.lang.reflect.InvocationTargetException e) {
                System.out.println(e);
            }
            //mainFrame = (JBofhFrame) new JBofhFrameImpl(this);
        }

        bc = new BofhdConnection(logger, this);
        String intTrust = (String) props.get("InternalTrustManager.enable");
        if(bofhd_url == null) bofhd_url = (String) props.get("bofhd_url");
        showMessage("Bofhd server is at "+bofhd_url, true);
        bc.connect(bofhd_url, (intTrust != null && intTrust.equals("true")),
                    cafile);
        String intHide =  (String) props.get("HideRepeatedReponseHeaders");
        hideRepeatedHeaders = (intHide != null && intHide.equals("true")
                );

        int idleWarnDelay = 0;
        int idleTerminateDelay = 0;
        try {
            String tmp;
            tmp = System.getProperty("JBOFH_IDLE_WARN_DELAY", null);
            if (tmp == null) tmp = (String) props.get("IdleWarnDelay");
            if (tmp != null) idleWarnDelay = Integer.parseInt(tmp);
            tmp = (String) props.get("IdleTerminateDelay");
            if (tmp != null) idleTerminateDelay = Integer.parseInt(tmp);
        } catch (NumberFormatException ex) {
            showMessage("Configure error, Idle*Delay must be a number", true);
            System.exit(1);
        }
        cLine = new CommandLine(logger, this, idleWarnDelay, idleTerminateDelay);
        readVersion();
    }

    /**
     *
     * @param uname
     * @param password
     * @throws BofhdException
     */
    public void initialLogin(String uname, String password) 
        throws BofhdException {
        if(! login(uname, password)) {
            System.exit(0);
        }
        String msg = bc.getMotd(version);
        if(msg.length() > 0)
            showMessage(msg, true);

        initCommands();
        showMessage("Welcome to jbofh, v "+version+", type \"help\" for help", true);
    }
    
    /**
     *
     * @param fname
     */
    protected void loadPropertyFiles(String fname) {
        try {
            URL url = ResourceLocator.getResource(this, fname);
            if(url == null) throw new IOException();
            props = new Properties();
            props.load(url.openStream());
            PropertyConfigurator.configure(props);
            props = new Properties();
            url = ResourceLocator.getResource(this, "/jbofh.properties");
            props.load(url.openStream());
        } catch(IOException e) {
            showMessage("Error reading property files", true);
            System.exit(1);
        }
    }

    private void readVersion() {
        version = "Error reading version file";
        URL url = ResourceLocator.getResource(this, "/version.txt");
        if(url != null) {
            try {
                try (BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(url.openStream()))) {
                    version = br.readLine();
                }
            } catch (IOException e) {}  // ignore failure
        }
    }

    /**
     *
     * @param uname
     * @param password
     * @return
     * @throws BofhdException
     */
    public boolean login(String uname, String password) throws BofhdException {
        try {
            while(uname == null) {
                uname = cLine.promptArg("Username: ", false);
            }

            if(password == null) {
                ConsolePassword cp = new ConsolePassword(mainFrame);
                String prompt = "Password for "+uname+":";
                if(guiEnabled) {
                    try {
                        password = cp.getPasswordByJDialog(prompt,
                                                          JBofhFrame.AWT_frame);
                    } catch (MethodFailedException e) {
                        return false;
                    }
                } else {
                    //password = cp.getPassword(prompt);
                    password = cLine.consolereader.readLine(prompt, '*');
                }
            }
            if(password == null) return false;
            bc.login(uname, password);
            this.uname = uname;
        } catch (IOException io) {
            return false;
        }
        return true;
    }
    @SuppressWarnings("unchecked")
    void initCommands() throws BofhdException {
        bc.updateCommands();
        bcompleter = new BofhdCompleter(this, logger);
        for (Iterator e = (bc.commands.keySet().iterator()); e.hasNext(); ) {
            String protoCmd = (String) e.next();
            ArrayList cmd =
                    (ArrayList) ((ArrayList) bc.commands.get(protoCmd)).get(0);
            bcompleter.addCompletion(cmd, protoCmd);
        }
        ArrayList v = new ArrayList();
        v.add("help");
        bcompleter.addCompletion(v, "");
        v.set(0, "source");
        bcompleter.addCompletion(v, "");
        v.set(0, "script");
        bcompleter.addCompletion(v, "");
        /* We don't want completion for quit
           v.set(0, new String("quit"));
           bcompleter.addCompletion(v, ""); */
        cLine.setCompleter(bcompleter);

        knownFormats = new HashMap();
    }

    void showMessage(String msg, boolean crlf) {
        if (script_file != null) {
            try {
                script_file.write(msg);
                script_file.flush();
                if(crlf) script_file.write("\n");
            } catch (IOException e) {
                try {
                    script_file.close();
                } catch (IOException e1) { }
                showMessage("Error writing to script-file: "+e.getMessage(), true);
            }
        }
        if(guiEnabled) {
            mainFrame.showMessage(msg, crlf);
        } else {
            if(crlf) {
                System.out.println(msg);
            } else {
                System.out.print(msg);
            }
        }
    }

    private boolean processCmdLine() {
        ArrayList args;
        try {
            bcompleter.setEnabled(true);
            args = cLine.getSplittedCommand();
            if (args == null) {
                return guiEnabled && (! mainFrame.confirmExit());
            }
        } catch (IOException io) {
            return guiEnabled && (! mainFrame.confirmExit());
        } catch (ParseException pe) {
            showMessage("Error parsing command: "+pe, true);
            return true;
        }
        if(args.isEmpty()) return true;
        try {
            if(script_file != null) {
                StringBuilder sb = new StringBuilder();
                for(Iterator iterator = args.iterator(); iterator.hasNext();) {
                    sb.append(iterator.next());
                    if(iterator.hasNext()) sb.append(" ");
                }
                try {
                    script_file.write(((String) props.get("console_prompt"))+sb.toString()+"\n");
                } catch (IOException e) {
                }
            }
            runCommand(args, false);
        } catch (BofhdException be) {
            showMessage(be.getMessage(), true);
        }
        return true;
    }

        private boolean handleNativeComands(ArrayList args)
                                                        throws BofhdException {
        switch ((String) args.get(0)) {
            case "commands":
                // Neat while debugging
                for (Iterator e = bc.commands.keySet().iterator(); e.hasNext();) {
                    Object key = e.next();
                    showMessage(key+" -> "+ bc.commands.get(key), true); 
                }
                break;
            case "quit":
                bye();
                break;
            case "script":
                if(args.size() == 1) {
                    if(script_file == null) { 
                        throw new BofhdException("Must specify output filename");
                    } else {
                        try {
                            script_file.close();
                            script_file = null;
                        } catch (IOException e) {
                            throw new BofhdException("Error closing file: "+e.getMessage());
                        }
                        showMessage("Script file closed", true);
                    }
                } else {
                    if(script_file != null)
                        throw new BofhdException("Scriptfile already active");
                    try {
                        script_file = new FileWriter((String) args.get(1));
                    } catch (IOException e) {
                        throw new BofhdException("Error creating file: "+e.getMessage());
                    }
                    showMessage("Script file started.  Run script with no args to close file", true);
                }
                break;
            case "source":
                if(args.size() == 1) {
                    showMessage("Must specify filename to source", true);
                } else {
                    boolean stop_on_error = true;
                    // --ignore-errors  from GNU Coding Standards
                    String fname = (String) args.get(1);
                    if(args.size() == 3) {
                        if("--ignore-errors".equals(args.get(1)))
                            stop_on_error = false;
                        fname = (String) args.get(2);
                    }
                    sourceFile(fname, stop_on_error);
                }
                break;
            case "help":
                args.remove(0);
                showMessage(bc.getHelp(args), true);
                break;
            default:
                return false;
        }
            return true;
        }
        @SuppressWarnings("unchecked")
        private void runCommand(ArrayList args, boolean sourcing) 
            throws BofhdException {
            if (handleNativeComands(args)) return;
            String protoCmd;
            ArrayList protoArgs;
            try {
                ArrayList lst = bcompleter.analyzeCommand(args, -1);
                protoCmd = (String) lst.get(lst.size() - 1);
                protoArgs = new ArrayList(
                    args.subList(lst.size()-1, args.size()));
            } catch (AnalyzeCommandException e) {
                if (sourcing) throw new BofhdException(e.getMessage());
                showMessage(e.getMessage(), true); return;
            }
            if(! sourcing) {
                protoArgs = checkArgs(protoCmd, protoArgs);
                if(protoArgs == null) return;
            }
            try {
                boolean multiple_cmds = false;
                for (Iterator e = protoArgs.iterator() ; e.hasNext() ;)
                    if(e.next() instanceof ArrayList)
                        multiple_cmds = true;
                if(guiEnabled && ! sourcing) mainFrame.showWait(true);
                Object resp = bc.sendCommand(protoCmd, protoArgs);
                if(resp != null) showResponse(protoCmd, resp, multiple_cmds, true);
            } catch (BofhdException ex) {
                if(sourcing) throw ex;
                showMessage(ex.getMessage(), true);
            } catch (Exception ex) {
                showMessage("Unexpected error (bug): "+ex, true);
                ex.printStackTrace(System.out);
            } finally {
                if(guiEnabled && ! sourcing) mainFrame.showWait(false);
            }
        }

        void enterLoop() {
            boolean keepLooping = true;
            while(keepLooping) {
                try {
                    keepLooping = processCmdLine();
                } catch (Exception ex) {
                    showMessage("Unexpected error (bug): "+ex, true);
                    ex.printStackTrace(System.out);
                }
            }
            bye();
        }

        void sourceFile(String filename, boolean stop_on_error) {
            if(guiEnabled) mainFrame.showWait(true);
            try {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filename)));
                String sin;
                int lineno = 0;
                ArrayList args;
                while((sin = in.readLine()) != null) {
                    lineno++;
                    sin = sin.trim();
                    if(sin.startsWith("#") ||  sin.length() == 0)
                        continue;
                    try {
                        args = cLine.splitCommand(sin);
                    } catch (ParseException ex) {
                        showMessage("Error parsing command ("+sin+")", true); 
                        if (stop_on_error) break;
                        continue;
                    }
                    showMessage(((String) props.get("console_prompt"))+sin, true);
                    try {
                        runCommand(args, true);
                    } catch (BofhdException be) {
                        showMessage(be.getMessage(), true);
                        if (stop_on_error) {
                            showMessage("Sourcing of "+filename+" aborted on line "+lineno, true);
                            showMessage("Hint: Use 'source --ignore-errors fname' to ignore errors", true);
                            break;                          
                        }
                    }
                }
            } catch (IOException io) {
                showMessage("Error reading file: "+io.getMessage(), true);
            }
            if(guiEnabled) mainFrame.showWait(false);
        }

        void bye() {
            showMessage(props.getProperty("exit_message"), true);
            try {
                bc.logout();
            } catch (BofhdException ex) { } // Ignore
            System.exit(0);
        }

       private boolean opt2bool(Object opt) {
           return (opt instanceof Boolean && ((Boolean) opt)) ||
                   (opt instanceof Integer && ((Integer) opt) == 1);
       }
        @SuppressWarnings("unchecked")
        ArrayList checkArgs(String cmd, ArrayList args) throws BofhdException {
            ArrayList ret = (ArrayList) args.clone();
            ArrayList cmd_def = (ArrayList) bc.commands.get(cmd);
            boolean did_prompt = false;
            if(cmd_def.size() == 1) return ret;
            Object pspec = cmd_def.get(1);
            if (pspec instanceof String) {
                if(! "prompt_func".equals(pspec)) {
                    throw new BofhdException("Bad param spec");
                }
                return processServerCommandPromptFunction(cmd, ret);
            }
        for(int i = args.size(); i < ((ArrayList) pspec).size(); i++) {
            HashMap param = (HashMap) ((ArrayList) pspec).get(i);
            logger.debug("ps: "+i+" -> "+param);
            Object opt = param.get("optional");
            if(! did_prompt && opt2bool(opt))
                break;  // If we have prompted, remain in prompt-mode also for optional args
            Object tmp = param.get("default");
            String defval = null;
            if(tmp != null) {
            if(tmp instanceof String){
                defval = (String) tmp;
            } else {
                ret.add(0, bc.sessid);
                ret.add(1, cmd);
                defval = (String) bc.sendRawCommand("get_default_param", ret, 0);
                ret.remove(0);
                ret.remove(0);
            }
            }
            did_prompt = true;
            String prompt = (String) param.get("prompt");
            String type = (String) param.get("type");
            try {
            String s;
            if (type != null && type.equals("accountPassword")) {
                ConsolePassword cp = new ConsolePassword(mainFrame);

                if(guiEnabled) {
                try {
                    s = cp.getPasswordByJDialog(prompt + ">",
                                                        JBofhFrame.AWT_frame);
                } catch (MethodFailedException e) {
                    s = "";
                }
                } else {
                s = cLine.consolereader.readLine(prompt + ">", '*');
                }
            } else {
                bcompleter.setEnabled(false);
                s = cLine.promptArg(prompt+
					(defval == null ? "" : " ["+defval+"]")+" >", false);
            if (s == null)
                return null;
		}
		if(defval != null && s.equals("")) {
		    ret.add(defval);
		} else if(s.equals("?")) {
		    i--;
		    ArrayList v = new ArrayList();
		    v.add("arg_help");
		    v.add(param.get("help_ref"));
		    String help = (String) bc.getHelp(v);
		    showMessage(help, true);
		} else {
		    if (param.get("optional") != null && s.equals("")) {
			// Ignore optional arguments when left empty
		    } else {
			ret.add(s);
		    }
		}
	    } catch (IOException io) {
		return null;
	    }
	}
        return ret;
    }
    @SuppressWarnings("unchecked")
    ArrayList processServerCommandPromptFunction(String cmd, ArrayList ret) 
                                                        throws BofhdException {
        while(true) {
            ret.add(0, bc.sessid);
            ret.add(1, cmd);
            Object obj =  bc.sendRawCommand("call_prompt_func", ret, 0);
            if (! (obj instanceof HashMap))
                throw new BofhdException("Server bug: prompt_func returned " + obj);
            HashMap arginfo = (HashMap) obj;
            ret.remove(0);
            ret.remove(0);
            try {
                if(arginfo.get("prompt") == null && arginfo.get("last_arg") != null)
                    break;
                String defval = (String) arginfo.get("default");
                ArrayList map = (ArrayList) arginfo.get("map");
                if(map != null) {
                    for(int i = 0; i < map.size(); i++) {
                        ArrayList line = (ArrayList) map.get(i);
                        ArrayList description = (ArrayList) line.get(0);
                        String format_desc = (String) description.get(0);
                        description.remove(0);
                        if(i == 0) {
                            format_desc = "%4s " + format_desc;
                            description.add(0, "Num");
                        } else {
                            format_desc = "%4i " + format_desc;
                            description.add(0, i);
                        }
                        PrintfFormat pf = new PrintfFormat(format_desc);
                        showMessage(pf.sprintf(description.toArray()), true);
                    }
                }
                bcompleter.setEnabled(false);
                String s = cLine.promptArg((String) arginfo.get("prompt") +
                    (defval == null ? "" : " ["+defval+"]")+" >", false);
                if (s == null)
                    return null;
                if(s.equals("") && defval == null) continue;
                if(s.equals("?")) {
                    if(arginfo.get("help_ref") == null) {
                        showMessage("Sorry, no help available", true);
                        continue;
                    }
                    ArrayList v = new ArrayList();
                    v.add("arg_help");
                    v.add(arginfo.get("help_ref"));
                    String help = (String) bc.getHelp(v);
                    showMessage(help, true);
                    continue;
                }
                if(! s.equals("")) {
                    if(map != null && arginfo.get("raw") == null) {
                        try {
                            int i = Integer.parseInt(s);
                            if(i == 0) throw new Exception("");
                            ret.add(((ArrayList)map.get(i)).get(1));
                        } catch (Exception e) {
                            showMessage("Value not in list", true);
                        }
                    } else {
                        ret.add(s);
                    } 
                } else {
                    if(defval != null) ret.add(defval);
                }
                if(arginfo.get("last_arg") != null) break;
            } catch (IOException io) {
                return null;
            }
        }
        return ret;
    }
    @SuppressWarnings("unchecked")
    void showResponse(String cmd, Object resp, boolean multiple_cmds, 
        boolean show_hdr) throws BofhdException {
        if(multiple_cmds) {
            /* TBD: Should we try to provide some text indicating
             * which command each response belongs to?
             */
            boolean first = true;
            for (Iterator e = ((ArrayList) resp).iterator() ; e.hasNext() ;) {
                Object next_resp = e.next();
                showResponse(cmd, next_resp, false, first);
                if (e.hasNext()) {
                    /* This is intended to enhance the visibility under multiple
                     * commands and as wished under the previous in line
                     * commentary. The \t on the other hand is to
                     * avoid being filtered out by the overridden
                     * showMessage@JBofhFrameImpl for the GUI, a less elegant
                     *.but very efficient way until the TBD above is resolved.
                     */
                    showMessage("\n\t",true);
                }
                if(hideRepeatedHeaders)
                    first = false;
                }
                return;
            }
        if(resp instanceof String) {
            showMessage((String) resp, true);
            return;
        }
        ArrayList args = new ArrayList();
        args.add(cmd);
        HashMap format = (HashMap) knownFormats.get(cmd);
        if(format == null) {
            Object f = bc.sendRawCommand("get_format_suggestion", args, -1);
            if(f instanceof String && ((String)f).equals(""))
                f = null;
            if(f != null) {
                knownFormats.put(cmd, f);
                format = (HashMap) knownFormats.get(cmd);
            } else {
                throw new IllegalArgumentException("result was class: "+
                    resp.getClass().getName()+ ", no format suggestion exists");
            }
        }
        if(! (resp instanceof ArrayList) ){
            ArrayList tmp = new ArrayList(); // Force value as ArrayList
            tmp.add(resp);
            resp = tmp;
        }
        String hdr = (String) format.get("hdr");
        if(hdr != null && show_hdr) showMessage(hdr, true);
        for (Iterator ef =
                ((ArrayList) format.get("str_vars")).iterator() ;
                ef.hasNext() ;) {
            ArrayList format_info = (ArrayList) ef.next();
            String format_str = (String) format_info.get(0);
            ArrayList order = (ArrayList) format_info.get(1);
            String sub_hdr = null;
            if(format_info.size() == 3)
                sub_hdr = (String) format_info.get(2);
            for (Iterator e = ((ArrayList) resp).iterator() ;
                    e.hasNext() ;) {
                HashMap row = (HashMap) (e.next());
            if(! row.containsKey(order.get(0)))
                    continue;
            try {
                    PrintfFormat pf = new PrintfFormat(format_str);
                    if(sub_hdr != null) {
                        // This dataset has a sub-header, optionaly %s formatted
                        if(sub_hdr.contains("%")) {
                            pf = new PrintfFormat(sub_hdr);
                        } else {
                            showMessage(sub_hdr, true);
                        }
                        sub_hdr = null;
                    }

            Object a[] = new Object[order.size()];
            for(int i = 0; i < order.size(); i++) {
                        String tmp = (String) order.get(i);
                        if(tmp.contains(":")) {
                            StringTokenizer st = new StringTokenizer(tmp, ":");
                            tmp = st.nextToken();
                            String type = st.nextToken();
                            String formatinfo = "";
                            while (st.hasMoreTokens()) {
                                if(formatinfo.length() > 0)
                                    formatinfo += ":";
                                formatinfo += st.nextToken();
                            }
                            a[i] = row.get(tmp);
                            if (type.equals("date") && (! "<not set>".equals(a[i]))) {
                                SimpleDateFormat sdf = new SimpleDateFormat(formatinfo);
                                a[i] = sdf.format(a[i]);
                            }
                        } else {
                            a[i] = row.get(tmp);
                        }
                    }
                    showMessage(pf.sprintf(a), true);
                } catch (IllegalArgumentException ex) {
                    logger.error("Error formatting "+resp+"\n as: "+format, ex);
                    showMessage("An error occoured formatting the response, see log for details", true);
                }
         }
        }
    }

    static boolean isMSWindows() { 
        String os = System.getProperty("os.name"); 
        return os != null && os.startsWith("Windows"); 
    }
    /**
     * @param args the command line arguments
     */
    @SuppressWarnings({"unchecked", "null"})
    public static void main(String[] args) {
        boolean gui = JBofh.isMSWindows();
        String uname = System.getProperty("user.name");
        JBofh jb = null;
        try {
            String bofhd_url = null;
            String cafile = null;
            boolean test_login = false;
            String log4jPropertyFile = "/log4j_normal.properties";
            HashMap propsOverride = new HashMap();

            for(int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-q":
                        test_login = true;
                        break;
                    case "--url":
                        bofhd_url = args[++i];
                        break;
                    case "--gui":
                        gui = true;
                        break;
                    case "--nogui":
                        gui = false;
                        break;
                    case "-u":
                        uname = args[++i];
                        break;
                    case "--set":
                        String tmp = args[++i];
                        String allargs [] = tmp.split(",");
                        int j;
                        for (j=0; j < allargs.length; j++) {
                            propsOverride.put(
                               allargs[j].substring(0, allargs[j].indexOf('=')),
                               allargs[j].substring(allargs[j].indexOf('=')+1));
                        }
                        break;
                    case "-d":
                        log4jPropertyFile = "/log4j.properties";
                        break;
                    case "--ca":
                        cafile = args[++i];
                        break;
                    default:
                        System.out.println(
                            "Usage: ... [-q | --url url | --gui | --nogui | -d]\n"+
                                    "-q : internal use only\n"+
                                    "-u uname : connect as the given user\n"+
                                    "--url url : connect to alternate server\n"+
                                    "--gui : start with primitive java gui\n"+
                                    "--nogui : start in console mode\n"+
                                    "--set key=value: override settings in property file\n"+
                                    "--ca cafile : explicitly provide path and "
                                    + "file name of a CA certificate file\n" +
                                    "-d : enable debugging");
                        System.exit(1);
                }
            }
            jb = new JBofh(gui, log4jPropertyFile, bofhd_url, propsOverride,
                            cafile);
            if(test_login) {
                jb.initialLogin("bootstrap_account", "test");
                // "test" md5: $1$F9feZuRT$hNAtCcCIHry4HKgGkkkFF/
                // insert into account_authentication values((select entity_id from entity_name where entity_name='bootstrap_account'), (select code from authentication_code where code_str='MD5-crypt'), '$1$F9feZuRT$hNAtCcCIHry4HKgGkkkFF/');
            } else {
                jb.initialLogin(uname, null);
            }
            jb.enterLoop();
        } catch (BofhdException be) {
            String msg = "Caught error during init, terminating: \n"+ be.getMessage();
            if(gui) {
                System.out.println(msg);
                jb.mainFrame.showErrorMessageDialog("Fatal error", msg);
                System.exit(0);
            } else {
                System.out.println(msg);
                System.exit(0);
            }
        }
    }    
}

// arch-tag: d0af466d-fc21-44cf-9677-ce75a0a9e1ef
