package edu.regis.shatu.objectives;

import java.util.Random;

import edu.regis.shatu.model.KnowledgeComponentKind;
import edu.regis.shatu.model.StepCompletion;
import edu.regis.shatu.model.Student;
import edu.regis.shatu.model.TutoringSession;
import edu.regis.shatu.model.aol.BitOpExample;
import edu.regis.shatu.model.aol.BitOpStep;
import edu.regis.shatu.model.aol.ProblemType;
import edu.regis.shatu.model.aol.StepSubType;
import edu.regis.shatu.svc.TutorReply;

public class XorBits extends Objective {

    private static final int MAX_ASCII_SIZE = 20;

    private static final int MAX_BITS_SIZE = 32;

    public XorBits(Student student) {
        super(student);
    }

    /**
     * Handler for the Xor Bits Step Completion
     * TODO: Refactor so that:
     *  1.) Steps in the database are actually completed since as of now, none exist
     *  2.) Steps are completed for Tasks (Task table) in Units (Unit Table)
     *  3.) Steps are completed for each Unit (See One, Do One, Teach One)
     *  As of now, this is only logging assessment data (Assessment table) to the database based on the number of
     *  exposures, successes, and hints the user has completed during the Do One section of the application and it is
     *  not actually logging anything
     *
     * @param completion
     * @return
     */
    @Override
    public TutorReply hint(StepCompletion completion) {
        return genericHint(completion, KnowledgeComponentKind.XOR_BITS,
                "XOR operation results in 1 only when the bits are different");
    }

    /**
     * Handles client requests for a new XOR bits example.
     *
     * @return a TutorReply
     */
    @Override
    public TutorReply example(TutoringSession session, String jsonData) {
        Random rnd = new Random();

        BitOpExample example = gson.fromJson(jsonData, BitOpExample.class);

        int size = example.getPreSize();

        if (size == 0) {
            // ToDo: The tutor should generate the string length and timeout
            // based on the the current student model.
            size = rnd.nextInt(MAX_BITS_SIZE - 1) + 1;
            example.setTimeOut(600);

        } else if (size > MAX_BITS_SIZE) {
            // The student is requesting practice for a specific string length.
            size = MAX_ASCII_SIZE;
            example.setTimeOut(0);
        }

        example.generatedRandomOperands(size);

        int xor = (int) example.getOperand1Val() ^ (int) example.getOperand2Val();
        example.setResultVal(xor);

        BitOpStep subStep = new BitOpStep();
        subStep.setExample(example);
        // ToDo: multistep should be determined by the student model.
        subStep.setMultiStep(rnd.nextBoolean());

        return genericExample(subStep, StepSubType.XOR_BITS, ProblemType.XOR_BITS, KnowledgeComponentKind.XOR_BITS,
                "Xor the bits in the two operands");
    }

    @Override
    public TutorReply completeStep(StepCompletion completion) {
        System.out.println("Tutor completeXorBitsStep");

        BitOpStep example = gson.fromJson(completion.getData(), BitOpStep.class);

        String operand1 = example.getExample().getOperand1();
        String operand2 = example.getExample().getOperand2();
        String result = example.getExample().getResult();

        String expectedResult = xorBitsFunction(operand1, operand2);

        return genericComplete(expectedResult, result, KnowledgeComponentKind.XOR_BITS);
    }

    private String xorBitsFunction(String operand1, String operand2) {
        StringBuilder result = new StringBuilder();
        int length = Math.min(operand1.length(), operand2.length());

        for (int i = 0; i < length; i++) {
            char bit1 = operand1.charAt(i);
            char bit2 = operand2.charAt(i);
            char xorBit = (char) (((bit1 - '0') ^ (bit2 - '0')) + '0');
            result.append(xorBit);
        }

        return result.toString();
    }
}
