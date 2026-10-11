package edu.regis.shatu.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CredentialsPanelTest {

    @BeforeAll
    static void setupHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void signInStartsEmptyAndRequiresBothFields() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SplashPanel panel = new SplashPanel();
            assertEquals("", panel.userId.getText());
            assertEquals(0, panel.password.getPassword().length);
            assertFalse(panel.getSigninButton().isEnabled());

            panel.userId.setText("student@example.edu");
            assertFalse(panel.getSigninButton().isEnabled());
            panel.password.setText("a password");
            assertTrue(panel.getSigninButton().isEnabled());
            panel.password.setText("");
            assertFalse(panel.getSigninButton().isEnabled());
        });
    }

    @Test
    void accountCreationRequiresValidUserInputAndDisablesAfterClear() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            NewAccountPanel panel = new NewAccountPanel();
            assertTrue(panel.fName.isDefaultValue());
            assertTrue(panel.lName.isDefaultValue());
            assertTrue(panel.userId.isDefaultValue());
            assertEquals(0, panel.pass1.getPassword().length);
            assertEquals(0, panel.pass2.getPassword().length);
            assertEquals(0, panel.secAnswer.getPassword().length);
            assertFalse(panel.createAcctBut.isEnabled());

            panel.fName.setText("Jane");
            panel.lName.setText("Doe");
            panel.userId.setText("jane@example.edu");
            panel.pass1.setText("secret123");
            panel.pass2.setText("different");
            panel.secAnswer.setText("Denver");
            assertFalse(panel.createAcctBut.isEnabled());

            panel.pass2.setText("secret123");
            assertTrue(panel.createAcctBut.isEnabled());
            panel.clearFields();
            assertFalse(panel.createAcctBut.isEnabled());
        });
    }
}
