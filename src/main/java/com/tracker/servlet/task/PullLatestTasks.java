package com.tracker.servlet.task;

import static com.tracker.ofy.OfyService.ofy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.tracker.entity.HITcontent;
import com.tracker.entity.HITgroup;
import com.tracker.entity.HITinstance;
import com.tracker.entity.HITrequester;
import com.tracker.entity.MarketStatistics;
import com.tracker.util.SafeCurrencyFormat;
import com.tracker.util.SafeDateFormat;

@SuppressWarnings("serial")
public class PullLatestTasks extends HttpServlet {

    private static final Logger logger = Logger.getLogger(PullLatestTasks.class.getName());
    private static final String URL = "https://www.mturk.com/mturk/viewhits?"
            + "&searchSpec=HITGroupSearch%23T%238%2310%23-1%23T%23%21%23%21LastUpdatedTime%211%21%23%21"
            + "&selectedSearchType=hitgroups"
            + "&searchWords=";

    private static final Set<String> ENGLISH_STOP_WORDS_SET;

    static {
        final List<String> stopWords = Arrays.asList(
                "a", "an", "and", "are", "as", "at", "be", "but", "by",
                "for", "if", "in", "into", "is", "it",
                "no", "not", "of", "on", "or", "such",
                "that", "the", "their", "then", "there", "these",
                "they", "this", "to", "was", "will", "with"
        );
        ENGLISH_STOP_WORDS_SET = new HashSet<String>(stopWords);
    }

    private static final Set<String> sortTypes = new HashSet<String>(Arrays.asList("LastUpdatedTime", "NumHITs",
            "Reward", "LatestExpiration", "Title", "AssignmentDurationInSeconds"));
    private static final String DEFAULT_SORT_TYPE = "LastUpdatedTime";
    private static final String DEFAULT_SORT_DIRECTION = "1";
    private static final String USERAGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36";
    private static final String MTURK_AUTH_COOKIES = "mturk_auth_cookies";
    private static final String MTURK_AUTH_CREDENTIALS = "mturk_auth_credentials";

    private static final String PREVIEW_URL = "https://www.mturk.com/mturk/preview?groupId=";
    private static final DateFormat df = SafeDateFormat.forPattern("MMM dd, yyyy");
    private static final NumberFormat cf = SafeCurrencyFormat.forLocale(Locale.US);

    private static final Pattern timePattern = Pattern.compile("\\d+ \\w+");

    private MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String pageNumberParam = req.getParameter("pageNumber");
        Integer pageNumber = (pageNumberParam == null) ? 1 : Integer.valueOf(pageNumberParam);
        if (pageNumber > 20) {
            logger.log(Level.WARNING, "MTurk does not return pages above 20 without sign-in.");
            return;
        }

        String sortType = req.getParameter("sortType");
        if (sortType == null || !sortTypes.contains(sortType)) {
            sortType = DEFAULT_SORT_TYPE;
        }

        String sortDirection = req.getParameter("sortDirection");
        if (sortDirection == null || !"0".equals(sortDirection) || !"1".equals(sortDirection)) {
            sortDirection = DEFAULT_SORT_DIRECTION;
        }

        String url = URL + "&pageNumber=" + pageNumber.toString()
                + "&sortType=" + URLEncoder.encode(sortType + ":" + sortDirection, "UTF-8");

