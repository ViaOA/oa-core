package com.viaoa.util;

/**
 * Convenience subclass of {@link OAConverter} providing a shorter and more
 * readable class name for common conversion use cases.
 *
 * <p>
 * {@code OAConv} exposes all functionality of {@link OAConverter} and is
 * interchangeable in every context. The intent is purely ergonomic:
 * developers who frequently perform value conversions may prefer the
 * simpler class name.
 * </p>
 *
 * <p>
 * Typical usage examples:
 * </p>
 *
 * <pre>{@code
 * int qty = OAConv.toInt(userInput);
 * BigDecimal amt = OAConv.toBigDecimal(priceString);
 * LocalDate date = OAConv.convert(LocalDate.class, dateString);
 * String s = OAConv.convertToString(someValue, "MM/dd/yyyy");
 * }</pre>
 *
 * <p>
 * No behavior or logic differences exist between this class and
 * {@code OAConverter}; it is simply a more concise, developer-friendly alias.
 * </p>
 *
 * @author Vince Via
 * @see OAConverter
 */
public class OAConv extends OAConverter {
}
