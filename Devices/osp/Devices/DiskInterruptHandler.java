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
    	 
    	 FrameTableEntry currentFTEntry = currentPTEntry./*insert method*/;
    	 Object currentObject = null;

    	 a locala = currentIORB1.b4();  /*implement class*/
    	    locala.cT(); /*change method*/
    	    if ((locala.cN() == 0) && (locala.eX)) {  /*change method*/
    	      locala.cS(); /*change method*/
    	    }
    	 
    	 
    	 
    	 if (currentThreadCB.getStatus() != GlobalVariable.ThreadKill) 
    	 {
    		 if(currentFTEntry == null)
    	     {
    			 return;
    		 }
    	 
    		 if(currentIORB1.getDeviceID() != 0)   /*change global variable*/
    		 {
    			 localFTEntry.jdMethod_int(true); /*change method*/
    		 }
    	 }

    	
    	 
    	 if ((currentIORB1.getDeviceID() != 0) && (currentIORB1.getIOType() == FileRead) && (currentTaskCB.getStatus() != 1))  /*change variable 0 e 1*/
    	 {
    	      currentFTEntry.jdMethod_for(true); /*change method*/
    	 }

    	 if ((currentIORB1.getDeviceID() == 0) && (currentThreadCB.getTask().getStatus() != 1))
    	 {
    	      currentFTEntry.jdMethod_for(false); /*change method*/
    	 }
    	 
    	 if (currentTaskCB.getStatus() == 1) { /*change global variable*/
    	      try
    	      {
    	        if (currentFTEntry.ao() == localTaskCB) /*change method*/
    	        {
    	          currentFTEntry.a(localTaskCB); /*change method*/
    	        }
    	      }
    	      catch (NullPointerException localNullPointerException)
    	      {
    	      }

    	    }
    	 
    	 currentIORB1.notifyThreads();

    	 IORB currentIORB2 = Device.get(currentIORB1.getDeviceID()).c7(); /*change method*/
    	 Device.get(currentIORB1.getDeviceID()).jdMethod_char(false); /*change method*/
    	 
    	 if (localIORB2 != null) 
    	 {
    	      Device.get(currentIORB1.getDeviceID()).jdMethod_new(localIORB2); /*change method*/
    	 }
    	 ThreadCB.dispatch();

    	  }
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
