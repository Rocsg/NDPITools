/***
 * Image/J Plugins
 * Copyright (c) 2012 Christophe Deroulers
 * after HelpPanel Copyright (C) 2002-2004 Jarek Sacha
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Latest release available at http://sourceforge.net/projects/ij-plugins/
 */
package com.phenomen.ndpilegacy;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

/**
 * A panel displaying help for the NDPITools plugin bundle.
 *
 * @author C. Deroulers after Jarek Sacha
 * @version $Revision: 1.7 $
 */
    @SuppressWarnings("serial")
final public class HelpPanel extends JPanel {

    /**
     * Default constructor.
     *
     * @throws IOException If help content cannot be loaded.
     */
    public HelpPanel() throws IOException
    {
        URL helpURL = HelpPanel.class.getResource("/docs/ndpitools.html");
        if (helpURL == null)
            throw new IOException("Couldn't find the NDPITools help file.");

        JEditorPane editorPane = new JEditorPane();
        editorPane.setPage(helpURL);
        editorPane.setEditable(false);

        /* Put the editor pane in a scroll pane. */
        JScrollPane editorScrollPane = new JScrollPane(editorPane);
        editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorScrollPane.setPreferredSize(new Dimension(600, 400));

        setLayout(new BorderLayout());
        add(editorScrollPane, BorderLayout.CENTER);
    }

    /**
     * Create and display help window.
     * 
     */
    static void showHelpWindow() {
        /* Create window to host help panel */
        final JFrame frame = new JFrame("About NDPITools plugins v. 1.7");

        JButton closeButton = new JButton("Close", null);
        closeButton.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                { frame.setVisible(false); }
            });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        /* Create help panel */
        final HelpPanel aboutNDPITools;
        try {
            aboutNDPITools = new HelpPanel();
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to create the NDPITools help window.", e);
        }

        frame.getContentPane().add(aboutNDPITools);
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        /* Display help window, to ensure thread safety run it in the 
         * Event Dispatch Thread. */
        SwingUtilities.invokeLater(
            new Runnable()
            {
                public void run()
                {
                frame.pack();

                Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
                Dimension frameSize = frame.getSize();
                if (frameSize.height > screenSize.height)
                    frameSize.height = screenSize.height;
                if (frameSize.width > screenSize.width)
                    frameSize.width = screenSize.width;
                frame.setLocation(
                    (screenSize.width - frameSize.width) / 2,
                    (screenSize.height - frameSize.height) / 2);

                frame.setVisible(true);
                }
            });
    }
}
