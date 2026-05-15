package com.radium.client.utils.misc;
// radium client

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NbtUtils {
    public static <T extends ISerializable<T>> NbtList listToTag(List<T> list) {
        NbtList nbtList = new NbtList();
        for (T item : list) {
            nbtList.add(item.toTag());
        }
        return nbtList;
    }

    public static <T> List<T> listFromTag(NbtList list, Function<NbtElement, T> mapper) {
        List<T> result = new ArrayList<>();
        for (NbtElement element : list) {
            T item = mapper.apply(element);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }
}
