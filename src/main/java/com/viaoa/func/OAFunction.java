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
package com.viaoa.func;

import com.viaoa.compare.OACompare;
import com.viaoa.converter.OAConv;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OADouble;
import com.viaoa.lang.OAInteger;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.template.OATemplate;

/**
 * Utility functions that evaluate values across an OAObject graph using
 * property-path traversal. These functions use {@link com.viaoa.find.OAFinder}
 * to walk relationships from a root {@link com.viaoa.object.OAObject} or
 * {@link com.viaoa.hub.Hub}, applying aggregation logic to the objects
 * encountered along the path. <p>
 *
 * Supported operations include:
 * <ul>
 *   <li>Counting objects reachable through a property path.</li>
 *   <li>Summing numeric property values.</li>
 *   <li>Computing minimum and maximum property values.</li>
 *   <li>Evaluating text templates using {@link com.viaoa.template.OATemplate}.</li>
 *   <li>Computing string lengths from an object's property value.</li>
 * </ul>
 *
 * Each function splits property paths into navigation and terminal-property
 * segments when needed, and converts or compares values using OA utility
 * classes such as {@link com.viaoa.converter.OAConv} and
 * {@link com.viaoa.compare.OACompare}. Traversal always processes the full set of
 * reachable objects and does not short-circuit. <p>
 *
 * OAFunction forms the foundation for high-level expressions used in
 * templates, reports, and dynamically computed UI or domain values.
 */
public class OAFunction {

	/**
	 * Counts the number of OAObjects reachable by traversing the supplied
	 * property path beginning at the given root object. Uses an {@link OAFinder}
	 * that increments a counter for each visited object.
	 *
	 * @param obj the starting {@link OAObject}; returns 0 if null
	 * @param pp  the property path used for traversal; returns 0 if empty
	 * @return the number of objects encountered along the path
	 */
	public static int count(OAObject obj, String pp) {
		if (obj == null || OAString.isEmpty(pp)) {
			return 0;
		}
		OAInteger cnt = new OAInteger();
		OAFinder f = new OAFinder(obj, pp) {
			@Override
			protected boolean isUsed(OAObject obj) {
				cnt.add();
				return false;
			}
		};
		f.find();
		return cnt.get();
	}

	/**
	 * Counts the number of OAObjects reachable by traversing the supplied
	 * property path beginning at the objects contained in the given Hub.
	 * Uses an {@link OAFinder} that increments a counter for each visited
	 * object.
	 *
	 * @param hub the Hub serving as the traversal root; returns 0 if null
	 * @param pp  the property path used for traversal; returns 0 if empty
	 * @return the number of objects encountered along the path
	 */
	public static int count(Hub hub, String pp) {
		if (hub == null || OAString.isEmpty(pp)) {
			return 0;
		}
		OAInteger cnt = new OAInteger();
		OAFinder f = new OAFinder(hub, pp) {
			@Override
			protected boolean isUsed(OAObject obj) {
				cnt.add();
				return false;
			}
		};
		f.find();
		return cnt.get();
	}

	/**
	 * Computes the sum of numeric property values reachable through the
	 * supplied property path beginning at the given root object. The path
	 * is split into the navigation segment and the terminal property.
	 *
	 * @param obj the starting {@link OAObject}; returns 0 if null
	 * @param pp  full property path to the numeric value; returns 0 if empty
	 * @return the computed sum of all values found
	 */
	public static double sum(OAObject obj, String pp) {
		if (obj == null || OAString.isEmpty(pp)) {
			return 0;
		}
		String pp1, pp2;
		int x = pp.lastIndexOf('.');
		if (x < 0) {
			pp1 = null;
			pp2 = pp;
		} else {
			pp1 = pp.substring(0, x);
			pp2 = pp.substring(x + 1);
		}
		return sum(obj, pp1, pp2);
	}

