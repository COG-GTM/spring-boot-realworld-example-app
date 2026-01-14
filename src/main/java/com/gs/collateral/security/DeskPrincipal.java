package com.gs.collateral.security;

import java.security.Principal;

public final class DeskPrincipal implements Principal {

    private final String desk;

    public DeskPrincipal(String desk) {
        this.desk = desk;
    }

    @Override
    public String getName() {
        return desk;
    }

    @Override
    public String toString() {
        return "desk:" + desk;
    }
}
