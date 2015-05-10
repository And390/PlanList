import utils.ByteArray;
import utils.RuntimeAppendable;
import utils.StringList;
import utils.Util;
import utils.objects.Consumer;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * And390 - 18.03.15.
 * Здесь все функции по получению и работе с данными (модель).
 * Также, все проверки ввода пользователя, типа неправильных символов.
 * Имена пользователей и планов здесь приводятся к нижнему регистру (остальная часть программы к ним регистронечувствительна)
 * Все пути уже должны передаваться в нижнем регистре (вообще, вместе с именами это както не очень логично получается).
 */
@SuppressWarnings("unused")
public class DataAccess
{
    public static final String PLAN_ENCODING = "UTF-8";


    private final String path;  // with ending slash

    public DataAccess(String path_) throws IOException  {
        if (!new File (path_).isDirectory())  throw new FileNotFoundException ("Directory does not exists: "+path_);
        if (!path_.endsWith("/"))  path_=path_+"/";  path=path_;
    }



    //        ----    common    ----

    private File getUserDir(String username)  {  return new File (path+username);  }
    private File getUserFile(String username, String filename)  {  return new File (path+username+"/"+filename);  }


    //        ----    функции работы с планами    ----

    // делает все проверки и добавляет план
    public void addPlan(PlanListService.User user, String parentFullPath, String name_, String title_, String content) throws UserException, IOException
    {
        //    проверки
        final String name = checkPlanName(name_);
        final String title = checkPlanTitle(title_);
        checkPlanOwner(user, parentFullPath);
        //    разложить полный путь на имя и путь относительно пользователя
        int s = Util.indexOf(parentFullPath, '/', 1);
        final String username = parentFullPath.substring(1, s);
        final String parentPath = parentFullPath.substring(s);
        final String planPath = parentPath+"/"+name;
        //    переписать файл plans
        File userDir = getUserDir(username);
        processPlans(userDir, parentFullPath, new PlansProcessor()  {
            int parentFinded = -2;  // -1 в level передает PlansParser в качестве маркера конца, поэтому используем -2
            public void process (String path, int level, String name_, String title_)  throws UserException  {
                if (level<=parentFinded)  {
                    //    добавить строку с новым планом
                    append(parentFinded+1, name, title);
                    parentFinded = -2;
                    find = true;
                }
                if (path.equals(parentPath))  parentFinded = level;
                if (path.equals(planPath))  throw new UserException("Plan already exists: " + path);
            }
        });
        //    записать .txt файл, создав каталог, если надо
        File contentDir = new File (userDir, parentPath);
        if (!contentDir.exists())  Util.mkdir(contentDir);
        Util.write(new File (contentDir, name+".txt"), content, PLAN_ENCODING);
    }

    // делает все проверки и удаляет план
    public void deletePlan(PlanListService.User user, String fullPath) throws UserException, IOException
    {
        //    проверки
        if (fullPath.indexOf('/', 1)==-1)  throw new UserException ("It is not possible to delete root plan");
        checkPlanOwner(user, fullPath);
        //    разложить полный путь на имя и путь относительно пользователя
        int s = Util.indexOf(fullPath, '/', 1);
        final String username = fullPath.substring(1, s);
        final String planPath = fullPath.substring(s);
        //    переписать файл plans
        File userDir = getUserDir(username);
        processPlans(userDir, fullPath, new PlansProcessor()  {
            int finded = -2;  // -1 в level передает PlansParser в качестве маркера конца, поэтому используем -2
            public void process (String path, int level, String name_, String title_)  throws UserException  {
                if (level<=finded)  {  finded = -2;  find = true;  }
                if (path.equals(planPath))  finded = level;
                copy = finded==-2;
            }
        });
        //    удалить .txt файл и каталог, если существует
        Util.delete(new File (userDir, planPath+".txt"));
        new File (userDir, planPath).delete();
    }

