package planlist.data;

import planlist.PlanNode;
import planlist.User;
import planlist.UserException;
import utils.ByteArray;
import utils.RuntimeAppendable;
import utils.StringList;
import utils.Util;
import utils.objects.Consumer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * ����� ��� ������� �� ��������� � ������ � ������� (������).
 * �����, ��� �������� ����� ������������, ���� ������������ ��������.
 * ����� ������������� � ������ ����� ���������� � ������� �������� (��������� ����� ��������� � ��� �����������������������)
 * ��� ���� ��� ������ ������������ � ������ �������� (������, ������ � ������� ��� ����� �� ����� ������� ����������).
 * And390 - 18.03.15.
 */
@SuppressWarnings("unused")
public class FileDataAccess extends DataAccess
{
    public static final String PLAN_ENCODING = "UTF-8";


    private final String path;  // with ending slash

    public FileDataAccess(String path_) throws IOException  {
        if (!new File (path_).isDirectory())  throw new FileNotFoundException ("Directory does not exists: "+path_);
        if (!path_.endsWith("/"))  path_=path_+"/";  path=path_;
    }


    //        ----    common    ----

    private File getUserDir(String username)  {  return new File (path+username);  }
    private File getUserFile(String username, String filename)  {  return new File (path+username+"/"+filename);  }


    //        ----    ������� ������ � �������    ----

    private static final String PLANS_FILENAME = "plans";
    private static final String PASSWORD_FILENAME = "password";
    private static final String PLAN_FILE_SUFFIX = ".txt";
    private static final String PLAN_DIR_SUFFIX = ".d";
    private static final String ROOT_PLAN_FILENAME = PLAN_FILE_SUFFIX;

    private static String getPlanDirName(String planName) {
        return planName+PLAN_DIR_SUFFIX;
    }

    private static String getPlanFileName(String planName) {
        return planName+PLAN_FILE_SUFFIX;
    }

    private static String getPlanDirPath(String path) {
        if (path.equals(""))  return path;
        return path.substring(0, 1)+path.substring(1).replace("/", PLAN_DIR_SUFFIX+"/")+PLAN_DIR_SUFFIX;
    }

    private static String getPlanFilePath(String path) {
        return path.substring(0, 1)+path.substring(1).replace("/", PLAN_DIR_SUFFIX+"/")+PLAN_FILE_SUFFIX;
    }

    // ������ ��� �������� � ��������� ����
    public void addPlan(User user, String parentFullPath, String name_, String title_) throws UserException, IOException
    {
        //    ��������
        final String name = checkPlanName(name_);
        final String title = checkPlanTitle(title_);
        checkPlanOwner(user, parentFullPath);
        //    ��������� ������ ���� �� ��� � ���� ������������ ������������
        int s = Util.indexOf(parentFullPath, '/', 1);
        final String username = parentFullPath.substring(1, s);
        final String parentPath = parentFullPath.substring(s);
        final String planPath = parentPath+"/"+name;
        //    ���������� ���� plans
        File userDir = getUserDir(username);
        processPlans(userDir, parentFullPath, new PlansProcessor()  {
            int parentFinded = -2;  // -1 � level �������� PlansParser � �������� ������� �����, ������� ���������� -2
            public void process (String path, int level, String name_, String title_)  throws UserException  {
                if (level<=parentFinded)  {
                    //    �������� ������ � ����� ������
                    append(parentFinded+1, name, title);
                    parentFinded = -2;
                    find = true;
                }
                if (path.equals(parentPath))  parentFinded = level;
                if (path.equals(planPath))  throw new UserException("Plan already exists: " + path);
            }
        });
        //    �������� .txt ����, ������ �������, ���� ����
        File contentDir = new File (userDir, getPlanDirPath(parentPath));
        if (!contentDir.exists())  Util.mkdir(contentDir);
        Util.write(new File (contentDir, getPlanFileName(name)), DEFAULT_PLAN_CONTENT, PLAN_ENCODING);
    }

