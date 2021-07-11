package mca.entity.data;

import mca.cobalt.minecraft.nbt.CNBT;
import mca.cobalt.minecraft.world.storage.CWorldSavedData;
import mca.util.WorldUtils;
import net.minecraft.world.World;

public class VillageSaveData extends CWorldSavedData {
    public static VillageSaveData get(World world) {
        return WorldUtils.loadData(world, VillageSaveData::new, "mca_village");
    }

    @Override
    public CNBT save(CNBT nbt) {
        return nbt;
    }
}
