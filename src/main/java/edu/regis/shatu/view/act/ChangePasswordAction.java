/*
 *  SHATU: SHA-256 Tutor
 * 
 *   (C) Johanna & Richard Blumenthal, All rights reserved
 * 
 *   Unauthorized use, duplication or distribution without the authors'
 *   permission is strictly prohibited.
 * 
 *   Unless required by applicable law or agreed to in writing, this
 *   software is distributed on an "AS IS" basis without warranties
 *   or conditions of any kind, either expressed or implied.
 */
package edu.regis.shatu.view.act;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import com.google.gson.Gson;

import edu.regis.shatu.model.Account;
import edu.regis.shatu.svc.ClientRequest;
import edu.regis.shatu.svc.ServerRequestType;
import edu.regis.shatu.svc.SvcFacade;
import edu.regis.shatu.svc.TutorReply;
import edu.regis.shatu.view.MainFrame;
import edu.regis.shatu.view.UpdateAccountPanel;

/**
 * An (MVC) controller handling a GUI gesture representing a user's request to
 * change their password from within the update account panel.
 *
 * @author Henry Ordonez
 * @author rickb
 */
public class ChangePasswordAction extends ShaTuGuiAction{
    
    /** Exceptions occurring in this class are also logged to this logger. */
    private static final Logger LOGGER = Logger.getLogger(ChangePasswordAction.class.getName());
    
    /** The single instance of this forgot password action action. */
    private static final ChangePasswordAction SINGLETON;
    
    /**
     * Create the singleton for this action, which occurs when this class
     * is loaded by the Java class.
     */
    static {
        SINGLETON = new ChangePasswordAction();
    }

    /**
     * Return the singleton instance of this change password action.
     * 
     * @return a singleton ChangePasswordAction object
     */
    public static ChangePasswordAction instance() {
	return SINGLETON;
    }

    /**
     * Initialize this change password action.
     */
    private ChangePasswordAction() {
        super("Change Password");
        putValue(SHORT_DESCRIPTION, "Request to change password");
        putValue(MNEMONIC_KEY, KeyEvent.VK_C);
    }
    
    /**
     * Creates an Account copy and prompts the user to enter their password 
     * before allowing the user to commit any changes. This Account copy is
     * returned and contains the password that was entered.
     * 
     * TODO: Determine if there is a more secure way of handling the way in which
     * a user enters a password and how it's temporarily stored.
     * 
     * @return an Account object containing the user-provided password
     */
    private Account buildAuthenticationAccount() {
        Account accountCopy;
        JPasswordField passwordField = new JPasswordField();
        Object[] message = {"Please confirm your password:", passwordField};

        int option = JOptionPane.showConfirmDialog(
                MainFrame.instance(),
                message,
                "Password Required",
                JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            // Kludge: Not using the typical MainFrame.instance().getAccount() because
            // leaving the security answer blank in the update account panel results 
            // in that "answer" being stored in the DB due to this action.
            accountCopy = new Account(MainFrame.instance().getModel().getStudent().getAccount());
            char[] passwordChars = passwordField.getPassword();
            try {
                // Pass the char array directly instead of converting to a String
                accountCopy.setPassword(UpdateAccountPanel.encryptSHA256(passwordChars));
            } finally {
                // Securely wipe the plaintext password from memory immediately
                Arrays.fill(passwordChars, '\0');
            }
        }
        else {
            accountCopy = null;
        }
        
        return accountCopy;
    }
    
    /**
     * Makes a request to the server to verify the user's password.
     * 
     * @param account the user's account containing the password to be verified
     * @return a TutorReply object
     */
    private TutorReply verifyPasswordRequest(Account account) {
        Gson gson = getGsonPretty();
        
        ClientRequest request = new ClientRequest(ServerRequestType.VERIFY_PASSWORD);
        request.setData(gson.toJson(account));
        
        return SvcFacade.instance().tutorRequest(request);
    }
    
    /**
     * Displays a GUI message to the user indicating an invalid password was
     * entered.
     */
    private void invalidPasswordMessage() {
        JOptionPane.showMessageDialog(
                MainFrame.instance(),
                "Password authentication failed.",
                "Invalid Password",
                JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Handle the user's request to change their password after clicking on
     * the change password button in the update account panel.
     * 
     * @param evt ignored
     */
    @Override
    public void actionPerformed(ActionEvent evt) {      
        Account account = buildAuthenticationAccount();
        
        if(account == null) return; // The user cancelled the prompt
        
        TutorReply reply = verifyPasswordRequest(account);
        if(reply.getStatus().equals("Authenticated")) {
            // Pass control over to the reset password panel
            MainFrame.instance().displayView(MainFrame.ViewName.RESET_PASSWORD);
        }
        else if(reply.getStatus().equals("InvalidPassword")) {
            invalidPasswordMessage();
        }
        else {
            MainFrame.instance().displayError(reply.getErrorMessage());
        }
    }
}
