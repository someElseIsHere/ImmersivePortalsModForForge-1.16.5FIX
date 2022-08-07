package qouteall.imm_ptl.core.compat.sodium_compatibility;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qouteall.imm_ptl.core.compat.mixin.IESodiumWorldRenderer;
import qouteall.imm_ptl.core.render.FrustumCuller;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class SodiumInterface {
    
    @Nullable
    public static FrustumCuller frustumCuller = null;
    
    public static class Invoker {
        public boolean isSodiumPresent() {
            return false;
        }
        
//        public ClientChunkManager createClientChunkManager(
//            ClientWorld world, int loadDistance
//        ) {
//            return new MyClientChunkManager(world, loadDistance);
//        }
        
        public Object createNewContext() {
            return null;
        }
        
        public void switchContextWithCurrentWorldRenderer(Object context) {
        
        }
    
        public void markSpriteActive(TextureAtlasSprite sprite) {
        
        }
    }
    
    public static Invoker invoker = new Invoker();
    
    public static class OnSodiumPresent extends Invoker {
        @Override
        public boolean isSodiumPresent() {
            return true;
        }
        
//        @Override
//        public ClientChunkManager createClientChunkManager(ClientWorld world, int loadDistance) {
//            return new ClientChunkManagerWithSodium(world, loadDistance);
//        }
        
        @Override
        public Object createNewContext() {
            return new SodiumRenderingContext();
        }
        
        @Override
        public void switchContextWithCurrentWorldRenderer(Object context) {
            SodiumWorldRenderer swr = ((WorldRendererExtended) Minecraft.getInstance().levelRenderer).getSodiumWorldRenderer();
            swr.scheduleTerrainUpdate();

            RenderSectionManager renderSectionManager = ((IESodiumWorldRenderer) swr).ip_getRenderSectionManager();

            ((IESodiumRenderSectionManager) renderSectionManager)
                .ip_swapContext(((SodiumRenderingContext) context));

            swr.scheduleTerrainUpdate();
        }
        
        @Override
        public void markSpriteActive(TextureAtlasSprite sprite) {
            SpriteUtil.markSpriteActive(sprite);
        }
    }
    
}
