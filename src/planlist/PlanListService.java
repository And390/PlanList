package planlist;

import planlist.data.DataAccess;
import planlist.data.FileDataAccess;
import planlist.data.MongoDBDataAccess;
import template.ServletTemplateManager;
import template.TemplateManager;
import template.WatchFileTemplateManager;
import utils.ConfigException;
import utils.StringList;
import utils.Util;
import utils.log.Logger;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * And390 - 09.03.15.
 */
public class PlanListService implements Filter
{
    // TODO ���� ������ ����������� ����������� ������, �� ��� ����������� ����� ������ ��������� (401)

    public static String AJAX_ENCODING = "UTF-8";

    private Logger logger = Logger.console;
    private String encoding;
    private TemplateManager templateManager;
    private DataAccess dataAccess;
    private String storageDir;  // �������� ���� � storage ������������ ����� ���-����������, ���� data ������ ����

    @Override
    public void init(FilterConfig config) throws ServletException
    {
        try  {
            ServletContext servletContext = config.getServletContext();
            //    read properties
            InputStream input = servletContext.getResourceAsStream("/WEB-INF/config.properties");
            if (input==null)  throw new ConfigException ("No 'WEB-INF/config.properties' file");
            Properties properties = Util.readProperties(input, "UTF-8");
            encoding = Util.get(properties, "encoding", "UTF-8");
            boolean watch = Util.getBool(properties, "watch", true);
            //    �������� ���� � �������� ���-���������� � ��������� �� ��� TemplateManager
            String path = servletContext.getRealPath("/");  //���� ������� ������� ����� �� ��������� � ����������� �� �������
            if (path==null)  path = new File (servletContext.getResource("/").getPath()).getCanonicalPath();  //����� ����� ����������� ��� �����
            //if (path==null)  path = new File (this.getClass().getResource("/").getPath()+"../../").getCanonicalPath();  //����� ����� ����������� ��� �����
            templateManager = watch ? new WatchFileTemplateManager(new File(path), encoding) :
                    new ServletTemplateManager(servletContext, encoding);
            //    storage
            String filesStorageDir = Util.get(properties, "storage.files.dir", null);
            String mongodbURL = Util.get(properties, "storage.mongodb.url", null);
            if (filesStorageDir==null && mongodbURL==null)
                throw new ConfigException ("You must specify one of storage config properties: 'storage.files.dir' or 'storage.mongodb.url'");
            if (filesStorageDir!=null && mongodbURL!=null)
                throw new ConfigException ("You can specify only one of storage config properties: 'storage.files.dir' or 'storage.mongodb.url'");
            if (mongodbURL!=null)  {
                //  mongo data storage
                dataAccess = new MongoDBDataAccess (mongodbURL);
            }
            else  {
                //  file storage, ���� ���������� � '/', '.' ��� '..', �� ��������� ���������� ����� ��� ����� ������������ ����� ������� (�������)
                //  ����� ��������� ����� ������������ ����� �������� ���-����������
                String data = Util.get(properties, "storage.files.dir", "data");
                if (!data.startsWith(".") && !new File (data).isAbsolute())  data = path+"/"+data;
                storageDir = Util.subFilePath(path, data);
                dataAccess = new FileDataAccess(data);
            }
        }
        catch (Exception e)  {
            destroy();
            throw //e instanceof ServletException ? (ServletException)e :
                  e instanceof ConfigException ? new ServletException (e.getMessage()) : new ServletException(e);
        }
    }

