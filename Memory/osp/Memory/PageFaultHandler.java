package osp.Memory;
//import java.util.*;
//import osp.Hardware.*;
//import javax.swing.text.Utilities;

//import com.sun.org.apache.bcel.internal.generic.SWAP;

import osp.Threads.*;
import osp.Tasks.*;
//import osp.FileSys.FileSys;
//import osp.FileSys.OpenFile;
import osp.IFLModules.*;
//import osp.Interrupts.*;
import osp.Utilities.*;
//import osp.IFLModules.*;
//import sun.rmi.runtime.NewThreadAction;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {
 	    	
    	TaskCB newTask = page.getTask();
    	//Check if no pagefault happen
    	if(page.isValid())
    	{
    		//If the page is valid the method was incorrect called, dispatch, and then return failure
    		page.notifyThreads();
    		ThreadCB.dispatch();
    		return FAILURE;
    	}
    	
    	//Trigger and pagefault event and suspend the thread
    	Event event = new SystemEvent("PageFault");
    	thread.suspend(event);
    	
    	
    	FrameTableEntry newFrame = null;
    	
    	//Try to allocate a frame
    	newFrame = GetNewFrame();
    	
    	if(newFrame == null)//Could not allocate a new frame because the system has no menory.
    	{
    		page.notifyThreads();
    		event.notifyThreads();
    		ThreadCB.dispatch();
    		return NotEnoughMemory;
    	}
    	
    	page.setValidatingThread(thread);
    	newFrame.setReserved(thread.getTask());
    	//if the frame has a page
    	if(newFrame.getPage() != null)
    	{
    		PageTableEntry newPage = newFrame.getPage();
    		//verify is the new frame is dirty
    		if(newFrame.isDirty())
    		{
    			//if it is dirty we must swapout the frame
    			SwapOut(thread, newFrame);
    			
    			//we need to verify if the thread was killed while we are waiting
    			if(thread.getStatus() == GlobalVariables.ThreadKill)
    			{
    				page.notifyThreads();
    				event.notifyThreads();
    				ThreadCB.dispatch();
    				//don't set the dirty bit because we don't know the state
    				return FAILURE;
    			}
    			//after that we make the dirty false
    			newFrame.setDirty(false);
    		}
    		//otherwise we just set the frame as cleaned
    		newFrame.setReferenced(false);
    		newPage.setValid(false);
    		newPage.setFrame(null);
    		
    	}
    	
    	//we need to check again if the thread was not killed before we get a free frame
    	if(thread.getStatus() == ThreadKill)
    	{
    		//if it was killed, the leave the page clean and dispatch
    		page.notifyThreads();
    		page.setValidatingThread(null);
    		page.setFrame(null);
    		event.notifyThreads();
    		ThreadCB.dispatch();
    		return FAILURE;
    	}
    	
    	//set the frame in the page
    	page.setFrame(newFrame);
    	//swap in the contents to the page
    	SwapIn(thread, page);
    	
    	//again we must verify if the thread was not killed before we find a page
    	if(thread.getStatus() == ThreadKill)
    	{
    		//if the page obtained is not null
    		if(newFrame.getPage() != null)
    		{
    			//and the task associated is the one that caused the page fault we need to free this frame from this task because it was killed
    			if(newFrame.getPage().getTask() == thread.getTask())
    			{
	    			newFrame.setPage(null);
	    		}
    		}
    		page.notifyThreads();
    		page.setValidatingThread(null);
    		page.setFrame(null);
    		event.notifyThreads();
    		ThreadCB.dispatch();
    		return FAILURE;
    	}
    	
    	//if all goes right, we can set the page as valid
    	newFrame.setPage(page);
    	page.setValid(true);
    	
    	//and then prepare the thread to execute again
    	PrepareThread(thread);
    	
    	//again the thread can be killed while waiting
    	if(thread.getStatus() == ThreadKill)
    	{
    		event.notifyThreads();
    		//here is important to check if the entire task was terminated
    		if(thread.getTask().getStatus() != TaskTerm)
    		{
    			//if it was, we null the page because it can be used by other process
    			newFrame.setPage(null);
    			newFrame.setReferenced(false);
    		}
    		//set the page invalid and the frame null
    		page.setValid(false);
    		page.setFrame(null);
    		ThreadCB.dispatch();
    		return FAILURE;
    	}
    	
    	//unreserve the frame if it is reserved for the task, because the task already has the frame
    	if(newFrame.getReserved() == newTask)
    	{
    		newFrame.setUnreserved(newTask);    		
    	}
    	
    	//remove the reference for thread that caused pagefault
    	page.setValidatingThread(null);
    	page.notifyThreads();
    	//notify all the threads
    	event.notifyThreads();
    	ThreadCB.dispatch();
    	return SUCCESS;
    }
    
    //get a new frame and return this frame
    //TODO refactor all this function in one loop
    private static FrameTableEntry GetNewFrame()
    {
    	FrameTableEntry newFrame = null;
    	//search in the table for the first free frame
    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((newFrame.getPage() == null) && (!newFrame.isReserved()) && (newFrame.getLockCount() == 0))
    		{
    			return newFrame; //Frame OK.
    		}
    	}
    	
    	//no page free page found, now we will try to search a page that is not dirty and not reserved
    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((!newFrame.isDirty()) && (!newFrame.isReserved()) && (newFrame.getLockCount() == 0))
    		{
    			return newFrame; //Frame clean. OK!
    		}
    	}
    	
    	//no good frame was found, so we try to find one that is not reseverd or locked
    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((!newFrame.isReserved()) && (newFrame.getLockCount() == 0))
    		{
    			return newFrame; 
    		}
    	}
    	//if really nothing is found, return the first frame
    	return MMU.getFrame(MMU.getFrameTableSize() - 1);
    }
    
    //TODO remove from function
    private static PageTableEntry AllocatePage(PageTable page)
    {
    	//calculate the size
    	 int i = (int)Math.pow(2.0D, MMU.getPageAddressBits());
    	 //search for a not locked page
    	 for (int j = 0; j < i; j++) {
    	      PageTableEntry newPage = page.pages[j];
    	      if ((!newPage.isReserved()) && (
    	        (!newPage.isValid()) || (newPage.getFrame().getLockCount()  == 0)))
    	      {
    	        return newPage;
    	      }
    	 }
    	 return null;
    }
    
    public static void PrepareThread(ThreadCB thread)
    {
    	TaskCB newTask = thread.getTask();
    	
    	//Allocate an page
    	PageTableEntry newPage = AllocatePage(newTask.getPageTable());
    	//if the page is null, or is valid, or never caused a pagefault the thread is ready
    	if((newPage == null) || (newPage.isValid()) || (newPage.getValidatingThread() != null))
    	{
    		return;
    	}
    	
    	//Else, allocate a newframe
    	FrameTableEntry newFrame = GetNewFrame();
    	//if null we cant do nothing
    	if(newFrame == null)
    		return;
    	
    	//setthe thread as who caused the pagefault
    	newPage.setValidatingThread(thread);
    	newPage.pageFaulted = true;
    	//set the frame reserved for the thread
    	newFrame.setReserved(thread.getTask());
    	
    	//if the frame already has a page
    	if(newFrame.getPage() != null)
    	{
    		//backup the frame old page
    		PageTableEntry newPage2 = newFrame.getPage();
    		if(newFrame.isDirty())
    		{
    			//swapout if it is dirty
    			SwapOut(thread, newFrame);
    			//cancel all if the calling thread is killed while waiting
    			if(thread.getStatus() == ThreadKill)
    			{
    				newPage.notifyThreads();
    				newPage.setValidatingThread(null);
    				newPage.pageFaulted = false;
    				return;
    			}
    			//set the frame clean after Swapout
    			newFrame.setDirty(false);
    		}
    		//set all frame properties as clean and free the old page
    		newFrame.setReferenced(false);
    		newPage2.setValid(false);
    		newPage2.setFrame(null);
    		newFrame.setPage(null);
    	}
    	//Associate the new frame with the new page and swapin the data
    	newPage.setFrame(newFrame);
    	
    	SwapIn(thread, newPage);
    	
    	//if the thread is killed while waiting clean the frame and the page
    	if(thread.getStatus() == ThreadKill)
    	{
    		//if the task is killed unreserve all frame
    		if((newFrame.getPage() != null) && (newFrame.getPage().getTask() == thread.getTask()))
    		{
    			newFrame.setUnreserved(null);
    		}
    		//set the page as cleaned
    		newPage.setValidatingThread(null);
    		newPage.setFrame(null);
    		newPage.pageFaulted = false;
    		newPage.notifyThreads();
    		return;
    	}
    	//Just set the page valid and not dirty
    	newPage.setValid(true);
    	newFrame.setPage(newPage);
    	newFrame.setDirty(false);
    	
    	//unreser the frame if it is reserved for this page
    	if(newFrame.getReserved() == newPage.getTask())
    	{
    		newFrame.setUnreserved(newPage.getTask());
    	}
    	newPage.pageFaulted = false;
    	newPage.setValidatingThread(null);
    	newPage.notifyThreads();
    }
    
    public static void SwapIn(ThreadCB thread, PageTableEntry page)
    {
    	TaskCB newTask = page.getTask();
    	//read from the disk the process
    	newTask.getSwapFile().read(page.getID(), page, thread);
    }
    
    public static void SwapOut(ThreadCB thread, FrameTableEntry frame)
    {
    	PageTableEntry newPage = frame.getPage();
    	TaskCB newTask = newPage.getTask();
    	//Write the page in disc
    	newTask.getSwapFile().write(newPage.getID(), newPage, thread);
    }

	public static void init() {
		// TODO Auto-generated method stub
		//we don't used this, but the book asked for :-S
	}
	
	static void StarvationAvoid(ThreadCB thread)
	{
		FrameTableEntry frame = GetNewFrame();
		
		//if the frame is not null
		if(frame == null)
		{
			return;
		}
		else
		{
			//verify if the page is really used
			if(frame.getPage() != null)
			{
				TaskCB task = frame.getPage().getTask();
				
				task.getSwapFile().write(frame.getPage().getID(), frame.getPage(), thread);
				frame.setDirty(false);
			}
		}
	}
    
    
    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
