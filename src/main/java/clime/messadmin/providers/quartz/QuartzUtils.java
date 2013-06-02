/**
 *
 */
package clime.messadmin.providers.quartz;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.SchedulerRepository;

/**
 * @author C&eacute;drik LIME
 */
final class QuartzUtils {

	// Quartz 1.7
	private static transient Class dateIntervalTriggerClass;
	private static transient Method dateIntervalTrigger_getRepeatInterval;
	private static transient Method dateIntervalTrigger_getRepeatIntervalUnit;

	static {
		// @since Quartz 1.7
		try {
			dateIntervalTriggerClass = Class.forName("org.quartz.DateIntervalTrigger");//$NON-NLS-1$
			dateIntervalTrigger_getRepeatInterval = dateIntervalTriggerClass.getMethod("getRepeatInterval");//$NON-NLS-1$
			dateIntervalTrigger_getRepeatIntervalUnit = dateIntervalTriggerClass.getMethod("getRepeatIntervalUnit");//$NON-NLS-1$
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
		Collection/*<SchedulerFactoryBean>*/ schedulerBeans = SpringQuartzUtils.getSchedulerFactoryBeans(context);
		if ( ! schedulerBeans.isEmpty()) {
			for (Object/*SchedulerFactoryBean*/ schedulerFactoryBean : schedulerBeans) {
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
		for (Scheduler scheduler : (Collection<Scheduler>) SchedulerRepository.getInstance().lookupAll()) {
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
		Class<? extends Job> jobClass = jobDetail.getJobClass();
		if (jobClass == null) {
			return false;
		}
		return (InterruptableJob.class.isAssignableFrom(jobClass));
	}


	/***********************************************************************/


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
			return (String) dateIntervalTrigger_getRepeatInterval.invoke(trigger);
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
			return dateIntervalTrigger_getRepeatIntervalUnit.invoke(trigger);
		} catch (Exception ignore) {
			return null;
		}
	}
}
