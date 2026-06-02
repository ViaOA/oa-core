package com.viaoa.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OATemplateConcurrentStateTest {

    public static class Item extends OAObject {
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private static Hub<Item> hub(String prefix) {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item(prefix + "1"));
        hub.add(new Item(prefix + "2"));
        hub.add(new Item(prefix + "3"));
        return hub;
    }

    @Test
    void independentTemplateInstancesCanRenderConcurrently() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                int x = i;
                tasks.add(() -> {
                    OATemplate<Item> t = new OATemplate<>("<%=foreach%><%=#counter%>:<%=name%>;<%=end%>");
                    return t.process(hub("H" + x + "-"));
                });
            }

            List<Future<String>> futures = es.invokeAll(tasks);
            for (int i = 0; i < futures.size(); i++) {
                assertEquals("1:H" + i + "-1;2:H" + i + "-2;3:H" + i + "-3;", futures.get(i).get(5, TimeUnit.SECONDS));
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void sharedTemplateConcurrentCounterStateDoesNotCrossContaminateDesiredContract() throws Exception {
        OATemplate<Item> t = new OATemplate<>("<%=foreach%><%=#counter%>:<%=name%>;<%=end%>");

        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                int x = i;
                tasks.add(() -> t.process(hub("H" + x + "-")));
            }

            List<Future<String>> futures = es.invokeAll(tasks);
            for (int i = 0; i < futures.size(); i++) {
                String s = futures.get(i).get(5, TimeUnit.SECONDS);
                assertTrue(s.matches("1:H\\d+-1;2:H\\d+-2;3:H\\d+-3;"),
                    "shared template counter state must be render-local/thread-safe, got=" + s);
            }
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void stopProcessingOnOneTemplateDoesNotCancelAnotherTemplate() {
        OATemplate<Item> a = new OATemplate<>("A");
        OATemplate<Item> b = new OATemplate<>("B");

        a.stopProcessing();

        assertEquals("cancelled", a.process());
        assertEquals("B", b.process());
    }

    @Test
    void repeatedSequentialSharedTemplateCounterDoesNotLeak() {
        OATemplate<Item> t = new OATemplate<>("<%=foreach%><%=#counter%><%=end%>");

        assertEquals("123", t.process(hub("A")));
        assertEquals("123", t.process(hub("B")));
        assertEquals("123", t.process(hub("C")));
    }
}
