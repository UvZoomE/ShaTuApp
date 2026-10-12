package edu.regis.shatu.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import edu.regis.shatu.model.aol.RotateStep;

class RotateViewTest {
    private static final Gson GSON = new Gson();

    private static JRadioButton radio(RotateView view, String fieldName) throws Exception {
        Field field = RotateView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (JRadioButton) field.get(view);
    }

    private static RotateStep requestStep(RotateView view) {
        return GSON.fromJson(view.newRequest().getData(), RotateStep.class);
    }

    @Test
    void initialSelectionRequestsSixteenBitsRotatedBySeven() throws Exception {
        AtomicReference<RotateView> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new RotateView()));
        RotateView view = ref.get();
        JRadioButton seven = radio(view, "rotate7Bits");
        JRadioButton sixteen = radio(view, "rotate16Bits");

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(seven.isSelected());
            assertFalse(sixteen.isEnabled());
            assertEquals(16, requestStep(view).getLength());
            assertEquals(7, requestStep(view).getAmount());
        });
    }

    @Test
    void switchingBackToSixteenBitsKeepsAnAmountSelected() throws Exception {
        AtomicReference<RotateView> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new RotateView()));
        RotateView view = ref.get();
        JRadioButton shortProblem = radio(view, "shortProblem");
        JRadioButton longProblem = radio(view, "longProblem");
        JRadioButton seven = radio(view, "rotate7Bits");
        JRadioButton sixteen = radio(view, "rotate16Bits");

        SwingUtilities.invokeAndWait(() -> {
            longProblem.doClick();
            assertTrue(sixteen.isEnabled());
            sixteen.doClick();
            assertEquals(32, requestStep(view).getLength());
            assertEquals(16, requestStep(view).getAmount());

            seven.doClick();
            assertTrue(shortProblem.isEnabled());
            shortProblem.doClick();
            assertTrue(seven.isSelected());
            assertFalse(sixteen.isSelected());
            assertFalse(sixteen.isEnabled());
            assertEquals(16, requestStep(view).getLength());
            assertEquals(7, requestStep(view).getAmount());
        });
    }

    @Test
    void enterSubmitsAnAnswerWhenCheckIsEnabled() throws Exception {
        AtomicReference<RotateView> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new RotateView()));
        RotateView view = ref.get();
        Field answer = RotateView.class.getDeclaredField("answerField");
        answer.setAccessible(true);
        JTextField input = (JTextField) answer.get(view);
        AtomicInteger checks = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            // Observe the button click without sending a request to the tutor.
            for (ActionListener listener : view.checkButton.getActionListeners()) {
                view.checkButton.removeActionListener(listener);
            }
            view.checkButton.addActionListener(event -> checks.incrementAndGet());
            view.checkButton.setEnabled(true);
            input.setText("1010101010101010");

            view.keyPressed(new KeyEvent(input, KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'));
            assertEquals(1, checks.get());

            view.keyPressed(new KeyEvent(input, KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a'));
            assertEquals(1, checks.get());
        });
    }
}