    // делает все проверки и изменяет имя и заголовок плана
    // если новое имя = null, то остается старое
    public void editPlan(PlanListService.User user, String fullPath, String name_, String title_) throws UserException, IOException
    {
        //    проверки
        int l = fullPath.lastIndexOf('/');       //указатель на последний слэш
        int p = l==0 ? fullPath.length() : l+1;  //указатель на начало имени плана в строке fullPath
        if (l==-1 || !fullPath.startsWith("/"))  throw new IllegalArgumentException ("Wrong fullPath: "+fullPath);
        if (l==0 && name_!=null)  throw new UserException ("It is not possible to change root plan name");
        final String name = name_!=null ? checkPlanName(name_) : fullPath.substring(p);
        final String title = checkPlanTitle(title_);
        checkPlanOwner(user, fullPath);
        //    разложить полный путь на имя и путь относительно пользователя
        int s = Util.indexOf(fullPath, '/', 1);
        final String username = fullPath.substring(1, s);
        final String oldPlanPath = fullPath.substring(s);
        final String newPlanPath = Util.startsWithPath(fullPath, name, p) ? null : fullPath.substring(s, p) + name;  // null, если имена эквивалентны
        //    переписать файл plans
        File userDir = getUserDir(username);
        processPlans(userDir, fullPath, new PlansProcessor()  {
            public void process (String path, int level, String name_, String title_)  throws UserException  {
                copy = true;
                if (path.equals(newPlanPath))  throw new UserException("Plan already exists: " + path);
                if (path.equals(oldPlanPath))  {
                    append(level, name, title);
                    find = true;
                    copy = false;
                }
            }
        });
        //    переименовать .txt и каталог
        if (newPlanPath!=null)  {
            Util.renameTo(new File (userDir, oldPlanPath+".txt"), new File (userDir, newPlanPath+".txt"));
            File oldPanDir = new File (userDir, oldPlanPath);
            if (oldPanDir.exists())  Util.renameTo(oldPanDir, new File (userDir, newPlanPath));
        }
    }

    // читает plans файл, парсит, ищет в нем указанную строку, параллельно переписывая контент, и сохраняет
    private static void processPlans(File userDir, String planPath, PlansProcessor processor) throws UserException, IOException
    {
        //    прочитать plans файл
        File plansFile = new File (userDir, "plans");
        String plansContent = Util.read(plansFile, PLAN_ENCODING);
        //    разобрать, формируя результат
        final StringList output = new StringList ();
        processor.output = output;
        parsePlans(plansContent, processor);
        if (!processor.find)  throw new UserException ("Plan is not found: "+planPath, 404);
        //    записать plans файл
        Util.write(plansFile, output.toString(), PLAN_ENCODING);
    }
    private static abstract class PlansProcessor extends PlansParser
    {
        public boolean find = false;
        public boolean copy = true;
        public RuntimeAppendable output = null;

        @Override
        public void process(String row) throws RuntimeException, UserException
        {
            super.process(row);
            // скопировать текущую строку, если указано
            if (copy)  output.append(row).append('\n');
        }

        public void append(int level, String name, String title)  {
            output.append(Util.fillChars(' ', level)).append(name).append("\t").append(title).append('\n');
        }
    }

    public String loadPlanContent(PlanListService.User user, String planPath) throws IOException, UserException
    {
        checkPlanOwner(user, planPath);
        return loadPlanContent(planPath);
    }
    private String loadPlanContent(String planPath) throws IOException
    {
        String fullPath = path.substring(0, path.length()-1) + planPath + (planPath.indexOf('/', 1)==-1 ? "/.txt" : ".txt");
        try  {  return Util.read(fullPath, PLAN_ENCODING);  }
        catch (FileNotFoundException e)  {  return null;  }
    }

    public boolean savePlanContent(PlanListService.User user, String planPath, String content) throws IOException, UserException
    {
        checkPlanOwner(user, planPath);
        return savePlanContent(planPath, content);
    }
    private boolean savePlanContent(String planPath, String content) throws IOException
    {
        String fullPath = path.substring(0, path.length()-1) + planPath + (planPath.indexOf('/', 1)==-1 ? "/.txt" : ".txt");
        try  {  Util.write(fullPath, content, PLAN_ENCODING);  return true;  }
        catch (FileNotFoundException e)  {  return false;  }
    }

