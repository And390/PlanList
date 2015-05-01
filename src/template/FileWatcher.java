package template;

import utils.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Мониторит изменения файлов в каталоге (рекурсивно в подкаталогах)
 * Пользовательский код должен использовать интерфейс Listener
 * User: And390
 * Date: 08.02.14
 * Time: 22:40
 */
public class FileWatcher implements AutoCloseable
{
    public interface Listener  {
        void changed(String path);  //пути относительно переданного base, всегда начинающиеся с '/', кроме базового, который равен ''
    }

    protected static class ListenerItem  {
        public ListenerItem next;
        public Listener listener;
    }

    protected ListenerItem listenerItems;

    public void addListener(Listener listener)  {
        ListenerItem listenerItem = new ListenerItem ();
        listenerItem.next = listenerItems;
        listenerItem.listener = listener;
        listenerItems = listenerItem;
    }


    private Set<String> ignore = new HashSet<> ();

    private WatchService watchService;
    private Map<WatchKey, String> watchPaths;  // в качестве значений пути с base
    private Thread watchThread;

    public FileWatcher(String base_, String... ignore_) throws IOException
    {
        final String base = Util.cutIfEnds(base_, "/");
        for (String ign : ignore_)
            if (ign.startsWith("/"))  throw new IllegalArgumentException("Absolute paths is not supported");
            else  ignore.add(base_+"/"+ign);

        //    обнаружение изменений файлов
        watchService = FileSystems.getDefault().newWatchService();
        watchPaths = new HashMap<>();
        watchDir(base);

        watchThread = new Thread ()  {
            @Override
            public void run()
            {
                for (;;)
                {
                    //    wait for key to be signalled
                    WatchKey key;
                    try  {  key = watchService.take();  }
                    catch (InterruptedException e)  {  throw new RuntimeException (e);  }
                    catch (ClosedWatchServiceException e)  {  break;  }  //stop now

                    //    pull it path
                    String dir = watchPaths.get(key);
                    if (dir == null)  throw new NullPointerException ("WatchKey is not registered");

                    //    process each event for this key
                    for (WatchEvent<?> event_ : key.pollEvents())  {
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> event = (WatchEvent<Path>) event_;

                        if (event_.kind()==OVERFLOW)  continue;

                        //    context for directory entry event is the file name of entry
                        String path = dir + "/" + event.context().toString();

                        //    if directory is created, and watching recursively, then register it and its sub-directories
                        if (event.kind()==ENTRY_CREATE)  {
                            if (!ignore.contains(path))
                                try  {  if (new File(path).isDirectory())  watchDir(path);  }
                                catch (IOException e)  {  e.printStackTrace();  }
                        }
                        else
                            //    call event listeners
                            for (ListenerItem lstnr=listenerItems; lstnr!=null; lstnr=lstnr.next)
                                lstnr.listener.changed(path.substring(base.length()));
                    }

                    //     reset key and remove from set if directory no longer accessible
                    boolean valid = key.reset();
                    if (!valid)  {
                        // may log watch dir...
                        watchPaths.remove(key);
                        if (watchPaths.isEmpty())  break;  //all directories are inaccessible
                    }
                }
            }
        };
        watchThread.start();
    }

    private void watchDir(String dir) throws IOException
    {
        final Path dirPath = Paths.get(dir);

        Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
                String child = dirPath + (dirPath.equals(path) ? "" : "/" + Util.cutIfEnds(dirPath.relativize(path).toString().replace("\\", "/"), "/"));
                if (ignore.contains(child))  return FileVisitResult.SKIP_SUBTREE;
                WatchKey watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                watchPaths.put(watchKey, child);
                // may log watch child...
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void close() throws IOException, InterruptedException
    {
        watchService.close();
        watchThread.join();
    }
}
