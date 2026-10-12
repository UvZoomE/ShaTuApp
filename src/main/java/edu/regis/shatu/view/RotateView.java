/**
 * SHATU: SHA-256 Tutor
 * 
 * (C) Johanna & Richard Blumenthal, All rights reserved
 * 
 * Unauthorized use, duplication, or distribution without the authors'
 * permission is strictly prohibited.
 * 
 * Unless required by applicable law or agreed to in writing, this software is
 * distributed on an "AS IS" basis without warranties or conditions of any kind,
 * either expressed or implied.
 */
package edu.regis.shatu.view;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import edu.regis.shatu.model.StepCompletion;
import edu.regis.shatu.model.aol.NewExampleRequest;
import edu.regis.shatu.model.aol.PendingTask;
import edu.regis.shatu.model.aol.ProblemType;
import edu.regis.shatu.model.aol.RotateStep;
import edu.regis.shatu.model.aol.StepSubType;
import edu.regis.shatu.model.aol.TutoringMode;
import edu.regis.shatu.model.steps.Step;

/**
 * RotateView class represents the GUI view for rotating strings using ROTR
 * (Right Rotate). It extends GPanel and implements ActionListener and
 * KeyListener interfaces.
 * 
 * The class provides a user interface for performing right rotations on strings
 * and checking the results. Inline comments have been added for better
 * understanding of the code.
 *
 * @author rickb
 */
public class RotateView extends UserRequestView implements KeyListener {

    TutoringSessionView view;
    private String problemString;
    private int numRotations;
    private JLabel prompt;
    private JLabel problem;
    private JTextField answerField;

    private JRadioButton shortProblem;
    private JRadioButton longProblem;
    private JRadioButton rightRotate;
    private JRadioButton rotate7Bits;
    private JRadioButton rotate16Bits;
    private ButtonGroup lengthType;
    private ButtonGroup rotationType;
    private ButtonGroup rotateAmount;
    private RotateStep currentStep;

    /**
     * Initializes the RotateView by creating and laying out its child
     * components.
     */
    public RotateView() {
        initializeComponents();
        initializeLayout();
    }

    /**
     * I think this could be done by ShaTuTutor but removing it breaks the class
     * 
     * Generates a NewExampleRequest to be sent to the tutor based on the
     * conditions selected by the user when newRequest() is called.
     *
     * @return The NewExampleRequest object to be sent to the tutor
     */
    @Override
    public NewExampleRequest newRequest() {
        NewExampleRequest ex = new NewExampleRequest();

        ex.setExampleType(ProblemType.ROTATE_BITS);
        RotateStep newStep = new RotateStep();
        if (shortProblem.isSelected()) {
            newStep.setLength(16);
        } else {
            newStep.setLength(32);
        }

        newStep.setDirection(RotateStep.Direction.RIGHT);

        if (rotate7Bits.isSelected()) {
            newStep.setAmount(7);
        } else {
            newStep.setAmount(16);
        }
        String rotateStepJson = gson.toJson(newStep);

        ex.setData(rotateStepJson);

        return ex;
    }

    @Override
    public StepCompletion stepCompletion() {
        Step currentStep = model.currentTask().currentStep().getStep();

        RotateStep example = gson.fromJson(currentStep.getData(), RotateStep.class);

        String userResponse = answerField.getText().replaceAll("\\s", "");

        example.setUserResponse(userResponse);

        StepCompletion step = new StepCompletion(currentStep, gson.toJson(example));
        step.setStep(currentStep);
        return step;
    }

