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
    		page.notifyThreads();
    		ThreadCB.dispatch();
    		return FAILURE;
    	}
    	
    	Event localEvent = new SystemEvent("PageFault");
    	thread.suspend(localEvent);
    	
    	FrameTableEntry newFrame = null;
    	
    	newFrame = AllocateNewFrame();
    	if(newFrame == null)//Could not allocate a new frame.
    	{
    		page.notifyThreads();
    		localEvent.notifyThreads();
    		ThreadCB.dispatch();
    		return NotEnoughMemory;
    	}
    	
    	page.setValidatingThread(thread);
    	
    	if(newFrame.getPage() != null)
    	{
    		PageTableEntry newPage = newFrame.getPage();
    		if(newFrame.isDirty())
    		{
    			SwapOut(newFrame, thread);
    			
    			if(thread.getStatus() == GlobalVariables.ThreadKill)
    			{
    				page.notifyThreads();
    				localEvent.notifyThreads();
    				ThreadCB.dispatch();
    				return FAILURE;
    			}
    			newFrame.setDirty(false);
    		}
    		newFrame.setReferenced(false);
    		newPage.setValid(false);
    		newPage.setFrame(null);
    		if(newPage.getHead().getStatus() != 1)
    		{
    			newFrame.setUnreserved(null);
    		}
    	}
    	
    	if(thread.getStatus() == ThreadKill)
    	{
    		page.notifyThreads();
    		page.setValidatingThread(null);
    		page.setFrame(null);
    		localEvent.notifyThreads();
    		ThreadCB.dispatch();
    		return FAILURE;
    	}
    	
    	page.setFrame(newFrame);
    	SwapIn(thread, page);
    	
    	if(thread.getStatus() == ThreadKill)
    	{
    		if((newFrame.getPage() == null) && (newFrame.getPage().getTask() == thread.getTask()))
    		{
    			newFrame.setUnreserved(null);
    		}
    		page.notifyThreads();
    		page.setValidatingThread(null);
    		page.setFrame(null);
    		localEvent.notifyThreads();
    		ThreadCB.dispatch();
    		return FAILURE;
    	}
    	
    	newFrame.setPage(page);
    	page.setValid(true);
    	PrepareThread(thread);
    	
    	if(thread.getStatus() == ThreadKill)
    	{
    		localEvent.notifyThreads();
    		if(thread.getTask().getStatus() != 1)
    		{
    			newFrame.setPage(null);
    			newFrame.setReferenced(false);
    		}
    		page.setValid(false);
    		page.setFrame(null);
    		ThreadCB.dispatch();
    		return FAILURE;
    	}
    	
    	if(newFrame.getReserved() == newTask)
    	{
    		newFrame.setUnreserved(newTask);    		
    	}
    	
    	page.setValidatingThread(null);
    	page.notifyThreads();
    	localEvent.notifyThreads();
    	ThreadCB.dispatch();
    	return SUCCESS;
    }
    
    /*public static void Init()
    {
    	Daemon.create("Daemon", Swa
    }*/

    private static FrameTableEntry AllocateNewFrame()
    {
    	FrameTableEntry newFrame = null;
    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((newFrame.getPage() == null) && (newFrame.isReserved() || newFrame.getLockCount() != 0))
    		{
    			return newFrame; //Frame Locked or Reserved... Should cause error in called method.
    		}
    		if((newFrame.getPage() == null) && (!newFrame.isReserved()) && (newFrame.getLockCount() == 0))
    		{
    			return newFrame; //Frame OK.
    		}
    	}
    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((!newFrame.isDirty()) && (!newFrame.isReserved()) && (newFrame.getLockCount() == 0))
    		{
    			return newFrame; //Frame clean. OK!
    		}
    	}
    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((!newFrame.isReserved()) && (newFrame.getLockCount() == 0))
    		{
    			return newFrame; 
    		}
    	}
    	return null;
    	
    }
    
    public static void SwapIn(ThreadCB thread, PageTableEntry page)
    {
    	TaskCB newTask = page.getTask();
    	newTask.getSwapFile().read(page.getID(), page, thread);
    	
    }
    
    public static void SwapOut(FrameTableEntry frame, ThreadCB thread)
    {
    	PageTableEntry newPage = frame.getPage();
    	TaskCB newTask = newPage.getTask();
    	newTask.getSwapFile().write(newPage.getID(), newPage, thread);
       	
    }
    
    public static void PrepareThread(ThreadCB thread)
    {
    	TaskCB newTask = thread.getTask();
    	PageTableEntry newPage = AllocatePage(false, newTask.getPageTable());
    	if((newPage == null) || (newPage.isValid()) || (newPage.getValidatingThread() != null))
    	{
    		return;
    	}
    	FrameTableEntry newFrame = AllocateNewFrame();
    	if(newFrame == null)
    		return;
    	newPage.setValidatingThread(thread);
    	newPage.pageFaulted = true;
    	newFrame.setReserved(thread.getTask());
    	if(newFrame.getPage() != null)
    	{
    		PageTableEntry newPage2 = newFrame.getPage();
    		if(newFrame.isDirty())
    		{
    			SwapOut(newFrame, thread);
    			if(thread.getStatus() == ThreadKill)
    			{
    				newPage.notifyThreads();
    				newPage.setValidatingThread(null);
    				newPage.pageFaulted = false;
    				return;
    			}
    			newFrame.setDirty(false);
    		}
    		newFrame.setReferenced(false);
    		newPage2.setValid(false);
    		newPage2.setFrame(null);
    		newFrame.setPage(null);
    	}
    	newPage.setFrame(newFrame);
    	SwapIn(thread, newPage);
    	
    	if(thread.getStatus() == ThreadKill)
    	{
    		if((newFrame.getPage() != null) && (newFrame.getPage().getTask() == thread.getTask()))
    		{
    			newFrame.setUnreserved(null);
    		}
    		newPage.setValidatingThread(null);
    		newPage.setFrame(null);
    		newPage.pageFaulted = false;
    		newPage.notifyThreads();
    		return;
    	}
    	newFrame.setPage(newPage);
    	newFrame.setDirty(false);
    	newPage.setValid(true);
    	if(newFrame.getReserved() == newPage.getTask())
    	{
    		newFrame.setUnreserved(newPage.getTask());
    	}
    	newPage.pageFaulted = false;
    	newPage.setValidatingThread(null);
    	newPage.notifyThreads();
    }
    
    /*private static FrameTableEntry FindFrame()
    {
    	FrameTableEntry newFrame = null;
    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((newFrame.getPage() == null) && (newFrame.getLockCount() == 0) && (!newFrame.isReserved()))
    		{
    			return newFrame;
    		}
    	}
    	return null;
    }*/
    
    private static PageTableEntry AllocatePage(boolean validade, PageTable page)
    {
    	 int i = (int)Math.pow(2.0D, MMU.getPageAddressBits());
    	 for (int j = 0; j < i; j++) {
    	      PageTableEntry newPage = page.pages[j];
    	      if ((!newPage.isReserved()) && 
    	        ((!validade) || (newPage.isValid())) && 
    	        ((validade) || (!newPage.isValid())) && (
    	        (!newPage.isValid()) || (newPage.getFrame().getLockCount()  == 0)))
    	      {
    	        return newPage;
    	      }
    	 }
    	 return null;
    }
    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
