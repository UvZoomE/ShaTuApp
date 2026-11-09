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

/**
 * A decorator that wraps a tutor reply to a user interface request.
 * 
 * The the initial request time specifies how to interpret the JSon encoded 
 * data.
 * 
 * @author rickb
 */
import edu.regis.shatu.model.aol.TutoringMode;

public class TutorReply {
    /**
     * Shown to the user when an ":ERR" reply arrives without a message.
     */
    public static final String DEFAULT_ERROR_MESSAGE =
            "An unexpected error occurred. Please contact ShaTu support.";

    /**
     * The status of this reply (of particular note is ERR)
     */
    private String status;
    
    /**
     * A JSon encoded object whose format depends on the associated request to
     * which this is a reply.
     */
    private String data = "";

    /**
     * Optional: the tutoring mode the server has synced for the student after
     * handling this request, or null if the server did not change it.
     */
    private TutoringMode tutoringMode;
    
    /**
     * A reply from the tutor with an ERR status.
     */
    public TutorReply() {
        this(":ERR");
    }
    
    /**
     * A reply from the tutor with the given status
     * 
     * @param status 
     */
    public TutorReply(String status) {
        this.status = status;
    }
    
    public TutorReply(String status, String data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    /**
     * Return the message to show the user for an ":ERR" reply.
     *
     * The tutor puts ":ERR" in the status and the readable message in the
     * data, so the data is the message. Some error replies carry no data, in
     * which case DEFAULT_ERROR_MESSAGE is returned.
     *
     * @return the error message, never null or empty
     */
    public String getErrorMessage() {
        if (data == null || data.isBlank()) {
            return DEFAULT_ERROR_MESSAGE;
        }

        return data.trim();
    }

    public TutoringMode getTutoringMode() {
        return tutoringMode;
    }

    public void setTutoringMode(TutoringMode tutoringMode) {
        this.tutoringMode = tutoringMode;
    }
}
