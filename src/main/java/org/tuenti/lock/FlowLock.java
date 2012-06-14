package org.tuenti.lock;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Executor;
import java.io.IOException;
import java.io.PrintStream;


/**
 * Class to manage locks for locksections in flow definition
 * Its in charge of managing it in Jenkins
 * @author Julia S.Simon <julia@tuenti.com>
 */
public class FlowLock {
    
    private String name;
    private final BuildListener listener;
    private final AbstractBuild build;
    public static ResourceAllocator manager = null;
    private final PrintStream logger;
    private Resource lock;
    
    public FlowLock(String name, AbstractBuild build,  BuildListener listener) {
        this.name = name;
        this.build = build;
        this.listener = listener;
        this.logger = listener.getLogger();
    }
    
    /**
     * Obtains the lock for a resource
     * @throws IOException
     * @throws InterruptedException 
     */
    public void getLock() throws IOException, InterruptedException{
        Computer node = Executor.currentExecutor().getOwner();
        manager = ResourceAllocator.getNodeAllocator(node);
        logger.println("Waiting to aquire lock " + this.name);
        lock = manager.allocate(build, name, listener);
        logger.println("Lock " + lock + " aquired");
    }
    
    /**
     * Frees the lock from the system
     * @throws IOException
     * @throws InterruptedException 
     */
    public void releaseLock() throws IOException, InterruptedException {
        logger.println("Releasing lock " + this.name);
        lock.free();
    }
    
    @Override
    public String toString(){
        return this.name;
    }
}
