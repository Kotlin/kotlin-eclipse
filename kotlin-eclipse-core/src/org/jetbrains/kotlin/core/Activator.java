package org.jetbrains.kotlin.core;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	@Override
    public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
	}

	@Override
    public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}
}
