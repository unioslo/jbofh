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
 * JBofhFrame.java
 *
 * Created on June 4, 2003, 11:23 AM
 */

package no.uio.jbofh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.KeyboardFocusManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.Keymap;

/**
 *
 * @author  runefro
 */
public final class JBofhFrameImpl implements ActionListener, JBofhFrame {
    JTextArea tfOutput;
    private JTextField tfCmdLine;
    JLabel lbPrompt;
    JFrame jframe;
    private boolean isBlocking = false;
    private boolean wasEsc = false;
    JBofh jbofh;
    ArrayList cmdLineHistory = new ArrayList();
    int historyLocation;
    JPopupMenu tfPopup;
    HashMap menuItems = new HashMap();

    class MyKeyAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public MyKeyAction(String name) {
            super(name);
        }
        
        public void actionPerformed(ActionEvent e) {
            if("esc".equals(getValue(Action.NAME))) {
                wasEsc = true;
                releaseLock();
            } else if("clearline".equals(getValue(Action.NAME))) {
                tfCmdLine.setText("");
            } else if("tab".equals(getValue(Action.NAME))) {
                ArrayList completions = new ArrayList();
                StringBuilder suggestions = new StringBuilder();
                int nlines = 1;
                jbofh.bcompleter.complete(
                        getCmdLineText(), 
                        tfCmdLine.getCaret().getDot(), 
                        completions);
                switch (completions.size()) {
                    case 1:
                        String str = getCmdLineText();
                        int loc = str.lastIndexOf(" ");
                        tfCmdLine.setText(str.substring(0, loc+1)+completions.get(0)+" ");
                        break;
                // No completions, do nothing (beeps are anoying)
                    case 0:
                        break;
                    default:
                        // Complete as much as possible
                        String common=""+completions.get(0);
                        for(int j = 1; j < completions.size(); j++) {
                            String tmp = (String) completions.get(j);
                            int minLen = Math.min(tmp.length(), common.length());
                            
                            for(int n = 0; n < minLen; n++) {
                                if(tmp.charAt(n) != common.charAt(n)) {
                                    //System.out.println(n+" "+tmp+" "+common);
                                    common = common.substring(0, n);
                                    break;
                                }
                            }
                        }   if(common.length() > 0) {
                            String strng = getCmdLineText();
                            int lc = strng.lastIndexOf(" ");
                            tfCmdLine.setText(strng.substring(0, lc+1)+common);
                        }   for (Object o: completions) {
                            if (suggestions.length() > 80*nlines) {
                                suggestions.append("\n");
                                ++nlines;
                            } else if (suggestions.length() > 0) {
                                suggestions.append(' ');
                            }
                            suggestions.append((String)o);
                        }   showMessage(suggestions.toString() + "\n", true);
                        break;
                }
            } else if("up".equals(getValue(Action.NAME))) {
                if(historyLocation > 0 && cmdLineHistory.size() > 0) {
                    tfCmdLine.setText(""+cmdLineHistory.get(--historyLocation));
                }
            } else if("down".equals(getValue(Action.NAME))) {
                if(historyLocation < cmdLineHistory.size()-1) {
                    tfCmdLine.setText(""+cmdLineHistory.get(++historyLocation));
                } else if (historyLocation == cmdLineHistory.size()-1){
                    ++historyLocation;
                    tfCmdLine.setText("");
                }
            }
        }
    }

    class PopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                if(e.getComponent() instanceof JTextArea) {
                    tfPopup.show(e.getComponent(), e.getX(), e.getY());                    
                }
            }
        }
    }

    /**
     *
     * @param jbofh
     */
    public JBofhFrameImpl(JBofh jbofh) {
        this.jbofh = jbofh;
        makeGUI();
    }
    @SuppressWarnings("unchecked")
    void makeGUI() {
        JPanel np = new JPanel();
        JScrollPane sp = new JScrollPane(tfOutput = new JTextArea());
        jframe = new JFrame("JBofh");
        tfOutput.setEditable(false);
        tfOutput.setFont(new Font(""+jbofh.props.get("gui.font.name.outputwindow"),
                             Font.PLAIN, Integer.parseInt(""+jbofh.props.get("gui.font.size.outputwindow"))));
        tfOutput.setBackground(Color.BLACK);
        tfOutput.setForeground(Color.WHITE);
        np.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        np.add(lbPrompt = new JLabel(), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        np.add(tfCmdLine = new JTextField(), gbc);
        tfCmdLine.addActionListener(this);
        jframe.getContentPane().add(sp, BorderLayout.CENTER);
        jframe.getContentPane().add(np, BorderLayout.SOUTH);
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
          .addKeyEventDispatcher((KeyEvent e) -> {
              tfCmdLine.requestFocus();
              return false;
        });

        // We want control over some keys used on tfCmdLine
        tfCmdLine.setFocusTraversalKeysEnabled(false);
        Keymap keymap = JTextField.addKeymap("MyBindings", tfCmdLine.getKeymap());
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), 
            new MyKeyAction("tab"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), 
            new MyKeyAction("down"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), 
            new MyKeyAction("up"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 
            new MyKeyAction("esc"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_U, 
                                         java.awt.event.InputEvent.CTRL_MASK), 
            new MyKeyAction("clearline"));
        tfCmdLine.setKeymap(keymap);

        String popups[] = {"Clear screen", "clear_screen"};
        tfPopup = new JPopupMenu();
        for(int i = 0; i < popups.length; i+= 2) {
            JMenuItem menuItem = new JMenuItem(popups[i]);
            menuItems.put(menuItem, popups[i+1]);
            menuItem.addActionListener(this);
            tfPopup.add(menuItem);
        }
        MouseListener popupListener = new PopupListener();
        tfOutput.addMouseListener(popupListener);

        jframe.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        jframe.pack();
        jframe.setSize(new Dimension(700,400));
        jframe.setVisible(true);
    }

    /**
     *
     * @return
     */
    public String getCmdLineText(){
        tfCmdLine.requestFocusInWindow();
        return tfCmdLine.getText();
    }

    /**
     *
     * @param on
     */
    public void showWait(boolean on) {
        if(on) {
            lbPrompt.setText("Wait");
            tfCmdLine.setEditable(false);
        } else {
            lbPrompt.setText((String) jbofh.props.get("console_prompt"));
            tfCmdLine.setEditable(true);
        }
    }

    /**
     *
     * @return
     */
    public boolean confirmExit() {
        return JOptionPane.OK_OPTION == 
                JOptionPane.showConfirmDialog(jframe, "Really exit?",
                                "Please confirm", JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE);
    }

    /**
     *
     * @param msg
     * @param crlf
     */
    public void showMessage(String msg, boolean crlf) {
        tfOutput.append(msg);
        if(crlf) tfOutput.append("\n");
        tfOutput.setCaretPosition(tfOutput.getText().length());
    }

    /**
     *
     * @param prompt
     * @param addHist
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public String promptArg(String prompt, boolean addHist) throws IOException {
        /* We lock the current thread so that execution does not
         * continue until JBofh.actionPerformed releases the
         * lock  */
        lbPrompt.setText(prompt);
        tfCmdLine.requestFocusInWindow();
        synchronized (tfCmdLine.getTreeLock()) {
            while (true) {
                try {
                    isBlocking = true;
                    tfCmdLine.getTreeLock().wait();  // Lock
                    if(wasEsc){
                        wasEsc = false;
                        throw new IOException("escape hit");
                    }
                    String text = getCmdLineText();
                    if(addHist){
                        if((text.length() > 0) &&
                            ! (cmdLineHistory.size() > 0 &&
                                text.equals(cmdLineHistory.get(cmdLineHistory.size()-1)))) {
                            cmdLineHistory.add(text);
                        }
                        historyLocation = cmdLineHistory.size();
                    }
                    showMessage("\n" + prompt + text, true);
                    tfCmdLine.setText("");
                    // If we don't set the caret position, requestFocus failes
                    tfCmdLine.setCaretPosition(0);
                    return text;
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
    }

    /**
     *
     * @param evt
     */
    public void actionPerformed(ActionEvent evt) {
        if(evt.getSource() == tfCmdLine) {
            releaseLock();
        } else {
            String action = (String) menuItems.get(evt.getSource());
            if(action.equals("clear_screen")) {
                tfOutput.setText("");
            }
        }
    }

    /**
     * Finished, release it.
     */
    protected void releaseLock() {
        if(! isBlocking) return;
        synchronized (tfCmdLine.getTreeLock()) {
            isBlocking = false;
            EventQueue.invokeLater(() -> {
            });
            tfCmdLine.getTreeLock().notifyAll();
        }
    }

    /**
     *
     * @param prompt
     * @param parent
     * @return
     * @throws MethodFailedException
     */
    public String getPasswordByJDialog(String prompt, Frame parent) 
        throws MethodFailedException {
        try {
            JPasswordField pf = new JPasswordField(10){
              public void addNotify() {
                super.addNotify();
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                   .addKeyEventDispatcher((KeyEvent e) -> {
                this.requestFocusInWindow();
                return false;
                });
              }
            };
            JOptionPane pane = new JOptionPane(pf, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = pane.createDialog(parent, prompt);
            dialog.setLocationRelativeTo(tfOutput);
            dialog.setVisible(true);
            pf.addNotify();
            dialog.dispose();
            Object value = pane.getValue();
            try {
                if(((Integer) value) == JOptionPane.OK_OPTION) 
                    return new String(pf.getPassword());
                return null;
            } catch (java.lang.NullPointerException e) {
                    System.exit(0);
                    throw new NullPointerException("/*Nothing to do, exit*/!");
            }
        } catch (java.lang.NoClassDefFoundError e) {
            throw new MethodFailedException();
            }
    }

    /**
     *
     * @param title
     * @param msg
     */
    public void showErrorMessageDialog(String title, String msg) {
        JOptionPane.showMessageDialog(null, msg, title,
            JOptionPane.ERROR_MESSAGE);
    }
}

// arch-tag: f9ab9f2a-5606-4a77-9fba-4ec72459b18b
