package edu.regis.shatu.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UpdateAccountPanelHashTest {

    @Test
    void charArrayHashMatchesStringHashForAllLengths() {
        for (int len = 1; len <= 30; len++) {
            char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".substring(0, len).toCharArray();
            String expected = UpdateAccountPanel.encryptSHA256(new String(chars));

            assertEquals(expected, UpdateAccountPanel.encryptSHA256(chars),
                    "Hash mismatch for password length " + len);
        }
    }

    @Test
    void charArrayHashMatchesStringHashForNonAsciiInput() {
        char[] chars = "pässwörd-élan-ü".toCharArray();

        assertEquals(UpdateAccountPanel.encryptSHA256(new String(chars)),
                UpdateAccountPanel.encryptSHA256(chars));
    }
}
