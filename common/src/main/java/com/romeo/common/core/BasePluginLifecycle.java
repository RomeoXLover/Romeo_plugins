package com.romeo.common.core;

import com.romeo.common.api.PluginLogger;

public abstract class BasePluginLifecycle {
    private final PluginLogger logger;

    protected BasePluginLifecycle(PluginLogger logger) {
        this.logger = logger;
    }

    public final void onLoad() {
        logger.info("Loading " + getName() + "...");
        onLoadInternal();
    }

    public final void onEnable() {
        logger.info("Enabling " + getName() + "...");
        onEnableInternal();
    }

    public final void onDisable() {
        logger.info("Disabling " + getName() + "...");
        onDisableInternal();
    }

    protected abstract String getName();

    protected void onLoadInternal() {
    }

    protected void onEnableInternal() {
    }

    protected void onDisableInternal() {
    }
}
