package osp.Devices;
import java.util.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.FileSys.*;

/**
    The disk interrupt handler.  When a disk I/O interrupt occurs,
    this class is called upon the handle the interrupt.

    @OSPProject Devices
*/
public class DiskInterruptHandler extends IflDiskInterruptHandler
{
    /** 
        Handles disk interrupts. 
        
        This method obtains the interrupt parameters from the 
        interrupt vector. The parameters are IORB that caused the 
        interrupt: (IORB)InterruptVector.getEvent(), 
        and thread that initiated the I/O operation: 
        InterruptVector.getThread().
        The IORB object contains references to the memory page 
        and open file object that participated in the I/O.
        
        The method must unlock the page, set its IORB field to null,
        and decrement the file's IORB count.
        
        The method must set the frame as dirty if it was memory write 
        (but not, if it was a swap-in, check whether the device was 
        SwapDevice)

        As the last thing, all threads that were waiting for this 
        event to finish, must be resumed.

        @OSPProject Devices 
    */
    public void do_handleInterrupt()
    {
    	 IORB currentIORB1 = (IORB)InterruptVector.getEvent();
    	 ThreadCB currentThreadCB = InterruptVector.getThread();
    	 TaskCB currentTaskCB = currentThreadCB.getTask();
    	 PageTableEntry currentPTEntry = currentIORB1.getPage();
    	 
    	 FrameTableEntry currentFTEntry = currentPTEntry.getFrame(); /*i am not sure*/
    	 Object currentObject = null;

/*
 * Method not completed
 */
    	 
    	 
    	 
    	 if (currentThreadCB.getStatus() != GlobalVariables.ThreadKill) 
    	 {
    		 if(currentFTEntry == null)
    	     {
    			 return;
    		 }
    	 
    		 if(currentIORB1.getDeviceID() != GlobalVariables.SwapDeviceID)
    		 {
    			 currentFTEntry.setReferenced(true);
    		 }
    	 }

    	
    	 
    	 if ((currentIORB1.getDeviceID() != GlobalVariables.SwapDeviceID) && (currentIORB1.getIOType() == GlobalVariables.FileRead) && (currentTaskCB.getStatus() != GlobalVariables.TaskTerm)) 
    	 {
    	      currentFTEntry.setDirty(true); 
    	 }

    	 if ((currentIORB1.getDeviceID() == GlobalVariables.SwapDeviceID) && (currentThreadCB.getTask().getStatus() != GlobalVariables.TaskTerm)) 
    	 {
    	      currentFTEntry.setDirty(false); 
    	 }
    	 
    	 if (currentTaskCB.getStatus() == GlobalVariables.TaskTerm) { 
    	      try
    	      {
    	        if (currentFTEntry.getReserved() == currentTaskCB)
    	        {
    	          currentFTEntry.setUnreserved(currentTaskCB);
    	        }
    	      }
    	      catch (NullPointerException localNullPointerException)
    	      {
    	      }

    	    }
    	 
    	 currentIORB1.notifyThreads();

    	 IORB currentIORB2 = Device.get(currentIORB1.getDeviceID()).dequeueIORB(); 
    	 Device.get(currentIORB1.getDeviceID()).setBusy(false); 
    	 
    	 if (currentIORB2 != null) 
    	 {
    	      Device.get(currentIORB1.getDeviceID()).startIO(currentIORB2); 
    	 }
    	 ThreadCB.dispatch();

    	  }
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

/*
      Feel free to add local classes to improve the readability of your code
*/
