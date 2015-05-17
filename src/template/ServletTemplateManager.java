package template;

import utils.Util;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Реализует CacheTemplateManager, загружая контент ServletContext.getResourceAsStream
 * And390 - 17.05.2015
 */
public class ServletTemplateManager extends CacheTemplateManager
{
    public final ServletContext servletContext;
    public final String encoding;

    public ServletTemplateManager(ServletContext servletContext)  {  this(servletContext, Charset.defaultCharset().name());  }
    public ServletTemplateManager(ServletContext servletContext_, String encoding_)  {  servletContext=servletContext_;  encoding=encoding_;  }

    protected String readContent(String path) throws IOException {
        try  {  return Util.read(servletContext.getResourceAsStream(path), encoding);  }
        catch (FileNotFoundException e)  {  return null;  }
    }
}
