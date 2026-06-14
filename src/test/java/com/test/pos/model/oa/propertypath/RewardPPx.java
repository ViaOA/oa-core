package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class RewardPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public RewardPPx(String name) {
        this(null, name);
    }

    public RewardPPx(PPxInterface parent, String name) {
        String s = null;
        if (parent != null) {
            s = parent.toString();
        }
        if (s == null) s = "";
        if (name != null && name.length() > 0) {
            if (s.length() > 0 && name.charAt(0) != ':') s += ".";
            s += name;
        }
        pp = s;
    }

    public RewardTypePPx rewardType() {
        RewardTypePPx ppx = new RewardTypePPx(this, Reward.P_RewardType);
        return ppx;
    }

    public String id() {
        return pp + "." + Reward.P_Id;
    }

    public String created() {
        return pp + "." + Reward.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
