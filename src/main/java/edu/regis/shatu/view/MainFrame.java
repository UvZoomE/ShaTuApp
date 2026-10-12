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
package edu.regis.shatu.view;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

import edu.regis.shatu.model.Account;
import edu.regis.shatu.model.TutoringSession;
import edu.regis.shatu.view.act.SaveSessionAction;

/**
 * The primary GUI window in the ShaTu application.
 *
 * Most of the display functionality is handled by child component views.
 *
 * @author rickb
 */
public class MainFrame extends JFrame implements WindowListener {

    /** The singleton instance of this frame. */
    private final static MainFrame SINGLETON;

    // Invoked when this class is loaded
    static {
        SINGLETON = new MainFrame();
    }

    /**
     * Return the singleton instance of this frame.
     *
     * @return the MainFrame singleton
     */
    public static MainFrame instance() {
        return SINGLETON;
    }

    /**
     * The size of this frame will the size of the user's screen minus this
     * screen size inset.
     */
    private static final int SCREEN_SIZE_INSET = 50;
    
    /** Allowed consecutive illegal passwords before the user is locked out. */
    public static final int MAX_SIGNIN_ATTEMPTS = 3;

    /** The SHA tutoring session displayed in this frame. */
    private TutoringSession model;

    /** The view currently displayed in the card panel. */
    private ViewName displayedView;

    /**
     * The card panel displayed in the content pane of this frame, which is
     * capable of displaying various views (see displayedView).
     */
    private JPanel cardPanel;

    /** The tutoring session view. */
    private TutoringSessionView tutorSessionView;

    /** The splash panel, which also allows signing in. */
    private SplashPanel splashPanel;

    private NewAccountPanel newAccountPanel;

    private ResetPasswordPanel resetPasswordPanel;

    private ForgotPasswordPanel forgotPasswordPanel;
    
    private DashboardPanel dashboardPanel;
    
    private UpdateAccountPanel updateAccountPanel;
    
    /**
     * The number of consecutive illegal passwords attempted by the current
     * user attempting to login (see MAX_SIGNIN_ATTEMPTS).
     */
    protected int signInAttempts = 0;

    /** Timer for inactivity tracking. */
    private Timer inactivityTimer;
    
    /**
     * Initialize and layout the child components displayed in this frame.
     */
    private MainFrame() {
        super("ShaTut");

        // Get screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Calculate the initial size
        int width = (int) (screenSize.width * 0.5);
        int height = (int) (screenSize.height * 0.9);
        setSize(width, height);

        setJMenuBar(new ShaTuMenuBar());

        initializeComponents();
        layoutComponents();

        addWindowListener(this);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // see windowClosing()

        displayView(ViewName.SPLASH);
        
        setVisible(true);
    }

    /**
     * Update the current model with changes made in this frame and return it.
     *
     * @return the TutoringSession model
     */
    public TutoringSession getModel() {
        updateModel();

        return model;
    }

    /**
     * Display the given model in the tutoring session view of this frame.
     *
     * @param model a TutoringSession model.
     */
    public void setModel(TutoringSession model) {
        if (model == null) {
            logout();
            return;
        }

        this.model = model;

        // Distribute the session model to session-specific views
        tutorSessionView.setModel(model); 
        dashboardPanel.setModel(model);
        
        // Extract and distribute the account model to account-specific views
        Account account = new Account(model.getStudent().getAccount());
        setAccountModels(account);
        
        displayView(ViewName.TUTOR);
    }

    /**
     * Handles user logout functionality, clears session data, and resets views.
     */
    public void logout() {
        this.model = null;
        
        // Clear session views
        tutorSessionView.setModel(null); 
        dashboardPanel.setModel(null);
        
        // Reset account views with a fresh, empty Account object
        setAccountModels(new Account());
        
        // Clear any lingering data in the UI text fields
        updateAccountPanel.clearFields();
        
        displayView(ViewName.SPLASH);
    }

    /**
     * Helper method to centralize the updating of all Account-related panels.
     * 
     * @param account the Account model to distribute
     */
    private void setAccountModels(Account account) {
        splashPanel.setModel(account);
        forgotPasswordPanel.setModel(account);
        resetPasswordPanel.setModel(account);
        updateAccountPanel.setModel(account);
    }

    /**
     * Display the given view in this frame.
     *
     * @param name the name of the view to display.
     */
    public void displayView(ViewName name) {
        CardLayout cl = (CardLayout) cardPanel.getLayout();

        cl.show(cardPanel, name.toString());

        displayedView = name;
    }
    
