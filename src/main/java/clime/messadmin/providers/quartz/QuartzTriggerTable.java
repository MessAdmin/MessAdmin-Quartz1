/**
 *
 */
package clime.messadmin.providers.quartz;

import java.text.Format;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import clime.messadmin.admin.BaseAdminActionProvider;
import clime.messadmin.i18n.I18NSupport;
import clime.messadmin.providers.spi.DisplayProvider;
import clime.messadmin.utils.DateUtils;
import clime.messadmin.utils.FastDateFormat;
import clime.messadmin.utils.StringUtils;

/**
 * TODO: pause / resume trigger group
 * TODO: trigger priority
 *
 * @author C&eacute;drik LIME
 */
class QuartzTriggerTable extends AbstractQuartzTable {

	/**
	 *
	 */
	public QuartzTriggerTable(ServletContext servletContext, DisplayProvider displayProvider) {
		super(servletContext, displayProvider);
	}

	@Override
	protected String getTableCaption(Scheduler scheduler) {
		//FIXME add ajax links to: pauseAll / resumeAll
		try {
			return I18NSupport.getLocalizedMessage(BUNDLE_NAME, "caption.trigger", new Object[] {scheduler.getSchedulerName()});//$NON-NLS-1$
		} catch (SchedulerException e) {
			return e.toString();
		}
	}

	@Override
	public String[] getTabularDataLabels() {
		//TODO add "priority" column
		return new String[] {
				"", // empty label for actions: run, interrupt, etc.//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.trigger.group"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.trigger.name"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.trigger.state"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.trigger.volatile"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.trigger.job.group"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.trigger.job.name"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.trigger.previousFireTime"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.trigger.nextFireTime")//$NON-NLS-1
		};
	}

	@Override
	public Object[][] getTabularData(Scheduler scheduler) throws SchedulerException {
		final NumberFormat numberFormatter = NumberFormat.getNumberInstance(I18NSupport.getAdminLocale());
		final Format dateFormatter = FastDateFormat.getInstance(DateUtils.DEFAULT_DATE_TIME_FORMAT);
		List<Object> data = new LinkedList<Object>();
		List<JobExecutionContext> currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();
		String[] allTriggerGroupNames = scheduler.getTriggerGroupNames();
		Set<String> pausedTriggerGroups = scheduler.getPausedTriggerGroups();
		for (int g = 0; g < allTriggerGroupNames.length; ++g) {
			String triggerGroupName = allTriggerGroupNames[g];
			String[] triggerNames = scheduler.getTriggerNames(triggerGroupName);
			for (int t = 0; t < triggerNames.length; ++t) {
				String triggerName = triggerNames[t];
				Trigger trigger = scheduler.getTrigger(triggerName, triggerGroupName);
//				assert triggerName.equals(trigger.getName());
//				assert triggerGroupName.equals(trigger.getGroup());
				JobDetail jobDetail = scheduler.getJobDetail(trigger.getJobName(), trigger.getJobGroup());
				int triggerPriority = trigger.getPriority();//TODO add as column (with +/- action) if != Trigger.DEFAULT_PRIORITY
				int triggerState = scheduler.getTriggerState(triggerName, triggerGroupName);
				int triggerMisfireInstruction = trigger.getMisfireInstruction();// TODO add information on MisfireInstruction of trigger (use i18n trigger.MisfireInstruction.n?)
				data.add(new Object[] {
						buildActionLinks(scheduler, trigger, pausedTriggerGroups, jobDetail, currentlyExecutingJobs),
						// trigger group
						pausedTriggerGroups.contains(triggerGroupName)//TODO action links on: trigger group (pause/resume)
							? I18NSupport.getLocalizedMessage(BUNDLE_NAME, "trigger.paused", new Object[] {StringUtils.escapeXml(triggerGroupName)})//$NON-NLS-1$
							: StringUtils.escapeXml(triggerGroupName),
						// trigger name
						"<span title=\""+getTriggerExtraInfo(trigger)+"\" style=\""+getTriggerCSSStyle(jobDetail, currentlyExecutingJobs)+"\">" + StringUtils.escapeXml(triggerName) + "</span>",
						// trigger state
						I18NSupport.getLocalizedMessage(BUNDLE_NAME, "trigger.state."+triggerState),//$NON-NLS-1$
						makeCheckBox(trigger.isVolatile()),
						// job group
						StringUtils.escapeXml(jobDetail.getGroup()),
						// job name
						"<span title=\""+jobDetail.getJobClass()+"\">" + StringUtils.escapeXml(jobDetail.getName()) + "</span>",
						trigger.getPreviousFireTime() != null ? dateFormatter.format(trigger.getPreviousFireTime()) : "",
						trigger.getNextFireTime() != null ? dateFormatter.format(trigger.getNextFireTime()) : ""
				});
			}
		}

		Object[][] result = data.toArray(new Object[data.size()][]);
		return result;
	}

