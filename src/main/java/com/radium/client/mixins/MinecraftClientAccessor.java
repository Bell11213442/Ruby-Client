package com.radium.client.mixins;
// radium client

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Accessor("session")
    Session getSession();

    @Accessor("session")
    @Mutable
    void radium$setSession(Session session);

    @Accessor("userApiService")
    @Mutable
    void radium$setUserApiService(UserApiService userApiService);

    @Accessor("socialInteractionsManager")
    @Mutable
    void radium$setSocialInteractionsManager(SocialInteractionsManager socialInteractionsManager);

    @Accessor("profileKeys")
    @Mutable
    void radium$setProfileKeys(ProfileKeys profileKeys);

    @Accessor("abuseReportContext")
    @Mutable
    void radium$setAbuseReportContext(AbuseReportContext abuseReportContext);

    @Accessor("gameProfileFuture")
    @Mutable
    void radium$setGameProfileFuture(CompletableFuture<ProfileResult> gameProfileFuture);

    @Accessor("mouse")
    Mouse getMouse();

    @Invoker("doAttack")
    boolean invokeDoAttack();

    @Invoker("doItemUse")
    void invokeDoItemUse();
}

