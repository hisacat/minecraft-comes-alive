package mca.network.client;

import mca.ClientProxy;
import mca.cobalt.network.Message;
import mca.resources.data.Analysis;
import net.minecraft.entity.player.PlayerEntity;

public class AnalysisResults implements Message {
    public final Analysis<?> analysis;

    public AnalysisResults(Analysis<?> analysis) {
        this.analysis = analysis;
    }

    @Override
    public void receive(PlayerEntity e) {
        ClientProxy.getNetworkHandler().handleAnalysisResults(this);
    }
}