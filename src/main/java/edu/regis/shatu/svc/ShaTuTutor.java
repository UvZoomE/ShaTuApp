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
package edu.regis.shatu.svc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.regis.shatu.dao.SessionDAO;
import edu.regis.shatu.dao.DeveloperModeDAO;
import edu.regis.shatu.err.NonRecoverableException;
import edu.regis.shatu.err.ObjDuplicateException;
import edu.regis.shatu.err.ObjNotFoundException;
import edu.regis.shatu.model.Account;
import edu.regis.shatu.model.Course;
import edu.regis.shatu.model.KnowledgeComponent;
import edu.regis.shatu.model.KnowledgeComponentKind;
import edu.regis.shatu.model.StepCompletion;
import edu.regis.shatu.model.Student;
import edu.regis.shatu.model.Task;
import edu.regis.shatu.model.TutoringSession;
import edu.regis.shatu.model.Unit;
import edu.regis.shatu.model.StudentModelFieldKind;
import edu.regis.shatu.model.aol.Assessment;
import edu.regis.shatu.model.aol.AssessmentLevel;
import edu.regis.shatu.model.aol.NewExampleRequest;
import edu.regis.shatu.model.aol.PendingStep;
import edu.regis.shatu.model.aol.PendingTask;
import edu.regis.shatu.model.aol.Problem;
import edu.regis.shatu.model.aol.ProblemType;
import static edu.regis.shatu.model.aol.ProblemType.ADD_BITS;
import static edu.regis.shatu.model.aol.ProblemType.ADD_MSG_LENGTH;
import static edu.regis.shatu.model.aol.ProblemType.ADD_ONE_BIT;
import static edu.regis.shatu.model.aol.ProblemType.ASCII_ENCODE;
import static edu.regis.shatu.model.aol.ProblemType.CHOICE_FUNCTION;
import static edu.regis.shatu.model.aol.ProblemType.COMPRESS_ROUND;
import static edu.regis.shatu.model.aol.ProblemType.INITIALIZE_VARS;
import static edu.regis.shatu.model.aol.ProblemType.MAJORITY_FUNCTION;
import static edu.regis.shatu.model.aol.ProblemType.PAD_ZEROS;
import static edu.regis.shatu.model.aol.ProblemType.PREPARE_SCHEDULE;
import static edu.regis.shatu.model.aol.ProblemType.ROTATE_BITS;
import static edu.regis.shatu.model.aol.ProblemType.SHA_ONE;
import static edu.regis.shatu.model.aol.ProblemType.SHA_ZERO;
import static edu.regis.shatu.model.aol.ProblemType.SHIFT_BITS;
import static edu.regis.shatu.model.aol.ProblemType.XOR_BITS;
import edu.regis.shatu.model.aol.StepSubType;
import static edu.regis.shatu.model.aol.StepSubType.ENCODE_ASCII;
import static edu.regis.shatu.model.aol.StepSubType.ENCODE_BINARY;
import static edu.regis.shatu.model.aol.StepSubType.ENCODE_HEX;
import static edu.regis.shatu.model.aol.StepSubType.INFO_MESSAGE;
import edu.regis.shatu.model.aol.StudentModel;
import edu.regis.shatu.model.aol.TutoringMode;
import static edu.regis.shatu.model.aol.TutoringMode.DO_ONE;
import static edu.regis.shatu.model.aol.TutoringMode.SEE_ONE;
import static edu.regis.shatu.model.aol.TutoringMode.TEACH_ONE;
import edu.regis.shatu.model.steps.Step;
import edu.regis.shatu.model.steps.EncodeAsciiStep;
import edu.regis.shatu.model.steps.StepCompletionReply;
import edu.regis.shatu.objectives.*;
import java.util.HashMap;

/**
 * The ShaTu tutor, which implements the tutoring service.
 *
 * @author rickb
 */
public class ShaTuTutor implements TutorSvc {

    /**
     * The id of the default course taught by the this tutor.
     */
    private static final int DEFAULT_COURSE_ID = 1;
    /**
     * The maximum number of characters allowed for encoding a example ASCII
     * encoding request from the student.
     */
    private static final int MAX_ASCII_SIZE = 20;

    private static final int MAX_BITS_SIZE = 32;

    /**
     * Handler for logging non-exception messages from this class versus thrown
     * exception, which are logged by the exception.
     */
    private static final Logger LOGGER = Logger.getLogger(ShaTuTutor.class.getName());

    /**
     * The current tutoring session, which contains information on the current
     * Student, StudentModel, Course, Task, Step, etc.
     */
    private TutoringSession session;

    private Student student;
    private StudentModel studentModel;

    /**
     * Convenience reference to the current gson object.
     */
    private Gson gson;

    private Objective currObjective;

