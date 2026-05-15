package com.radium.client.systems.accounts.types;
// radium client

import com.radium.client.systems.accounts.Account;
import com.radium.client.systems.accounts.AccountType;
import net.minecraft.client.session.Session;
import net.minecraft.util.Uuids;

import java.util.Optional;

public class CrackedAccount extends Account<CrackedAccount> {
    public CrackedAccount(String name) {
        super(AccountType.Cracked, name);
    }

    @Override
    public boolean fetchInfo() {
        cache.username = name;
        cache.uuid = Uuids.getOfflinePlayerUuid(name).toString();
        return true;
    }

    @Override
    public boolean login() {
        setSession(new Session(name, Uuids.getOfflinePlayerUuid(name), "", Optional.empty(), Optional.empty(), Session.AccountType.MOJANG));
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CrackedAccount)) return false;
        return ((CrackedAccount) o).getUsername().equals(this.getUsername());
    }
}

