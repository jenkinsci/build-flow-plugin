package org.tuenti.lock;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Result;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Class in charge of allocating resources and managing ownership
 * @author Julia S.Simon <julia@tuenti.com>
 */
public final class ResourceAllocator {

    private final Computer node;
    public final static Map<String, AbstractBuild> resourceHolders = new HashMap<String, AbstractBuild>();
    private static final Map<Computer, WeakReference<ResourceAllocator>> nodeResourceManagers = new WeakHashMap<Computer, WeakReference<ResourceAllocator>>();

    private ResourceAllocator(Computer node) {
        this.node = node;
    }
    
    /**
     * 
     * @param node Reference to a Jenkins Slave
     * @return Resource allocator associated with given node
     */
    public static ResourceAllocator getNodeAllocator(Computer node) {
        ResourceAllocator manager;
        WeakReference<ResourceAllocator> currentNodeRM = nodeResourceManagers.get(node);
        if (currentNodeRM != null) {
            manager = currentNodeRM.get();
            if (manager != null) {
                return manager;
            }
        }
        manager = new ResourceAllocator(node);
        ResourceAllocator.nodeResourceManagers.put(node, new WeakReference<ResourceAllocator>(manager));
        return manager;
    }

    /**
     * 
     * @param owner The build which is requesting the resource
     * @param resourceName Name of resource we want to allocate
     * @param buildListener
     * @return An allocated Resource
     * @throws InterruptedException
     * @throws IOException 
     */
    public synchronized Resource allocate(AbstractBuild owner, String resourceName, BuildListener buildListener) throws InterruptedException, IOException {
        PrintStream logger = buildListener.getLogger();
        AbstractBuild holder = resourceHolders.get(resourceName);
        
        while (holder != null) {
            if (holderIsNotRunning(holder)) {
                logger.println("Resource " + resourceName + " currently use by : " + holder.toString()
                    + " which is not running, freeing it");
                this.asyncFree(resourceName);
            } else {
                logger.println("Waiting ressource : " + resourceName + " currently use by : " + resourceHolders.get(resourceName).toString());
            }
            wait();
            holder = resourceHolders.get(resourceName);
        }

        resourceHolders.put(resourceName, owner);
        return new Resource(resourceName, this);
    }

    /**
     * Frees a resource
     * @param resourceName Name of the resource we want to free 
     */
    public synchronized void free(String resourceName) {
        this.asyncFree(resourceName);
        notifyAll();
    }
    
    private boolean holderIsNotRunning(AbstractBuild holder){
        Result result = holder.getResult();
        return result == Result.ABORTED ||
            result == Result.NOT_BUILT ||
            result == Result.FAILURE; 
    }
    
    private void asyncFree(String n) {
        resourceHolders.remove(n);
    }
}
