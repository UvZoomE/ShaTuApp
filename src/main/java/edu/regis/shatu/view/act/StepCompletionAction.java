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

/*
* Corrected misspelling per SHAT-218 John Hennessey 23 Feb 2025
*/

package edu.regis.shatu.view.act;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.regis.shatu.err.IllegalArgException;
import edu.regis.shatu.model.Account;
import edu.regis.shatu.model.StepCompletion;
import edu.regis.shatu.model.Task;
import edu.regis.shatu.model.aol.PendingTask;
import edu.regis.shatu.model.aol.ProblemType;
import edu.regis.shatu.model.aol.StepSubType;
import edu.regis.shatu.model.steps.Step;
import edu.regis.shatu.model.steps.StepCompletionReply;
import edu.regis.shatu.svc.ClientRequest;
import edu.regis.shatu.svc.ServerRequestType;
import edu.regis.shatu.svc.SvcFacade;
import edu.regis.shatu.svc.TutorReply;
import edu.regis.shatu.view.GuiController;
import edu.regis.shatu.view.MainFrame;
import edu.regis.shatu.view.StepSelection;
import edu.regis.shatu.view.UserRequestView;

/**
 * An (MVC) controller handling a GUI gesture representing a user's request to
 * completed the current step, which is the finest grained interface gesture
 * a user can perform that is sent to the tutor.
 *
 * If successful, the appropriate view will be updated with the tutor's reply.
 * 
 * @author rickb
 */
public class StepCompletionAction extends ShaTuGuiAction {

    /**
     * Exceptions occurring in this class are also logged to this logger.
     */
    private static final Logger LOGGER = Logger.getLogger(StepCompletionAction.class.getName());

    /**
     * The single instance of this sign-in action.
     */
    private static final StepCompletionAction SINGLETON;

    /**
     * Create the singleton for this action, which occurs when this class is
     * loaded by the Java class loaded, as a result of the class being
     * referenced by executing SignInAction.instance() in the
     * initializeComponents() method of the SplashPanel class.
     */
    static {
        SINGLETON = new StepCompletionAction();
    }

    /**
     * Return the singleton instance of this sign-in action.
     *
     * @return
     */
    public static StepCompletionAction instance() {
        return SINGLETON;
    }

