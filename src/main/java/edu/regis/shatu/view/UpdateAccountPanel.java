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
package edu.regis.shatu.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import edu.regis.shatu.model.Account;
import edu.regis.shatu.view.act.BackToLogin;
import edu.regis.shatu.view.act.ChangePasswordAction;
import edu.regis.shatu.view.act.UpdateAcctAction;
import edu.regis.shatu.view.style.ColorScheme;

/**
 * A GUI that enables a user to update their account information.
 *
 * @author rickb
 * @author Henry Ordonez
 */
public class UpdateAccountPanel extends GPanel {
    
    /** Events of interest occurring in this class are logged to this logger. */
    private static final Logger LOGGER = Logger.getLogger(UpdateAccountPanel.class.getName());

    /** A regex pattern used to validate user email ids (e.g. "rick@regis.edu"). */
    public static final Pattern VALID_EMAIL_ADDRESS_REGEX
            = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    /** The account being created and displayed in this panel. */
    private Account model;

    /**
     * The editable fields appearing in this dialog.
     */
    protected JTextField userId;
    protected HintTextField fName;
    protected HintTextField lName;
    protected JComboBox secQuestions;
    protected JPasswordField secAnswer;
    protected JLabel strength;
    protected JLabel msg;
    protected JButton updateAcctBut;
    protected JButton backBut;
    protected JButton changePasswordBut;
    
    public UpdateAccountPanel() {
        super();

        model = new Account();

        initComponents();
        layoutPanel();

        enableButtons(fName.getDocument());
    }

    /**
     * Update and return the model with the view's account information.
     *
     * @return an Account object
     */
    public Account getModel() {
        updateModel();

        return model;
    }

    /**
     * Sets the given model in the view and updates the display.
     *
     * @param model an Account object
     */
    public void setModel(Account model) {
        this.model = model;
        updateDisplay();
    }

    public void updateFocus() {
        fName.requestFocusInWindow();
    }
    
    /**
     * Sets all of the text fields in this view to the empty string.
     */
    public void clearFields() {
        userId.setText("");
        fName.setText("");
        lName.setText("");
        secQuestions.setSelectedIndex(0);
        secAnswer.setText("");
    }

    /**
     * Updates the model with the current values contained in this view.
     */
    private void updateModel() {
        model.setUserId(userId.getText());
        model.setFirstName(fName.getText());
        model.setLastName(lName.getText());
        model.setSecurityQuestion(secQuestions.getSelectedIndex());
        char[] answerChars = secAnswer.getPassword();
        try {
            // Use the new secure char[] method
            model.setSecurityAnswer(encryptSHA256(answerChars));
        } finally {
            // Securely wipe the security answer from memory
            Arrays.fill(answerChars, '\0');
        };
    }

    /**
     * Update this view with the current values in the model.
     */
    private void updateDisplay() {
        userId.setText(model.getUserId());
        fName.setText(model.getFirstName());
        lName.setText(model.getLastName());
        secQuestions.setSelectedIndex(model.getSecurityQuestion());
        secAnswer.setText("");
    }

    /**
     * Initialize the components with the values that may be edited.
     */
    private void initComponents() {
        UpdateAccountDocumentListener docListener = new UpdateAccountDocumentListener();
        
        userId = new JTextField(model.getUserId(), 10);
        userId.setEditable(false);  // Users must not be allowed change their own ID
        
        fName = new HintTextField("First", 15);
        fName.getDocument().addDocumentListener(docListener);

        lName = new HintTextField("Last", 30);
        lName.getDocument().addDocumentListener(docListener);
        
        String questionsArray[] = {"What city were you born in?", "What is your mother's maiden name?"};
        secQuestions = new JComboBox(questionsArray);
        
        secAnswer = new JPasswordField(20);
        secAnswer.getDocument().addDocumentListener(docListener);

        updateAcctBut = new JButton(UpdateAcctAction.instance());
        updateAcctBut.setEnabled(false);

        backBut = new JButton(BackToLogin.instance());
        backBut.setEnabled(true);
        
        changePasswordBut = new JButton(ChangePasswordAction.instance());
    }

    private void layoutPanel() {
        setBackground(ColorScheme.REGIS_BLUE);

        setPreferredSize(new Dimension(300, 400));

        addc(createHeader(), 0, 0, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                5, 5, 5, 5);

        addc(createOverview(), 0, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                5, 5, 5, 5);

        addc(createForm(), 1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                10, 5, 5, 5);

        JLabel copyright = new JLabel("(C) 2019-2025 Johanna and Richard Blumenthal. All Rights Reserved");
        copyright.setFont(new Font("Dialog", Font.PLAIN, 10));

        copyright.setForeground(ColorScheme.REGIS_YELLOW);

        addc(copyright, 0, 2, 2, 1, 1.0, 1.0,
                GridBagConstraints.NORTH, GridBagConstraints.CENTER,
                5, 5, 5, 5);

        setSize(490, 400);
    }

    private GPanel createHeader() {
        GPanel panel = new GPanel();
        panel.setBackground(ColorScheme.REGIS_YELLOW);

        JLabel ccis = new JLabel("Regis University Department of Computer and Cyber Sciences");
        ccis.setFont(new Font("Dialog", Font.PLAIN, 20));
        ccis.setForeground(ColorScheme.REGIS_BLUE);

        panel.addc(ccis, 0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                5, 5, 5, 5);

        return panel;
    }

