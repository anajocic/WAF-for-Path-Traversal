package com.example.websecurity.waf;

import java.util.List;
import java.util.regex.Pattern;

public class WafRules {

    public static final Pattern white_list = Pattern.compile("^[a-zA-Z0-9-]{1,64}$");

    public static final List<Pattern> black_list = List.of(
            Pattern.compile("\\.\\."),
            Pattern.compile("%2e%2e", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%252e", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%c0%ae", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%ef%bc%8f", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\x2e\\x2e"),
            Pattern.compile("/"),
            Pattern.compile("\\\\")
    );

    public static boolean isValid(String kid) {
        if(kid == null || kid.isBlank())
            return false;
        if(!white_list.matcher(kid).matches()){
            return false;
        }
        for(Pattern p : black_list){
            if (p.matcher(kid).matches()){
                return false;
            }
        }
        return true;
    }
}
