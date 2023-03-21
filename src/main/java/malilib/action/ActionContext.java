package malilib.action;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import malilib.overlay.message.MessageOutput;
import malilib.util.game.wrap.GameUtils;

public class ActionContext
{
    public static final ActionContext COMMON = new ActionContext();

    @Nullable public final MessageOutput messageOutput;

    public ActionContext()
    {
        this(null);
    }

    public ActionContext(@Nullable MessageOutput messageOutput)
    {
        this.messageOutput = messageOutput;
    }

    public MessageOutput getMessageOutputOrDefault(MessageOutput defaultOutput)
    {
        return this.messageOutput != null ? this.messageOutput : defaultOutput;
    }

    public Minecraft getClient()
    {
        return GameUtils.getClient();
    }

    @Nullable
    public ClientLevel getWorld()
    {
        return GameUtils.getClientWorld();
    }

    @Nullable
    public LocalPlayer getPlayer()
    {
        return GameUtils.getClientPlayer();
    }

    @Nullable
    public Entity getCameraEntity()
    {
        return GameUtils.getCameraEntity();
    }
}
