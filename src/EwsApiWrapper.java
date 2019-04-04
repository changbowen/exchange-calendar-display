import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.ResolveNameSearchLocation;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.Mailbox;
import microsoft.exchange.webservices.data.search.CalendarView;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.ItemView;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;

public class EwsApiWrapper
{
    static
    {
        try {
            var configFile = Util.AppRootDir + "/WEB-INF/config.xml";
            //load access keys from config file
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(configFile));
            doc.getDocumentElement().normalize();
            var xpath = XPathFactory.newInstance().newXPath();
            var domainCred = (Element)((NodeList) xpath.compile("//Credentials//Credential[@Type=\"domain\"]").evaluate(doc, XPathConstants.NODESET)).item(0);
            username = domainCred.getElementsByTagName("Username").item(0).getTextContent();
            password = domainCred.getElementsByTagName("Password").item(0).getTextContent();
            domain = domainCred.getElementsByTagName("Domain").item(0).getTextContent();
            ewsUri = doc.getElementsByTagName("EwsUri").item(0).getTextContent();
        }
        catch (SAXException | XPathExpressionException | ParserConfigurationException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    private static String username;
    private static String password;
    private static String domain;
    private static String ewsUri;

    public class Appointment {
        public String Subject;
        public Date Start;
        public Date End;
        public Appointment(String subject, Date start, Date end) {
            Subject = subject;
            Start = start;
            End = end;
        }
    }

    public class Response<T> {
        public String ErrorMsg;
        public String Email;
        public String DisplayName;
        public ArrayList<T> Items;
    }

    private ExchangeService service;


    public EwsApiWrapper() throws URISyntaxException
    {
        service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        service.setCredentials(new WebCredentials(username, password, domain));
        service.setUrl(new URI(ewsUri));
        //service.autodiscoverUrl(email, s -> s.toLowerCase().startsWith("https://"));
    }

    public Response<Appointment> GetAppointments(String email, Date startDate, Date endDate) throws Exception
    {
        var args = new Object[] { email, startDate, endDate };
        return Cachier.GetCached(args, this::getAppointments);
    }

    /**
     * @param args Three arguments are mailbox, startDate, endDate.
     * @return The ArrayList of appointments. Or null if there is an error.
     */
    private Response<Appointment> getAppointments(Object[] args)
    {
        var mailbox = (String) args[0];
        var startDate = (Date) args[1];
        var endDate = (Date) args[2];

        var resp = new Response<Appointment>();
        try {
            var nameRes = service.resolveName(mailbox, ResolveNameSearchLocation.DirectoryOnly, true);
            if (nameRes.getCount() > 0) {
                var resolution = nameRes.nameResolutionCollection(0);
                resp.DisplayName = resolution.getContact().getDisplayName();
                resp.Email =  resolution.getMailbox().getAddress();
            }
            else throw new Exception("The target mailbox cannot be resolved.");

            var folderId = new FolderId(WellKnownFolderName.Calendar, new Mailbox(resp.Email));
            var resultRaw = service.findAppointments(folderId, new CalendarView(startDate, endDate));
            resp.Items = new ArrayList<>();
            for (var app : resultRaw.getItems()) {
                resp.Items.add(new Appointment(app.getSubject(), app.getStart(), app.getEnd()));
            }
        } catch (Exception ex) {
            resp.ErrorMsg = ex.toString();
        }
        return resp;
    }

    /**
     * This is WIP...
     */
    private Response<Item> getItems(String email, WellKnownFolderName folder) {
        //for better tolerance searches check out the below page
        //https://docs.microsoft.com/en-us/exchange/client-developer/exchange-web-services/how-to-perform-paged-searches-by-using-ews-in-exchange
        int pageSize = 50;
        var resp = new Response<Item>();
        try {
            var view = new ItemView(pageSize);
            FindItemsResults<Item> resultRaw;
            do {
                var folderId = new FolderId(folder, new Mailbox(email));
                resultRaw = service.findItems(folderId, view);
                resp.Items = new ArrayList<>();
                for(Item item : resultRaw.getItems())
                {
                    resp.Items.add(item);
                }
                view.setOffset(view.getOffset() + pageSize);
            } while (resultRaw.isMoreAvailable());
        }
        catch (Exception ex) {
            resp.ErrorMsg = ex.toString();
        }
        return resp;
    }
}
