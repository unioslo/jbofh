/*
 * Copyright 2002-2017 University of Oslo, Norway
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
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Keymap;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import com.camick.SmartScroller;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.regex.Pattern;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

/**
 *
 * @author  runefro, estephaz
 */
public final class JBofhFrameImpl extends KeyAdapter implements ActionListener,
                                JBofhFrame {
    //Beginning implementation of the JTextPane style and context variabels
    StyleContext context = new StyleContext();
    DefaultStyledDocument document = new DefaultStyledDocument(context);
    Style styleOri = context.addStyle("Base Style", null);
    Style styleNew = context.addStyle("Special Style",styleOri);
    Style styleAlt = context.addStyle("Alternative Style", styleOri);
    JTextPane tfOutput;
    //End implementation of the JTextPane style and context variabels.

    //Hit counters for keyboard key hits
    int kb, vkenter;

    JLabel lbPrompt;
    JFrame jframe;
    private boolean isBlocking = false;
    private boolean wasEsc = false;
    // Boolean variabels that are mainly toggled after conditions in runtime
    private boolean actionReady = false;
    private boolean shouldPopup = false;
    private boolean emptyEnter = false;
    private boolean exitingGUI = false;
    private boolean historyMatched = false;
    // Global variabel that decides on whether the ComboBox would be shown
    public boolean disableCombo;
    JBofh jbofh;
    ArrayList cmdLineHistory = new ArrayList();
    int historyLocation;
    JPopupMenu tfPopup;
    HashMap menuItems = new HashMap();
    // Declaration of the prompt line JComboBox specific variabels
    static String[] cmdLineStrngArr = {"",};
    JComboBox<String> combo = drawComboBox(cmdLineStrngArr);
    public JTextField tfCmdLine = 
        (JTextField) combo.getEditor().getEditorComponent();
    private final JComboBox<String> comboBox;
    String popupMenuMessage = "\n\t\t- POP-UP MENU BOX FOR COMMAND HISTORY" +
                  " ACTIVE -\nTo disable: " +
                  "click outside or validate (hit <┘) - " +
                  "\"Clear highlights\" right-mouse-click"
                  + " option clears those messages\n";
   /* Declaration of the search input dialog and highlights specific variabels.
    * The related Combo which is to be declared later as en entry field for that
    * dialog is designed to be less fancy than the previous one.
    */
    static String[] searchStrngArr = {"",};
    Highlighter hilit = new DefaultHighlighter();
    Highlighter.HighlightPainter painter;
    DefaultStyledDocument documentBackup = new DefaultStyledDocument(context);

    class MyKeyAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public MyKeyAction(String name) {
            super(name);
        }
        
        public void actionPerformed(ActionEvent e) {
            if("esc".equals(getValue(Action.NAME)) ||
            // Same action triggered by the same key combination Ctrl-D
            "Ctrl-D".equals(getValue(Action.NAME))) {
                wasEsc = true;
                releaseLock();
              /* Action for clearing the display from the mouse context menu or
                 like on a Linux terminal with the Ctrl-L key combination
               */
            } else if ("clearline".equals(getValue(Action.NAME))) {
                tfCmdLine.setText("");
            } else if ("Ctrl-L".equals(getValue(Action.NAME))) {
                tfOutput.setText("");
                try {
                // Reset the backup copy of the output screen as well
                    documentBackup.insertString(0, "", styleOri);
                } catch (BadLocationException ex) {
                    Logger.getLogger(JBofhFrameImpl.class.getName()).
                        log(Level.SEVERE, null, ex);
                }
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
                        tfCmdLine.setText(
                                 str.substring(0, loc+1)+completions.get(0)+"");
                        break;
                // No completions, do nothing (beeps are anoying)
                    case 0:
                        break;
                    default:
                        // Complete as much as possible
                        String common=""+completions.get(0);
                        for(int j = 1; j < completions.size(); j++) {
                            String tmp = (String) completions.get(j);
                            int minLen = Math.min(tmp.length(),
                                                               common.length());
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
                        }   showMessage("\n\n" + ((String) (getCmdLineText()))
                                        + "\n" + suggestions.toString()
                                                                  + "\n", true);
                        break;
                }
            } else if("up".equals(getValue(Action.NAME)) && disableCombo) {
                if(historyLocation > 0 && cmdLineHistory.size() > 0) {
                    tfCmdLine.setText(""+cmdLineHistory.get(--historyLocation));
            }
            } else if("down".equals(getValue(Action.NAME)) && disableCombo) {
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
                if(e.getComponent() instanceof JTextPane) {
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
        // Initializing the global static variabel disableCombo from config.
        this.disableCombo = Boolean.parseBoolean((String) jbofh.props.get(
                                                           "gui.disableCombo"));
        this.jbofh = jbofh;
        this.comboBox = combo;
        makeGUI();
    }
    @SuppressWarnings("unchecked")
    void makeGUI() {
        /* Try forcing the Java GUI not to look as coming from Mars when ordered
           through the global systemLookAndFeel parameter (Warning: this has its
           drawbacks unfortunately, look at the README for more details).
         */
        if (Boolean.parseBoolean(
            (String) jbofh.props.get("gui.systemLookAndFeel")) == true) {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException
                   | IllegalAccessException |
                UnsupportedLookAndFeelException ex) {
            }
        }
        JPanel np = new JPanel();
        JScrollPane sp = new JScrollPane(tfOutput = new JTextPane(document));
        /* Initialize the smartScroller watcher in order to keep the scroll bar
           at the bottom everytime tfOutput is updated (needed to leave room for
           the ComboBox menu).
        .*/
        SmartScroller smartScroller = new SmartScroller(sp);
        jframe = new JFrame("JBofh");
        tfOutput.setEditable(false);
        Color bgColor = new Color(Integer.parseInt(jbofh.props.get(
                                    "gui.background.number.outputwindow")+""));
        Color fgColor = new Color(Integer.parseInt(jbofh.props.get(
                                    "gui.foreground.number.outputwindow")+""));
        tfOutput.setBackground(bgColor);
        int outputFontsize = Integer.parseInt(""+
                jbofh.props.get("gui.font.size.outputwindow"));
        Font font = new Font(""+jbofh.props.get("gui.font.name.outputwindow"),
            Font.PLAIN, outputFontsize);
        tfOutput.setFont(font);
        // Initialising the StyleConstants for use with the JTextPane.
        StyleConstants.setFontFamily(styleOri, font.getFamily());
        StyleConstants.setBackground(styleOri, bgColor);
        StyleConstants.setForeground(styleAlt, bgColor);
        StyleConstants.setForeground(styleOri, fgColor);
        StyleConstants.setForeground(styleNew, bgColor);
        StyleConstants.setItalic(styleNew, true);
        StyleConstants.setBold(styleNew, true);
        StyleConstants.setFontSize(styleNew, 1*outputFontsize/2);
        /* Declaring and initialising the Highlighter background color which is
           by design meant to be equal to the font color of the regular
           foreground font, on the other hand styleAlt would be the font color
           of the characters printed within the highlighted area.
        */
        painter = new DefaultHighlighter.DefaultHighlightPainter(fgColor);
        np.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        np.add(lbPrompt = new JLabel(), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        // Initialising the ComboBox and its related TextField constants.
        np.add(combo, gbc);
        tfCmdLine.addKeyListener(this);
        combo.addActionListener(this);
        tfCmdLine.setFocusable(true);
        combo.setEditable(true);
        combo.setSelectedIndex(combo.getItemCount()-1);
        combo.setOpaque(false);
        combo.setMaximumRowCount(5);
        tfCmdLine.requestFocusInWindow();
        combo.setFont(new Font(""+jbofh.props.get(
                                                   "gui.font.name.inputwindow"),
            Font.PLAIN, Integer.parseInt(""+jbofh.props.get(
                                                "gui.font.size.inputwindow"))));
        combo.setBackground(new Color(Integer.parseInt(jbofh.props.get(
                                     "gui.background.number.inputwindow")+"")));
        combo.setForeground(new Color(Integer.parseInt(jbofh.props.get(
                                     "gui.foreground.number.inputwindow")+"")));
        combo.setLightWeightPopupEnabled(false);
        jframe.getContentPane().add(sp, BorderLayout.CENTER);
        jframe.getContentPane().add(np, BorderLayout.SOUTH);
        /* The KeyboardFocusManager is a crucial part for holding the focus
           on the TextField while typing in the GUI or for switching the focus
           to and from the password dialog box(es) from the start.
        */
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher((KeyEvent e) -> {
                if ((e.getKeyCode() != KeyEvent.VK_CONTROL) && !exitingGUI) {
                    if ((e.getKeyCode() == KeyEvent.VK_ENTER) &&
                        (!("".equals(getCmdLineText())))) {
                        preRelease();
                    }
                    if (((!(e.getKeyCode() == KeyEvent.KEY_PRESSED)) ||
                        (!(e.getKeyCode() == KeyEvent.KEY_TYPED))) &&
                        (!(tfCmdLine.hasFocus())) &&
                        (!((e.getKeyCode() == KeyEvent.VK_C) &&
                        ((e.getModifiers() &
                        KeyEvent.CTRL_MASK) !=
                        0)))) {
                        combo.requestFocusInWindow();
                    }
                    if (tfOutput.hasFocus() && (e.getKeyCode() == 0)) {
                        String tmp = getCmdLineText() + e.getKeyChar();
                        tfCmdLine.setText(tmp);
                    }
                    if ((e.getKeyCode() == KeyEvent.VK_ENTER) &&
                        ("".equals(getCmdLineText())) && (isBlocking) &&
                        !(actionReady) && !(emptyEnter)) {
                        /* Use of the following keyboard hit counter is very
                           tricky and is the one that will dictate when it is
                           allowed to validate the full command (versus just a
                           part of the command), and when the keyboard enter key
                           is hit within the selection menu of the ComboBox,
                           which means litterally don't execute the command and
                           just list it up on the command line.
                         */
                        if (vkenter > 3) {
                            emptyEnter = true;
                            releaseLock();
                        } else if (vkenter > 0) {
                            jframe.requestFocusInWindow();
                            shouldPopup = false;
                            String tmp = getCmdLineText() + e.getKeyChar();
                            tfCmdLine.setText(tmp.trim());
                        }
                        vkenter++;
                    }
                }
                return false;
        });

        // We want control over some keys used on tfCmdLine
        tfCmdLine.setFocusTraversalKeysEnabled(false);
        Keymap keymap = JTextField.addKeymap("MyBindings", tfCmdLine.getKeymap());
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), 
            new MyKeyAction("tab"));
        if (disableCombo) { // Skip that if the GUI is started without ComboBox
            keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.
                                                                    VK_DOWN, 0),
                    new MyKeyAction("down"));
            keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP,
                                                                            0),
                    new MyKeyAction("up"));
        }
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 
            new MyKeyAction("esc"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_U, 
                                         java.awt.event.InputEvent.CTRL_MASK), 
            new MyKeyAction("clearline"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_D, 
                                         java.awt.event.InputEvent.CTRL_MASK),
            new MyKeyAction("Ctrl-D"));
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_L, 
                                         java.awt.event.InputEvent.CTRL_MASK),
            new MyKeyAction("Ctrl-L"));
       tfCmdLine.setKeymap(keymap);

        // Defining the context menu options (Right mouse click) on the GUI.
        String popups[] =  {"Copy scr sel  (Ctrl-C)", "copy_out",
                            "Paste in cmd (Ctrl-V)", "paste_in",
                            "─────────────────", "",
                            "Clear scr       (Ctrl-L)", "clear_screen",
                            "Clear cmd     (Ctrl-U)","clear_cmd",
                            "Clear highlights", "clear_hilit",
                            "─────────────────", "",
                            "Get cmd hist (Ctrl-R)", "get_hist",
                            "Find string    (Ctrl-F)", "find",
                            "─────────────────", "",
                            "Abort | Quit (Ctrl-D)", "abort"};
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
        jframe.setSize(Integer.parseInt("" + jbofh.props.get(
                       "gui.initialSize.width")),Integer.parseInt("" + jbofh.
                        props.get("gui.initialSize.height")));
        jframe.setVisible(true);
    }

        /** Function for expanding the string array that is used by the popup
         *  menus in the Combo boxes.
         * @param par0 the value of the StringArray par0
         */
    private String[] updateStrngArr(String[] par0) {
        String[] swpArray = new String[par0.length + 1];
        System.arraycopy(par0, 0, swpArray, 1,
                                          par0.length);
        swpArray[0] = "";
        par0 = swpArray;
        return par0;
    }

    /**
     *
     * @return
     */
    public String getCmdLineText(){
        tfCmdLine.requestFocusInWindow();
        return tfCmdLine.getText();
    }

    private JComboBox<String> drawComboBox(String... model) {
        //TODO: Possibly some additional useless overlapping overrides exit here
        return new JComboBox<String>(model){
            @Override
            public void updateUI() {
                super.updateUI();
                setUI(new BasicComboBoxUI() {
                /* This allows removing the arrow button from the JComboBox when
                   forced through the global disableCombo parameter.
                 */
                    @Override
                    protected JButton createArrowButton() {
                        JButton button;
                        if (disableCombo) {
                            button = null;
                        } else { 
                            button = new BasicArrowButton(
                                BasicArrowButton.NORTH,
                                UIManager.getColor("ComboBox.buttonBackground"),
                                UIManager.getColor("ComboBox.buttonShadow"),
                                UIManager.getColor("ComboBox.buttonDarkShadow"),
                                UIManager.getColor("ComboBox.buttonHighlight"))
                                                                               {
                                @Override
                                public int getWidth() {
                                    if (disableCombo) {
                                        return 0;
                                    } else return super.getWidth();
                                }
                            };
                        }
                        return button;
                    }
                    /* We need to have control and pinpoint where the popup menu
                       of the ComboBox, would display and force it to do so on a
                       fixed basis in at most 5 lines at the bottom right part
                       of the GUI and atop the width of the editable TextField.
                    */
                    @Override
                    protected BasicComboPopup createPopup() {
                        return new BasicComboPopup(comboBox) {
                        @Override
                        protected Rectangle computePopupBounds(int px, int py,
                                                               int pw,int ph) {
                            Toolkit toolkit = Toolkit.getDefaultToolkit();
                            Rectangle screenBounds;
                            GraphicsConfiguration gc =
                                    comboBox.getGraphicsConfiguration();
                            Point p = new Point();
                            SwingUtilities.convertPointFromScreen(p, comboBox);
                            if (gc != null) {
                                Insets screenInsets =
                                                    toolkit.getScreenInsets(gc);
                                screenBounds = gc.getBounds();
                                screenBounds.width -= (screenInsets.left +
                                                       screenInsets.right);
                                screenBounds.height -= (screenInsets.top +
                                                        screenInsets.bottom);
                                screenBounds.x += (p.x + screenInsets.left);
                                screenBounds.y += (p.y + screenInsets.top);
                            }
                            Rectangle rect = new Rectangle(px,py,pw,ph);
                            rect.y = -rect.height;
                            return rect;
                            }
                            @Override
                            public void show() {
                                // Condition to preserve the legacy behavior.
                                if (!disableCombo) {
                                    comboBox.firePopupMenuWillBecomeVisible();
                                    setListSelection(comboBox.
                                                     getSelectedIndex());
                                    Point location = getPopupLocation();
                                    show( comboBox, location.x, location.y );
                                    // Now, try cleaning the CmdLine from noise.
                                    String tmp = getCmdLineText();
                                    if (!"".equals(tmp) ||
                                        (!"".equals(tmp)) ||
                                            (!"".equals(tmp)) ||
                                                tmp.length() > 0) {
                                        try {
                                            int tmp2 = (int) (tmp.charAt(0));
                                            if ((tmp2 > 32)
                                                         || (tmp.length() > 1)){
                                                /* Needed to debug a weird
                                                  behavior where the cmdline
                                                  is presented with an "empty"
                                                  character at popup menu init
                                                 */
                                                System.out.println(tmp2);
                                                tfCmdLine.setText(tmp);
                                            } else {
                                                tfCmdLine.setText("");
                                            }
                                        } catch (StringIndexOutOfBoundsException
                                                 ex) {}
                                    } else tfCmdLine.setText("");
                                }
                            }
                            @Override
                            public void hide() {
                                /* The only way to have control over automatic
                                   off and on of the popup menu was by inserting
                                   the following boolean check.
                                 */
                                if (shouldPopup == false) {
                                    MenuSelectionManager manager =
                                          MenuSelectionManager.defaultManager();
                                    MenuElement [] selection =
                                                      manager.getSelectedPath();
                                    for (MenuElement selection1 : selection) {
                                        if (selection1 == this) {
                                            manager.clearSelectedPath();
                                            break;
                                        }
                                    }
                                    if (selection.length > 0) {
                                        /* The extra space is primarily to
                                           workaround a weird bug where the
                                           ComboBox strips out the carriage
                                           return randomly and for unexplained
                                           reasons, but also for extra
                                           convenience for the user while
                                           reediting the history if ever needed.
                                         */
                                        tfCmdLine.setText(getCmdLineText() +
                                                                           " ");
                                        comboBox.repaint();
                                    }
                                }
                            }
                            private void setListSelection(int selectedIndex) {
                                if ( selectedIndex == -1) {
                                    super.list.clearSelection();
                                        }
                                else {
                                    super.list.setSelectedIndex(selectedIndex);
                                    super.list.ensureIndexIsVisible(
                                                                selectedIndex );
                                }
                            }
                            /* This is somehow an implicit "override" of the
                               method with the same name in BasicComboPopup. The
                               method is mainly taken as it is because required
                               locally by the show() method in here that is
                               itself overridden. There is definitely other
                               types of solutions to that, but this seems to be
                               and to our actual knowledge the most elegant.
                            */
                            private Point getPopupLocation() {
                                Dimension popupSize = comboBox.getSize();
                                Insets insets = getInsets();
                                popupSize.setSize(popupSize.width - (
                                                    insets.right + insets.left),
                                                      getPopupHeightForRowCount(
                                                                       comboBox.
                                                         getMaximumRowCount()));
                                Rectangle popupBounds = computePopupBounds(0,
                                                    comboBox.getBounds().height,
                                                                popupSize.width,
                                                              popupSize.height);
                                Dimension scrollSize = popupBounds.getSize();
                                Point popupLocation = popupBounds.getLocation();
                                scroller.setMaximumSize( scrollSize );
                                scroller.setPreferredSize( scrollSize );
                                scroller.setMinimumSize( scrollSize );
                                list.revalidate();
                                return popupLocation;
                            }
                        };
                    }
                 });
            }
            /* This is needed to be overridden as well in order to help the
               previous overridden methods in reaching the behavior change.
            */
            @Override
            public boolean contains(int x, int y) {
                 Insets i = getInsets();
                 int w = getWidth()  - i.left - i.right;
                 int h = getHeight() - i.top - i.bottom;
                 return (x >= i.left) && (x < w) && (y >= i.top) && (y < h);
            }
         };
    }

    /**
     *
     * @param on
     */
    public void showWait(boolean on) {
        if(on) {
            lbPrompt.setText("Wait");
            combo.setEditable(false);
        } else {
            lbPrompt.setText((String) jbofh.props.get("console_prompt"));
            combo.setEditable(true);
        }
    }

    /**
     *
     * @return
     */
    public boolean confirmExit() {
            // Capturing this situation status in order to reset focus
            exitingGUI = true;
            return exitingGUI = (JOptionPane.OK_OPTION == 
                JOptionPane.showConfirmDialog(jframe, "Really exit?",
                                "Please confirm", JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE));
    }
    /**
     * Implementation of a JTextPane search functionality via JOptionPane.
     * Including own simplified JComboBox to retain the search history.
     * @throws no.uio.jbofh.MethodFailedException
     */
    public void searchString() throws MethodFailedException {
            JComboBox<String> searchCombo = new
                                              JComboBox<String>(searchStrngArr){
                public void addNotify() {
                    super.addNotify();
                    KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                        .addKeyEventDispatcher((KeyEvent e) -> {
                        this.getEditor().getEditorComponent().
                                                         requestFocusInWindow();
                        return false;
                    });
                }
            };
            searchCombo.setEditable(true);
            JOptionPane.showMessageDialog(jframe, searchCombo,
                    "Text to look for in the output area:",
                    JOptionPane.QUESTION_MESSAGE);
                    String tmp = (String) searchCombo.getSelectedItem();
            if (!tmp.isEmpty()) {
                            searchStrngArr = updateStrngArr(searchStrngArr);
                            searchStrngArr[0] = tmp;
                            searchReplaceString(tmp, styleAlt, document, true,
                                    false, true);
            }
    }

    /**
     *
     * @param msg
     * @param crlf
     */
    @Override
    public void showMessage(String msg, boolean crlf) {
            try {
                document.insertString(document.getLength(), msg, styleOri);
            } catch (BadLocationException ex) {
                Logger.getLogger(JBofhFrameImpl.class.getName()).log(Level.
                                                              SEVERE, null, ex);
            }
        if(crlf) {
            try {
                int docLength = tfOutput.getDocument().getLength();
                // Remove the annoying extra trailing prompt from the output.
                String prompt = (String) jbofh.props.get("console_prompt");
                if (docLength > prompt.length()) {
                    if (tfOutput.getDocument().getText(docLength - prompt.
                           length()- 3, prompt.length() + 3).contains(prompt)) {
                        /*
                          TODO: Those 2 statements can be in own function as
                                they are reused in the next "else if" condition.
                        */
                        tfOutput.getDocument().remove(docLength - prompt.
                                                      length() -1, prompt.
                                                                   length() +1);
                        tfOutput.getDocument().insertString(tfOutput.
                                                            getDocument().
                                                            getLength(), "\n",
                                                                      styleOri);
                    }
                } else if (tfOutput.getDocument().getText(0,docLength).
                                                             contains(prompt)) {
                    tfOutput.getDocument().remove(docLength - prompt.
                                                      length() -1, prompt.
                                                                   length() +1);
                    tfOutput.getDocument().insertString(tfOutput.getDocument().
                                                        getLength(), "\n",
                                                                      styleOri);
                        }
                // Then try removing unnecessary extra empty lines if existing.
                int docLength2 = tfOutput.getDocument().getLength();
                int emptyLine = 0;
                for (int i = 2; i < docLength2; i++) {
                    if ((int) (tfOutput.getDocument().
                        getText(docLength2 - 1, 1).charAt(0)) == 10 ||
                            (int)(tfOutput.getDocument().
                                  getText(docLength2 - 1, 1)).charAt(0) == 32) {
                        if ((int) (tfOutput.getDocument().getText(docLength2 -i,
                             1).charAt(0)) == 10 ||
                            (int)(tfOutput.getDocument().getText(docLength2 - i,
                             1)).charAt(0) == 32) {
                            emptyLine++;
                        } else break;
                    }
                }
                if (emptyLine > 0) {
                    tfOutput.getDocument().remove(docLength2 - emptyLine - 1,
                                                  emptyLine + 1);
                }
                // Make sure there is an extra legitimate space between results.
                tfOutput.getDocument().insertString(tfOutput.getDocument().
                                                    getLength(),"\n", styleOri);
            } catch (BadLocationException ex) {
                Logger.getLogger(JBofhFrameImpl.class.getName()).log(Level.
                                                              SEVERE, null, ex);
            }
        }
        // Leave enough place if eventually the ComboBox would display.
        if (!(disableCombo)) {
            Insets m = new Insets(0,0,100,0);
            tfOutput.setMargin(m);
        }
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
        if ((actionReady == false) && (shouldPopup == true)) {
            combo.validate();
            combo.repaint();
            combo.requestFocusInWindow();
            combo.addNotify();
            combo.setPopupVisible(!disableCombo);
            } else if (shouldPopup == false) {
            combo.requestFocusInWindow();
            combo.hidePopup();
        }
        synchronized (combo.getTreeLock()) {
            while (true) {
                try {
                    isBlocking = true;
                    combo.getTreeLock().wait();  // Lock
                    if(wasEsc){
                        wasEsc = false;
                        throw new IOException("escape hit");
                    }
                    String text = getCmdLineText();
                    if(addHist && disableCombo){
                        if((text.length() > 0) &&
                            ! (cmdLineHistory.size() > 0 &&
                                text.equals(cmdLineHistory.get(cmdLineHistory.size()-1)))) {
                            cmdLineHistory.add(text);
                        }
                        historyLocation = cmdLineHistory.size();
                    } else {
                        // History in the ComboBox should be tidyer.
                        for (int i = 0; i<cmdLineStrngArr.length;i++) {
                            if (cmdLineStrngArr[i].equalsIgnoreCase(
                                    text.trim()) && !("".equals(text.trim()))) {
                                if (i > 1) {
                                    String[] swapArr =
                                        new String[cmdLineStrngArr.
                                        length];
                                    System.arraycopy(
                                        cmdLineStrngArr, i,
                                        swapArr, i,
                                        cmdLineStrngArr.
                                            length-i);
                                    System.arraycopy(
                                        cmdLineStrngArr, 1,
                                        swapArr, 2, i-1);
                                    swapArr[0] = "";
                                    swapArr[1] = text.trim();
                                    cmdLineStrngArr = swapArr;
                                }
                                historyMatched = true;
                            }
                        }
                        if (!historyMatched) {
                            if (!("".equals(text.trim()))) {
                                String[] tmpStrngArr =
                                    (text.trim()).split("\\s+");
                                ArrayList tmpCmpl = new ArrayList();
                                if (tmpStrngArr.length >= 1) {
                                    if (tmpStrngArr.length > 1) {
                                        String tmp1;
                                        tmp1 = tmpStrngArr[0].
                                            concat(" "+
                                                tmpStrngArr[1]);
                                        jbofh.bcompleter.
                                            complete(tmp1,
                                                tmp1.length(),
                                                tmpCmpl);
                                    } else {
                                        jbofh.bcompleter.
                                            complete(tmpStrngArr[0],
                                                tmpStrngArr[0].
                                                    length(),
                                                tmpCmpl);
                                    }
                                }
                                if (!tmpCmpl.isEmpty()) {
                                    cmdLineStrngArr =
                                        updateStrngArr(cmdLineStrngArr);
                                    cmdLineStrngArr[1] = text.trim();
                                }
                            }
                        }
                    }
                    showMessage("\n" + prompt + text, true);
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
        if ((evt.getSource() != combo)) {
            String action = (String) menuItems.get(evt.getSource());
            // To have the Ctrl-L functionality from the mouse context menu
            if(action.equals("clear_screen")) {
                tfOutput.setText("");
                try {
                    documentBackup.insertString(0, "", styleOri);
                } catch (BadLocationException ex) {
                    Logger.getLogger(JBofhFrameImpl.class.getName()).log(Level.
                                                              SEVERE, null, ex);
                }
            }
            if (action.equals("paste_in")) {
                String tmp = getCmdLineText();
                int tmp1 = tfCmdLine.getCaretPosition();
                String clipboardText = "";
                try {
                    clipboardText = (String) Toolkit.getDefaultToolkit()
                                   .getSystemClipboard().getData(DataFlavor.
                                                                  stringFlavor);
                } catch (UnsupportedFlavorException | IOException ex) {
                         Logger.getLogger(JBofhFrameImpl.class.getName()).
                                                    log(Level.SEVERE, null, ex);
                }
                if (!"".equals(clipboardText) && (clipboardText != null)) {
                    if (tmp1 != 0 && !"".equals(tmp)) {
                        tfCmdLine.setText(tmp.substring(0, tmp1) +
                                           clipboardText + tmp.substring(tmp1));
                        tfCmdLine.setCaretPosition(tmp1 +
                                                        clipboardText.length());
                        } else tfCmdLine.setText(clipboardText);
                }
	    }
	    if (action.equals("copy_out")) {
		StringSelection tmp =
				new StringSelection(tfOutput.getSelectedText());
		Clipboard tmp1 = Toolkit.getDefaultToolkit().
				getSystemClipboard();
		tmp1.setContents(tmp, null);
	    }
	    if (action.equals("clear_hilit")) {
				tfOutput.setHighlighter(hilit);
				searchReplaceString(popupMenuMessage, styleOri,
						    document, false, true,
						    false);
	    }
	    if (action.equals("get_hist")) {
				combo.setPopupVisible(true);
				tfCmdLine.setText("");
	    }
	    if (action.equals("find")) {
		    try {
			searchReplaceString(popupMenuMessage, styleOri,
						    document, false, true,
						    false);
			    searchString();
		    } catch (MethodFailedException ex) {
			    Logger.getLogger(JBofhFrameImpl.class.getName()).
				    log(Level.SEVERE, null, ex);
		    }
	    }
	    if (action.equals("abort")) {
		    wasEsc = true;
		    releaseLock();
	    }
	    if (action.equals("clear_cmd")) {
		    tfCmdLine.setText("");
	    }
	}
    }

    /**
     * Check to see whether ready for release
     */
    protected void preRelease() {
	    if (combo.isPopupVisible() && combo.getSelectedItem() != null) {
		    if (!"".equals(combo.getItemAt(
			    combo.getSelectedIndex())) &&
				    (!"".equals(getCmdLineText()) ||
					(!"".equals(getCmdLineText())) ||
				            (!"".equals(getCmdLineText()))))
			    {
				    tfCmdLine.setText(combo.getItemAt(
					    combo.getSelectedIndex()));
				    vkenter = 4;
				    actionReady = false;
		    }
		    jframe.requestFocusInWindow();
		    shouldPopup = false;
	    }
	    else {
			    actionReady = true;
			    }
    }
    
    /**
     * Finished, release it.
     */
    protected void releaseLock() {
        if(! isBlocking) {
            return;
        }
        synchronized (combo.getTreeLock()) {
	    actionReady = false;
            isBlocking = false;
            EventQueue.invokeLater(() -> {
	        historyMatched = false;
	        emptyEnter = false;
	        vkenter = 1;
                /* Resetting the following keyboard hit counter which has a
                   crucial function allowing to delegate the keyboard focus on
                   the initial password dialog box as well as on the CmdLine
                   whenever focus is lost from the window and gained up again.
                 */
                kb = 0;
	        tfCmdLine.setText("");
           });
           combo.getTreeLock().notifyAll();
        }
    }

	@Override public void keyPressed(KeyEvent e) {
		if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_F) {
			try {
				searchReplaceString(popupMenuMessage, styleOri,
						    document, false, true,
						    false);
				searchString();
			} catch (MethodFailedException ex) {
				Logger.getLogger(JBofhFrameImpl.class.
					getName()).log(Level.SEVERE, null, ex);
			}
		}
		if ((e.isControlDown() && e.getKeyCode() == KeyEvent.VK_R) ||
			(!combo.isPopupVisible() &&
				e.getKeyCode() == KeyEvent.VK_UP) ||
				(!combo.isPopupVisible() &&
				e.getKeyCode() == KeyEvent.VK_DOWN)) {
			if (disableCombo) {
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					combo.removeAllItems();
					tfOutput.requestFocusInWindow();
				}
			}
			else {
			    shouldPopup = true;
			    try {
			        tfOutput.setHighlighter(hilit);
			        int start = documentBackup.getLength();
			        if (start > 0 && document.getLength() > start){
					    document.replace(0,
						    start,
						    documentBackup.getText(0,
						    documentBackup.getLength()),
								    styleOri);
					    }
			        document.insertString(
				    document.getLength(), popupMenuMessage,
								      styleNew);
				// Workaround a bug where everything afterwards might get highlighted.
				document.insertString(document.getLength() + 1,
							    "\t\t\t", styleOri);
				int endindex = document.getLength();
				hilit.addHighlight(endindex - popupMenuMessage.
					length() + 4, endindex - 5, painter);
				} catch (BadLocationException ex) {
					Logger.getLogger(
						JBofhFrameImpl.class.getName()).
						log(Level.SEVERE, null, ex);
			        }
				combo.setPopupVisible(true);
			}
		}
	}

	@Override
	public void keyTyped(final KeyEvent e) {
	    if ((actionReady == true) && (e.getKeyChar()=='\n')) {
	      releaseLock();
	   }
	  EventQueue.invokeLater(() -> {
		  String text = getCmdLineText();
		  ComboBoxModel<String> m;
		  List<String> list = Arrays.asList(cmdLineStrngArr);
		  Collections.reverse(list);
		  String[] swpStrngArr = list.toArray(new String[0]);
		  if (text.isEmpty()) {
			  m = new DefaultComboBoxModel<>(swpStrngArr);
			  insertSuggestions(comboBox, m, "");
			  Collections.reverse(list);
		  } else {
			  m = listSuggestions(list, text);
			  if (m.getSize() == 0) {
			  } else {
				  int tmp = tfCmdLine.getCaretPosition();
				  insertSuggestions(comboBox, m, text);
				  tfCmdLine.setCaretPosition(tmp);
			  }
			  Collections.reverse(list);
		  }
	   });
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
                            if (kb == 1) {
                                this.processKeyEvent(e);
                            }
                        kb++;
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

	/**
	 *
	 * @param str the value of str
	 * @param style the value of style
	 * @param doc the value of doc
	 * @param hlite whether search results would be highlighted or not
	 * @param remove whether search results would be replaced by empty strng
	 */
	@SuppressWarnings("ReplaceAllDot")
	private void searchReplaceString (String str, Style style,
						DefaultStyledDocument doc,
						Boolean hlite, Boolean remove,
						Boolean backup) {
		if (hlite == true) {
			tfOutput.setHighlighter(hilit);
			}
		String docContent = " ";
		try {
		    docContent = doc.getText(0, doc.getLength());
		    } catch (BadLocationException ex) {
		    Logger.getLogger(JBofhFrameImpl.class.getName()).log(Level.
					    SEVERE, null, ex);
		    }
		try {
		    int start = documentBackup.getLength();
		    if (start > 0 && doc.getLength() > start){
					    doc.replace(0,
						    start,
						    documentBackup.getText(0,
						    documentBackup.getLength()),
								    styleOri);
					    }
		    if ((doc.getLength() > start) && backup == true) {
			documentBackup.insertString(start, doc.getText(start,
						doc.getLength()- start),
						styleOri);
		    }
		} catch (BadLocationException ex) {
		    Logger.getLogger(JBofhFrameImpl.class.getName()).log(Level
			    .SEVERE, null, ex);
		}
			if ((docContent.contains(str)) && !(" ".equals(str)) &&
				remove == false) {
			    int index = docContent.indexOf(str, 0);
				while (docContent.lastIndexOf(str) > index) {
				    try {
					    int end = index + str.length();
					    doc.replace(index,
						        str.length(),
						        str, style);
					    if (hlite == true) {
						    hilit.addHighlight(index,
							    end, painter);
							index = docContent.
							    indexOf(str, end);
					    }
				    } catch (BadLocationException e) {
				    }
				}
				if (docContent.lastIndexOf(str) == index) {
					try {
					    doc.replace(index,
					        str.length(), str, style);
					    if (hlite == true) {
					        hilit.addHighlight(index, index +
						    str.length(), painter);
						// Workaround a bug where everything afterwards might get highlighted.
						if ((docContent.length() -
						      index - str.length() - 1) < 3){
							doc.insertString(docContent.length() + 1 , "\t\t\t",
								styleOri);
						}
					    }
					} catch (BadLocationException e) {
					}
				}
			    }
			if (remove == true) {
				try {
					doc.remove(0, doc.getLength());
					doc.insertString(0, docContent.
						replaceAll(Pattern.quote(str),
							""), style);
					documentBackup.remove(0, documentBackup.
						getLength());
					documentBackup.insertString(0, doc.
						getText(0, doc.getLength()),
						style);
				} catch (BadLocationException ex) {
					Logger.getLogger(JBofhFrameImpl.class.
						getName()).log(Level.SEVERE,
							null, ex);
				}
			}
		releaseLock();
		tfCmdLine.setText("");
    }

    private void insertSuggestions(JComboBox<String> comboBox,
				    ComboBoxModel<String> mdl, String str) {
        comboBox.setModel(mdl);
        comboBox.setSelectedIndex(comboBox.getItemCount()-1);
        tfCmdLine.setText(str);
    }
    private ComboBoxModel<String> listSuggestions(List<String> localList,
			    String text) {
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
	localList.forEach((s) -> {
		if (s.startsWith(text)) {
			m.addElement(s);
		} else {
			m.insertElementAt("", 0);
			m.setSelectedItem("");
		}	});
        return m;
    }
}

// arch-tag: f9ab9f2a-5606-4a77-9fba-4ec72459b18b