    private static String checkPlanName(String name) throws UserException
    {
        if (name.equals(""))  throw new UserException ("Empty plan name");
        if (name.length()>=256)  throw new UserException("Plan name must be less than 256 characters in length");
        for (char c : name.toCharArray())  if (!Character.isLetterOrDigit(c) && c!='.' && c!='-' && c!='_')
            throw new UserException("Plan name must contains only letters, digits, '.', '-' or '_'");
        return name.toLowerCase();
    }

    private static String checkPlanTitle(String title) throws UserException
    {
        for (char c : title.toCharArray())  if (c=='\t' || c=='\n' || c=='\r')
            throw new UserException("Plan title must not contain tab and line breaks");
        return title;
    }

    // TODO плохо, что в DataAccess коды http
    private void checkPlanOwner(PlanListService.User user, String planPath) throws UserException
    {
        if (user==null)  throw new UserException ("not logged in", 403);
        String planUser = planPath.substring(1, Util.indexOf(planPath, '/', 1));
        if (!planUser.equals(user.name))  throw new UserException ("You have no access to other user's plans", 403);
    }

    private static void parsePlans(String plansContent, PlansParser parser) throws IOException, UserException
    {
        plansContent = Util.cutIfEnds(plansContent, "\n");
        Util.slice(plansContent, '\n', parser);
        parser.process(".", -1, null, null);
    }
    private static abstract class PlansParser implements Consumer<String, UserException>
    {
        boolean parentFinded = false;
        StringList pathBuffer = new StringList ();
        int line = 0;

        public void process(String row) throws UserException
        {
            line++;
            //
            int i = 0;
            int level = 0;
            for (; i<row.length() && row.charAt(i)==' '; i++)  level++;
            //
            String[] values = Util.slice(row.substring(i), '\t');
            if (values.length!=2)  throw new RuntimeException ("Wrong 'plans' file, line "+line+" has "+(values.length==1 ? "no" : "too much")+" tab characters");
            //    проверки корневой записи
            if (line==1)  {
                if (level!=0)  throw new RuntimeException ("Wrong 'plans' file, first line must be root (non-indented)");
                if (!values[0].equals(""))  throw new RuntimeException ("Wrong 'plans' file, first line (root) must have empty name");
            }
            else  {
                if (level==0)  throw new RuntimeException ("Wrong 'plans' file, root node (non-indented) on line "+line);
                if (values[0].equals(""))  throw new RuntimeException ("Wrong 'plans' file, line "+line+" has empty name");
            }
            //    разобрать уровень вложенности
            int prevLevel = pathBuffer.size() / 2;
            if (level<prevLevel)  {  pathBuffer.setSize(level * 2);  }
            if (level<=prevLevel)  {  if (level!=0)  pathBuffer.set(pathBuffer.size()-1, values[0]);  }
            else if (level==prevLevel+1)  {  pathBuffer.append("/").append(values[0]);  }
            else  throw new RuntimeException ("Wrong 'plans' file, too much white spaces at start on line "+line+", previous level "+prevLevel+", found "+level);
            //    обработать
            process(pathBuffer.toString(), level, values[0], values[1]);
        }

        public abstract void process(String path, int level, String name, String title) throws UserException;
    }

    // сохранение файла plans
    private void savePlans(String username, PlanNode root) throws IOException
    {
        String content = writePlans(root);
        Util.write(new File (getUserDir(username), "plans"), content, PLAN_ENCODING);
    }
    private static String writePlans(PlanNode root) throws IOException
    {
        StringList buffer = new StringList ();
        writePlans(root, "", buffer);
        return buffer.toString();
    }
    private static void writePlans(PlanNode node, String indent, Appendable out) throws IOException
    {
        out.append(indent).append(node.name).append("\t").append(node.title).append("\n");
        if (node.childs!=null)  for (PlanNode child : node.childs)  writePlans(child, indent+" ", out);
    }

