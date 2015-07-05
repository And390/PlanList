package planlist.data;

import planlist.PlanNode;
import planlist.User;
import planlist.UserException;
import utils.Util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.math.BigInteger;

/**
 * And390 - 24.05.2015
 */
public abstract class DataAccess implements AutoCloseable
{
    // корневой план имеет путь "", дочерний корневого - "/child", его дочерний - "/child/sub"

    public abstract void register(String username, String password) throws Exception;
    public abstract void login(String username, String password) throws Exception;
    public abstract void changeUsername(String oldname, String password, String newname) throws Exception;
    public abstract void changePassword(String username, String oldpswd, String newpswd) throws Exception;
    public abstract void deleteUser(String username, String password) throws Exception;

    public abstract void addPlan(User user, String path, String name, String title) throws Exception;
    public abstract void editPlan(User user, String path, String name, String title) throws Exception;
    public abstract void deletePlan(User user, String path) throws Exception;

    public abstract String loadPlanContent(User user, String path) throws Exception;
    public abstract void savePlanContent(User user, String path, String content) throws Exception;
    public abstract PlanNode loadPlans(User user, String path, int depth) throws Exception;

    public void close()  {}

    public static final String FIRST_PLAN_TITLE = "My plans";

    public static final String FIRST_PLAN_CONTENT =
        "Use Edit button, Ctrl+E, Ctrl+Enter or click on empty space on the sides of this text to edit full plan\n" +
        "Use Save button, Ctrl+S, Ctrl+Enter or click on empty space on the sides of this text to save edition and return view\n" +
        "Use Cancel button or Esc to cancel changes\n" +
        "You can edit single records by clicking on them\n" +
        "You can change marker by clicking on them\n" +
        " - This is a plan example\n" +
        "   - try to modify it\n" +
        "   * look at another markers\n" +
        "   - remove it and write your own\n" +
        "\n" +
        "It is possible to hierarchically create child plan pages\n" +
        "The following buttons (only one 'Add' for now) are used for this";

    public static final String DEFAULT_PLAN_CONTENT = "";


    // проверяет, корректно ли имя пользователя
    protected void checkUsername(String username) throws UserException
    {
        if (username.equals(""))  throw new UserException ("Empty username");
        if (username.length()>=256)  throw new UserException("Username must be less than 256 characters in length");
        for (char c : username.toCharArray())  if (!Character.isLetterOrDigit(c) && c!=' ' && c!='.' && c!='-' && c!='_')
            throw new UserException("Username must contains only letters, digits, ' ', '.', '-' or '_'");
    }

    protected static String checkPlanName(String name) throws UserException
    {
        if (name.equals(""))  throw new UserException ("Empty plan name");
        if (name.length()>=256)  throw new UserException("Plan name must be less than 256 characters in length");
        for (char c : name.toCharArray())  if (!Character.isLetterOrDigit(c) && c!='.' && c!='-' && c!='_')
            throw new UserException("Plan name must contains only letters, digits, '.', '-' or '_'");
        return name.toLowerCase();
    }

    protected static String checkPlanTitle(String title) throws UserException
    {
        for (char c : title.toCharArray())  if (c=='\t' || c=='\n' || c=='\r')
            throw new UserException("Plan title must not contain tab and line breaks");
        return title;
    }

    protected void checkPlanOwner(User user, String planPath) throws UserException
    {
        if (user==null)  throw new UserException ("not logged in", UserException.FORBIDDEN);
        String planUser = planPath.substring(1, Util.indexOf(planPath, '/', 1));
        if (!planUser.equals(user.name.toLowerCase()))  throw new UserException ("You have no access to other user's plans", UserException.FORBIDDEN);
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
        public static void main(String[] args) throws IOException {
            System.out.print(passwordClientHash(args[0].toLowerCase(), args[1]));
        }
    }

    public static class PasswordServerHash
    {
        public static void main(String[] args) throws IOException  {
            System.out.write(passwordServerHash(args[0].toLowerCase(), args[1]));
        }
    }

    public static class PasswordClientServerHash
    {
        public static void main(String[] args) throws IOException  {
            String username = args[0].toLowerCase();
            System.out.write(passwordServerHash(username, passwordClientHash(username, args[1])));
        }
    }
}