	protected String getTriggerCSSStyle(JobDetail jobDetail, List<JobExecutionContext> currentlyExecutingJobs) {
		if (isCurrentlyExecuting(jobDetail, currentlyExecutingJobs)) {
			return "font-style: italic;";
		} else {
			return "";//$NON-NLS-1$
		}
	}

	private String buildActionLinks(Scheduler scheduler, Trigger trigger, Set<String> pausedTriggerGroups, JobDetail jobDetail, List<JobExecutionContext> currentlyExecutingJobs) throws SchedulerException {
		StringBuffer out = new StringBuffer(64);
//		boolean isTriggerGroupPaused = pausedTriggerGroups.contains(trigger.getGroup());
		int triggerState = scheduler.getTriggerState(trigger.getName(), trigger.getGroup());
		switch (triggerState) {
		case Trigger.STATE_NORMAL:
		case Trigger.STATE_BLOCKED:
			//link to: pause Trigger
			String urlPause = QuartzStatistics.getUrlPrefix(servletContext, scheduler).append(QuartzStatistics.QUARTZ_ACTION_TRIGGER_PAUSE)
				.append('&').append(QuartzStatistics.PARAM_TRIGGER_NAME).append('=').append(urlEncodeUTF8(trigger.getName()))
				.append('&').append(QuartzStatistics.PARAM_TRIGGER_GROUP).append('=').append(urlEncodeUTF8(trigger.getGroup()))
				.toString();
			out.append(BaseAdminActionProvider.buildActionLink(urlPause,
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.trigger.pause"), displayProvider));//$NON-NLS-1$
			break;
		case Trigger.STATE_PAUSED:
			//link to: resume Trigger
			String urlResume = QuartzStatistics.getUrlPrefix(servletContext, scheduler).append(QuartzStatistics.QUARTZ_ACTION_TRIGGER_RESUME)
				.append('&').append(QuartzStatistics.PARAM_TRIGGER_NAME).append('=').append(urlEncodeUTF8(trigger.getName()))
				.append('&').append(QuartzStatistics.PARAM_TRIGGER_GROUP).append('=').append(urlEncodeUTF8(trigger.getGroup()))
				.toString();
			out.append(BaseAdminActionProvider.buildActionLink(urlResume,
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.trigger.resume"), displayProvider));//$NON-NLS-1$
			break;
		default:
			break;
		}
		if (! isCurrentlyExecuting(jobDetail, currentlyExecutingJobs)) {
			//link to: unschedule
			String urlDelete = QuartzStatistics.getUrlPrefix(servletContext, scheduler).append(QuartzStatistics.QUARTZ_ACTION_TRIGGER_UNSCHEDULE)
				.append('&').append(QuartzStatistics.PARAM_TRIGGER_NAME).append('=').append(urlEncodeUTF8(trigger.getName()))
				.append('&').append(QuartzStatistics.PARAM_TRIGGER_GROUP).append('=').append(urlEncodeUTF8(trigger.getGroup()))
				.toString();
			out.append("&nbsp;");
			out.append(BaseAdminActionProvider.buildActionLink(urlDelete,
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.trigger.delete"),//$NON-NLS-1$
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.trigger.delete.confirmJS"), displayProvider));//$NON-NLS-1$
		}
		return out.toString();
	}
}
