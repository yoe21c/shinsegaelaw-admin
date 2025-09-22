package com.shinsegaelaw.admin.controller.page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class DefaultAdminPage {

    @Autowired
    private Environment environment;

    protected boolean isDevOrLocal() {
        for (String activeProfile : environment.getActiveProfiles()) {
            if(activeProfile.equals("dev") || activeProfile.equals("local")) return true;
        }
        return false;
    }
}
