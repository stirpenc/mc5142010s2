package osp.Threads;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Enumeration;

import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
	private ArrayList<ThreadCB> listThreads;
    /**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    public ThreadCB()
    {
    	
    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
        // your code goes here

    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject Threads
    */
    public ThreadCB do_create(TaskCB task)
    {

        if (task == null) {
          dispatch();
          return null;
        }

        if (task.getThreadCount() >= MaxThreadsPerTask)
        {
          if (h.J.ac()) {
            System.out.println("Ignoring MaxThreadsPerTask. FAILURE expected, returns SUCCESS");

            return new ThreadCB();
          }

          System.out.printf("osp.Threads.ThreadCB", "Failed to create new thread  -- maximum number of threads for " + task + " reached");

          dispatch();
          return null;
        }

        ThreadCB localThreadCB = new ThreadCB();
        System.out.printf("osp.Threads.ThreadCB", "Created " + localThreadCB);

        localThreadCB.q(task.getPriority());
        localThreadCB.p(20);

        if (h.K.ac()) {
          h.K.jdMethod_case("Not setting task of the new thread. Returning SUCCESS immediately");

          return localThreadCB;
        }

        localThreadCB.do_create(task);

        if (task.jdMethod_else(localThreadCB) != 100) {
         System.out.printf("osp.Threads.ThreadCB", "Could not add thread " + localThreadCB + " to task " + task);

          dispatch();
          return null;
        }

        listThreads.append(localThreadCB);

        System.out.printf("osp.Threads.ThreadCB", "Successfully added " + localThreadCB + " to " + task);

        if (h.at.ac()) {
          h.at.jdMethod_case("Exiting without dispatching a thread");

          return localThreadCB;
        }

        dispatch();

        if (h.aY.ac()) {
          h.aY.jdMethod_case("SUCCESS expected, returning FAILURE");

          return null;
        }

        return localThreadCB;
      }

      public void dD()
      {
        System.out.printf(this, "Entering do_kill(" + this + ")");

        TaskCB localTaskCB = c9();

        switch (dn())
        {
        case 20:
          if (listThreads.remove(this) != null) break;
          aa.jdMethod_goto(this, "Could not delete thread " + this + " from ready queue");

          return;
        case 21:
          if (this == MMU.aN().b4().getCurrentThread()) {
            MMU.aN().b4().setCurrentThread(null);
          } else {
            aa.jdMethod_goto(this, "The running thread != the current thread?");
            return;
          }

        default:
          if (dn() >= 30) break;
          aa.jdMethod_goto(this, "In do_kill(" + this + "): Thread is not running, waiting, or ready?");

          return;
        }

        if (localTaskCB.jdMethod_goto(this) != 100) {
          System.out.printf(this, "Could not remove thread " + this + " from task " + localTaskCB);

          return;
        }

        System.out.printf(this, this + " is set to be destroyed");

        if (h.F.ac()) {
          h.F.jdMethod_case("Neglected to change status of killed thread");
        }
        else
        {
          p(22);
        }

        for (int i = 0; i < b.dY(); i++) {
          System.out.printf(this, "Purging IORBs on Device " + i);
          b.v(i).e(this);
        }

        osp.c.c.jdMethod_int(this);

        dispatch();

        if (c9().getThreadCount() == 0) {
          System.out.printf(this, "After destroying " + this + ": " + c9() + " has no threads left; destroying the task");

          if (h.A.ac()) {
            h.A.jdMethod_case("Task has no threads, but returns without killing the task");

            return;
          }

          c9().kill();
        }
      }

    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
        // your code goes here

    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {
        // your code goes here

    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
        // your code goes here

    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    public static int do_dispatch()
    {
    	 ThreadCB localThreadCB1 = null;
    	    ThreadCB localThreadCB2 = null;
    	    TaskCB localTaskCB = null;
    	    try {
    	      localTaskCB = MMU.aN().b4();
    	      localThreadCB2 = localTaskCB.getCurrentThread();
    	    }
    	    catch (NullPointerException localNullPointerException)
    	    {
    	    }
    	    if (localThreadCB2 != null) {
    	      System.out.printf("osp.Threads.ThreadCB", "Preempting currently running " + localThreadCB2);

    	      localTaskCB.setCurrentThread(null);

    	      MMU.getExceptionStack(null); //????????

    	      localThreadCB2.p(20);
    	      listThreads.append(localThreadCB2);
    	    }

    	    localThreadCB1 = (ThreadCB)listThreads.removeHead();
    	    if (localThreadCB1 == null) {
    	      aa.jdMethod_else("osp.Threads.ThreadCB", "Can't find suitable thread to dispatch");

    	      MMU.a(null);
    	      return 101;
    	    }

    	    if (h.h.ac()) {
    	      h.h.jdMethod_case("Should have set PTBR, but didn't");
    	    }
    	    else
    	    {
    	      MMU.a(localThreadCB1.c9().getPageTable());
    	    }

    	    localThreadCB1.c9().setCurrentThread(localThreadCB1);

    	    if (h.g.ac()) {
    	      h.g.jdMethod_case("Should have set the status of thread " + localThreadCB1 + " to running, but didn't");
    	    }
    	    else
    	    {
    	      localThreadCB1.p(21);
    	    }

    	    aa.jdMethod_else("osp.Threads.ThreadCB", "Dispatching " + localThreadCB1);

    	    if (h.t.ac()) {
    	      h.t.jdMethod_case("Returning FAILURE when SUCCESS is expected");

    	      MMU.a(null);
    	      return 101;
    	    }

    	    HTimer.a(150);

    	    return 100;
    	  }


    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
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


/* TEXT FROM JAVADECOMPILER
public class ThreadCB extends a0
{
  private static GenericList fc;

  public static void dC()
  {
    fc = new GenericList();
  }

  public static ThreadCB jdMethod_new(TaskCB paramTaskCB)
  {
    if (paramTaskCB == null) {
      dispatch();
      return null;
    }

    if (paramTaskCB.getThreadCount() >= MaxThreadsPerTask)
    {
      if (h.J.ac()) {
        h.J.jdMethod_case("Ignoring MaxThreadsPerTask. FAILURE expected, returns SUCCESS");

        return new ThreadCB();
      }

      aa.jdMethod_else("osp.Threads.ThreadCB", "Failed to create new thread  -- maximum number of threads for " + paramTaskCB + " reached");

      dispatch();
      return null;
    }

    ThreadCB localThreadCB = new ThreadCB();
    aa.jdMethod_else("osp.Threads.ThreadCB", "Created " + localThreadCB);

    localThreadCB.q(paramTaskCB.getPriority());
    localThreadCB.p(20);

    if (h.K.ac()) {
      h.K.jdMethod_case("Not setting task of the new thread. Returning SUCCESS immediately");

      return localThreadCB;
    }

    localThreadCB.jdMethod_int(paramTaskCB);

    if (paramTaskCB.jdMethod_else(localThreadCB) != 100) {
      aa.jdMethod_else("osp.Threads.ThreadCB", "Could not add thread " + localThreadCB + " to task " + paramTaskCB);

      dispatch();
      return null;
    }

    fc.append(localThreadCB);

    aa.jdMethod_else("osp.Threads.ThreadCB", "Successfully added " + localThreadCB + " to " + paramTaskCB);

    if (h.at.ac()) {
      h.at.jdMethod_case("Exiting without dispatching a thread");

      return localThreadCB;
    }

    dispatch();

    if (h.aY.ac()) {
      h.aY.jdMethod_case("SUCCESS expected, returning FAILURE");

      return null;
    }

    return localThreadCB;
  }

  public void dD()
  {
    aa.jdMethod_else(this, "Entering do_kill(" + this + ")");

    TaskCB localTaskCB = c9();

    switch (dn())
    {
    case 20:
      if (fc.remove(this) != null) break;
      aa.jdMethod_goto(this, "Could not delete thread " + this + " from ready queue");

      return;
    case 21:
      if (this == MMU.aN().b4().getCurrentThread()) {
        MMU.aN().b4().setCurrentThread(null);
      } else {
        aa.jdMethod_goto(this, "The running thread != the current thread?");
        return;
      }

    default:
      if (dn() >= 30) break;
      aa.jdMethod_goto(this, "In do_kill(" + this + "): Thread is not running, waiting, or ready?");

      return;
    }

    if (localTaskCB.jdMethod_goto(this) != 100) {
      aa.jdMethod_else(this, "Could not remove thread " + this + " from task " + localTaskCB);

      return;
    }

    aa.jdMethod_else(this, this + " is set to be destroyed");

    if (h.F.ac()) {
      h.F.jdMethod_case("Neglected to change status of killed thread");
    }
    else
    {
      p(22);
    }

    for (int i = 0; i < b.dY(); i++) {
      aa.jdMethod_else(this, "Purging IORBs on Device " + i);
      b.v(i).e(this);
    }

    osp.c.c.jdMethod_int(this);

    dispatch();

    if (c9().getThreadCount() == 0) {
      aa.jdMethod_else(this, "After destroying " + this + ": " + c9() + " has no threads left; destroying the task");

      if (h.A.ac()) {
        h.A.jdMethod_case("Task has no threads, but returns without killing the task");

        return;
      }

      c9().kill();
    }
  }

  public void jdMethod_if(aw paramaw)
  {
    int i = dn();
    aa.jdMethod_else(this, "Entering suspend(" + this + "," + paramaw + ")");

    ThreadCB localThreadCB = null;
    TaskCB localTaskCB = null;
    try {
      localTaskCB = MMU.aN().b4();
      localThreadCB = localTaskCB.getCurrentThread();
    }
    catch (NullPointerException localNullPointerException)
    {
    }

    if (h.aP.ac()) {
      h.aP.jdMethod_case("Forgot to set current thread of task " + localTaskCB + " to null");
    }
    else if (this == localThreadCB) {
      c9().setCurrentThread(null);
    }

    if (h.aj.ac()) {
      h.aj.jdMethod_case("Forgot to set thread status and event");
    }
    else
    {
      if (dn() == 21)
        p(30);
      else if (dn() >= 30) {
        p(dn() + 1);
      }
      fc.remove(this);
      paramaw.jdMethod_null(this);
    }

    if (h.o.ac()) {
      h.o.jdMethod_case("Forgot to dispatch after suspend");

      return;
    }

    dispatch();
  }

  public void dz()
  {
    if (dn() < 30) {
      aa.jdMethod_else(this, "Attempt to resume " + this + ", which wasn't waiting");

      return;
    }

    aa.jdMethod_else(this, "Resuming " + this);

    if (dn() == 30)
      p(20);
    else if (dn() > 30) {
      p(dn() - 1);
    }

    if (h.p.ac()) {
      h.p.jdMethod_case("Didn't put resumed thread " + this + " on the ready queue");

      return;
    }

    if (dn() == 20) {
      fc.append(this);
    }

    if (h.B.ac()) {
      h.B.jdMethod_case("Returning without dispatching");

      return;
    }

    dispatch();
  }

  public static int dA()
  {
    ThreadCB localThreadCB1 = null;
    ThreadCB localThreadCB2 = null;
    TaskCB localTaskCB = null;
    try {
      localTaskCB = MMU.aN().b4();
      localThreadCB2 = localTaskCB.getCurrentThread();
    }
    catch (NullPointerException localNullPointerException)
    {
    }
    if (localThreadCB2 != null) {
      aa.jdMethod_else("osp.Threads.ThreadCB", "Preempting currently running " + localThreadCB2);

      localTaskCB.setCurrentThread(null);

      MMU.a(null);

      localThreadCB2.p(20);
      fc.append(localThreadCB2);
    }

    localThreadCB1 = (ThreadCB)fc.removeHead();
    if (localThreadCB1 == null) {
      aa.jdMethod_else("osp.Threads.ThreadCB", "Can't find suitable thread to dispatch");

      MMU.a(null);
      return 101;
    }

    if (h.h.ac()) {
      h.h.jdMethod_case("Should have set PTBR, but didn't");
    }
    else
    {
      MMU.a(localThreadCB1.c9().getPageTable());
    }

    localThreadCB1.c9().setCurrentThread(localThreadCB1);

    if (h.g.ac()) {
      h.g.jdMethod_case("Should have set the status of thread " + localThreadCB1 + " to running, but didn't");
    }
    else
    {
      localThreadCB1.p(21);
    }

    aa.jdMethod_else("osp.Threads.ThreadCB", "Dispatching " + localThreadCB1);

    if (h.t.ac()) {
      h.t.jdMethod_case("Returning FAILURE when SUCCESS is expected");

      MMU.a(null);
      return 101;
    }

    HTimer.a(150);

    return 100;
  }

  public static void dy()
  {
  }

  public static void dB()
  {
  }
}
*/