    // # блин, какой-то усложненный на ровном месте алгоритм получился, я таких не пишу обычно :)
    // загружает информацию о плане по указанному пути и рекурсивно о всех его детях, не больше глубины рекурсии depth
    // для глубины рекурсии 0 загружается только план по указанному пути, 1 - плюс все его непосредственные дети, и т.д.
    public PlanNode loadPlans(String planPath, int depth) throws IOException
    {
        if (!planPath.startsWith("/"))  throw new IllegalArgumentException ("planPath must starts with '/'");
        int i = Util.indexOf(planPath, '/', 1);
        return loadPlans(planPath.substring(0, i), planPath.substring(i), depth);
    }
    // корневой план имеет путь "", дочерний корневого - "/child", его дочерний - "/child/sub"
    public PlanNode loadPlans(String username, String path, int depth) throws IOException
    {
        String content = Util.read(new File (getUserDir(username), "plans"), PLAN_ENCODING);
        content = Util.cutLastNL(content);
        return parsePlans(content, new int[]{0}, new int[]{1}, 0, path, depth);
    }
    // Считывает строку из content с позиции pos[0] и если уровень вложенности равен level, то разбирает запись,
    // перемещает позицию на следующую строку и вызывает рекурсиный разбор для дочерних записей,
    // если уровень меньше, то возвращает возвращает null, позиция не меняется, если уровень больше, то ошибка.
    // В row[0] передается и увеличивается номер строки для отладки.
    // Параметры path и depth аналогичны таковым в предыдущей функции.
    // Если depth<0, то режим игнорирования, для разобранной записи возвращается UNLOADED_PLAN_NODE.
    // Если path="", то для разобранной записи возвращается объект PlanNode рекурсивно вместе с детьми, если они проходят по depth.
    // Иначе для непустого path рекурсивные вызовы вернут ветку по указанному path, если она будет найдена, или null.
    private static PlanNode parsePlans(String content, int[] pos, int[] row, int level, String path, int depth)
    {
        //    по количеству пробелов в начале строки определить уровень вложенности
        int i1 = pos[0];
        while (i1<content.length() && content.charAt(i1)==' ')  i1++;
        int lev = i1 - pos[0];
        //    если уровень равен ожидаемому - разбираем дальше
        if (lev==level)  {
            //    разобрать текущую строку (после пробелов)
            int i2 = Util.indexOf(content, '\n', i1);
            String[] values = Util.slice(content.substring(i1, i2), '\t');
            if (values.length!=2)  throw new RuntimeException ("Wrong 'plans' file, line "+row[0]+" has "+(values.length==1 ? "no" : "too much")+" tab characters");
            if (pos[0]==0 && !values[0].equals(""))  throw new RuntimeException ("Wrong 'plans' file, root record must have empty name");
            if (pos[0]!=0 && values[0].equals(""))  throw new RuntimeException ("Wrong 'plans' file, line "+row[0]+" has empty name");
            //    переход к следующей строке
            pos[0] = i2 + 1;
            row[0]++;
            //    если указан не пустой путь и не корень
            if (depth>=0 && path.length()!=0 && !values[0].equals(""))  {
                //    если путь начинается со считанного имени
                if (path.charAt(0)=='/' && Util.startsWithPath(path, values[0], 1))
                    //    то обрубить путь
                    path = path.substring(1+values[0].length());
                //    иначе переключиться в режим игнорирования
                else  depth = -1;
            }
            //    в режиме игнорирования разобрать детей и вернуть UNLOADED_PLAN_NODE
            if (depth<0)  {
                while (parsePlans(content, pos, row, level + 1, null, -1)!=null)  ;
                return UNLOADED_PLAN_NODE;
            }
            //    path="" - сохраняем узел и парсим детей
            else if (path.equals(""))  {
                PlanNode result = new PlanNode ();
                result.name = values[0];
                result.title = values[1];
                //    рекурсивно обработать дочерние записи
                for (;;)  {
                    PlanNode child = parsePlans(content, pos, row, level + 1, path, depth - 1);
                    if (child==null)  break;
                    if (child!=UNLOADED_PLAN_NODE)  {
                        if (result.childs==null)  result.childs = new ArrayList<> ();
                        result.childs.add(child);
                    }
                }
                return result;
            }
            //    иначе спуск по текущей ветке, поиск результата среди детей
            else  {
                for (;;)  {
                    PlanNode result = parsePlans(content, pos, row, level + 1, path, depth);
                    if (result==null)  return null;  // конец этого уровня вложенности, выходим
                    if (result!=UNLOADED_PLAN_NODE)  return result;  // нашли нужную ветку, дальше можно не парсить
                }
            }
        }
        //    уровень больше - ошибка
        else if (lev>level)  throw new RuntimeException ("Wrong 'plans' file, too much white spaces at start on line "+row[0]+", level="+level+", but found "+lev);
        //    уровень меньше - выход
        else  return null;
    }
    private static PlanNode UNLOADED_PLAN_NODE = new PlanNode ();
    // test it
    private static class TestParsePlans
    {
        public static boolean equals(Object o1, Object o2)  {
            return o1!=null && o2!=null ? o1.equals(o2) : o1==null && o2==null;
        }
        public static void test(String content, String error)  {
            try  {  PlanNode result = parsePlans(content, new int[]{0}, new int[]{1}, 0, "", Integer.MAX_VALUE);
                    System.err.print("result:\n" + result + "\nbut expected error: " + error + "\n");  }
            catch (Exception e)  {  if (!equals(e.getMessage(), error))  System.err.print("error: " + e.getMessage() + "\nbut expected: " + error + "\n");  }
        }
        public static void test(String content, String path, int depth, PlanNode expected)  {
            try  {
                PlanNode result = parsePlans(content, new int[]{0}, new int[]{1}, 0, path, depth);
                // первая проверка на соответствие ожидаемому
                if (!equals(result, expected))  System.err.print("result:\n" + result + "\nbut expected:\n" + expected + "\n");
                else  {
                    // вторая проверка - после записи контент результата измениться не должен
                    String writed = writePlans(parsePlans(content, new int[]{0}, new int[]{1}, 0, "", Integer.MAX_VALUE));
                    if (!content.endsWith("\n"))  content += "\n";  // нужен необязательный \n, который всегда добавляет writePlans
                    if (!writed.equals(content))  System.err.print("result:\n" + writed + "\nbut expected:\n" + content + "\n");
                }
            }
            catch (Exception e)  {  System.err.print("error: " + e.getMessage() +"\nbut expected result:\n" + expected + "\n");  }
        }
        public static void test(String content, String path, PlanNode expected)  {
            test(content, path, Integer.MAX_VALUE, expected);
        }
        public static void test(String content, PlanNode expected)  {
            test(content, "", expected);
        }
        public static void main(String[] args) throws Exception  {
            test("", "Wrong 'plans' file, line 1 has no tab characters");
            test("\t", new PlanNode ("", ""));
            test("\tRoot title", new PlanNode ("", "Root title"));
            test("\tRoot title\t", "Wrong 'plans' file, line 1 has too much tab characters");
            test("\tRoot\n", new PlanNode ("", "Root"));
            test("\tRoot\n ", "Wrong 'plans' file, line 2 has no tab characters");
            test("\tRoot\n xxx", "Wrong 'plans' file, line 2 has no tab characters");
            test("\tRoot\n  ", "Wrong 'plans' file, too much white spaces at start on line 2, level=1, but found 2");
            test("\tRoot\n plan1\t", new PlanNode ("", "Root", new PlanNode ("plan1", "")));
            test("\tRoot\n plan1\tPlan 1\n plan2\tPlan 2\n", new PlanNode ("", "Root",
                    new PlanNode ("plan1", "Plan 1"),
                    new PlanNode ("plan2", "Plan 2")
            ));
            test("\tRoot\n plan1\tPlan 1\n  sub_plan\tSub plan\n plan2\tPlan 2\n", new PlanNode ("", "Root",
                    new PlanNode ("plan1", "Plan 1",
                        new PlanNode ("sub_plan", "Sub plan")),
                    new PlanNode ("plan2", "Plan 2")
            ));
            test("\tRoot\n plan1\tPlan 1\n plan2\tPlan 2\n  plan2.1\tPlan 2.1\n  plan2.2\tPlan 2.2", new PlanNode ("", "Root",
                    new PlanNode ("plan1", "Plan 1"),
                    new PlanNode ("plan2", "Plan 2",
                        new PlanNode ("plan2.1", "Plan 2.1"),
                        new PlanNode ("plan2.2", "Plan 2.2"))
            ));
            test("\tRoot\n plan1\tPlan 1\n plan2\tPlan 2\n  plan2.1\tPlan 2.1\n    ",
                    "Wrong 'plans' file, too much white spaces at start on line 5, level=3, but found 4");
            // test path
            test("\tRoot\n plan1\tPlan 1\n plan2\tPlan 2\n  plan2.1\tPlan 2.1\n   plan2.1.1\tPlan 2.1.1\n   plan2.1.2\tPlan 2.1.2\n plan3\tPlan 3",
                    "/plan2",
                    new PlanNode ("plan2", "Plan 2",
                        new PlanNode ("plan2.1", "Plan 2.1",
                            new PlanNode ("plan2.1.1", "Plan 2.1.1"),
                            new PlanNode ("plan2.1.2", "Plan 2.1.2")
                        )
                    )
            );
            test("\tRoot\n plan1\tPlan 1\n plan2\tPlan 2\n  plan2.1\tPlan 2.1\n   plan2.1.1\tPlan 2.1.1\n   plan2.1.2\tPlan 2.1.2",
                    "/plan2/plan2.1",
                    new PlanNode ("plan2.1", "Plan 2.1",
                        new PlanNode ("plan2.1.1", "Plan 2.1.1"),
                        new PlanNode ("plan2.1.2", "Plan 2.1.2")
                    )
            );
            test("\tRoot\n plan1\tPlan 1\n plan2\tPlan 2\n  plan2.1\tPlan 2.1\n   plan2.1.1\tPlan 2.1.1\n   plan2.1.2\tPlan 2.1.2",
                    "/plan1",
                    new PlanNode ("plan1", "Plan 1")
            );
            test("\tRoot\n plan1\tPlan 1\n plan2\tPlan 2\n  plan2.1\tPlan 2.1\n   plan2.1.1\tPlan 2.1.1\n   plan2.1.2\tPlan 2.1.2",
                    "/plan3",
                    null
            );
            // test depth
            test("\tRoot\n plan1\tPlan 1\n plan2\tPlan 2\n  plan2.1\tPlan 2.1\n   plan2.1.1\tPlan 2.1.1\n   plan2.1.2\tPlan 2.1.2",
                    "/plan2", 1,
                    new PlanNode ("plan2", "Plan 2",
                        new PlanNode ("plan2.1", "Plan 2.1")
                    )
            );
            test("\tRoot\n plan1\tPlan 1\n plan2\tPlan 2\n  plan2.1\tPlan 2.1\n   plan2.1.1\tPlan 2.1.1\n   plan2.1.2\tPlan 2.1.2",
                    "/plan2", 0,
                    new PlanNode ("plan2", "Plan 2")
            );
        }
    }


