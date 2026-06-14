package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class RewardPP {
    private static RewardTypePPx rewardType;
     

    public static RewardTypePPx rewardType() {
        if (rewardType == null) rewardType = new RewardTypePPx(Reward.P_RewardType);
        return rewardType;
    }

    public static String id() {
        String s = Reward.P_Id;
        return s;
    }

    public static String created() {
        String s = Reward.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
