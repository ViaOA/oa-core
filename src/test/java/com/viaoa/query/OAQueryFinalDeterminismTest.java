package com.viaoa.query;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OAQueryFinalDeterminismTest implements OAQueryTokenType {
    @Test void sameQueryParsesToSameTokensRepeatedly() {
        String q = "(customer.lastName LIKE 'Sm%' and age >= 18) or id in (?)";
        Vector<OAQueryToken> first = tokens(q);
        for (int i = 0; i < 25; i++) assertSameTokens(first, tokens(q));
    }

    @Test void sameInvalidQueryFailsRepeatedly() {
        for (int i = 0; i < 25; i++) assertThrows(RuntimeException.class, () -> tokens("name = 'Bob' and"));
    }

    @Test void concurrentParsingWithSeparateInstancesIsStable() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Vector<OAQueryToken>>> calls = new ArrayList<>();
            for (int i = 0; i < 50; i++) calls.add(() -> tokens("name = 'Bob' and age >= 18"));
            List<Future<Vector<OAQueryToken>>> futures = es.invokeAll(calls);
            Vector<OAQueryToken> first = futures.get(0).get(5, TimeUnit.SECONDS);
            for (Future<Vector<OAQueryToken>> f : futures) assertSameTokens(first, f.get(5, TimeUnit.SECONDS));
        } finally {
            es.shutdownNow();
        }
    }

    @Test void queryFacadeRepeatedParseIsStable() {
        OAQuery q = new OAQuery();
        Vector<OAQueryToken> first = q.parse("name = 'Bob'");
        for (int i = 0; i < 10; i++) assertSameTokens(first, q.parse("name = 'Bob'"));
    }

    static Vector<OAQueryToken> tokens(String q) { return new OAQueryTokenizer().convertToTokens(q); }
    static void assertSameTokens(Vector<OAQueryToken> a, Vector<OAQueryToken> b) {
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).type, b.get(i).type, "type index=" + i);
            assertEquals(a.get(i).value, b.get(i).value, "value index=" + i);
        }
    }
}
