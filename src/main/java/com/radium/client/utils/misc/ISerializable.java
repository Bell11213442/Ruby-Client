package com.radium.client.utils.misc;
// radium client

import net.minecraft.nbt.NbtCompound;

public interface ISerializable<T> {
    NbtCompound toTag();

    T fromTag(NbtCompound tag);
}

