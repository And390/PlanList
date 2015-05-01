package template;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TemplateManager, реализующий потокобезопасное кэширование шаблонов в методе getTemplate,
 * загрузку контента абстрактному методу readContent,
 * And390 - 20.04.2015
 */
public abstract class CacheTemplateManager extends TemplateManager
{
    private ConcurrentHashMap<String, TemplateWrapper> cache = new ConcurrentHashMap<> ();

    // Обертки нужны исключительно для синхронизации, чтобы реализовать double-check locking
    private static class TemplateWrapper  {
        volatile Template template;
        TemplateWrapper()  {}
        TemplateWrapper(Template template_)  {  template=template_;  }
    }
    private static final Template NO_TEMPLATE = new StaticTemplate (null)  {};

    public Template getTemplate(String path) throws Exception
    {
        path = checkPath(path);
        //    достать из кэша объект-обертку, если нет создать (с пустым шаблоном)
        TemplateWrapper wrapper = cache.get(path);
        if (wrapper==null)  {
            wrapper = new TemplateWrapper ();
            TemplateWrapper concurrent = cache.putIfAbsent(path, wrapper);  // в кэше временно пустая обертка
            if (concurrent!=null)  wrapper = concurrent;
        }
        //    если нет шаблона в обертке, синхронизированно загрузить его (double-check locking)
        Template template = wrapper.template;
        if (template==null)
            synchronized (wrapper)  {
                template = wrapper.template;
                if (template==null)  {  // при параллельных запросах одинаковых path, только первый поток получит здесь null
                    String content = readContent(path);  // прочитать контент
                    template = content==null ? NO_TEMPLATE : parseAndPut(content, this, path);  // разобрать, если найден, иначе спец константа
                    wrapper.template = template;  // записать значение в обертку
                    if (content==null && !cacheAbsent)  cache.remove(path);  // если не найдено и не кэшируем промахи, удалить из кэша
                }
            }
        return template==NO_TEMPLATE ? null : template;
    }

    public void putTemplate(String path, Template template)
    {
        path = checkPath(path);
        if (template!=null)  cache.put(path, new TemplateWrapper (template));
        else  cache.remove(path);
    }

    public void clear()
    {
        cache.clear();
    }

    // читает и возвращает контент по указанному пути, если не найдено, возвращает null
    protected abstract String readContent(String path) throws IOException;


    //        ----    config    ----

    private volatile boolean cacheAbsent = false;

    public boolean isCacheAbsent() {
        return cacheAbsent;
    }

    public void setCacheAbsent(boolean cacheAbsent) {
        this.cacheAbsent = cacheAbsent;
    }
}
