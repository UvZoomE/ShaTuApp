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
package edu.regis.shatu.dao;

import com.google.gson.Gson;
import edu.regis.shatu.err.InconsistentDBException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import edu.regis.shatu.err.NonRecoverableException;
import edu.regis.shatu.err.ObjNotFoundException;
import edu.regis.shatu.model.Hint;
import edu.regis.shatu.model.steps.EncodeAsciiStep;
import edu.regis.shatu.model.Task;
import edu.regis.shatu.model.aol.Problem;
import edu.regis.shatu.model.aol.ProblemType;
import edu.regis.shatu.model.aol.StepSubType;
import static edu.regis.shatu.model.aol.StepSubType.ADD_BITS;
import static edu.regis.shatu.model.aol.StepSubType.ADD_MSG_LENGTH;
import static edu.regis.shatu.model.aol.StepSubType.ADD_ONE_BIT;
import static edu.regis.shatu.model.aol.StepSubType.CHOICE_FUNCTION;
import static edu.regis.shatu.model.aol.StepSubType.COMPRESS_ROUND;
import static edu.regis.shatu.model.aol.StepSubType.ENCODE_ASCII;
import static edu.regis.shatu.model.aol.StepSubType.ENCODE_BINARY;
import static edu.regis.shatu.model.aol.StepSubType.ENCODE_HEX;
import static edu.regis.shatu.model.aol.StepSubType.GUI_ACTION;
import static edu.regis.shatu.model.aol.StepSubType.INFO_MESSAGE;
import static edu.regis.shatu.model.aol.StepSubType.INITIALIZE_VARS;
import static edu.regis.shatu.model.aol.StepSubType.MAJORITY_FUNCTION;
import static edu.regis.shatu.model.aol.StepSubType.PAD_ZEROS;
import static edu.regis.shatu.model.aol.StepSubType.PREPARE_SCHEDULE;
import static edu.regis.shatu.model.aol.StepSubType.REQUEST_HINT;
import static edu.regis.shatu.model.aol.StepSubType.ROTATE_BITS;
import static edu.regis.shatu.model.aol.StepSubType.SHA_ONE;
import static edu.regis.shatu.model.aol.StepSubType.SHA_ZERO;
import static edu.regis.shatu.model.aol.StepSubType.SHIFT_BITS;
import static edu.regis.shatu.model.aol.StepSubType.STEP_COMPLETION_REPLY;
import static edu.regis.shatu.model.aol.StepSubType.XOR_BITS;
import edu.regis.shatu.model.aol.TaskKind;
import edu.regis.shatu.model.aol.Timeout;
import edu.regis.shatu.model.steps.AddOneStep;
import edu.regis.shatu.model.steps.InformationStep;
import edu.regis.shatu.model.steps.Step;
import edu.regis.shatu.svc.ProblemSvc;
import java.util.ArrayList;

/**
 * A Data Access Object that implements ProblemSvc behaviors.
 * 
 * @author rickb
 */
public class ProblemDAO extends MySqlDAO implements ProblemSvc {
    /**
     * GSON instance for JSON serialization/deserialization.
     */
    private static final Gson gson = new Gson();
    
