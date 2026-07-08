package net.frankheijden.serverutils.velocity.reflection;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import dev.frankheijden.minecraftreflection.ClassObject;
import dev.frankheijden.minecraftreflection.MinecraftReflection;
import dev.frankheijden.minecraftreflection.exceptions.MinecraftReflectionException;

public class RVelocityConsole {

    private static final MinecraftReflection reflection = MinecraftReflection
            .of("com.velocitypowered.proxy.console.VelocityConsole");

    private RVelocityConsole() {}

    /**
     * Sets the permission function of the console.
     * Velocity stores it in the 'permissionFunction' field, whereas Velocity-CTD wraps it
     * into a PermissionResolver and stores it in the 'permissionResolver' field.
     */
    public static void setPermissionFunction(ConsoleCommandSource velocityConsole, PermissionFunction function) {
        try {
            reflection.getClazz().getDeclaredField("permissionFunction");
        } catch (NoSuchFieldException ex) {
            setPermissionResolver(velocityConsole, function);
            return;
        }

        reflection.set(velocityConsole, "permissionFunction", function);
    }

    private static void setPermissionResolver(ConsoleCommandSource velocityConsole, PermissionFunction function) {
        Class<?> resolverClass = MinecraftReflection
                .of("com.velocityctd.api.permission.PermissionResolver")
                .getClazz();

        Object resolver;
        if (resolverClass.isInstance(function)) {
            resolver = function;
        } else {
            resolver = createPermissionResolver(velocityConsole, function);
        }

        reflection.set(velocityConsole, "permissionResolver", resolver);
    }

    private static Object createPermissionResolver(
            ConsoleCommandSource velocityConsole,
            PermissionFunction function
    ) {
        try {
            return MinecraftReflection
                    .of("com.velocityctd.proxy.permission.PermissionResolverAdapterFactory")
                    .invoke(
                            null,
                            "createPermissionResolverAdapter",
                            ClassObject.of(PermissionSubject.class, velocityConsole),
                            ClassObject.of(PermissionFunction.class, function)
                    );
        } catch (MinecraftReflectionException ex) {
            return MinecraftReflection
                    .of("com.velocityctd.api.permission.PermissionResolverFunctionAdapter")
                    .newInstance(ClassObject.of(PermissionFunction.class, function));
        }
    }
}