    //        ----    логин и функции управления аккаунтами    ----

    // Проверяет логин и пароль
    public void login(String username, String password) throws UserException, IOException
    {
        username = username.toLowerCase();
        File passwordFile = getUserFile(username, "password");
        login(username, password, passwordFile);
    }
    private void login(String username, String password, File passwordFile) throws UserException, IOException
    {
        //    проверить, что пользователь существует (если существует его файл с паролем)
        if (!passwordFile.exists())  throw new UserException("Wrong username or password");
        //    прочитать пароль из файла
        byte[] expect = ByteArray.read(passwordFile);
        //    посчитать хеш принятого пароля и проверить
        byte[] hash = DataAccess.passwordServerHash(username, password);
        if (!Arrays.equals(hash, expect))  throw new UserException("Wrong username or password");
    }

    // Регистрирует нового пользователя
    public synchronized void register(String username, String password) throws UserException, IOException
    {
        username = username.toLowerCase();
        //    проверить новое имя
        File userDir = checkUsername(username);
        //    создать каталог пользователя
        Util.mkdir(userDir);
        //    посчитать хеш пароля, и записать в файл
        savePassword(username, password, new File (userDir, "password"));
        //    создать корневой план
        Util.write(new File (userDir, ".txt"),
                " - this is your plan example\n" +
                "   - add tasks\n" +
                "     - add sub items by indent it with spaces\n" +
                "   + mark it as completed\n" +
                "   | or set any other supported mark\n", PLAN_ENCODING);
        Util.write(new File (userDir, "plans"), "\tMy plans", PLAN_ENCODING);
    }

