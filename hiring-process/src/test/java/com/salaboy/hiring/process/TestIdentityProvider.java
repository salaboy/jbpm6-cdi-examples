package com.salaboy.hiring.process;

import java.util.Collections;
import java.util.List;

import org.droolsjbpm.services.api.IdentityProvider;

public class TestIdentityProvider implements IdentityProvider {

    public String getName() {
        return "testUser";
    }

    public List<String> getRoles() {
        return Collections.EMPTY_LIST;
    }

}
