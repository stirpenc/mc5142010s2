/*
* Grupo 12
* RA: 071552
* RA: 080664
* RA: 084294
*
* Status: Concluded
*
* 09/04/10
* 1. Implementation of all functions less do_remove
* 09/07/10
* 2. Implementation of do_remove
* 
* Difficulties: the function MMU.getVirtualAddressBits() return the number of bits used in the representation,
* we took a lot of time to note that
* 
* Main Considerations:
* We used ArrayList instead Vector because Vector is deprecated for a long time.
* */

package osp.Tasks;

import java.util.ArrayList;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Ports.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**
    The student module dealing with the creation and killing of
    tasks.  A task acts primarily as a container for threads and as
    a holder of resources.  Execution is associated entirely with
    threads.  The primary methods that the student will implement
    are do_create(TaskCB) and do_kill(TaskCB).  The student can choose
    how to keep track of which threads are part of a task.  In this
    implementation, an array is used.

    @OSPProject Tasks
*/
public class TaskCB extends IflTaskCB
{
	//Arrays that will maintain the open files, open 
	private ArrayList<ThreadCB> listThreads;
	private ArrayList<PortCB> listPorts;
	private ArrayList<OpenFile> listFiles;
	
    /**
       The task constructor. Must have

       	   super();

       as its first statement.

       @OSPProject Tasks
    */
    public TaskCB()
    {
    	super();
    	//Instantiate Array lists for ports and Threads
    	listThreads = new ArrayList<ThreadCB>();
    	listPorts = new ArrayList<PortCB>();
    	listFiles = new ArrayList<OpenFile>();
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Tasks
    */
    public static void init()
    {

    }

    /** 
        Sets the properties of a new task, passed as an argument. 
        
        Creates a new thread list, sets TaskLive status and creation time,
        creates and opens the task's swap file of the size equal to the size
	(in bytes) of the addressable virtual memory.

	@return task or null

        @OSPProject Tasks
    */
    static public TaskCB do_create()
    {
    	//Pointer used to instantiate the taskCB that will be returned
        TaskCB newTask = new TaskCB();
        
        //Instantiate a new PageTable an associate it with the newTask
        newTask.setPageTable(new PageTable(newTask));
        
        //Defines priority
        newTask.setPriority(0);
        //Set Creation time
        newTask.setCreationTime(HClock.get());
        //Set Status
        newTask.setStatus(GlobalVariables.TaskLive);
        
        //Define Swap file
        //Creates the swap file
        //We need to use pow to calc 2^AddressBits because the function returns how many bits are used in representation
        FileSys.create(SwapDeviceMountPoint + newTask.getID(), (int)Math.pow(2.0D, MMU.getVirtualAddressBits()));
        //Open the swap file
        OpenFile swap = OpenFile.open(SwapDeviceMountPoint + newTask.getID(), newTask);	
        //if swap file fail when creating dispatch  thread (Asked in the book, but why do this?)
        if(swap == null)
        {
        	ThreadCB.dispatch();
        	return null;
        }
        newTask.setSwapFile(swap);
        
        //Creates the first thread
        ThreadCB.create(newTask);
        
    	return newTask;
    }

    /**
       Kills the specified task and all of it threads. 

       Sets the status TaskTerm, frees all memory frames 
       (reserved frames may not be unreserved, but must be marked 
       free), deletes the task's swap file.
	
       @OSPProject Tasks
    */
    public void do_kill()
    {
        //Kill all threads
    	while(listThreads.size() != 0)
    	{
    		listThreads.get(0).kill();
		}
    	//Destroy all ports
    	while(listPorts.size() != 0)
    	{
    		listPorts.get(0).destroy();
    	}
    	//Set the status as terminated
    	this.setStatus(GlobalVariables.TaskTerm);
    	//Delocate the memory
    	this.getPageTable().deallocateMemory();
    	//Close all files
    	int i;
    	for (i = listFiles.size() - 1; i >= 0; i--) //Here we need to interate from the last element because a file can have a delay to close 
    	{
    		//We must verify if that position in already null, in other words, if the file position is null means that the file is already closed
    		if (listFiles.get(i) != null) 
    	    {
    			listFiles.get(i).close();
    	    }
    	}
    	
    	//delete the swap file
    	FileSys.delete(SwapDeviceMountPoint + this.getID());
    }

    /** 
	Returns a count of the number of threads in this task. 
	
	@OSPProject Tasks
    */
    public int do_getThreadCount()
    {
        return listThreads.size();
    }

    /**
       Adds the specified thread to this task. 
       @return FAILURE, if the number of threads exceeds MaxThreadsPerTask;
       SUCCESS otherwise.
       
       @OSPProject Tasks
    */
    public int do_addThread(ThreadCB thread)
    {
    	//Verifies if can add another thread
    	if(listThreads.size() >= ThreadCB.MaxThreadsPerTask)
    	{
    		return GlobalVariables.FAILURE;
    	}
    	listThreads.add(thread);
        return GlobalVariables.SUCCESS;
    }

    /**
       Removes the specified thread from this task. 		

       @OSPProject Tasks
    */
    public int do_removeThread(ThreadCB thread)
    {
    	//Failure in empty list
    	if(listThreads.size() == 0)
    	{
    		return GlobalVariables.FAILURE;
    	}
    	//Try to find the Thread in the list
    	else if(listThreads.contains(thread))
    	{
    		listThreads.remove(thread);
    		return GlobalVariables.SUCCESS;
    	}
    	//Thread not found return Failure
    	else
    	{
    		return GlobalVariables.FAILURE;
    	}
    }

    /**
       Return number of ports currently owned by this task. 

       @OSPProject Tasks
    */
    public int do_getPortCount()
    {
    	return listPorts.size();
    }

    /**
       Add the port to the list of ports owned by this task.
	
       @OSPProject Tasks 
    */ 
    public int do_addPort(PortCB newPort)
    {
    	//Verifies if can add another Port
    	if(listPorts.size() >= PortCB.MaxPortsPerTask)
    	{
    		return GlobalVariables.FAILURE;
    	}
    	listPorts.add(newPort);
        return GlobalVariables.SUCCESS;
    }

    /**
       Remove the port from the list of ports owned by this task.

       @OSPProject Tasks 
    */ 
    public int do_removePort(PortCB oldPort)
    {
    	//Failure in empty list
    	if(listPorts.size() == 0)
    	{
    		return GlobalVariables.FAILURE;
    	}
    	//Try to find the Port in the list
    	else if(listPorts.contains(oldPort))
    	{
    		listPorts.remove(oldPort);
    		return GlobalVariables.SUCCESS;
    	}
    	//Port not found return Failure
    	else
    	{
    		return GlobalVariables.FAILURE;
    	}
    }

    /**
       Insert file into the open files table of the task.

       @OSPProject Tasks
    */
    public void do_addFile(OpenFile file)
    {
    	//Add the new file to list
    	listFiles.add(file);
    }

    /** 
	Remove file from the task's open files table.

	@OSPProject Tasks
    */
    public int do_removeFile(OpenFile file)
    {
    	//Failure in empty list
    	if(listFiles.size() == 0)
    	{
    		return GlobalVariables.FAILURE;
    	}
    	//Try to find the Port in the list
    	else if(listFiles.contains(file))
    	{
    		listFiles.remove(file);
    		return GlobalVariables.SUCCESS;
    	}
    	//Port not found return Failure
    	else
    	{
    		return GlobalVariables.FAILURE;
    	}
    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures
       in their state just after the error happened.  The body can be
       left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atError()
    {
        // your code goes here

    }

    /**
       Called by OSP after printing a warning message. The student
       can insert code here to print various tables and data
       structures in their state just after the warning happened.
       The body can be left empty, if this feature is not used.
       
       @OSPProject Tasks
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
