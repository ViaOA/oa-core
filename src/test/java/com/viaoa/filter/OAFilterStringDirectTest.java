package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterStringDirectTest {

    @Test
    void likeAndNotLikeUseOACompareLikeSemantics() {
        assertTrue(new OALikeFilter("ab*de").isUsed("abcde"));
        assertTrue(new OALikeFilter("ab%de").isUsed("abcde"));
        assertFalse(new OALikeFilter("ab*z").isUsed("abcde"));

        assertFalse(new OANotLikeFilter("ab*de").isUsed("abcde"));
        assertTrue(new OANotLikeFilter("ab*z").isUsed("abcde"));
    }

    @Test
    void containsFilterUsesSubstringSemantics() {
        assertTrue(new OAContainsFilter("bc").isUsed("abcde"));
        assertFalse(new OAContainsFilter("BC").isUsed("abcde"));
        assertTrue(new OAContainsFilter("BC", true).isUsed("abcde"));
        assertFalse(new OAContainsFilter("xy").isUsed("abcde"));
    }

    @Test
    void startsWithFilterUsesPrefixSemantics() {
        assertTrue(new OAStartsWithFilter("ab").isUsed("abcde"));
        assertFalse(new OAStartsWithFilter("AB").isUsed("abcde"));
        assertTrue(new OAStartsWithFilter("AB", true).isUsed("abcde"));
        assertFalse(new OAStartsWithFilter("bc").isUsed("abcde"));
    }

    @Test
    void indexOfFilterUsesContainsAtOrAfterZeroCurrentContract() {
        assertTrue(new OAIndexOfFilter("bc").isUsed("abcde"));
        assertFalse(new OAIndexOfFilter("BC").isUsed("abcde"));
        assertTrue(new OAIndexOfFilter("BC", true).isUsed("abcde"));
        assertFalse(new OAIndexOfFilter("xy").isUsed("abcde"));
    }

    @Test
    void stringFiltersHandleNullCandidateDeterministically() {
        assertFalse(new OALikeFilter("*").isUsed(null));
        assertTrue(new OANotLikeFilter("*").isUsed(null));

        assertFalse(new OAContainsFilter("x").isUsed(null));
        assertFalse(new OAStartsWithFilter("x").isUsed(null));
        assertFalse(new OAIndexOfFilter("x").isUsed(null));
    }

    @Test
    void nullSearchValueCurrentContractIsDocumented() {
        assertFalse(new OAContainsFilter(null).isUsed("abc"));
        assertFalse(new OAStartsWithFilter(null).isUsed("abc"));
        assertFalse(new OAIndexOfFilter(null).isUsed("abc"));
    }
}
