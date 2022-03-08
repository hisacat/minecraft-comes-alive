package mca.structure;

import mca.mixin.MixinStructure;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedList;
import java.util.List;

public class StructureTask {
    private final Structure structure;
    private final BlockPos start;
    private final ServerWorld world;
    private final StructurePlacementData placementData;
    private List<Task> tasks;

    /**
     * Load a structure file from the disk
     *
     * @param world The world itself
     * @param id    The id itself
     * @param start
     * @param data
     */
    public StructureTask(ServerWorld world, Identifier id, BlockPos start, StructurePlacementData data) {
        this.structure = world.getStructureManager().getStructure(id).orElse(new Structure());
        this.world = world;
        this.start = start;
        this.tasks = new LinkedList<>();
        this.placementData = data;

        List<Structure.StructureBlockInfo> list = placementData.getRandomBlockInfos(((MixinStructure)structure).getBlockInfoLists(), start).getAll();
        List<Structure.StructureBlockInfo> blockInfos = Structure.process(world, start, start, placementData, list);

        for (Structure.StructureBlockInfo info : blockInfos) {
            BlockState blockState = info.state.mirror(placementData.getMirror()).rotate(placementData.getRotation());
            tasks.add(new Task(info.pos, blockState, info.nbt));
        }
    }

    public Structure getStructure() {
        return structure;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public class Task {
        ItemStack resources;
        BlockPos position;
        BlockState state;
        NbtCompound nbt;

        public Task(BlockPos pos, BlockState state, NbtCompound nbt) {
            this.resources = new ItemStack(Items.STONE);
            this.position = pos;
            this.state = state;
            this.nbt = nbt;
        }

        public void place() {
            world.setBlockState(position, state);
        }
    }
}
