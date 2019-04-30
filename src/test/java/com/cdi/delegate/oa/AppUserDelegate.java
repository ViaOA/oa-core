package com.cdi.delegate.oa;

import com.cdi.delegate.ModelDelegate;
import com.cdi.model.oa.*;
import com.viaoa.util.OAString;

public class AppUserDelegate {

    
    public static void updateUser(final AppUser appUser) {
        if (appUser == null) return;
        User user = appUser.getUser();
        if (user != null) return;

        String userId = appUser.getLoginId();
        if (OAString.isEmpty(userId)) return;
        
        for (User userx : ModelDelegate.getUsers()) {
            String id = userx.getLoginId();
            if (id == null) continue;
            if (id.equalsIgnoreCase(userId)) {
                user = userx;
                break;
            }
        }
        appUser.setUser(user);
        appUser.save();
        user.save();
    }

}
