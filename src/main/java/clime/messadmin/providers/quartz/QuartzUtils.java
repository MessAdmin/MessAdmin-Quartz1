/**
 *
 */
package clime.messadmin.providers.quartz;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.quartz.InterruptableJob;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.SchedulerRepository;

import clime.messadmin.utils.Integers;

/**
 * @author C&eacute;drik LIME
 */
final class QuartzUtils {

	// Quartz 1.6
	private static transient Method scheduler_isStarted = null;
	private static transient Method trigger_getPriority = null;
	private static transient Method trigger_setPriority = null;
	public static transient int Trigger_DEFAULT_PRIORITY = 5;

	// Quartz 1.7
	private static transient Class dateIntervalTriggerClass;
	private static transient Method dateIntervalTrigger_getRepeatInterval;
	private static transient Method dateIntervalTrigger_getRepeatIntervalUnit;

	static {
		// @since Quartz 1.6
		try {
			scheduler_isStarted = Scheduler.class.getMethod("isStarted", null);//$NON-NLS-1$
			trigger_getPriority = Trigger.class.getMethod("getPriority", null);//$NON-NLS-1$
			trigger_setPriority = Trigger.class.getMethod("setPriority", new Class[] {Integer.TYPE});//$NON-NLS-1$
			Trigger_DEFAULT_PRIORITY = Trigger.class.getField("DEFAULT_PRIORITY").getInt(null);//$NON-NLS-1$
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (NoSuchFieldException e) {
		} catch (IllegalAccessException e) {
		}
		// @since Quartz 1.7
		try {
			dateIntervalTriggerClass = Class.forName("org.quartz.DateIntervalTrigger");//$NON-NLS-1$
			dateIntervalTrigger_getRepeatInterval = dateIntervalTriggerClass.getMethod("getRepeatInterval", null);//$NON-NLS-1$
			dateIntervalTrigger_getRepeatIntervalUnit = dateIntervalTriggerClass.getMethod("getRepeatIntervalUnit", null);//$NON-NLS-1$
		} catch (LinkageError e) {
		} catch (ClassNotFoundException e) {
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
	}


	private QuartzUtils() {
	}


	public static String getUniqueIdentifier(Scheduler scheduler) {
		try {
			return scheduler.getSchedulerName() + "_$_" + scheduler.getSchedulerInstanceId();
		} catch (SchedulerException e) {
			throw (IllegalStateException) new IllegalStateException().initCause(e);
		}
	}

	public static Scheduler getSchedulerWithUID(String uid, ServletContext context) {
		// Spring
		Collection schedulerBeans = SpringQuartzUtils.getSchedulerFactoryBeans(context);
		if ( ! schedulerBeans.isEmpty()) {
			for (Iterator/*<SchedulerFactoryBean>*/ it = schedulerBeans.iterator(); it.hasNext();) {
				Object/*SchedulerFactoryBean*/ schedulerFactoryBean = it.next();
				boolean schedulerBeanIsRunning = SpringQuartzUtils.isRunning(schedulerFactoryBean);//schedulerBean.isRunning()
//				if (schedulerBeanIsRunning) {// Spring 2.0
					Scheduler scheduler = SpringQuartzUtils.getScheduler(schedulerFactoryBean);// (Scheduler) schedulerBean.getObject();
					if (scheduler != null && uid.equals(QuartzUtils.getUniqueIdentifier(scheduler))) {
						return scheduler;
					}
//				}
			}
		}
		// Quartz
//		((SchedulerFactory) context.getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY)).getAllSchedulers()
//		==
//		SchedulerRepository.getInstance().lookupAll()
		for (Iterator/*<Scheduler>*/ it = SchedulerRepository.getInstance().lookupAll().iterator(); it.hasNext();) {
			Scheduler scheduler = (Scheduler) it.next();
			if (uid.equals(QuartzUtils.getUniqueIdentifier(scheduler))) {
				return scheduler;
			}
		}
		return null;
	}

	/**
	 * <p>
	 * Whether or not the <code>Job</code> implements the interface <code>{@link InterruptableJob}</code>.
	 * </p>
	 */
	public static boolean isInterruptable(JobDetail jobDetail) {
		Class jobClass = jobDetail.getJobClass();
		if (jobClass == null) {
			return false;
		}
		return (InterruptableJob.class.isAssignableFrom(jobClass));
	}


	/***********************************************************************/


	/**
	 * @since Quartz 1.6
	 */
	public static boolean isStarted(Scheduler scheduler) {
		boolean isStarted = true;// = scheduler.isStarted();
		if (scheduler_isStarted != null) {
			try {
				Object started = scheduler_isStarted.invoke(scheduler, null);
				isStarted = ((Boolean) started).booleanValue();
			} catch (Exception ignore) {
			}
		}
		return isStarted;
	}

	/**
	 * @since Quartz 1.6
	 */
	public static boolean hasTriggerPriority() {
		return trigger_getPriority != null && trigger_setPriority != null;
	}

	/**
	 * @since Quartz 1.6
	 */
	public static Integer getTriggerPriority(Trigger trigger) {
		Integer priority = null;// = trigger.getPriority()
		if (trigger_getPriority != null) {
			try {
				priority = (Integer) trigger_getPriority.invoke(trigger, null);
			} catch (Exception ignore) {
			}
		}
		return priority;
	}

	/**
	 * @since Quartz 1.6
	 */
	public static void setTriggerPriority(Trigger trigger, int priority) {
		//trigger.setPriority(priority)
		if (trigger_setPriority != null) {
			try {
				trigger_setPriority.invoke(trigger, new Object[] {Integers.valueOf(priority)});
			} catch (Exception ignore) {
			}
		}
	}

	/**
	 * @since Quartz 1.7
	 */
	public static boolean isDateIntervalTrigger(Trigger trigger) {
		return dateIntervalTriggerClass != null && dateIntervalTriggerClass.isInstance(trigger);
	}

	/**
	 * @since Quartz 1.7
	 */
	public static String getDateIntervalTrigger_RepeatInterval(Trigger trigger) {
		if (! isDateIntervalTrigger(trigger) || dateIntervalTrigger_getRepeatInterval == null) {
			throw new IllegalArgumentException(String.valueOf(trigger));
		}
		try {
			return (String) dateIntervalTrigger_getRepeatInterval.invoke(trigger, null);
		} catch (Exception ignore) {
			return null;
		}
	}

	/**
	 * @since Quartz 1.7
	 */
	public static Object getDateIntervalTrigger_RepeatIntervalUnit(Trigger trigger) {
		if (! isDateIntervalTrigger(trigger) || dateIntervalTrigger_getRepeatIntervalUnit == null) {
			throw new IllegalArgumentException(String.valueOf(trigger));
		}
		try {
			return dateIntervalTrigger_getRepeatIntervalUnit.invoke(trigger, null);
		} catch (Exception ignore) {
			return null;
		}
	}
}
