package au.com.codeka.warworlds.server.handlers;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.SituationReportController;
import au.com.codeka.warworlds.server.model.Empire;

/**
 * Handler that handles both /realms/.../sit-reports and /realms/.../stars/<star_id>/sit-reports.
 */
public class SitReportsHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        String starKey = getUrlParameter("starid");
        Integer starID = starKey == null
                       ? null : Integer.parseInt(starKey);

        // the cursor is basically just a base64-encoding of the last one we returned
        DateTime before = DateTime.now().plusHours(1);
        String cursor = getRequest().getParameter("cursor");
        if (cursor != null && !cursor.isEmpty()) {
            before = new DateTime(Long.parseLong(cursor), DateTimeZone.UTC);
        }

        DateTime after = null;
        if (getRequest().getParameter("show-old-items") == null) {
            Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
            DateTime lastReadTime = empire.getLastSitrepReadTime();
            if (lastReadTime != null) {
                after = lastReadTime;
            }
        }

        Messages.SituationReportFilter filter = null;
        if (getRequest().getParameter("filter") != null) {
            filter = Messages.SituationReportFilter.valueOf(getRequest().getParameter("filter"));
        }

        Integer empireID = getSession().getEmpireID();
        if (getSession().isAdmin()) {
            empireID = null;
        }
        List<Messages.SituationReport> sitreps = new SituationReportController().fetch(
                empireID, starID, before, after, filter, 50);

        DateTime first = null;
        if (!sitreps.isEmpty()) {
            first = new DateTime(sitreps.get(sitreps.size() - 1).getReportTime() * 1000,
                    DateTimeZone.UTC);
        }

        Messages.SituationReports.Builder sitreps_pb = Messages.SituationReports.newBuilder();
        for (Messages.SituationReport sitrep : sitreps) {
            sitreps_pb.addSituationReports(sitrep);
        }

        if (first == null) {
            cursor = "";
        } else {
            cursor = Long.toString(first.getMillis());
        }
        sitreps_pb.setCursor(cursor);
        setResponseBody(sitreps_pb.build());
    }
}
