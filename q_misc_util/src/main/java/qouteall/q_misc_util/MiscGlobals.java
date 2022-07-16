package qouteall.q_misc_util;

import net.minecraft.server.MinecraftServer;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class MiscGlobals {
    public static final Set<String> stableNamespaces = new HashSet<>();
    
    public static WeakReference<MinecraftServer> refMinecraftServer =
        new WeakReference<>(null);
    
    public static final MyTaskList serverTaskList = new MyTaskList();
    
}
