package mca.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class NonePlayerItemUsageContext extends ItemUsageContext {
    public NonePlayerItemUsageContext(World world, Hand hand, ItemStack stack, BlockHitResult hit) {
        super(world, null, hand, stack, hit);
    }
}
