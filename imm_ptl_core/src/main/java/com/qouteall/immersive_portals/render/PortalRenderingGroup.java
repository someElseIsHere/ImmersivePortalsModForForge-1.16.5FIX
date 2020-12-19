package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.my_util.Plane;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class PortalRenderingGroup implements PortalLike {
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    public final Portal.TransformationDesc transformationDesc;
    public final List<Portal> portals = new ArrayList<>();
    
    private Box exactBoundingBox;
    private Vec3d origin;
    private Vec3d dest;
    
    // null means not checked
    // empty means does not enclose
    @Nullable
    private Optional<Box> enclosedDestAreaBoxCache = null;
    
    private final UUID uuid = MathHelper.randomUuid();
    
    public PortalRenderingGroup(Portal.TransformationDesc transformationDesc) {
        this.transformationDesc = transformationDesc;
    }
    
    public void addPortal(Portal portal) {
        Validate.isTrue(portal.world.isClient());
        Validate.isTrue(!portal.getIsGlobal());
        
        if (portals.contains(portal)) {
            limitedLogger.err("Adding duplicate portal into group " + portal);
            return;
        }
        
        portals.add(portal);
        updateCache();
    }
    
    public void removePortal(Portal portal) {
        portals.remove(portal);
        
        updateCache();
    }
    
    public void updateCache() {
        exactBoundingBox = null;
        origin = null;
        dest = null;
        enclosedDestAreaBoxCache = null;
    }
    
    public Box getEnclosedDestAreaBox() {
        if (enclosedDestAreaBoxCache == null) {
            initEnclosedDestAreaBox();
        }
        
        return enclosedDestAreaBoxCache.orElse(null);
    }
    
    private void initEnclosedDestAreaBox() {
        if (portals.size() < 6) {
            enclosedDestAreaBoxCache = Optional.empty();
        }
        else {
            boolean enclose = portals.stream().map(
                p -> {
                    Vec3d contentDirection = p.getContentDirection();
                    return Direction.getFacing(
                        contentDirection.x, contentDirection.y, contentDirection.z
                    );
                }
            ).collect(Collectors.toSet()).size() == 6;
            
            if (!enclose) {
                enclosedDestAreaBoxCache = Optional.empty();
            }
            else {
                enclosedDestAreaBoxCache = Optional.of(
                    Helper.transformBox(getExactAreaBox(), pos -> {
                        return portals.get(0).transformPoint(pos);
                    })
                );
            }
        }
    }
    
    @Override
    public boolean isConventionalPortal() {
        return false;
    }
    
    @Override
    public Box getExactAreaBox() {
        if (exactBoundingBox == null) {
            exactBoundingBox = portals.stream().map(
                Portal::getExactBoundingBox
            ).reduce(Box::union).get();
        }
        return exactBoundingBox;
    }
    
    @Override
    public Vec3d transformPoint(Vec3d pos) {
        return portals.get(0).transformPoint(pos);
    }
    
    @Override
    public Vec3d transformLocalVec(Vec3d localVec) {
        return portals.get(0).transformLocalVec(localVec);
    }
    
    @Override
    public double getDistanceToNearestPointInPortal(Vec3d point) {
        return Helper.getDistanceToBox(getExactAreaBox(), point);
    }
    
    @Override
    public double getDestAreaRadiusEstimation() {
        double maxDimension = getSizeEstimation();
        return maxDimension * transformationDesc.scaling;
    }
    
    
    @Override
    public Vec3d getOriginPos() {
        if (origin == null) {
            origin = getExactAreaBox().getCenter();
        }
        
        return origin;
    }
    
    @Override
    public Vec3d getDestPos() {
        if (dest == null) {
            dest = transformPoint(getOriginPos());
        }
        
        return dest;
    }
    
    @Override
    public World getOriginWorld() {
        return portals.get(0).world;
    }
    
    @Override
    public World getDestWorld() {
        return portals.get(0).getDestWorld();
    }
    
    @Override
    public RegistryKey<World> getDestDim() {
        return portals.get(0).getDestDim();
    }
    
    @Override
    public boolean isRoughlyVisibleTo(Vec3d cameraPos) {
        return true;
    }
    
    @Nullable
    @Override
    public Plane getInnerClipping() {
        return null;
    }
    
    @Nullable
    @Override
    public Quaternion getRotation() {
        return transformationDesc.rotation;
    }
    
    @Override
    public double getScale() {
        return transformationDesc.scaling;
    }
    
    @Override
    public boolean getIsGlobal() {
        return false;
    }
    
    @Nullable
    @Override
    public Vec3d[] getInnerFrustumCullingVertices() {
        return null;
    }
    
    @Nullable
    @Override
    public Vec3d[] getOuterFrustumCullingVertices() {
        return null;
    }
    
    @Override
    public void renderViewAreaMesh(Vec3d posInPlayerCoordinate, Consumer<Vec3d> vertexOutput) {
        for (Portal portal : portals) {
            Vec3d relativeToGroup = portal.getOriginPos().subtract(getOriginPos());
            portal.renderViewAreaMesh(
                posInPlayerCoordinate.add(relativeToGroup),
                vertexOutput
            );
        }
    }
    
    @Nullable
    @Override
    public Matrix4f getAdditionalCameraTransformation() {
        return portals.get(0).getAdditionalCameraTransformation();
    }
    
    @Nullable
    @Override
    public UUID getDiscriminator() {
        return uuid;
    }
    
    public void purge() {
        portals.removeIf(portal -> {
            return portal.removed;
        });
    }
    
    @Override
    public boolean isParallelWith(Portal portal) {
        return portals.stream().anyMatch(p -> p.isParallelWith(portal));
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public void doAdditionalRenderingCull(ObjectList<?> visibleChunks) {
        Box enclosedDestAreaBox = getEnclosedDestAreaBox();
        if (enclosedDestAreaBox != null) {
            Helper.removeIf(visibleChunks, (obj) -> {
                ChunkBuilder.BuiltChunk builtChunk =
                    ((IEWorldRendererChunkInfo) obj).getBuiltChunk();
                
                return !builtChunk.boundingBox.intersects(enclosedDestAreaBox);
            });
        }
    }
    
    @Override
    public String toString() {
        return String.format("PortalRenderingGroup(%s)%s", portals.size(), portals.get(0).portalTag);
    }
    
}