    // Проверяет, можно ли использовать имя пользователя. Возвращает каталог для нового пользователя (не созданный)
    private File checkUsername(String username) throws UserException, IOException
    {
        if (username.equals(""))  throw new UserException ("Empty username");
        if (username.length()>=256)  throw new UserException("Username must be less than 256 characters in length");
        for (char c : username.toCharArray())  if (!Character.isLetterOrDigit(c) && c!=' ' && c!='.' && c!='-' && c!='_')
            throw new UserException("Username must contains only letters, digits, ' ', '.', '-' or '_'");
        File userDir = getUserDir(username);
        if (userDir.exists())  throw new UserException("Username is already taken");
        return userDir;
    }

    // Меняет пароль пользователя
    public synchronized void changePassword(String username, String oldpswd, String newpswd) throws UserException, IOException
    {
        username = username.toLowerCase();
        //    проверить пользователя и старый пароль
        File userDir = getUserDir(username);
        File passwordFile = new File (userDir, "password");
        login(username, oldpswd, passwordFile);
        //    посчитать хеш пароля и сохранить в файл новый пароль
        savePassword(username, newpswd, new File (userDir, "password"));
    }

    // Меняет имя пользователя
    public synchronized void changeUsername(String oldname, String password, String newname) throws UserException, IOException
    {
        oldname = oldname.toLowerCase();
        newname = newname.toLowerCase();
        //    проверить существование старого пользователя и его пароль
        File oldUserDir = getUserDir(oldname);
        login(oldname, password, new File (oldUserDir, "password"));
        //    проверить новое имя
        File newUserDir = checkUsername(newname);
        //    переименовать каталог
        Util.renameTo(oldUserDir, newUserDir);
        //    посчитать хеш пароля, и записать в файл
        savePassword(newname, password, new File (newUserDir, "password"));
    }

