/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.func;

import com.viaoa.hub.*;
import com.viaoa.object.*;
import com.viaoa.util.*;


/**
 * OA functions that work from OAObject, Hub and use property paths.
 * @author vvia
 *
 */
public class OAFunction {

    public static int count(OAObject obj, String pp) {
        if (obj == null || OAString.isEmpty(pp)) return 0;
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
    public static int count(Hub hub, String pp) {
        if (hub == null || OAString.isEmpty(pp)) return 0;
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

    public static double sum(OAObject obj, String pp) {
        if (obj == null || OAString.isEmpty(pp)) return 0;
        String pp1, pp2;
        int x = pp.lastIndexOf('.');
        if (x < 0) {
            pp1 = null;
            pp2 = pp;
        }
        else {
            pp1 = pp.substring(0, x);
            pp2 = pp.substring(x+1);
        }
        return sum(obj, pp1, pp2);
    }
    public static double sum(Hub hub, String pp) {
        if (hub == null || OAString.isEmpty(pp)) return 0;
        String pp1, pp2;
        int x = pp.lastIndexOf('.');
        if (x < 0) {
            pp1 = null;
            pp2 = pp;
        }
        else {
            pp1 = pp.substring(0, x);
            pp2 = pp.substring(x+1);
        }
        return sum(hub, pp1, pp2);
    }
    public static double sum(OAObject obj, String ppToObject, String pp) {
        if (obj == null || OAString.isEmpty(pp)) return 0;
        OADouble sum = new OADouble();
         
        OAFinder f = new OAFinder(obj, ppToObject) {
            @Override
            protected boolean isUsed(OAObject obj) {
                Object val = obj.getProperty(pp);
                if (val != null) {
                    try {
                        double d = OAConv.toDouble(val);
                        sum.add(d);
                    }
                    catch (Exception e) {}
                }
                return false;
            }
        };
        f.find();
        return sum.get();
    }
    public static double sum(Hub hub, String ppToObject, String pp) {
        if (hub == null || OAString.isEmpty(pp)) return 0;
        OADouble sum = new OADouble();
         
        OAFinder f = new OAFinder(hub, ppToObject) {
            @Override
            protected boolean isUsed(OAObject obj) {
                Object val = obj.getProperty(pp);
                if (val != null) {
                    try {
                        double d = OAConv.toDouble(val);
                        sum.add(d);
                    }
                    catch (Exception e) {}
                }
                return false;
            }
        };
        f.find();
        return sum.get();
    }


    public static Object max(OAObject obj, String pp) {
        if (obj == null || OAString.isEmpty(pp)) return 0;
        String pp1, pp2;
        int x = pp.lastIndexOf('.');
        if (x < 0) {
            pp1 = null;
            pp2 = pp;
        }
        else {
            pp1 = pp.substring(0, x);
            pp2 = pp.substring(x+1);
        }
        return max(obj, pp1, pp2);
    }
    public static Object max(Hub hub, String pp) {
        if (hub == null || OAString.isEmpty(pp)) return 0;
        String pp1, pp2;
        int x = pp.lastIndexOf('.');
        if (x < 0) {
            pp1 = null;
            pp2 = pp;
        }
        else {
            pp1 = pp.substring(0, x);
            pp2 = pp.substring(x+1);
        }
        return max(hub, pp1, pp2);
    }
    public static Object max(OAObject obj, String ppToObject, String pp) {
        if (obj == null || OAString.isEmpty(pp)) return 0;
        Object[] object = new Object[1];
         
        OAFinder f = new OAFinder(obj, ppToObject) {
            @Override
            protected boolean isUsed(OAObject obj) {
                Object val = obj.getProperty(pp);
                if (val != null) {
                    try {
                        if (object[0] == null) object[0] = val;
                        else {
                            int x = OACompare.compare(object[0], val);
                            if (x < 0) object[0] = val;
                        }
                    }
                    catch (Exception e) {}
                }
                return false;
            }
        };
        f.find();
        return object[0];
    }
    public static Object max(Hub hub, String ppToObject, String pp) {
        if (hub == null || OAString.isEmpty(pp)) return 0;
        Object[] object = new Object[1];
         
        OAFinder f = new OAFinder(hub, ppToObject) {
            @Override
            protected boolean isUsed(OAObject obj) {
                Object val = obj.getProperty(pp);
                if (val != null) {
                    try {
                        if (object[0] == null) object[0] = val;
                        else {
                            int x = OACompare.compare(object[0], val);
                            if (x < 0) object[0] = val;
                        }
                    }
                    catch (Exception e) {}
                }
                return false;
            }
        };
        f.find();
        return object[0];
    }


    public static Object min(OAObject obj, String pp) {
        if (obj == null || OAString.isEmpty(pp)) return 0;
        String pp1, pp2;
        int x = pp.lastIndexOf('.');
        if (x < 0) {
            pp1 = null;
            pp2 = pp;
        }
        else {
            pp1 = pp.substring(0, x);
            pp2 = pp.substring(x+1);
        }
        return min(obj, pp1, pp2);
    }
    public static Object min(Hub hub, String pp) {
        if (hub == null || OAString.isEmpty(pp)) return 0;
        String pp1, pp2;
        int x = pp.lastIndexOf('.');
        if (x < 0) {
            pp1 = null;
            pp2 = pp;
        }
        else {
            pp1 = pp.substring(0, x);
            pp2 = pp.substring(x+1);
        }
        return min(hub, pp1, pp2);
    }
    public static Object min(OAObject obj, String ppToObject, String pp) {
        if (obj == null || OAString.isEmpty(pp)) return 0;
        Object[] object = new Object[1];
         
        OAFinder f = new OAFinder(obj, ppToObject) {
            @Override
            protected boolean isUsed(OAObject obj) {
                Object val = obj.getProperty(pp);
                if (val != null) {
                    try {
                        if (object[0] == null) object[0] = val;
                        else {
                            int x = OACompare.compare(object[0], val);
                            if (x > 0) object[0] = val;
                        }
                    }
                    catch (Exception e) {}
                }
                return false;
            }
        };
        f.find();
        return object[0];
    }
    public static Object min(Hub hub, String ppToObject, String pp) {
        if (hub == null || OAString.isEmpty(pp)) return 0;
        Object[] object = new Object[1];
         
        OAFinder f = new OAFinder(hub, ppToObject) {
            @Override
            protected boolean isUsed(OAObject obj) {
                Object val = obj.getProperty(pp);
                if (val != null) {
                    try {
                        if (object[0] == null) object[0] = val;
                        else {
                            int x = OACompare.compare(object[0], val);
                            if (x > 0) object[0] = val;
                        }
                    }
                    catch (Exception e) {}
                }
                return false;
            }
        };
        f.find();
        return object[0];
    }
    
    
    
    
    
    public static String template(OAObject obj, String template) {
        if (obj == null || OAString.isEmpty(template)) return null;
        OATemplate temp = new OATemplate();
        temp.setTemplate(template);
        String s = temp.process(obj);
        return s;
    }
    public static String template(Hub hub, String template) {
        if (hub == null || OAString.isEmpty(template)) return null;
        OATemplate temp = new OATemplate();
        temp.setTemplate(template);
        String s = temp.process(hub);
        return s;
    }
    

/** TODO:  function with math parser  qqqqqqqqqqqqqqqqqqqqqqqqqq     
    public static String func(OAObject obj, String equation) {
        return null;
    }
    public static double math(OAObject obj, String equation) {
        return 0.0d;
    }
*/    
    
}