    private GPanel createOverview() {
        GPanel panel = new GPanel();
        panel.setBackground(ColorScheme.REGIS_YELLOW);

        panel.setSize(300, 400);
        panel.setPreferredSize(new Dimension(300, 400));

        JLabel logo = new JLabel("ShaTu");
        logo.setFont(new Font("Dialog", Font.PLAIN, 20));
        logo.setForeground(ColorScheme.REGIS_BLUE);

        panel.addc(logo, 0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                5, 5, 5, 5);

        JLabel description = new JLabel("Edit Account Information");
        description.setFont(new Font("Dialog", Font.PLAIN, 14));
        panel.addc(description, 0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                0, 5, 5, 5);

        JLabel loginMsg = new JLabel("To use the tutor, you must sign in.");
        panel.addc(loginMsg, 0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                5, 5, 5, 5);

        panel.addc(new JLabel(" "), 0, 4, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                5, 5, 5, 5);

        return panel;
    }

    private GPanel createForm() {
        GPanel panel = new GPanel();

        panel.setBackground(ColorScheme.REGIS_YELLOW);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 5));

        JLabel label = new JLabel("User Id");
        label.setLabelFor(userId);
        panel.addc(label, 0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                5, 5, 5, 5);

        panel.addc(userId, 0, 1, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                0, 5, 5, 5);

        label = new JLabel("Name");
        label.setLabelFor(fName);

        panel.addc(label, 0, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                5, 5, 5, 5);

        panel.addc(fName, 0, 3, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                5, 5, 5, 5);
        panel.addc(lName, 1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                5, 5, 5, 5);
        
        label = new JLabel("Choose Security Question:");
        label.setLabelFor(secQuestions);

        panel.addc(label, 0, 9, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                15, 5, 5, 5);
        
        panel.addc(secQuestions, 0, 10, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                0, 5, 5, 5);
        
        label = new JLabel("Answer:");
        label.setLabelFor(secAnswer);

        panel.addc(label, 0, 11, 2, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                15, 5, 5, 5);
        
        panel.addc(secAnswer, 0, 12, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                0, 5, 5, 5);

        msg = new JLabel("");
        msg.setFont(new Font("Dialog", Font.PLAIN, 10));
        msg.setForeground(new Color(173,7,1));

        panel.addc(msg, 0, 13, 2, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                5, 5, 5, 5);
                
        panel.addc(backBut, 0, 14, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                10, 5, 5, 5);
        panel.addc(changePasswordBut, 1, 14, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                10, 5, 5, 5);
        panel.addc(updateAcctBut, 2, 14, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                10, 5, 5, 5);

        return panel;
    }

    /**
     * If the userId or password fields are empty, disable the OK 'Login'
     * button.
     */
    private void enableButtons(Document e) {
        boolean isValidFName = !fName.isDefaultValue();
        if (isValidFName) {
            fName.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }
        else {
            fName.setBorder(BorderFactory.createLineBorder(new Color(173,7,1)));
        }

        boolean isValidLName = !lName.isDefaultValue();
        if (isValidLName) {
            lName.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }
        else {
            lName.setBorder(BorderFactory.createLineBorder(new Color(173,7,1)));
        }
        
        int isValid = secAnswer.getDocument().getLength();
        boolean isValidAnswer = isValid > 0;
        
        if (isValidFName && isValidLName && isValidAnswer) {
            updateAcctBut.setEnabled(true);
            msg.setText("");
        }
        else {
            updateAcctBut.setEnabled(false);
            msg.setText("(* Please fix problems highlighted in red.)");
        }
    }

    /**
     * Listens to changes made to the LoginDialog's userId and password fields
     * in order to appropriate enable the buttons in the dialog.
     */
    public class UpdateAccountDocumentListener implements DocumentListener {
        /**
         * As text is inserted into the fields, check whether
         * we need to enable or disable the LoginDialog's buttons.
         */
        @Override
        public void insertUpdate(DocumentEvent e) {
            enableButtons(e.getDocument());
        }

        /**
         * As text is removed from the fields, check whether
         * we need to enable or disable the LoginDialog's buttons.
         */
        @Override
        public void removeUpdate(DocumentEvent e) {
            enableButtons(e.getDocument());
        }

        /**
         * As text is changed in the fields, check whether we
         * need to enable or disable the LoginDialog's buttons.
         */
        @Override
        public void changedUpdate(DocumentEvent e) {
            enableButtons(e.getDocument());
        }
    }

    /**
     * Encrypt the given password using SHA-256.
     *
     * @param base the raw password String
     * @return a SHA-256 encrypted String
     */
    public static String encryptSHA256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);

                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Encrypt the given password securely using SHA-256 without creating 
     * immutable String objects in memory.
     *
     * @param passwordChars the raw password character array
     * @return a SHA-256 encrypted String
     */
    public static String encryptSHA256(char[] passwordChars) {
        ByteBuffer byteBuffer = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Securely convert char[] to byte[] using UTF-8 encoding
            CharBuffer charBuffer = CharBuffer.wrap(passwordChars);
            byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
            
            // Generate the hash from the underlying byte array
            byte[] hash = digest.digest(byteBuffer.array());
            
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);

                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        } finally {
            // Securely wipe the byte buffer that holds the plaintext bytes in memory
            if (byteBuffer != null && byteBuffer.hasArray()) {
                Arrays.fill(byteBuffer.array(), (byte) 0);
            }
        }
    }
}
