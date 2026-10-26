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
 * An MVC controller handling a user GUI gesture requesting the update of a
 * student account within the UpdateAccountPanel.
 *
 * @author Henry Ordonez
 * @author rickb
 */
public class UpdateAcctAction extends ShaTuGuiAction {
    
    /** Exceptions occurring in this class are also logged to this logger. */
    private static final Logger LOGGER = Logger.getLogger(UpdateAcctAction.class.getName());

    /** The single instance of this update account action. */
    private static final UpdateAcctAction SINGLETON;

    /**
     * Create the singleton for this action, which occurs when this class
     * is loaded by the Java class, as a result of the class being 
     * referenced by executing UpdateAcctAction.instance() in the 
     * initComponents() method of the UpdateAccountPanel class.
     */
    static {
        SINGLETON = new UpdateAcctAction();
    }

    /**
     * Return the singleton instance of this update action.
     *
     * @return a Singleton UpdateAcctAction object
     */
    public static UpdateAcctAction instance() {
        return SINGLETON;
    }
    
    /**
     * Initialize this update account action with the "Update Account" text.
     */
    private UpdateAcctAction() {
        super("Update Account");

        putValue(SHORT_DESCRIPTION, "Update user information");
        putValue(MNEMONIC_KEY, KeyEvent.VK_U);
    }
    
    /**
     * Creates an Account copy and prompts the user to enter their password 
     * before allowing the user to commit any changes. This Account copy is
     * returned and contains the password that was entered.
     * 
     * @return an Account object that will be used for authentication and updating
     * if the account is verified
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
            accountCopy = new Account(MainFrame.instance().getAccount());
            char[] passwordChars = passwordField.getPassword();
            try {
                accountCopy.setPassword(
                        UpdateAccountPanel.encryptSHA256(new String(passwordChars))
                );
            } finally {
                // Securely clear the password character array from memory immediately after use
                java.util.Arrays.fill(passwordChars, '\0');
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
     * Makes a request to the server to update the user's account information.
     */
    private TutorReply makeUpdateRequest(Account account) {
        Gson gson = getGson();
        
        ClientRequest request = new ClientRequest(ServerRequestType.UPDATE_ACCOUNT);
        request.setUserId(account.getUserId());
        request.setSecurityToken(MainFrame.instance().getModel().getSecurityToken());
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
     * Displays a GUI message indicating success or failure.
     */
    private void displayResultMessage(String s) {
        if(s.equals("Success")) {
            JOptionPane.showMessageDialog(
                    MainFrame.instance(),
                    "Your account information was successfully updated!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            JOptionPane.showMessageDialog(
                    MainFrame.instance(),
                    "Unable to update your account information at this time.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        
    }
    
    /**
     * Handle the user's request to update a user's account by
     * forwarding the account information in the UpdateAccountPanel to the tutor.
     *
     * @param evt ignored
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        Account account = buildAuthenticationAccount();
        
        if(account == null) return; // The user cancelled the prompt
        
        TutorReply passwordReply = verifyPasswordRequest(account);
        if(!passwordReply.getStatus().equals("Authenticated")) {
            invalidPasswordMessage();
            return;
        }
        
        TutorReply updateReply = makeUpdateRequest(account);
        displayResultMessage(updateReply.getStatus());
        MainFrame.instance().displayView(MainFrame.ViewName.TUTOR);
    }
}