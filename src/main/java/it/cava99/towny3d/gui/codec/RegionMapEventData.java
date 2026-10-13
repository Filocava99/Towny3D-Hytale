package it.cava99.towny3d.gui.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Java event data class for RegionMapGui.
 * Declares its BuilderCodec directly via addField (matching the Kotlin GUIs), replacing
 * the former codeclib CodecFactory reflection approach.
 */
public class RegionMapEventData {

    public static final BuilderCodec<RegionMapEventData> CODEC =
        BuilderCodec.builder(RegionMapEventData.class, RegionMapEventData::new)
            .addField(
                new KeyedCodec<>("Action", Codec.STRING, false),
                RegionMapEventData::setAction,
                RegionMapEventData::getAction)
            .build();

    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