        try {
            Map<String, String> authCookies = new HashMap<String, String>();
            authCookies.put("session-id", getProperty("session-id"));
            authCookies.put("worker_state", getProperty("worker_state"));
            //should be: Map<String, String> authCookies = getAuthCookies(); - disabled because CAPATCHA, using workaround with hardcoded session-id and worker_state
            loadAndParse(url, authCookies, pageNumber == 1 && sortType == DEFAULT_SORT_TYPE && sortDirection == DEFAULT_SORT_DIRECTION);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing page with URL: " + url, e);
        }
    }

    private void loadAndParse(String url, Map<String, String> authCookies, boolean fetchStatistics) throws Exception {
        Date now = new Date();
        List<HITgroup> hitGroups = new ArrayList<HITgroup>();
        List<HITcontent> hitContents = new ArrayList<HITcontent>();
        List<HITrequester> hitRequesters = new ArrayList<HITrequester>();
        List<HITinstance> hitInstances = new ArrayList<HITinstance>();

        HttpURLConnection connection = createDefaultConnection(url);
        connection.setRequestProperty("Cookie", getCookiesString(authCookies));
        Document doc = Jsoup.parse(connection.getInputStream(), "UTF-8", connection.getURL().toString());

        //market statistics
        if (fetchStatistics) {
            MarketStatistics statistics = extractMarketStatistics(doc);
            statistics.setTimestamp(now);
            ofy().save().entity(statistics);
        }

        Elements rows = getHitRows(doc);

        if (rows == null) {
            logger.log(Level.WARNING, "HITgroups are not found");
            return;
        }
        Iterator<Element> rowsIterator = rows.iterator();
        String groupId = null;
        String date = null;

        while (rowsIterator.hasNext()) {
            try {
                Element row = rowsIterator.next();
                Element titleElement = row.select("a.capsulelink").first();
                Element groupElement = row.select("a:matchesOwn(View a HIT in this group+)").first();
                Element requesterElement = row.select("a:matchesOwn(Requester+)").first();
                Element expirationDateElement = row.select("a:matchesOwn(HIT Expiration Date+)").first();
                Element timeAllotedElement = row.select("a:matchesOwn(Time Allotted+)").first();
                Element rewardElement = row.select("a:matchesOwn(Reward:+)").first();
                Element hitsAvailableElement = row.select("a:matchesOwn(HITs Available:+)").first();
                Element descriptionElement = row.select("a:matchesOwn(Description+)").first();
                Element keywordElement = row.select("a:matchesOwn(Keywords:+)").first();
                Element qualificationElement = row.select("a:matchesOwn(Qualifications Required+)").first();

                groupId = getQueryParamValue(groupElement.attr("href"), "groupId");
                if (groupId == null) {
                    continue;
                }

                date = expirationDateElement.parent().nextElementSibling().text();
                Date expirationDate = df.parse(date.substring(0, date.indexOf(" (") - 1));
                Integer hitsAvailable = Integer.parseInt(hitsAvailableElement.parent().nextElementSibling().text());
                Number reward = cf.parse(rewardElement.parent().nextElementSibling().child(0).text());
                Integer rewardValue = Math.round(100 * reward.floatValue());
                Integer rewardAvailable = rewardValue * hitsAvailable;

                //check existing HITgroup
                HITgroup existingGroup = ofy().load().type(HITgroup.class).id(groupId).now();
                if (existingGroup != null) {
                    // If we already have a HITgroup, we just update the lastSeen page
                    existingGroup.setLastSeen(now);

                    // If the group was inactive, we revive it and create a new HITinstance
                    if (existingGroup.isActive() == false) {
                        existingGroup.setActive(true);
                        existingGroup.setExpirationDate(expirationDate);

                        HITinstance hitinstance = new HITinstance(groupId, new Date(), hitsAvailable, hitsAvailable, rewardAvailable, rewardAvailable);
                        hitInstances.add(hitinstance);
                    }

                    hitGroups.add(existingGroup);
                    continue;
                }

                String title = titleElement.text();
                String requesterId = getQueryParamValue(
                        requesterElement.parent().nextElementSibling().child(0).attr("href"), "requesterId");
                String requesterName = requesterElement.parent().nextElementSibling().child(0).text();
                String timeAlloted = timeAllotedElement.parent().nextElementSibling().text();
                String description = descriptionElement.parent().nextElementSibling().text();

                //keywords
                List<String> keywords = getKeywords(keywordElement);

                //qualifications
                List<String> qualifications = getQualifications(qualificationElement);

                String content = loadHitContent(groupId);

                //create new HITgroup
                HITgroup hitGroup = new HITgroup(groupId, requesterId, title,
                        description, keywords, expirationDate, rewardValue,
                        parseTime(timeAlloted), qualifications, now, now);
                hitGroups.add(hitGroup);

            //create new HITrequester if HIT group is not exists,
                //requester will be updated if he already exists
                if (existingGroup == null) {
                    HITrequester requester = new HITrequester(requesterId, requesterName, new Date());
                    hitRequesters.add(requester);
                }

                //create new HITcontent
                HITcontent hitContent = new HITcontent(groupId, content);
                addToIndex(hitGroup, requesterId, requesterName, content);
                hitContents.add(hitContent);

                // Create a new (the first) HITinstance
                HITinstance hitinstance = new HITinstance(groupId, now, hitsAvailable, hitsAvailable, rewardAvailable, rewardAvailable);
                hitInstances.add(hitinstance);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error parsing HITgroup row", e);
            }
        }

        if (hitGroups.size() != 0 && hitInstances.size() != 0) {
            ofy().save().entities(hitGroups);
            ofy().save().entities(hitContents);
            ofy().save().entities(hitRequesters);
            ofy().save().entities(hitInstances);
        }
    }

    private void addToIndex(HITgroup hitGroup, String requesterId, String requesterName, String content) {
        Index index = SearchServiceFactory.getSearchService()
                .getIndex(IndexSpec.newBuilder().setName("hit_group_index"));

        com.google.appengine.api.search.Document doc = com.google.appengine.api.search.Document.newBuilder()
                .addField(Field.newBuilder().setName("requesterId").setText(requesterId))
                .addField(Field.newBuilder().setName("requesterName").setText(requesterName))
                .addField(Field.newBuilder().setName("title").setText(hitGroup.getTitle()))
                .addField(Field.newBuilder().setName("description").setText(hitGroup.getDescription()))
                .addField(Field.newBuilder().setName("hitContent").setText(optimizeContentForIndex(content)))
                .addField(Field.newBuilder().setName("keywords").setText(StringUtils.join(hitGroup.getKeywords(), ", ")))
                .addField(Field.newBuilder().setName("qualifications").setText(StringUtils.join(hitGroup.getQualificationsRequired(), ", ")))
                .addField(Field.newBuilder().setName("reward").setText(String.valueOf(hitGroup.getReward())))
                .addField(Field.newBuilder().setName("timeAllotted").setText(String.valueOf(hitGroup.getTimeAlloted())))
                .setId(hitGroup.getGroupId())
                .build();
        index.put(doc);
    }

    /*remove html tags, styles, scripts, stop words and duplicate words*/
    private String optimizeContentForIndex(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }

        String text = Jsoup.parse(content).text();
        StringTokenizer st = new StringTokenizer(text);
        Set<String> set = new LinkedHashSet<String>();

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (!ENGLISH_STOP_WORDS_SET.contains(token)) {
                set.add(token);
            }
        }

        return StringUtils.join(set, " ");
    }

    private Elements getHitRows(Document doc) {
        Elements tables = doc.select("table[cellspacing=5][width=100%]");
        for (Element table : tables) {
            Elements rows = table.select("> tbody > tr");
            if (rows.size() == 10) {
                return rows;
            }
        }

        return null;
    }

    private List<String> getKeywords(Element keywordElement) {
        //keywords
        List<String> keywords = new ArrayList<String>();
        Elements keywordLinks = keywordElement.parent().nextElementSibling().getElementsByTag("a");
        for (int j = 0; j < keywordLinks.size(); j++) {
            String keyword = keywordLinks.get(j).text();
            keywords.add(keyword);
        }
        return keywords;

    }

    private List<String> getQualifications(Element qualificationElement) {
        //qualifications
        List<String> qualifications = new ArrayList<String>();
        //Element qValues = qualificationElement.parent().nextElementSibling();
        Elements qValues = qualificationElement.parent().parent().siblingElements();

        if (qValues != null) {
            /*Elements qElements = qValues.getElementsByTag("a");
              for(Element qElement : qElements){
                qualifications.add(qElement.text());
            }*/
            for (Element qElement : qValues) {
                qualifications.add(qElement.text());
            }
        }
        return qualifications;

    }

    private MarketStatistics extractMarketStatistics(Document doc) {
        String availableHitsText = doc.select("span:matchesOwn(available now+)").first().child(0).text();
        Integer availableHits = Integer.parseInt(availableHitsText.substring(0,
                availableHitsText.indexOf(" ")).trim().replaceAll(",", ""));

        String availableGroupsText = doc.select("td:matchesOwn([\\d-]+ of \\d+ Results+)").text();
        Integer availableGroups = Integer.parseInt(availableGroupsText.substring(
                availableGroupsText.indexOf("of") + 2, availableGroupsText.indexOf("Results")).trim());

        MarketStatistics statistics = new MarketStatistics(new Date(), availableGroups, availableHits);
        return statistics;
    }

    private String loadHitContent(String groupId) {
        try {
            Document preview;
            preview = Jsoup.connect(PREVIEW_URL + groupId).timeout(30000).get();

            Element internalContentElement = preview.getElementById("hit-wrapper");
            if (internalContentElement != null) {
                return internalContentElement.html();
            }

            Elements iframes = preview.select("iframe[name=ExternalQuestionIFrame]");
            Element iframe = iframes.first();
            if (iframe != null) {
                Document content = Jsoup.connect(iframes.attr("src")).timeout(30000).get();
                return content.html();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to load external content", e);
        }
        return null;
    }

    private int parseTime(String str) {
        Matcher matcher = timePattern.matcher(str);
        int result = 0;

        while (matcher.find()) {
            String[] pairs = matcher.group().split(" ");
            Integer val = Integer.parseInt(pairs[0]);
            String unit = pairs[1];
            if ("hour".equals(unit) || "hours".equals(unit)) {
                result += val * 60 * 60;
            } else if ("minute".equals(unit) || "minutes".equals(unit)) {
                result += val * 60;
            } else if ("second".equals(unit) || "seconds".equals(unit)) {
                result += val;
            }
        }

        return result;
    }

    private String getQueryParamValue(String url, String param) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        int queryStart = url.indexOf('?');
        if (queryStart > 0) {
            String queryString = url.substring(queryStart + 1);
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String paramName = pair.substring(0, idx);

                if (paramName.equals(param)) {
                    return pair.substring(idx + 1);
                }
            }
        }

        return null;
    }

    @SuppressWarnings({ "unused", "unchecked" })
    private Map<String, String> getAuthCookies() throws Exception {
        if(memcacheService.contains(MTURK_AUTH_COOKIES)) {
            return (Map<String, String>) memcacheService.get(MTURK_AUTH_COOKIES);
        }

        //1.begin request
        HttpURLConnection beginConnection = createDefaultConnection("https://www.mturk.com/mturk/beginsignin");
        beginConnection.setInstanceFollowRedirects(false);
        beginConnection.connect();

        String referer = beginConnection.getHeaderField("Location");
        //2.signin request
        HttpURLConnection signinConnection = createDefaultConnection(referer);
        signinConnection.setDoInput(true);
        signinConnection.setInstanceFollowRedirects(false);
        signinConnection.addRequestProperty("Referer", "https://www.mturk.com/mturk/findhits?match=false");
        signinConnection.connect();

        Document doc = Jsoup.parse(signinConnection.getInputStream(), "UTF-8", signinConnection.getURL().toString());
        FormElement signinForm = (FormElement) doc.getElementById("ap_signin_form");
        if(signinForm == null) {//because CAPATCHA
            return null;
        }
        Map<String, String> signinFormParms = new HashMap<String, String>();
        Elements elements = signinForm.select("input[type=hidden]");
        for (Iterator<Element> iterator = elements.iterator(); iterator.hasNext();) {
            Element element = iterator.next();
            signinFormParms.put(element.attr("name"), element.val());
        }
        signinFormParms.put("email", getProperty("email"));
        signinFormParms.put("password", getProperty("password"));

        //3.submit request
        HttpURLConnection submitConnection = createDefaultConnection(signinForm.attr("action"));
        submitConnection.setDoOutput(true);
        submitConnection.setRequestProperty("Cookie", getCookiesString(readCookies(signinConnection)));
        submitConnection.addRequestProperty("Referer", referer);
        submitConnection.setInstanceFollowRedirects(false);
        submitConnection.connect();

        OutputStream os = null;
        BufferedWriter writer = null;
        try {
            os = submitConnection.getOutputStream();
            writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(signinFormParms));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        } finally{
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(writer);
        }

        //4.return request
        HttpURLConnection returnConnection = createDefaultConnection(submitConnection.getHeaderField("Location"));
        returnConnection.setRequestProperty("Cookie", getCookiesString(readCookies(beginConnection)));
        returnConnection.addRequestProperty("Referer", referer);
        returnConnection.setInstanceFollowRedirects(false);
        returnConnection.connect();

        //5.endsignin request
        HttpURLConnection endsigninConnection = createDefaultConnection(returnConnection.getHeaderField("Location"));
        Map<String, String> endCookies = new HashMap<String, String>();
        endCookies.putAll(readCookies(beginConnection));
        endCookies.putAll(readCookies(returnConnection));
        endsigninConnection.setRequestProperty("Cookie", getCookiesString(endCookies));
        endsigninConnection.setInstanceFollowRedirects(false);
        endsigninConnection.addRequestProperty("Referer", referer);
        endsigninConnection.connect();
        endCookies.putAll(readCookies(endsigninConnection));

        //6.checksignin request
        HttpURLConnection checksigninConnection = createDefaultConnection(endsigninConnection.getHeaderField("Location"));
        checksigninConnection.setRequestProperty("Cookie", getCookiesString(endCookies));
        endsigninConnection.addRequestProperty("Referer", referer);
        checksigninConnection.connect();

        memcacheService.put(MTURK_AUTH_COOKIES, endCookies);
        return endCookies;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String getProperty(String id) {
        Map<String, String> properties;
        if(memcacheService.contains(MTURK_AUTH_CREDENTIALS)) {
            properties = (Map<String, String>) memcacheService.get(MTURK_AUTH_CREDENTIALS);
            return properties.get(id);
        } else {
            InputStream is = null;
            Properties prop = new Properties();
            try {
                is = PullLatestTasks.class.getClassLoader().getResourceAsStream("mturk-credentials.properties");
                prop.load(is);
                properties = new HashMap<String, String>((Map) prop);
                memcacheService.put(MTURK_AUTH_CREDENTIALS, properties);
                return properties.get(id);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(is);
            }
            return null;
        }
    }

    private HttpURLConnection createDefaultConnection(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(60*1000);

        connection.setRequestProperty("User-Agent", USERAGENT);
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.8");

        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        return connection;
    }

    private Map<String, String> readCookies(HttpURLConnection connection) {
        Map<String, String> cookies = new HashMap<String, String>();
        Map<String, List<String>> headerFields = connection.getHeaderFields();

        Set<String> headerFieldsSet = headerFields.keySet();
        Iterator<String> headerFieldsIter = headerFieldsSet.iterator();

        while (headerFieldsIter.hasNext()) {
            String headerFieldKey = headerFieldsIter.next();
            if ("Set-Cookie".equalsIgnoreCase(headerFieldKey)) {
                List<String> headerFieldValue = headerFields.get(headerFieldKey);
                for (String headerValue : headerFieldValue) {
                    String[] fields = headerValue.split(";\\s*");
                    String cookieValue = fields[0];
                    if (cookieValue.indexOf('=') > 0) {
                        String[] f = fields[0].split("=");
                        cookies.put(f[0], f[1]);
                    }
                }
            }
        }
        return cookies;
    }

    private String getPostDataString(Map<String, String> params) throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    private String getCookiesString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first) {
                first = false;
            } else {
                result.append("; ");
            }
            result.append(entry.getKey());
            result.append("=");
            result.append(entry.getValue());
        }

        return result.toString();
    }
}
