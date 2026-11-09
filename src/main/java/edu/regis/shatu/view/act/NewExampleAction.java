/*
 * SHATU: SHA-256 Tutor
 * 
 *  (C) Johanna & Richard Blumenthal, All rights reserved
 * 
 *  Unauthorized use, duplication or distribution without the authors'
 *  permission is strictly prohibited.
 * 
 *  Unless required by applicable law or agreed to in writing, this
 *  software is distributed on an "AS IS" basis without warranties
 *  or conditions of any kind, either expressed or implied.
 */
package edu.regis.shatu.view.act;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.regis.shatu.err.IllegalArgException;
import edu.regis.shatu.model.Account;
import edu.regis.shatu.model.aol.NewExampleRequest;
import edu.regis.shatu.model.aol.PendingTask;
import edu.regis.shatu.svc.ClientRequest;
import edu.regis.shatu.svc.ServerRequestType;
import edu.regis.shatu.svc.SvcFacade;
import edu.regis.shatu.svc.TutorReply;
import edu.regis.shatu.view.GuiController;
import edu.regis.shatu.view.MainFrame;
import edu.regis.shatu.view.UserRequestView;

/**
 * An (MVC) controller handling a GUI gesture representing a user's request for
 * a new problem to solve via selecting a new panel or requesting a new example
 * directly. A client request for a new example is sent to the tutor who will
 * reply with the new Task and Step to be completed along with their associated
 * data.
 *
 * If successful, the current view will be updated with the data associated with
 * the tutor's reply.
 *
 * @author Oskar Thiede
 */
public class NewExampleAction extends ShaTuGuiAction {

    /**
     * Exceptions occurring in this class are also logged to this logger.
     */
    private static final Logger LOGGER
            = Logger.getLogger(NewExampleAction.class.getName());

    /**
     * The single instance of this sign-in action.
     */
    private static final NewExampleAction SINGLETON;

    /**
     * Create the singleton for this action, which occurs when this class is
     * loaded by the Java class loaded, as a result of the class being
     * referenced by executing SignInAction.instance() in the
     * initializeComponents() method of the SplashPanel class.
     */
    static {
        SINGLETON = new NewExampleAction();
    }

    /**
     * Return the singleton instance of this sign-in action.
     *
     * @return
     */
    public static NewExampleAction instance() {
        return SINGLETON;
    }

    /**
     * Initialize action with the "Sign In" text and set its text.
     */
    private NewExampleAction() {
        super("New Example");
        putValue(SHORT_DESCRIPTION, "Next Example");
        putValue(MNEMONIC_KEY, KeyEvent.VK_N);
    }

    /**
     * Handle the user's request for a new example by sending it to the tutor.
     *
     * If successful, the current view is updated with the contents of the new
     * example (and associated task) in the reply from the tutor.
     *
     * @param evt ignored
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        System.out.println("actionPerformed");
        Gson gson = getGsonPretty();

        Account account = MainFrame.instance().getAccount();

        //Catches a possible IllegalArgumentException thrown by the 
        try {
            //Get the view that originated the NewExampleRequest
            UserRequestView exView = GuiController.instance().getStepView().getUserRequestView();

            //Call the overridden newRequest() method to generate an appropriate
            //request to the tutor based on the current view
            NewExampleRequest ex = exView.newRequest();

            //Construct the request with the users data and NewExampleRequest
            //returned by the newRequest() method
            ClientRequest request = new ClientRequest(ServerRequestType.NEW_EXAMPLE);
            request.setUserId(account.getUserId());
            request.setSecurityToken(MainFrame.instance().getModel().getSecurityToken());
            request.setData(gson.toJson(ex));
            
            //Send the request to the tutor and save the reply
            TutorReply reply = SvcFacade.instance().tutorRequest(request);

            switch (reply.getStatus()) {
                case ":ERR":
                    MainFrame.instance().displayError(reply.getErrorMessage());
                    break;
                default:
                    applyTutoringMode(reply);
                    //If the status was not an error, we can update the model and the
                    //view with the new task sent by the tutor

                    exView.setCurrentTask(gson.fromJson(reply.getData(), PendingTask.class));
            }
        } catch (IllegalArgException e) {
            System.out.println("Illegal arg exception " + e);
        } catch (IllegalArgumentException e) {
            MainFrame.instance().displayError(e.getMessage());
        }
        
    }
}
