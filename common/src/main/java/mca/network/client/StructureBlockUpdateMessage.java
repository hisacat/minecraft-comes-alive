package mca.network.client;

import mca.block.StructureBlockEntity;
import mca.network.S2CNbtDataMessage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;

public class StructureBlockUpdateMessage extends S2CNbtDataMessage {
    private final int x;
    private final int y;
    private final int z;
    private final StructureBlockEntity.Action action;

    public StructureBlockUpdateMessage(BlockPos pos, StructureBlockEntity.Action action, NbtCompound data) {
        super(data);
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.action = action;
    }

    @Override
    public void receive(PlayerEntity e) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity blockEntity = e.world.getBlockEntity(pos);
        if (blockEntity instanceof StructureBlockEntity structureBlockEntity) {
            structureBlockEntity.readNbt(getData());

            if (structureBlockEntity.hasStructureName()) {
                String string = structureBlockEntity.getStructureName();
                if (action == StructureBlockEntity.Action.SAVE_AREA) {
                    if (structureBlockEntity.saveStructure()) {
                        e.sendMessage(new TranslatableText("structure_block.save_success", string), false);
                    } else {
                        e.sendMessage(new TranslatableText("structure_block.save_failure", string), false);
                    }
                } else if (action == StructureBlockEntity.Action.LOAD_AREA) {
                    if (!structureBlockEntity.isStructureAvailable()) {
                        e.sendMessage(new TranslatableText("structure_block.load_not_found", string), false);
                    } else if (structureBlockEntity.loadStructure((ServerWorld)e.getWorld())) {
                        e.sendMessage(new TranslatableText("structure_block.load_success", string), false);
                    } else {
                        e.sendMessage(new TranslatableText("structure_block.load_prepare", string), false);
                    }
                } else if (action == StructureBlockEntity.Action.SCAN_AREA) {
                    if (structureBlockEntity.detectStructureSize()) {
                        e.sendMessage(new TranslatableText("structure_block.size_success", string), false);
                    } else {
                        e.sendMessage(new TranslatableText("structure_block.size_failure"), false);
                    }
                }
            } else {
                e.sendMessage(new TranslatableText("structure_block.invalid_structure_name", structureBlockEntity.getStructureName()), false);
            }
            structureBlockEntity.markDirty();
        }
    }
}