    /**
     * Initialize action with the "Sign In" text and set its text.
     */
    private StepCompletionAction() {
        super("Check");
        this.setEnabled(false);
        putValue(SHORT_DESCRIPTION, "Check Example");
        putValue(MNEMONIC_KEY, KeyEvent.VK_C);
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
        Gson gson = getGsonPretty();
        Account account = MainFrame.instance().getAccount();
        // Catches a possible IllegalArgumentException thrown by the
        // getUserRequestView() method
        try {
            // Get the current view that initiated the StepCompletionRequest
            UserRequestView exView = GuiController.instance().getStepView().getUserRequestView();
            // Call the overridden newRequest() method to generate an appropriate
            // request to the tutor based on the current view
            StepCompletion ex = exView.stepCompletion();
            // Construct the request with the users data and NewExampleRequest
            // returned by the newRequest() method
            ClientRequest request = new ClientRequest(ServerRequestType.COMPLETED_STEP);
            request.setUserId(account.getUserId());
            request.setSecurityToken(MainFrame.instance().getModel().getSecurityToken());
            request.setData(gson.toJson(ex));

            // Send the request to the tutor and save the reply
            TutorReply reply = SvcFacade.instance().tutorRequest(request);

            switch (reply.getStatus()) {
                case ":ERR":
                    MainFrame.instance().displayError(reply.getErrorMessage());
                    break;

                default:
                    applyTutoringMode(reply);
                    PendingTask pendingTask = gson.fromJson(reply.getData(), PendingTask.class);
                    Task task = pendingTask.getTask();
                    // Task task = gson.fromJson(reply.getData(), Task.class);

                    if (task.getType() == ProblemType.STEP_COMPLETION_REPLY) {
                        String selection1 = "Move on to Next Task";
                        String selection2 = "Try Same Problem Again";
                        String selection3 = "Try a Similar Problem";
                        String selection4 = "Show the correct Answer";

                        Step step = pendingTask.getCurrentStep().getStep();
                       
                        if (step.getSubType() == StepSubType.STEP_COMPLETION_REPLY) {
                            System.out.println("StepData: " + step.getData());
                            StepCompletionReply stepReply = gson.fromJson(step.getData(), StepCompletionReply.class);
  
                            if (stepReply.isCorrect()) {
                                if (task.getType() != ProblemType.STEP_COMPLETION_REPLY) {
                                    loadReturnedTask(task);
                                } else if (stepReply.isNewTask()) {
                                    String prompt = "Congratulations, the answer you submitted is correct. "
                                            + "As I believe you've mastered this outcome, I suggest moving on to a different task. "
                                            + "However, if you'd like you can try a similar problem again.";
                                    String[] options = { selection1, selection3 };
                                    int choice = JOptionPane.showOptionDialog(MainFrame.instance(),
                                            prompt, "Tutor Reply", 0, 3, null, options, options[0]);

                                    switch (choice) {
                                        case 0 -> {
                                            System.out.println("Next Task returned by tutor");
                                            // Do nothing here yet if tutor still returned only a completion reply.
                                            // Once ShaTuTutor returns the real next PendingTask, this branch can
                                            // also call loadReturnedTask(task).
                                        }
                                        default -> {
                                            System.out.println("try similar problem");
                                            NewExampleAction.instance().actionPerformed(null);
                                        }
                                    }
                                } else {
                                    String prompt = "Congratulations, the answer you submitted is correct. "
                                            + "Keep up the good work to complete this task!";
                                    String[] options = { selection3 };
                                    JOptionPane.showOptionDialog(MainFrame.instance(),
                                            prompt, "Tutor Reply", 0, 3, null, options, options[0]);
                                    NewExampleAction.instance().actionPerformed(null);
                                }
                            } else {
                                String prompt = "Unfortunately, your answer was incorrect. Please try agian.";
                                String[] options = { selection2, selection3, selection4 };
                                int choice = JOptionPane.showOptionDialog(MainFrame.instance(),
                                        prompt, "Tutor Reply", 0, 3, null, options, options[0]);

                                switch (choice) {
                                    case 1 -> {
                                        System.out.println("try similar problem");
                                        NewExampleAction.instance().actionPerformed(null);
                                    }
                                    case 2 -> {
                                        System.out.println("show answer");
                                        recordCorrectAnswerRequest(gson, account, ex);
                                        JOptionPane.showMessageDialog(MainFrame.instance(),
                                                stepReply.getCorrectAnswer(), "Tutor Reply",
                                                JOptionPane.INFORMATION_MESSAGE);
                                        NewExampleAction.instance().actionPerformed(null);
                                    }
                                    default -> System.out.println("try again");
                                }
                            }

                        }
                    }

                    //exView.setCurrentTask(task);

            }
        } catch (IllegalArgException e) {
            System.out.println("Illegal arg exception " + e);
        }
    }

    
    /**
    * Routes the client to the correct view based on the real task returned
    * by the tutor after a successful completion.
    *
    * @param task the next task returned by the tutor
    */
   private void loadReturnedTask(Task task) {
       switch (task.getType()) {
           case ASCII_ENCODE -> StepSelection.ENCODE.getLabel().select();
           case ADD_ONE_BIT -> StepSelection.ADD1.getLabel().select();
           case PAD_ZEROS -> StepSelection.PAD.getLabel().select();
           case ADD_MSG_LENGTH -> StepSelection.LENGTH.getLabel().select();
           case PREPARE_SCHEDULE -> StepSelection.PREPARE.getLabel().select();
           case INITIALIZE_VARS -> StepSelection.INIT_VARS.getLabel().select();
           case COMPRESS_ROUND -> StepSelection.COMPRESS.getLabel().select();
           case ROTATE_BITS -> StepSelection.ROTATE_BITS.getLabel().select();
           case SHIFT_BITS -> StepSelection.SHIFT_RIGHT.getLabel().select();
           case XOR_BITS -> StepSelection.XOR.getLabel().select();
           case ADD_BITS -> StepSelection.ADD_TWO_BIT.getLabel().select();
           case CHOICE_FUNCTION -> StepSelection.CHOICE_FUNCTION.getLabel().select();
           case MAJORITY_FUNCTION -> StepSelection.MAJ_FUNCTION.getLabel().select();
           case SHA_ZERO -> StepSelection.SHA_ZERO.getLabel().select();
           case SHA_ONE -> StepSelection.SHA_ONE.getLabel().select();
           default -> System.out.println("Unhandled task type: " + task.getType());
       }
   }
    
    /**
     * Notify the tutor that the student requested to see the correct answer so the
     * event can be persisted.
     */
    private void recordCorrectAnswerRequest(Gson gson, Account account, StepCompletion completion) {
        try {
            ClientRequest correctAnswerRequest = new ClientRequest(ServerRequestType.REQUEST_CORRECT_ANSWER);
            correctAnswerRequest.setUserId(account.getUserId());
            correctAnswerRequest.setSecurityToken(MainFrame.instance().getModel().getSecurityToken());
            correctAnswerRequest.setData(gson.toJson(completion));

            TutorReply recordReply = SvcFacade.instance().tutorRequest(correctAnswerRequest);

            if (!":Success".equals(recordReply.getStatus())) {
                LOGGER.warning("Failed to record correct answer request: " + recordReply.getStatus());
                MainFrame.instance().displayError(recordReply.getErrorMessage());
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Unable to record correct answer request", ex);
        }
    }
}