	/**
	 * Computes the sum of numeric property values reachable through the
	 * supplied property path beginning at all objects contained in the Hub.
	 * The path is split into the navigation segment and the terminal property.
	 *
	 * @param hub the Hub serving as the traversal root; returns 0 if null
	 * @param pp  full property path to the numeric value; returns 0 if empty
	 * @return the computed sum of all values found
	 */
	public static double sum(Hub hub, String pp) {
		if (hub == null || OAString.isEmpty(pp)) {
			return 0;
		}
		String pp1, pp2;
		int x = pp.lastIndexOf('.');
		if (x < 0) {
			pp1 = null;
			pp2 = pp;
		} else {
			pp1 = pp.substring(0, x);
			pp2 = pp.substring(x + 1);
		}
		return sum(hub, pp1, pp2);
	}

	/**
	 * Computes the sum of numeric property values for objects reached by
	 * the navigation property path and extracts the numeric value from the
	 * terminal property. Values are converted using {@link OAConv#toDouble}.
	 *
	 * @param obj        the starting {@link OAObject}; returns 0 if null
	 * @param ppToObject navigation property path to reach objects
	 * @param pp         terminal property name containing numeric values
	 * @return the computed sum
	 */
	public static double sum(OAObject obj, String ppToObject, String pp) {
		if (obj == null || OAString.isEmpty(pp)) {
			return 0;
		}
		OADouble sum = new OADouble();

		OAFinder f = new OAFinder(obj, ppToObject) {
			@Override
			protected boolean isUsed(OAObject obj) {
				Object val = obj.getProperty(pp);
				if (val != null) {
					try {
						double d = OAConv.toDouble(val);
						sum.add(d);
					} catch (Exception e) {
					}
				}
				return false;
			}
		};
		f.find();
		return sum.get();
	}

	/**
	 * Computes the sum of numeric property values for objects reached by
	 * the navigation property path from a Hub and extracts the numeric
	 * value from the terminal property. Values are converted using
	 * {@link OAConv#toDouble}.
	 *
	 * @param hub        the Hub serving as the traversal root; returns 0 if null
	 * @param ppToObject navigation property path to reach objects
	 * @param pp         terminal property name containing numeric values
	 * @return the computed sum
	 */
	public static double sum(Hub hub, String ppToObject, String pp) {
		if (hub == null || OAString.isEmpty(pp)) {
			return 0;
		}
		OADouble sum = new OADouble();

		OAFinder f = new OAFinder(hub, ppToObject) {
			@Override
			protected boolean isUsed(OAObject obj) {
				Object val = obj.getProperty(pp);
				if (val != null) {
					try {
						double d = OAConv.toDouble(val);
						sum.add(d);
					} catch (Exception e) {
					}
				}
				return false;
			}
		};
		f.find();
		return sum.get();
	}

	/**
	 * Evaluates the maximum property value reachable through the supplied
	 * property path beginning at the given root object. The path is split
	 * into navigation and terminal segments.
	 *
	 * @param obj the starting {@link OAObject}; returns 0 if null
	 * @param pp  full property path to the comparable value; returns 0 if empty
	 * @return the maximum value encountered, or null if none found
	 */
	public static Object max(OAObject obj, String pp) {
		if (obj == null || OAString.isEmpty(pp)) {
			return 0;
		}
		String pp1, pp2;
		int x = pp.lastIndexOf('.');
		if (x < 0) {
			pp1 = null;
			pp2 = pp;
		} else {
			pp1 = pp.substring(0, x);
			pp2 = pp.substring(x + 1);
		}
		return max(obj, pp1, pp2);
	}

	/**
	 * Evaluates the maximum property value reachable through the supplied
	 * property path beginning at objects in the Hub. The path is split into
	 * navigation and terminal segments.
	 *
	 * @param hub the Hub used as the traversal root; returns 0 if null
	 * @param pp  full property path to the comparable value; returns 0 if empty
	 * @return the maximum value encountered, or null if none found
	 */
	public static Object max(Hub hub, String pp) {
		if (hub == null || OAString.isEmpty(pp)) {
			return 0;
		}
		String pp1, pp2;
		int x = pp.lastIndexOf('.');
		if (x < 0) {
			pp1 = null;
			pp2 = pp;
		} else {
			pp1 = pp.substring(0, x);
			pp2 = pp.substring(x + 1);
		}
		return max(hub, pp1, pp2);
	}

