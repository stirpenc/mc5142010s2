package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.IFLModules.*;

public class PageTable extends IflPageTable
{
    /** 
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
    	super(ownerTask);
    	//calculate the number os passible pages
    	int numberOfPages = (int)Math.pow(2, MMU.getPageAddressBits());
    	
    	//Instantiate an array of PageTableEntry with the size
    	pages = new PageTableEntry[numberOfPages];
    	
    	//Instantiate all new pagetableentry in the array
    	for(int i = 0; i < numberOfPages; i++)
    	{
    		pages[i] = new PageTableEntry(this, i);
    	}
    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
        TaskCB task = getTask();
        
        //We need to cleanup all Frames
        for(int i = 0; i < MMU.getFrameTableSize(); i++)
        {
        	//Gets a reference to the current page
        	FrameTableEntry tempFrameTableEntry = MMU.getFrame(i);
        	PageTableEntry tempPageTableEntry = tempFrameTableEntry.getPage();
        	if(tempPageTableEntry.getTask() == task)
        	{
        		//Makes the page null, the dirty and referenced false and unreserve 
        		tempFrameTableEntry.setPage(null);
        		tempFrameTableEntry.setDirty(false);
        		tempFrameTableEntry.setReferenced(false);
        		if(tempFrameTableEntry.getReserved() != task)
        			tempFrameTableEntry.setUnreserved(task);
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