    // ������ ��� �������� � ������� ����
    public void deletePlan(User user, String fullPath) throws UserException, IOException
    {
        //    ��������
        if (fullPath.indexOf('/', 1)==-1)  throw new UserException ("It is not possible to delete root plan");
        checkPlanOwner(user, fullPath);
        //    ��������� ������ ���� �� ��� � ���� ������������ ������������
        int s = Util.indexOf(fullPath, '/', 1);
        final String username = fullPath.substring(1, s);
        final String planPath = fullPath.substring(s);
        //    ���������� ���� plans
        File userDir = getUserDir(username);
        processPlans(userDir, fullPath, new PlansProcessor()  {
            int finded = -2;  // -1 � level �������� PlansParser � �������� ������� �����, ������� ���������� -2
            public void process (String path, int level, String name_, String title_)  throws UserException  {
                if (level<=finded)  {  finded = -2;  find = true;  }
                if (path.equals(planPath))  finded = level;
                copy = finded==-2;
            }
        });
        //    ������� .txt ���� � �������, ���� ����������
        Util.delete(new File (userDir, getPlanFilePath(planPath)));
        File planDir = new File (userDir, getPlanDirPath(planPath));
        if (planDir.exists())  Util.delete(planDir);
    }

    // ������ ��� �������� � �������� ��� � ��������� �����
    // ���� ����� ��� = null, �� �������� ������
    public void editPlan(User user, String fullPath, String name_, String title_) throws UserException, IOException
    {
        //    ��������
        int l = fullPath.lastIndexOf('/');       //��������� �� ��������� ����
        int p = l==0 ? fullPath.length() : l+1;  //��������� �� ������ ����� ����� � ������ fullPath
        if (l==-1 || !fullPath.startsWith("/"))  throw new IllegalArgumentException ("Wrong fullPath: "+fullPath);
        if (l==0 && name_!=null)  throw new UserException ("It is not possible to change root plan name");
        final String name = name_!=null ? checkPlanName(name_) : fullPath.substring(p);
        final String title = checkPlanTitle(title_);
        checkPlanOwner(user, fullPath);
        //    ��������� ������ ���� �� ��� � ���� ������������ ������������
        int s = Util.indexOf(fullPath, '/', 1);
        final String username = fullPath.substring(1, s);
        final String oldPlanPath = fullPath.substring(s);
        final String newPlanPath = Util.startsWithPath(fullPath, name, p) ? null : fullPath.substring(s, p) + name;  // null, ���� ����� ������������
        //    ���������� ���� plans
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
        //    ������������� .txt � �������
        if (newPlanPath!=null)  {
            Util.renameTo(new File (userDir, getPlanFilePath(oldPlanPath)), new File (userDir, getPlanFilePath(newPlanPath)));
            File oldPanDir = new File (userDir, getPlanDirPath(oldPlanPath));
            if (oldPanDir.exists())  Util.renameTo(oldPanDir, new File (userDir, getPlanDirPath(newPlanPath)));
        }
    }

    // ������ plans ����, ������, ���� � ��� ��������� ������, ����������� ����������� �������, � ���������
    private static void processPlans(File userDir, String planPath, PlansProcessor processor) throws UserException, IOException
    {
        //    ��������� plans ����
        File plansFile = new File (userDir, PLANS_FILENAME);
        String plansContent = Util.read(plansFile, PLAN_ENCODING);
        //    ���������, �������� ���������
        final StringList output = new StringList ();
        processor.output = output;
        parsePlans(plansContent, processor);
        if (!processor.find)  throw new UserException ("Plan is not found: "+planPath, UserException.NOT_FOUND);
        //    �������� plans ����
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
            // ����������� ������� ������, ���� �������
            if (copy)  output.append(row).append('\n');
        }

