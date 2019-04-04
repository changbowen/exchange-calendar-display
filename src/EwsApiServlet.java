import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

@javax.servlet.annotation.WebServlet(name = "EwsApiServlet", urlPatterns = "/EwsApiServlet")
public class EwsApiServlet extends javax.servlet.http.HttpServlet
{
    @Override
    public void init() throws ServletException
    {
        super.init();
        Util.AppRootDir = getServletContext().getRealPath("/");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        callback(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        callback(request, response);
    }

    private void callback(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String respJson = "";
        response.setCharacterEncoding("UTF-8");
        var funcName = request.getParameter("funcName");
        try {
            switch (funcName) {
                case "GetAppointments": {
                    var para_mailbox = request.getParameter("mailbox");
                    var para_startDate = Long.parseLong(request.getParameter("startDate"));
                    var para_endDate = Long.parseLong(request.getParameter("endDate"));

                    var startDate = new Date(para_startDate);
                    var endDate = new Date(para_endDate);
                    var resp = new EwsApiWrapper().GetAppointments(para_mailbox, startDate, endDate);
                    if (resp == null)
                        throw new Exception("There was an error getting response.");
                    else if (resp.ErrorMsg != null)
                        throw new Exception("There was an error getting response.\r\n" + resp.ErrorMsg);

                    respJson = new ObjectMapper().writeValueAsString(Map.of("response", resp));
                    break;
                }
                case "GetFileList": {
                    var para_relPath = request.getParameter("relPath");
                    var files = new File(Util.AppRootDir + para_relPath).list();
                    respJson = new ObjectMapper().writeValueAsString(Map.of("response", files));
                    break;
                }
            }
        }
        catch (Exception ex) {
            respJson = new ObjectMapper().writeValueAsString(Map.of("error", ex.getMessage()));
        }
        response.getWriter().write(respJson);
    }
}
