package planlist.data;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import planlist.PlanNode;
import planlist.User;
import planlist.UserException;
import utils.ExternalException;
import utils.Util;

import java.util.ArrayList;

/**
 * And390 - 24.05.2015
 */
public class MongoDBDataAccess extends DataAccess
{
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> users;

    public MongoDBDataAccess(String url)  {
        MongoClientURI mongoURI = new MongoClientURI(url);
        mongoClient = new MongoClient (mongoURI);
        database = mongoClient.getDatabase(mongoURI.getDatabase());
        users = database.getCollection("users");
    }

    @Override
    public void close() {
        mongoClient.close();
    }



    //        ----    логин и функции управлени€ аккаунтами    ----

    private Document makeUserQuery(String username, String password) {
        username = username.toLowerCase();
        return new Document("name", username).append("password", passwordServerHash(username, password));
    }

    // ѕровер€ет логин и пароль
    public void login(String username, String password) throws UserException
    {
        try (MongoCursor<Document> cursor = users.find(makeUserQuery(username, password)).iterator())  {
            if (!cursor.hasNext())  throw new UserException("Wrong username or password");
        }
    }

    // –егистрирует нового пользовател€
    public void register(String username, String password) throws UserException
    {
        //    проверить новое им€
        checkUsername(username);
        //    сохранить нового пользовател€
        UpdateResult result = users.updateOne(
                new Document("name", username), // find by username
                new Document("$setOnInsert",  // insert if not found
                    makeUserQuery(username, password)
                            .append("title", FIRST_PLAN_TITLE)
                            .append("content", FIRST_PLAN_CONTENT)),
                new UpdateOptions().upsert(true));
        if (result.getMatchedCount()==1)  throw new UserException("Username is already taken");
    }

    public void changeUsername(String oldname, String password, String newname) throws ExternalException
    {
         throw new UnsupportedOperationException();
//        // need atomically check existance
//        //...
//        //    проверить новое им€
//        checkUsername(newname);
//        //    изменить им€
//        UpdateResult result = users.updateOne(makeUserQuery(oldname, password), new Document("$set", new Document("name", newname)));
//        if (result.getMatchedCount()==0)  throw new UserException("Username and password are not match");
//        if (result.getModifiedCount()!=1)
//            throw new ExternalException ("Wrong update MongoDB .users: update count="+result.getMatchedCount());
    }

    public void changePassword(String username, String oldpswd, String newpswd) throws ExternalException
    {
        UpdateResult result = users.updateOne(makeUserQuery(username, oldpswd), new Document("$set", new Document("password", newpswd)));
        if (result.getMatchedCount()==0)  throw new UserException("Username and password are not match");
        if (result.getModifiedCount()!=1)
            throw new ExternalException ("Wrong update MongoDB .users: update count="+result.getMatchedCount());
    }

    public synchronized void deleteUser(String username, String password) throws ExternalException  {
        DeleteResult result = users.deleteOne(makeUserQuery(username, password));
        if (result.getDeletedCount()==0)  throw new UserException("Username and password are not match");
        if (result.getDeletedCount()!=1)
            throw new ExternalException ("Error delete MongoDB .users: delete count="+result.getDeletedCount());
    }


    //        ----    функции работы с планами    ----

    public static final String CHILD_PREFIX = "#";

    // загружает данные пользовател€ по его имени
    private Document loadUserData(String username) throws UserException
    {
        try (MongoCursor<Document> cursor = users.find(new Document("name", username)).iterator())  {
            if (!cursor.hasNext())  throw new UserException ("User is not found: "+username, UserException.NOT_FOUND);
            return cursor.next();
        }
    }

    // находит в данных пользовател€ нужный узел данных плана по его пути
    @SuppressWarnings("unused")
    private Document findChild(Document data, String path, int offset) throws UserException
    {
        for (int i=offset; i!=path.length();)  {
            i++;
            int i0 = i;
            i = Util.indexOf(path, '/', i);
            data = getChild(data, path, i0, i);
        }
        return data;
    }

    // находит в данных пользовател€ родител€ указанного узла плана по его пути
    private Document findChildParent(Document data, String path, int offset) throws UserException
    {
        for (int i=offset;;)  {
            i++;
            int i0 = i;
            i = path.indexOf('/', i);
            if (i==-1)  return data;
            data = getChild(data, path, i0, i);
        }
    }

    private Document getChild(Document data, String path, int offsetStart, int offsetEnd) throws UserException
    {
        data = (Document)data.get(CHILD_PREFIX + path.substring(offsetStart, offsetEnd));
        if (data==null)  throw new UserException ("Plan is not found: "+path, UserException.NOT_FOUND);
        return data;
    }

