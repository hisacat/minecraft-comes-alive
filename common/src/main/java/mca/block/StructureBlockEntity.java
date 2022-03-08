package mca.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StructureBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public class StructureBlockEntity extends BlockEntity {
    private Identifier structureName;
    private BlockPos offset = new BlockPos(0, 1, 0);
    private Vec3i size = Vec3i.ZERO;
    private BlockMirror mirror = BlockMirror.NONE;
    private BlockRotation rotation = BlockRotation.NONE;
    private StructureBlockMode mode;
    private boolean powered;
    private boolean showAir;
    private boolean showBoundingBox = true;

    public StructureBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntityTypesMCA.STRUCTURE_BLOCK, blockPos, blockState);
        this.mode = blockState.get(StructureBlock.MODE);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("name", this.getStructureName());
        nbt.putInt("posX", this.offset.getX());
        nbt.putInt("posY", this.offset.getY());
        nbt.putInt("posZ", this.offset.getZ());
        nbt.putInt("sizeX", this.size.getX());
        nbt.putInt("sizeY", this.size.getY());
        nbt.putInt("sizeZ", this.size.getZ());
        nbt.putString("rotation", this.rotation.toString());
        nbt.putString("mirror", this.mirror.toString());
        nbt.putString("mode", this.mode.toString());
        nbt.putBoolean("powered", this.powered);
        nbt.putBoolean("showair", this.showAir);
        nbt.putBoolean("showboundingbox", this.showBoundingBox);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.setStructureName(nbt.getString("name"));
        int i = MathHelper.clamp(nbt.getInt("posX"), -48, 48);
        int j = MathHelper.clamp(nbt.getInt("posY"), -48, 48);
        int k = MathHelper.clamp(nbt.getInt("posZ"), -48, 48);
        this.offset = new BlockPos(i, j, k);
        int l = MathHelper.clamp(nbt.getInt("sizeX"), 0, 48);
        int m = MathHelper.clamp(nbt.getInt("sizeY"), 0, 48);
        int n = MathHelper.clamp(nbt.getInt("sizeZ"), 0, 48);
        this.size = new Vec3i(l, m, n);
        try {
            this.rotation = BlockRotation.valueOf(nbt.getString("rotation"));
        } catch (IllegalArgumentException illegalArgumentException) {
            this.rotation = BlockRotation.NONE;
        }
        try {
            this.mirror = BlockMirror.valueOf(nbt.getString("mirror"));
        } catch (IllegalArgumentException illegalArgumentException) {
            this.mirror = BlockMirror.NONE;
        }
        try {
            this.mode = StructureBlockMode.valueOf(nbt.getString("mode"));
        } catch (IllegalArgumentException illegalArgumentException) {
            this.mode = StructureBlockMode.DATA;
        }
        this.powered = nbt.getBoolean("powered");
        this.showAir = nbt.getBoolean("showair");
        this.showBoundingBox = nbt.getBoolean("showboundingbox");
        this.updateBlockMode();
    }

    private void updateBlockMode() {
        if (this.world == null) {
            return;
        }
        BlockPos blockPos = this.getPos();
        BlockState blockState = this.world.getBlockState(blockPos);
        if (blockState.isOf(Blocks.STRUCTURE_BLOCK)) {
            this.world.setBlockState(blockPos, blockState.with(StructureBlock.MODE, this.mode), Block.NOTIFY_LISTENERS);
        }
    }

    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    public String getStructureName() {
        return this.structureName == null ? "" : this.structureName.toString();
    }

    public String getStructurePath() {
        return this.structureName == null ? "" : this.structureName.getPath();
    }

    public boolean hasStructureName() {
        return this.structureName != null;
    }

    public void setStructureName(@Nullable String name) {
        this.setStructureName(StringHelper.isEmpty(name) ? null : Identifier.tryParse(name));
    }

    public void setStructureName(@Nullable Identifier structureName) {
        this.structureName = structureName;
    }

    public BlockPos getOffset() {
        return this.offset;
    }

    public void setOffset(BlockPos offset) {
        this.offset = offset;
    }

    public Vec3i getSize() {
        return this.size;
    }

    public void setSize(Vec3i size) {
        this.size = size;
    }

    public BlockMirror getMirror() {
        return this.mirror;
    }

    public void setMirror(BlockMirror mirror) {
        this.mirror = mirror;
    }

    public BlockRotation getRotation() {
        return this.rotation;
    }

    public void setRotation(BlockRotation rotation) {
        this.rotation = rotation;
    }

    public StructureBlockMode getMode() {
        return this.mode;
    }

    public void setMode(StructureBlockMode mode) {
        this.mode = mode;
        BlockState blockState = this.world.getBlockState(this.getPos());
        if (blockState.isOf(Blocks.STRUCTURE_BLOCK)) {
            this.world.setBlockState(this.getPos(), blockState.with(StructureBlock.MODE, mode), Block.NOTIFY_LISTENERS);
        }
    }

    public boolean detectStructureSize() {
        if (this.mode != StructureBlockMode.SAVE) {
            return false;
        }
        BlockPos blockPos = this.getPos();
        BlockPos blockPos2 = new BlockPos(blockPos.getX() - 80, this.world.getBottomY(), blockPos.getZ() - 80);
        BlockPos blockPos3 = new BlockPos(blockPos.getX() + 80, this.world.getTopY() - 1, blockPos.getZ() + 80);
        Stream<BlockPos> stream = this.streamCornerPos(blockPos2, blockPos3);
        return StructureBlockEntity.getStructureBox(blockPos, stream).filter(box -> {
            int i = box.getMaxX() - box.getMinX();
            int j = box.getMaxY() - box.getMinY();
            int k = box.getMaxZ() - box.getMinZ();
            if (i > 1 && j > 1 && k > 1) {
                this.offset = new BlockPos(box.getMinX() - blockPos.getX() + 1, box.getMinY() - blockPos.getY() + 1, box.getMinZ() - blockPos.getZ() + 1);
                this.size = new Vec3i(i - 1, j - 1, k - 1);
                this.markDirty();
                BlockState blockState = this.world.getBlockState(blockPos);
                this.world.updateListeners(blockPos, blockState, blockState, Block.NOTIFY_ALL);
                return true;
            }
            return false;
        }).isPresent();
    }

    /**
     * Streams positions of {@link StructureBlockMode#CORNER} mode structure blocks with matching names.
     */
    private Stream<BlockPos> streamCornerPos(BlockPos start, BlockPos end) {
        return BlockPos.stream(start, end).filter(pos -> this.world.getBlockState(pos).isOf(Blocks.STRUCTURE_BLOCK)).map(this.world::getBlockEntity).filter(blockEntity -> blockEntity instanceof StructureBlockEntity).map(blockEntity -> (StructureBlockEntity)blockEntity).filter(blockEntity -> blockEntity.mode == StructureBlockMode.CORNER && Objects.equals(this.structureName, blockEntity.structureName)).map(BlockEntity::getPos);
    }

    private static Optional<BlockBox> getStructureBox(BlockPos pos, Stream<BlockPos> corners) {
        Iterator<BlockPos> iterator = corners.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        }
        BlockPos blockPos = iterator.next();
        BlockBox blockBox = new BlockBox(blockPos);
        if (iterator.hasNext()) {
            iterator.forEachRemaining(blockBox::encompass);
        } else {
            blockBox.encompass(pos);
        }
        return Optional.of(blockBox);
    }

    public boolean saveStructure() {
        return this.saveStructure(true);
    }

    public boolean saveStructure(boolean bl) {
        Structure structure;
        if (this.mode != StructureBlockMode.SAVE || this.world.isClient || this.structureName == null) {
            return false;
        }
        BlockPos blockPos = this.getPos().add(this.offset);
        ServerWorld serverWorld = (ServerWorld)this.world;
        StructureManager structureManager = serverWorld.getStructureManager();
        try {
            structure = structureManager.getStructureOrBlank(this.structureName);
        } catch (InvalidIdentifierException invalidIdentifierException) {
            return false;
        }
        structure.saveFromWorld(this.world, blockPos, this.size, false, Blocks.STRUCTURE_VOID);
        if (bl) {
            try {
                return structureManager.saveStructure(this.structureName);
            } catch (InvalidIdentifierException invalidIdentifierException) {
                return false;
            }
        }
        return true;
    }

    public boolean loadStructure(ServerWorld world) {
        return this.loadStructure(world, true);
    }

    private static Random createRandom(long seed) {
        if (seed == 0L) {
            return new Random(Util.getMeasuringTimeMs());
        }
        return new Random(seed);
    }

    public boolean loadStructure(ServerWorld world, boolean bl) {
        Optional<Structure> optional;
        if (this.mode != StructureBlockMode.LOAD || this.structureName == null) {
            return false;
        }
        StructureManager structureManager = world.getStructureManager();
        try {
            optional = structureManager.getStructure(this.structureName);
        } catch (InvalidIdentifierException invalidIdentifierException) {
            return false;
        }
        if (!optional.isPresent()) {
            return false;
        }
        return this.place(world, bl, optional.get());
    }

    public boolean place(ServerWorld world, boolean bl, Structure structure) {
        Vec3i vec3i;
        boolean bl2;
        BlockPos blockPos = this.getPos();
        if (!StringHelper.isEmpty(structure.getAuthor())) {
        }
        if (!(bl2 = this.size.equals(vec3i = structure.getSize()))) {
            this.size = vec3i;
            this.markDirty();
            BlockState blockState = world.getBlockState(blockPos);
            world.updateListeners(blockPos, blockState, blockState, Block.NOTIFY_ALL);
        }
        if (!bl || bl2) {
            StructurePlacementData structurePlacementData = new StructurePlacementData().setMirror(this.mirror).setRotation(this.rotation).setIgnoreEntities(true);
            BlockPos blockPos2 = blockPos.add(this.offset);
            structure.place(world, blockPos2, blockPos2, structurePlacementData, StructureBlockEntity.createRandom(1), Block.NOTIFY_LISTENERS);
            return true;
        }
        return false;
    }

    public void unloadStructure() {
        if (this.structureName == null) {
            return;
        }
        ServerWorld serverWorld = (ServerWorld)this.world;
        StructureManager structureManager = serverWorld.getStructureManager();
        structureManager.unloadStructure(this.structureName);
    }

    public boolean isStructureAvailable() {
        if (this.mode != StructureBlockMode.LOAD || this.world.isClient || this.structureName == null) {
            return false;
        }
        ServerWorld serverWorld = (ServerWorld)this.world;
        StructureManager structureManager = serverWorld.getStructureManager();
        try {
            return structureManager.getStructure(this.structureName).isPresent();
        } catch (InvalidIdentifierException invalidIdentifierException) {
            return false;
        }
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public boolean shouldShowAir() {
        return this.showAir;
    }

    public void setShowAir(boolean showAir) {
        this.showAir = showAir;
    }

    public boolean shouldShowBoundingBox() {
        return this.showBoundingBox;
    }

    public void setShowBoundingBox(boolean showBoundingBox) {
        this.showBoundingBox = showBoundingBox;
    }

    private static /* synthetic */ void setStructureVoid(ServerWorld world, BlockPos pos) {
        world.setBlockState(pos, Blocks.STRUCTURE_VOID.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    public static enum Action {
        UPDATE_DATA,
        SAVE_AREA,
        LOAD_AREA,
        SCAN_AREA;

    }
}

