package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OATemplateLocaleAndDirectiveRegressionTest {

    static class Item extends OAObject {
    }

    private final Locale originalLocale = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void directiveRecognitionIsStableUnderTurkishLocaleDesiredContract() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        OATemplate<Item> t = new OATemplate<>("<%=IF $flag%>Y<%=END%>");
        t.setProperty("flag", true);

        assertEquals("Y", t.process(),
            "directive lowercasing should use Locale.ROOT, not default locale");
    }

    @Test
    void foreachDirectiveRecognitionIsStableUnderTurkishLocaleDesiredContract() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        OATemplate<Item> t = new OATemplate<>("<%=FOREACH%>x<%=END%>");

        assertDoesNotThrow(t::process);
    }

    @Test
    void propertyContainingEndIsStillPropertyInAnyLocaleDesiredContract() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        OATemplate<Item> t = new OATemplate<>("<%= $friend %>");
        t.setProperty("friend", "Buddy");

        assertEquals("Buddy", t.process());
    }

    @Test
    void propertyStartingWithForeachIsStillPropertyInAnyLocaleDesiredContract() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        OATemplate<Item> t = new OATemplate<>("<%= $foreachTotal %>");
        t.setProperty("foreachTotal", "10");

        assertEquals("10", t.process());
    }

    @Test
    void mixedCaseCommandsRemainRecognized() {
        OATemplate<Item> t = new OATemplate<>("<%=If $flag%>Y<%=EnD%>");
        t.setProperty("flag", true);

        assertEquals("Y", t.process());
    }
}
