package com.viaoa.schedule;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OADateChangeControllerTest {

    @Test
    void onChangeIgnoresNullCallback() {
        assertDoesNotThrow(() -> OADateChangeController.onChange(null));
    }
}
