package com.radium.client.systems.accounts;
// radium client

import com.radium.client.systems.accounts.types.CrackedAccount;
import com.radium.client.systems.accounts.types.MicrosoftAccount;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AccountManager implements Iterable<Account<?>> {
    private final File file;
    private final List<Account<?>> accounts = new ArrayList<>();

    public AccountManager(File configDir) {
        if (configDir == null) {
            throw new IllegalArgumentException("Config directory cannot be null");
        }
        this.file = new File(configDir, "accounts.nbt");
        // Ensure parent directory exists
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        load();
    }

    public void add(Account<?> account) {
        accounts.add(account);
        save();
    }

    public boolean exists(Account<?> account) {
        return accounts.contains(account);
    }

    public void remove(Account<?> account) {
        if (accounts.remove(account)) {
            save();
        }
    }

    public int size() {
        return accounts.size();
    }

    @Override
    public @NotNull Iterator<Account<?>> iterator() {
        return accounts.iterator();
    }

    public void save() {
        try {
            NbtCompound tag = new NbtCompound();
            NbtList list = new NbtList();

            for (Account<?> account : accounts) {
                if (account != null) {
                    try {
                        list.add(account.toTag());
                    } catch (Exception e) {
                        System.err.println("Failed to serialize account: " + e.getMessage());
                    }
                }
            }

            tag.put("accounts", list);

            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                    System.err.println("Failed to create directory: " + parentFile.getAbsolutePath());
                    return;
                }
            }

            if (parentFile != null) {
                NbtIo.writeCompressed(tag, file.toPath());
            }
        } catch (IOException e) {
            System.err.println("Failed to save accounts: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error while saving accounts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void load() {
        if (!file.exists()) {
            return;
        }

        if (!file.canRead()) {
            System.err.println("Cannot read accounts file: " + file.getAbsolutePath());
            return;
        }

        try {
            NbtCompound tag = NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
            if (tag == null) {
                System.err.println("Accounts file is empty or corrupted");
                return;
            }

            NbtList list = tag.getList("accounts", NbtElement.COMPOUND_TYPE);
            if (list == null) {
                System.err.println("No accounts list found in file");
                return;
            }

            accounts.clear();

            for (int i = 0; i < list.size(); i++) {
                NbtCompound accountTag = list.getCompound(i);
                if (accountTag == null) continue;

                if (!accountTag.contains("type")) {
                    System.err.println("Account at index " + i + " missing type field");
                    continue;
                }

                try {
                    String typeString = accountTag.getString("type");
                    if (typeString == null || typeString.isEmpty()) {
                        continue;
                    }

                    AccountType type = AccountType.valueOf(typeString);

                    Account<?> account = switch (type) {
                        case Cracked -> new CrackedAccount(null).fromTag(accountTag);
                        case Microsoft -> new MicrosoftAccount(null).fromTag(accountTag);
                    };

                    if (account != null) {
                        accounts.add(account);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown account type in file: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Failed to load account at index " + i + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("Loaded " + accounts.size() + " accounts");
        } catch (IOException e) {
            System.err.println("Failed to load accounts: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error while loading accounts: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
