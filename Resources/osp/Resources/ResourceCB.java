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
	private static Hashtable<ThreadCB, RRB> HashT = new Hashtable<ThreadCB, RRB>();
	private static RRB resource = new RRB(null, null, 0);
    /**
       Creates a new ResourceCB instance with the given number of 
       available instances. This constructor must have super(qty) 
       as its first statement.

       @OSPProject Resources
    */
    public ResourceCB(int qty)  	//REVIEW ALREADY DONE
    {
    	super(qty);
    }

    /**
       This method is called once, at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Resources
    */
    public static void init()       	//REVIEW ALREADY DONE 
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
    public RRB do_acquire(int quantity)   	//REVIEW ALREADY DONE
    {
        TaskCB currentTask = MMU.getPTBR().getTask();
        ThreadCB currentThread = currentTask.getCurrentThread();
        RRB currentResource = new RRB(currentThread, this, quantity);
        
        if((quantity + getAllocated(currentThread)) > getTotal())		//Described in the method header
        	return null;
        
        if(!HashT.containsKey(currentThread))
        {
        	HashT.put(currentThread, resource);
        }

        if(getDeadlockMethod() == GlobalVariables.Avoidance){
        	if(BankerAlgorith(currentResource) == Granted){               /*Banker algorthm has to answer that currentResource is granted*/
        		currentResource.grant();
        	}
        	
        	if(currentResource.getStatus() == GlobalVariables.Suspended)
        		{
        			if(!HashT.containsValue(currentResource)){ 
        				HashT.put(currentThread, currentResource);
        			}
        		}
        }
    
        if(getDeadlockMethod() == GlobalVariables.Detection){
        	if(quantity <= getAvailable()){
        		currentResource.grant();
        	}
        	else{
        		if(currentThread.getStatus() != ThreadWaiting){		//the request cannot be granted, so it is suspended and return NULL
        			currentResource.setStatus(GlobalVariables.Suspended);
        			currentThread.suspend(currentResource);
        			return null;
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
    public static Vector<ThreadCB> do_deadlockDetection()		//REVIEW ALREADY DONE
    {  
    	Vector<ThreadCB> currentVector = deadLockDetectionAlgorith();	//verify if there is a deadlock, return NULL to safe state and not null to deadlock state
    	
    	if(currentVector == null)
    	{
    		return null;
    	}
    	
    	solveDeadLock(currentVector);		
    	return currentVector;
    }
    
    public static Vector<ThreadCB> deadLockDetectionAlgorith()
    {
    	int[] arrayOfResources = new int[resourceCounter];
    	int count = 0;
    	
    	while(count < resourceCounter)
    	{
    		arrayOfResources[count] = ResourceTable.getResourceCB(count).getAvailable();
    		count++;
    	}
    		
    	Hashtable<ThreadCB, Boolean> currentHashtable = new Hashtable<ThreadCB, Boolean>();
        Enumeration<ThreadCB> currentEnumeration = HashT.keys();
        
        count = 0;
        while(currentEnumeration.hasMoreElements() == true)
        {
        	ThreadCB currentThread = (ThreadCB)currentEnumeration.nextElement();
        	currentHashtable.put(currentThread, new Boolean(false));
        	
        	while(count < resourceCounter)
        	{
        		ResourceCB currentResource = ResourceTable.getResourceCB(count);
        
        		if(currentResource.getAllocated(currentThread) != 0)
        		{
        			currentHashtable.put(currentThread, new Boolean(true));
        			break;
        		}
        		count++;
        	}
        }
        
        int var = 0;
        ThreadCB currentThread;
        while(true)
        {
        	var = 0;
        	currentEnumeration = HashT.keys();
        	
        	while(currentEnumeration.hasMoreElements() == true)
        	{
        		currentThread = (ThreadCB)currentEnumeration.nextElement();
        		int cont = 0;
        		 if (((Boolean)currentHashtable.get(currentThread)).booleanValue())
        		 {
        			 cont = 1;
        			 int res = ((RRB)HashT.get(currentThread)).getQuantity();
        			 
        			 if(res != 0)
        			 {
        				 ResourceCB currentResource2 = ((RRB)HashT.get(currentThread)).getResource();
        				 
        				 if(res > arrayOfResources[currentResource2.getID()])
        				 {
        					 cont = 0;
        				 }
        			 }
        			 if(cont != 0)
        			 {
        				 for(int count2 = 0; count2 < resourceCounter; count2++)
        				 {
        					 arrayOfResources[count2] += ResourceTable.getResourceCB(count2).getAllocated(currentThread);
        				 }
        				 currentHashtable.put(currentThread, new Boolean(false));
        				 var = 1;
        			 }
        		 }
        	}
        	if(var == 0)
        	{
        		break;
        	}
        }
        Vector<ThreadCB> vectorThreads = new Vector<ThreadCB>();
        Enumeration<ThreadCB> currentEnumeration2 = currentHashtable.keys();
        
        while(currentEnumeration2.hasMoreElements() == true)
        {
        	ThreadCB currentThread2 = (ThreadCB)currentEnumeration2.nextElement();
            if (((Boolean)currentHashtable.get(currentThread2)).booleanValue()) {
            	vectorThreads.addElement(currentThread2);
              }
        }
        if(vectorThreads.isEmpty() == true)
        {
        	return null;
        }
        return vectorThreads;
    }
    
    public static void solveDeadLock(Vector<ThreadCB> vecThread)		//REVIEW ALREADY DONE
    {
    	int count = 0;
    	RRB currentRRB = null;
    	
    	while(count < vecThread.size())
    	{
    		ThreadCB currentThread = (ThreadCB)vecThread.get(count);
    		if(deadLockDetectionAlgorith() == null)
    		{
    			break;
    		}
    		currentThread.kill();
    	}
    	
    	currentRRB = findGranted();
    	while(currentRRB != null)
    	{
    		currentRRB.grant();
    		HashT.put(currentRRB.getThread(), resource);
    		currentRRB = findGranted();
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
    	
    	Vector<ThreadCB> localVector = new Vector<ThreadCB>();
    	Enumeration<ThreadCB> localEnumeration = HashT.keys();
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
    public static void do_giveupResources(ThreadCB thread)		//REVIEW ALREADY DONE
    {
    	int counter = 0;

        while(counter < resourceCounter)
        {
        	ResourceCB currentResource = ResourceTable.getResourceCB(counter);
        	
        	if(currentResource.getAllocated(thread) != 0)
        	{ 
        		currentResource.setAvailable(currentResource.getAvailable() + currentResource.getAllocated(thread));     
        	}
        	currentResource.setAllocated(thread, 0);    
        	counter++;
        }
        
        HashT.remove(thread);
        
        RRB newRRB = findGranted();
        while(newRRB != null)
        {
        	if(newRRB.getThread().getStatus() != GlobalVariables.ThreadKill)
        	{
        		if(newRRB.getThread() != thread)
        		{
        			newRRB.grant();     
        		}	
        	}
    		HashT.put(newRRB.getThread(), resource);
    		newRRB = findGranted();
        }
        

    }

    /**
        Release a previously acquired resource.

	@param quantity

        @OSPProject Resources
    */
    public void do_release(int quantity)  	//REVIEW ALREADY DONE
    {
        TaskCB currentTask = MMU.getPTBR().getTask();
        ThreadCB currentThread = currentTask.getCurrentThread();
        int counter = getAllocated(currentThread);
     
        setAllocated(currentThread, counter - quantity);
        setAvailable(getAvailable() + quantity);
        
        RRB newRRB = findGranted();
        while (newRRB != null){		// find if any suspended requests can be granted 
        	if(newRRB.getThread().getStatus() == GlobalVariables.Suspended){
        		newRRB.grant();         
        	}

    		HashT.put(newRRB.getThread(), resource);
    		newRRB = findGranted();
        }
    }
    
    public static RRB findGranted()		//we can change the data structure to list with the hash values
    {
    	Collection<RRB> currentCollection = HashT.values();
    	Iterator<RRB> currentIterator = currentCollection.iterator();
    	
    	while(currentIterator.hasNext() == true)
    	{
    		RRB currentRRB = (RRB)currentIterator.next();
    		if(currentRRB.getThread() == null)
    		{
    			continue;
    		}
    		
    		if(getDeadlockMethod() == GlobalVariables.Avoidance)
			{
    			if(IsGranted(currentRRB))
    			{
    				currentRRB.setStatus(GlobalVariables.Granted);
    				return currentRRB;
    			}
			}
    		if(getDeadlockMethod() == GlobalVariables.Detection)
    		{
    			if(currentRRB.getQuantity() <= currentRRB.getResource().getAvailable())
    			{
    				return currentRRB;
    			}
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