    /**
     * Creates and initializes the child GUI components appearing in this frame.
     */
    private void initializeComponents() {
        prompt = new JLabel("Rotate n Bits");
        problem = new JLabel("Click 'New Example' to begin");

        answerField = new JTextField(10);
        answerField.addKeyListener(this);
        answerField.setHorizontalAlignment(JTextField.CENTER);
        answerField.setEnabled(false);

        shortProblem = new JRadioButton("16-bit");
        shortProblem.setSelected(true);

        longProblem = new JRadioButton("32-bit");

        rightRotate = new JRadioButton("Right Rotation");
        rightRotate.setSelected(true);

        rotate7Bits = new JRadioButton("Rotate 7 bits");
        rotate7Bits.setSelected(true);

        rotate16Bits = new JRadioButton("Rotate 16 bits");

        lengthType = new ButtonGroup();
        lengthType.add(shortProblem);
        lengthType.add(longProblem);

        rotationType = new ButtonGroup();
        rotationType.add(rightRotate);

        rotateAmount = new ButtonGroup();
        rotateAmount.add(rotate7Bits);
        rotateAmount.add(rotate16Bits);
        rotate16Bits.setEnabled(false);

        // Logic for radio buttons (selected/enabled)
        rotate7Bits.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rotate7Bits.isSelected()) {
                    shortProblem.setEnabled(true);
                }
            }
        });

        rotate16Bits.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rotate16Bits.isSelected()) {
                    shortProblem.setEnabled(false);
                    longProblem.setSelected(true);
                } else {
                    shortProblem.setEnabled(true);
                }
            }
        });

        shortProblem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (shortProblem.isSelected()) {
                    rotate7Bits.setSelected(true);
                    rotate16Bits.setEnabled(false);
                } else {
                    rotate16Bits.setEnabled(true);
                }
            }
        });

        longProblem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (longProblem.isSelected()) {
                    shortProblem.setEnabled(true);
                    rotate16Bits.setEnabled(true);
                }
            }
        });
    }

    /**
     * Lays out the child components in this view using GridBagConstraints.
     */
    private void initializeLayout() {
        GridBagConstraints c = new GridBagConstraints();
        addc(prompt, 2, 0, 2, 1, 0.2, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                5, 5, 5, 5);
        addc(problem, 2, 1, 2, 1, 0.2, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                5, 5, 5, 5);
        addc(answerField, 2, 2, 2, 1, 0.2, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                5, 5, 5, 5);
        addc(shortProblem, 0, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                5, 5, 5, 5);
        addc(longProblem, 0, 7, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                5, 5, 5, 5);
        addc(rotate7Bits, 3, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                5, 5, 5, 5);
        addc(rotate16Bits, 3, 7, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                5, 5, 5, 5);

        addc(buttonPanel, 0, 8, 4, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                5, 5, 5, 5);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_ENTER) {
            return;
        }
        if (answerField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Please provide an answer");
        } else if (checkButton.isEnabled()) {
            checkButton.doClick();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Update the view with the contents of a new step sent by the tutor
     */
    @Override
    protected void updateView() {
         if (!checkHintEnabled) {
            resetButtonListeners(); // Clear any listeners applied from other views
        }

        answerField.setText("");   

        Step step = model.currentTask().getCurrentStep().getStep();

        if (step.getSubType() == StepSubType.ROTATE_BITS) {
            RotateStep example = gson.fromJson(step.getData(), RotateStep.class);

            this.currentStep = example;

            String problemData = currentStep.getData();

            // SHA-256 only uses right rotation (ROTR)
            currentStep.setDirection(RotateStep.Direction.RIGHT);
            prompt.setText("Perform ROR(" + currentStep.getAmount() + ") on the following String:");

            if (problemData == null || problemData.isEmpty()) {
                prompt.setText("Rotate n Bits");
                problem.setText("Click 'New Example' to begin");
                checkButton.setEnabled(false);
                hintButton.setEnabled(false);
                answerField.setEnabled(false);
            } else {
                problem.setText(problemData);
                this.problemString = problemData;
                this.numRotations = currentStep.getAmount();
                checkButton.setEnabled(true);
                hintButton.setEnabled(true);
                answerField.setEnabled(true);
            }
        }
        

    }
    
    /**
     * Configure UI components based on the current tutoring mode
     * Disables fields not available for SEE_ONE mode for passive viewing.
     * 
     */
    @Override
    protected void configureModeSpecificUI() {
        super.configureModeSpecificUI();
        
        if(model == null) {
            return;
        }
        
        TutoringMode mode = model.getTutoringMode();
        
        if (mode == TutoringMode.SEE_ONE) {
            //Length Buttons
            shortProblem.setEnabled(false);
            longProblem.setEnabled(false);
            
            //Rotation type button
            rightRotate.setEnabled(false);
            
            //Rotation amount buttons
            rotate7Bits.setEnabled(false);
            rotate16Bits.setEnabled(false);
            
            //Field answer
            answerField.setEnabled(false);
        }
        // DO_ONE & TEACH_ONE
        else {
            //Length Buttons
            shortProblem.setEnabled(!rotate16Bits.isSelected());
            longProblem.setEnabled(true);
            
            //Rotation type button
            rightRotate.setEnabled(true);
            
            //Rotation amount buttons
            rotate7Bits.setEnabled(true);
            rotate16Bits.setEnabled(longProblem.isSelected());
            
            //Field answer
            answerField.setEnabled(true);
        }
    } 


    @Override
    public void setCurrentTask(PendingTask task) {
        this.model.addCurrentTask(task);
        updateView();
    }
}
