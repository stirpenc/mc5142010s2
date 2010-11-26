package osp.Resources;

import java.util.*;

import osp.IFLModules.*;
import osp.Tasks.TaskCB;
import osp.Threads.ThreadCB;
import osp.Utilities.*;
import osp.Memory.*;

/**
    Class ResourceCB is the core of the resource management module.
    Students implement all the do_* methods.
    @OSPProject Resources
*/
public class ResourceCB extends IflResourceCB
{
	private static int resourceCounter = 0;
	private static Hashtable HashT = new Hashtable();
	private static RRB resource = new RRB(null, null, 0);
    /**
       Creates a new ResourceCB instance with the given number of 
       available instances. This constructor must have super(qty) 
       as its first statement.

       @OSPProject Resources
    */
    public ResourceCB(int qty)
    {
    	super(qty);
    }

    /**
       This method is called once, at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Resources
    */
    public static void init()        /*i think it is right*/
    {
    	resourceCounter = ResourceTable.getSize();
    }

    /**
       Tries to acquire the given quantity of this resource.
       Uses deadlock avoidance or detection depending on the
       strategy in use, as determined by ResourceCB.getDeadlockMethod().

       @param quantity
       @return The RRB corresponding to the request.
       If the request is invalid (quantity+allocated>total) then return null.

       @OSPProject Resources
    */
    public RRB do_acquire(int quantity) 
    {
        ThreadCB currentThread = null;
        TaskCB currentTask = null;
        
        try{
        	currentTask = MMU.getPTBR().getTask();
        	currentThread = currentTask.getCurrentThread();
        }

        catch (NullPointerException localNullPointerException){}
        
        if((quantity + getAllocated(currentThread)) > getTotal())
        	return null;
        
        if(!HashT.containsKey(currentThread))
        {
        	HashT.put(currentThread, resource);
        }
        
        RRB currentResource = new RRB(currentThread, this, quantity);
        
        if(getDeadlockMethod() == GlobalVariables.Avoidance){
        	if(BankerAlgorith(currentResource) == Granted){               /*Banker algorthm has to answer that currentResource is granted*/
        		currentResource.grant();
        	}
        	if((currentResource.getStatus() == GlobalVariables.Suspended) && (!HashT.containsValue(currentResource))){ 
        		HashT.put(currentThread, currentResource);
        	}
        }
    
        if(getDeadlockMethod() == GlobalVariables.Detection){
        	if(quantity <= getAvailable()){
        		currentResource.grant();
        	}
        	else{
        		if(currentThread.getStatus() != ThreadWaiting){
        			currentResource.setStatus(GlobalVariables.Suspended);
        			currentThread.suspend(currentResource);
        		}
        		
        		if(!HashT.containsValue(currentResource)){
        			HashT.put(currentThread, currentResource);
        		}
        	}
        }
        
        return currentResource;
    }

    /**
       Performs deadlock detection.
       @return A vector of ThreadCB objects found to be in a deadlock.

       @OSPProject Resources
    */
    public static Vector do_deadlockDetection()
    {  
    	Vector currentVector = deadLockDetectionAlgorith();
    	
    	if(currentVector != null)
    	{
    		solveDeadLock(currentVector);
    		return currentVector;
    	}
    	return null;
    }
    
    public static Vector deadLockDetectionAlgorith()
    {
    	int[] arrayOfResources = new int[resourceCounter];
    	for(int count = 0; count < resourceCounter; count++)
    	{
    		arrayOfResources[count] = ResourceTable.getResourceCB(count).getAvailable();
    	}
    	Hashtable currentHashtable = new Hashtable();
        Enumeration currentEnumeration = HashT.keys();
        
        while(currentEnumeration.hasMoreElements())
        {
        	ThreadCB currentThread = (ThreadCB)currentEnumeration.nextElement();
        	currentHashtable.put(currentThread, new Boolean(false));
        	
        	for(int count = 0; count < resourceCounter; count++)
        	{
        		ResourceCB currentResource = ResourceTable.getResourceCB(count);
        		
        		if(currentResource.getAllocated(currentThread) != 0)
        		{
        			currentHashtable.put(currentThread, new Boolean(true));
        			break;
        		}
        	}
        }
        boolean var;
        ThreadCB currentThread;
        while(true)
        {
        	var = false;
        	currentEnumeration = HashT.keys();
        	
        	while(currentEnumeration.hasMoreElements())
        	{
        		currentThread = (ThreadCB)currentEnumeration.nextElement();
        		boolean cont = false;
        		 if (((Boolean)currentHashtable.get(currentThread)).booleanValue())
        		 {
        			 cont = true;
        			 int res = ((RRB)HashT.get(currentThread)).getQuantity();
        			 
        			 if(res != 0)
        			 {
        				 ResourceCB currentResource2 = ((RRB)HashT.get(currentThread)).getResource();
        				 
        				 if(res > arrayOfResources[currentResource2.getID()])
        				 {
        					 cont = false;
        				 }
        			 }
        			 if(cont)
        			 {
        				 for(int count = 0; count < resourceCounter; count++)
        				 {
        					 arrayOfResources[count] += ResourceTable.getResourceCB(count).getAllocated(currentThread);
        				 }
        				 currentHashtable.put(currentThread, new Boolean(false));
        				 var = true;
        			 }
        		 }
        	}
        	if(!var)
        	{
        		break;
        	}
        }
        Vector vectorThreads = new Vector();
        Enumeration currentEnumeration2 = currentHashtable.keys();
        
        while(currentEnumeration2.hasMoreElements())
        {
        	ThreadCB currentThread2 = (ThreadCB)currentEnumeration2.nextElement();
            if (((Boolean)currentHashtable.get(currentThread2)).booleanValue()) {
            	vectorThreads.addElement(currentThread2);
              }
        }
        if(vectorThreads.isEmpty())
        {
        	return null;
        }
        return vectorThreads;
    }
    