    @Override
    public void destroy()
    {
        try  {
            if (templateManager!=null && templateManager instanceof AutoCloseable)  ((AutoCloseable)templateManager).close();
            TemplateManager.free();
            if (dataAccess!=null)  dataAccess.close();
        }
        catch (RuntimeException e)  {  throw e;  }
        catch (Exception e)  {  throw new RuntimeException (e);  }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        //    try process
        try  {  if (processRequest(request, response))  return;  }
        catch (IOException|ServletException|RuntimeException e)  {  throw e;  }
        catch (Exception e)  {  throw new ServletException (e);  }
        //    default process if function returns false
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public boolean processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        //    get request path
        String context = request.getContextPath();
        String path = request.getRequestURI().substring(context.length());
        path = java.net.URLDecoder.decode(path, "UTF-8");
        if (path.equals("/"))  path = "";
        else if (!path.startsWith("/"))  path = '/'+path;  // � ����� ���� ��� ������ ������������ ��������

        //    ��-html ����� � �������� ����������� �������� �� ���������, �� �������� storage �������� ������
        if (!path.equals("") && !(storageDir !=null && Util.startsWithPath(path, storageDir))
                && path.indexOf('.')!=-1 && !path.endsWith(".html"))  {
            return false;
        }

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        String error = null;

        //    ��������� ����� action (������� ���� ����� html �����, �� �������������� � ����� ajax
        //    ������ - ����������� �������� (������ ��� ������ login � logout)
        try  {
            //    ��������� register action
            if (request.getParameter("register")!=null)
            {
                //    ������� ���������
                String username = getNotEmpty(request, "register", true);
                String password = getNotEmpty(request, "password", true);
                boolean remember = getFlag(request, "remember", true);
                //    ���������������� ������������
                dataAccess.register(username, password);
                //    �������� � ������ � ��� �������� �������
                user = new User ();
                user.name = username;
                session.setAttribute("user", user);
                if (remember)  session.setMaxInactiveInterval(-1);  // ����������� ������, ���� ������ ����
            }
            //    ��������� login action
            if (request.getParameter("login")!=null)
            {
                //    ������� ���������
                String username = getNotEmpty(request, "login", true);
                String password = getNotEmpty(request, "password", true);
                boolean remember = getFlag(request, "remember", true);
                //    ��������� �����-������
                dataAccess.login(username, password);
                //    �������� � ������ � ��� �������� �������
                user = new User ();
                user.name = username;
                session.setAttribute("user", user);
                if (remember)  session.setMaxInactiveInterval(-1);  // ����������� ������, ���� ������ ����
            }
            //    ��������� logout action
            if (request.getParameter("logout")!=null)
            {
                session.removeAttribute("user");
                user = null;
            }
        }
        catch (ClientException e)  {  error = e.getUserMessage();  }
        catch (Exception e)  {  logger.log(e);  error = "Internal server error";  }

        //        ----    ��������� ajax actions    ----
        String action = request.getParameter("action");
        if (action!=null)
        {
            String result = "success:";
            try
            {
                //    ������������� ����
                path = Util.cutIfEnds(path, "/");
                if (!path.equals(path.toLowerCase()))  throw new ClientException ("Path must be lowercase: "+path);
                //    ��������� ���������� �����
                if (action.equals("saveplan"))
                {
                    String planContent = getPost(request);
                    //    ���������
                    dataAccess.savePlanContent(user, path, planContent);
                }
                //    �������� ����
                else if (action.equals("addplan"))
                {
                    String name = getNotEmpty(request, "name", false);
                    String title = getNotEmpty(request, "title", false);
                    dataAccess.addPlan(user, path, name, title);
                }
                //    ������� ����
                else if (action.equals("deleteplan"))
                {
                    dataAccess.deletePlan(user, path);
                }
                //    ������� ����
                else if (action.equals("editplan"))
                {
                    String name = getNotEmptyIfExists(request, "name", false);
                    String title = getNotEmpty(request, "title", false);
                    dataAccess.editPlan(user, path, name, title);
                }
                //    ������������ action
                else
                    throw new ClientException ("Unknow 'action' parameter value: "+action);
            }
            //    error
            catch (ClientException e)  {
                logger.log(e.getMessage());
                result = (e.user ? "error:user:" : "error:client:") + e.getMessage();
                response.setStatus(e.statusCode);
            }
            catch (Exception e)  {
                logger.log(e);
                result = "error:server:"+e.toString();
                response.setStatus(500);
            }
            //    write result
            response.setContentType("text/plain;charset="+AJAX_ENCODING);
            response.getOutputStream().write(result.getBytes(AJAX_ENCODING));
            return true;
        }

        //        ----    ������� html �����    ----
        String planHeader = null;
        String planContent = null;
        PlanNode[] childs = null;
        PlanNode parent = null;

        if (error!=null)  {
            response.setStatus(500);
        }
        else try
        {
            //    redirect, ��� ������������ path
            String lowerCasePath = path.toLowerCase();
            if (!path.equals(lowerCasePath))  {
                response.setStatus(301);
                response.setHeader("Location", context+lowerCasePath);
                return true;
            }
            path = Util.cutIfEnds(path, "/");  // �� ������ �� ����� ���� ����� ���� �� �����������, �� ��� ������ ����� ����������� ��������� ���������, ���� ��� ������������� �� �����������
            //    �������� ������������
            if (user!=null)
            {
                String userPath = "/"+user.name.toLowerCase();
                if (path.equals("") || path.equals("/index.html"))  path = userPath;
                //    � ��������� ���� ����� ��������������� ���� � �������
                planContent = dataAccess.loadPlanContent(user, path);
                if (planContent==null)  {
                    response.setStatus(404);
                    throw new ClientException ("Wrong link to resource "+path, true);
                }
                //    ��������� ����-���������� � ��������� ����� � ��� ���������������� �����,
                //    ���� �� ����� ��������, �� ����� ��� ���������� � ��� ��������
                String loadPlanPath = path.substring(user.name.length()+1);
                int p = loadPlanPath.lastIndexOf('/');
                PlanNode plan = dataAccess.loadPlans(user, p!=-1 ? path.substring(0, path.lastIndexOf('/')) : path, p!=-1 ? 2 : 1);
                if (p!=-1)  {  parent = plan;  plan = plan.getChild(loadPlanPath.substring(p + 1));  }
                planHeader = plan.title;  //loadPlans ����� ������� null � ������ ������ �������, ����� ������� NullPointerException
                if (plan.childs!=null)  childs = plan.childs.toArray(new PlanNode [plan.childs.size()]);
            }
            //    �������� ������
            else
            {
                //    ���� ������������� �� �������� ��������, �� ���������� ������ 403
                if (!path.equals("/") && !path.equals("/index.html"))  {
                    response.setStatus(403);
                }
            }
        }
        catch (ClientException e)  {  error = e.getUserMessage();  }
        catch (Exception e)  {  logger.log(e);  response.setStatus(500);  error = "Internal server error";  }

        //    set bindings
        Bindings bindings = new SimpleBindings();
        bindings.put("app", context);
        bindings.put("path", path);
        bindings.put("error", error);
        bindings.put("user", user);
        bindings.put("header", planHeader);
        bindings.put("planlist", planContent);
        bindings.put("childs", childs);
        bindings.put("parent", parent);
        //    eval
        StringList buffer = new StringList ();
        templateManager.eval(user != null ? "/planlist.html" : "/login.html", bindings, buffer);
        //    write
        response.setContentType("text/html;charset="+encoding);
        response.getOutputStream().write(buffer.toString().getBytes(encoding));
        return true;
    }


