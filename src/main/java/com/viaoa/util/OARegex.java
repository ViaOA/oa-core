package com.viaoa.util;

import java.util.Arrays;
import java.util.List;

public class OARegex {

    
    // http://daringfireball.net/2010/07/improved_regex_for_matching_urls
    // original
    // (?i)\b((?:[a-z][\w-]+:(?:/{1,3}|[a-z0-9%])|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?������]))
    public final static String Regex_URL = "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?������]))";

    // http://ntt.cc/2008/05/10/over-10-useful-javascript-regular-expression-functions-to-improve-your-web-applications-efficiency.html
    public final static String Regex_Digits = "^\\s*\\d+\\s*$";
    public final static String Regex_Integer = "^\\s*(\\+|-)?\\d+\\s*$";
    public final static String Regex_Decimal = "^\\s*(\\+|-)?((\\d+(\\.\\d+)?)|(\\.\\d+))\\s*$";
    public final static String Regex_Currency = "^\\s*(\\+|-)?((\\d+(\\.\\d\\d)?)|(\\.\\d\\d))\\s*$";

    public final static String Regex_SingleDigit = "^([0-9])$";
    public final static String Regex_DoubleDigit = "^([1-9][0-9])$";

    // http://www.zparacha.com/validate-email-address-using-javascript-regular-expression/
    public final static String Regex_Email = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$";
    //was: public final static String Regex_Email = "^\\s*[\\w\\-\\+_]+(\\.[\\w\\-\\+_]+)*\\@[\\w\\-\\+_]+\\.[\\w\\-\\+_]+(\\.[\\w\\-\\+_]+)*\\s*$";

    public final static String Regex_CreditCard = "^\\s*\\d+\\s*$"; // qqq currently only checks if digits

    // http://stackoverflow.com/questions/123559/a-comprehensive-regex-for-phone-number-validation
    public final static String Regex_USPhoneNumber = "^(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?$";

    public final static String Regex_DateMMDDYYYY = "^\\d{1,2}\\/\\d{1,2}\\/\\d{4}$";
    public final static String Regex_DateMMDDYY = "^\\d{1,2}\\/\\d{1,2}\\/\\d{2}$";
    public final static String Regex_Time12hr = "^(0?[1-9]|1[012]):[0-5][0-9]$";
    public final static String Regex_Time24hr = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$";
    
    
    
/*

> comma separated with 0+ whitespace chars
   List<String> al = Arrays.asList(s.split(",\\s*"));     
    
*/    
    
}