    /**
     * Instantiate this Problem DAO with default values.
     */
    public ProblemDAO() {
        super("Problem", "Id");
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Problem retrieve(int id) throws ObjNotFoundException, NonRecoverableException {
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(URL);

            return retrieve(id, conn);

        } catch (SQLException e) {
            throw new NonRecoverableException("ProblemDAO-ERR-1" + e.toString(), e);
        } finally {
            close(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Problem retrieve(int id, Connection conn) throws ObjNotFoundException, NonRecoverableException {
        final String sql = "SELECT Title, Description, UnitId, SequenceIndex, Message FROM Problem WHERE Id = ?";

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {               
                Problem problem = new Problem(id);
                problem.setTitle(rs.getString("Title"));
                problem.setDescription(rs.getString("Description"));
                problem.setUnitId(rs.getInt("UnitId"));
                problem.setSequenceIndex(rs.getInt("SequenceIndex"));
                problem.setMessageToHash(rs.getString("Message"));
                             
                problem.setTasks(retrieveTasks(id, conn));
                
                return problem;

            } else {
                throw new ObjNotFoundException("Problem Id:" + id);
            }
        } catch (SQLException e) {
            throw new NonRecoverableException("ProblemDAO-ERR-2" + e.toString(), e);
        } finally {
            close(stmt);
        }
    }
    
     /**
     * {@inheritDoc}
     */
    @Override
    public ArrayList<Problem> retrieveByUnitId(int unitId) throws NonRecoverableException {
        final String sql = "SELECT Id FROM Problem WHERE UnitId = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        ArrayList<Problem> problems = new ArrayList<>();

        int problemId = -1; // Here for better error messages

        try {
            conn = DriverManager.getConnection(URL);

            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, unitId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                problemId = rs.getInt("Id");
                problems.add(retrieve(problemId, conn));
            }

            return problems;

        } catch (ObjNotFoundException e) {
            InconsistentDBException ex = new InconsistentDBException("Problem not found in unit " + unitId + " for problem id: " + problemId);
            throw new NonRecoverableException("InconsistentDB see internal exception", ex);
        } catch (SQLException e) {
            throw new NonRecoverableException("ProblemDAO-ERR-3" + e.toString(), e);
        } finally {
            close(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
   /* @Override
    public Task retrieveTask(int problemId, int taskId, Connection conn)
            throws ObjNotFoundException, NonRecoverableException {
        final String sql = "SELECT Title, Description, Kind, SequenceIndex, ExampleType FROM Task WHERE ProblemId = ? AND TaskId = ?";

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, problemId);
            stmt.setInt(2, taskId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Task task = new Task(taskId);
                task.setTitle(rs.getString("Title"));
                task.setDescription(rs.getString("Description"));
                task.setKind(TaskKind.valueOf(rs.getString("Kind")));
                task.setSequenceIndex(rs.getInt("SequenceIndex"));

                if (task.getKind() == TaskKind.PROBLEM) {
                    task.setType(ProblemType.valueOf(rs.getString("ExampleType")));
                }

                task.setSteps(retrieveSteps(task.getId(), conn));

                return task;

            } else {
                throw new ObjNotFoundException("taskId " + taskId + " Problem Id:" + problemId);
            }
        } catch (SQLException e) {
            throw new NonRecoverableException("ProblemDAO-ERR-4" + e.toString(), e);
        } finally {
            close(stmt);
        }
    }
    */
    
        /**
     * Extract and return the tasks in the given &lt;Unit> element
     * 
     * @param parent &lt;Unit> // ToDo is this always true?
     * @return a Task list.
     */
    private ArrayList<Task> retrieveTasks(int problemId, Connection conn) throws NonRecoverableException {
        final String sql = 
                "SELECT Id, Title, Description, Kind, SequenceIndex, ExampleType FROM Task WHERE ProblemId = ? ";

        ArrayList<Task> tasks = new ArrayList<>();

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, problemId);

            ResultSet rs = stmt.executeQuery();

             while (rs.next()) {
                Task task = new Task(rs.getInt("Id"));
                task.setTitle(rs.getString("Title"));
                task.setDescription(rs.getString("Description"));
                task.setKind(TaskKind.valueOf(rs.getString("Kind")));
                task.setSequenceIndex(rs.getInt("SequenceIndex"));
                task.setType(ProblemType.valueOf(rs.getString("ExampleType")));

                tasks.add(task);    // Should this line go after task.setSteps()?
                
                task.setSteps(retrieveSteps(task.getId(), conn));

                // ToDo: retrieve exercising locations
            }

            return tasks;
        } catch (SQLException e) {
            throw new NonRecoverableException("ProblemDAO-ERR-5" + e.toString(), e);
        } finally {
            close(stmt); // Don't close the connection, retrieve(courseId) will
        }

    }

    /**
     * Extract and return a list of child steps from the given parent element.
     * 
     * @param courseId
     * @param taskId
     * @param conn     an open connection to the DB, which isn't closed by this
     *                 method.
     * @return a List of Step elements, may be empty.
     */
    private ArrayList<Step> retrieveSteps(int taskId, Connection conn)
            throws NonRecoverableException {
        final String sql =
                "SELECT Id, Title, Description, SequenceIndex, StepSubType, SubTypeId, TimeoutId FROM Step WHERE TaskId = ?";

        ArrayList<Step> steps = new ArrayList<>();

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, taskId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                StepSubType subType = StepSubType.valueOf(rs.getString("StepSubType"));
                int subTypeId = rs.getInt("SubTypeId");
                
                Step step = new Step(rs.getInt("Id"), rs.getInt("SequenceIndex"), subType);
                
                step.setTitle(rs.getString("Title"));           
                step.setDescription(rs.getString("Description"));      
                step.setTimeout(retrieveTimeout(rs.getInt("TimeoutId"), conn));
                
                String stepData = extractStepSubTypeData(subType, subTypeId, conn);
                
                // Convert raw text to JSON for INFO_MESSAGE
                if (subType == StepSubType.INFO_MESSAGE && stepData != null && !stepData.isEmpty()) {
                    InformationStep infoStep = new InformationStep();
                    infoStep.setMsg(stepData);
                    stepData = gson.toJson(infoStep);
                }
                
                step.setData(stepData); 
                steps.add(step);
                step.setHints(retrieveHints(step.getId(), conn));
            }

            return steps;
        } catch (SQLException e) {
            throw new NonRecoverableException("ProblemDAO-ERR-6" + e.toString(), e);
        } finally {
            close(stmt); // Don't close the connection, retrieve(courseId) will
        }
    }

    /**
     * Extract and return a list of hints from the given parent element.
     * 
     *
     * @param conn an open connection to the DB, which isn't closed by this method.
     * @return an ArrayList of Hint
     */
    private ArrayList<Hint> retrieveHints(int stepId, Connection conn)
            throws NonRecoverableException {

        final String sql = "SELECT Id, Text, SequenceIndex FROM Hint WHERE StepId = ?";

        ArrayList<Hint> hints = new ArrayList<>();

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, stepId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Hint hint = new Hint(rs.getInt("Id"));
                hint.setText(rs.getString("Text"));
                hint.setSequenceIndex(rs.getInt("SequenceIndex"));

                hints.add(hint);
            }

