package template;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TemplateManager, ����������� ���������������� ����������� �������� � ������ getTemplate,
 * �������� �������� ������������ ������ readContent,
 * And390 - 20.04.2015
 */
public abstract class CacheTemplateManager extends TemplateManager
{
    private ConcurrentHashMap<String, TemplateWrapper> cache = new ConcurrentHashMap<> ();

    // ������� ����� ������������� ��� �������������, ����� ����������� double-check locking
    private static class TemplateWrapper  {
        volatile Template template;
        TemplateWrapper()  {}
        TemplateWrapper(Template template_)  {  template=template_;  }
    }
    private static final Template NO_TEMPLATE = new StaticTemplate (null)  {};

    public Template getTemplate(String path) throws Exception
    {
        path = checkPath(path);
        //    ������� �� ���� ������-�������, ���� ��� ������� (� ������ ��������)
        TemplateWrapper wrapper = cache.get(path);
        if (wrapper==null)  {
            wrapper = new TemplateWrapper ();
            TemplateWrapper concurrent = cache.putIfAbsent(path, wrapper);  // � ���� �������� ������ �������
            if (concurrent!=null)  wrapper = concurrent;
        }
        //    ���� ��� ������� � �������, ����������������� ��������� ��� (double-check locking)
        Template template = wrapper.template;
        if (template==null)
            synchronized (wrapper)  {
                template = wrapper.template;
                if (template==null)  {  // ��� ������������ �������� ���������� path, ������ ������ ����� ������� ����� null
                    String content = readContent(path);  // ��������� �������
                    template = content==null ? NO_TEMPLATE : parseAndPut(content, this, path);  // ���������, ���� ������, ����� ���� ���������
                    wrapper.template = template;  // �������� �������� � �������
                    if (content==null && !cacheAbsent)  cache.remove(path);  // ���� �� ������� � �� �������� �������, ������� �� ����
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

    // ������ � ���������� ������� �� ���������� ����, ���� �� �������, ���������� null
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