    public static void solveDeadLock(Vector vecThread)
    {
    	int cont = 1;
    	for(int count = 0; count < vecThread.size(); count++)
    	{
    		ThreadCB currentThread = (ThreadCB)vecThread.get(count);
    		if(cont != 0)
    		{
    			currentThread.kill();
    		}
    		
    		if(deadLockDetectionAlgorith() == null)
    		{
    			cont = 0;
    			break;
    		}
    		
    		cont = 1;
    	}
    	
    	RRB currentRRB = null;
    	while((currentRRB = findGranted()) != null)
    	{
    		currentRRB.grant();
    		HashT.put(currentRRB.getThread(), resource);
    	}
    }
    
    int BankerAlgorith(RRB incRes)
    {
    	ThreadCB localThread = incRes.getThread();
    	
    	int reqResources = incRes.getQuantity();
    	
    	ResourceCB localResource = incRes.getResource();
    	if (localResource.getAllocated(localThread) + reqResources > localResource.getMaxClaim(localThread))
    	{
    		//Allocated + Required is bigger than Total
    		incRes.setStatus(GlobalVariables.Denied);
    		return GlobalVariables.Denied;
    	}
    	if (reqResources > localResource.getAvailable())
    	{
    		if((localThread.getStatus() != GlobalVariables.ThreadWaiting) && (incRes.getStatus() != GlobalVariables.Detection))
    		{
    			if(!HashT.contains(incRes))
    			{
    				HashT.put(localThread, incRes);
    			}
    			localThread.suspend(incRes);
    		}
    		incRes.setStatus(GlobalVariables.Suspended);
    		return GlobalVariables.Suspended;
    	}
    	if(IsGranted(incRes))
    	{
    		incRes.setStatus(GlobalVariables.Granted);
    		return GlobalVariables.Granted;
    	}
    	
    	int threadStatus = localThread.getStatus();
    	
    	if( (threadStatus != GlobalVariables.ThreadWaiting) && (threadStatus != GlobalVariables.ThreadKill) && (incRes.getStatus() != GlobalVariables.Suspended))
    	{
    		if(!HashT.contains(incRes))
			{
				HashT.put(localThread, incRes);
			}
			localThread.suspend(incRes);
		}
		incRes.setStatus(GlobalVariables.Suspended);
		return GlobalVariables.Suspended;
    }
    