    //    util

//    public static String getIfExists(HttpServletRequest request, String name) throws ClientException
//    {
//        return request.getParameter(name);
//    }

    public static String get(HttpServletRequest request, String name) throws ClientException
    {
        String value = request.getParameter(name);
        if (value==null)  throw new ClientException("No \""+name+"\" parameter");
        return value;
    }

    public static String getNotEmpty(HttpServletRequest request, String name, boolean user) throws ClientException
    {
        String value = get(request, name);
        notEmpty(value, name, user);
        return value;
    }

    public static String getNotEmptyIfExists(HttpServletRequest request, String name, boolean user) throws ClientException
    {
        String value = request.getParameter(name);
        if (value!=null)  notEmpty(value, name, user);
        return value;
    }

    public static void notEmpty(String value, String name, boolean user) throws ClientException
    {
        if (value.length()==0)  throw new ClientException("Empty \""+name+"\" parameter", user);
    }

    public static boolean getFlag(HttpServletRequest request, String name, boolean user) throws ClientException
    {
        String value = request.getParameter(name);
        if (value==null)  return false;
        String v = value.toLowerCase();
        if (v.equals("true") || v.equals("on") || v.equals("yes") || v.equals("1"))  return true;
        if (v.equals("false") || v.equals("off") || v.equals("no") || v.equals("0"))  return true;
        throw new ClientException("Wrong \""+name+"\" parameter value: "+value, user);
    }

    public static String getPost(HttpServletRequest request) throws IOException  {
        return Util.read(request.getInputStream(), AJAX_ENCODING);
    }
}