    /**
     * Assumes the tutoring session view is displayed.
     * 
     * @param selection 
     */
    public void displayStep(StepSelection selection) {
        if (displayedView != ViewName.TUTOR)
            displayView(ViewName.TUTOR);
        
        tutorSessionView.displayStep(selection);
    }
    
    /**
     * Returns the current account, which depends on the view currently being
     * displayed.

     * TODO: Each view contains its own Account object. Determine if it's necessary for
     * this class to fetch the Accounts from different views, or if the Accounts
     * should be fetched directly from the views themselves. Also, is the Account
     * ever different from view to view?

     * @return the current user's Account
     */
    public Account getAccount() {
        
        switch (displayedView) {
            case DASHBOARD:
            case TUTOR:
                if (model != null) {
                    return model.getStudent().getAccount();
                }
                
            case SPLASH:
                return splashPanel.getModel();
                
            case FORGOT_PASSWORD:
                return forgotPasswordPanel.getModel();
                
            case RESET_PASSWORD:
                return resetPasswordPanel.getModel();
            
            case UPDATE_ACCOUNT:
                return updateAccountPanel.getModel();
        
            default:
                return newAccountPanel.getModel();     
        }
    }
    
    /**
     * Check if a user is currently logged in.
     * @return true if a user session exists, false otherwise
     */
    public boolean isUserLoggedIn() {
        return model != null && model.getStudent() != null && model.getStudent().getAccount() != null;
    }
    
     /**
     * Displays to the user the result of an invalid password during sign-in.
     * 
     * Handles an invalid password response from a SignInAction keeping track
     * of the number of user attempts thus far.
     */
    public void invalidPass() {
        if (signInAttempts < MAX_SIGNIN_ATTEMPTS) {
           
            signInAttempts++;
            String msg = "Invalid Password attempt " + 
                         String.valueOf(signInAttempts) + " of " + 
                         MAX_SIGNIN_ATTEMPTS;
            
            JOptionPane.showMessageDialog(this, msg, "SignIn Error", JOptionPane.ERROR_MESSAGE);
            
        } else {
            String msg = "You exceeded the max number of sign in attempts\n" +
                         "Please contact the ShaTu administrator";
            
            JOptionPane.showMessageDialog(this, msg, "SignIn Error", JOptionPane.ERROR_MESSAGE);

            // ToDo: What?
            this.dispose();
            System.exit(1);
        }
    }
    
    /**
     * Reset the text fields in the new account panel to the empty string.
     */
    public void clearNewAccountPanel() {
        newAccountPanel.clearFields();
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        // SHAT-362: Adding save functionality.
        if (isUserLoggedIn()) {
            SaveSessionAction.instance().actionPerformed(null);
        }
        this.dispose();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    /**
     * Create the child components used in this frame.
     */
    private void initializeComponents() {
        cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout());

        dashboardPanel = new DashboardPanel();
        forgotPasswordPanel = new ForgotPasswordPanel();
        newAccountPanel = new NewAccountPanel();
        resetPasswordPanel = new ResetPasswordPanel();
        splashPanel = new SplashPanel();
        tutorSessionView = new TutoringSessionView();
        updateAccountPanel = new UpdateAccountPanel();
    }

    /**
     * Adds the child component panels/views to the card layout, which is then
     * set as the content pane of this frame.
     */
    private void layoutComponents() {
        cardPanel.add(tutorSessionView, ViewName.TUTOR.toString());
        cardPanel.add(splashPanel, ViewName.SPLASH.toString());
        cardPanel.add(newAccountPanel, ViewName.NEW_ACCOUNT.toString());
        cardPanel.add(resetPasswordPanel, ViewName.RESET_PASSWORD.toString());
        cardPanel.add(forgotPasswordPanel, ViewName.FORGOT_PASSWORD.toString());
        cardPanel.add(dashboardPanel, ViewName.DASHBOARD.toString());
        cardPanel.add(updateAccountPanel, ViewName.UPDATE_ACCOUNT.toString());

        setContentPane(cardPanel);
    }

    /**
     * Update the current model with any changes made in this frame's view.
     * TODO: implement how to update the model
     */
    private void updateModel() {
        
        if(displayedView == ViewName.DASHBOARD || displayedView == ViewName.TUTOR){
            TutoringSession current = tutorSessionView.getModel();
            if(current != null){
                model = current;
            }
        }
    }

    /**
     * Display the current model in this frame's view.
     */
    private void updateView() {
    }

    /**
     * The sub-views that can be displayed in the MainFrame;
     */
    public enum ViewName {
        DASHBOARD,
        FORGOT_PASSWORD,
        NEW_ACCOUNT,
        RESET_PASSWORD,
        SPLASH,
        TUTOR,
        UPDATE_ACCOUNT;
    }
}