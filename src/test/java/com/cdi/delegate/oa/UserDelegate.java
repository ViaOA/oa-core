package com.cdi.delegate.oa;

import com.cdi.delegate.ModelDelegate;
import com.cdi.model.oa.AppUser;
import com.cdi.model.oa.User;
import com.viaoa.util.OAString;

public class UserDelegate {

    
    public static void updateAppUser(final User user) {
        if (user == null) return;
        AppUser appUser = user.getAppUser();
        if (user != null) return;

        String userId = user.getLoginId();
        if (OAString.isEmpty(userId)) return;
        
        for (AppUser appUserx : ModelDelegate.getAppUsers()) {
            String id = appUserx.getLoginId();
            if (id == null) continue;
            if (id.equalsIgnoreCase(userId)) {
                appUser = appUserx;
                break;
            }
        }
        if (appUser == null) {
            appUser = new AppUser();
            appUser.setFirstName(user.getFirstName());
            appUser.setLastName(user.getLastName());
            appUser.setLoginId(user.getLoginId());
            
            String pw = user.getPassword();
            pw = OAString.getSHAHash(pw);
            
            appUser.setPassword(pw);
            appUser.setAdmin(user.getAdminAccess());
            appUser.setInactiveDate(user.getInactiveDate());
            appUser.setUser(user);
        }
        
        user.setAppUser(appUser);
        appUser.save();
        user.save();
    }

}
