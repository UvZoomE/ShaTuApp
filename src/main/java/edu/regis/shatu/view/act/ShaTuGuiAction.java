/*
 * SHATU: SHA-256 Tutor
 * 
 *  (C) Johanna & Richard Blumenthal, All rights reserved
 * 
 *  Unauthorized use, duplication or distribution without the authors'
 *  permission is strictly prohibted.
 * 
 *  Unless required by applicable law or agreed to in writing, this
 *  software is distributed on an "AS IS" basis without warranties
 *  or conditions of any kind, either expressed or implied.
 */
package edu.regis.shatu.view.act;

import java.awt.Image;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.regis.shatu.svc.TutorReply;
import edu.regis.shatu.util.ImgFactory;
import edu.regis.shatu.view.MainFrame;

/**
 * Abstract root for all GUI actions in the ShaTu application.
 * 
 * Provides support for loading image icons and assigning the GUI controller.
 * 
 * @author rickb
 */
public abstract class ShaTuGuiAction extends AbstractAction {
    private static final Gson gson = new Gson();
    private static final Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();

    public ShaTuGuiAction(String name) {
        super(name);
    }
    
    /**
     * Load and return the image icon specified by the given image file name,
     * as found in the "img/" directory in the root CLASSPATH.
     * 
     * @param imageFileName name of file with suffix in img/ director
     *                      (e.g., "save16.gif"
     * @param altText
     * @return 
     */
    protected ImageIcon loadIcon(String imageFileName, String altText) {
        // ToDo: Better error reporting
        Image img = ImgFactory.createImage(imageFileName);
        return new ImageIcon(img, altText);
    }
    
    /**
     * Return the normal Gson instance
     * 
     * This instance is used by ResetPasswordAction, CreateAcctAction, and CheckSecurityQuestAction
     *
     * @return gson = new Gson()
     */
    protected static final Gson getGson() {
        return gson;
    }

    /**
     * Return the pretty Gson instance
     * 
     * This instance is used by SignInAction, StepCompletionAction, NewExampleAction, and HintAction
     *
     * @return gsonPretty = new GsonBuilder().setPrettyPrinting().create()
     */
    protected static final Gson getGsonPretty() {
        return gsonPretty;
    }

    /**
     * If the tutor's reply carries a synced tutoring mode, apply it to the
     * client's session so views read the up-to-date mode.
     *
     * @param reply the reply from the tutor
     */
    protected void applyTutoringMode(TutorReply reply) {
        if (reply.getTutoringMode() != null && MainFrame.instance().getModel() != null) {
            MainFrame.instance().getModel().setTutoringMode(reply.getTutoringMode());
        }
    }
}