    /**
     * Initialize the tutor singleton (a NoOp).
     */
    public ShaTuTutor() {
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Handles incoming tutor requests from the GUI client.
     *
     * This method converts the request type into a matching ShaTuTutor method name,
     * verifies the user's security token when required, reloads the active student
     * model and tutoring session, and then dispatches the request using reflection.
     *
     * Requests such as ":SignIn" and ":CreateAccount" do not require an existing
     * session. Requests such as ":CompletedStep", ":RequestHint", and
     * ":SaveSession" require a valid security token before being processed.
     *
     * @param request the ClientRequest containing the request type, user id,
     * security token, and JSon encoded request data
     * @return a TutorReply containing the result of the requested tutor operation,
     * or a TutorReply with status ":ERR" if the request cannot be processed
     */
    @Override
    public TutorReply request(ClientRequest request) {
        // Uses reflection to invoke a method derived from the request name in
        // the client request (e.g., ":SignIn" invokes "signIn(...)").
        Logger.getLogger(ShaTuTutor.class.getName()).log(Level.INFO, request.getRequestType().getRequestName());

        // Parse the method name (e.g., ":SignIn" -> "signIn")
        String methodName = parseMethodName(request.getRequestType().getRequestName());

        // TODO: Consider making this switch into its own method.
        // Most methods require verifying the given security token with the
        // one current known in the DB for the given user.
        switch (methodName) {
            case "completedStep":
            case "completedTask":
            //case "getTask":
            case "newExample":
            case "requestHint":
            case "requestCorrectAnswer":
            case "resetPassword":
            case "updateAccount":
            case "saveSession":
                String userId = request.getUserId();
                try {
                    if (verifySession(userId, request.getSecurityToken())) {

                        Account account = ServiceFactory.findAccountSvc().retrieve(userId);
                        student = new Student(account);

                        try {
                            StudentModelSvc stuModSvc = ServiceFactory.findStudentModelSvc();
                            studentModel = stuModSvc.retrieve(userId);
                            student.setStudentModel(studentModel);

                        } catch (ObjNotFoundException ex) {
                            TutorReply reply = new TutorReply(":ERR");
                            reply.setData("Student model not found for: " + userId);
                            return reply;
                        }

                        session = ServiceFactory.findSessionSvc().retrieve(student.getAccount().getUserId());

                    } else {
                        TutorReply reply = new TutorReply(":ERR");
                        reply.setData("Illegal Security Token");
                        return reply;
                    }

                } catch (ObjNotFoundException ex) {
                    return createError("No session exists for user: " + userId, ex);
                } catch (NonRecoverableException ex) {
                    return createError(ex.toString(), ex);
                }

                String msg = "Session verified for " + request.getUserId();
                Logger.getLogger(ShaTuTutor.class.getName()).log(Level.INFO, msg);
                break;

            default: // e.g., signIn itself, newAccount
                Logger.getLogger(ShaTuTutor.class.getName()).log(Level.INFO, "No token verification required");
        }

        // Security token has been verified or not required (e.g., signIn,
        // createAccount).
        try {
            Method method = getClass().getMethod(methodName, String.class);

            return (TutorReply) method.invoke(this, request.getData());

        } catch (NoSuchMethodException ex) {
            return createError("Tutor received an unknown request type: " + request.getRequestType().getRequestName(),
                    ex);
        } catch (SecurityException ex) {
            return createError("ShaTuTutor_ERR_2", ex);
        } catch (IllegalAccessException ex) {
            return createError("ShaTuTutor_ERR_3", ex);
        } catch (IllegalArgumentException ex) {
            return createError("ShaTuTutor_ERR_4", ex);
        } catch (InvocationTargetException ex) {
            // The handler threw, e.g., the database is down. Send its message
            // so the user sees more than an error code.
            Exception cause = (Exception) ex.getCause();
            return createError("Unable to complete " + methodName + ": " + cause.getMessage(), cause);
        }
    }

    /**
     * Converts a request name (e.g., ":SignIn") into a method name (e.g., "signIn").
     * 
     * @param requestName The raw request string from the client
     * @return The formatted method name for reflection
     */
    private String parseMethodName(String requestName) {
        if (requestName == null || requestName.length() < 2) {
            return requestName; 
        }
        // Grabs the character after the colon, lowercases it, and appends the rest of the string
        return Character.toLowerCase(requestName.charAt(1)) + requestName.substring(2);
    }

    /**
     * Creates a new student account.
     *
     * This method handles ":CreateAccount" requests from the GUI client.
     *
     * @param jsonAcct a JSon encoded Account object
     * @throws NonRecoverableException perhaps see getCause().getErrorCode().
     * @return a TutorReply if successful the status is "Created", otherwise the
     * status is ":ERR".
     */
    public TutorReply createAccount(String jsonAcct) throws NonRecoverableException {
        Account acct = gson.fromJson(jsonAcct, Account.class);

        int courseId = DEFAULT_COURSE_ID; // Currently only one course

        try {
            ServiceFactory.findAccountSvc().create(acct);
        } catch (ObjDuplicateException e) {
            return createError(String.format("Account %s exists", acct.getUserId()), null);
        }

        try {
            Course course = ServiceFactory.findCourseSvc().retrieve(courseId);

            student = createStudent(acct, course);

            createSession(student, course);

            return new TutorReply("Created");

        } catch (ObjNotFoundException ex) {
            return createError("Unknown course: " + courseId, null);
        }

        // try {
        // }
        // catch (IllegalArgException ex) { // The account already exists
        // return new TutorReply("IllegalUserId");
        // }
    }

    /**
     * Updates an existing student's account information in the database.
     *
     * This method handles ":UpdateAccount" requests from the GUI client.
     *
     * TODO: Database tables (Account & Student) are updated independently.
     * Determine if this should be done with a single transaction. Delete this
     * TODO when a decision is made and implemented.
     *
     * @param jsonAcct a JSON representation of an account
     * @return a TutorReply indicating "Success" or ":ERR"
     */
    public TutorReply updateAccount(String jsonAcct) {
        Account account = gson.fromJson(jsonAcct, Account.class);
        Student student = new Student(account);

        try {
            ServiceFactory.findAccountSvc().update(account);    // Updates Account
            ServiceFactory.findStudentModelSvc().update(student);   // Updates Student
            return new TutorReply("Success");
        } catch (ObjNotFoundException ex) {
            return createError("Account not found for: " + account.getUserId(), ex);
        } catch (NonRecoverableException ex) {
            return createError("Unable to update account: " + ex.getMessage(), ex);
        }
    }

    /**
     * Verifies the user is in the database and the security question and answer
     * match.
     *
     * This method handles ":VerifyUser" requests from the GUI client.
     *
     * @param jsonAcct a JSon encoded Account object
     * @return a TutorReply if successful the status is "Verified", otherwise
     * the status is ":ERR".
     * @throws edu.regis.shatu.err.NonRecoverableException
     */
    public TutorReply verifyUser(String jsonAcct) throws NonRecoverableException {
        Account requestAcct = gson.fromJson(jsonAcct, Account.class);
        AccountSvc acctSvc = ServiceFactory.findAccountSvc();

        if (!acctSvc.exists(acctSvc.getPrimaryKey(), requestAcct.getUserId())) {
            return new TutorReply("IllegalUserId");
        }

        try {
            Account dbAcct = acctSvc.retrieve(requestAcct.getUserId());

            if ((dbAcct.getSecurityAnswer().equals(requestAcct.getSecurityAnswer()))
                    && (dbAcct.getSecurityQuestion() == requestAcct.getSecurityQuestion())) {

                student = new Student(dbAcct);

                try {
                    StudentModelSvc stuModSvc = ServiceFactory.findStudentModelSvc();
                    studentModel = stuModSvc.retrieve(dbAcct.getUserId());
                    student.setStudentModel(studentModel);
                } catch (ObjNotFoundException e) {
                    student = createStudent(dbAcct, ServiceFactory.findCourseSvc().retrieve(DEFAULT_COURSE_ID));
                }

                // Check if session already exists before creating
                SessionSvc sessionSvc = ServiceFactory.findSessionSvc();

                try {
                    session = sessionSvc.retrieve(dbAcct.getUserId()); // already exists
                } catch (ObjNotFoundException e) {
                    // session does not exist, create it
                    session = createSession(student, ServiceFactory.findCourseSvc().retrieve(DEFAULT_COURSE_ID));
                }

                TutorReply reply = new TutorReply("Verified");
                reply.setData("\"" + session.getSecurityToken() + "\"");
                return reply;

            } else {
                return new TutorReply("InvalidAnswer");
            }

        } catch (ObjNotFoundException e) {
            return new TutorReply("UnknownUser");
        } catch (NonRecoverableException ex) {
            return createError("Unable to verify user: " + ex.getMessage(), ex);
        }
    }

    /**
     * Checks a user-provided password against the password stored in the
     * database for the user's account.
     *
     * This method handles ":VerifyPassword" requests from the GUI client.
     *
     * @param jsonAcct a JSON encoded Account object
     * @return a TutorReply object
     */
    public TutorReply verifyPassword(String jsonAcct) {
        Account requestAcct = gson.fromJson(jsonAcct, Account.class);

        try {
            Account dbAcct = ServiceFactory.findAccountSvc().retrieve(requestAcct.getUserId());

            if (dbAcct.getPassword().equals(requestAcct.getPassword())) {
                return new TutorReply("Authenticated");
            } else {
                return new TutorReply("InvalidPassword");
            }
        } catch (ObjNotFoundException e) {
            return createError("Account not found for: " + requestAcct.getUserId(), null);
        } catch (NonRecoverableException ex) {
            return createError("Unable to verify password: " + ex.getMessage(), ex);
        }
    }

    /**
     * Resets password for user currently in database.
     *
     * This method handles ":ResetPassword" requests from the GUI client.
     *
     * @param jsonAcct a JSon encoded Account object
     * @return a TutorReply if successful the status is "PasswordReset",
     * otherwise the status is ":ERR".
     * @throws edu.regis.shatu.err.NonRecoverableException
     */
    public TutorReply resetPassword(String jsonAcct) throws NonRecoverableException {
        Account acct = gson.fromJson(jsonAcct, Account.class);
        AccountSvc acctSvc = ServiceFactory.findAccountSvc();

        if (!acctSvc.exists(acctSvc.getPrimaryKey(), acct.getUserId())) {
            return new TutorReply("IllegalUserId");
        }

        try {
            // Retrieve full DB account so we don't lose names or other info
            Account dbAcct = acctSvc.retrieve(acct.getUserId());

            // Only update the changed fields
            dbAcct.setPassword(acct.getPassword());

            // Only set security question/answer if needed
            dbAcct.setSecurityQuestion(acct.getSecurityQuestion());
            dbAcct.setSecurityAnswer(acct.getSecurityAnswer());

            acctSvc.update(dbAcct);

            return new TutorReply("PasswordReset");

        } catch (ObjNotFoundException ex) {
            return new TutorReply("IllegalUserId");
        }
    }

    
    

    /**
    * Applies a developer mode tutoring override for the signed-in user.
    *
    * This method checks whether the current user has a developer mode tutoring
    * mode saved in the database. If an override exists, the current session's
    * tutoring mode is replaced with that developer-selected mode.
    *
    * Developer mode is treated as optional testing behavior. If the override
    * cannot be loaded, the failure is logged and sign-in continues normally.
    *
    * @param userId the user id of the student currently signing in
    */
    private void applyDeveloperModeOverride(String userId) {
        try {
            DeveloperModeDAO dao = new DeveloperModeDAO();
            TutoringMode mode = dao.retrieveTutoringMode(userId);

            if (mode == null) {
                System.out.println("[ShaTuTutor.java] - [DeveloperMode] - no override for userId=" + userId);
                return;
            }

            session.setTutoringMode(mode);

            System.out.println("[ShaTuTutor.java] - [DeveloperMode] - tutoringMode override applied=" + mode);

        } catch (NonRecoverableException ex) {
            Logger.getLogger(ShaTuTutor.class.getName()).log(
                    Level.WARNING,
                    "Developer mode override failed. Login will continue normally.",
                    ex
            );
        }
    }
    
    /**
    * Attempts to sign a student in.
    *
    * This method handles ":SignIn" requests from the GUI client. It verifies the
    * submitted user id and password, retrieves or creates the user's tutoring
    * session, restores the student and student model data, applies any developer
    * mode override, and records the login event.
    *
    * If an existing session is found but contains no pending tasks, this method
    * attempts to rebuild the first pending task from the session's assigned
    * problem. This prevents the tutor from opening into an unusable state when
    * the TutoringSession row exists but the PendingTask rows are missing.
    *
    * @param jsonUser a JSon encoded Account object containing the user's sign-in
    * credentials
    * @return a TutorReply if successful the status is "Authenticated" and the
    * data is a JSon encoded TutoringSession object; otherwise the status is
    * ":Success" or ":ERR"
    */
    public TutorReply signIn(String jsonUser) {
    Account requestAcct = gson.fromJson(jsonUser, Account.class);

    try {
        Account dbAcct = ServiceFactory.findAccountSvc().retrieve(requestAcct.getUserId());

        if (dbAcct.getPassword().equals(requestAcct.getPassword())) {
            String userId = dbAcct.getUserId();

            SessionSvc svc = ServiceFactory.findSessionSvc();
            student = new Student(dbAcct);
            try {
                session = svc.retrieve(dbAcct.getUserId());
            } catch (ObjNotFoundException e) {
                session = createSession(student, ServiceFactory.findCourseSvc().retrieve(DEFAULT_COURSE_ID));
                }
                if (session.getTasks().isEmpty() && session.getProblem() != null) {
                    System.out.println("signIn recovery: session has no pending tasks, rebuilding first task");

                    Task firstTask = session.getProblem().findTaskBySequence(0);

                    if (firstTask != null) {
                        SessionDAO dao = (SessionDAO) ServiceFactory.findSessionSvc();
                        dao.addPendingTask(session.getId(), firstTask);

                        session = svc.retrieve(dbAcct);

                        System.out.println("signIn recovery added taskId=" + firstTask.getId());
                        System.out.println("signIn recovery added stepId=" + firstTask.getCurrentStep().getId());
                    }
                }
                
                
                System.out.println("signIn sessionId=" + session.getId());
                System.out.println("signIn pending task count=" + session.getTasks().size());
                System.out.println("signIn problem task count=" + session.getProblem().getTasks().size());

                student = session.getStudent();
                
                applyDeveloperModeOverride(userId);
                
            // Do not let login history problems block a successful login
            try {
                notifyLogin(student);
            } catch (NonRecoverableException ex) {
                Logger.getLogger(ShaTuTutor.class.getName()).log(
                        Level.WARNING,
                        "Login succeeded, but login event could not be recorded.",
                        ex
                );
            }

            TutorReply reply = new TutorReply("Authenticated");
            reply.setData(gson.toJson(session));
            return reply;

        } else {
            return new TutorReply("InvalidPassword");
        }

    } catch (ObjNotFoundException e) {
        return new TutorReply("UnknownUser");
    } catch (NonRecoverableException ex) {
        return createError("Unable to sign in: " + ex.getMessage(), ex);
    }
}

    /**
     * Attempts to sign a student out.
     *
     * This method handles ":SignOut" requests from the GUI client.
     *
     * It is invoked indirectly as a reflection from within request().
     *
     * @param jsonUser a JSON encoded User object
     * @return a TutorReply indicating "SIGN_OUT" for success or ":ERR" for
     * failure
     */
    public TutorReply signOut(String jsonUser) {
        Account requestAcct = gson.fromJson(jsonUser, Account.class);

        try {
            student = new Student(requestAcct);
            notifyLogout(student);
            return new TutorReply("SIGN_OUT");
        } catch (ObjNotFoundException ex) {
            TutorReply reply = new TutorReply(":ERR");
            reply.setData("Student model not found for: " + requestAcct.getUserId());
            return reply;
        } catch (NonRecoverableException ex) {
            Logger.getLogger(ShaTuTutor.class.getName()).log(Level.SEVERE, null, ex);
            TutorReply reply = new TutorReply(":ERR");
            reply.setData("NonRecoverableException occured during sign-out");
            return reply;
        }
    }
    
    /**
     * SHAT-362
     * Attempts to save a student session.
     * 
     * This method handles ":SaveSession" requests from the GUI client.
     * 
     * It is invoked indirectly as a reflection from within request().
     * 
     * @param jsonUser a JSON encoded User object
     * @return a TutorReply indicating "SAVED" for success or ":ERR" for failure
     */
    public TutorReply saveSession(String jsonSession) {
        try {
            ServiceFactory.findSessionSvc().update(session);
            return new TutorReply("SAVED");
        }
        // If the user does not have an existing session catch it.
        catch (ObjNotFoundException ex) {
            TutorReply reply = new TutorReply(":ERR");
            reply.setData("Session not found for: " + session.getStudent().getAccount().getUserId());
            return reply;
        }
        // Is the database connection good?
        catch (NonRecoverableException ex) {
            System.getLogger(ShaTuTutor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            TutorReply reply = new TutorReply(":ERR");
            reply.setData("NonRecoverableException occurred during save");
            return reply;
        }
    } // End saveSession()

    /*
    public TutorReply getTask(String jsonObj) {
        System.out.println("get task method");

        Task task = new Task();
        int[] taskOrder = { 102, 103, 104, 105, 106, 107, 108, 109, 101, 110, 111, 100, 112, 113, 114 };
        int currentTask = 102;

        for (int id : taskOrder) {
            Assessment assessment = studentModel.findAssessment(id);
            if (!assessment.getAssessment().equals(AssessmentLevel.COMPLETED)) {
                currentTask = id;
                break;
            }
        }
        System.out.println("Current task: " + currentTask);

        switch (currentTask) {
            case 106:
                task.setType(ProblemType.PREPARE_SCHEDULE);
                currObjective = new PrepareSchedule(student);
                break;
            case 108:
                task.setType(ProblemType.COMPRESS_ROUND);
                currObjective = new CompressRound(student);
                break;
            case 101:
                task.setType(ProblemType.SHIFT_BITS);
                currObjective = new ShiftBits(student);
                break;
            case 110:
                task.setType(ProblemType.XOR_BITS);
                currObjective = new XorBits(student);
                break;
            case 111:
                task.setType(ProblemType.ADD_BITS);
                currObjective = new AddBits(student);
                break;
            case 112:
                task.setType(ProblemType.MAJORITY_FUNCTION);
                currObjective = new MajorityFunction(student);
                break;
            case 103:
                task.setType(ProblemType.ADD_ONE_BIT);
                currObjective = new AddOne(student);
                break;
            case 104:
                task.setType(ProblemType.PAD_ZEROS);
                currObjective = new PadZeros(student);
                break;
            case 109:
                task.setType(ProblemType.ROTATE_BITS);
                currObjective = new RotateBits(student);
                break;
            case 107:
                task.setType(ProblemType.INITIALIZE_VARS);
                currObjective = new InitVars(student);
                break;
            case 100:
                task.setType(ProblemType.CHOICE_FUNCTION);
                currObjective = new ChoiceFunction(student);
                break;
            case 105:
                task.setType(ProblemType.ADD_MSG_LENGTH);
                currObjective = new AddMsgLen(student);
                break;
            case 113:
                task.setType(ProblemType.SHA_ZERO);
                currObjective = new ShaZero(student);
                break;
            case 114:
                task.setType(ProblemType.SHA_ONE);
                currObjective = new ShaOne(student);
                break;
            default:
                task.setType(ProblemType.ASCII_ENCODE);
                currObjective = new EncodeAscii(student);
                break;
        }
        TutorReply reply = new TutorReply(":Success");
        reply.setData(gson.toJson(task));
        return reply;
    }
     */
    /**
     * Returns a hint to the GUI client, if any.
     *
     * This method handles ":RequestHint" requests from the GUI client.
     *
     * @param jsonObj
     * @return a TutorReply, if successful, the status is "Hint" with data being
     * a displayable hint text string.
     */
    public TutorReply requestHint(String jsonObj) {
        System.out.println("requestHint");
        StepCompletion completion = gson.fromJson(jsonObj, StepCompletion.class);

        Step step = completion.getStep();

        switch (step.getSubType()) {
            case INFO_MESSAGE:
                return completeInfoMsgStep(completion);

            default:
                currObjective = getCurrentObjectiveByProbelmType(convertStepToProblemType(step.getSubType()));
                return currObjective.hint(completion);
        }
    }

    /**
     * Records that the student asked to see the correct answer after an
     * incorrect attempt.
     *
     * This method handles ":RequestCorrectAnswer" requests from the GUI client.
     *
     * @param jsonObj a JSon encoded StepCompletion object
     * @return a TutorReply with status ":Success" or ":ERR"
     */
    public TutorReply requestCorrectAnswer(String jsonObj) {
        StepCompletion completion = gson.fromJson(jsonObj, StepCompletion.class);

        if (completion == null || completion.getStep() == null) {
            return createError("Missing step information for correct answer request", null);
        }

        ProblemType problemType = convertStepToProblemType(completion.getStep().getSubType());
        KnowledgeComponentKind knowledgeComponentKind = mapProblemTypeToKnowledgeComponent(problemType);

        if (knowledgeComponentKind == null) {
            return createError("Unknown problem type for correct answer request: " + problemType, null);
        }

        Assessment assessment = studentModel.findAssessment(knowledgeComponentKind.dbId());

        if (assessment == null) {
            return createError("Assessment not found for: " + knowledgeComponentKind.title(), null);
        }

        assessment.incrementCorrectAnswersRequested();

        try {
            StudentModelSvc modelSvc = ServiceFactory.findStudentModelSvc();
            modelSvc.updateAssessment(studentModel, assessment, StudentModelFieldKind.CORRECT_ANSWERS_REQUESTED);
        } catch (NonRecoverableException ex) {
            return createError("Unknown error updating correct answer request count", ex);
        }

        return new TutorReply(":Success");
    }

    /**
    * Handles a completed step submitted by the GUI client.
    *
    * This method handles ":CompletedStep" requests from the GUI client. It
    * unmarshals the submitted StepCompletion object, checks the subtype of the
    * completed step, and routes the completion to the correct objective.
    *
    * INFO_MESSAGE steps are handled separately because they introduce the lesson
    * rather than checking a normal student answer. ENCODE_ASCII steps also receive
    * additional transition handling because a correct ASCII encoding answer should
    * move the student into the first real SHA-256 task.
    *
    * @param jsonObj a JSon encoded StepCompletion object
    * @return a TutorReply containing either a StepCompletionReply, the next
    * pending task, or an error reply if the completed step cannot be processed
    */
    public TutorReply completedStep(String jsonObj) {
        System.out.println("completedStep");

        StepCompletion completion = gson.fromJson(jsonObj, StepCompletion.class);

        Step step = completion.getStep();

        switch (step.getSubType()) {
            case INFO_MESSAGE:
                return completeInfoMsgStep(completion);

            default:
                ProblemType pType = convertStepToProblemType(step.getSubType());
                currObjective = getCurrentObjectiveByProbelmType(pType);
                TutorReply reply = currObjective.completeStep(completion);

                // Sync the mode after the step is graded
                TutoringMode syncedMode = syncSessionTutoringMode(pType);

                if (step.getSubType() == StepSubType.ENCODE_ASCII) {
                    reply = handleEncodeAsciiCompletion(completion, reply);
                }
                // Carry the synced mode back so the client can follow it
                if (syncedMode != null) {
                    reply.setTutoringMode(syncedMode);
                }
                return reply;
        }
    }

    /**
    * Handles the transition from ASCII encoding to the add-one-bit task.
    *
    * This method is called after an ENCODE_ASCII step has already been checked by
    * its objective. If the answer was incorrect, the original completion reply is
    * returned. If the answer was correct, this method builds the next task,
    * copies the generated step data into the persisted ADD_ONE_BIT task, updates
    * the PendingTask row in the database, refreshes the in-memory session task,
    * and returns the next pending task to the client.
    *
    * The generated task contains the new example data, while the persisted task
    * contains the task and step ids that are already tied to the database problem
    * sequence. This method keeps those ids aligned while still passing the newly
    * generated example data forward.
    *
    * @param completion the StepCompletion submitted by the client for the
    * ENCODE_ASCII step
    * @param reply the TutorReply returned by the ENCODE_ASCII objective
    * @return the original TutorReply if the answer was incorrect, or a TutorReply
    * containing the next PendingTask if the answer was correct
    */
    private TutorReply handleEncodeAsciiCompletion(StepCompletion completion, TutorReply reply) {
        PendingTask replyTask = gson.fromJson(reply.getData(), PendingTask.class);

        if (replyTask == null || replyTask.getCurrentStep() == null) {
            return reply;
        }

        Step replyStep = replyTask.getCurrentStep().getStep();
        if (replyStep == null || replyStep.getSubType() != StepSubType.STEP_COMPLETION_REPLY) {
            return reply;
        }

        StepCompletionReply stepReply = gson.fromJson(replyStep.getData(), StepCompletionReply.class);
        if (stepReply == null || !stepReply.isCorrect()) {
            return reply;
        }

        try {
            PendingTask currentTask = session.currentTask();
            if (currentTask == null || currentTask.getTask() == null) {
                return reply;
            }

            int oldTaskId = currentTask.getTask().getId();

            // Generate the next ADD ONE BIT example data.
            Objective nextObjective = getCurrentObjectiveByProbelmType(ProblemType.ADD_ONE_BIT);
            TutorReply nextReply = nextObjective.example(session, null);
            PendingTask nextPendingTask = gson.fromJson(nextReply.getData(), PendingTask.class);

            if (nextPendingTask == null
                    || nextPendingTask.getCurrentStep() == null
                    || nextPendingTask.getCurrentStep().getStep() == null) {
                return reply;
            }

            // Use the persisted ADD_ONE_BIT task from the current problem,
            // not the task id from the generated example reply.
            Task persistedNextTask = null;
            for (Task task : session.getProblem().getTasks()) {
                if (task.getType() == ProblemType.ADD_ONE_BIT) {
                    persistedNextTask = task;
                    break;
                }
            }
            System.out.println("persistedNextTask id=" + persistedNextTask.getId());
            System.out.println("persistedNextTask type=" + persistedNextTask.getType());
            System.out.println("persistedNextTask steps size=" +
                    (persistedNextTask.getSteps() == null ? "null" : persistedNextTask.getSteps().size()));

            if (persistedNextTask == null) {
                Logger.getLogger(ShaTuTutor.class.getName()).log(Level.SEVERE,
                        "Could not find persisted ADD_ONE_BIT task in current problem");
                return createError("Failed to locate ADD ONE BIT task", null);
            }
            if (persistedNextTask.getSteps() == null || persistedNextTask.getSteps().isEmpty()) {
                return createError("ADD ONE BIT task has no loaded steps", null);
            }

             // Use the persisted ADD_ONE_BIT step from that task.
            Step persistedNextStep = null;
            for (Step step : persistedNextTask.getSteps()) {
                if (step.getSubType() == StepSubType.ADD_ONE_BIT) {
                    persistedNextStep = step;
                    break;
                }
            }

            if (persistedNextStep == null) {
                Logger.getLogger(ShaTuTutor.class.getName()).log(Level.SEVERE,
                        "Could not find persisted ADD_ONE_BIT step in current problem");
                return createError("Failed to locate ADD ONE BIT step", null);
            }

            int newTaskId = persistedNextTask.getId();
            int newStepId = persistedNextStep.getId();

            // Preserve generated step data, but point at the persisted step.
            Step generatedStep = nextPendingTask.getCurrentStep().getStep();
            persistedNextStep.setData(generatedStep.getData());

            nextPendingTask.setTask(persistedNextTask);
            nextPendingTask.getCurrentStep().setStep(persistedNextStep);

            SessionDAO dao = (SessionDAO) ServiceFactory.findSessionSvc();
            dao.updatePendingTask(session.getId(), oldTaskId, newTaskId, newStepId);

            session.removeTask(oldTaskId);
            session.addCurrentTask(nextPendingTask);

            return nextReply;

        } catch (Exception ex) {
            Logger.getLogger(ShaTuTutor.class.getName()).log(Level.SEVERE,
                    "Failed to transition ENCODE_ASCII to ADD_ONE_BIT", ex);
            return createError("Failed to complete Encode ASCII transition", ex);
        }
    }
    
    /**
    * Completes the introductory information message step.
    *
    * This method handles INFO_MESSAGE step completions from the GUI client. It
    * marks the introductory step as complete, updates the student's exposure and
    * assessment information, selects ASCII encoding as the first real task, and
    * returns an ASCII encoding example to the client.
    *
    * The database transition currently expects the introductory task to use task
    * id 0 and the first ASCII encoding task to use task id 10 with step id 1.
    * These values are tied to the seeded problem data and should be updated if
    * the database seed data changes.
    *
    * @param completion the StepCompletion object for the completed information
    * message step
    * @return a TutorReply containing the first ASCII encoding example, or an
    * error reply if the transition cannot be processed
    */
    public TutorReply completeInfoMsgStep(StepCompletion completion) {

        // Advance past the one-time INFO_MESSAGE so it won't reappear on the next sign-in.
        // IMPORTANT: The client UI assumes there is ALWAYS at least one pending task in the session,
        // so we update the existing PendingTask row instead of deleting it.
        try {
            if (session != null) {
                SessionDAO dao = (SessionDAO) ServiceFactory.findSessionSvc();
                // Welcome task is always TaskId = 0
                // SHAT-368: as per function note above, PendingTask should never be deleted
                
                //dao.deletePendingTask(session.getId(), 0);
                
                
                //// update task from 'WELCOME' to 'ENCODE ASCII'
                //dao.updatePendingTask(session.getId(), 0, 10, 0);
                
                

            // TODO: Hard-coded for Choice Function development. When sequential task
            // navigation is implemented, this should be replaced with a proper call to
            // insertPendingTask() using the dynamically generated task from
            // currObjective.example(). TaskId and StepId must match rows in the
            // Task and Step tables in the database.
            // update pending task, DO NOT DELETE PENDING TASK
            // note: updating pendingTask to something other than 'welcome' 
            // breaks the tutor sign-in process for a returning user
            // due to a null step/pendingstep.
//                dao.updatePendingTask(session.getId(), 0, 110, 0);
                dao.updatePendingTask(session.getId(), 0, 0, 0);

            }
        } catch (Exception ex) {
            Logger.getLogger(ShaTuTutor.class.getName())
                  .log(Level.WARNING, "Unable to clear welcome task from PendingTask", ex);
        }
        TutoringMode mode = session.getTutoringMode();
        ProblemType firstProblemType;



        
        //Does this need to be updated to display where they last where for DO_ONE,  TEACH_ONE?? Will they always start with ASCII?
        System.out.println("[ShaTuTutor.java] - [completeInfoMsgStep] - starting INFO_MESSAGE completion");

        try {
            Assessment welcomeAssessment = studentModel.findAssessment(0);

            if (welcomeAssessment != null
                    && welcomeAssessment.getAssessment() != AssessmentLevel.COMPLETED) {

                welcomeAssessment.incrementExposures();
                welcomeAssessment.incrementSuccessess();
                welcomeAssessment.setAssessment(AssessmentLevel.COMPLETED);

                StudentModelSvc modelSvc = ServiceFactory.findStudentModelSvc();
                modelSvc.updateAssessment(studentModel, welcomeAssessment, StudentModelFieldKind.ATTEMPTS);
                modelSvc.updateAssessment(studentModel, welcomeAssessment, StudentModelFieldKind.SUCCESSES);
                modelSvc.updateAssessment(studentModel, welcomeAssessment, StudentModelFieldKind.ASSESSMENT_LEVEL);
            }
        } catch (NonRecoverableException ex) {
            return createError("Failed to update welcome acknowledgement assessment", ex);
        }

        try {
            logInfoMessageTransitionReadiness();
        } catch (Exception ex) {
            Logger.getLogger(ShaTuTutor.class.getName())
                    .log(Level.WARNING, "SHAT-347: unable to inspect next task during INFO_MESSAGE completion", ex);
        }

        mode = session.getTutoringMode();
        
        switch (mode) {
            case SEE_ONE:
                firstProblemType = ProblemType.ASCII_ENCODE;
                break;
            case DO_ONE:
                firstProblemType = ProblemType.ASCII_ENCODE;
                break;
            case TEACH_ONE:
                firstProblemType = ProblemType.ASCII_ENCODE;
                break;
            default:
                firstProblemType = ProblemType.ASCII_ENCODE;
        }

        currObjective = getCurrentObjectiveByProbelmType(firstProblemType);

        String messageToHash;
        if (session.getProblem() != null && session.getProblem().getMessageToHash() != null) {
            messageToHash = session.getProblem().getMessageToHash();
        } else {
            messageToHash = "Regis Computer Science Rocks!";
        }

        EncodeAsciiStep encodeStep = new EncodeAsciiStep();
        encodeStep.setQuestion(messageToHash);
        String jsonData = gson.toJson(encodeStep);

        try {
            System.out.println("[ShaTuTutor.java] - [completeInfoMsgStep] - attempting DB transition sessionId="
                    + session.getId() + ", oldTaskId=0, newTaskId=10, newStepId=1");
            SessionDAO dao = (SessionDAO) ServiceFactory.findSessionSvc();
            dao.updatePendingTask(session.getId(), 0, 10, 1);
            System.out.println("[ShaTuTutor.java] - [completeInfoMsgStep] - DB transition updatePendingTask completed successfully");
        } catch (Exception ex) {
            Logger.getLogger(ShaTuTutor.class.getName())
                    .log(Level.SEVERE, "SHAT-347: failed to persist INFO_MESSAGE -> ASCII_ENCODE transition", ex);
            System.out.println("[ShaTuTutor.java] - [completeInfoMsgStep] - DB transition failed: " + ex.getMessage());
        }

        System.out.println("[ShaTuTutor.java] - [completeInfoMsgStep] - returning ASCII example to client");
        return currObjective.example(session, jsonData);
    }

    public TutorReply completedTask(String taskInfo) {
        return new TutorReply();
    }

    /**
     * Handles :NewExample requests from the client. Returns a new problem for
     * the student, either specified or adaptively chosen.
     *
     * @param json JSon encoding a NewExampleRequest; may specify ProblemType
     * @return TutorReply with the generated Problem
     */
    public TutorReply newExample(String json) {
        NewExampleRequest request;
        if (json != null && !json.isEmpty()) {
            request = gson.fromJson(json, NewExampleRequest.class);
        } else {
            request = new NewExampleRequest(); // default request
        }

        ProblemType type = (request.getExampleType() != null)
                ? request.getExampleType()
                : determineNextProblemType(studentModel);

        currObjective = getCurrentObjectiveByProbelmType(type);

        // Sync the mode before generating the new example
        TutoringMode syncedMode = syncSessionTutoringMode(type);

        // Use the existing method in Objective subclasses
        TutorReply reply = currObjective.example(session, request.getData());
        if (syncedMode != null) {
            reply.setTutoringMode(syncedMode);
        }
        return reply;
    }

    /**
     *
     * ToDO: Can ProblemType and StepSubType be combined?
     *
     * @param problemType
     * @return
     */
    private Objective getCurrentObjectiveByProbelmType(ProblemType problemType) {
        switch (problemType) {
            case PREPARE_SCHEDULE:
                return new PrepareSchedule(student);
            case COMPRESS_ROUND:
                return new CompressRound(student);
            case SHIFT_BITS:
                return new ShiftBits(student);
            case XOR_BITS:
                return new XorBits(student);
            case ADD_BITS:
                return new AddBits(student);
            case MAJORITY_FUNCTION:
                return new MajorityFunction(student);
            case ADD_ONE_BIT:
                return new AddOne(student);
            case PAD_ZEROS:
                return new PadZeros(student);
            case ROTATE_BITS:
                return new RotateBits(student);
            case INITIALIZE_VARS:
                return new InitVars(student);
            case CHOICE_FUNCTION:
                return new ChoiceFunction(student);
            case ADD_MSG_LENGTH:
                return new AddMsgLen(student);
            case ASCII_ENCODE:
                return new EncodeAscii(student);
            case SHA_ZERO:
                return new ShaZero(student);
            case SHA_ONE:
                return new ShaOne(student);
            default:
                System.out.println("ShaUnknown new example request: " + problemType);
                return null;
        }
    }

    /**
     * KLUDGE ToDO: Can ProblemType and StepSubType be combined?
     *
     * @param stepType
     * @return
     */
    private ProblemType convertStepToProblemType(StepSubType stepType) {
        switch (stepType) {
            case ENCODE_BINARY:
            case ENCODE_HEX:
            case ENCODE_ASCII:
                return ProblemType.ASCII_ENCODE;
            case ADD_ONE_BIT:
                return ProblemType.ADD_ONE_BIT;
            case PAD_ZEROS:
                return ProblemType.PAD_ZEROS;
            case ADD_MSG_LENGTH:
                return ProblemType.ADD_MSG_LENGTH;
            case PREPARE_SCHEDULE:
                return ProblemType.PREPARE_SCHEDULE;
            case INITIALIZE_VARS:
                return ProblemType.INITIALIZE_VARS;
            case COMPRESS_ROUND:
                return ProblemType.COMPRESS_ROUND;
            case ROTATE_BITS:
                return ProblemType.ROTATE_BITS;
            case SHIFT_BITS:
                return ProblemType.SHIFT_BITS;
            case XOR_BITS:
                return ProblemType.XOR_BITS;
            case ADD_BITS:
                return ProblemType.ADD_BITS;
            case MAJORITY_FUNCTION:
                return ProblemType.MAJORITY_FUNCTION;
            case CHOICE_FUNCTION:
                return ProblemType.CHOICE_FUNCTION;
            case SHA_ZERO:
                return ProblemType.SHA_ZERO;
            case SHA_ONE:
                return ProblemType.SHA_ONE;
            default:
                System.out.println("Unknown step type in convert: " + stepType);
                return null;
        }
    }

    /**
     * Map a problem type to the associated knowledge component.
     *
     * @param problemType the problem type for which the student asked to see
     * the answer
     * @return the matching KnowledgeComponentKind or null if none exists
     */
    private KnowledgeComponentKind mapProblemTypeToKnowledgeComponent(ProblemType problemType) {
        if (problemType == null) {
            return null;
        }

        switch (problemType) {
            case ASCII_ENCODE:
                return KnowledgeComponentKind.ASCII_ENCODE;
            case ADD_ONE_BIT:
                return KnowledgeComponentKind.ADD_ONE_BIT;
            case PAD_ZEROS:
                return KnowledgeComponentKind.PAD_ZEROS;
            case ADD_MSG_LENGTH:
                return KnowledgeComponentKind.ADD_MSG_LENGTH;
            case PREPARE_SCHEDULE:
                return KnowledgeComponentKind.PREPARE_SCHEDULE;
            case INITIALIZE_VARS:
                return KnowledgeComponentKind.INITIALIZE_VARS;
            case COMPRESS_ROUND:
                return KnowledgeComponentKind.COMPRESS_ROUND;
            case ROTATE_BITS:
                return KnowledgeComponentKind.ROTATE_BITS;
            case SHIFT_BITS:
                return KnowledgeComponentKind.SHIFT_BITS;
            case XOR_BITS:
                return KnowledgeComponentKind.XOR_BITS;
            case ADD_BITS:
                return KnowledgeComponentKind.ADD_BITS;
            case MAJORITY_FUNCTION:
                return KnowledgeComponentKind.MAJORITY_FUNCTION;
            case CHOICE_FUNCTION:
                return KnowledgeComponentKind.CHOICE_FUNCTION;
            case SHA_ZERO:
                return KnowledgeComponentKind.SHA_ZERO;
            case SHA_ONE:
                return KnowledgeComponentKind.SHA_ONE;
            default:
                return null;
        }
    }

    /**
     * Create and save a new tutoring session associated with the given account.
     *
     * @param account the student user
     * @throws NonRecoverableException
     * @return the new TutoringSession
     */
    private TutoringSession createSession(Student student, Course course) throws NonRecoverableException, ObjNotFoundException {
        Account account = student.getAccount();

        TutoringSession tSession = new TutoringSession(student);
        tSession.setStartDate(new GregorianCalendar());
        tSession.setCourse(course.getDigest());
        tSession.setUnit(course.currentUnit().getDigest());

        //set tutoringmode to SEE_ONE or Unit 0
        tSession.setTutoringMode(TutoringMode.SEE_ONE);

        Unit unit = course.findUnitBySequenceId(0); // The first unit
        tSession.setUnit(unit.getDigest());

        Problem problem = unit.findProblemBySequence(0); // First Problem
        tSession.setProblem(problem);

        Task task = problem.findTaskBySequence(0); //Task task = getFirstTask(course);
        PendingTask pendingTask = new PendingTask(task);
        pendingTask.setCurrentStep(new PendingStep(task.getCurrentStep()));
        tSession.addTask(pendingTask);

        // Generate the security token for this tutoring session.
        Random rnd = new Random();
        String clearToken = "Session" + account.getUserId() + Integer.toString(rnd.nextInt());
        tSession.setSecurityToken(SHA_256.instance().sha256(clearToken));

        try {
            ServiceFactory.findSessionSvc().create(tSession);

            return tSession;

        } catch (ObjDuplicateException ex) {
            throw new NonRecoverableException("Session already exists", ex);
        }
    }

    /**
     * Create and save the student and their initial student model.
     *
     * @param acct
     * @param course
     * @return
     */
    private Student createStudent(Account account, Course course)
            throws NonRecoverableException {

        student = new Student(account);
        studentModel = student.getStudentModel();

        for (KnowledgeComponent outcome : course.getOutcomes()) {
            AssessmentLevel startingLevel
                    = (outcome.getId() == 0)
                    ? AssessmentLevel.IN_PROGRESS // Welcome acknowledgement starts immediately
                    : AssessmentLevel.NOT_STARTED;

            Assessment assessment = new Assessment(outcome, startingLevel);
            studentModel.addAssessment(outcome.getId(), assessment);
        }

        StudentModelSvc stuSvc = ServiceFactory.findStudentModelSvc();
        stuSvc.create(student);

        return student;
    }

    /**
     * Verify that the user with the given id has a session with the given
     * session id.
     *
     * @param userId String "user@regis.edu"
     * @param sessionId String identifying a previously generated session id.
     * @return the current TutoringSession associated with the given user id and
     * session id
     */
    private boolean verifySession(String userId, String sessionId)
            throws ObjNotFoundException, NonRecoverableException {

        SessionSvc svc = ServiceFactory.findSessionSvc();
        String dbToken = svc.retrieveSecurityToken(userId);

        return dbToken.equals(sessionId);
    }

    /**
     * Return the first task that should be performed in the given course.
     *
     * @param course
     * @return a Task that should be completed first.
     * @throws NonRecoverableException see the message text.
     */
    /*
    private Task getFirstTask(Course course) throws NonRecoverableException {
        switch (course.getPrimaryPedagogy()) {
            case STUDENT_CHOICE:
                return null; // ToDo

            case FIXED_SEQUENCE:
                Unit unit = course.findUnitBySequenceId(0);

                if (unit == null) {
                    throw new NonRecoverableException("Unit 0 not found in course: " + course.getId());
                }

                Task task = unit.findTaskBySequence(0);

                if (task == null) {
                    throw new NonRecoverableException("Task 0 not found in Unit 0 of course: " + course.getId());
                }

                return task;

            case MASTERY_LEARNING:
                return null; // ToDo

            case MICROADAPTATION:
                return null; // ToDo

            default:
                throw new NonRecoverableException("Unknwon task selection in course: " + course.getId());
        }
    }
     */
    /**
     * Utility for logging an error and an creating a tutoring reply error with
     * the given message, and optional originating exception.
     *
     * @param errMsg a displayable error message
     * @param ex the original exception, if any, that caused the error,
     * otherwise null.
     * @return a TutorReply with an ":ERR" status
     */
    public TutorReply createError(String errMsg, Exception ex) {
        if (ex == null) {
            Logger.getLogger(ShaTuTutor.class.getName()).log(Level.SEVERE, errMsg);
        } else {
            Logger.getLogger(ShaTuTutor.class.getName()).log(Level.SEVERE, errMsg, ex);
        }

        return new TutorReply(":ERR", errMsg);
    }

    /**
     * Method that generates and returns a random string.
     *
     * @param length
     *
     * @return
     */
    protected String generateRandomString(int length) {

        Random random = new Random();
        StringBuilder sb = new StringBuilder(length); // StringBuilder allows easier altering of a string.

        for (int i = 0; i < length; i++) {
            // Generates a random integer between 32 (inclusive) and 126 (inclusive)
            int randomChar = 32 + random.nextInt(95); // 126 - 32 + 1 = 95
            sb.append((char) randomChar);
        }

        return sb.toString();
    }

    /**
     * Update the database to record the fact the student logged in.
     *
     * @param student the student who logged in.
     * @throws ObjNotFoundException
     * @throws NonRecoverableException
     */
    private void notifyLogin(Student student) throws ObjNotFoundException, NonRecoverableException {
        StudentModelSvc stuModelSvc = ServiceFactory.findStudentModelSvc();

        Date now = new Date();
        long milliseconds = now.getTime();

        student.setLastLogin(milliseconds);
        stuModelSvc.recordLoginEvent(student.getAccount().getUserId(), milliseconds);

        // LOGGER.log(Level.INFO, "Student {0} logged in at {1}", new Object[]{student.getAccount().getUserId(), milliseconds});
    }

    /**
     * Update the database to record the fact the student logged out.
     *
     * @param student the student who logged out.
     * @throws ObjNotFoundException
     * @throws NonRecoverableException
     */
    private void notifyLogout(Student student) throws ObjNotFoundException, NonRecoverableException {
        StudentModelSvc stuModelSvc = ServiceFactory.findStudentModelSvc();

        Date now = new Date();
        long milliseconds = now.getTime();

        student.setLastLogout(milliseconds);
        stuModelSvc.recordLogoutEvent(student.getAccount().getUserId(), milliseconds);

        // LOGGER.log(Level.INFO, "Student {0} logged out at {1}", new Object[]{student.getAccount().getUserId(), milliseconds});
    }

    /**
     * Determines the next problem type to suggest for the student based on
     * their progress and performance.
     *
     * @param student the Student whose model is used
     * @return the suggested next ProblemType
     */
    private ProblemType determineNextProblemType(StudentModel model) {
        for (ProblemType type : ProblemType.values()) {
            if (!model.hasCompleted(type)) {
                return type;
            }
        }
        return model.getWeakestProblemType();
    }
    
    private void logInfoMessageTransitionReadiness() {
        if (session == null) {
            System.out.println("SHAT-347: session was null during INFO_MESSAGE completion");
            return;
        }

        if (session.getProblem() == null) {
            System.out.println("SHAT-347: problem was null during INFO_MESSAGE completion");
            return;
        }

        Task nextTask = session.getProblem().findTaskById(10);

        if (nextTask == null) {
            System.out.println("SHAT-347: next task not found. Expected TaskId 10 for first ASCII task.");
            return;
        }

        if (nextTask.getSteps() == null || nextTask.getSteps().isEmpty()) {
            System.out.println(
                "SHAT-347: cannot persist transition to task "
                + nextTask.getId()
                + " because no Step rows exist for this task in seeded DB"
            );
            return;
        }

        System.out.println(
            "SHAT-347: next task "
            + nextTask.getId()
            + " has "
            + nextTask.getSteps().size()
            + " step(s); DB persistence path may be available"
        );
    }

    /**
     * Dynamically updates the tutoring session's mode based on the student's
     * assessment level for the current problem type.
     *
     * @return the mode now set on the session, or null if it was not changed
     */
    private TutoringMode syncSessionTutoringMode(ProblemType problemType) {
        KnowledgeComponentKind kc = mapProblemTypeToKnowledgeComponent(problemType);
        if (kc != null && studentModel != null && session != null) {
            Assessment assessment = studentModel.findAssessment(kc.dbId());
            if (assessment != null) {
                switch (assessment.getAssessment()) {
                    case IN_PROGRESS, MEDIUM, HIGH, VERY_HIGH ->
                        session.setTutoringMode(DO_ONE);
                    case COMPLETED ->
                        session.setTutoringMode(TEACH_ONE);
                    default ->
                        session.setTutoringMode(SEE_ONE);
                }
                System.out.println("Session tutoring mode synced to: " + session.getTutoringMode() + " for " + problemType);
                return session.getTutoringMode();
            }
        }
        return null;
    }
}