    // объедин€ет две предыдущие функции, возвращает загруженные данные пользовател€, найденный узел данных плана
    private Document[] loadData(String path) throws UserException
    {
        //    разложить полный путь на им€ и путь относительно пользовател€
        int s = Util.indexOf(path, '/', 1);
        final String username = path.substring(1, s);  //им€ целевого пользовател€, может отличатьс€ от исполн€ющего
        //    load user data
        Document[] result = new Document [3];
        result[0] = loadUserData(username);  //root
        //    find child plan by path
        if (s != path.length())  {
            result[2] = findChildParent(result[0], path, s);  //parent of target
            result[1] = getChild(result[2], path, path.lastIndexOf('/')+1, path.length());  //target
        }  else  {
            result[2] = null;
            result[1] = result[0];
        }
        return result;
    }

    private void modifyData(Document data) throws ExternalException
    {
        //TODO нет защиты от одновременных модификаций
        UpdateResult result = users.replaceOne(new Document("_id", data.getObjectId("_id")), data);
        if (result.getModifiedCount()!=1)  throw new ExternalException(String.format(
                "Can not modify user data: %s (modified count=%d)", data.getObjectId("_id"), result.getModifiedCount()));
    }

    public void addPlan(User user, String path, String name_, String title_) throws ExternalException  {
        //    проверки
        final String name = checkPlanName(name_);
        final String title = checkPlanTitle(title_);
        checkPlanOwner(user, path);
        //    load
        Document[] data = loadData(path);
        //    modify
        Object old = data[1].put(CHILD_PREFIX+name, new Document("title", title).append("content", DEFAULT_PLAN_CONTENT));
        if (old!=null)  throw new UserException ("Plan already exists: "+path);
        modifyData(data[0]);
    }

    public void editPlan(User user, String fullPath, String name_, String title_) throws ExternalException
    {
        //    проверки
        int l = fullPath.lastIndexOf('/');       //указатель на последний слэш
        int p = l==0 ? fullPath.length() : l+1;  //указатель на начало имени плана в строке fullPath
        if (l==-1 || !fullPath.startsWith("/"))  throw new IllegalArgumentException ("Wrong fullPath: "+fullPath);
        if (l==0 && name_!=null)  throw new UserException ("It is not possible to change root plan name");
        final String oldName = fullPath.substring(p);
        final String name = name_!=null ? checkPlanName(name_) : oldName;
        final String title = checkPlanTitle(title_);
        checkPlanOwner(user, fullPath);
        //    load
        Document[] data = loadData(fullPath);
        //    modify
        data[1].put("title", title);
        if (!name.equals(oldName))  {
            data[2].remove(CHILD_PREFIX+oldName);
            if (data[2].put(CHILD_PREFIX+name, data[1]) != null)  throw new UserException("Plan already exists: "+name);
        }
        modifyData(data[0]);
    }

    public void deletePlan(User user, String fullPath) throws Exception
    {
        //    check
        int l = fullPath.lastIndexOf('/');       //указатель на последний слэш
        int p = l==0 ? fullPath.length() : l+1;  //указатель на начало имени плана в строке fullPath
        if (l==-1 || !fullPath.startsWith("/"))  throw new IllegalArgumentException ("Wrong fullPath: "+fullPath);
        if (l==0)  throw new UserException ("It is not possible to delete root plan");
        checkPlanOwner(user, fullPath);
        //    load
        Document[] data = loadData(fullPath);
        //    modify
        String oldName = fullPath.substring(p);
        data[2].remove(CHILD_PREFIX+oldName);
        modifyData(data[0]);
    }

    public String loadPlanContent(User user, String path) throws Exception  {
        //    проверить
        checkPlanOwner(user, path);
        //    load
        String content = loadData(path)[1].getString("content");
        if (content==null)  throw new ExternalException ("content is null for "+path);
        return content;
    }

    public void savePlanContent(User user, String path, String content) throws Exception
    {
        //    check
        checkPlanOwner(user, path);
        //    load
        Document[] data = loadData(path);
        //    modify
        data[1].put("content", content);
        modifyData(data[0]);
    }

    public PlanNode loadPlans(User user, String path, int depth) throws Exception
    {
        //    проверить
        checkPlanOwner(user, path);
        //    load
        Document[] data = loadData(path);
        //    parse and return
        return parsePlanNode(data[1], Util.sliceAfterLast(path, '/'), depth);
    }

    private PlanNode parsePlanNode(Document data, String planName, int recursion)  {
        PlanNode node = new PlanNode ();
        node.name = planName;
        node.title = data.getString("title");
        if (recursion>0)  {
            node.childs = new ArrayList<> ();
            for (String key : data.keySet())  if (key.startsWith(CHILD_PREFIX))
                node.childs.add(parsePlanNode((Document)data.get(key), key.substring(CHILD_PREFIX.length()), recursion-1));
        }
        return node;
    }
}
