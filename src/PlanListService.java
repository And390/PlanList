import template.TemplateManager;
import template.WatchFileTemplateManager;
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

/**
 * And390 - 09.03.15.
 */
public class PlanListService implements Filter
{
    // TODO ���� ������ ����������� ����������� ������, �� ��� ����������� ����� ������ ��������� (401)

    public static String AJAX_ENCODING = "UTF-8";

    private Logger logger = Logger.console;
    private ServletContext servletContext;
    private String encoding;
    private WatchFileTemplateManager templateManager;
    private DataAccess dataAccess;
    private String webDataDir;  // �������� ���� � data ������������ ����� ���-����������, ���� data ������ ����

    @Override
    public void init(FilterConfig config) throws ServletException
    {
        try  {
            servletContext = config.getServletContext();
            encoding = config.getInitParameter("encoding");
            if (encoding==null)  encoding = "UTF-8";
            templateManager = new WatchFileTemplateManager(new File(config.getServletContext().getRealPath("")), encoding);

            // ��������� data, ���� ���������� � '/', '.' ��� '..', �� ��������� ���������� ����� ��� ����� ������������ ����� ������� (�������)
            // ����� ��������� ����� ������������ ����� �������� ���-����������
            String data = config.getInitParameter("data");
            if (Util.isEmpty(data))  data = "data";
            if (!data.startsWith(".") && !new File (data).isAbsolute())  data = servletContext.getRealPath("/"+data);
            webDataDir = Util.subFilePath(servletContext.getRealPath("/"), data);
            dataAccess = new DataAccess (data);
        }
        catch (IOException e)  {  destroy();  throw new ServletException (e);  }
    }

    @Override
    public void destroy()
    {
        try  {
            if (templateManager!=null)  templateManager.close();
            TemplateManager.free();
        }
        catch (IOException|InterruptedException e)  {  throw new RuntimeException (e);  }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String context = request.getContextPath();
        String path = request.getRequestURI().substring(context.length());
        path = java.net.URLDecoder.decode(path, "UTF-8");
        if (!path.startsWith("/"))  path = '/'+path;
        //    try process
        try  {  if (processRequest(request, response, path))  return;  }
        catch (IOException|ServletException|RuntimeException e)  {  throw e;  }
        catch (Exception e)  {  throw new ServletException (e);  }
        //    default process if function returns false
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public boolean processRequest(HttpServletRequest request, HttpServletResponse response, String path) throws Exception
    {
        //    ��-html ����� �� �� �������� data � �������� ����������� �������� �� ���������
        if (!path.equals("/") && !(webDataDir!=null && Util.startsWithPath(path, webDataDir))
                && path.indexOf('.')!=-1 && !path.endsWith(".html"))  {
            return false;
        }
        //    ������������� ����
        path = Util.cutIfEnds(path, "/");

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
                String username = getNotEmpty(request, "register");
                String password = getNotEmpty(request, "password");
                boolean remember = getFlag(request, "remember");
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
                String username = getNotEmpty(request, "login");
                String password = getNotEmpty(request, "password");
                boolean remember = getFlag(request, "remember");
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
        catch (UserException e)  {  error = e.getMessage();  }
        catch (Exception e)  {  logger.log(e);  error = "Internal server error";  }

        //        ----    ��������� ajax actions    ----
        String action = request.getParameter("action");
        if (action!=null)
        {
            String result = "success:";
            try
            {
                //    ��������� ���������� �����
                if (action.equals("saveplan"))
                {
                    String planContent = getPost(request);
                    //    ���������
                    path = Util.cutIfEnds(path, "/");
                    if (!dataAccess.savePlanContent(user, path, planContent))  throw new UserException ("Plan does not exists", 404);
                }
                //    �������� ����
                else if (action.equals("addplan"))
                {
                    String name = getNotEmpty(request, "name");
                    String title = getNotEmpty(request, "title");
                    dataAccess.addPlan(user, path, name, title, "");
                }
                //    ������� ����
                else if (action.equals("deleteplan"))
                {
                    dataAccess.deletePlan(user, path);
                }
                //    ������� ����
                else if (action.equals("editplan"))
                {
                    String name = getNotEmpty(request, "name");
                    String title = getNotEmpty(request, "title");
                    dataAccess.editPlan(user, path, name, title);
                }
                //    ������������ action
                else
                    throw new ClientException ("Unknow 'action' parameter value: "+action);
            }
            //    error
            catch (ClientException e)  {
                logger.log(e.getMessage());
                result = (e instanceof UserException ? "error:user:" : "error:client:") + e.getMessage();
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
            //    �������� ������������
            if (user!=null)
            {
                String userPath = "/"+user.name.toLowerCase();
                if (path.equals("") || path.equals("/index.html"))  path = userPath;
                //    � ��������� ���� ����� ��������������� ���� � �������
                planContent = dataAccess.loadPlanContent(user, path);
                if (planContent==null)  {
                    response.setStatus(404);
                    throw new UserException("Wrong link to resource "+path);
                }
                //    ��������� ����-���������� � ��������� ����� � ��� ���������������� �����, ���� �� �� ����� ��������,
                //    ���� �����, �� ����� ��� ���������� � ��� ��������
                String loadPlanPath = path.substring(user.name.length()+1);
                int p = loadPlanPath.lastIndexOf('/');
                PlanNode plan = dataAccess.loadPlans(user.name, p!=-1 ? loadPlanPath.substring(0, p) : loadPlanPath, p!=-1 ? 2 : 1);
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
        catch (UserException e)  {  error = e.getMessage();  }
        catch (Exception e)  {  logger.log(e);  response.setStatus(500);  error = "Internal server error";  }

        //    set bindings
        Bindings bindings = new SimpleBindings();
        bindings.put("app", request.getContextPath());
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

    public static class User
    {
        public String name;
    }



    //    util

    public static String get(HttpServletRequest request, String name) throws ClientException
    {
        String value = request.getParameter(name);
        if (value==null)  throw new ClientException("No \""+name+"\" parameter");
        return value;
    }

    public static String getNotEmpty(HttpServletRequest request, String name) throws ClientException
    {
        String value = get(request, name);
        if (value.length()==0)  throw new UserException("Empty \""+name+"\" parameter");
        return value;
    }

    public static boolean getFlag(HttpServletRequest request, String name) throws ClientException
    {
        String value = request.getParameter(name);
        if (value==null)  return false;
        String v = value.toLowerCase();
        if (v.equals("true") || v.equals("on") || v.equals("yes") || v.equals("1"))  return true;
        if (v.equals("false") || v.equals("off") || v.equals("no") || v.equals("0"))  return true;
        throw new UserException("Wrong \""+name+"\" parameter value: "+value);
    }

    public static String getPost(HttpServletRequest request) throws IOException  {
        return Util.read(request.getInputStream(), AJAX_ENCODING);
    }
}
