/**
 *
 */
package clime.messadmin.providers.quartz;

import java.text.Format;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import clime.messadmin.admin.BaseAdminActionProvider;
import clime.messadmin.i18n.I18NSupport;
import clime.messadmin.providers.spi.DisplayProvider;
import clime.messadmin.utils.DateUtils;
import clime.messadmin.utils.FastDateFormat;
import clime.messadmin.utils.StringUtils;

/**
 * TODO: pause / resume job group
 * @author C&eacute;drik LIME
 */
class QuartzJobTable extends AbstractQuartzTable {

	/**
	 *
	 */
	public QuartzJobTable(ServletContext servletContext, DisplayProvider displayProvider) {
		super(servletContext, displayProvider);
	}

	@Override
	protected String getTableCaption(Scheduler scheduler) {
		//FIXME add ajax links to: pause / resume
		try {
			return I18NSupport.getLocalizedMessage(BUNDLE_NAME, "caption.job", new Object[] {scheduler.getSchedulerName()});//$NON-NLS-1$
		} catch (SchedulerException e) {
			return e.toString();
		}
	}

	@Override
	public String[] getTabularDataLabels() {
		return new String[] {
				"", // empty label for actions: run, interrupt, etc.//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.job.group"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.job.name"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.job.description"),//$NON-NLS-1
				//I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.job.class"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.job.durable"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.job.volatile"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.job.stateful"),//$NON-NLS-1
				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "label.job.interruptable")//$NON-NLS-1
		};
	}

	@Override
	public Object[][] getTabularData(Scheduler scheduler) throws SchedulerException {
		final NumberFormat numberFormatter = NumberFormat.getNumberInstance(I18NSupport.getAdminLocale());
		final Format dateFormatter = FastDateFormat.getInstance(DateUtils.DEFAULT_DATE_TIME_FORMAT);
		List<Object> data = new LinkedList<Object>();
		List<JobExecutionContext> currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();//TODO add information "currently running" (italic, ...) + "currently running but paused"
		String[] allJobGroupNames = scheduler.getJobGroupNames();
		for (int g = 0; g < allJobGroupNames.length; ++g) {
			String jobGroupName = allJobGroupNames[g];
			String[] jobNames = scheduler.getJobNames(jobGroupName);
			for (int t = 0; t < jobNames.length; ++t) {
				String jobName = jobNames[t];
				JobDetail jobDetail = scheduler.getJobDetail(jobName, jobGroupName);
//				assert jobName.equals(jobDetail.getName());
//				assert jobGroupName.equals(jobDetail.getGroup());

				data.add(new Object[] {
						buildActionLinks(scheduler, jobDetail, currentlyExecutingJobs),
						StringUtils.escapeXml(jobGroupName),//TODO action links on: job group (pause/resume)
						"<span title=\""+getJobExtraInfo(scheduler, jobDetail)+"\" style=\""+getJobCSSStyle(jobDetail, currentlyExecutingJobs)+"\">" + StringUtils.escapeXml(jobName) + "</span>",
						jobDetail.getDescription() == null ? "" : StringUtils.escapeXml(String.valueOf(jobDetail.getDescription())),
						makeCheckBox(jobDetail.isDurable()),
						makeCheckBox(jobDetail.isVolatile()),
						makeCheckBox(jobDetail.isStateful()),
						makeCheckBox(QuartzUtils.isInterruptable(jobDetail))
				});
			}
		}

		Object[][] result = data.toArray(new Object[data.size()][]);
		return result;
	}

	protected String getJobCSSStyle(JobDetail jobDetail, List<JobExecutionContext> currentlyExecutingJobs) {
		if (isCurrentlyExecuting(jobDetail, currentlyExecutingJobs)) {
			return "font-style: italic;";
		} else {
			return "";//$NON-NLS-1$
		}
	}

	private String buildActionLinks(Scheduler scheduler, JobDetail jobDetail, List<JobExecutionContext> currentlyExecutingJobs) {
		StringBuffer out = new StringBuffer(64);
		if (isCurrentlyExecuting(jobDetail, currentlyExecutingJobs)) {
			if (QuartzUtils.isInterruptable(jobDetail)) {
				//link to: job.interrupt() | scheduler.interrupt(String jobName, String groupName)
				String urlInterrupt = QuartzStatistics.getUrlPrefix(servletContext, scheduler).append(QuartzStatistics.QUARTZ_ACTION_JOB_INTERRUPT)
					.append('&').append(QuartzStatistics.PARAM_JOB_NAME).append('=').append(urlEncodeUTF8(jobDetail.getName()))
					.append('&').append(QuartzStatistics.PARAM_JOB_GROUP).append('=').append(urlEncodeUTF8(jobDetail.getGroup()))
					.toString();
				out.append(BaseAdminActionProvider.buildActionLink(urlInterrupt,
						I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.job.interrupt"), displayProvider));//$NON-NLS-1$
			}
		} else {
			//link to: scheduler.triggerJobWithVolatileTrigger(String jobName, String groupName)
			String urlTrigger = QuartzStatistics.getUrlPrefix(servletContext, scheduler).append(QuartzStatistics.QUARTZ_ACTION_JOB_TRIGGER_VOLATILE)
				.append('&').append(QuartzStatistics.PARAM_JOB_NAME).append('=').append(urlEncodeUTF8(jobDetail.getName()))
				.append('&').append(QuartzStatistics.PARAM_JOB_GROUP).append('=').append(urlEncodeUTF8(jobDetail.getGroup()))
				.toString();
			out.append(BaseAdminActionProvider.buildActionLink(urlTrigger,
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.job.trigger"), displayProvider));//$NON-NLS-1$
		}
		//link to: pause/resume Job[Group]
//		out.append("&nbsp;");
//		String urlPause = QuartzStatistics.getUrlPrefix(servletContext, scheduler).append(QuartzStatistics.QUARTZ_ACTION_JOB_PAUSE)
//			.append('&').append(QuartzStatistics.PARAM_JOB_NAME).append('=').append(urlEncodeUTF8(jobDetail.getName()))
//			.append('&').append(QuartzStatistics.PARAM_JOB_GROUP).append('=').append(urlEncodeUTF8(jobDetail.getGroup()))
//			.toString();
//		out.append(BaseAdminActionProvider.buildActionLink(urlPause,
//				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.job.pause"), displayProvider));//$NON-NLS-1$
//		String urlResume = QuartzStatistics.getUrlPrefix(servletContext, scheduler).append(QuartzStatistics.QUARTZ_ACTION_JOB_RESUME)
//			.append('&').append(QuartzStatistics.PARAM_JOB_NAME).append('=').append(urlEncodeUTF8(jobDetail.getName()))
//			.append('&').append(QuartzStatistics.PARAM_JOB_GROUP).append('=').append(urlEncodeUTF8(jobDetail.getGroup()))
//			.toString();
//		out.append(BaseAdminActionProvider.buildActionLink(urlResume,
//				I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.job.resume"), displayProvider));//$NON-NLS-1$
		if (! isCurrentlyExecuting(jobDetail, currentlyExecutingJobs)) {
			//link to: delete Job
			String urlDelete = QuartzStatistics.getUrlPrefix(servletContext, scheduler).append(QuartzStatistics.QUARTZ_ACTION_JOB_DELETE)
				.append('&').append(QuartzStatistics.PARAM_JOB_NAME).append('=').append(urlEncodeUTF8(jobDetail.getName()))
				.append('&').append(QuartzStatistics.PARAM_JOB_GROUP).append('=').append(urlEncodeUTF8(jobDetail.getGroup()))
				.toString();
			out.append("&nbsp;");
			out.append(BaseAdminActionProvider.buildActionLink(urlDelete,
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.job.delete"),//$NON-NLS-1$
					I18NSupport.getLocalizedMessage(BUNDLE_NAME, "action.job.delete.confirmJS"), displayProvider));//$NON-NLS-1$
		}
		return out.toString();
	}
}