	/**
	 * Computes the maximum value for the terminal property across all
	 * objects reached by the navigation property path from the root object.
	 * Uses {@link OACompare#compare(Object, Object)} to evaluate ordering.
	 *
	 * @param obj        the starting {@link OAObject}; returns 0 if null
	 * @param ppToObject navigation path to traverse
	 * @param pp         terminal property whose values are compared
	 * @return the maximum value, or null if none found
	 */
	public static Object max(OAObject obj, String ppToObject, String pp) {
		if (obj == null || OAString.isEmpty(pp)) {
			return 0;
		}
		Object[] object = new Object[1];

		OAFinder f = new OAFinder(obj, ppToObject) {
			@Override
			protected boolean isUsed(OAObject obj) {
				Object val = obj.getProperty(pp);
				if (val != null) {
					try {
						if (object[0] == null) {
							object[0] = val;
						} else {
							int x = OACompare.compare(object[0], val);
							if (x < 0) {
								object[0] = val;
							}
						}
					} catch (Exception e) {
					}
				}
				return false;
			}
		};
		f.find();
		return object[0];
	}

	/**
	 * Computes the maximum value for the terminal property across all
	 * objects reached by the navigation property path from a Hub. Uses
	 * {@link OACompare#compare(Object, Object)} to evaluate ordering.
	 *
	 * @param hub        the Hub serving as traversal root; returns 0 if null
	 * @param ppToObject navigation path to traverse
	 * @param pp         terminal property whose values are compared
	 * @return the maximum value, or null if none found
	 */
	public static Object max(Hub hub, String ppToObject, String pp) {
		if (hub == null || OAString.isEmpty(pp)) {
			return 0;
		}
		Object[] object = new Object[1];

		OAFinder f = new OAFinder(hub, ppToObject) {
			@Override
			protected boolean isUsed(OAObject obj) {
				Object val = obj.getProperty(pp);
				if (val != null) {
					try {
						if (object[0] == null) {
							object[0] = val;
						} else {
							int x = OACompare.compare(object[0], val);
							if (x < 0) {
								object[0] = val;
							}
						}
					} catch (Exception e) {
					}
				}
				return false;
			}
		};
		f.find();
		return object[0];
	}

	/**
	 * Evaluates the minimum property value reachable through the supplied
	 * property path beginning at the given root object. The path is split
	 * into navigation and terminal segments.
	 *
	 * @param obj the starting {@link OAObject}; returns 0 if null
	 * @param pp  full property path to the comparable value; returns 0 if empty
	 * @return the minimum value encountered, or null if none found
	 */
	public static Object min(OAObject obj, String pp) {
		if (obj == null || OAString.isEmpty(pp)) {
			return 0;
		}
		String pp1, pp2;
		int x = pp.lastIndexOf('.');
		if (x < 0) {
			pp1 = null;
			pp2 = pp;
		} else {
			pp1 = pp.substring(0, x);
			pp2 = pp.substring(x + 1);
		}
		return min(obj, pp1, pp2);
	}

	/**
	 * Evaluates the minimum property value reachable through the supplied
	 * property path beginning at objects in the Hub. The path is split into
	 * navigation and terminal segments.
	 *
	 * @param hub the Hub used as the traversal root; returns 0 if null
	 * @param pp  full property path to the comparable value; returns 0 if empty
	 * @return the minimum value encountered, or null if none found
	 */
	public static Object min(Hub hub, String pp) {
		if (hub == null || OAString.isEmpty(pp)) {
			return 0;
		}
		String pp1, pp2;
		int x = pp.lastIndexOf('.');
		if (x < 0) {
			pp1 = null;
			pp2 = pp;
		} else {
			pp1 = pp.substring(0, x);
			pp2 = pp.substring(x + 1);
		}
		return min(hub, pp1, pp2);
	}

	/**
	 * Computes the minimum value for the terminal property across all
	 * objects reached by the navigation property path from the root object.
	 * Uses {@link OACompare#compare(Object, Object)} to evaluate ordering.
	 *
	 * @param obj        the starting {@link OAObject}; returns 0 if null
	 * @param ppToObject navigation path to traverse
	 * @param pp         terminal property whose values are compared
	 * @return the minimum value, or null if none found
	 */
	public static Object min(OAObject obj, String ppToObject, String pp) {
		if (obj == null || OAString.isEmpty(pp)) {
			return 0;
		}
		Object[] object = new Object[1];

		OAFinder f = new OAFinder(obj, ppToObject) {
			@Override
			protected boolean isUsed(OAObject obj) {
				Object val = obj.getProperty(pp);
				if (val != null) {
					try {
						if (object[0] == null) {
							object[0] = val;
						} else {
							int x = OACompare.compare(object[0], val);
							if (x > 0) {
								object[0] = val;
							}
						}
					} catch (Exception e) {
					}
				}
				return false;
			}
		};
		f.find();
		return object[0];
	}