            return hints;

        } catch (SQLException e) {
            throw new NonRecoverableException("ProblemDAO-ERR-7" + e.toString(), e);
        } finally {
            close(stmt); // Don't close the connection, retrieve(courseId) will
        }
    }
    
    
    private String extractEncodeAsciiData(Connection conn) throws NonRecoverableException {
        try {
            EncodeAsciiStep step = new EncodeAsciiStep();

            String question = "Regis Computer Science Rocks!";
            step.setQuestion(question);

            StringBuilder binary = new StringBuilder();
            char[] chars = question.toCharArray();

            for (char c : chars) {
                String binaryChar = String.format("%8s", Integer.toBinaryString(c)).replace(" ", "0");
                binary.append(binaryChar);
            }

            step.setResult(binary.toString());

            return gson.toJson(step);
        } catch (Exception ex) {
            throw new NonRecoverableException("Failed to rebuild ENCODE_ASCII step data", ex);
        }
    }


    private String extractAddOneBitData(Connection conn) throws NonRecoverableException {
        AddOneStep step = new AddOneStep();

        String question = "Regis Computer Science Rocks!";
        step.setQuestion(question);
        step.setMessageLength(question.length());
        step.setResult(addOneBitAnswer(question));

        return gson.toJson(step);
    }
    
    private String addOneBitAnswer(String question) {
        char[] chars = question.toCharArray();
        StringBuilder binary = new StringBuilder();

        for (char c : chars) {
            String binaryChar = String.format("%8s", Integer.toBinaryString(c)).replace(" ", "0");
            binary.append(binaryChar).append(" ");
        }

        return binary + "1";
    }


    /**
     * @param subType
     * @param subTypeId index into the appropriate table determined by subType
     * @return
     */
    private String extractStepSubTypeData(StepSubType subType, int subTypeId, Connection conn)
            throws NonRecoverableException {
        switch (subType) {
            case INFO_MESSAGE:
                return extractInfoMsgData(subTypeId, conn);
            case GUI_ACTION:
                return ""; // TBD
            case ENCODE_BINARY:
                return ""; // TBD
            case ENCODE_HEX:
                return ""; // TBD
            case ENCODE_ASCII:
                return extractEncodeAsciiData(conn);
            case ADD_ONE_BIT:
                return extractAddOneBitData(conn);
            case PAD_ZEROS:
                return ""; // TBD
            case ADD_MSG_LENGTH:
                return ""; // TBD
            case PREPARE_SCHEDULE:
                return ""; // TBD
            case INITIALIZE_VARS:
                return ""; // TBD
            case COMPRESS_ROUND:
                return ""; // TBD
            case ROTATE_BITS:
                return ""; // TBD
            case SHIFT_BITS:
                return ""; // TBD
            case XOR_BITS:
                return ""; // TBD
            case ADD_BITS:
                return ""; // TBD
            case MAJORITY_FUNCTION:
                return ""; // TBD
            case CHOICE_FUNCTION:
                return ""; // TBD
            case STEP_COMPLETION_REPLY:
                return ""; // TBD
            case REQUEST_HINT:
                return ""; // TBD
            case SHA_ONE:
                return "";
            case SHA_ZERO:
                return "";
            default:
                return "";
        }
    }

    /**
     * 
     * The data is a POJO String object
     */
    private String extractInfoMsgData(int subTypeId, Connection conn) throws NonRecoverableException {
        final String sql = "SELECT Text FROM InfoMsgStep WHERE SubStepId = ?";

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, subTypeId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString(1);
            } else {
                String errMsg = "ERROR: ToDo: Throw database inconsistency InfoMsgStep table: " + subTypeId;
                throw new NonRecoverableException(errMsg, new InconsistentDBException(errMsg));
            }
        } catch (SQLException e) {
            throw new NonRecoverableException("ProblemDAO-ERR-10" + e.toString(), e);
        } finally {
            close(stmt); // Don't close the connection, retrieve(courseId) will
        }
    }

    /**
     * Load the timeout with the given id from the database
     * 
     * @param id the id of the timeout that is being retrieved
     * @param conn
     * @return the Timeout with the given id.
     * @throws NonRecoverableException perhaps see getCause().getErrorCode().
     */
    private Timeout retrieveTimeout(int id, Connection conn) throws NonRecoverableException {
        final String sql = "SELECT TimeoutType, Seconds, Event, Msg FROM Timeout WHERE Id = ?";
                           
        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Timeout timeout = new Timeout(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4));
                return timeout;
            } else {
                // ToDo: throw a dabase inconsistency error
                String errMsg = "Timeout not found, id: " + id;
                throw new NonRecoverableException(errMsg, new InconsistentDBException(errMsg));
            }
        } catch (SQLException e) {
            throw new NonRecoverableException("ProblemDAO-ERR-11" + e.toString(), e);
        } finally {
            close(stmt); // Don't close the connection, retrieve(courseId) will
        }
    }
}
