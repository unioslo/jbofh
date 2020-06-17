Change log
==========


Changes and improvements with version 1.0.0
-------------------------------------------

- Major enhancement to `BofhdConnection` allowing it to use the
  native JRE `KeyStore` which would make the use of the already defined
  properties parameter `InternalTrustManager.enable=false` finally
  possible over encrypted communications, in practice that would mean
  that jBofh can rely on server certificates signed by third parties.

- Major changes to `JBofhFrameImpl` (JBofh’s GUI interface) to enable
  easier history search through a `JComboBox`.  Legacy search can be
  enabled using the configuration parameter `disableCombo=true`.

  List of GUI and technical changes in the `JBofhFrameImpl`:

  - Upgrade to `JTextPane` as a replacement to the `JTextArea` which implies the
    availability of different colors, styles and highlights on the display area.

  - Support for the `systemLookAndFeel` parameter which if set to
    true will yield a GUI style that is similar to the GUI style of the
    OS desktop environment. All styles (fonts, colours and sizes) are
    not supported though with the `systemLookAndFeel` parameter enabled
    and would be overridden if specified in the configuration and not
    supported by the operating system natively.

  - Implementation of the Java `ComboBox` on top of the previously
    implemented Java `TextField`.

    Important modifications were brought in to the default behavior of
    `JComboBox`, mainly to the fact that it will always display on the
    top of the editable `JTextField` with all the modifications that
    might implicitly include where the least tom mention is forcing
    the scroll bar of the `JTextPane` to always reset to the bottom
    not allowing a vacuum distance to the `CmdLine` that might include
    output text hidden by the `ComboBox`. The `JComboBox` is enabled in
    the four following manners:

    - Explicitly, by right clicking the mouse and then left clicking
      the option _Get cmd hist_. By left clicking the menu arrow of the
      `ComboBox`. Also relatively explicit whilst focused on the `CmdLine`
      (or when simply typing) and the keyboard key combination shortcut
      <kbd>Ctrl</kbd> + <kbd>R</kbd> is pressed.

    - Implicitly whilst focused on the `CmdLine` or when typing and
      then the keyboard either up or down arrow keys (<kbd>↑</kbd>
      or <kbd>↓</kbd>) are hit.

  - The automatic focus mechanisms to the various window components
    has been drastically enhanced, allowing the use of the GUI windows,
    keyboard shortcuts in a seamless manner.

  - The right mouse context menu includes seven additional options
    with their related shortcuts that allow visualizing old and new
    funcitonalities for handling data from the output and input areas of
    the GUI.

  - An important option among the seven options mentioned above on the
    context menu and that is introduced with this version is the search
    functionality through the text displayed in the output area. That, could
    be initiated with a mouse click or with the keyboard combination key
    shortcut: <kbd>Ctrl</kbd> + <kbd>F</kbd> when focused on the `CmdLine`.

  - Text highlights is another important enhancement brought in the GUI.
    It has been nevertheless decided to limit it to permuting the background
    colour and the foreground colour for the highlighted area, and that
    in order not to make the GUI very flashy.

    Highlights are activated under two circumstances: first displaying
    the search strings when ordered so and second when the `ComboBox`
    is implicitly activated from the keyboard (to explain for the user
    what is going on).

    Highlights are removed in two cases: Implicityl when a new search
    string is entered, the highlights are removed from all previous
    highlights in the GUI, explicitly when right clicking the mouse and
    then left clicking the option _Clear highlights_. That latter option
    would not only remove all highlights but would permanently delete
    as well all instances of those message texts from within the output
    area: `- POP-UP MENU BOX FOR COMMAND HISTORY ACTIVE -`.  To disable:
    click outside or validate (hit <kbd><┘</kbd>) - _Clear highlights_
    right-mouse-click option clears those messages.


Changes and improvements with version 0.9.9
-------------------------------------------

- All obsolete code was upgraded in the core jBofh Java classes,
  references to `Vector` and `Hashtable` Java classes have been replaced,
  other standard coding issues were detected and corrected with the help
  of the NetBeans IDE.

- All API libraries (e.g. XMLRPC, JLine) were upgraded to the latest stable.
  released versions before EOL.

- After upgrade of the underlying XMLRPC API and touching the code a
  bit, a serious bug/vulnerability that would have allowed sending the
  password over an unsecure though encrypted wire during handshake has
  been fixed.

- Possibility to pass muliple `--set` arguments on the command line
  separated by commas like this:

  	bofh --gui --set gui.font.size.outputwindow=9,gui.font.name.outputwindow=Sans

  or to run the UiO TSD instance:

  	bofh --url https://tsd-cere-prod01.tsd.usit.no:8000 --set console_prompt="tsd-jbofh>"

- jbofh trims blanks at the end of the command now before sending them
  over to the XMLRPC daemon.

- Reverse search in the jbofh console (not the GUI) works well now, in
  addition to all the previously defect keymaps that were fixed due to a
  newer and more stable version of JLine.

- A line break has been added at the end of each command as requested
  by some bofh users.

- The GUI interface had some face liftings as well, we hereby name the
  most important and relevant ones:

  - The keyboard is focused automatically on the text fields when the
    focus is set on the open Java GUI (focus is set as well by default
    when the GUI is started).

  - Spaces have been forced between the results of the commands for an
    enhanced readability experience.
