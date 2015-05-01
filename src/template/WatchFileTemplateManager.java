package template;

import java.io.File;
import java.io.IOException;

/**
 * В дополнение к FileTemplateManager мониторит заданный каталог, выгружая измененные шаблоны из кэша
 * And390 - 21.04.2015
 */
public class WatchFileTemplateManager extends FileTemplateManager implements AutoCloseable
{
    private FileWatcher watcher;

    public WatchFileTemplateManager(File file, String encoding) throws IOException
    {
        super(file, encoding);
        watcher = new FileWatcher (file.getPath());
        watcher.addListener(new FileWatcher.Listener ()  {
            public void changed(String path)  {
                putTemplate(path, null);
            }
        });
    }

    public void close() throws IOException, InterruptedException
    {
        watcher.close();
    }
}
