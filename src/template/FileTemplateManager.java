package template;

import utils.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Реализует CacheTemplateManager, загружая контент из файлов по указанным путям
 * And390 - 20.04.2015
 */
public class FileTemplateManager extends CacheTemplateManager
{
    public final File root;
    public final String encoding;

    public FileTemplateManager()  {  this(new File ("."));  }
    public FileTemplateManager(File root)  {  this(root, Charset.defaultCharset().name());  }
    public FileTemplateManager(String encoding)  {  this(new File ("."), encoding);  }
    public FileTemplateManager(File root_, String encoding_)  {  root=root_;  encoding=encoding_;  }

    protected String readContent(String path) throws IOException  {
        try  {  return Util.read(new File(root, path), encoding);  }
        catch (FileNotFoundException e)  {  return null;  }
    }
}
