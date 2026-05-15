package com.radium.client.systems.accounts;
// radium client

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.radium.client.mixins.MinecraftClientAccessor;
import com.radium.client.utils.misc.ISerializable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Util;

import java.util.concurrent.CompletableFuture;

public abstract class Account<T extends Account<?>> implements ISerializable<T> {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    protected final AccountCache cache;
    protected AccountType type;
    protected String name;

    protected Account(AccountType type, String name) {
        this.type = type;
        this.name = name;
        this.cache = new AccountCache();
    }

    public static void setSession(Session session) {
        if (mc == null) {
            System.err.println("[Account] MinecraftClient is null, cannot set session");
            return;
        }
        
        if (session == null) {
            System.err.println("[Account] Session is null, cannot set session");
            return;
        }
        
        try {
            MinecraftClientAccessor mca = (MinecraftClientAccessor) mc;
            mca.radium$setSession(session);

            YggdrasilAuthenticationService yggdrasilAuthenticationService = new YggdrasilAuthenticationService(mc.getNetworkProxy());

            UserApiService apiService = yggdrasilAuthenticationService.createUserApiService(session.getAccessToken());
            mca.radium$setUserApiService(apiService);
            mca.radium$setSocialInteractionsManager(new SocialInteractionsManager(mc, apiService));
            
            if (mc.runDirectory != null) {
                mca.radium$setProfileKeys(ProfileKeys.create(apiService, session, mc.runDirectory.toPath()));
            }
            
            mca.radium$setAbuseReportContext(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
            mca.radium$setGameProfileFuture(CompletableFuture.supplyAsync(() -> mc.getSessionService().fetchProfile(mc.getSession().getUuidOrNull(), true), Util.getIoWorkerExecutor()));
        } catch (Exception e) {
            System.err.println("[Account] Failed to set session: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public abstract boolean fetchInfo();

    public abstract boolean login();

    public String getUsername() {
        if (cache.username.isEmpty()) return name;
        return cache.username;
    }

    public AccountType getType() {
        return type;
    }

    public AccountCache getCache() {
        return cache;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("type", type.name());
        tag.putString("name", name);
        tag.put("cache", cache.toTag());
        return tag;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fromTag(NbtCompound tag) {
        name = tag.getString("name");
        cache.fromTag(tag.getCompound("cache"));
        return (T) this;
    }
}