	/**
	 * Computes the minimum value for the terminal property across all
	 * objects reached by the navigation property path from a Hub. Uses
	 * {@link OACompare#compare(Object, Object)} to evaluate ordering.
	 *
	 * @param hub        the Hub serving as traversal root; returns 0 if null
	 * @param ppToObject navigation path to traverse
	 * @param pp         terminal property whose values are compared
	 * @return the minimum value, or null if none found
	 */
	public static Object min(Hub hub, String ppToObject, String pp) {
		if (hub == null || OAString.isEmpty(pp)) {
			return 0;
		}
		Object[] object = new Object[1];

		OAFinder f = new OAFinder(hub, ppToObject) {
			@Override
			protected boolean isUsed(OAObject obj) {
				Object val = obj.getProperty(pp);
				if (val != null) {
					try {
						if (object[0] == null) {
							object[0] = val;
						} else {
							int x = OACompare.compare(object[0], val);
							if (x > 0) {
								object[0] = val;
							}
						}
					} catch (Exception e) {
					}
				}
				return false;
			}
		};
		f.find();
		return object[0];
	}

	/**
	 * Evaluates the supplied text template against the given OAObject using
	 * {@link OATemplate}. The template is applied to the object to produce a
	 * formatted string.
	 *
	 * @param obj      the source {@link OAObject}; returns null if null
	 * @param template the template text; returns null if empty
	 * @return the processed template output, or null if no result
	 */
	public static String template(OAObject obj, String template) {
		if (obj == null || OAString.isEmpty(template)) {
			return null;
		}
		OATemplate temp = new OATemplate();
		temp.setTemplate(template);
		String s = temp.process(obj);
		return s;
	}

	/**
	 * Evaluates the supplied text template against all objects in the Hub
	 * using {@link OATemplate}. Produces a formatted string containing the
	 * aggregated template output.
	 *
	 * @param hub      the Hub serving as the template input; returns null if null
	 * @param template the template text; returns null if empty
	 * @return the processed template output, or null if no result
	 */
	public static String template(Hub hub, String template) {
		if (hub == null || OAString.isEmpty(template)) {
			return null;
		}
		OATemplate temp = new OATemplate();
		temp.setTemplate(template);
		String s = temp.process(hub);
		return s;
	}

	/**
	 * Evaluates the supplied text template against all objects in the Hub
	 * using {@link OATemplate}. Produces a formatted string containing the
	 * aggregated template output.
	 *
	 * @param hub      the Hub serving as the template input; returns null if null
	 * @param template the template text; returns null if empty
	 * @return the processed template output, or null if no result
	 */
	public static int length(OAObject obj, String pp) {
		Object val = obj.getProperty(pp);
		if (val == null) {
			return 0;
		}
		if (!(val instanceof String)) {
			return 0;
		}
		return ((String) val).length();
	}

	/**
	 * Computes the cumulative string length for the specified property across
	 * all OAObjects contained within a Hub. Only string values contribute to
	 * the total.
	 *
	 * @param hub the Hub whose objects are evaluated; returns 0 if null
	 * @param pp  the property name containing string values
	 * @return the total combined string length
	 */
	public static int length(Hub hub, String pp) {
		if (hub == null) {
			return 0;
		}
		int len = 0;
		for (Object obj : hub) {
			if (obj instanceof OAObject) {
				len += length((OAObject) obj, pp);
			}
		}
		return len;
	}

	/**
	 * Placeholder for future math and expression functions that operate on
	 * OAObjects using a formula parser.
	 */
	/* TODO:  function with math parser
	public static String func(OAObject obj, String equation) {
	    return null;
	}
	public static double math(OAObject obj, String equation) {
	    return 0.0d;
	}
	*/
}
