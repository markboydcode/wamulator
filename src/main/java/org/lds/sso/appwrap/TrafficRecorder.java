package org.lds.sso.appwrap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.proxy.TrafficType;

/**
 * Keeps track of url hits and outcomes and the rest service instances started.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class TrafficRecorder {

    private static final Logger cLog = Logger.getLogger(TrafficRecorder.class);
    private SortedSet<Hit> hits = new TreeSet<Hit>();
    private boolean recordTraffic = false;
    private int rHitCount = 0;
    private SortedSet<RestHit> rhits = new TreeSet<RestHit>();
    private boolean recordRestTraffic = false;
    private List<RestInstanceInfo> restInstances = new ArrayList<RestInstanceInfo>();

    /**
     * Class for exposing in the UI the rest service instance locations that
     * were started for the simulator.
     * 
     * @author BOYDMR
     *
     */
    public static class RestInstanceInfo {
        
        private String policyDomain;
        private String cookiePath;
        private String base;
        private String baseResolved;

        public RestInstanceInfo(String urlBase, String urlBaseResolved, String getCookiePoint, String policyDomain) {
            this.base = urlBase;
            this.baseResolved = urlBaseResolved;
            this.cookiePath = getCookiePoint;
            this.policyDomain = policyDomain;
        }

        public String getPolicyDomain() {
            return policyDomain;
        }

        public String getCookiePath() {
            return cookiePath;
        }

        public String getUrlBase() {
            return base;
        }

        public String getResolvedUrlBase() {
            return baseResolved;
        }
    }
    
    /**
     * Add info for a rest instance that was started.
     * 
     * @param urlBase
     * @param getCookiePoint
     * @param policyDomain
     */
    public void addRestInst(String urlBase, String urlBaseResolved, String getCookiePoint, String policyDomain) {
       this.restInstances.add(new RestInstanceInfo(urlBase, urlBaseResolved, getCookiePoint, policyDomain)); 
    }
    
    /**
     * Expose rest instance info to UI.
     * @return
     */
    public List<RestInstanceInfo> getRestInstances() {
        return restInstances;
    }
    public synchronized void recordHit(long time, String connId, String username, int respCode, boolean isProxyRes, TrafficType trafficType, String method, String uri) {
        if (cLog.isInfoEnabled()) {
            cLog.info(
                    connId
                    + " " + username
                    + " " + (isProxyRes ? 'P' : '-')
                    + " " + trafficType.getTypeCharForLogEntries()
                    + " " + respCode
                    + " " + method
                    + " " + uri);
        }
        if (recordTraffic) {
            Hit hit = new Hit(time, connId, username, respCode, method, uri, isProxyRes, trafficType.getTypeCharForLogEntries());
            /*
             * The following two lines are subtle but needed. Hits implement
             * comparable and base equality on the connection id. In the event
             * that an exception occurs in RequestHandler that exception
             * catch block at the end logs the hit as a 500 using the same
             * connection id so that if a hit for the connection was already
             * recorded prior to the exception that hit will be replaced here
             * with the one from the catch block. Only calling add leaves the
             * existing one in the set and ignores the new one being added.
             */
            hits.remove(hit);
            hits.add(hit);
        }
    }

    public synchronized void recordRestHit(String path, int code, String response, Map<String, String> props) {
        rHitCount++;
        if (cLog.isInfoEnabled()) {
            cLog.info(
                    path
                    + " " + response
                    + " " + props);
        }
        if (recordRestTraffic) {
            RestHit rhit = new RestHit(rHitCount, path, code, response, props);
            /*
             * The following two lines are subtle but needed. Hits implement
             * comparable and base equality on the connection id. In the event
             * that an exception occurs in RequestHandler that exception
             * catch block at the end logs the hit as a 500 using the same
             * connection id so that if a hit for the connection was already
             * recorded prior to the exception that hit will be replaced here
             * with the one from the catch block. Only calling add leaves the
             * existing one in the set and ignores the new one being added.
             */
            rhits.remove(rhit);
            rhits.add(rhit);
        }
        if (rHitCount >= Config.getInstance().getMaxEntries()) {
            rHitCount = 0; //Reset the count
        }
    }

    public Set<Hit> getHits() {
        return hits;
    }

    public List<Hit> getTimestampSortedHits() {
        List sortedHits = new ArrayList<Hit>();
        sortedHits.addAll(hits);
        Collections.sort(sortedHits, new HitTimestampComparator());
        Collections.reverse(sortedHits);
        return sortedHits;
    }

    public Set<RestHit> getRestHits() {
        return rhits;
    }

    public class HitTimestampComparator implements Comparator<Hit> {

        public int compare(Hit hit1, Hit hit2) {
            int retval = 0;
            if (hit1.getTime() < hit2.getTime()) {
                retval = -1;
            } else if (hit1.getTime() > hit2.getTime()) {
                retval = 1;
            }
            return retval;
        }
    }

    public static class Hit implements Comparable<Hit> {

        private int code;
        private String username;
        private String method;
        private String uri;
        private boolean isProxyCode;
        private String connId;
        private int conn = -1;
        private long time;
        private String fileName;
        private char trafficType;
        private static final SimpleDateFormat fmtr = new SimpleDateFormat("HH:mm:ss.SSS");
        private static final SimpleDateFormat longfmtr = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");

        public Hit(long time, String connId, String username, int code, String method, String uri, boolean proxyResponse, char trafficType) {
            this.username = username;
            this.time = time;
            this.connId = connId;
            String cid = connId.substring("C-".length());
            this.conn = Integer.parseInt(cid);

            this.code = code;
            this.method = method;
            // replace "&" with &amp; in uris since firefox freaks out if there
            // is an "&l" in the URI like for "&lang=eng" causing rendering to
            // break into three times the element height and replacing the 
            // &lang with a less than char "<". Weird.
            this.uri = uri.replace("&", "&amp;"); 
            this.isProxyCode = proxyResponse;
            this.trafficType = trafficType;
        }

        public char getTrafficType() {
            return trafficType;
        }

        public String getConnId() {
            return connId;
        }

        public int getCode() {
            return code;
        }

        public String getUsername() {
            return username;
        }

        public String getMethod() {
            return method;
        }

        public String getUri() {
            return uri;
        }

        public boolean isProxyCode() {
            return isProxyCode;
        }

        public boolean getIsProxyCode() {
            return isProxyCode;
        }

        public int compareTo(Hit o) {
            return Long.signum(conn - o.conn);
        }

        public String getTimestamp() {
            return fmtr.format(getTime());
        }

        public String getLongTimestamp() {
            return longfmtr.format(getTime());
        }

        public String getFilename() {
            if (fileName == null) {
                fileName = uri.substring(uri.lastIndexOf("/"));
            }
            return fileName;
        }

        /**
         * @return the time
         */
        public long getTime() {
            return time;
        }

        /**
         * @param time the time to set
         */
        public void setTime(long time) {
            this.time = time;
        }
    }

    public static class RestHit implements Comparable<RestHit> {

        private String path;
        private String response;
        private Map<String, String> props;
        private long time;
        private int code;
        private static final SimpleDateFormat fmtr = new SimpleDateFormat("HH:mm:ss.SSS");

        public RestHit(long time, String path, int code, String response, Map<String, String> props) {
            this.path = path;
            this.code = code;
            this.response = response;
            this.props = props;
            this.time = time;
        }

        public int getCode() {
            return code;
        }

        public String getPath() {
            return path;
        }

        public String getResponse() {
            return response;
        }

        public Map<String, String> getProperties() {
            return props;
        }

        public int compareTo(RestHit o) {
            return Long.signum(time - o.time);
        }

        public String getTimestamp() {
            return fmtr.format(time);
        }
    }

    /**
     * Indicates if recording of SSO connections is active.
     * @return
     */
    public boolean isRecording() {
        return recordTraffic;
    }

    /**
     * Indicates if recording of rest calls is active.
     * @return
     */
    public boolean isRecordingRest() {
        return recordRestTraffic;
    }

    /**
     * Turns on recording of SSO connections.
     *
     * @param recordTraffic
     */
    public void setRecording(boolean recordTraffic) {
        this.recordTraffic = recordTraffic;
    }

    /**
     * Turns on recording of rest calls.
     *
     * @param recordTraffic
     */
    public void setRecordingRest(boolean recordTraffic) {
        this.recordRestTraffic = recordTraffic;
    }
}
