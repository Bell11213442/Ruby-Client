package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.misc.NameProtect;
import com.radium.client.utils.FriendsManager;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin({TextVisitFactory.class})
public class TextVisitFactoryMixin {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", ordinal = 0), method = {"visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z"}, index = 0)
    private static String adjustText(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        if (RadiumClient.moduleManager == null) return s;
        final NameProtect nameprotect = RadiumClient.moduleManager.getModule(NameProtect.class);
        if (nameprotect == null || !nameprotect.isEnabled()) {
            return s;
        }

        String result = s;
        String username = RadiumClient.mc.getSession().getUsername();

        if (s.contains(username)) {
            result = result.replace(username, nameprotect.getFakeName());
        }

        if (nameprotect.shouldProtectFriends()) {
            List<String> friends = FriendsManager.getFriends();
            if (friends != null && !friends.isEmpty()) {
                for (String friendName : friends) {
                    if (friendName != null && !friendName.trim().isEmpty() && result.contains(friendName)) {
                        String fakeName = nameprotect.getFakeNameFor(friendName);
                        result = result.replace(friendName, fakeName);
                    }
                }
            }
        }

        return result;
    }
}
