/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.converter;

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
 * @see OAConverter
 */
public class OAConv extends OAConverter {
}
