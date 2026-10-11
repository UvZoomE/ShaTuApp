package edu.regis.shatu.objectives;

import java.util.Random;

import edu.regis.shatu.err.NonRecoverableException;
import edu.regis.shatu.model.Hint;
import edu.regis.shatu.model.KnowledgeComponentKind;
import edu.regis.shatu.model.StepCompletion;
import edu.regis.shatu.model.Student;
import edu.regis.shatu.model.StudentModelFieldKind;
import edu.regis.shatu.model.TutoringSession;
import edu.regis.shatu.model.aol.Assessment;
import edu.regis.shatu.model.aol.PendingStep;
import edu.regis.shatu.model.aol.ProblemType;
import edu.regis.shatu.model.aol.StepSubType;
import edu.regis.shatu.model.aol.Timeout;
import edu.regis.shatu.model.steps.BitShiftStep;
import edu.regis.shatu.model.steps.Step;
import edu.regis.shatu.model.steps.StepCompletionReply;
import edu.regis.shatu.svc.ServiceFactory;
import edu.regis.shatu.svc.StudentModelSvc;
import edu.regis.shatu.svc.TutorReply;

public class ShiftBits extends Objective {

    public ShiftBits(Student student) {
        super(student);
    }

    @Override
    public TutorReply hint(StepCompletion completion) {
        System.out.println("Tutor hintShiftBits");

        BitShiftStep example = gson.fromJson(completion.getData(), BitShiftStep.class);
        String operand = example.getOperand();
        int shiftLength = example.getShiftLength();
        boolean shiftRight = example.isShiftRight();
        int bitLength = example.getBitLength();
        String expectedResult = bitShiftFunction(operand,
                shiftLength,
                shiftRight,
                bitLength);
        int displacedBits = Math.min(shiftLength, bitLength);

        StepCompletionReply stepReply = new StepCompletionReply();
        stepReply.setCorrectAnswer(expectedResult);
        // ToDo: What is this?
        //stepReply.setResponse(result);

        stepReply.setIsCorrect(false);
        stepReply.setIsRepeatStep(true);
        stepReply.setIsNewStep(false);
        stepReply.setIsNewTask(false);
        stepReply.setIsNextStep(false);

        Hint hintOne = new Hint();
        hintOne.setId(0);
        String hintText = "There will be " + displacedBits + " zeros on the "
                + (shiftRight ? "left" : "right");
        hintOne.setText(hintText);

        Hint hintTwo = new Hint();
        hintTwo.setId(1);
        hintText = "Remove " + displacedBits + " bits from the "
                + (shiftRight ? "right" : "left");
        hintTwo.setText(hintText);

        Step step = completion.getStep();
        step.addHint(hintOne);
        step.addHint(hintTwo);

        step.setSubType(StepSubType.REQUEST_HINT);
        // ToDo: fix timeouts
        Timeout timeout = new Timeout("Complete Step", 0, ":No-Op", "Exceed time");
        step.setTimeout(timeout);
        step.setData(gson.toJson(stepReply));

        PendingStep pendingStep = new PendingStep(step);
        pendingStep.setCurrentHintIndex(0);
        pendingStep.setNotifyTutor(true);
        pendingStep.setIsCompleted(false);

        TutorReply reply = new TutorReply(":Success");

        reply.setData(gson.toJson(pendingStep));

        // Update the assessment data and save it to the database.
        int dbId = KnowledgeComponentKind.fromString("Shift Bits").dbId();
        Assessment assessment = studentModel.findAssessment(dbId);
        assessment.incrementHints();

        try {
            StudentModelSvc modelSvc = ServiceFactory.findStudentModelSvc();
            modelSvc.updateAssessment(studentModel, assessment, StudentModelFieldKind.HINTS);

        } catch (NonRecoverableException ex) {
            return createError("Unknown error", ex);
        }

        return reply;
    }

    /**
     * Handles client requests for a new shift bits zeros example.
     *
     * @return a TutorReply
     */
    @Override
    public TutorReply example(TutoringSession session, String jsonData) {
        System.out.println("newShiftBitsExample");
        BitShiftStep substep = gson.fromJson(jsonData, BitShiftStep.class);

        int bitLength = substep.getBitLength();
        validateBitLength(bitLength);

        String operand = generateInputString(bitLength);
        int shiftLength = new Random().nextInt(bitLength);
        boolean shiftRight = substep.isShiftRight();

        substep.setOperand(operand);
        substep.setShiftLength(shiftLength);
        substep.setShiftRight(shiftRight);

        substep.setResult(bitShiftFunction(operand, shiftLength, shiftRight, bitLength));

        return genericExample(substep, StepSubType.SHIFT_BITS, ProblemType.SHIFT_BITS,
                KnowledgeComponentKind.SHIFT_BITS, "Compute the result of the bitshift on the operand");
    }

    /**
     * Handler for the Shift Bits Step Completion
     * TODO: Refactor so that:
     * 1.) Steps in the database are actually completed since as of now, none exist
     * 2.) Steps are completed for Tasks (Task table) in Units (Unit Table)
     * 3.) Steps are completed for each Unit (See One, Do One, Teach One)
     * As of now, this is only logging assessment data (Assessment table) to the
     * database based on the number of
     * exposures, successes, and hints the user has completed during the Do One
     * section of the application and it is
     * not actually logging anything
     *
     * @param completion
     * @return
     */
    @Override
    public TutorReply completeStep(StepCompletion completion) {
        System.out.println("Tutor completeShiftBitsStep");

        BitShiftStep example = gson.fromJson(completion.getData(), BitShiftStep.class);
        String operand = example.getOperand();
        int shiftLength = example.getShiftLength();
        boolean shiftRight = example.isShiftRight();
        int bitLength = example.getBitLength();
        String result = example.getResult();

        String expectedResult = bitShiftFunction(operand,
                shiftLength,
                shiftRight,
                bitLength);

        return genericComplete(expectedResult, result, KnowledgeComponentKind.SHIFT_BITS);
    }

    /**
     * Performs a bit shift (left or right) on a binary string operand.
     *
     * @param operand     The binary string to be shifted.
     * @param shiftLength The nonnegative number of positions to shift the bits.
     * @param bitLength   The length of the resulting binary string.
     * @param shiftRight  If true, performs a right shift; if false, performs a
     *                    left shift.
     * @return A binary string of exactly bitLength bits after shifting.
     */
    String bitShiftFunction(String operand, int shiftLength, boolean shiftRight, int bitLength) {
        validateBitLength(bitLength);
        if (shiftLength < 0) {
            throw new IllegalArgumentException("shiftLength must not be negative");
        }
        if (operand == null) {
            throw new IllegalArgumentException("operand must contain exactly " + bitLength + " binary digits");
        }

        String bits = operand.replaceAll("\\s", "");
        if (bits.length() != bitLength || !bits.matches("[01]+")) {
            throw new IllegalArgumentException("operand must contain exactly " + bitLength + " binary digits");
        }

        long value = Long.parseLong(bits, 2);
        long mask = (1L << bitLength) - 1;
        // A shift by the full width (or more) clears every bit. Java masks the
        // shift count for long values, which would otherwise wrap at 64.
        long shifted = shiftLength >= bitLength ? 0
                : shiftRight ? value >>> shiftLength : value << shiftLength;

        return formatResult(shifted & mask, bitLength);
    }

    private static void validateBitLength(int bitLength) {
        if (bitLength != 4 && bitLength != 8 && bitLength != 16 && bitLength != 32) {
            throw new IllegalArgumentException("bitLength must be 4, 8, 16, or 32");
        }
    }
}
