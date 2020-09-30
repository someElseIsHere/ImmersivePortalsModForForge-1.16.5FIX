package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class PortalPresentation {
    
    // not stored inside Portal because this has its own dispose strategy
    private static final HashMap<Portal, PortalPresentation> dataMap = new HashMap<>();
    
    private static long lastPurgeTime = 0;
    
    // dispose by the last active time
    // using finalize() depends on GC and is not reliable
    private long lastActiveNanoTime;
    
    @Nullable
    public Map<List<UUID>, GlQueryObject> lastFrameQuery;
    
    @Nullable
    public Map<List<UUID>, GlQueryObject> thisFrameQuery;
    
    public int thisFrameQueryFrameIndex = -1;
    
    @Nullable
    public Boolean lastFrameRendered;
    
    @Nullable
    public Boolean thisFrameRendered;
    
    private long mispredictTime1 = 0;
    private long mispredictTime2 = 0;
    
    public static void init() {
        ModMain.preRenderSignal.connect(() -> {
            long currTime = System.nanoTime();
            if (currTime - lastPurgeTime > Helper.secondToNano(30)) {
                lastPurgeTime = currTime;
                dataMap.entrySet().removeIf(entry -> {
                    Portal portal = entry.getKey();
                    PortalPresentation presentation = entry.getValue();
                    
                    boolean shouldRemove = portal.removed || presentation.shouldDispose(currTime);
                    
                    if (shouldRemove) {
                        presentation.dispose();
                    }
                    
                    return shouldRemove;
                });
            }
        });
    }
    
    public static void cleanup() {
        for (PortalPresentation presentation : dataMap.values()) {
            presentation.dispose();
        }
        dataMap.clear();
        
        Helper.log("Cleaning up portal presentation info");
    }
    
    private static void purge() {
        final long currTime = System.nanoTime();
        dataMap.entrySet().removeIf(entry -> {
            final PortalPresentation presentation = entry.getValue();
            return presentation.shouldDispose(currTime);
        });
    }
    
    public static PortalPresentation get(Portal portal) {
        return dataMap.computeIfAbsent(
            portal, k -> new PortalPresentation()
        );
    }
    
    public PortalPresentation() {
        lastActiveNanoTime = System.nanoTime();
    }
    
    public void onUsed() {
        lastActiveNanoTime = System.nanoTime();
    }
    
    private boolean shouldDispose(long currTime) {
        return currTime - lastActiveNanoTime > Helper.secondToNano(60);
    }
    
    public void dispose() {
        if (lastFrameQuery != null) {
            for (GlQueryObject queryObject : lastFrameQuery.values()) {
                GlQueryObject.returnQueryObject(queryObject);
            }
            lastFrameQuery.clear();
        }
        
        if (thisFrameQuery != null) {
            for (GlQueryObject queryObject : thisFrameQuery.values()) {
                GlQueryObject.returnQueryObject(queryObject);
            }
            thisFrameQuery.clear();
        }
    }
    
    private void updateQuerySet() {
        onUsed();
        if (RenderStates.frameIndex != thisFrameQueryFrameIndex) {
            
            if (RenderStates.frameIndex == thisFrameQueryFrameIndex + 1) {
                
                if (lastFrameQuery != null) {
                    for (GlQueryObject queryObject : lastFrameQuery.values()) {
                        GlQueryObject.returnQueryObject(queryObject);
                    }
                    lastFrameQuery.clear();
                }
                
                lastFrameQuery = thisFrameQuery;
                thisFrameQuery = null;
                
                lastFrameRendered = thisFrameRendered;
                thisFrameRendered = null;
            }
            else {
                if (lastFrameQuery != null) {
                    for (GlQueryObject queryObject : lastFrameQuery.values()) {
                        GlQueryObject.returnQueryObject(queryObject);
                    }
                    lastFrameQuery.clear();
                }
                
                lastFrameRendered = null;
                thisFrameRendered = null;
            }
            
            thisFrameQueryFrameIndex = RenderStates.frameIndex;
        }
    }
    
    @Nullable
    public GlQueryObject getLastFrameQuery(List<UUID> desc) {
        updateQuerySet();
        if (lastFrameQuery == null) {
            return null;
        }
        return lastFrameQuery.get(desc);
    }
    
    public GlQueryObject acquireThisFrameQuery(List<UUID> desc) {
        updateQuerySet();
        if (thisFrameQuery == null) {
            thisFrameQuery = new HashMap<>();
        }
        return thisFrameQuery.computeIfAbsent(desc, k -> GlQueryObject.acquireQueryObject());
    }
    
    public void clearMispredictionRecord() {
//        mispredictTime1 = 0;
//        mispredictTime2 = 0;
    
    }
    
    public void onMispredict() {
        mispredictTime1 = mispredictTime2;
        mispredictTime2 = System.nanoTime();
    }
    
    public boolean isFrequentlyMispredicted() {
        long currTime = System.nanoTime();
        
        return (currTime - mispredictTime1) < Helper.secondToNano(30);
    }
}
