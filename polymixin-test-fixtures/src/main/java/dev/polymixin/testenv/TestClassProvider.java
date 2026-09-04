package dev.polymixin.testenv;

import org.spongepowered.asm.service.IClassProvider;

import java.net.URL;

public final class TestClassProvider implements IClassProvider {

    @Override
    @SuppressWarnings("deprecation")
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, false, TestClassProvider.class.getClassLoader());
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, TestClassProvider.class.getClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, TestClassProvider.class.getClassLoader());
    }
}
