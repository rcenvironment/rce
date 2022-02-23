/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.rpc.api.RemotableCallbackService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Background task which sets the time to live (TTL) for call back objects and call back proxy objects which are held by
 * {@link CallbackService} and {@link CallbackProxyService}. Additionally, it removes objects and proxies which time to live is expired.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class CleanJob {

    /** Represents 10 min. */
    public static final int TTL_MSEC = 600000;

    /** Represents 8 min. */
    public static final int UPDATE_INTERVAL_MSEC = 480000;

    /** Key for the service to call contained in the job context. */
    public static final String SERVICE = "de.rcenvironment.rce.communication.callback.service";

    /** Key for the weak object references map contained in the job context. */
    public static final String WEAK_MAP = "de.rcenvironment.rce.communication.callback.weak";

    /** Key for the TTL map contained in the job context. */
    public static final String TTL_MAP = "de.rcenvironment.rce.communication.callback.ttl";

    /** Key for the platforms (to call) map contained in the job context. */
    public static final String PLATFORMS_MAP = "de.rcenvironment.rce.communication.callback.platforms";

    private static CommunicationService communicationService;

    private static Map<String, ScheduledFuture<?>> backgroundCleanJob = new HashMap<String, ScheduledFuture<?>>();

    /** Only called by OSGi. */
    @Deprecated
    public CleanJob() {}

    protected void activate(BundleContext bundleContext) {}

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }

    protected void unbindCommunicationService(CommunicationService oldCommunicationService) {
        communicationService = ServiceUtils.createFailingServiceProxy(CommunicationService.class);
    }

    /**
     * Cleans callback objects and callback proxies.
     * 
     * @author Doreen Seider
     */
    protected static class CleanRunnable implements Runnable {

        private final Class<?> iface;

        private final Map<String, WeakReference<Object>> objects;

        private final Map<String, InstanceNodeSessionId> nodes;

        private final Map<String, Long> ttls;

        protected CleanRunnable(Class<?> iface, Map<String, WeakReference<Object>> objects,
            Map<String, Long> ttls, Map<String, InstanceNodeSessionId> nodes) {

            this.iface = iface;
            this.objects = objects;
            this.nodes = nodes;
            this.ttls = ttls;
        }

        @Override
        public void run() {

            // remove all unreferenced and expired objects and renew TTL for all remaining objects
            synchronized (objects) {
                for (Iterator<String> iterator = objects.keySet().iterator(); iterator.hasNext();) {
                    String id = iterator.next();
                    if (objects.get(id).get() == null || new Date(ttls.get(id)).before(new Date())) {
                        iterator.remove();
                        ttls.remove(id);
                        nodes.remove(id);
                    } else {
                        try {
                            if (iface == CallbackProxyService.class) {
                                RemotableCallbackService remoteService =
                                    (RemotableCallbackService) communicationService.getRemotableService(RemotableCallbackService.class,
                                        nodes.get(id));
                                remoteService.setTTL(id, ttls.get(id));
                            } else if (iface == CallbackService.class) {
                                // this code path doesn't seem to be used anymore; replacing with exception to test - misc_ro, Oct 2015
                                throw new RemoteOperationException("Unexpected callback code path used");
                                // CallbackProxyService service = (CallbackProxyService) communicationService
                                // .getService(CallbackProxyService.class, nodes.get(id), context);
                                // service.setTTL(id, new Date(System.currentTimeMillis() + CleanJob.TTL_MSEC).getTime());
                            }
                        } catch (RemoteOperationException | RuntimeException e) {
                            // temporary fix for remote call failures;
                            // see https://www.sistec.dlr.de/mantis/view.php?id=6542
                            LogFactory.getLog(getClass()).debug("Failed to update TTL for id " + id + " via " + iface.getSimpleName()
                                + " @ " + nodes.get(id));
                        }
                    }
                }
            }
        }

    }

    /**
     * Schedules a clean job.
     * 
     * @param iface The service's iface to call for TTL update.
     * @param objects The referenced objects.
     * @param ttls The TTLs of the references objects.
     * @param platforms The platforms to call for TTL update.
     */
    @SuppressWarnings({ "rawtypes" })
    public static void scheduleJob(Class iface,
        Map<String, WeakReference<Object>> objects,
        Map<String, Long> ttls,
        Map<String, InstanceNodeSessionId> platforms) {

        synchronized (CleanJob.class) {
            if (!backgroundCleanJob.containsKey(iface.getCanonicalName())) {
                CleanRunnable runnable = new CleanRunnable(iface, objects, ttls, platforms);
                backgroundCleanJob.put(iface.getCanonicalName(),
                    ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedInterval(
                        "Communication Layer: Purge old callback objects/proxies and renew TTL for remaining", runnable,
                        CleanJob.UPDATE_INTERVAL_MSEC));
            }

        }
    }

    /**
     * Unschedules a clean job.
     * 
     * @param iface The service's iface to call for TTL update.
     */
    @SuppressWarnings({ "rawtypes" })
    public static void unscheduleJob(Class iface) {

        synchronized (CleanJob.class) {
            if (backgroundCleanJob.containsKey(iface.getCanonicalName())) {
                boolean cancelled = backgroundCleanJob.get(iface.getCanonicalName()).cancel(true);
                if (!cancelled) {
                    LogFactory.getLog(CleanJob.class).warn("Clean job triggered by " + iface.getCanonicalName()
                        + " could not be cancelled. Probably, it was already done.");
                }
            }
        }

    }

}
