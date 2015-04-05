import utils.Util;
import web.WebTemplateFilter;
import web.WebTemplateManager;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * And390 - 09.03.15.
 */
public class PlanListService extends WebTemplateFilter
{
    // TODO если давать возможность расшаривать ссылки, то для поисковиков лучше делать редиректы (401)

    public static String AJAX_ENCODING = "UTF-8";

    public static final String DATA = "/data";  // путь к каталогу с данными пользователей

    private DataAccess dataAccess;

    @Override
    public void init(FilterConfig config) throws ServletException
    {
        super.init(config);
        dataAccess = new DataAccess (config.getServletContext().getRealPath(DATA));
    }

    @Override
    public boolean processRequest(HttpServletRequest request, HttpServletResponse response, String path) throws Exception
    {
        //    не-html файлы не из каталога data с непустым расширением отдаются по умолчанию
        if (!path.equals("/") && !Util.startsWithPath(path, DATA) && path.indexOf('.')!=-1 && !path.endsWith(".html"))  {
            return false;
        }
        //    нормализовать путь
        path = Util.cutIfEnds(path, "/");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        String error = null;

        //    обработка общих action (которые идут через html форму, но поддерживаются и через ajax
        //    маркер - специальный параметр (сейчас это только login и logout)
        try  {
            //    обработка register action
            if (request.getParameter("register")!=null)
            {
                //    достать параметры
                String username = getNotEmpty(request, "register");
                String password = getNotEmpty(request, "password");
                //    зарегистрировать пользователя
                dataAccess.register(username, password);
                //    положить в сессию и для текущего запроса
                user = new User ();
                user.name = username;
                session.setAttribute("user", user);
            }
            //    обработка login action
            if (request.getParameter("login")!=null)
            {
                //    достать параметры
                String username = getNotEmpty(request, "login");
                String password = getNotEmpty(request, "password");
                //    проверить логин-пароль
                dataAccess.login(username, password);
                //    положить в сессию и для текущего запроса
                user = new User ();
                user.name = username;
                session.setAttribute("user", user);
            }
            //    обработка logout action
            if (request.getParameter("logout")!=null)
            {
                session.removeAttribute("user");
                user = null;
            }
        }
        catch (UserException e)  {  error = e.getMessage();  }
        catch (Exception e)  {  log(e);  error = "Internal server error";  }

        //        ----    обработка ajax actions    ----
        String action = request.getParameter("action");
        if (action!=null)
        {
            String result = "success:";
            try
            {
                //    сохранить содержимое плана
                if (action.equals("saveplan"))
                {
                    String planContent = getPost(request);
                    //    сохранить
                    path = Util.cutIfEnds(path, "/");
                    if (!dataAccess.savePlanContent(user, path, planContent))  throw new UserException ("Plan does not exists", 404);
                }
                //    добавить план
                else if (action.equals("addplan"))
                {
                    String name = getNotEmpty(request, "name");
                    String title = getNotEmpty(request, "title");
                    dataAccess.addPlan(user, path, name, title, "");
                }
                //    удалить план
                else if (action.equals("deleteplan"))
                {
                    dataAccess.deletePlan(user, path);
                }
                //    удалить план
                else if (action.equals("editplan"))
                {
                    String name = getNotEmpty(request, "name");
                    String title = getNotEmpty(request, "title");
                    dataAccess.editPlan(user, path, name, title);
                }
                //    неиззвестный action
                else
                    throw new ClientException ("Unknow 'action' parameter value: "+action);
            }
            //    error
            catch (ClientException e)  {
                log(e.getMessage());
                result = (e instanceof UserException ? "error:user:" : "error:client:") + e.getMessage();
                response.setStatus(e.statusCode);
            }
            catch (Exception e)  {
                log(e);
                result = "error:server:"+e.toString();
                response.setStatus(500);
            }
            //    write result
            response.setContentType("text/plain;charset="+AJAX_ENCODING);
            response.getOutputStream().write(result.getBytes(AJAX_ENCODING));
            return true;
        }

        //        ----    вернуть html ответ    ----
        String planHeader = null;
        String planContent = null;
        PlanNode[] childs = null;
        PlanNode parent = null;

        if (error!=null)  {
            response.setStatus(500);
        }
        else try
        {
            //    страница пользователя
            if (user!=null)
            {
                String userPath = "/"+user.name.toLowerCase();
                if (path.equals("") || path.equals("/index.html"))  path = userPath;
                //    с проверкой прав найти соответствующий план и вернуть
                planContent = dataAccess.loadPlanContent(user, path);
                if (planContent==null)  {
                    response.setStatus(404);
                    throw new UserException("Wrong link to resource "+path);
                }
                //    прочитать мета-информацию о выбранном плане и его непосредственных детях, если он не имеет родителя,
                //    если имеет, то нужна еще информация о его родителе
                String loadPlanPath = path.substring(user.name.length()+1);
                int p = loadPlanPath.lastIndexOf('/');
                PlanNode plan = dataAccess.loadPlans(user.name, p!=-1 ? loadPlanPath.substring(0, p) : loadPlanPath, p!=-1 ? 2 : 1);
                if (p!=-1)  {  parent = plan;  plan = plan.getChild(loadPlanPath.substring(p + 1));  }
                planHeader = plan.title;  //loadPlans может вернуть null в случае битого конфига, тогда получим NullPointerException
                if (plan.childs!=null)  childs = plan.childs.toArray(new PlanNode [plan.childs.size()]);
            }
            //    страница логина
            else
            {
                //    если запрашивается не корневая страница, то установить статус 403
                if (!path.equals("/") && !path.equals("/index.html"))  {
                    response.setStatus(403);
                }
            }
        }
        catch (UserException e)  {  error = e.getMessage();  }
        catch (Exception e)  {  log(e);  response.setStatus(500);  error = "Internal server error";  }

        Bindings bindings = new SimpleBindings();
        bindings.put("context", request.getContextPath());
        bindings.put("path", path);
        bindings.put("error", error);
        bindings.put("user", user);
        bindings.put("header", planHeader);
        bindings.put("planlist", planContent);
        bindings.put("childs", childs);
        bindings.put("parent", parent);
        WebTemplateManager.instance.eval(user!=null ? "/planlist.html" : "/login.html", bindings, response);
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

    public static String getPost(HttpServletRequest request) throws IOException  {
        return Util.read(request.getInputStream(), AJAX_ENCODING);
    }
}
