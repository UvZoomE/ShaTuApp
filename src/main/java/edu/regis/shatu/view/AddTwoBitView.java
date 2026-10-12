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
* Set responseTextArea to disabled at initialization per SHAT-225 John hennessey 23 Feb 2025
*/
package edu.regis.shatu.view;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.math.BigInteger;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.regis.shatu.model.StepCompletion;
import edu.regis.shatu.model.aol.BitOpStep;
import edu.regis.shatu.model.aol.NewExampleRequest;
import edu.regis.shatu.model.aol.ProblemType;
import edu.regis.shatu.model.aol.StepSubType;
import edu.regis.shatu.model.steps.Step;
import edu.regis.shatu.model.aol.TutoringMode;

/**
 * AddTwoBitView class represents a GUI view for adding two binary numbers
 * modulo 2^m. Users can input their answers in a JTextField and check
 * correctness. Provides functionality for hints and moving to the next
 * question.
 *
 * @author rickb
 * @author Amanda Roskelley
 */
public class AddTwoBitView extends UserRequestView implements KeyListener {

    /**
     * The width of the current problem. Addition is modulo 2^m. Updated from
     * each new example the tutor sends.
     */
    private int m = 8;

    private String binary1 = "";
    private String binary2 = "";
    private String result = "";

    private TutoringSessionView view;
    private JTextField responseTextArea;
    private JLabel instructionLabel;
    private JLabel stringLabel1;
    private JLabel stringLabel2;
    private JLabel stringLabel3;
    private JLabel stringLabel4; // Only for testing that view is communicating with server

    /**
     * Initializes the AddTwoBitView by creating and laying out its child
     * components.
     */
    public AddTwoBitView() {
        initializeComponents();
        initializeLayout();
    }

    /**
     * Creates child GUI components for the view.
     */
    private void initializeComponents() {
        instructionLabel = new JLabel("Add two binary numbers using modulo 2^" + m + " addition");

        stringLabel1 = new JLabel("binary number1 : ");
        stringLabel2 = new JLabel("binary number2 : ");
        stringLabel3 = new JLabel("Hit New Example to get set of binary numbers");

        stringLabel4 = new JLabel(); // only for testing that view is communicating with server

        responseTextArea = new JTextField(10);
        responseTextArea.addKeyListener(this);

    }

    /**
     * Lays out the child components in the view.
     */
    private void initializeLayout() {
        GridBagConstraints centerConstraints = new GridBagConstraints();
        centerConstraints.anchor = GridBagConstraints.CENTER;
        centerConstraints.insets = new Insets(5, 5, 5, 5);

        // Add instructionLabel centered
        addc(instructionLabel, 0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                5, 5, 5, 5);

        // Add binaryNumberOneLabel centered below instructionLabel
        addc(stringLabel1, 0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                5, 5, 5, 5);

        // Add binaryNumberTwoLabel centered below binaryNumberOneLabel
        addc(stringLabel2, 0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                5, 5, 5, 5);

        addc(stringLabel3, 0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                5, 5, 5, 5);

        // To provide answer for easier testing during build of application
        addc(stringLabel4, 0, 9, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                5, 5, 5, 5);

        // Add responseTextArea centered below binaryNumberTwoLabel
        addc(responseTextArea, 0, 4, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                5, 5, 5, 5);

        responseTextArea.setEnabled(false); // Text area disabled at initialization

        addc(buttonPanel, 0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                5, 5, 5, 5);

    }

    /**
     * Calculates the modulo addition of two binary numbers.
     *
     * @param binary1 The first binary number.
     * @param binary2 The second binary number.
     * @return The result of the modulo addition in binary form.
     */
    private String calculateModulo(String binary1, String binary2) {
        if (binary1 == null || binary1.isEmpty()) {
            return "";
        }
        if (binary2 == null || binary2.isEmpty()) {
            return "";
        }

        // Convert binary strings to BigIntegers
        BigInteger num1 = new BigInteger(binary1, 2);
        BigInteger num2 = new BigInteger(binary2, 2);

        // Perform addition
        BigInteger sum = num1.add(num2);

        // Calculate the result modulo 2^256
        BigInteger modulo = new BigInteger("2").pow(m);
        BigInteger result = sum.mod(modulo);

        // Convert the result back to a binary string
        String resultBinary = result.toString(2);

        // Ensure the binary string has 256 bits (pad with leading zeros if necessary)
        while (resultBinary.length() < m) {
            resultBinary = "0" + resultBinary;
        }

        System.out.println("Result : " + resultBinary);

        return resultBinary;
    }