        public void append(int level, String name, String title)  {
            output.append(Util.fillChars(' ', level)).append(name).append("\t").append(title).append('\n');
        }
    }

    public String loadPlanContent(User user, String planPath) throws IOException, UserException
    {
        checkPlanOwner(user, planPath);
        return loadPlanContent(planPath);
    }
    private String loadPlanContent(String planPath) throws IOException
    {
        int i = planPath.indexOf('/', 1);
        File userDir = getUserDir(planPath.substring(1, i==-1 ? planPath.length() : i));
        File planFile = new File (userDir, i==-1 ? ROOT_PLAN_FILENAME : getPlanFilePath(planPath.substring(i)));
        try  {  return Util.read(planFile, PLAN_ENCODING);  }
        catch (FileNotFoundException e)  {  return null;  }
    }

    public void savePlanContent(User user, String planPath, String content) throws IOException, UserException
    {
        checkPlanOwner(user, planPath);
        savePlanContent(planPath, content);
    }
    private void savePlanContent(String planPath, String content) throws IOException, UserException
    {
        int i = planPath.indexOf('/', 1);
        File userDir = getUserDir(planPath.substring(1, i==-1 ? planPath.length() : i));
        File planFile = new File (userDir, i==-1 ? ROOT_PLAN_FILENAME : getPlanFilePath(planPath.substring(i)));
        try  {  Util.write(planFile, content, PLAN_ENCODING);  }
        catch (FileNotFoundException e)  {  throw new UserException ("Plan is not found: "+path, UserException.NOT_FOUND);  }
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
            if (values.length!=2)  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, line "+line+" has "+(values.length==1 ? "no" : "too much")+" tab characters");
            //    �������� �������� ������
            if (line==1)  {
                if (level!=0)  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, first line must be root (non-indented)");
                if (!values[0].equals(""))  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, first line (root) must have empty name");
            }
            else  {
                if (level==0)  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, root node (non-indented) on line "+line);
                if (values[0].equals(""))  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, line "+line+" has empty name");
            }
            //    ��������� ������� �����������
            int prevLevel = pathBuffer.size() / 2;
            if (level<prevLevel)  {  pathBuffer.setSize(level * 2);  }
            if (level<=prevLevel)  {  if (level!=0)  pathBuffer.set(pathBuffer.size()-1, values[0]);  }
            else if (level==prevLevel+1)  {  pathBuffer.append("/").append(values[0]);  }
            else  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, too much white spaces at start on line "+line+", previous level "+prevLevel+", found "+level);
            //    ����������
            process(pathBuffer.toString(), level, values[0], values[1]);
        }

        public abstract void process(String path, int level, String name, String title) throws UserException;
    }

    // ���������� ����� plans
    private void savePlans(String username, PlanNode root) throws IOException
    {
        String content = writePlans(root);
        Util.write(new File (getUserDir(username), PLANS_FILENAME), content, PLAN_ENCODING);
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

    // # ����, �����-�� ����������� �� ������ ����� �������� ���������, � ����� �� ���� ������ :)
    // ��������� ���������� � ����� �� ���������� ���� � ���������� � ���� ��� �����, �� ������ ������� �������� depth
    // ��� ������� �������� 0 ����������� ������ ���� �� ���������� ����, 1 - ���� ��� ��� ���������������� ����, � �.�.
    public PlanNode loadPlans(User user, String path, int depth) throws IOException, UserException
    {
        checkPlanOwner(user, path);
        path = path.substring(1, path.indexOf('/', 1));
        String content = Util.read(new File (getUserDir(user.name), PLANS_FILENAME), PLAN_ENCODING);
        content = Util.cutLastNL(content);
        return parsePlans(content, new int[]{0}, new int[]{1}, 0, path, depth);
    }
    // ��������� ������ �� content � ������� pos[0] � ���� ������� ����������� ����� level, �� ��������� ������,
    // ���������� ������� �� ��������� ������ � �������� ���������� ������ ��� �������� �������,
    // ���� ������� ������, �� ���������� ���������� null, ������� �� ��������, ���� ������� ������, �� ������.
    // � row[0] ���������� � ������������� ����� ������ ��� �������.
    // ��������� path � depth ���������� ������� � ���������� �������.
    // ���� depth<0, �� ����� �������������, ��� ����������� ������ ������������ UNLOADED_PLAN_NODE.
    // ���� path="", �� ��� ����������� ������ ������������ ������ PlanNode ���������� ������ � ������, ���� ��� �������� �� depth.
    // ����� ��� ��������� path ����������� ������ ������ ����� �� ���������� path, ���� ��� ����� �������, ��� null.
    private static PlanNode parsePlans(String content, int[] pos, int[] row, int level, String path, int depth)
    {
        //    �� ���������� �������� � ������ ������ ���������� ������� �����������
        int i1 = pos[0];
        while (i1<content.length() && content.charAt(i1)==' ')  i1++;
        int lev = i1 - pos[0];
        //    ���� ������� ����� ���������� - ��������� ������
        if (lev==level)  {
            //    ��������� ������� ������ (����� ��������)
            int i2 = Util.indexOf(content, '\n', i1);
            String[] values = Util.slice(content.substring(i1, i2), '\t');
            if (values.length!=2)  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, line "+row[0]+" has "+(values.length==1 ? "no" : "too much")+" tab characters");
            if (pos[0]==0 && !values[0].equals(""))  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, root record must have empty name");
            if (pos[0]!=0 && values[0].equals(""))  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, line "+row[0]+" has empty name");
            //    ������� � ��������� ������
            pos[0] = i2 + 1;
            row[0]++;
            //    ���� ������ �� ������ ���� � �� ������
            if (depth>=0 && path.length()!=0 && !values[0].equals(""))  {
                //    ���� ���� ���������� �� ���������� �����
                if (path.charAt(0)=='/' && Util.startsWithPath(path, values[0], 1))
                    //    �� �������� ����
                    path = path.substring(1+values[0].length());
                //    ����� ������������� � ����� �������������
                else  depth = -1;
            }
            //    � ������ ������������� ��������� ����� � ������� UNLOADED_PLAN_NODE
            if (depth<0)  {
                while (parsePlans(content, pos, row, level + 1, null, -1)!=null)  ;
                return UNLOADED_PLAN_NODE;
            }
            //    path="" - ��������� ���� � ������ �����
            else if (path.equals(""))  {
                PlanNode result = new PlanNode ();
                result.name = values[0];
                result.title = values[1];
                //    ���������� ���������� �������� ������
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
            //    ����� ����� �� ������� �����, ����� ���������� ����� �����
            else  {
                for (;;)  {
                    PlanNode result = parsePlans(content, pos, row, level + 1, path, depth);
                    if (result==null)  return null;  // ����� ����� ������ �����������, �������
                    if (result!=UNLOADED_PLAN_NODE)  return result;  // ����� ������ �����, ������ ����� �� �������
                }
            }
        }
        //    ������� ������ - ������
        else if (lev>level)  throw new RuntimeException ("Wrong '"+PLANS_FILENAME+"' file, too much white spaces at start on line "+row[0]+", level="+level+", but found "+lev);
        //    ������� ������ - �����
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
                // ������ �������� �� ������������ ����������
                if (!equals(result, expected))  System.err.print("result:\n" + result + "\nbut expected:\n" + expected + "\n");
                else  {
                    // ������ �������� - ����� ������ ������� ���������� ���������� �� ������
                    String writed = writePlans(parsePlans(content, new int[]{0}, new int[]{1}, 0, "", Integer.MAX_VALUE));
                    if (!content.endsWith("\n"))  content += "\n";  // ����� �������������� \n, ������� ������ ��������� writePlans
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
            test("", "Wrong '"+PLANS_FILENAME+"' file, line 1 has no tab characters");
            test("\t", new PlanNode ("", ""));
            test("\tRoot title", new PlanNode ("", "Root title"));
            test("\tRoot title\t", "Wrong '"+PLANS_FILENAME+"' file, line 1 has too much tab characters");
            test("\tRoot\n", new PlanNode ("", "Root"));
            test("\tRoot\n ", "Wrong '"+PLANS_FILENAME+"' file, line 2 has no tab characters");
            test("\tRoot\n xxx", "Wrong '"+PLANS_FILENAME+"' file, line 2 has no tab characters");
            test("\tRoot\n  ", "Wrong '"+PLANS_FILENAME+"' file, too much white spaces at start on line 2, level=1, but found 2");
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
                    "Wrong '"+PLANS_FILENAME+"' file, too much white spaces at start on line 5, level=3, but found 4");
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


    //        ----    ����� � ������� ���������� ����������    ----

    // ��������� ����� � ������
    public void login(String username, String password) throws UserException, IOException
    {
        username = username.toLowerCase();
        File passwordFile = getUserFile(username, PASSWORD_FILENAME);
        login(username, password, passwordFile);
    }
    private void login(String username, String password, File passwordFile) throws UserException, IOException
    {
        //    ���������, ��� ������������ ���������� (���� ���������� ��� ���� � �������)
        if (!passwordFile.exists())  throw new UserException("Wrong username or password");
        //    ��������� ������ �� �����
        byte[] expect = ByteArray.read(passwordFile);
        //    ��������� ��� ��������� ������ � ���������
        byte[] hash = FileDataAccess.passwordServerHash(username, password);
        if (!Arrays.equals(hash, expect))  throw new UserException("Wrong username or password");
    }

    // ������������ ������ ������������
    public synchronized void register(String username, String password) throws UserException, IOException
    {
        username = username.toLowerCase();
        //    ��������� ����� ���
        File userDir = checkUsernameFile(username);
        //    ������� ������� ������������
        Util.mkdir(userDir);
        //    ��������� ��� ������, � �������� � ����
        savePassword(username, password, new File (userDir, PASSWORD_FILENAME));
        //    ������� �������� ����
        Util.write(new File (userDir, ROOT_PLAN_FILENAME), FIRST_PLAN_CONTENT, PLAN_ENCODING);
        Util.write(new File (userDir, PLANS_FILENAME), "\t"+FIRST_PLAN_TITLE, PLAN_ENCODING);
    }

    // ���������, ����� �� ������������ ��� ������������. ���������� ������� ��� ������ ������������ (�� ���������)
    protected File checkUsernameFile(String username) throws UserException, IOException
    {
        super.checkUsername(username);
        File userDir = getUserDir(username);
        if (userDir.exists())  throw new UserException("Username is already taken");
        return userDir;
    }

    // ������ ������ ������������
    public synchronized void changePassword(String username, String oldpswd, String newpswd) throws UserException, IOException
    {
        username = username.toLowerCase();
        //    ��������� ������������ � ������ ������
        File userDir = getUserDir(username);
        File passwordFile = new File (userDir, PASSWORD_FILENAME);
        login(username, oldpswd, passwordFile);
        //    ��������� ��� ������ � ��������� � ���� ����� ������
        savePassword(username, newpswd, new File (userDir, PASSWORD_FILENAME));
    }

    // ������ ��� ������������
    public synchronized void changeUsername(String oldname, String password, String newname) throws UserException, IOException
    {
        oldname = oldname.toLowerCase();
        newname = newname.toLowerCase();
        //    ��������� ������������� ������� ������������ � ��� ������
        File oldUserDir = getUserDir(oldname);
        login(oldname, password, new File (oldUserDir, PASSWORD_FILENAME));
        //    ��������� ����� ���
        File newUserDir = checkUsernameFile(newname);
        //    ������������� �������
        Util.renameTo(oldUserDir, newUserDir);
        //    ��������� ��� ������, � �������� � ����
        savePassword(newname, password, new File (newUserDir, PASSWORD_FILENAME));
    }

    // ������� ������������
    public synchronized void deleteUser(String username, String password) throws UserException, IOException
    {
        username = username.toLowerCase();
        File userDir = getUserDir(username);
        //    ��������� ������������� ������� ������������ � ��� ������
        login(username, password, new File (userDir, PASSWORD_FILENAME));
        //    ������� �������
        Util.delete(userDir);
    }

    private void savePassword(String username, String password, File passwordFile) throws IOException  {
        byte[] hash = passwordServerHash(username, password);
        ByteArray.write(passwordFile, hash);
    }
}
