package com.radium.client.systems.accounts;
// radium client

import com.radium.client.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;

public class AccountCache implements ISerializable<AccountCache> {
    public String username = "";
    public String uuid = "";

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("username", username);
        tag.putString("uuid", uuid);
        return tag;
    }

    @Override
    public AccountCache fromTag(NbtCompound tag) {
        username = tag.getString("username");
        uuid = tag.getString("uuid");
        return this;
    }
}

