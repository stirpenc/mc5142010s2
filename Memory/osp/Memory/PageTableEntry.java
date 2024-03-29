package osp.Memory;

import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.
   
   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{
	boolean pageFaulted = false;
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);
	   
       as its first statement.

       @OSPProject Memory
    */
		
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
        super(ownerPageTable, pageNumber);
    }

    /**
       This method increases the lock count on the page by one. 

	The method must FIRST increment lockCount, THEN  
	check if the page is valid, and if it is not and no 
	page validation event is present for the page, start page fault 
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the 
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
    {
    	ThreadCB thread = iorb.getThread();
    	
    	
    	//if the page is not valid
    	if(!isValid())
	    {
    		//if the thread is null means that it is not in memory
	    	if(getValidatingThread() == null)
	    	{
	    		PageFaultHandler.handlePageFault(thread, GlobalVariables.MemoryLock, this);
	    	}
	    	else
	    	{
	    		//verify if the thread has caused a pagefault
	    		if(getValidatingThread() != thread)
	            {
	    			//suspend the thread if it not caused a pagefault
	            	thread.suspend(this);
	            	//failure if the thread was killed while suspended
	            	if(thread.getStatus() == GlobalVariables.ThreadKill)
	                {
	                	return GlobalVariables.FAILURE;
	                }
	            }
	    	}
    	}
        
    	//if not returned an error before, increment the count
    	getFrame().incrementLockCount();
    	return GlobalVariables.SUCCESS;
    	
    }

    /** This method decreases the lock count on the page by one. 

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {
    	//Just decrement the lock count
        getFrame().decrementLockCount();
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
