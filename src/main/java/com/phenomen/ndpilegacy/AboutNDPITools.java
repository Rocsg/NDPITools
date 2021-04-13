/***
 * Image/J Plugins
 * Copyright (C) 2012 Christophe Deroulers
 * after code (C) 2002-2004 Jarek Sacha
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

import ij.IJ;
import ij.plugin.PlugIn;

/**
 * @author Christophe Deroulers after Jarek Sacha
 * @version $Revision: 1.4.2 $
 */
public class AboutNDPITools implements PlugIn {

    private static final String TITLE = "About NDPITools Plugin Bundle";
    private static final String MESSAGE =
            "NDPITools plugins add to ImageJ support for NDPI files\n" +
            "with the help of the NDPITools command line tools.\n" +
            "For more detailed informations see NDPITools home page at:\n" +
            "http://www.imnc.in2p3.fr/pagesperso/deroulers/software/ndpitools";

    public void run(String string) {

        try {
            IJ.showStatus("Starting \""+TITLE+"\" plugin...");
            HelpPanel.showHelpWindow();
        } catch (RuntimeException e) {
            String msg = MESSAGE + "\n" +
                    "*****************************************************************\n" +
                    "Regular NDPITools Plugin help failed to load content from HTML resource.\n" +
                    "This may be a problem with the installation of the current version\n" +
                    "of ImageJ.\n" +
                    "________________________________________________________________\n" +
                    "Original error message:\n" + e;
            IJ.showMessage(TITLE, msg);
        } finally {
            IJ.showStatus("");
        }

    }

}