    // Удаляет пользователя
    public synchronized void deleteUser(String username, String password) throws UserException, IOException
    {
        username = username.toLowerCase();
        File userDir = getUserDir(username);
        //    проверить существование старого пользователя и его пароль
        login(username, password, new File (userDir, "password"));
        //    удалить каталог
        Util.delete(userDir);
    }

    private void savePassword(String username, String password, File passwordFile) throws IOException  {
        byte[] hash = DataAccess.passwordServerHash(username, password);
        ByteArray.write(passwordFile, hash);
    }


    //        ----    вспомогательные функции хеширования паролей    ----

    private static String cryptEncoding = "UTF-8";

    public static String passwordClientHash(String username, String password)
    {
        try  {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(new PBEKeySpec(
                    password.toCharArray(), ("#"+username+"-salt!").getBytes(cryptEncoding), 100, 128)).getEncoded();
            return toHex(result);
        }
        catch (RuntimeException e)  {  throw e;  }
        catch (Exception e)  {  throw new RuntimeException (e);  }
    }

    public static byte[] passwordServerHash(String username, String password)
    {
        try  {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(new PBEKeySpec(
                    password.toCharArray(), ("#"+username+"-server-salt!").getBytes(cryptEncoding), 200, 128)).getEncoded();
            return result;  //return toHex(result);
        }
        catch (RuntimeException e)  {  throw e;  }
        catch (Exception e)  {  throw new RuntimeException (e);  }
    }

    private static String toHex(byte[] array)
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if (paddingLength > 0)  return String.format("%0"+paddingLength+"d", 0) + hex;
        else  return hex;
    }

    public static class PasswordClientHash
    {
        public static void main(String[] args) throws IOException  {
            System.out.print(passwordClientHash(args[0], args[1]));
        }
    }

    public static class PasswordServerHash
    {
        public static void main(String[] args) throws IOException  {
            System.out.write(passwordServerHash(args[0], args[1]));
        }
    }

    public static class PasswordClientServerHash
    {
        public static void main(String[] args) throws IOException  {
            System.out.write(passwordServerHash(args[0], passwordClientHash(args[0], args[1])));
        }
    }
}
