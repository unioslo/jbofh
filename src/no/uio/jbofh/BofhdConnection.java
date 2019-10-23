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
 * BofdConnection.java
 *
 * Created on November 19, 2002, 11:48 AM
 */

package no.uio.jbofh;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.log4j.Category;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author  runefro
 */
public class BofhdConnection {
    Category logger;
    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    XmlRpcClient xmlrpc = new XmlRpcClient();
    String sessid;
    HashMap commands;
    JBofh jbofh;

    /** Creates a new instance of BofdConnection
     * @param log
     * @param jbofh */
    public BofhdConnection(Category log, JBofh jbofh) {
        this.logger = log;
        this.jbofh = jbofh;
        // The xmlrpc-1.1 driver doesn't handle character encoding correctly
        System.setProperty("sax.driver", "com.jclark.xml.sax.Driver");
    }

    void connect(String host_url, boolean use_int_trust, String cafile)
                                                      throws KeyStoreException,
                  NoSuchAlgorithmException, MalformedURLException, IOException {
        //XmlRpc.setDebug(true);
        /*
          The SecurityTool overrides the default key_store and
          trust_store.  The latter is used by the client to validate
          the server certificate.
        */
        /*  This works, but requiers server-cert on local filesystem:
            SecurityTool st = new SecurityTool();
            st.setTrustStore("jbofh.truststore");
            try {
            st.setup();
            } catch (Exception e) {
            System.out.println("Error setting SecurityTool: "+e);
            e.printStackTrace();
            }
        */
        if(host_url.startsWith("https:")) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL");  // TLS?
                if (use_int_trust) {
                        InternalTrustManager tm =
                                               new InternalTrustManager(cafile);
                        TrustManager[] tma = {tm};
                        sc.init(null,tma, null);
                } else {
                        TrustManagerFactory trustManagerFactory = 
                                TrustManagerFactory.getInstance(
                                     TrustManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init((KeyStore)null);
                        TrustManager[] tma =
                                         trustManagerFactory.getTrustManagers();
                        sc.init(null,tma, null);
                }
                SSLSocketFactory sf1 = sc.getSocketFactory();
                HttpsURLConnection.setDefaultSSLSocketFactory(sf1);
                URL url = new URL(host_url);
                HttpsURLConnection con =
                                       (HttpsURLConnection)url.openConnection();
                con.connect();
                con.disconnect();
            } catch (IOException | CertificateException |
                    NoSuchAlgorithmException | KeyManagementException e) {
                System.out.println("Error setting up SSL cert handling: "+e);
                System.exit(0);
            }
        }
        try {
            config.setServerURL(new URL(host_url));
            xmlrpc.setConfig(config);
            // XmlRpc.setDebug(true);
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage() + " Most probably Bad url '"
                    + host_url + "',"
                    + "check your property file");
            System.exit(0);
        }
    }
    @SuppressWarnings("unchecked")
    String login(String uname, String password) throws BofhdException {
        ArrayList args = new ArrayList();
        args.add(0, uname);
        args.add(1, password);
        String newsessid = (String) sendRawCommand("login", args, -1);
        logger.debug("Login ret: "+newsessid);
        this.sessid = newsessid;
        return newsessid;
    }
    @SuppressWarnings("unchecked")
    String getMotd(String version) throws BofhdException {
        ArrayList args = new ArrayList();
        args.add("jbofh");
        args.add(version);
        String msg = (String) sendRawCommand("get_motd", args, -1);
        return msg;
    }
    @SuppressWarnings("unchecked")
    String logout() throws BofhdException {
        ArrayList args = new ArrayList();
        args.add(sessid);
        return (String) sendRawCommand("logout", args, 0);
    }
    @SuppressWarnings("unchecked")
    void updateCommands() throws BofhdException {
        ArrayList args = new ArrayList();
        args.add(sessid);
        commands = (HashMap) sendRawCommand("get_commands", args, 0);
    }
    @SuppressWarnings("unchecked")
    String getHelp(ArrayList args) throws BofhdException {
        args.add(0, sessid);
        return (String) sendRawCommand("help", args, 0);
    }
    @SuppressWarnings("unchecked")
    Object sendCommand(String cmd, ArrayList args) throws BofhdException {
        args.add(0, sessid);
        args.add(1, cmd);
        return sendRawCommand("run_command", args, 0);
    }

    private String washSingleObject(String str) {
        if(str.startsWith(":")) {
            str = str.substring(1);
            if(str.equals("None")) return "<not set>";
            if(! (str.substring(0,1).equals(":"))) {
                System.err.println("Warning: unknown escape sequence: "+str);
            }
        }
        return str;
    }

    /**
     * We have extended XML-rpc by allowing NULL values to be
     * returned.  <code>washResponse</code> handles this.
     *
     * @param o the object to wash
     * @return the washed object
     */
    @SuppressWarnings("unchecked")
    Object washResponse(Object o) {
        if(o instanceof ArrayList || o instanceof Object[]) {
            ArrayList ret = new ArrayList();
            Object[] o_arr = (Object[]) o;
            ArrayList o_arr_list = new ArrayList(Arrays.asList(o_arr));
            for (Iterator e = o_arr_list.iterator() ; e.hasNext() ;) {
                ret.add(washResponse(e.next()));
            }
            return ret;
        } else if(o instanceof String) {
            return washSingleObject((String) o);
        } else if(o instanceof Integer) {
            return o;
        } else if(o instanceof HashMap) {
            HashMap ret = new HashMap();
            for (Iterator e =
                    (((HashMap) o).keySet().iterator());
                    e.hasNext(); ) {
                Object key = e.next();
                ret.put(key, washResponse(((HashMap) o).get(key)));
            }
            return ret;
        } else {
            return o;
        }
    }

    Object sendRawCommand(String cmd, ArrayList args, int sessid_loc)
                                                        throws BofhdException {
        return sendRawCommand(cmd, args, false, sessid_loc);
    }

    private void checkSafeString(String s) throws BofhdException {
        /* http://www.w3.org/TR/2004/REC-xml-20040204/#NT-Char
           #x9 | #xA | #xD | [#x20-#xD7FF] */
        char chars[] = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int c = chars[i];
            if (c >= 0x20){ continue; }
            if (c == 0x9 || c == 0xa || c == 0xd){ continue; }
            throw new BofhdException("You entered an illegal charcter:"+c);
        }
    }
    /**
     * Handle bofhds extensions to XML-RPC by pre-prosessing the
     * arguments.  Since we only send String (or ArrayList with
     * String), we don't support the other extensions.
     * (Not sure how much this is needed since all arguments er strongly typed)
     *
     * @param args a <code>ArrayList</code> representing the arguments
     */
    @SuppressWarnings("unchecked")
    void washCommandArgs(ArrayList args) throws BofhdException {
        for (int i = args.size()-1; i >= 0; i--) {
            Object tmp = args.get(i);
            if (tmp instanceof String) {
                if (((String) tmp).length() > 0 && ((String) tmp).charAt(0) == ':') {
                    tmp = ":"+((String) tmp);
                    args.set(i, tmp);
                }
                checkSafeString((String) tmp);
            } else if (tmp instanceof ArrayList) {
                ArrayList v = (ArrayList) tmp;
                for (int j = v.size()-1; j >= 0; j--) {
                    tmp = v.get(j);
                    if ((tmp instanceof String) && (((String) tmp).charAt(0) == ':')) {
                        tmp = ":"+((String) tmp);
                        v.set(j, tmp);
                    }
                    checkSafeString((String) tmp);
                }
            }
        }
    }

    /**
     * Sends a raw command to the server.
     *
     * @param cmd a <code>String</code> with the name of the command
     * @param args a <code>ArrayList</code> of arguments
     * @param sessid_loc an <code>int</code> the location of the
     * sessionid.  Needed if the command triggers a re-authentication
     * @return an XML-rpc <code>Object</code>
     * @exception BofhdException if an error occurs
     */
    @SuppressWarnings("unchecked")
    Object sendRawCommand(String cmd, ArrayList args, boolean gotRestart,
        int sessid_loc) throws BofhdException {
        try {
            switch (cmd) {
                case "login":
                    logger.debug("sendCommand("+cmd+", ********");
                    break;
                case "run_command":
                ArrayList cmdDef = (ArrayList) commands.get(args.get(1));
                if(cmdDef.size() == 1 ||
                                     (! (cmdDef.get(1) instanceof ArrayList))) {
                    logger.debug("sendCommand("+cmd+", "+args);
                    } else {
                    ArrayList protoArgs = (ArrayList) cmdDef.get(1);
                    ArrayList logArgs = new ArrayList();
                    logArgs.add(args.get(0));
                    logArgs.add(args.get(1));
                    int i = 2;
                    for (Iterator e = protoArgs.iterator() ; e.hasNext() ;) {
                        if(i >= args.size()) break;
                        HashMap h = (HashMap) e.next();
                            String type = (String) h.get("type");
                            if (type != null && type.equals("accountPassword")) {
                                logArgs.add("********");
                            } else {
                                logArgs.add(args.get(i));
                            }
                            i++;
                        }
                        logger.debug("sendCommand("+cmd+", "+logArgs);
                    }   break;
                default:
                    logger.debug("sendCommand("+cmd+", "+args);
                    break;
            }
            washCommandArgs(args);
            Object r = washResponse(xmlrpc.execute(cmd, args));
            logger.debug("<-"+r);
            return r;
        } catch (XmlRpcException e) {
            logger.debug("exception-message: "+e.getMessage());
            String match = "Cerebrum.modules.bofhd.errors.";
            if(! gotRestart && 
                e.getMessage().startsWith(match+"ServerRestartedError")) {
                jbofh.initCommands();
                return sendRawCommand(cmd, args, true, sessid_loc);
            } else if (e.getMessage().startsWith(match+"SessionExpiredError")) {
                jbofh.showMessage("Session expired, you must re-authenticate", true);
                jbofh.login(jbofh.uname, null);
                if (sessid_loc != -1) args.set(sessid_loc, sessid);
                return sendRawCommand(cmd, args, true, sessid_loc);
            } else if(e.getMessage().startsWith(match)) {
                String msg = e.getMessage().substring(e.getMessage().indexOf(":")+1);
                if(msg.startsWith("CerebrumError: ")) msg = msg.substring(msg.indexOf(":")+2);
                throw new BofhdException("Error: "+msg);
            } else {
                logger.debug("err: code="+e.code, e);
                throw new BofhdException("Error: "+e.getMessage());
            }
        }
    }

}

// arch-tag: e689905d-cdab-4978-9ea4-28e1647b512e
