package org.tuenti.lock;

/**
 * Class that respresents a resource, should be allocated by ResourceAllocator
 * @author Julia S.Simon <julia@tuenti.com>
 */
public class Resource {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final ResourceAllocator manager;
   
    protected Resource(String name, ResourceAllocator manager) {
        this.name = name;
        this.manager = manager;
    }

    /**
     * Frees the resource from the system
     */
    public void free(){
        manager.free(name);
    }
    
    @Override
    public String toString(){
        return this.name;
    }
}