    /**
     * Handles the keyTyped event for the view.
     *
     * @param e The KeyEvent that occurred.
     */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Handles the keyPressed event for the view.
     *
     * @param e The KeyEvent that occurred.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER && responseTextArea.getText().equals("")) {
            JOptionPane.showMessageDialog(this, "Please provide an answer");
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            checkButton.doClick();
        }
    }

    /**
     * Handles the keyReleased event for the view.
     *
     * @param e The KeyEvent that occurred.
     */
    @Override
    public void keyReleased(KeyEvent e) {
    }

    /**
     * NO LONGER NEEDED KEPT FOR REFRENCE
     * Verifies the user's answer against the correct answer.

    private void verifyAnswer() {
        String correctAnswer = calculateModulo(binary1, binary2);
        // Get the text from the responseTextArea when the checkButton is clicked
        String userAnswer = responseTextArea.getText();

        if (userAnswer.equals(correctAnswer)) {
            JOptionPane.showMessageDialog(this, "Correct");
        } else {
            JOptionPane.showMessageDialog(this, "Incorrect. The correct answer is: " + correctAnswer);
        }
    }
     */

    /**
     * NO LONGER NEEDED KEPT FOR REFRENCE
     * Handles the action for the Next Question button.
    private void onNextQuestion() {
        // JOptionPane.showMessageDialog(this, "Next Question");
    }
     */

    /**
     * NO LONGER NEEDED KEPT FOR REFRENCE
     * Handles the action for the Hint button.
    private void onNextHint() {
        JOptionPane.showMessageDialog(this, "Hint");
    }
     */

    /**
     * NO LONGER NEEDED KEPT FOR REFRENCE
     * Handles the action for the Check button.
    private void onCheckButton() {
        if (responseTextArea.getText().equals("")) {
            JOptionPane.showMessageDialog(this, "Please provide an answer");
        }
    }
     */

    /**
     * Create and return the server request this view makes when a user selects
     * that they want to practice a new add two n bits example.
     *
     * @return
     */
    @Override
    public NewExampleRequest newRequest() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        NewExampleRequest ex = new NewExampleRequest();

        ex.setExampleType(ProblemType.ADD_BITS);

        BitOpStep newStep = new BitOpStep();

        ex.setData(gson.toJson(newStep));

        return ex;
    }

    /**
     * Gets the current step, then gets the next example, completes the current
     * step and then gets the next step.
     *
     * @return
     */
    @Override
    public StepCompletion stepCompletion() {
        Step currentStep = model.currentTask().currentStep().getStep();

        BitOpStep example = gson.fromJson(currentStep.getData(), BitOpStep.class);

        String userResponse = responseTextArea.getText().replaceAll("\\s", "");

        example.getExample().setResult(userResponse);

        StepCompletion step = new StepCompletion(currentStep, gson.toJson(example));

        step.setStep(currentStep);

        return step;
    }

    /**
     * Update the view with the new operands.
     *
     */
    @Override
    protected void updateView() {
             responseTextArea.setText("");

        // If check and hint buttons are disabled, reset listenerers and apply those
        // used by this view
        if (!checkHintEnabled) {
            resetButtonListeners(); // Clear any listeners applied from other views
        }

        if (model != null) {
            // ****TO-DO*****
            // Update the view's information from the model
            // Debugging dynamic updates to the model can be done here.
            System.out.println("AddTwoBitView");

            Step step = model.currentTask().getCurrentStep().getStep();

            if (step.getSubType() == StepSubType.ADD_BITS) {
                BitOpStep example = gson.fromJson(step.getData(), BitOpStep.class);

                try {
                    binary1 = example.getExample().getOperand1();
                    binary2 = example.getExample().getOperand2();

                    if (example.getExample().getPostSize() > 0) {
                        m = example.getExample().getPostSize();
                    }
                    instructionLabel.setText("Add two binary numbers using modulo 2^" + m + " addition");

                    result = calculateModulo(binary1, binary2);
                    checkButton.setEnabled(true);
                    responseTextArea.setEnabled(true);
                    hintButton.setEnabled(true);
                } catch (NullPointerException e) {
                    System.out.println("Example is empty.");
                    checkButton.setEnabled(false);
                    responseTextArea.setEnabled(false);
                    hintButton.setEnabled(false);
                }

                stringLabel1.setText("binary number1: " + binary1);
                stringLabel2.setText("binary number2: " + binary2);
            }
        }
    }
    
    /**
     * Configure UI components based on the current tutoring mode
     *Disables fields not available for SEE_ONE mode for passive viewing.
     * 
     */
    
    @Override
    protected void configureModeSpecificUI() {
        super.configureModeSpecificUI();//call parent to handle buttons
        
        if(model == null) {
            return;
        }
        
        TutoringMode mode = model.getTutoringMode();
        
        if (mode == TutoringMode.SEE_ONE) {
            responseTextArea.setEnabled(false);
        }
        // DO_ONE & TEACH_ONE
        else {
            responseTextArea.setEnabled(true);
        }
    }
    
}