    private static boolean IsGranted(RRB resource)
    {
    	ThreadCB localThread1 = resource.getThread();
    	ResourceCB localResource1 = resource.getResource();
    	
    	int quantityRequired = resource.getQuantity();
    	int quantityAllocated = localResource1.getAllocated(localThread1);
    	
    	if(quantityRequired > localResource1.getAvailable())
    	{
    		return false;
    	}
    	
    	int[] arrayOfResources = new int[resourceCounter];
    	for(int k = 0; k < resourceCounter; k++)
    	{
    		arrayOfResources[k] = ResourceTable.getResourceCB(k).getAvailable();
    	}
    	localResource1.setAllocated(localThread1, quantityRequired + quantityAllocated);
    	localResource1.setAvailable(arrayOfResources[localResource1.getID()] - quantityRequired);
    	
    	Vector localVector = new Vector();
    	Enumeration localEnumeration = HashT.keys();
    	while (localEnumeration.hasMoreElements())
    	{
    		ThreadCB localThread2 = (ThreadCB)localEnumeration.nextElement();
    		localVector.addElement(localThread2);
    	}
    	
    	boolean cont = true;
    	boolean test = true;
    	
    	while(cont)
    	{
    		test = true;
    		
    		ThreadCB localThread3 = null;
    		
    		for(int count = 0; count < localVector.size(); count ++)
    		{
    			test = true;
    			localThread3 = (ThreadCB)localVector.get(count);
    			if(localVector.isEmpty())
    			{
    				localResource1.setAllocated(localThread1, quantityAllocated);
    				localResource1.setAvailable(arrayOfResources[localResource1.getID()]);
    				
    				return true;
    			}
    			for(int count2 = 0; count2 < resourceCounter; count2++)
    			{
    				ResourceCB localResource3 = ResourceTable.getResourceCB(count2);
    				if(localResource3.getMaxClaim(localThread3) - localResource3.getAllocated(localThread3) >= localResource3.getAvailable())
    					break;
    				test = false;
    			}
    			if(test)
    				break;
    		}
    		if(test)
    		{
    			for(int count = 0; count < resourceCounter; count ++)
    			{
    				ResourceCB localResource2 = ResourceTable.getResourceCB(count);
    				localResource2.setAvailable(localResource2.getAvailable() + localResource2.getAllocated(localThread3));
    			}
    			localVector.remove(localThread3);
    			if(localVector.isEmpty())
    				break;
    		}
    		else
    		{
    			cont = false;
    		}
    	}
    		localResource1.setAllocated(localThread1, quantityAllocated);
    		for(int count = 0; count < resourceCounter; count ++)
    		{
    			ResourceTable.getResourceCB(count).setAvailable(arrayOfResources[count]);
    		}
    		return localVector.isEmpty();
    }

    /**
       When a thread was killed, this is called to release all
       the resources owned by that thread.

       @param thread -- the thread in question

       @OSPProject Resources
    */
    public static void do_giveupResources(ThreadCB thread)
    {
    	int counter = 0;
    	
        if(!HashT.containsKey(thread))
        	return;
        
        while(counter < resourceCounter)
        {
        	ResourceCB currentResource = ResourceTable.getResourceCB(counter);
        	
        	if(currentResource.getAllocated(thread) != 0)
        	{ 
        		currentResource.setAvailable(currentResource.getAvailable() + currentResource.getAllocated(thread));      /*falta a chamada do metodo*/ /* ------------- Qual metodo? Algoritmo do banqueio ---------- */
        	}
        	
        	currentResource.setAllocated(thread, 0);    /*I am not sure*/
        	counter++;
        }
        
        HashT.remove(thread);
        
        RRB newRRB = null;
        
        while((newRRB = findGranted()) != null){ /* ------------- AQUI 523---------- */
        	if((newRRB.getThread().getStatus() != GlobalVariables.ThreadKill) && (newRRB.getThread() != thread)){
        		newRRB.grant();      
        		HashT.put(newRRB.getThread(), resource);
        	}
        
    		HashT.put(newRRB.getThread(), resource);
        }
        

    }

    /**
        Release a previously acquired resource.

	@param quantity

        @OSPProject Resources
    */
    public void do_release(int quantity)     /*i think it is*/
    {
        ThreadCB currentThread = null;
        TaskCB currentTask = null;
        
        try{
        	currentTask = MMU.getPTBR().getTask();
            currentThread = currentTask.getCurrentThread();
        }
        
        catch (NullPointerException localNullPointerException) {}
        
        int counter = getAllocated(currentThread);
        
        if( quantity > counter){
        	quantity = counter;
        }
        
        setAllocated(currentThread, counter - quantity);
        setAvailable(getAvailable() + quantity);
        
        RRB newRRB = null;
        while ((newRRB = findGranted()) != null){    /* ------------- AQUI ---------- */
        	if(newRRB.getThread().getStatus() != GlobalVariables.ThreadKill){
        		newRRB.grant();         
        		
        		HashT.put(newRRB.getThread(), resource);
        	}
        
    		HashT.put(newRRB.getThread(), resource);
        }

    }
    
    public static RRB findGranted()
    {
    	Collection currentCollection = HashT.values();
    	Iterator currentIterator = currentCollection.iterator();
    	
    	while(currentIterator.hasNext())
    	{
    		RRB currentRRB = (RRB)currentIterator.next();
    		if(currentRRB.getThread() == null)
    		{
    			continue;
    		}
    		
    		if((getDeadlockMethod() == GlobalVariables.Avoidance) && (IsGranted(currentRRB)))
			{
				currentRRB.setStatus(GlobalVariables.Granted);
				return currentRRB;
			}
    		if((getDeadlockMethod() == GlobalVariables.Detection) && (currentRRB.getQuantity() <= currentRRB.getResource().getAvailable()))
    		{
    			return currentRRB;
    		}
    	}
    	return null;
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Resources
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
	@OSPProject Resources
    */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
