package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class RewardTypePP {
    private static RewardPPx rewards;
     

    public static RewardPPx rewards() {
        if (rewards == null) rewards = new RewardPPx(RewardType.P_Rewards);
        return rewards;
    }

    public static String id() {
        String s = RewardType.P_Id;
        return s;
    }

    public static String created() {
        String s = RewardType.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
