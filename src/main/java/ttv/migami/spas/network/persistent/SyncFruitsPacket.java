package ttv.migami.spas.network.persistent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import ttv.migami.spas.common.FruitDataHandler;
import ttv.migami.spas.effect.FruitEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncFruitsPacket {
    private final int currentEffectId;
    private final List<Integer> previousEffectIds;

    public SyncFruitsPacket(int currentEffectId, List<Integer> previousEffectIds) {
        this.currentEffectId = currentEffectId;
        this.previousEffectIds = previousEffectIds;
    }

    public SyncFruitsPacket(FriendlyByteBuf buf) {
        this.currentEffectId = buf.readInt();
        int size = buf.readInt();
        this.previousEffectIds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            previousEffectIds.add(buf.readInt());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(currentEffectId);
        buf.writeInt(previousEffectIds.size());
        for (int id : previousEffectIds) {
            buf.writeInt(id);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(this::handleClient);
        context.setPacketHandled(true);
    }

	@OnlyIn(Dist.CLIENT) // if you port to a later version onlyin stops working - you will need to change this impl
	private void handleClient() {
		Player player = net.minecraft.client.Minecraft.getInstance().player;
		if (player != null) {
			CompoundTag persistentData = player.getPersistentData();
			persistentData.putInt(FruitDataHandler.CURRENT_EFFECT_KEY, currentEffectId);

			ListTag listTag = new ListTag();
			for (int id : previousEffectIds) {
				listTag.add(IntTag.valueOf(id));
			}
			persistentData.put(FruitDataHandler.PREVIOUS_EFFECTS_KEY, listTag);

			MobEffect currentEffect = MobEffect.byId(currentEffectId);
			if (currentEffect instanceof FruitEffect && !player.hasEffect(currentEffect) &&
					player.getEffect(currentEffect) != null && player.getEffect(currentEffect).getDuration() == -1) {
				player.addEffect(new MobEffectInstance(currentEffect, -1, 0, false, false));
			}
		}
	}
}